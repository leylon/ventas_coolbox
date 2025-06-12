package com.pedidos.android.persistence.model.cotizacion

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CotizacionDet(
    @SerializedName("numlin")
    val numLin: String, // Aunque parece un número, está como String en el JSON

    @SerializedName("ean")
    val ean: String?,

    @SerializedName("unidades")
    val unidades: Int,

    @SerializedName("precio")
    val precio: Double,

    @SerializedName("precioiva")
    val precioIva: Double,

    @SerializedName("dto")
    val dto: Double, // O Double si puede tener decimales

    @SerializedName("total")
    val total: Double,

    @SerializedName("totaligv")
    val totalIgv: Double,

    @SerializedName("codarticulo")
    val codArticulo: String,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("sku")
    val sku: String


): Serializable
