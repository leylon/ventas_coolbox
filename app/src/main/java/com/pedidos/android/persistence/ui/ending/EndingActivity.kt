package com.pedidos.android.persistence.ui.ending

import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.db.entity.ReceiptEntity
import com.pedidos.android.persistence.db.entity.SaleEntity
import com.pedidos.android.persistence.db.entity.SaleSubItemEntity
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrint
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrintRequest
import com.pedidos.android.persistence.ui.menu.MenuActivity
import com.pedidos.android.persistence.ui.payment.PaymentActivity
import com.pedidos.android.persistence.ui.sale.SaleActivity
import com.pedidos.android.persistence.ui.sale.SaleActivity.Companion
import com.pedidos.android.persistence.utils.Formatter
import com.pedidos.android.persistence.viewmodel.EndingViewModel
import kotlinx.android.synthetic.main.ending_activity.*
import kotlinx.android.synthetic.main.ending_activity.fltLoading
import kotlinx.android.synthetic.main.ending_activity.toolbar
import kotlinx.android.synthetic.main.sales_activity.btnProcess
import kotlinx.android.synthetic.main.sales_activity.etwAddProduct
import kotlinx.android.synthetic.main.sales_activity.imbwAddProductManualOnly
import kotlinx.android.synthetic.main.sales_activity.imbwAddProductoWithCamera


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
        viewModel.saveCotizacionLiveData.observe(this, Observer { it ->
            if (it != null && it.success) {

                Log.d(TAG, "Cotizacion guardada correctamente")
                onError(it.message.toString())
                obtenerCotizacion("")
            } else {
                Log.e(TAG, "")
                onError(it?.message.toString())
            }
        })
        viewModel.saleLiveData.postValue(saleEntity)
        btnCobrar.setOnClickListener { cobrarPedido() }
        btnEliminar.setOnClickListener { eliminarPedido() }
        btnImprimir.setOnClickListener { obtenerPedido() }
        btnCotizacion.setOnClickListener { obtenerCotizacion("1") }
        btnSaveCotizacion.setOnClickListener { savePedido() }
        btnRegresar.setOnClickListener { onBackPressed() }
        btnVisa.setOnClickListener { cobrarPedido() }
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
        viewModel.saveCotizacion(CotizacionPrintRequest(tipo = "PED",
            documento = viewModel.saleLiveData.value!!.documento,""))
        //PDF
        //viewModel.getSaleReceiptPDF(viewModel.saleLiveData.value!!.documento)
    }
    private fun obtenerCotizacion(numero : String){

        viewModel.getSaleReceiptPrintCotizacion(CotizacionPrintRequest(tipo = "PED",
            documento = viewModel.saleLiveData.value!!.documento,
            numero = numero))
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
    private fun performPrintingOrShare(documentoPrint: String): Boolean {
        return performPrintingCotizacion(documentoPrint)
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
            if (performPrintingOrShare(receiptEntity.cotiCabecera)) {
                if (performPrintingOrShareQR(receiptEntity.cotiBarraNumero)) {
                    if (performPrintingOrShare(receiptEntity.cotiCuerpo)) {
                        if (performPrintingOrShareQR(receiptEntity.cotiBarraCliente)) {
                            if (performPrintingOrShare(receiptEntity.cotiPie)) {
                                startActivity(Intent(this, SaleActivity::class.java))
                            }
                        }
                    }
                }
            }
            /*if (performPrintingCotizacionNormal(receiptEntity)){
                startActivity(Intent(this, SaleActivity::class.java))
            }*/
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