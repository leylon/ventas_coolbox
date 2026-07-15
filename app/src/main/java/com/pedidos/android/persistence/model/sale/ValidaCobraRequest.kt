package com.pedidos.android.persistence.model.sale

data class ValidaCobraRequest(
    var tipoDocumento: String,
    var documento: String,
    var androidimei: String,
    var androiduuid: String,
)
