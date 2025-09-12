package com.pedidos.android.persistence.ui.sale

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.jakewharton.rxbinding.widget.RxTextView
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.db.entity.ProductEntity
import com.pedidos.android.persistence.db.entity.SaleEntity
import com.pedidos.android.persistence.db.entity.SaleSubItemEntity
import com.pedidos.android.persistence.model.SaleSubItem
import com.pedidos.android.persistence.model.cotizacion.CotizacionCab
import com.pedidos.android.persistence.model.cotizacion.CotizacionDet
import com.pedidos.android.persistence.model.cotizacion.CotizacionRequest
import com.pedidos.android.persistence.model.cotizacion.Presupuesto
import com.pedidos.android.persistence.model.guide.DataResponse
import com.pedidos.android.persistence.model.sale.*
import com.pedidos.android.persistence.ui.BasicApp
import com.pedidos.android.persistence.ui.ClientPopUpFragment
import com.pedidos.android.persistence.ui.ending.EndingActivity
import com.pedidos.android.persistence.ui.guide.fragment.CityPopUpFragment
import com.pedidos.android.persistence.ui.menu.MenuActivity
import com.pedidos.android.persistence.ui.sale.fragment.CotizacionPopUpFragment
import com.pedidos.android.persistence.ui.sale.fragment.QuestionPopUpFragment
import com.pedidos.android.persistence.ui.sale.fragment.SendCodPopUpFragment
import com.pedidos.android.persistence.ui.search.SearchProductActivity
import com.pedidos.android.persistence.utils.Defaults
import com.pedidos.android.persistence.utils.Formatter
import com.pedidos.android.persistence.utils.complementProductTempCode
import com.pedidos.android.persistence.viewmodel.SaleViewModel
import com.pedidos.android.persistence.viewmodel.SearchProductViewModel
import kotlinx.android.synthetic.main.sales_activity.*
import kotlinx.android.synthetic.main.search_imei_dialog.view.*
import rx.android.schedulers.AndroidSchedulers
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class SaleActivity : MenuActivity(),
    QuestionPopUpFragment.newDialoglistenerQuestion,
    SendCodPopUpFragment.newDialoglistenerSendCod,
CotizacionPopUpFragment.newDialoglistenerCotizacion {

    private lateinit var saleViewModel: SaleViewModel
    private lateinit var searchViewModel: SearchProductViewModel
    private lateinit var saleAdapter: SaleAdapter
    lateinit var ventaProductoResponse: VentaProductoResponse
    lateinit var envioCorreoResponse: EnvioCorreoResponse
    private var listProdu = ArrayList<VentaProductoRequest>()
    private var dialog: AlertDialog? = null
    private var view: View? = null
    lateinit var  dialgCustomSenCod: SendCodPopUpFragment
    var flag_pop: Boolean = false
    var flag_cotizacion: Boolean = false
    var listSaleSubItem: MutableList<SaleSubItem> = mutableListOf()
    var flagAddCoti = false
    var imeiCotizacion = ""
    var detalllesCotizacion: CotizacionDet? = null
    var listDetalletCotizacion: MutableList<CotizacionDet> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithMenu(R.layout.sales_activity)
        setSupportActionBarMenu(toolbar)
        println("onCreate")
        checkSession()

        rvwProducts.layoutManager = LinearLayoutManager(this)
        saleAdapter = SaleAdapter(mutableListOf()) { saleSubItem ->
            saleViewModel.deleteItem(saleSubItem)
        }
        saleAdapter.compProductActionCall = {complementProductTempCode = null}
        toolbar.title = "${getString(R.string.title_sale_tienda)} ${getSession().tienda}"
        textVersion.text = """Version : ${getSession().version}"""
        rvwProducts.adapter = saleAdapter
        btnProcess.setOnClickListener {
            btnProcess.isEnabled =false
            rvwProducts.isEnabled = false
            validaProssesSale()} //processSale() }
        imbwAddProductCombined.setOnClickListener { productSearchCombined() }
        imbwAddProductoWithCamera.setOnClickListener {
            flag_pop = false
            flag_cotizacion = false
            tvwCotizacion.text = ""
            productSearch() }
        imbwAddProductManualOnly.setOnClickListener { productManualSearch() }
        btnSelectClient.setOnClickListener { showClientPopUp() }
        if (getSession().pagoFalabella){
            chkGenerateCotization.visibility = View.VISIBLE
            btnSelectCotization.visibility = View.VISIBLE
        }else {
            chkGenerateCotization.visibility = View.GONE
            btnSelectCotization.visibility = View.GONE
        }
        btnSelectCotization.setOnClickListener {
            listSaleSubItem = mutableListOf()
            initSale()
            showCotizacionConfirm("show",null)
        }
        //this init the viewModel
        val saleFactory = SaleViewModel.Companion.Factory(application, getSettings().urlbase)
        val searchFactory = SearchProductViewModel.Companion.Factory(application, getSettings().urlbase)
        saleViewModel = ViewModelProviders.of(this, saleFactory).get(SaleViewModel::class.java)
        searchViewModel = ViewModelProviders.of(this, searchFactory).get(SearchProductViewModel::class.java)
        searchViewModel.searchResults.observe(this, Observer {
            checkResult(it)
        })
        searchViewModel.errorResults.observe(this, Observer { showError(it) })
        searchViewModel.searchResultsCotizacion.observe(this, Observer {
            checkResult(it,detalllesCotizacion!!)
        })
        subscribeToModel(saleViewModel)
        //etwAddProduct.focusable = View.FOCUSABLE

        etwAddProduct.isFocusable = true
        etwAddProduct.requestFocus()
        RxTextView.textChanges(etwAddProduct)
                .filter { it.length > 2 }
                .debounce(600, TimeUnit.MILLISECONDS)
                //.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    productSearchCombined()
                }

        initSale()
    }

    override fun onStop() {
        super.onStop()
        println("onStop")
    }

    override fun onResume() {
        super.onResume()
        println("onResume")
        btnProcess.isEnabled = true
        rvwProducts.isEnabled = true
    }

    override fun onStart() {
        super.onStart()
        println("onStart")
        etwAddProduct.requestFocus()
        //btnProcess.isEnabled = true
    }

    override fun onBackPressed() {
    }

    private fun showError(it: String?) {
        showProgress(false)
        if (it != null)
            printOnSnackBar(it)
    }

    private fun checkResult(productEntity: ProductEntity?) {
        if (productEntity != null) {
            if (productEntity.stimei) {
                //request IMEI
                if (TextUtils.isEmpty(productEntity.imei)) {

                    val dialogView = LayoutInflater.from(this).inflate(R.layout.search_imei_dialog, lltRoot, false)
                    dialogView?.textTitle?.setText("${productEntity.codigo}: ${productEntity.descripcion}")
                    val mydialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)
                            .setTitle(R.string.propt_imei_title)
                            .show()

                    dialogView?.btnScan?.setOnClickListener {
                        val integrator = IntentIntegrator(this)
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                        integrator.setPrompt("ESCANEAR IMEI")
                        integrator.setOrientationLocked(false)
                        integrator.setBeepEnabled(true)
                        integrator.setBarcodeImageEnabled(true)
                        integrator.setRequestCode(113)
                        integrator.initiateScan()
                    }
                    dialogView?.tvwAccept?.setOnClickListener {
                        println("ley: IMEI: "+ mydialog?.isShowing)
                        mydialog?.dismiss()
                        fltLoading.visibility = View.VISIBLE
                        if (!TextUtils.isEmpty(dialogView?.edtImei?.text)) {
                            productEntity.imei = dialogView.edtImei?.text.toString()

                        }else {
                            productEntity.imei = view?.edtImei?.text.toString()
                            //onErrorImei("IMEI VACIO", productEntity)
                        }
                        checkResult(productEntity)
                    }
                    dialogView?.tvwCancelar?.setOnClickListener {
                        showProgress(false)
                        mydialog?.dismiss()
                    }
                } else {
                    searchViewModel.checkAutomatically(productEntity,::onErrorImei)
                }
            } else if (productEntity.stimei2) {
                if (TextUtils.isEmpty(productEntity.imei2)) {
                    //request imei2
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.search_imei_dialog, lltRoot, false)
                    val mydialog2 = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .setCancelable(false)
                            .setTitle(R.string.propt_imei_title2)
                            .show()

                    dialogView?.btnScan?.setOnClickListener {
                        val integrator = IntentIntegrator(this)
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                        integrator.setPrompt("Escanear Imei 2")
                        integrator.setOrientationLocked(false)
                        integrator.setBeepEnabled(true)
                        integrator.setBarcodeImageEnabled(true)
                        integrator.setRequestCode(115)
                        integrator.initiateScan()
                    }
                    dialogView?.tvwAccept?.setOnClickListener {
                        fltLoading.visibility = View.VISIBLE
                        if (!dialogView?.edtImei?.text.toString().isEmpty()) {
                            productEntity.imei2 = dialogView?.edtImei?.text.toString()
                            mydialog2?.dismiss()
                        }else {
                            productEntity.imei2 = view?.edtImei?.text.toString()
                        }
                        checkResult(productEntity)
                    }
                    dialogView?.tvwCancelar?.setOnClickListener {
                        showProgress(false)
                        mydialog2?.dismiss()
                    }
                } else {
                    if (!flag_cotizacion) {
                        addItem(productEntity)

                    } else {
                        if (isImeiRepeated(productEntity.imei)){
                            onErrorImei("Imei ${productEntity.imei} se encuentra registrado", productEntity)
                        }else {
                            addItem(productEntity,detalllesCotizacion!!)
                        }


                    }

                }
            } else {
                // get product from search edittext
                complementProductTempCode = productEntity.codigo
                if (!flag_cotizacion) {

                }else {

                }
                addItem(productEntity)

            }
        } else {
            showProgress(false)
            val intent = Intent(this, SearchProductActivity::class.java)
            intent.putExtra("STATUS_COTIZACION",if(chkGenerateCotization.isChecked) 1 else 0)
            startActivityForResult(intent, SEARCH_REQUEST)
        }

    }
    private fun checkResult(productEntity: ProductEntity?,detalles: CotizacionDet) {
        detalllesCotizacion = detalles
        if (productEntity != null) {
            if (productEntity.stimei) {
                //request IMEI
                if (TextUtils.isEmpty(productEntity.imei)) {

                    val dialogView = LayoutInflater.from(this).inflate(R.layout.search_imei_dialog, lltRoot, false)
                    dialogView?.textTitle?.setText("${productEntity.codigo}: ${productEntity.descripcion}")
                    val mydialog = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .setTitle(R.string.propt_imei_title)
                        .show()

                    dialogView?.btnScan?.setOnClickListener {
                        val integrator = IntentIntegrator(this)
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                        integrator.setPrompt("ESCANEAR IMEI")
                        integrator.setOrientationLocked(false)
                        integrator.setBeepEnabled(true)
                        integrator.setBarcodeImageEnabled(true)
                        integrator.setRequestCode(113)
                        integrator.initiateScan()
                    }
                    dialogView?.tvwAccept?.setOnClickListener {
                        println("ley: IMEI: "+ mydialog?.isShowing)
                        mydialog?.dismiss()
                        fltLoading.visibility = View.VISIBLE
                        if (!TextUtils.isEmpty(dialogView?.edtImei?.text)) {
                            productEntity.imei = dialogView.edtImei?.text.toString()

                        }else {
                            productEntity.imei = view?.edtImei?.text.toString()
                            //onErrorImei("IMEI VACIO", productEntity)
                        }
                        if (!flag_cotizacion){
                            checkResult(productEntity)
                        }else {
                            checkResult(productEntity,detalles)
                        }

                    }
                    dialogView?.tvwCancelar?.setOnClickListener {
                        showProgress(false)
                        mydialog?.dismiss()
                        if (flag_cotizacion) {
                            finish()
                            startActivity(Intent(this, SaleActivity::class.java))
                        }
                    }
                } else {
                    if (!flag_cotizacion) {
                        searchViewModel.checkAutomatically(productEntity,::onErrorImei)
                    } else {
                            searchViewModel.checkAutomatically(productEntity,::onErrorImei,detalles)
                //        searchViewModel.checkAutomatically(productEntity,::onErrorImei,detalles)
                    }

                }
            } else if (productEntity.stimei2) {
                if (TextUtils.isEmpty(productEntity.imei2)) {
                    //request imei2
                    val dialogView = LayoutInflater.from(this).inflate(R.layout.search_imei_dialog, lltRoot, false)
                    val mydialog2 = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .setTitle(R.string.propt_imei_title2)
                        .show()

                    dialogView?.btnScan?.setOnClickListener {
                        val integrator = IntentIntegrator(this)
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                        integrator.setPrompt("Escanear Imei 2")
                        integrator.setOrientationLocked(false)
                        integrator.setBeepEnabled(true)
                        integrator.setBarcodeImageEnabled(true)
                        integrator.setRequestCode(115)
                        integrator.initiateScan()
                    }
                    dialogView?.tvwAccept?.setOnClickListener {
                        fltLoading.visibility = View.VISIBLE
                        if (!dialogView?.edtImei?.text.toString().isEmpty()) {
                            productEntity.imei2 = dialogView?.edtImei?.text.toString()
                            mydialog2?.dismiss()
                        }else {
                            productEntity.imei2 = view?.edtImei?.text.toString()
                        }
                        if (!flag_cotizacion){
                            checkResult(productEntity)
                        }else {
                            checkResult(productEntity,detalles)
                        }

                    }
                    dialogView?.tvwCancelar?.setOnClickListener {
                        showProgress(false)
                        mydialog2?.dismiss()
                    }
                } else {
                    if (!flag_cotizacion) {
                        addItem(productEntity)
                    }else {
                        if (isImeiRepeated(productEntity.imei)){
                            onErrorImei("Imei ${productEntity.imei} se encuentra registrado", productEntity)
                        }else {
                            addItem(productEntity,detalles)
                        }

                    }

                }
            } else {
                // get product from search edittext
                complementProductTempCode = productEntity.codigo

                if (!flag_cotizacion) {
                    addItem(productEntity)
                }else {
                    if (isImeiRepeated(productEntity.imei)){
                        onErrorImei("Imei ${productEntity.imei} se encuentra registrado", productEntity)
                    }else {
                        addItem(productEntity,detalles)
                    }

                }

            }
        } else {
            showProgress(false)
            val intent = Intent(this, SearchProductActivity::class.java)
            intent.putExtra("STATUS_COTIZACION",if(chkGenerateCotization.isChecked) 1 else 0)
            startActivityForResult(intent, SEARCH_REQUEST)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        btnProcess.isEnabled = true
        println("result_bar: "+data?.dataString)
        when (requestCode) {
            SEARCH_REQUEST -> {
                println("SEARCH_REQUEST: "+data?.dataString)
                if (Activity.RESULT_OK == resultCode) {
                    val productEntity: ProductEntity? = data?.getParcelableExtra(SearchProductActivity.PRODUCT_KEY)
                    if(productEntity != null) addItem(productEntity)
                }
            }

            GARANTIE_REQUEST -> {
                println("GARANTIE_REQUEST: "+data?.dataString)
                if (Activity.RESULT_OK == resultCode) {
                    val productEntityExt: ProductEntity? = data?.getParcelableExtra(SearchProductActivity.PRODUCT_KEY_EXT)
                    val productEntityDamage: ProductEntity? = data?.getParcelableExtra(SearchProductActivity.PRODUCT_KEY_DAMAGE)

                    if(productEntityExt != null) addItem(productEntityExt)
                    if(productEntityDamage != null)addItem(productEntityDamage)
                }
            }

            COMPLEMENTARY_REQUEST -> {
                if( Activity.RESULT_OK == resultCode) {
                    var sizeList = data!!.getIntExtra(SearchProductActivity.SIZE_PRODUCTS, 0)

                    for (index in 0 .. (sizeList - 1)) {
                        val productEntity: ProductEntity? = data?.getParcelableExtra(SearchProductActivity.PRODUCT_KEY+index)
                        if(productEntity != null) addItem(productEntity)
                    }
                }
            }
            113 -> {
                if (data != null) {
                    val result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data)
                    if (result != null) {
                        if (result.contents == null) {
                            showProgress(false)

                            AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                                    .setTitle(R.string.app_name)
                                    .setMessage("Lectura cancelada")
                                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                                    .setCancelable(false)
                                    .create().show()
                        } else {
                            showProgress(true)
                            searchViewModel.searchResults.value?.imei = result.contents
                            checkResult(searchViewModel.searchResults.value)
                        }
                    } else {
                        showProgress(false)
                        Log.d(TAG, "result returned null")
                    }
                } else {
                    showProgress(false)
                    Log.d(TAG, "data returned null")
                }
            }
            115 -> {
                if (data != null) {
                    val result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data)
                    if (result != null) {
                        if (result.contents == null) {
                            showProgress(false)

                            AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                                    .setTitle(R.string.app_name)
                                    .setMessage("Lectura cancelada")
                                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                                    .setCancelable(false)
                                    .create().show()
                        } else {
                            showProgress(false)
                            searchViewModel.searchResults.value?.imei2 = result.contents
                            checkResult(searchViewModel.searchResults.value)
                        }
                    } else {
                        showProgress(false)
                        Log.d(TAG, "result returned null")
                    }
                } else {
                    showProgress(false)
                    Log.d(TAG, "data returned null")
                }
            }
            114 -> {
                if (data != null) {
                    val result = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, resultCode, data)
                    if (result != null) {
                        if (result.contents == null) {
                            showProgress(false)
                            AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                                    .setTitle(R.string.app_name)
                                    .setMessage("Lectura cancelada")
                                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                                    .setCancelable(false)
                                    .create().show()
                        } else {
                            showProgress(true)

                            if (flag_pop){
                                sendBarCodManual(result.contents ?: "")
                            }else {
                                if (flag_cotizacion){
                                    findCotizacion(result.contents ?: "")
                                }else{
                                    searchViewModel.searchProductDirectly(result.contents ?: "",if(chkGenerateCotization.isChecked) 1 else 0,detalllesCotizacion!!,::checkResult)
                                }

                            }
                        }
                    } else {
                        showProgress(false)
                        Log.d(TAG, "result returned null")
                    }
                } else {
                    showProgress(false)
                    Log.d(TAG, "data returned null")
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun addItem(productEntity: ProductEntity) {
       chkGenerateCotization.isEnabled = false
       chkGenerateCotization.isClickable = false
        val datos = saleViewModel.saleLiveData.value
        datos?.statusCotizacion = if (chkGenerateCotization.isChecked) 1 else 0
        saleViewModel.saleLiveData.postValue(datos)
        if (!flag_cotizacion) {
            var lastSecuencialOrDefault = 0
            if (saleViewModel.saleLiveData.value != null) {
                if (saleViewModel.saleLiveData.value!!.productos.size > 0)
                    lastSecuencialOrDefault = saleViewModel.saleLiveData.value!!.productos[saleViewModel.saleLiveData.value!!.productos.size - 1].secuencial
            }

            val saleSubItem = SaleSubItemEntity().apply {
                secuencial = lastSecuencialOrDefault + 1
                codigoventa = productEntity.codigoVenta
                codigoProducto = productEntity.codigo
                descripcion = productEntity.descripcion
                cantidad = 1
                precio = productEntity.precio
                imei = productEntity.imei
                imei2 = productEntity.imei2
                monedaSimbolo = productEntity.monedaSimbolo
                complementaryRowColor = productEntity.complementaryRowColor
                secgaraexte = productEntity.secgaraexte
                codgaraexte = productEntity.codgaraexte
            }

            showProgress(true)
            saleViewModel.saveDetail(saleSubItem)
        }else {
            imeiCotizacion = productEntity.imei
            val saleSubItem = SaleSubItemEntity().apply {
                secuencial = detalllesCotizacion?.numLin.toString().toInt()
                codigoventa = detalllesCotizacion?.sku.toString()
                codigoProducto = detalllesCotizacion?.sku.toString()
                descripcion = detalllesCotizacion?.descripcion.toString()
                cantidad = detalllesCotizacion?.unidades.toString().toInt()
                precio = detalllesCotizacion?.precioIva.toString().toDouble()
                pcdcto = detalllesCotizacion?.dto.toString().toDouble()
                ean = detalllesCotizacion?.ean.toString()
                imei = imeiCotizacion
                monedaSimbolo = ""
                complementaryRowColor = ""
                totaldetalle = detalllesCotizacion?.total.toString().toDouble()


            }
            listSaleSubItem.add(saleSubItem)
             if (listSaleSubItem.size == listDetalletCotizacion.size){
                saleViewModel.saveDetail(listSaleSubItem)
            }
        }

    }
    fun isImeiRepeated(imei: String): Boolean {
        return imei.isNotEmpty() && listSaleSubItem.any { it.imei == imei }
    }
    private fun addItem(productEntity: ProductEntity,detallesCotizacion: CotizacionDet) {
        chkGenerateCotization.isEnabled = false
        chkGenerateCotization.isClickable = false
        val datos = saleViewModel.saleLiveData.value
        datos?.statusCotizacion = if (chkGenerateCotization.isChecked) 1 else 0
        saleViewModel.saleLiveData.postValue(datos)
        if (!flag_cotizacion) {
            var lastSecuencialOrDefault = 0
            if (saleViewModel.saleLiveData.value != null) {
                if (saleViewModel.saleLiveData.value!!.productos.size > 0)
                    lastSecuencialOrDefault = saleViewModel.saleLiveData.value!!.productos[saleViewModel.saleLiveData.value!!.productos.size - 1].secuencial
            }

            val saleSubItem = SaleSubItemEntity().apply {
                secuencial = lastSecuencialOrDefault + 1
                codigoventa = productEntity.codigoVenta
                codigoProducto = productEntity.codigo
                descripcion = productEntity.descripcion
                cantidad = 1
                precio = productEntity.precio
                imei = productEntity.imei
                imei2 = productEntity.imei2
                monedaSimbolo = productEntity.monedaSimbolo
                complementaryRowColor = productEntity.complementaryRowColor
                secgaraexte = productEntity.secgaraexte
                codgaraexte = productEntity.codgaraexte
            }

            showProgress(true)
            saleViewModel.saveDetail(saleSubItem)
        }else {
            imeiCotizacion = productEntity.imei
            val saleSubItem = SaleSubItemEntity().apply {
                secuencial = detalllesCotizacion?.numLin.toString().toInt()
                codigoventa = detalllesCotizacion?.sku.toString()
                codigoProducto = detalllesCotizacion?.sku.toString()
                descripcion = detalllesCotizacion?.descripcion.toString()
                cantidad = detalllesCotizacion?.unidades.toString().toInt()
                precio = detalllesCotizacion?.precioIva.toString().toDouble()
                pcdcto = detalllesCotizacion?.dto.toString().toDouble()
                ean = detalllesCotizacion?.ean.toString()
                imei = imeiCotizacion
                monedaSimbolo = ""
                complementaryRowColor = ""
                totaldetalle = detalllesCotizacion?.total.toString().toDouble()


            }
            listSaleSubItem.add(saleSubItem)
            println("listSaleSubItem: ${listSaleSubItem.size} - listDetalletCotizacion: ${listDetalletCotizacion.size}")

            if (listSaleSubItem.size == listDetalletCotizacion.size){
                saleViewModel.saveDetail(listSaleSubItem)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.isEmpty()) return
        when (requestCode) {
            SearchProductActivity.CAMERA_REQUEST_ID -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    productSearch()
                } else {
                    showMessageForCamPermission(true)
                }
            }
        }
    }

    private fun subscribeToModel(viewModel: SaleViewModel) {
        viewModel.saleLiveData.observe(this, Observer { newItem ->
            showProgress(false)
            if (newItem != null) {
                //(rvwProducts.adapter as SaleAdapter).items.removeAll { true }
                (rvwProducts.adapter as SaleAdapter).clearItems()
                (rvwProducts.adapter as SaleAdapter).addItems(newItem.productos)
                println("subscribeToModel: ${newItem.productos.size}")
                if (newItem.productos.size > 0) {
                    chkGenerateCotization.isEnabled = false
                    chkGenerateCotization.isClickable = false
                }else {
                    chkGenerateCotization.isEnabled = true
                    chkGenerateCotization.isClickable = true
                }
                updateScreen(newItem)
            }
        })

        viewModel.showProgress.observe(this, Observer {
            showProgress(it!!)
        })

        viewModel.message.observe(this, Observer {
            if (it != null) {
                printOnSnackBar(it)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateScreen(entity: SaleEntity) {
        println("updateScreen")
        tvwOrderNumber.text = entity.documento
        tvwOrderDate.text = entity.fecha //format
        tvwSaleTotal.text = Formatter.DoubleToString(entity.total, entity.monedaSimbolo)
        tvwClient.text = "${entity.clienteCodigo} ${entity.clienteNombres}"
        etwAddProduct.requestFocus()
        etwAddProduct.text = Editable.Factory.getInstance().newEditable("")
    }

    private fun processSale() {
        if (saleViewModel.saleLiveData.value!!.productos.size == 0) {
            AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                    .setTitle(R.string.app_name)
                    .setMessage(getString(R.string.sale_validation_no_products))
                    .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                    .setCancelable(false)
                    .create().show()
            return
        }

        showProgress(true)
        saleViewModel.saveSale(::goToResumenPedido, ::onError)
    }
    fun validaProssesSale() {
        //btnProcess.isEnabled =false
        val userInfo = getSession()
        val listProducts = saleAdapter.items
        if (saleViewModel.saleLiveData.value!!.productos.size == 0) {
            AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.sale_validation_no_products))
                .setPositiveButton(R.string.aceptar) { d, _ ->
                    btnProcess.isEnabled = true
                    showProgress(false)
                    d.dismiss()

                }
                .setCancelable(false)
                .create().show()
            //btnProcess.isEnabled = true
            return
        }
        /*
        saleAdapter.items.forEach {
            data ->
            val dataRequest  = VentaProductoRequest(
                coEmpr = userInfo.empresa,
                nuSecu = data.secuencial,
                coItem = data.codigoProducto,
                coVent = data.codigoventa,
                deItem = data.descripcion,
                caDocu = data.cantidad,
                prVent =  data.precio

            )
        }*/
         listProdu = ArrayList<VentaProductoRequest>()
        saleAdapter.items.forEach { data ->
            listProdu.add(
                VentaProductoRequest(
                    coEmpr = userInfo.empresa,
                    nuSecu = data.secuencial,
                    coItem = data.codigoProducto,
                    coVent = data.codigoventa,
                    deItem = data.descripcion,
                    caDocu = data.cantidad,
                    prVent = data.precio

                )
            )
        }
        /*listProdu.add(VentaProductoRequest(
            coEmpr = userInfo.empresa,
            nuSecu = 1,
            coItem = "CE104MOT81",
            coVent = "ABC",
            deItem = "ABC",
            caDocu = 1,
            prVent =  9.9
        ))*/
        saleViewModel.ventaProducto(listProdu,::showQuestionsConfirm, ::onError)
        //showQuestionsConfirm()
    }

    fun showQuestionsConfirm(response: VentaProductoResponse){
        //btnProcess.isEnabled = true
        showProgress(false)
        if(response.muestramensaje){
            val dialgCustom = QuestionPopUpFragment()
            dialgCustom.show(supportFragmentManager, "P")
            val args = Bundle()
            args.putString("OptionQuestion","Question")
            args.putSerializable("DataList", response as Serializable)
            dialgCustom.arguments = args
            val fragment = supportFragmentManager.findFragmentByTag("P")
            if (fragment != null) {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        }else {
            processSale()
        }
    }

    fun showCotizacionConfirm(option: String, dataPresupuesto: List<CotizacionCab>?){
        //btnProcess.isEnabled = true
        showProgress(false)
       // if(response.muestramensaje){
            val dialgCustom = CotizacionPopUpFragment()
            dialgCustom.show(supportFragmentManager, "P")
            val args = Bundle()
            args.putString("OptionQuestion",option)
           //
           if (option.equals("search")){
               args.putSerializable("DataList", dataPresupuesto as Serializable)
           }
        //
            dialgCustom.arguments = args
            val fragment = supportFragmentManager.findFragmentByTag("P")
            if (fragment != null) {
                supportFragmentManager.beginTransaction().remove(fragment).commit()
            }
        //}else {
          ///  processSale()
        //}
    }

    fun showSendCodConfirm(optionData: VentaProductoResponse){
        ventaProductoResponse = optionData
        showProgress(false)
        dialgCustomSenCod = SendCodPopUpFragment()
        dialgCustomSenCod.isCancelable = false
        dialgCustomSenCod.show(supportFragmentManager, "P")
        val args = Bundle()
        args.putString("OptionQuestion","SendCod")
        args.putSerializable("DataList", ventaProductoResponse as Serializable)
        dialgCustomSenCod.arguments = args
        val fragment = supportFragmentManager.findFragmentByTag("P")
        if (fragment != null) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
        }
    }

    private fun goToResumenPedido(entity: SaleEntity) {
        val androidID: String =
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        //prevent nulls after back pressed
        //prevent nulls after back pressed
        saleViewModel.saleLiveData.postValue(setSessionInfo(saleViewModel.saleLiveData.value!!))
        val currentSaleEntity : SaleEntity?  = saleViewModel.saleLiveData.value
        if (currentSaleEntity != null) {
            //update fields
            currentSaleEntity.subTotal = entity.subTotal
            currentSaleEntity.descuento = entity.descuento
            currentSaleEntity.impuesto = entity.impuesto
            currentSaleEntity.impuesto2 = entity.impuesto2
            currentSaleEntity.impuesto3 = entity.impuesto3
           // currentSaleEntity.cotizacion = entity.cotizacion
            currentSaleEntity.nombreimpuesto1 = entity.nombreimpuesto1
            currentSaleEntity.nombreimpuesto2 = entity.nombreimpuesto2
            currentSaleEntity.nombreimpuesto3 = entity.nombreimpuesto3

            currentSaleEntity.total = entity.total

            currentSaleEntity.evento = entity.evento
            currentSaleEntity.productoconcomplemento = entity.productoconcomplemento
            currentSaleEntity.telefono = entity.telefono
            currentSaleEntity.email = saleViewModel.saleLiveData?.value?.email ?: ""
            currentSaleEntity.tipodocumentogenera = entity.tipodocumentogenera
           // currentSaleEntity.androidimei = "a9731e8ca60a4207"
        }
        //end nulls prevent

        startActivity(Intent(this, EndingActivity::class.java).apply {
            //btnProcess.isEnabled = true
            putExtra(EndingActivity.EXTRA_ENTITY, currentSaleEntity)
        })
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

    private fun productSearchCombined() {
        flagAddCoti = false
        val productCode = etwAddProduct.text.toString()
        if (!productCode.isNullOrEmpty()) {
            showProgress(true)

            searchViewModel.searchProductDirectly(productCode,if(chkGenerateCotization.isChecked) 1 else 0)
        }
    }
    private fun productSearchCombined(datoCodigo: String,detalles: CotizacionDet) {
        val productCode = datoCodigo
        flagAddCoti = true
        if (!productCode.isNullOrEmpty()) {
            showProgress(true)

            searchViewModel.searchProductDirectly(productCode,if(chkGenerateCotization.isChecked) 1 else 0, detalles,::checkResult)
        }
    }


    private fun productManualSearch() {
        val intent = Intent(this, SearchProductActivity::class.java)
        intent.putExtra("STATUS_COTIZACION",if(chkGenerateCotization.isChecked) 1 else 0)
        startActivityForResult(intent, SEARCH_REQUEST)
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

    private fun showClientPopUp() {
        val ft = supportFragmentManager.beginTransaction()

        val prev = supportFragmentManager.findFragmentByTag("dialog")
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)

        val codigoCliente = saleViewModel.saleLiveData.value!!.clienteCodigo
        val data = saleViewModel.saleLiveData.value
        val listTipoDocumento = saleViewModel.listTipoDocumento
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
                        data!!.clienteTipoDocumento = client.identityDocumentType
                        data.clienteCodigo = client.documentNumber
                        data.clienteNombres = client.fullName
                        data.telefono = client.phone
                        data.email = client.email
                        saleViewModel.saleLiveData.postValue(data)
                    }
                },mapTipoDocumento)

        popUpFragment.show(ft, "ClientPopup")
    }





    private fun onError(message: String) {
        btnProcess.isEnabled =true
        etwAddProduct.visibility = View.GONE
        imbwAddProductoWithCamera.visibility = View.GONE
        imbwAddProductManualOnly.visibility = View.GONE
        Log.e(TAG, message)

        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                .setCancelable(false)
                .create().show()
    }
    private fun onErrorImei(message: String, productEntity: ProductEntity) {
        Log.e(TAG, message)
        //dialog?.dismiss()
        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.aceptar) { d, _ ->
                d.dismiss()

                if (!flag_cotizacion){
                    checkResult(productEntity)
                }else{
                    productEntity.stimei = true
                    productEntity.imei = ""
                    checkResult(productEntity,detalllesCotizacion!!)
                }

            }
            .setCancelable(false)
            .create().show()
    }

    private fun showProgress(show: Boolean) {
        //fltLoading.visibility =
            if (show) showProgressBar() else hideProgressBar()
    }

    private fun initSale() {
        val newSaleEntity = SaleEntity()
        setSessionInfo(newSaleEntity)
        newSaleEntity.fecha = Formatter.DateToString(Date())
        newSaleEntity.clienteCodigo = Defaults.Cliente.documentNumber
        newSaleEntity.clienteNombres = Defaults.Cliente.fullName
        newSaleEntity.clienteTipoDocumento = Defaults.Cliente.identityDocumentType
        newSaleEntity.email = Defaults.Cliente.email
        newSaleEntity.telefono = Defaults.Cliente.phone
        newSaleEntity.papelSize = getSettings().pageSize
        saleViewModel.saleLiveData.postValue(newSaleEntity)
    }

    private fun setSessionInfo(saleEntity: SaleEntity): SaleEntity {
        val userInfo = getSession()
        println("urlCorreo: ${userInfo.urlcorreo}")
        println("urlCorreoRespuesta: ${userInfo.urlcorreorespuesta}")
        saleEntity.vendedorCodigo = userInfo.vendedorcodigo
        saleEntity.usuario = userInfo.usuario
        saleEntity.cajaCodigo = userInfo.cajacodigo
        saleEntity.tienda = userInfo.tienda
        saleEntity.androidimei = userInfo.imei
        saleEntity.papelSize = getSettings().pageSize
       // saleEntity.email = userInfo.email
        return saleEntity
    }

    companion object {
        private const val SEARCH_REQUEST = 1234
        const val COMPLEMENTARY_REQUEST = 543
        const val GARANTIE_REQUEST = 544
        val TAG = SaleActivity::class.java.simpleName!!
    }

    override fun closeDialogQuestion(data: String, optionData: VentaProductoResponse) {
        when(data){
            "SI" -> {
                showSendCodConfirm(optionData)
            }
            "NO" -> {
                println("se cancelo el dialog questions")
            }
        }

    }
    fun sendMail() {

        var request = EnvioCorreoRequest(
            coTien = getSession().tienda,
            deCodi = ventaProductoResponse.codigoautorizacion,
            feCodi = Formatter.DateToString(Date()),
            deMail = ventaProductoResponse.correo,
            tiDocuIden = Defaults.Cliente.identityDocumentType.toString(),
            nuDocuIden = Defaults.Cliente.documentNumber,
            noClie =  Defaults.Cliente.fullName,
            coVend = getSession().vendedorcodigo,
            noVend = getSession().vendedornombre,
            detalle = listProdu
        )
        println("request: ${Gson().toJson(request)}")

        saleViewModel.ventaProductoEnvioCorreo(getSession().urlcorreo,request,::responseSendMail,::error)
    }
    fun responseSendMail(dataResponse: EnvioCorreoResponse,flag: Boolean){
        envioCorreoResponse = dataResponse
        if (dataResponse.result){
            if (flag){
                println("esperando respuesta")
                saleViewModel.envioCodigoRespuesta(
                    getSession().urlcorreo,
                    EnvioCodigoRequest(
                        coEmpr = "01",
                        deCodi = ventaProductoResponse.codigoautorizacion
                    ), ::goToResumenValid ,::error)
                Thread.sleep(3000L)
            }else{
                //error("El usuario Aprobo el pedido!")
                Toast.makeText(this,"No esta aprobado!!",Toast.LENGTH_LONG).show()
                println("Termino")
            }

        }else {
           // error("No esta aprobado!!")
            println("No esta aprobado!!")
            Toast.makeText(this,"No esta aprobado!!",Toast.LENGTH_LONG).show()
        }
    }
    fun goToResumenValid(data: EnvioCodigoResponse){
        when(data.deResu){
            "" -> {
                responseSendMail(envioCorreoResponse,true)
            }
            "ACT" -> {
                responseSendMail(envioCorreoResponse,true)
            }
            "APR" -> {
                dialgCustomSenCod.dismiss()
                processSale()

               // responseSendMail(envioCorreoResponse,false)
            }
            "DSP" -> {
                dialgCustomSenCod.dismiss()
                //error("")
                Toast.makeText(this,"usuario DESAPROBO la autorización",Toast.LENGTH_LONG).show()
            }
            else -> {
                dialgCustomSenCod.dismiss()
                println("La respuesta no se reconoce.: ${data.deResu}")
            }
        }
        if(data.deResu.length == 0){
            responseSendMail(envioCorreoResponse,true)
        }

    }
    override fun closeDialogSendCod(data: String, optionData: String) {
        when (data) {
            "SEND_MAIL" -> {
                sendMail()
            }
            "VALID_COD" -> {
                sendCodManual(optionData)
            }
            "VALID_BAR_COD" -> {
                sendBarCodManual(optionData)
            }
            "CAM_COD" ->{
                flag_pop = true
                productSearch()
            }
        }

    }
    fun sendCodManual(codigo: String){
        var request = VentaProductoValidaCodigoRequest(
            coTien = getSession().tienda,
            deCodi = ventaProductoResponse.codigoautorizacion,
            coEmpr = "01",
            deCodiIngr = codigo,
            opcion = "VALIDA_AUTORIZACION"
        )
        println("request_sendCodManual: ${Gson().toJson(request)}")
        saleViewModel.ventaProductoValidaCodigo(request,::responseSendCod,::error)
    }
    fun sendBarCodManual(codigo: String){
        var request = VentaProductoValidaCodigoRequest(
            coTien = getSession().tienda,
            deCodi = ventaProductoResponse.codigoautorizacion,
            coEmpr = "01",
            deCodiIngr = codigo,
            opcion = "VALIDA_FOTOCHECK"
        )
        println("request_sendCodManual: ${Gson().toJson(request)}")
        saleViewModel.ventaProductoValidaCodigo(request,::responseSendCod,::error)
    }
    fun responseSendCod(data: VentaProductoValidaCodigoResponse){
        if (data.result){
            //error("¡El pedido esta aprobado!")
            dialgCustomSenCod.dismiss()
            Toast.makeText(this,"el pedido esta aprobado",Toast.LENGTH_LONG).show()

            //error("el pedido esta aprobado")
            processSale()
        }else {
            Toast.makeText(this,data.mensaje,Toast.LENGTH_LONG).show()
            dialgCustomSenCod.dismiss()
            //error(data.mensaje)

        }
    }
    private fun toggleButtons(root: ViewGroup, isEnabled: Boolean) {

        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            when (child) {
                is Button -> child.isEnabled = isEnabled
                is EditText -> child.isEnabled = isEnabled
                is RecyclerView -> child.isEnabled = isEnabled

                is ViewGroup -> toggleButtons(child, isEnabled) // Recursión para layouts anidados
            }
        }
    }

    private fun showProgressBar() {
        val fltLoading = findViewById<View>(R.id.fltLoading)
        fltLoading.visibility = View.VISIBLE

        val rootLayout = findViewById<ViewGroup>(R.id.lltRoot)
        toggleButtons(rootLayout, false) // Bloquea botones
        rvwProducts.isEnabled = false
        //btnProcess.isEnabled =false
    }

    private fun hideProgressBar() {
        val fltLoading = findViewById<View>(R.id.fltLoading)
        fltLoading.visibility = View.GONE

        val rootLayout = findViewById<ViewGroup>(R.id.lltRoot)
        toggleButtons(rootLayout, true) // Habilita botones
        rvwProducts.isEnabled = true
        //btnProcess.isEnabled =true
    }

    override fun closeDialogQuestion(datos: String, optionData: CotizacionCab?) {
        println("closeDialogQuestion: $datos")


        when (datos) {
            "search" -> {
                println("closeDialogQuestion-search: ${Gson().toJson(optionData)}")
                //saleViewModel.saveCotizacion(optionData, ::goToResumenPedido, ::onError)
            }
            "item" -> {

                println("closeDialogQuestion-item: ${Gson().toJson(optionData)}")
                addCotizacion(optionData!!)
                println("se cancelo el dialog questions")
            }
            "scanner" -> {
                flag_cotizacion = true
                productSearch()
            }
            else -> {
                /*saleViewModel.findCotizacion(CotizacionRequest(
                    fecha = "20250507",
                    docCliente = datos
                ), ::dataPresuesto, ::onError)*/
                findCotizacion(datos)
            }

        }
    }
    fun findCotizacion(codigo: String) {
        println("findCotizacion: $codigo")
        saleViewModel.findCotizacion(CotizacionRequest(
            fecha = Formatter.DateToString(Date()),
            docCliente = codigo
        ), ::dataPresuesto, ::onError)
    }
    fun addCotizacion(cotizacionCab: CotizacionCab) {
        flag_cotizacion = true
        etwAddProduct.visibility = View.GONE
        imbwAddProductoWithCamera.visibility = View.GONE
        imbwAddProductManualOnly.visibility = View.GONE

        println("addCotizacion: ${Gson().toJson(cotizacionCab)}")
        val data = saleViewModel.saleLiveData.value
        data?.cotizacion = "${cotizacionCab.serie}-${cotizacionCab.numero}"
        tvwCotizacion.text = data?.cotizacion
        saleViewModel.saleLiveData.postValue(data)
        cotizacionCab.detalles.forEach {
            if (it.unidades > 0) {
                listDetalletCotizacion.add(it)
            }

        }
        cotizacionCab.detalles.forEach {
            if (it.unidades > 0) {
                println("unidades: ${it.unidades}")
                detalllesCotizacion = it

                productSearchCombined(it.sku, it)
                println("detalle: ${Gson().toJson(it)}")
            }

/*            val saleSubItem = SaleSubItemEntity().apply {
                secuencial = it.numLin.toInt()
                codigoventa = it.sku
                codigoProducto = it.sku
                descripcion = it.descripcion
                cantidad = it.unidades
                precio = it.precioIva
                pcdcto = it.dto
                ean = it.ean.toString()
                imei = imeiCotizacion
                monedaSimbolo = ""
                complementaryRowColor = ""
                totaldetalle = it.total


            }
            listSaleSubItem.add(saleSubItem)
                */
        }

       // saleViewModel.saveDetail(listSaleSubItem)


    }


    fun dataPresuesto(data: Presupuesto) {
        println("dataPresuesto: ${Gson().toJson(data.presupuestos)}")
        val listaCotizacion: MutableList<CotizacionCab>? = data.presupuestos
        println("size dataPresupuesto: ${Gson().toJson(data.presupuestos?.size)}")
        println("listaCotizacion: ${Gson().toJson(listaCotizacion)}")
        showCotizacionConfirm("search", data.presupuestos)
    }

}



