package com.pedidos.android.persistence.db.entity

import android.os.Parcel
import android.os.Parcelable
import com.pedidos.android.persistence.model.Settings

class SettingsEntity() : Settings {
    override var urlbase: String = ""
    override var logoUrl: String = ""
    override var impresora: String = "Printer_"
    override var pageSize: String = "80mm"
    override var  typePrint: String = "SUNMI"

    constructor(parcel: Parcel) : this() {
        urlbase = parcel.readString().toString()
        logoUrl = parcel.readString().toString()
        impresora = parcel.readString().toString()
        pageSize = parcel.readString().toString()
        typePrint = parcel.readString().toString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(urlbase)
        parcel.writeString(logoUrl)
        parcel.writeString(impresora)
        parcel.writeString(pageSize)
        parcel.writeString(typePrint)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SettingsEntity> {
        override fun createFromParcel(parcel: Parcel): SettingsEntity {
            return SettingsEntity(parcel)
        }

        override fun newArray(size: Int): Array<SettingsEntity?> {
            return arrayOfNulls(size)
        }
    }

}