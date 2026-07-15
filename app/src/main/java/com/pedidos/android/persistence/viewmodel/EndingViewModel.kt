package com.pedidos.android.persistence.viewmodel

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.util.Base64
import android.util.Log
import com.pedidos.android.persistence.api.CoolboxApi
import com.pedidos.android.persistence.db.entity.ReceiptEntity
import com.pedidos.android.persistence.db.entity.SaleEntity
import com.pedidos.android.persistence.model.ReceiptRequest
import com.pedidos.android.persistence.model.SelectedCreditCard
import com.pedidos.android.persistence.model.SelectedOtherPayment
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrint
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrintRequest
import com.pedidos.android.persistence.model.cotizacion.CotizacionRequest
import com.pedidos.android.persistence.model.firma.ActualizarFirmaRequest
import com.pedidos.android.persistence.model.firma.FirmaDataResponse
import com.pedidos.android.persistence.model.firma.FirmaRequest
import com.pedidos.android.persistence.model.firma.FirmaResponse
import com.pedidos.android.persistence.model.sale.ValidaCobraRequest
import com.pedidos.android.persistence.model.sale.ValidaCobraResponse
import com.pedidos.android.persistence.ui.BasicApp
import com.pedidos.android.persistence.utils.ApiWrapper
import com.pedidos.android.persistence.utils.Defaults
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class EndingViewModel(private val repository: CoolboxApi) : ViewModel() {
    companion object {
        val TAG = EndingViewModel::class.java.simpleName!!

        class Factory(application: Application, urlBase: String) : ViewModelProvider.NewInstanceFactory() {
            private var repository = (application as BasicApp).getApiRepository(urlBase)

            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EndingViewModel(repository) as T
            }
        }
    }

    var saleLiveData = MutableLiveData<SaleEntity>()
    var showProgress = MutableLiveData<Boolean>()
    var cardsAccepted = MutableLiveData<ArrayList<SelectedCreditCard>>()
    var otherPayments = MutableLiveData<ArrayList<SelectedOtherPayment>>()
    var receiptLiveData = MutableLiveData<ReceiptEntity>()
    var receiptQrLiveData = MutableLiveData<ReceiptEntity>()
    var errorMessages = MutableLiveData<String>()
    var receiptPrintCotizacionLiveData = MutableLiveData<CotizacionPrint>()
    var saveCotizacionLiveData = MutableLiveData<CotizacionPrint>()
    var validaFirma = MutableLiveData<FirmaResponse>()
    var actualizaFirma = MutableLiveData<FirmaDataResponse>()
    var message = MutableLiveData<String>()
    var validadCobra = MutableLiveData<ValidaCobraResponse>()
    init {
        val newSale = SaleEntity().apply {
            clienteCodigo = Defaults.Cliente.documentNumber
            clienteNombres = Defaults.Cliente.fullName
        }

        saleLiveData.postValue(newSale)
        getCardsAccepted()
        getOtherPayments()
    }


    private fun getCardsAccepted() {
       showProgress.postValue(true)
        repository.cardsAvailable().enqueue(object : Callback<ApiWrapper<ArrayList<SelectedCreditCard>>>{
            override fun onResponse(
                call: Call<ApiWrapper<ArrayList<SelectedCreditCard>>>,
                response: Response<ApiWrapper<ArrayList<SelectedCreditCard>>>
            ) {
                if(response.isSuccessful && response.body()!!.result) {
                    cardsAccepted.value = response.body()?.data ?: arrayListOf()
                }
                showProgress.postValue(false)
            }

            override fun onFailure(
                call: Call<ApiWrapper<ArrayList<SelectedCreditCard>>>,
                t: Throwable
            ) {
                Log.e(TAG, t.message.toString())
                showProgress.postValue(false)
            }

        })
    }

    private fun getOtherPayments() {
        showProgress.postValue(true)
        repository.otherPatments().enqueue(object : Callback<ApiWrapper<ArrayList<SelectedOtherPayment>>>{
            override fun onResponse(
                call: Call<ApiWrapper<ArrayList<SelectedOtherPayment>>>,
                response: Response<ApiWrapper<ArrayList<SelectedOtherPayment>>>
            ) {
                if(response.isSuccessful && response.body()!!.result) {
                    otherPayments.value = response.body()?.data ?: arrayListOf()
                }
                showProgress.postValue(false)
            }

            override fun onFailure(
                call: Call<ApiWrapper<ArrayList<SelectedOtherPayment>>>,
                t: Throwable
            ) {
                Log.e(TAG, t.message.toString())
                showProgress.postValue(false)
            }

        })
    }

     fun obtenerValidacionFirmas(firmaRequest: FirmaRequest) {
        showProgress.postValue(true)
        repository.consultaFirmaGex(firmaRequest).enqueue(object : Callback<FirmaResponse> {
            override fun onFailure(call: Call<FirmaResponse>, t: Throwable) {
                Log.e(TAG, t.message.toString())
                showProgress.postValue(false)
            }

            override fun onResponse(call: Call<FirmaResponse>, response: Response<FirmaResponse>) {
                if (response.isSuccessful && response.body()!!.result) {
                    validaFirma.postValue(response.body()!!)
                } else {
                    message.postValue(response.body()?.message ?: "Error al validar la firma")
                }
                showProgress.postValue(false)
            }

        })
    }
    fun actualizarFirmas(firmaRequest: ActualizarFirmaRequest) {
        showProgress.postValue(true)
        repository.actualizafirmagex(firmaRequest).enqueue(object : Callback<FirmaDataResponse> {
            override fun onFailure(call: Call<FirmaDataResponse>, t: Throwable) {
                Log.e(TAG, t.message.toString())
                showProgress.postValue(false)
            }

            override fun onResponse(call: Call<FirmaDataResponse>, response: Response<FirmaDataResponse>) {
                if (response.isSuccessful && response.body()!!.result) {
                    actualizaFirma.postValue(response.body()!!)
                } else {
                    message.postValue(response.body()?.message ?: "Error al validar la firma")
                }
                showProgress.postValue(false)
            }

        })
    }
    fun validaCobra(validaCobraRequest: ValidaCobraRequest) {
        showProgress.postValue(true)
        repository.validabotonCobrar(validaCobraRequest).enqueue(object : Callback<ValidaCobraResponse> {
            override fun onFailure(call: Call<ValidaCobraResponse>, t: Throwable) {
                Log.e(TAG, t.message.toString())
                showProgress.postValue(false)
            }

            override fun onResponse(call: Call<ValidaCobraResponse>, response: Response<ValidaCobraResponse>) {
                if (response.isSuccessful && response.body()!!.result) {
                    validadCobra.postValue(response.body()!!)
                } else {
                    validadCobra.postValue(response.body()!!)
                    //message.postValue(response.body()?.message ?: "Error al validar el Cobro")
                }
                showProgress.postValue(false)
            }

        })
    }

    fun getSaleReceipt(numeroDocumento: String) {
        showProgress.postValue(true)
        repository.getReceipt(ReceiptRequest(numeroDocumento)).enqueue(object : Callback<ApiWrapper<ReceiptEntity>> {
            override fun onFailure(call: Call<ApiWrapper<ReceiptEntity>>, t: Throwable) {
                Log.e(TAG, t.message.toString())
            }

            override fun onResponse(call: Call<ApiWrapper<ReceiptEntity>>, response: Response<ApiWrapper<ReceiptEntity>>) {
                if (response.isSuccessful && response.body()!!.result) {
                    val receiptEntity = response.body()!!.data
                    if (receiptEntity != null) {
                        val data = Base64.decode(receiptEntity.documentoPrint, Base64.DEFAULT)
                        val dataQr = receiptEntity.qrbase64
                        receiptEntity.documentoPrint = String(data)
                        receiptEntity.qrbase64 = dataQr
                        receiptLiveData.postValue(receiptEntity)
                    } else {
                        errorMessages.postValue(response.body()!!.message)
                    }
                } else {
                    errorMessages.postValue("Error al obtener el documento")
                }

                showProgress.postValue(false)
            }

        })
    }
    fun getSaleReceiptPrintCotizacion(cotizacionRequest: CotizacionPrintRequest) {
        showProgress.postValue(true)
        repository.imprimirCotizacion(cotizacionRequest).enqueue(object : Callback<CotizacionPrint> {
            override fun onFailure(call: Call<CotizacionPrint>, t: Throwable) {
                showProgress.postValue(false)
                Log.e(TAG, t.message.toString())
            }

            override fun onResponse(call: Call<CotizacionPrint>, response: Response<CotizacionPrint>) {
                if (response.isSuccessful && response.body()!!.success) {
                    val cotizacionPrint = response.body()!!
                    if (cotizacionPrint != null) {
                        receiptPrintCotizacionLiveData.postValue(cotizacionPrint)
                    } else {
                        errorMessages.postValue(response.body()!!.message.toString())
                    }
                } else {
                    errorMessages.postValue(response.body()!!.message.toString())
                }

                showProgress.postValue(false)
            }

        })
    }
    fun saveCotizacion(cotizacionRequest: CotizacionPrintRequest) {
        showProgress.postValue(true)
        repository.crearCotizacion(cotizacionRequest).enqueue(object : Callback<CotizacionPrint> {
            override fun onFailure(call: Call<CotizacionPrint>, t: Throwable) {
                showProgress.postValue(false)
                Log.e(TAG, t.message.toString())
            }

            override fun onResponse(call: Call<CotizacionPrint>, response: Response<CotizacionPrint>) {
                if (response.isSuccessful && response.body()!!.success) {
                    val cotizacionPrint = response.body()!!
                    if (cotizacionPrint != null) {
                        saveCotizacionLiveData.postValue(cotizacionPrint)
                    } else {
                        errorMessages.postValue(response.body()!!.message.toString())
                    }
                } else {
                    errorMessages.postValue(response.body()!!.message.toString())
                }

                showProgress.postValue(false)
            }

        })
    }

    fun getSaleReceiptPDF(numeroDocumento: String) {
        showProgress.postValue(true)
        repository.getReceiptPDF(ReceiptRequest(numeroDocumento)).enqueue(object : Callback<ApiWrapper<ReceiptEntity>> {
            override fun onFailure(call: Call<ApiWrapper<ReceiptEntity>>, t: Throwable) {
                Log.e(TAG, t.message.toString())
            }

            override fun onResponse(call: Call<ApiWrapper<ReceiptEntity>>, response: Response<ApiWrapper<ReceiptEntity>>) {
                if (response.isSuccessful && response.body()!!.result) {
                    val receiptEntity = response.body()!!.data
                    if (receiptEntity != null) {
                        val data = Base64.decode(receiptEntity.documentoPrint, Base64.DEFAULT)

                        receiptEntity.pdfBytes = data//getPDFBytes(data)
                        receiptLiveData.postValue(receiptEntity)
                    } else {
                        errorMessages.postValue(response.body()!!.message)
                    }
                } else {
                    errorMessages.postValue("Error al obtener el documento")
                }

                showProgress.postValue(false)
            }

        })
    }

    //review
    private fun getPDFBytes(byteArray: ByteArray): ByteArray {
        val input = byteArray.inputStream()
        val bos = ByteArrayOutputStream()
        val b = ByteArray(1024)
        while ((input.read(b)) != -1) {
            bos.write(b, 0, input.read(b))
        }

        return bos.toByteArray()
    }

    fun eliminarPedido() {

    }

    fun cobrarPedido() {

    }

}