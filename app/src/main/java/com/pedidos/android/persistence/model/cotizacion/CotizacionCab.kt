package com.pedidos.android.persistence.model.cotizacion

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CotizacionCab(

    val serie: String,
    val numero: Int,
    val version: Int,
    val fecha: String, // Consider using a more specific date type if needed
    val hora: String, // Consider using a more specific time type if needed
    val codCliente: Int,
    val docCliente: String,
    val nombreCliente: String,
    @SerializedName("totalneto")
    val totalNeto: Double,
    val detalles: List<CotizacionDet>
): Serializable