package com.pedidos.android.persistence


import com.pedidos.android.persistence.utils.Formatter
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatterTest {

    @Test
    fun testConvertirFecha() {
        val fechaOriginal = "2025-06-20T12:00:00"
        val resultadoEsperado = "20/06/2025"

        val resultado = Formatter.convertirFecha(fechaOriginal)

        assertEquals(resultadoEsperado, resultado)
    }

    @Test
    fun testConvertirFechaFormatoIncorrecto() {
        val fechaOriginal = "fecha_invalida"
        val resultadoEsperado = ""

        val resultado = Formatter.convertirFecha(fechaOriginal)

        assertEquals(resultadoEsperado, resultado)
    }
}