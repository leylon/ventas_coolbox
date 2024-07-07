package com.pedidos.android.persistence.viewmodel

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.util.Base64
import android.util.Log
import com.pedidos.android.persistence.api.CoolboxApi
import com.pedidos.android.persistence.db.entity.PaymentEntity
import com.pedidos.android.persistence.db.entity.PaymentResponseEntity
import com.pedidos.android.persistence.db.entity.SaleEntity
import com.pedidos.android.persistence.model.pagos.PagoValeRequest
import com.pedidos.android.persistence.model.pagos.PagoValeResponse
import com.pedidos.android.persistence.ui.BasicApp
import com.pedidos.android.persistence.utils.ApiWrapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentViewModel(private val repository: CoolboxApi) : ViewModel() {
    companion object {
        val TAG = PaymentViewModel::class.java.simpleName!!

        class Factory(private var application: Application, urlBase: String) : ViewModelProvider.NewInstanceFactory() {
            private var repository = (application as BasicApp).getApiRepository(urlBase)

            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return PaymentViewModel(repository) as T
            }
        }
    }

    var liveData = MutableLiveData<PaymentResponseEntity>()
    var showLoading = MutableLiveData<Boolean>()
    var resultMessages = MutableLiveData<String>()

    // CPV: Se implemento onError
    fun savePayment(data: PaymentEntity, onError: (message: String) -> Unit) {
        showLoading.postValue(true)
        repository.payReceiptNew(data).enqueue(object : Callback<ApiWrapper<PaymentResponseEntity>> {

            override fun onFailure(call: Call<ApiWrapper<PaymentResponseEntity>>, t: Throwable) {
                Log.e(TAG, t.message.toString())
                resultMessages.postValue(t.message.toString())
                showLoading.postValue(false)
                // Agregado CPV
                onError(t.message.toString())
            }

            override fun onResponse(call: Call<ApiWrapper<PaymentResponseEntity>>, response: Response<ApiWrapper<PaymentResponseEntity>>) {
                val responseResult = response.body()!!.data
                responseResult!!.serviceResultMessage = response.body()!!.message
                if (response.isSuccessful && response.body()!!.result) {
                    val receipt = Base64.decode(responseResult.documentoPrint, Base64.DEFAULT)
                    val qrReceip = responseResult.qrPrint
                    val pieReceip = Base64.decode(responseResult.piedocumentoPrint, Base64.DEFAULT)
                    responseResult.documentoPrint = String(receipt)
                    responseResult.qrPrint = qrReceip
                    responseResult.piedocumentoPrint = String(pieReceip)
                    if (responseResult.voucherMposPrint.trim().isNotEmpty()) {
                        val voucherVisa = Base64.decode(responseResult.voucherMposPrint, Base64.DEFAULT)
                        responseResult.voucherMposPrint = String(voucherVisa)
                    }

                    liveData.postValue(responseResult)
                } else {
                    resultMessages.postValue(response.body()!!.message)
                    // Agregado CPV
                    onError(response.body()!!.message)
                }

                showLoading.postValue(false)
            }
        })
    }



    fun saveSale(saleEntity: SaleEntity,onSuccess: (entity: SaleEntity) -> Unit, onError: (message: String) -> Unit) {
        //validations
        repository.insertSale(saleEntity)
            .enqueue(object : Callback<ApiWrapper<SaleEntity>> {
                override fun onResponse(call: Call<ApiWrapper<SaleEntity>>, response: Response<ApiWrapper<SaleEntity>>) {
                    if (response.isSuccessful) {
                        if (response.body()!!.result) {

                            //saleLiveData.postValue(response.body()!!.data!!)
                            onSuccess(response.body()!!.data!!)

                        } else {
                            //Log.e(TAG, "error at ${response.body()!!.message}")
                            showLoading.postValue(false)
                            // Mensaje en pequeño de pantalla
                            resultMessages.postValue(response.body()!!.message)
                            // Mensaje poppup
                            onError(response.body()!!.message)



                        }
                    }
                }

                override fun onFailure(call: Call<ApiWrapper<SaleEntity>>, t: Throwable) {
                    Log.e(SaleViewModel.TAG, "error at ${t.message.toString()}")
                    showLoading.postValue(false)
                    // Mensaje en pequeño de pantalla
                    resultMessages.postValue(t.message.toString())
                    // Mensaje poppup
                    onError(t.message.toString())


                }

            })
    }

    fun pagoVale(request: PagoValeRequest,onSuccess: (entity: PagoValeResponse) -> Unit, onError: (message: String) -> Unit){
        repository.pagovale(request)
            .enqueue(object: Callback<PagoValeResponse> {
                override fun onResponse(
                    call: Call<PagoValeResponse>,
                    response: Response<PagoValeResponse>
                ) {
                    if (response.isSuccessful) {
                        if (response.body()?.result == true) {

                            //saleLiveData.postValue(response.body()!!.data!!)
                            onSuccess(response.body()!!)

                        } else {
                            //Log.e(TAG, "error at ${response.body()!!.message}")
                            showLoading.postValue(false)
                            // Mensaje en pequeño de pantalla
                            resultMessages.postValue(response.body()!!.message)
                            // Mensaje poppup
                            response.body()!!.message?.let { onError(it) }



                        }
                    }
                }

                override fun onFailure(call: Call<PagoValeResponse>, t: Throwable) {
                    Log.e(SaleViewModel.TAG, "error at ${t.message.toString()}")
                    showLoading.postValue(false)
                    // Mensaje en pequeño de pantalla
                    resultMessages.postValue(t.message.toString())
                    // Mensaje poppup
                    onError(t.message.toString())

                }
            })
    }
}