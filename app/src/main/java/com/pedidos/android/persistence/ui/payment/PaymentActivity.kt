package com.pedidos.android.persistence.ui.payment

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.design.widget.TextInputEditText
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import com.google.zxing.integration.android.IntentIntegrator
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.db.entity.*
import com.pedidos.android.persistence.model.CreditCard
import com.pedidos.android.persistence.model.SelectedCreditCard
import com.pedidos.android.persistence.model.SelectedOtherPayment
import com.pedidos.android.persistence.model.TipoDocumento
import com.pedidos.android.persistence.model.pagos.PagoValeDataResponse
import com.pedidos.android.persistence.model.pagos.PagoValeRequest
import com.pedidos.android.persistence.model.pagos.PagoValeResponse
import com.pedidos.android.persistence.ui.BasicApp
import com.pedidos.android.persistence.ui.ClientPopUpFragment
import com.pedidos.android.persistence.ui.menu.MenuActivity
import com.pedidos.android.persistence.ui.sale.SaleActivity
import com.pedidos.android.persistence.ui.sale.SaleActivity.Companion
import com.pedidos.android.persistence.ui.search.SearchProductActivity
import com.pedidos.android.persistence.utils.Defaults
import com.pedidos.android.persistence.utils.Formatter
import com.pedidos.android.persistence.viewmodel.EndingViewModel
import com.pedidos.android.persistence.viewmodel.PaymentViewModel
import kotlinx.android.synthetic.main.activity_complementary_product.*
import kotlinx.android.synthetic.main.nav_header_menu.view.*
import kotlinx.android.synthetic.main.payment_activity.*
import kotlinx.android.synthetic.main.payment_activity.fltLoading
import kotlinx.android.synthetic.main.payment_credict_cards_selectec_dialog.view.*
import kotlinx.android.synthetic.main.payment_make_and_wish_dialog.view.*
import kotlinx.android.synthetic.main.payment_make_and_wish_dialog.view.edtAmount
import kotlinx.android.synthetic.main.payment_make_and_wish_dialog.view.tvwAccept
import kotlinx.android.synthetic.main.sales_activity.*
import kotlinx.android.synthetic.main.search_imei_dialog.view.*
import pe.beyond.visanet.manager.MPOSError
import pe.beyond.visanet.manager.MPOSManagerSession
import pe.beyond.visanet.manager.MPOSResponseBean
import pe.beyond.visanet.manager.listener.MPOSAuthorizationListener
import java.util.*
import kotlin.collections.ArrayList


class PaymentActivity : MenuActivity() {
    companion object {
        val TAG = PaymentEntity::class.java.simpleName!!
        const val ENTITY_EXTRA = "com.example.android.persistence.ui.payment.entity"
    }

    private lateinit var viewModel: PaymentViewModel
    private lateinit var endingViewModel: EndingViewModel
    private var idOrderPagoLink : String = ""
    private var idOrderFpay : String = ""
    private val codeFpayResult  : Int = 101
    private val codePagoLinkResult : Int  = 102
    private var creditCardSelected: String = ""
    private var otherPaymentSelected : String = ""
    private lateinit var numeroDocumento: String
    private var isMposVISA: Boolean = true
    private lateinit var manager: MPOSManagerSession
    private lateinit var saleEntity: SaleEntity
    private var montoReference: String = ""
    private var dialog: AlertDialog? = null
    private var view: View? = null
    private var numVale: String = ""
    private var isSaleSucceses: Boolean = false
    private var refTarje: String = ""
    lateinit var editEfectivo: TextInputEditText
    lateinit var etwPagoFalabellaTienda : TextInputEditText
    lateinit var etwPagoFalabellaCaja : TextInputEditText
    lateinit var etwPagoFalabellaTransaccion: TextInputEditText
    lateinit var etwPagoFalabellaTicket  : TextInputEditText
    private var isEnablePagoFalabella: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithMenu(R.layout.payment_activity)
        setSupportActionBarMenu(tvApp)

        checkSession()
        llFlight.visibility = if (getSession().dutyfree == 1) View.VISIBLE else View.GONE

        saleEntity = intent.getParcelableExtra(ENTITY_EXTRA) as SaleEntity? ?: SaleEntity()

        numeroDocumento = saleEntity.documento
        tvwTotalVenta.text = Formatter.DoubleToString(saleEntity.total, saleEntity.monedaSimbolo)

        etwMpos.text = Editable.Factory.getInstance().newEditable("")
        editEfectivo = findViewById(R.id.etwEfectivo)
        //etwFpay.text = Editable.Factory.getInstance().newEditable(saleEntity.total.toString())
        //etwPlink.text = Editable.Factory.getInstance().newEditable(saleEntity.total.toString())
        etwPagoFalabellaTransaccion = findViewById(R.id.etwPagoFalabellaTransaccion)
        etwPagoFalabellaTienda = findViewById(R.id.etwPagoFalabellaTienda)
        etwPagoFalabellaCaja = findViewById(R.id.etwPagoFalabellaCaja)

        etwPagoFalabellaTicket = findViewById(R.id.etwPagoFalabellaTicket)
        etwPagoFalabellaTicket.isFocusable = true
        etwPagoFalabellaTicket.requestFocus()

        println("saleEntity.tipodocumentogenera: "+ saleEntity.tipodocumentogenera)
        when(saleEntity.tipodocumentogenera) {
            "TIK" -> {
                rbwTicket.isChecked = true
                rbwBoleta.isChecked = false
                rdwFactura.isChecked = false
            }
            "BOL" -> {
                rbwTicket.isChecked = false
                rbwBoleta.isChecked = true
                rdwFactura.isChecked = false
            }
            "FAC" -> {
                rbwTicket.isChecked = false
                rbwBoleta.isChecked = false
                rdwFactura.isChecked = true
            }
            else -> {
                rbwTicket.isChecked = false
                rbwBoleta.isChecked = true
                rdwFactura.isChecked = false
            }
        }
        // rdwFactura.isChecked = saleEntity.clienteTipoDocumento == TipoDocumento.RUC
        //rbwBoleta.isChecked = saleEntity.clienteTipoDocumento != TipoDocumento.RUC

        val factory = PaymentViewModel.Companion.Factory(application, getSettings().urlbase)
        viewModel = ViewModelProviders.of(this, factory)[PaymentViewModel::class.java]
        viewModel.showLoading.observe(this, Observer { showLoading(it!!) })
        viewModel.resultMessages.observe(this, Observer {
            println("ley: printOnSnackBar => ${it!!}")
            printOnSnackBar(it!!)
        })
        viewModel.liveData.observe(this, Observer { performAfterOperations(it) })

        val endingFactory = EndingViewModel.Companion.Factory(application, getSettings().urlbase)
        endingViewModel = ViewModelProviders.of(this, endingFactory)[EndingViewModel::class.java]
        endingViewModel.receiptLiveData.observe(this, Observer { performViewOperations(it) })
        endingViewModel.cardsAccepted.observe(this, Observer { setFirstCard(it ?: arrayListOf()) })
        endingViewModel.otherPayments.observe(this, Observer { setFirstOtherPayment(it ?: arrayListOf()) })
        swTipoTarjeta.onItemSelectedListener = onSpinerSelectedItem
        swTipoTarjeta.adapter = arrayAdapter()

        try {
            manager = MPOSManagerSession(this, BasicApp.URL_VISA, BasicApp.KEY_VISA)
            manager.setIsVoucherRequired(true)
        } catch (ex: Exception) {
            Log.d("VISANET-APP", ex.message, ex)
        }
        if (saleEntity.cotizacion.isEmpty()){
            btnTarjeta.setOnClickListener { btnOnClickCreditCard(endingViewModel.cardsAccepted.value ?: arrayListOf()) }
            btnOther.setOnClickListener { btnOnClickOthers(endingViewModel.otherPayments.value ?: arrayListOf()) }
            btnMakeAndWish.setOnClickListener { onClickMakeAndWish() }
            btnMpos.setOnClickListener { cobrarMPOS() }
            btnFpay.setOnClickListener { btnOnClickFpay() }
            btnPlink.setOnClickListener { btnOnClickPLink() }
            btnOtherVale.setOnClickListener{ btnOnClickVale()}
            btnMposMasterCard.setOnClickListener {
                isMposVISA = false
                printOnSnackBar("En construccion")
            }
        }




        // -- Agregado CPV
        btnRegresar.setOnClickListener { onBackPressed() }

        btnFinalizar.setOnClickListener {
            btnFinalizar.isEnabled = false
            btnRegresar.isEnabled = false
            if (isValidFlight()) {
                finalizarPedido(createPaymentEntity())
            }
        }
        etwPagoFalabellaTicket.isFocusable = true
        etwPagoFalabellaTicket.isFocusableInTouchMode = true


        btnFalabella.setOnClickListener {
            productSearch()
        }
        //etwPagoFalabellaTienda.isEnabled = false
        //etwPagoFalabellaCaja.isEnabled = false
        //etwPagoFalabellaTransaccion.isEnabled = false
        btnPagoFalabellaTicket.setOnClickListener {
            if(isEnablePagoFalabella){
                btnPagoFalabellaTicket.background = ContextCompat.getDrawable(this, R.mipmap.ic_arrow_abajo)
                isEnablePagoFalabella = false
                //etwPagoFalabellaTienda.isEnabled =false
                //etwPagoFalabellaCaja.isEnabled = false
                //etwPagoFalabellaTransaccion.isEnabled = false
                disableFalabellaPaymentFields()

            }else {
                btnPagoFalabellaTicket.background = ContextCompat.getDrawable(this, R.mipmap.ic_arrow_arriba)
                isEnablePagoFalabella = true
                etwPagoFalabellaTienda.visibility = View.VISIBLE
                etwPagoFalabellaCaja.visibility = View.VISIBLE
                etwPagoFalabellaTransaccion.visibility = View.VISIBLE

            }

        }
        setupVisualizacionTipoPago()
        disableFalabellaPaymentFields()
    }

    override fun onResume() {
        super.onResume()
        println("onResume")
        etwPagoFalabellaTicket.isFocusable = true
        //etwPagoFalabellaTicket.isFocusableInTouchMode = true
        etwPagoFalabellaTicket.requestFocus()
        //disableFalabellaPaymentFields()
    }
    override fun onStart() {
        super.onStart()
        println("onStart")
        etwPagoFalabellaTicket.isFocusable = true
        etwPagoFalabellaTicket.requestFocus()
    }
    override fun onPause() {
        super.onPause()
        println("onStart")
        etwPagoFalabellaTicket.isFocusable = true
        etwPagoFalabellaTicket.requestFocus()
    }

    private fun disableFalabellaPaymentFields() {
        println("ley: disableFalabellaPaymentFields")
        etwPagoFalabellaTienda.visibility = View.GONE
        etwPagoFalabellaCaja.visibility = View.GONE
        etwPagoFalabellaTransaccion.visibility = View.GONE
       // isEnablePagoFalabella = false
    }

    private fun setupVisualizacionTipoPago() {
        val userInfo = getSession()
        if(saleEntity.cotizacion.isNotEmpty()){
            btnPagoFalabellaTicket.visibility = isVisbleView(userInfo.pagoFalabella)
            etwPagoFalabellaTicket.visibility = isVisbleView(userInfo.pagoFalabella)

            etwPagoFalabellaTicket.text = Editable.Factory.getInstance().newEditable("")

            etwFalabellaImporte.setText(saleEntity.total.toString())
            val rootLayout = findViewById<ViewGroup>(R.id.payment_activity_root)
            toggleButtons(rootLayout, false)
            etwPagoFalabellaCotizacion.text = "Cotización: ${saleEntity.cotizacion}"
            editEfectivo.isEnabled = false
            editEfectivo.isFocusable = false
            etwInputEfectivo.isEnabled = false
            textPagoFalabella.visibility = isVisbleView(userInfo.pagoFalabella)
            linerPagoFalabella.visibility = isVisbleView(userInfo.pagoFalabella)
            etwPagoFalabellaCotizacion.visibility = isVisbleView(userInfo.pagoFalabella)
            etwPagoFalabellaTienda.isEnabled = false
            //etwPagoFalabellaCaja.isEnabled = false
            //etwPagoFalabellaTransaccion.isEnabled = false
            disableFalabellaPaymentFields()
            etwInputPagoFalabellaTicket.isEnabled = false
            etwPagoFalabellaTicket.isFocusable = true
            //etwPagoFalabellaTicket.requestFocus()
            // Forzar el teclado
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

            // Usar un Handler con retraso
            Handler(Looper.getMainLooper()).postDelayed({
                etwPagoFalabellaTicket.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etwPagoFalabellaTicket, InputMethodManager.SHOW_IMPLICIT)
            }, 300)

        }else {
            etwFalabellaImporte.setText("")
            val rootLayout = findViewById<ViewGroup>(R.id.payment_activity_root)
            toggleButtons(rootLayout, true)
            editEfectivo.isEnabled = true
            editEfectivo.isFocusable = true
            etwInputEfectivo.isEnabled = true
            btnPagoFalabellaTicket.visibility = isVisbleView(false)
            etwPagoFalabellaTicket.visibility = isVisbleView(false)
            textPagoFalabella.visibility = isVisbleView(false)
            linerPagoFalabella.visibility = isVisbleView(false)
            etwPagoFalabellaTienda.visibility = isVisbleView(false)
            etwPagoFalabellaCaja.visibility = isVisbleView(false)
            etwPagoFalabellaTransaccion.visibility = isVisbleView(false)
            etwPagoFalabellaCotizacion.visibility = isVisbleView(false)
        }
        linerEfectivo.visibility = isVisbleView(userInfo.efectivo)
        linerCobranzaEfectivo.visibility = isVisbleView(userInfo.efectivo)
        linerFpay.visibility = isVisbleView(userInfo.fpay)
        textFpay.visibility = isVisbleView(userInfo.fpay)
        textFpay.visibility = isVisbleView(userInfo.fpay)
        linerPagoLink.visibility = isVisbleView(userInfo.pagoLink)
        textPagoLink.visibility = isVisbleView(userInfo.pagoLink)
        linerTarjeta.visibility = isVisbleView(userInfo.tarjeta)
        linerMakeAndWish.visibility = isVisbleView(userInfo.makeaWish)
        linerVales.visibility = isVisbleView(userInfo.vales)
        linerOtosPagos.visibility = isVisbleView(userInfo.otroPago)
        linerMpos.visibility = isVisbleView(userInfo.mPos)
        textMpos.visibility = isVisbleView(userInfo.mPos)


    }


    private fun isVisbleView(statusView: Boolean) : Int {
        return if(statusView) View.VISIBLE else View.GONE
    }

    private fun btnOnClickVale() {
        view = LayoutInflater.from(this)
            .inflate(R.layout.search_imei_dialog, lltRoot, false)
        dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .setTitle("VALE/GIFTCARD")
            .show()

        view?.btnScan?.setOnClickListener {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            integrator.setPrompt("ESCANEAR IMEI")
            integrator.setOrientationLocked(false)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.setRequestCode(113)
            integrator.initiateScan()
        }
        view?.tvwAccept?.setOnClickListener {

            // checkResult(productEntity)
            //addItem(productEntity)
            validarPagoVale(view?.edtImei?.text.toString())
            dialog?.dismiss()
            //searchViewModel.checkAutomaticallyGuide(productEntity)
            // addItem(productEntity)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        println("result_bar: "+data?.dataString)
        println("resultCode: "+resultCode)
        println("requestCode: "+requestCode)
        println("result_bar_code: "+data?.getParcelableExtra(SearchProductActivity.PRODUCT_KEY))

        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        } else if( requestCode == codePagoLinkResult && resultCode == RESULT_OK) {
            finalizarPedido(createPaymentEntity())
        } else if ( requestCode == codeFpayResult && resultCode == RESULT_OK) {
            if (isValidFlight()) {
                finalizarPedido(createPaymentEntity())
            }
        } else if (requestCode == 113){
            validarPagoVale(resultCode.toString())
        }else if (requestCode == 114) {
            if (data != null) {
                val result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data)
                if (result != null) {
                    if (result.contents == null) {

                        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                            .setTitle(R.string.app_name)
                            .setMessage("Lectura cancelada")
                            .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                            .setCancelable(false)
                            .create().show()
                    } else {
                        etwPagoFalabellaTicket.setText(result.contents)


                    }
                } else {
                    Log.d(SaleActivity.TAG, "result returned null")
                }
            } else {
                Log.d(SaleActivity.TAG, "data returned null")
            }
        }

        if (::manager.isInitialized)
            manager.parseResult(requestCode, resultCode, data)
    }

    private fun finalizarPedido(paymentEntity: PaymentEntity) {
        viewModel.savePayment(paymentEntity, ::onError)
    }

    // Agregado CPV
    private fun onError(message: String) {
        Log.e(SaleActivity.TAG, message)
        btnFinalizar.isEnabled = true
        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                .setCancelable(false)
                .create().show()
    }


    private fun validarPagoVale(codigo: String){
        etwOtherVale.setText("")
        var pagoValeRequest = PagoValeRequest()
        pagoValeRequest.usuario =getSession().usuario
        pagoValeRequest.fecha = Formatter.DateToString(Date())
        pagoValeRequest.tienda = getSession().tienda
        pagoValeRequest.vale = codigo
        viewModel.pagoVale(pagoValeRequest,::setImportePagoVale, ::onError)

    }
    private fun setImportePagoVale(pagoValeResponse: PagoValeResponse){
        var pagoValeDataResponseData = PagoValeDataResponse()
        pagoValeDataResponseData = pagoValeResponse.data!!
        if (pagoValeResponse.result == true){
            numVale = pagoValeDataResponseData.vale.toString()
            etwOtherVale.setText(pagoValeDataResponseData.importe.toString())
        }

    }

    private fun createPaymentEntity(): PaymentEntity {
        val paymentEntity = PaymentEntity()
        paymentEntity.tipoDocumento = if (rbwBoleta.isChecked) Defaults.BOLETA else Defaults.FACTURA
        paymentEntity.numeroDocumento = numeroDocumento
        if (editEfectivo.text.toString() != "") {
            paymentEntity.montoEfectivo = editEfectivo.text.toString().toDouble()
        }

        paymentEntity.codigoTarjeta = creditCardSelected
        if (etwTarjeta.text.toString() != "") {
            paymentEntity.montoTarjeta = etwTarjeta.text.toString().toDouble()
            paymentEntity.retarj = refTarje
        }
        paymentEntity.retarj = refTarje
        paymentEntity.codigoOtro = otherPaymentSelected
        if ( etwOther.text.toString() != "") {
            paymentEntity.montoOtro = etwOther.text.toString().toDouble()
            paymentEntity.numotro = montoReference
        }

        paymentEntity.mposAmount = if (TextUtils.isEmpty(etwMpos.text.toString())) 0.0 else etwMpos.text.toString().toDouble()
        paymentEntity.montoMakeAndWish = if (TextUtils.isEmpty(etwMakeAndWish.text.toString())) 0.0 else etwMakeAndWish.text.toString().toDouble()
        paymentEntity.mpos = if (isMposVISA) "1" else "2"
        paymentEntity.flight = tvwFlight.text.toString()
        paymentEntity.impago_fpay = if (TextUtils.isEmpty(etwFpay.text.toString())) 0.0 else etwFpay.text.toString().toDouble()
        paymentEntity.impago_link = if (TextUtils.isEmpty(etwPlink.text.toString())) 0.0 else etwPlink.text.toString().toDouble()
        paymentEntity.idpago_fpay = getPagoIdFpay()
        paymentEntity.idpago_link = getPagoIdPlink()
        paymentEntity.numvale = numVale
        paymentEntity.impvale = if (TextUtils.isEmpty(etwOtherVale.text.toString())) 0.0 else etwOtherVale.text.toString().toDouble()
        paymentEntity.pagofalabellaImporte = if (TextUtils.isEmpty(etwFalabellaImporte.text.toString())) 0.0 else etwFalabellaImporte.text.toString().toDouble()
        paymentEntity.pagofalabellaTienda = etwPagoFalabellaTienda.text.toString()
        paymentEntity.pagofalabellaCaja = etwPagoFalabellaCaja.text.toString()
        paymentEntity.pagofalabellaTransaccion = etwPagoFalabellaTransaccion.text.toString()
        paymentEntity.pagofalabellaTicket = etwPagoFalabellaTicket.text.toString()
        return paymentEntity
    }

    private fun cobrarMPOS() {
        if (!isValidFlight()) {
            return
        }

        isMposVISA = true
        val amount = if (TextUtils.isEmpty(etwMpos.text.toString())) 0f else etwMpos.text.toString().toFloat()

        if (amount == 0f) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Ingresa un monto valido")
                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .create().show()
            return
        }

        manager.setIsVoucherRequired(true)
        manager.authorize(this, amount, null, null, object : MPOSAuthorizationListener {
            override fun mPOSAuthorizationError(mposError: MPOSError) {
                if (mposError.errorCode != 5) { // TODO MAPEAR CODIGO DE CANCELACION
                    AlertDialog.Builder(this@PaymentActivity)
                            .setTitle(R.string.app_name)
                            .setMessage("Error de autorizacion: ${mposError.getMessage()}")
                            .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                            .setCancelable(false)
                            .create().show()
                }
            }

            override fun mPOSAuthorization(mposResponse: MPOSResponseBean) {
                //if (mposResponse.isResponseSuccessful()) { // TODO Si todo esta bien continua con el flujo

                val paymentEntity = createPaymentEntity()

                paymentEntity.mposAmount = amount.toDouble()
                paymentEntity.mposTransaction = mposResponse.toString() //transactionId; traceNumber

                finalizarPedido(paymentEntity)
                //}
            }
        })
    }

    private fun showLoading(show: Boolean) {
        //fltLoading.visibility =
            if (show) showProgressBar() else hideProgressBar()
    }

    private val onSpinerSelectedItem = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {
            Log.e(TAG, "tarjeta no Seleccionada")
        }

        override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
            creditCardSelected = CreditCard.getValByPosition(position).toString()
        }
    }

    private fun arrayAdapter(): ArrayAdapter<String> {
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, CreditCard.getAll().values.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        return adapter
    }

    private fun performAfterOperations(entity: PaymentResponseEntity?) {
        if (entity != null) {
            //Get PDF
            //endingViewModel.getSaleReceiptPDF(numeroDocumento)
            savePagoIdFpay("")
            savePagoIdPLink("")
            if (performPrintingOrShare(entity.documentoPrint)) {
                if(performPrintingOrShareQR(entity.qrPrint)) {
                    if(performPrintingOrShare(entity.piedocumentoPrint)) {
                        confirmResultMessage(entity.serviceResultMessage, onOk = {
                            //try to print, if its not stop process
                            if (entity.voucherMposPrint.trim().isNotEmpty()) {
                                performPrintingOrShare(entity.voucherMposPrint)
                            }

                            it.dismiss()
                            startNewSale()
                        })
                    }else{
                        Log.e(TAG, "Error al imprimir el pie del recibo")
                        AlertDialog.Builder(this)
                                .setTitle(R.string.app_name)
                                .setMessage("Error al imprimir el pie del recibo")
                                .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                                .setCancelable(false)
                                .create().show()
                    }
                }else {
                    Log.e(TAG, "Error al imprimir el QR del recibo")
                    AlertDialog.Builder(this)
                            .setTitle(R.string.app_name)
                            .setMessage("Error al imprimir el QR del recibo")
                            .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                            .setCancelable(false)
                            .create().show()
                }
            } else {
                confirmResultMessage(entity.serviceResultMessage, onOk = {
                    it.dismiss()
                    startNewSale()
                })
            }
        }else {
            Log.e(TAG, "Error al obtener el recibo")
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Error al obtener el recibo")
                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .create().show()
        }
    }

    private fun performViewOperations(receipt: ReceiptEntity?) {
        if (receipt != null) {
            saveAndShareFile(receipt.pdfBytes, numeroDocumento)
        }
    }

    private fun confirmResultMessage(message: String, onOk: (alert: DialogInterface) -> Unit) {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { it, _ ->
                    onOk(it)
                }.show()
    }

    private fun startNewSale() {
        finish()
        startActivity(Intent(this, SaleActivity::class.java))
    }

    private fun performPrintingOrShare(documentoPrint: String): Boolean {
        return performPrinting(documentoPrint)
        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }

    private fun performPrintingOrShareQR(documentoPrint: String): Boolean {
        return performPrintingQr(documentoPrint)
        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }

    private fun isValidFlight(): Boolean {
        //if pide vuelo y es vacio _ false
        //if pide vuelo y esta lleno ->true
        //if NO pide vuelo siempre true
        if (getSession().dutyfree == 1 && TextUtils.isEmpty(tvwFlight.text.toString())) {
            AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Ingrese el VUELO")
                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .create().show()

            return false
        }

        return true
    }

    private fun onClickMakeAndWish() {
        val view = LayoutInflater.from(this).inflate(R.layout.payment_make_and_wish_dialog, payment_activity_root, false)
        val dialog = AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(false)
                .show()

        view.btn010.setOnClickListener{
            view.edtAmount.text =  Editable.Factory.getInstance().newEditable("0.10")
            // dialog.dismiss()
        }
        view.btn020.setOnClickListener{
            view.edtAmount.text =  Editable.Factory.getInstance().newEditable("0.20")
            // dialog.dismiss()
        }
        view.btn050.setOnClickListener{
            view.edtAmount.text =  Editable.Factory.getInstance().newEditable("0.50")
            // dialog.dismiss()
        }
        view.btn1sol.setOnClickListener{
            view.edtAmount.text =  Editable.Factory.getInstance().newEditable("1.00")
            // dialog.dismiss()
        }

        view.tvwAccept.setOnClickListener {
            val makeAndWishAmountString = view.edtAmount.text.toString()
            var defaultAmountValue = ""

            if (makeAndWishAmountString.isNotEmpty()) {
                val makeAndWishAmount = makeAndWishAmountString.toDouble()
                defaultAmountValue = Formatter.DoubleToString(makeAndWishAmount)
                val newAmount = saleEntity.total + makeAndWishAmount
                updateAmounts(newAmount)
                layoutTotalCobrar.visibility = View.VISIBLE
            } else {
                layoutTotalCobrar.visibility = View.GONE
            }

            etwMakeAndWish.text = Editable.Factory.getInstance().newEditable(defaultAmountValue)

            dialog.dismiss()
        }
    }

    private fun setFirstCard(list : ArrayList<SelectedCreditCard>) {
        if(list.size > 0) {
            creditCardSelected = list[0].codeCard.toString()
            btnTarjeta.text = list[0].description
            btnTarjeta.setCompoundDrawablesWithIntrinsicBounds(getDrawable(list[0].getImageResource(list[0].icon)),null,null,null)
        }
    }

    private fun setFirstOtherPayment(list : ArrayList<SelectedOtherPayment>) {
        if(list.size > 0) {
            otherPaymentSelected = list[0].codeOther.toString()
            btnOther.text = list[0].description
            btnOther.setCompoundDrawablesWithIntrinsicBounds(getDrawable(list[0].getImageResource(list[0].icon)),null,null,null)
        }
    }

    // for credit cards
    private fun btnOnClickCreditCard(list : ArrayList<SelectedCreditCard>) {
        list.forEach { it.isSelected = false }
        list[0].isSelected = true
        var appCardList = list
        val adapter = CardsAdapter(appCardList)

        adapter.listener = { it ->
            appCardList.forEach { card ->
                card.isSelected = false
            }
            appCardList[it].isSelected = true
            adapter.updateList(appCardList)
        }

        val view = LayoutInflater.from(this).inflate(R.layout.payment_credict_cards_selectec_dialog,payment_activity_root,false)
        view.rwCards.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .show()

        view.tvwAccept.setOnClickListener {
            refTarje = view.edtAmountOther.text.toString()
            if(view.edtAmount.text.toString().isNullOrEmpty() || view.edtAmount.text.toString() == "0"  || view.edtAmount.text.toString() == "0.0" ) {
                etwTarjeta.setText("")

                creditCardSelected = list[0].codeCard.toString()
                btnTarjeta.text = list[0].description
                btnTarjeta.setCompoundDrawablesWithIntrinsicBounds(getDrawable(list[0].getImageResource(list[0].icon)),null,null,null)
                dialog.dismiss()
            } else {
                etwTarjeta.setText(view.edtAmount.text.toString())
                appCardList.forEach {
                    if(it.isSelected) {

                        creditCardSelected = it.codeCard.toString()
                        btnTarjeta.text = it.description
                        btnTarjeta.setCompoundDrawablesWithIntrinsicBounds(getDrawable(it.getImageResource(it.icon)),null,null,null)
                    }
                }
                dialog.dismiss()
            }
        }
    }

    // for other payments
    private fun btnOnClickOthers(list : ArrayList<SelectedOtherPayment>) {
        list.forEach { it.isSelected = false }
        list[0].isSelected = true
        var appCardList = list
        val adapter = OtherPaymentAdapter(appCardList)

        adapter.listener = { it ->
            appCardList.forEach { card ->
                card.isSelected = false
            }
            appCardList[it].isSelected = true
            adapter.updateList(appCardList)
        }

        val view = LayoutInflater.from(this).inflate(R.layout.payment_credict_cards_selectec_dialog,payment_activity_root,false)
        view.rwCards.adapter = adapter
        view.textDialogSelect.text = getString(R.string.select_payment_other)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .show()

        view.tvwAccept.setOnClickListener {
            if(view.edtAmount.text.toString().isNullOrEmpty() || view.edtAmount.text.toString() == "0"  || view.edtAmount.text.toString() == "0.0" ) {
                etwOther.setText("")

                otherPaymentSelected = list[0].codeOther.toString()
                btnOther.text = list[0].description
                btnOther.setCompoundDrawablesWithIntrinsicBounds(getDrawable(list[0].getImageResource(list[0].icon)),null,null,null)
                dialog.dismiss()
            } else {
                etwOther.setText(view.edtAmount.text.toString())
                montoReference = view.edtAmountOther.text.toString()
                appCardList.forEach {
                    if(it.isSelected) {

                        otherPaymentSelected = it.codeOther.toString()
                        btnOther.text = it.description
                        btnOther.setCompoundDrawablesWithIntrinsicBounds(getDrawable(it.getImageResource(it.icon)),null,null,null)
                    }
                }
                dialog.dismiss()
            }
        }
    }

    // for pagolink, fpay

    private fun btnOnClickFpay() {
        val amount = saleEntity.total.toFloat()//if (TextUtils.isEmpty(etwPlink.text.toString())) 0f else etwPlink.text.toString().toFloat()
        if (TextUtils.isEmpty(etwFpay.text.toString())) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage("Ingresa un monto valido")
                .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                .setCancelable(false)
                .create().show()
            return
        }
        showClientPopUp{
            if(saleEntity.email.isNotEmpty() && saleEntity.clienteCodigo.isNotEmpty()) {
                 try {
                    viewModel.saveSale(saleEntity,::gotoFpay,::onError)

                } catch (e : Throwable ) {
                    Log.d("aca",e.message.toString())
                }
            }else {
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Ingrese un cliente valido")
                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .create().show()
            }
        }

    }

    private fun gotoFpay(entity : SaleEntity) {
        val amount = etwFpay.text.toString().toFloat()
        val intent = Intent(this, FpayActivity::class.java)
        val currentRequest = PaymentIntentionsEntity(saleEntity.tienda,amount,saleEntity.email,pedido = saleEntity.documento)
        intent.putExtra(PagoLinkActivity.ENTITY_EXTRA,currentRequest)
        startActivityForResult(intent,codeFpayResult)

    }

    private fun gotoPagoLink(entity : SaleEntity) {
        val amount = etwPlink.text.toString().toFloat()
        val intent = Intent(this, PagoLinkActivity::class.java)
        val currentRequest = PaymentIntentionsEntity(saleEntity.tienda,amount,saleEntity.email, pedido = saleEntity.documento)
        intent.putExtra(PagoLinkActivity.ENTITY_EXTRA,currentRequest)
        startActivityForResult(intent,codePagoLinkResult)
    }

    private fun btnOnClickPLink() {
        //val amount = etwPlink.text.toString().toFloat()//if (TextUtils.isEmpty(etwPlink.text.toString())) 0f else etwPlink.text.toString().toFloat()
        if (TextUtils.isEmpty(etwPlink.text.toString()) ) {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage("Ingresa un monto valido")
                .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                .setCancelable(false)
                .create().show()
            return
        }

        showClientPopUp {
            if(saleEntity.email.isNotEmpty() && saleEntity.clienteCodigo.isNotEmpty()) {
                viewModel.saveSale(saleEntity,::gotoPagoLink,::onError)

            } else {
                AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage("Ingrese un cliente valido")
                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .create().show()
            }
        }

    }
    //Update amounts in every input that has value
    private fun updateAmounts(newTotal: Double) {
        tvwTotalCobrar.text = Formatter.DoubleToString(newTotal, saleEntity.monedaSimbolo)
        if (!TextUtils.isEmpty(etwPlink.text.toString())) {
            etwPlink.text = Editable.Factory.getInstance().newEditable(Formatter.DoubleToString(newTotal))
        }

        if (!TextUtils.isEmpty(etwMpos.text.toString())) {
            etwMpos.text = Editable.Factory.getInstance().newEditable(Formatter.DoubleToString(newTotal))
        }
    }


    private fun getPagoIdPlink() : String {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var  id = sharedPreferences.getString("id_plink","") ?: ""
        //savePagoIdPLink("")
        return id
    }

    private fun savePagoIdPLink(id : String) {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putString("id_plink",id)
        editor.apply()
    }

    private fun getPagoIdFpay() : String {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        var id = sharedPreferences.getString("id_fpay","") ?: ""
        //savePagoIdFpay("")
        return id
    }

    private fun savePagoIdFpay(id : String) {
        var sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putString("id_fpay",id)
        editor.apply()
    }

    private fun showClientPopUp(callback : (() -> Unit)) {
        val ft = supportFragmentManager.beginTransaction()

        val prev = supportFragmentManager.findFragmentByTag("dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        val codigoCliente = saleEntity.clienteCodigo
        val data = saleEntity
        val listTipoDocumento = viewModel.listTipoDocumento
        val mapTipoDocumento = LinkedHashMap<Int, String>()
        for (list in listTipoDocumento.value!!){
            mapTipoDocumento[list.codigo]= list.description
        }
        val popUpFragment = ClientPopUpFragment.createFragment(codigoCliente,
            data!!.clienteTipoDocumento,
            getSettings().urlbase,
            getSession(),
            onSelectClient = { client ->
                run {
                    saleEntity.clienteTipoDocumento = client.identityDocumentType
                    saleEntity.clienteCodigo = client.documentNumber
                    saleEntity.clienteNombres = client.fullName
                    saleEntity.telefono = client.phone
                    saleEntity.email = client.email
                    callback.invoke()
                }
            },mapTipoDocumento)

        popUpFragment.show(ft, "ClientPopup")
    }
    private fun toggleButtons(root: ViewGroup, isEnabled: Boolean) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            when (child) {
                is Button -> child.isEnabled = isEnabled
                is EditText -> child.isEnabled = isEnabled
                is ViewGroup -> toggleButtons(child, isEnabled) // Recursión para layouts anidados
            }
        }
    }

    private fun showProgressBar() {
        val fltLoading = findViewById<View>(R.id.fltLoading)
        fltLoading.visibility = View.VISIBLE

        val rootLayout = findViewById<ViewGroup>(R.id.payment_activity_root)
        toggleButtons(rootLayout, false) // Bloquea botones
    }

    private fun hideProgressBar() {
        val fltLoading = findViewById<View>(R.id.fltLoading)
        fltLoading.visibility = View.GONE

        val rootLayout = findViewById<ViewGroup>(R.id.payment_activity_root)
        toggleButtons(rootLayout, true) // Habilita botones
    }

    private fun productSearch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                showMessageForCamPermission()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), SearchProductActivity.CAMERA_REQUEST_ID)
            }
        } else {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
            integrator.setPrompt("Escanear producto")
            integrator.setOrientationLocked(false)
            integrator.setBeepEnabled(true)
            integrator.setBarcodeImageEnabled(true)
            integrator.setRequestCode(114)
            integrator.initiateScan()
        }
    }
    private fun showMessageForCamPermission(messageType: Boolean = false) {
        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
            .setTitle(R.string.app_name)
            .setMessage(if (messageType) R.string.cam_permission_request_message_dont_show else R.string.cam_permission_request_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (!messageType)
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), SearchProductActivity.CAMERA_REQUEST_ID)
            }.show()
    }


}
