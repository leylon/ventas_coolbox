package com.pedidos.android.persistence.model.firma

data class FirmaResponse(
    val `data`: FirmaHead,
    val message: String,
    val result: Boolean
)
