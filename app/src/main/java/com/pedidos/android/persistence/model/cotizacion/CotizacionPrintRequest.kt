package com.pedidos.android.persistence.model.cotizacion

import com.google.gson.annotations.SerializedName

data class CotizacionPrintRequest(
    val tipo: String,
    val documento: String,
    val numero : String,
    @SerializedName("tamaño_papel")
    val papelSize: String
)
