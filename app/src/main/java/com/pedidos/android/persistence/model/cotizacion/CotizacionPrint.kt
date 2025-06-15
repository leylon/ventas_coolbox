package com.pedidos.android.persistence.model.cotizacion

data class CotizacionPrint(
    val success: Boolean,
    val message: String? = null,
    val cotiCabecera: String,
    val cotiBarraNumero: String,
    val cotiCuerpo: String,
    val cotiBarraCliente: String,
    val cotiPie: String
)
