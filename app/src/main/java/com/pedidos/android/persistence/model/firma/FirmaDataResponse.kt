package com.pedidos.android.persistence.model.firma

import com.pedidos.android.persistence.db.entity.SaleEntity

data class FirmaDataResponse(
    val `data`: SaleEntity,
    val message: String,
    val result: Boolean
)
