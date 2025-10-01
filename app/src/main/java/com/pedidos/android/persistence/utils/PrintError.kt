package com.pedidos.android.persistence.utils

enum class PrintError {
    CONNECTION_FAILED, // Falla de conexión, impresora apagada, fuera de alcance.
    INVALID_DATA,      // La cadena Base64 está corrupta o el formato de imagen no es válido.
    UNKNOWN_ERROR      // Cualquier otro error inesperado.
}