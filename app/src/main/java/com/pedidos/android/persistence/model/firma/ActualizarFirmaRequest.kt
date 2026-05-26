package com.pedidos.android.persistence.model.firma

data class ActualizarFirmaRequest(
    val tienda: String,
    val pedido: String,
    val usuario: String,
    val firmaCliente: String,
    val correoCliente: String,
    val aceptaTerminos: Boolean,
    val fecha: String,
    val hora: String,
)
