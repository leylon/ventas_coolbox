package com.pedidos.android.persistence.ui.ending

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.db.entity.ReceiptEntity
import com.pedidos.android.persistence.db.entity.SaleEntity
import com.pedidos.android.persistence.db.entity.SaleSubItemEntity
import com.pedidos.android.persistence.model.LoginResponse
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrint
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrintRequest
import com.pedidos.android.persistence.model.firma.ActualizarFirmaRequest
import com.pedidos.android.persistence.model.firma.FirmaDetail
import com.pedidos.android.persistence.model.firma.FirmaHead
import com.pedidos.android.persistence.model.firma.FirmaRequest
import com.pedidos.android.persistence.model.firma.FirmaResponse
import com.pedidos.android.persistence.model.sale.ValidaCobraRequest
import com.pedidos.android.persistence.ui.menu.MenuActivity
import com.pedidos.android.persistence.ui.payment.PaymentActivity
import com.pedidos.android.persistence.ui.sale.SaleActivity
import com.pedidos.android.persistence.ui.sale.SaleActivity.Companion
import com.pedidos.android.persistence.ui.sale.fragment.AceptacionGarantiaFragment
import com.pedidos.android.persistence.ui.sale.fragment.GexDetalleAdapter
import com.pedidos.android.persistence.utils.DrawCustomView
import com.pedidos.android.persistence.utils.Formatter
import com.pedidos.android.persistence.viewmodel.EndingViewModel
import kotlinx.android.synthetic.main.ending_activity.*
import kotlinx.android.synthetic.main.ending_activity.fltLoading
import kotlinx.android.synthetic.main.ending_activity.toolbar
import kotlinx.android.synthetic.main.item_cotizacion.tvFecha
import kotlinx.android.synthetic.main.sales_activity.btnProcess
import kotlinx.android.synthetic.main.sales_activity.etwAddProduct
import kotlinx.android.synthetic.main.sales_activity.imbwAddProductManualOnly
import kotlinx.android.synthetic.main.sales_activity.imbwAddProductoWithCamera
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class EndingActivity : MenuActivity() {
    private lateinit var viewModel: EndingViewModel
    companion object {
        val TAG = EndingActivity::class.java.simpleName!!
        const val EXTRA_ENTITY = "ui.ending.EndingActivity.SaleEntity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithMenu(R.layout.ending_activity)
        setSupportActionBarMenu(toolbar)
        checkSession()
        val user = getSession()
        val saleEntity = intent.getParcelableExtra(EXTRA_ENTITY) as SaleEntity?
        lateinit var factory : EndingViewModel.Companion.Factory
        try {
            factory = EndingViewModel.Companion.Factory(application, getSettings().urlbase)
        } catch (e : Throwable) {
        }
        viewModel = ViewModelProviders.of(this, factory)[EndingViewModel::class.java]
        viewModel.showProgress.observe(this, Observer { it -> showLoading(it!!) })
        viewModel.saleLiveData.observe(this, Observer { updateScreen(it) })
        viewModel.receiptLiveData.observe(this, Observer { saleEntity?.let { it1 -> performAfterOperations(it, it1.documento) } })
        viewModel.receiptPrintCotizacionLiveData.observe( this, Observer { it -> performAfterOperationsCoti(it!!) })
        viewModel.errorMessages.observe(this, Observer { it ->
            if (it != null) {
                onError(it)
            }
        })
        if (user.pagoFalabella && saleEntity?.cotizacion.isNullOrEmpty() ) {
            saleEntity?.statusCotizacion = 1
            btnCobrar.visibility = View.GONE
            btnSaveCotizacion.visibility = View.VISIBLE
        }else {
            saleEntity?.statusCotizacion = 0
            btnSaveCotizacion.visibility = View.GONE
            btnCobrar.visibility = View.VISIBLE
        }
        viewModel.saveCotizacionLiveData.observe(this, Observer { it ->
            btnSaveCotizacion.isEnabled = true
            btnSaveCotizacion.isClickable = true
            if (it != null && it.success) {

                Log.d(TAG, "Cotizacion guardada correctamente")
                onError(it.message.toString())
                obtenerCotizacion("")
            } else {
                Log.e(TAG, "")
                onError(it?.message.toString())
            }
        })
        if (getSession().pagoFalabella) {
            btnSaveCotizacion.visibility = View.VISIBLE
            btnCotizacion.visibility = View.VISIBLE
        } else {
            btnSaveCotizacion.visibility = View.GONE
            btnCotizacion.visibility = View.GONE
        }
        viewModel.saleLiveData.postValue(saleEntity)
        btnCobrar.setOnClickListener { validaCobraPedido()}//cobrarPedido() }
        btnEliminar.setOnClickListener { eliminarPedido() }
        btnImprimir.setOnClickListener { obtenerPedido() }
        btnCotizacion.setOnClickListener { obtenerCotizacion("1") }
        btnSaveCotizacion.setOnClickListener { savePedido() }
        btnRegresar.setOnClickListener {  //cobrarPedido()
            onBackPressed()
        }
        btnVisa.setOnClickListener { cobrarPedido() }
        viewModel.validaFirma.observe( this, Observer { firmaResponse ->
            if (firmaResponse != null) {
                if (firmaResponse.result) {

                    mostrarFirma(firmaResponse.data)

                } // else {
                   // onError("Error al validar firma")
                //}
            }
        })
        viewModel.actualizaFirma.observe( this, Observer { firmaResponse ->
            if (firmaResponse != null) {
                if (firmaResponse.result) {
                    val message : String = firmaResponse.message ?: "Firma actualizada correctamente"
                    onError(message)
                    cobrarPedido()
                } else {
                    onError(firmaResponse.message.toString())
                }
            }
        })

        viewModel.validadCobra.observe( this, Observer { firmaResponse ->
            if (firmaResponse != null) {
                if (firmaResponse.result) {
                    cobrarPedido()
                } else {
                    onError(firmaResponse.message.toString())
                }
            }

        })


    }

    private fun validaCobraPedido() {
        val settings = getSettings()
        val request = ValidaCobraRequest(
            tipoDocumento = "PED",
            documento = viewModel.saleLiveData.value!!.documento,
            androidimei = settings.imei ,
            androiduuid = settings.uuid,
        )
        viewModel.validaCobra(request)

    }

    private fun validarFirma() {
        println("Validar firma para tienda: ${getSession().tienda} , " +
              //  "pedido: ${viewModel.}, " +
                "usuario: ${getSession().usuario}")
        viewModel.obtenerValidacionFirmas(
            FirmaRequest(
                tienda =  "${getSession().tienda}",
                pedido = tvwOrderNumber.text.toString(),//"viewModel.saleLiveData.value!!.documento",
                usuario = "${getSession().usuario}"
            ))

    }


    private fun mostrarFirma(firmaResponse: FirmaHead) {
        // 1. Creamos el Dialog asociado a esta Activity
        val dialog = Dialog(this)

        // Quitamos el título por defecto de Android para que se vea moderno
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // 2. Le asignamos tu diseño XML (asumiendo que tu archivo se llama fragment_aceptacion_garantia.xml,
        // aunque ahora ya no sea un fragmento, el nombre del archivo no importa)
        dialog.setContentView(R.layout.fragment_aceptacion_garantia)

        // 3. ¡AQUÍ CONTROLAS EL TAMAÑO!
        // MATCH_PARENT de ancho para que ocupe casi toda la pantalla horizontal
        // WRAP_CONTENT de alto para que la ventana sea lo más chica posible (solo lo que ocupa el contenido)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Opcional: fondo transparente para que se respeten los bordes curvos si los tuviera
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 4. Referenciamos las vistas del XML
        val tvNombreCompleto = dialog.findViewById<TextView>(R.id.tvNombreCompleto)
        val tvDescripcion = dialog.findViewById<TextView>(R.id.tvDescripcion)
        val tvDoi = dialog.findViewById<TextView>(R.id.tvDoi)
        val etEmail = dialog.findViewById<EditText>(R.id.etEmail)
        val tvPoliticaLink = dialog.findViewById<TextView>(R.id.tvPoliticaLink)
        val cbPolitica = dialog.findViewById<CheckBox>(R.id.cbPolitica)
        val viewFirma = dialog.findViewById<DrawCustomView>(R.id.viewFirma)
        val tvLimpiar = dialog.findViewById<TextView>(R.id.tvLimpiar)
        val btnContinuar = dialog.findViewById<Button>(R.id.btnContinuar)

        fun validarBotonContinuar() {
            // Solo es verdadero si tiene firma Y la política está marcada
            val esValido = cbPolitica.isChecked && viewFirma.hasSignature()

            btnContinuar.isEnabled = esValido
            // Bajamos la opacidad a la mitad si está deshabilitado para que se vea gris/apagado
            btnContinuar.alpha = if (esValido) 1.0f else 0.5f
        }
        validarBotonContinuar()

        val btnRechazar = dialog.findViewById<Button>(R.id.btnRechazar)
        val tvVerDetalle = dialog.findViewById<TextView>(R.id.tvVerDetalle)
        // 3. Escuchar cambios en el CheckBox
        cbPolitica.setOnCheckedChangeListener { _, _ ->
            validarBotonContinuar()
        }
        viewFirma.onSignatureChanged = {
            validarBotonContinuar()
        }
        tvLimpiar.setOnClickListener {
            viewFirma.resetCanvasDrawing()
        }
        // 5. Llenamos los datos que vienen del servicio
        tvNombreCompleto.text = "Nombre completo: ${firmaResponse.noClie}"
        tvDoi.text = "DOI: ${firmaResponse.nuDocuIden}"
        etEmail.setText(firmaResponse.deMailClie)
        tvDescripcion.text ="Deseo incluir Garantía Extendida ${firmaResponse.gexDescripcion} por un valor de ${firmaResponse.gexImport} al artículo ${firmaResponse.gexProducto}. Dejo mi firma en señal de conformidad:"

        // 6. Lógica de los botones
        tvPoliticaLink.setOnClickListener {
            // Llamamos a la función para abrir la web (está definida más abajo)
            mostrarPopupWeb(firmaResponse.urlTermino)
        }
        tvVerDetalle.setOnClickListener {
            // Suponiendo que tu API trae la lista en firmaResponse.detallesGex
            // Aquí paso una lista simulada basada en tu imagen. Deberás pasar tu lista real.

            mostrarDialogoDetalle(firmaResponse.detalle)
        }
        tvLimpiar.setOnClickListener {
            viewFirma.resetCanvasDrawing()
        }

        btnContinuar.setOnClickListener {
            if (cbPolitica.isChecked) {
                val emailFinal = etEmail.text.toString()
                val firmaBitmap = viewFirma.getSignatureBitmap()

                // 2. Lo convertimos a Base64 usando nuestra nueva función
                val firmaBase64 = convertirBitmapABase64(firmaBitmap)
                // TODO: Aquí ya tienes la firma (firmaBitmap) y el email (emailFinal)
                // Puedes pasarlos a tu ViewModel para enviarlos al servidor.
                val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val horaActual = formatoHora.format(Date())
                viewModel.actualizarFirmas(ActualizarFirmaRequest(
                    tienda =  "${getSession().tienda}",
                    pedido = tvwOrderNumber.text.toString(),
                    usuario = "${getSession().usuario}",
                    correoCliente = emailFinal,
                    firmaCliente = firmaBase64,
                    aceptaTerminos = cbPolitica.isChecked ,
                    fecha = tvwOrderDate.text.toString(),
                    hora = horaActual // Aquí podrías poner la hora actual si quieres
                ))
                dialog.dismiss() // Cierra la ventana
            } else {
                onError("Debe aceptar la Política de Privacidad")
            }
        }

        btnRechazar.setOnClickListener {
            dialog.dismiss() // Solo cierra la ventana
        }

        // 7. Finalmente, mostramos el Dialog
        dialog.show()
    }

    private fun convertirBitmapABase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Comprimimos en PNG (mantiene mejor la nitidez de los trazos) al 100% de calidad
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        // Usamos NO_WRAP para que el string generado sea continuo y no rompa tu JSON con saltos de línea
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    // Función para mostrar la página web de los términos
    private fun mostrarPopupWeb(url: String) {
        val webView = WebView(this).apply {
            webViewClient = WebViewClient()
            loadUrl(url)
        }

        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
            .setTitle("Política de Privacidad")
            .setView(webView)
            .setPositiveButton(R.string.aceptar) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Cambiamos el parámetro para que reciba tu modelo real
    private fun mostrarDialogoDetalle(listaInicial: MutableList<FirmaDetail>) {
        val dialogDetalle = Dialog(this)
        dialogDetalle.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogDetalle.setContentView(R.layout.dialog_detalle_gex)
        dialogDetalle.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val rvDetalleGex = dialogDetalle.findViewById<RecyclerView>(R.id.rvDetalleGex)
        val tvTotalItem = dialogDetalle.findViewById<TextView>(R.id.tvTotalItem)
        val tvTotalGex = dialogDetalle.findViewById<TextView>(R.id.tvTotalGex)
        val btnCerrar = dialogDetalle.findViewById<Button>(R.id.btnCerrarDetalle)

        rvDetalleGex.layoutManager = LinearLayoutManager(this)

        var listaActual = listaInicial

        // Recalcular usando tu modelo
        fun recalcularTotales() {
            var totalItem = 0.0
            var totalGex = 0.0
            for (articulo in listaActual) {
                // Sumamos usando tus campos, protegiendo contra nulos con ?: 0.0
                totalItem += articulo.prVentCimp ?: 0.0
                totalGex += articulo.prGex ?: 0.0
            }
            tvTotalItem.text = "S/ ${String.format("%.2f", totalItem)}"
            tvTotalGex.text = "S/ ${String.format("%.2f", totalGex)}"
        }

        val adapter = GexDetalleAdapter(listaActual) { itemBorrar, posicion ->
            // --- LÓGICA DE ELIMINACIÓN ---
            // itemBorrar ahora es un objeto FirmaDetail.
            // Puedes usar itemBorrar.nuSecu o itemBorrar.coItemGex para enviarlo a tu API
            // Ejemplo: viewModel.eliminarGarantia(itemBorrar.nuSecu)

            listaActual.removeAt(posicion)
            rvDetalleGex.adapter?.notifyItemRemoved(posicion)
            recalcularTotales()
        }

        rvDetalleGex.adapter = adapter
        recalcularTotales()

        btnCerrar.setOnClickListener {
            dialogDetalle.dismiss()
        }

        dialogDetalle.show()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun eliminarPedido() {
        finish()
        startActivity(Intent(this, SaleActivity::class.java))
    }

    private fun obtenerPedido() {
        viewModel.getSaleReceipt(viewModel.saleLiveData.value!!.documento)
        //PDF
        //viewModel.getSaleReceiptPDF(viewModel.saleLiveData.value!!.documento)
    }
    private fun savePedido() {
        btnSaveCotizacion.isEnabled = false
        btnSaveCotizacion.isClickable = false
        viewModel.saveCotizacion(CotizacionPrintRequest(tipo = "PED",
            documento = viewModel.saleLiveData.value!!.documento,"",""))
        //PDF
        //viewModel.getSaleReceiptPDF(viewModel.saleLiveData.value!!.documento)
    }
    private fun obtenerCotizacion(numero : String){

        viewModel.getSaleReceiptPrintCotizacion(CotizacionPrintRequest(tipo = "PED",
            documento = viewModel.saleLiveData.value!!.documento,
            numero = numero,
            papelSize = getSettings().pageSize))
    }

    private fun cobrarPedido() {
        startActivity(Intent(this, PaymentActivity::class.java).apply {
            putExtra(PaymentActivity.ENTITY_EXTRA, viewModel.saleLiveData.value)
        })
    }

    private fun showLoading(show: Boolean) {
        fltLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun updateScreen(entity: SaleEntity?) {
        if (entity == null) {
            return
        }

        tvwOrderNumber.text = entity.documento
        tvwOrderDate.text = entity.fecha
        tvwEvento.text = entity.evento
        tvwSaleTotal.text = Formatter.DoubleToString(entity.total, entity.monedaSimbolo)
        tvwSubTotal.text = Formatter.DoubleToString(entity.subTotal, entity.monedaSimbolo)
        tvwDescuento.text = Formatter.DoubleToString(entity.descuento, entity.monedaSimbolo)
        tvwImpuestoA.text = Formatter.DoubleToString(entity.impuesto, entity.monedaSimbolo)
        tvwImpuestoB.text = Formatter.DoubleToString(entity.impuesto2, entity.monedaSimbolo)
        tvwImpuestoC.text = Formatter.DoubleToString(entity.impuesto3, entity.monedaSimbolo)

        lbwImpuestoA.text = entity.nombreimpuesto1
        lbwImpuestoB.text = entity.nombreimpuesto2
        lbwImpuestoC.text = entity.nombreimpuesto3

        tvwClient.text = "${entity.clienteCodigo} ${entity.clienteNombres}"
        //validarFirma()
    }

    private fun performAfterOperations(receiptEntity: ReceiptEntity?, numeroDocumento: String) {
        if (receiptEntity != null) {
            //PDF
            //saveAndShareFile(receiptEntity.pdfBytes, numeroDocumento)

            //Normal
            if (performPrinting(receiptEntity.documentoPrint)) {
                //print qr
                if(performPrintingQr(receiptEntity.qrbase64)) {
                    startActivity(Intent(this, SaleActivity::class.java))
                }else{
                    Log.e(TAG, "Error al imprimir QR")
                    onError("Error al imprimir QR")
                }

            }else {
                Log.e(TAG, "Error al imprimir documento")
                onError("Error al imprimir documento")
            }
        }
    }
    private fun performPrintingOrShare(documentoPrint: String,tipo: Int): Boolean {
        return performPrintingCotizacion(documentoPrint,tipo)
        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }

    private fun performPrintingOrShareQR(documentoPrint: String): Boolean {
        return performPrintingQr(documentoPrint)
        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }
    private fun performAfterOperationsCoti(receiptEntity: CotizacionPrint) {
        if (receiptEntity != null) {
            //PDF
            //saveAndShareFile(receiptEntity.pdfBytes, numeroDocumento)

            //Normal
            /*if (performPrintingOrShare(receiptEntity.cotiCabecera, 1)) {
                if (performPrintingOrShareQR(receiptEntity.cotiBarraNumero)) {
                    if (performPrintingOrShare(receiptEntity.cotiCuerpo,1)) {
                        if (performPrintingOrShareQR(receiptEntity.cotiBarraCliente)) {
                            if (performPrintingOrShare(receiptEntity.cotiPie,0)) {
                                startActivity(Intent(this, SaleActivity::class.java))
                            }
                        }
                    }
                }
            }*/
            if (performPrintingCotizacionNormal(receiptEntity)){

                startActivity(Intent(this, SaleActivity::class.java))
            }
        }
    }
    private fun onError(message: String) {
               Log.e(SaleActivity.TAG, message)

        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
            .setCancelable(false)
            .create().show()
    }
}