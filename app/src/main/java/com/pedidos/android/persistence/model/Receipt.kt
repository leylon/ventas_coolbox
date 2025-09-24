package com.pedidos.android.persistence.model

import android.arch.persistence.room.Ignore

interface Receipt {
    var documentoPrint: String
    var qrbase64 : String
    var imagenqr: String
    var imagenqr2: String
    var piedocumentoprint: String
    var serviceResultMessage : String
}