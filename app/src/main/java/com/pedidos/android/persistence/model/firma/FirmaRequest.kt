package com.pedidos.android.persistence.model.firma

data class FirmaRequest(
    val tienda: String,
    val pedido: String,
    val usuario: String
)
