package com.pedidos.android.persistence.utils

import android.annotation.SuppressLint
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Formatter {
    companion object {
        val decimalFormat = DecimalFormat("#0.00", DecimalFormatSymbols(Locale.US))

        @SuppressLint("SimpleDateFormat")
        val dateFormat = SimpleDateFormat("yyyyMMdd")

        @SuppressLint("SimpleDateFormat")
        val dateFormatStandar = SimpleDateFormat("dd/MM/yyyy")

        fun DateToString(date: Date): String {
            return dateFormat.format(date)
        }
        fun DateToStringStandar(date: Date): String {
            return dateFormatStandar.format(date)
        }


        fun DoubleToString(input: Double): String {
            return decimalFormat.format(input)
        }

        fun DoubleToString(input: Double, symbol: String): String {
            return "$symbol ${DoubleToString(input)}"
        }
        fun formato(xcade: String, l: Int, xcar: String): String? {
            var xcar = xcar
            val auxi: String
            val aux1: String
            var cad = ""
            val i: Int
            val j: Int
            auxi = xcade.trim { it <= ' ' }
            i = auxi.length
            j = l - i
            //aux1=FillA(xcar,j)+auxi;
            if (j == 0) {
                xcar = ""
            } else {
                for (k in 0 until j) {
                    cad += xcar
                }
            }
            aux1 = cad + auxi
            return aux1
        }
        fun convertirFecha(fechaOriginal: String): String {
            return try {
                var fechaOriginal = "${fechaOriginal}Z"
                val formatoEntrada  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                //formatoEntrada.timeZone = TimeZone.getTimeZone("UTC")
                val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                // Parsear la fecha original y formatearla al nuevo formato
                val fecha = formatoEntrada.parse(fechaOriginal)
                return formatoSalida.format(fecha)
            } catch (e: Exception) {
                e.printStackTrace()
                return "" // o manejar el error como prefieras
            }
        }
    }
}