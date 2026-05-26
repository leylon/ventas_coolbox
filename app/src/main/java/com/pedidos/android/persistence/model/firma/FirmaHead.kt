package com.pedidos.android.persistence.model.firma

import com.google.gson.annotations.SerializedName

data class FirmaHead(
    @SerializedName("URL_TERMINOS")
    val urlTermino: String,
    @SerializedName("TI_DOCU_IDEN")
    val tiDocIden: String,
    @SerializedName("NU_DOCU_IDEN")
    val nuDocuIden: String,
    @SerializedName("NO_CLIE")
    val noClie: String,
    @SerializedName("DE_MAIL_CLIE")
    val deMailClie: String,
    @SerializedName("GEX_DESCRIPCION")
    val gexDescripcion: String,
    @SerializedName("GEX_IMPORTE")
    val gexImport: String,
    @SerializedName("GEX_PRODUCTO")
    val gexProducto: String,

    val detalle: MutableList<FirmaDetail>
)