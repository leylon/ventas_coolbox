package com.pedidos.android.persistence.model.cotizacion

import com.google.gson.annotations.SerializedName

data class CotizacionPrint(
    val success: Boolean,
    @SerializedName("mensaje")
    val message: String? = null,
    val cotiCabecera: String,
    val cotiBarraNumero: String,
    val cotiCuerpo: String,
    val cotiBarraCliente: String,
    val cotiPie: String
)
