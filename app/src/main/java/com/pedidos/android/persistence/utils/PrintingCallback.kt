package com.pedidos.android.persistence.utils

interface PrintingCallback {
    fun onPrintingSuccess()
    fun onPrintingError(errorMessage: String?)
}