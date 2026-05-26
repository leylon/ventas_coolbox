package com.pedidos.android.persistence.model.firma

import com.google.gson.annotations.SerializedName

data class FirmaDetail(
    @SerializedName("NU_SECU")
    var nuSecu: Int? =null,
    @SerializedName("CO_ITEM")
    var coItem: String? =null,
    @SerializedName("DE_ITEM")
    var deItem: String? =null,
    @SerializedName("CA_DOCU")
    var caDocu: Double? =null,
    @SerializedName("PR_VENT_CIMP")
    var prVentCimp: Double? =null,
    @SerializedName("PR_GEX")
    var prGex: Double? =null,
    @SerializedName("CO_ITEM_GEX")
    var coItemGex: String? =null,
    @SerializedName("NU_SECU_GEX")
    var nuSecuGex: Int? =null
)
