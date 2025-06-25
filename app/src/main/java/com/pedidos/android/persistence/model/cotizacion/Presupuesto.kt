package com.pedidos.android.persistence.model.cotizacion

import java.io.Serializable

data class Presupuesto(
    val success: Boolean,
    val message: String,
    val presupuestos: MutableList<CotizacionCab>?
): Serializable
