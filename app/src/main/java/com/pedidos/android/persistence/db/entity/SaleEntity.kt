package com.pedidos.android.persistence.db.entity

import android.arch.persistence.room.Ignore
import android.os.Parcel
import android.os.Parcelable
import com.pedidos.android.persistence.model.Sale
import com.pedidos.android.persistence.model.SaleSubItem
import com.google.gson.annotations.SerializedName

class SaleEntity() : Sale {
    override var documento: String = ""
    override var fecha: String = ""
    override var clienteTipoDocumento: Int = 1

    @SerializedName("clientecodigo")
    override var clienteCodigo: String = ""

    @SerializedName("clientenombres")
    override var clienteNombres: String = ""
    override var usuario: String = ""

    @SerializedName("vendedorcodigo")
    override var vendedorCodigo: String = ""

    @SerializedName("cajacodigo")
    override var cajaCodigo: String = ""
    override var tienda: String = ""

    @SerializedName("evento")
    override var evento: String = ""

    @SerializedName("subtotal")
    override var subTotal: Double = 0.0
    override var descuento: Double = 0.0
    override var impuesto: Double = 0.0
    override var total: Double = 0.0

    @SerializedName("sbmone")
    override var monedaSimbolo: String = ""

    @SerializedName("colordetalle")
    override var complementaryRowColor: String = ""

    override var impuesto2: Double = 0.0
    override var impuesto3: Double = 0.0
    override var nombreimpuesto1: String = ""
    override var nombreimpuesto2: String = ""
    override var nombreimpuesto3: String = ""
    override var productoconcomplemento: Int = 0
    override var telefono: String = ""
    override var email: String = ""
    override var androidimei: String = ""
    @Ignore
    var productos: MutableList<SaleSubItem> = mutableListOf()

    constructor(parcel: Parcel) : this() {
        documento = parcel.readString().toString()
        fecha = parcel.readString().toString()
        clienteTipoDocumento = parcel.readInt()
        clienteCodigo = parcel.readString().toString()
        clienteNombres = parcel.readString().toString()
        usuario = parcel.readString().toString()
        vendedorCodigo = parcel.readString().toString()
        cajaCodigo = parcel.readString().toString()
        tienda = parcel.readString().toString()
        evento = parcel.readString().toString()
        subTotal = parcel.readDouble()
        descuento = parcel.readDouble()
        impuesto = parcel.readDouble()
        impuesto2 = parcel.readDouble()
        impuesto3 = parcel.readDouble()
        nombreimpuesto1 = parcel.readString().toString()
        nombreimpuesto2 = parcel.readString().toString()
        nombreimpuesto3 = parcel.readString().toString()
        //productoconcomplemento = parcel.readInt()
        total = parcel.readDouble()
        productos = mutableListOf()
        (productos as List<*>?)?.let { parcel.readList(it, SaleSubItemEntity::class.java.classLoader) }
        //parcel.readTypedList(productos, SaleSubItemEntity.CREATOR)
        monedaSimbolo = parcel.readString().toString()
        complementaryRowColor = parcel.readString().toString()
        productoconcomplemento = parcel.readInt()
        telefono = parcel.readString().toString()
        email = parcel.readString().toString()
        androidimei = parcel.readString().toString()
    }

    constructor(sale: Sale) : this() {
        documento = sale.documento
        fecha = sale.fecha
        clienteTipoDocumento = sale.clienteTipoDocumento
        clienteCodigo = sale.clienteCodigo
        clienteNombres = sale.clienteNombres
        usuario = sale.usuario
        vendedorCodigo = sale.vendedorCodigo
        cajaCodigo = sale.cajaCodigo
        tienda = sale.tienda
        evento = sale.evento
        subTotal = sale.subTotal
        descuento = sale.descuento
        impuesto = sale.impuesto
        impuesto2 = sale.impuesto2
        impuesto3 = sale.impuesto3
        nombreimpuesto1 = sale.nombreimpuesto1
        nombreimpuesto2 = sale.nombreimpuesto2
        nombreimpuesto3 = sale.nombreimpuesto3
        total = sale.total
        productos = mutableListOf()
        monedaSimbolo = sale.monedaSimbolo
        complementaryRowColor = sale.complementaryRowColor
        productoconcomplemento = sale.productoconcomplemento
        telefono = sale.telefono
        email = sale.email
        androidimei = sale.androidimei
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(documento)
        parcel.writeString(fecha)
        parcel.writeInt(clienteTipoDocumento)
        parcel.writeString(clienteCodigo)
        parcel.writeString(clienteNombres)
        parcel.writeString(usuario)
        parcel.writeString(vendedorCodigo)
        parcel.writeString(cajaCodigo)
        parcel.writeString(tienda)
        parcel.writeString(evento)
        parcel.writeDouble(subTotal)
        parcel.writeDouble(descuento)
        parcel.writeDouble(impuesto)
        parcel.writeDouble(impuesto2)
        parcel.writeDouble(impuesto3)
        parcel.writeString(nombreimpuesto1)
        parcel.writeString(nombreimpuesto2)
        parcel.writeString(nombreimpuesto3)
        parcel.writeDouble(total)
        parcel.writeList(productos as List<*>?)
        parcel.writeString(monedaSimbolo)
        parcel.writeString(complementaryRowColor)
        parcel.writeInt(productoconcomplemento)
        parcel.writeString(telefono)
        parcel.writeString(email)
        parcel.writeString(androidimei)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SaleEntity> {
        override fun createFromParcel(parcel: Parcel): SaleEntity {
            return SaleEntity(parcel)
        }

        override fun newArray(size: Int): Array<SaleEntity?> {
            return arrayOfNulls(size)
        }
    }
}