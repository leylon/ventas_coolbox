package com.pedidos.android.persistence.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.pedidos.android.persistence.model.LoginResponse
import com.pedidos.android.persistence.ui.login.LoginActivity
import com.google.gson.Gson
import com.pedidos.android.persistence.BuildConfig
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.api.CoolboxApi
import com.pedidos.android.persistence.db.entity.SettingsEntity
import com.pedidos.android.persistence.model.Settings
import com.pedidos.android.persistence.model.cotizacion.CotizacionPrint
import com.pedidos.android.persistence.ui.cancel.CancelActivity
import com.pedidos.android.persistence.ui.menu.MenuActivity
import com.pedidos.android.persistence.utils.BluetoothConnector
import com.pedidos.android.persistence.utils.Extensions
import com.pedidos.android.persistence.utils.PaperWidth
import com.pedidos.android.persistence.utils.PrintingCallback
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.math.min


@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    fun checkSession() {
        val sessionActive = getSession()
        //no quitar validacion de null
        if (sessionActive.usuario == null || sessionActive.usuario == "") {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    fun getSession(): LoginResponse {
        val preferences = getSharedPreferences(LoginActivity.NAMESPACE, Context.MODE_PRIVATE)
        return Gson().fromJson(preferences.getString(LoginActivity.SESSION_USER_NAME, "{}"), LoginResponse::class.java)
    }

    fun cleanSession() {
        val preferences = getSharedPreferences(LoginActivity.NAMESPACE, Context.MODE_PRIVATE)
        preferences.edit().putString(
                LoginActivity.SESSION_USER_NAME, "{}").apply()
    }

    fun saveSetting(settings: Settings) {
        val preferences = getSharedPreferences(MenuActivity.NAMESPACE, Context.MODE_PRIVATE)

        preferences.edit().putString(
                MenuActivity.SETTINGS,
                Gson().toJson(settings)
        ).apply()
    }

    fun getSettings(): Settings {
        val preferences = getSharedPreferences(MenuActivity.NAMESPACE, Context.MODE_PRIVATE)
        val settings = Gson().fromJson(preferences.getString(MenuActivity.SETTINGS, "{}"), SettingsEntity::class.java)
        if (settings.urlbase == "") {
            val defaultApiUrl = if (BuildConfig.DEBUG) BasicApp.DEFAULT_BASE_URL_DEBUG else BasicApp.DEFAULT_BASE_URL
            settings.urlbase = defaultApiUrl
        }

        return settings
    }

    fun getRepository(): CoolboxApi {
        return CoolboxApi.create(getSettings().urlbase)
    }

    fun printOnSnackBar(content: String) {
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) as ViewGroup
        Snackbar.make(view, content, Snackbar.LENGTH_INDEFINITE)
                .setDuration(2000)
                .setAction("Action", null).show()
    }
    fun printOnDialogMessaging(content: String) {
        /*val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) as ViewGroup
        Snackbar.make(view, content, Snackbar.LENGTH_INDEFINITE)
            .setDuration(2000)
            .setAction("Action", null).show()*/
        AlertDialog.Builder(this, R.style.AppTheme_DIALOG)
            .setTitle(R.string.app_name)
            .setMessage(content)
            .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
            .setCancelable(false)
            .create().show()
    }
    fun printOnSnackBarTop(content: String) {
        val view = (findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0) as ViewGroup
        Snackbar.make(view, content, Snackbar.LENGTH_LONG)
            .setDuration(2000)
            .setAction("Action", null).show()
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params

    }

    //todo: add optional lambda, replace uses
    fun confirmMessage(content: String) {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(content)
                .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
                .setCancelable(false)
                .create().show()

    }

    private fun setupPrinter(): BluetoothConnector.BluetoothSocketWrapper? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Permisos para Android 12 (API 31) y superiores
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )

            requestPermissions( permissions, 1)
        } else {
            // Permisos para versiones anteriores
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            requestPermissions( permissions, 1)
        }
        // Verificar si la versión de Android es 12 o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            ), 1)
            Log.i(CancelActivity.TAG, "Solicitando permisos de Bluetooth")
            // Verificar si el permiso BLUETOOTH_CONNECT está otorgado
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED  ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf( Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN), 1)
                Log.e(CancelActivity.TAG, "Error checkSelfPermission: ${getString(R.string.bluetooth_permission_required)}")
                printOnSnackBar(getString(R.string.bluetooth_permission_required))
                return null
            }
        }



        val pairedDevices = bluetoothAdapter.bondedDevices
        Log.i(CancelActivity.TAG, "Paired devices: ${pairedDevices.size}")
        // Verificar si hay dispositivos emparejados
        Log.i(CancelActivity.TAG, "Paired devices: ${pairedDevices.map { it.name }}")


        if (pairedDevices == null || pairedDevices.isEmpty()) {
            Log.e(CancelActivity.TAG,
                "Error pairedDevices: ${getString(R.string.no_devices_paired)}")

            printOnSnackBar(getString(R.string.no_devices_paired))
            return null
        }

        val settings = getSettings()
        if (settings.impresora.isEmpty()) {
            Log.e(CancelActivity.TAG,
                "Error settings: ${getString(R.string.printer_not_configured)}")

            printOnSnackBar(getString(R.string.printer_not_configured))
            return null
        }else{
            Log.i(CancelActivity.TAG, "Impresora configurada: ${settings.impresora}")
        }

        val device = pairedDevices.first { it.name == settings.impresora }

        if (device == null) {
            Log.e(CancelActivity.TAG, "Error device: ${getString(R.string.printer_not_found)}")
            printOnSnackBar(getString(R.string.printer_not_found))
            return null
        }

        return try {
            BluetoothConnector(device, false, bluetoothAdapter, null).connect()
        } catch (e: Exception) {
            Log.e(CancelActivity.TAG, "Error connecting to printer: ${e.message}")
            printOnSnackBar(getString(R.string.printer_error))
            null
        }
    }
    /* version antigua de la impresora
    private fun setupPrinter(): BluetoothConnector.BluetoothSocketWrapper? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter.bondedDevices

        if (pairedDevices == null || pairedDevices.size == 0) {
            printOnSnackBar(getString(R.string.no_devices_paired))
            return null
        }

        val settings = getSettings()
        if (settings.impresora.isEmpty()) {
            printOnSnackBar(getString(R.string.printer_not_configured))
        }

        val device = pairedDevices.first { it ->
            it.name == settings.impresora
        }

        if (device == null) {
            printOnSnackBar(getString(R.string.printer_not_found))
        }

        return BluetoothConnector(device, false, bluetoothAdapter, null).connect()
    }*/



    protected fun performPrinting(bytes: ByteArray): Boolean {
        try {
            val blueToothWrapper = this.setupPrinter()
            if (blueToothWrapper != null) {
                println("Bytes a imprimir: ${bytes.size}")
                val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                val setUtf8  = getCodePageCommandSunmi(16)
                blueToothWrapper.outputStream.write(setMulti)
                blueToothWrapper.outputStream.write(setUtf8)
                blueToothWrapper.outputStream.write(bytes)
                blueToothWrapper.outputStream.close()
                blueToothWrapper.close()
                return true
            } else {
                printOnSnackBar(getString(R.string.printer_error))
            }
        } catch (ex: Exception) {
            Log.e(CancelActivity.TAG, ex.message.toString())
          //  printOnSnackBar("Error seleccionando la impresora: " + ex.message)
        }

        return false
    }
    fun getCodePageCommand(page: Int): ByteArray {
        return byteArrayOf(0x1B, 0x74, page.toByte()) // ESC t <n>
    }
    fun getCodePageCommandSunmi(page: Int): ByteArray {
        return byteArrayOf(0x1C.toByte(), 0x43.toByte(), 0xFF.toByte()) // ESC t <n>
    }
    protected fun performPrinting(documentoPrint: String): Boolean {

        val settings = getSettings()
        val width = settings.pageSize
        val typePrint = settings.typePrint
        println("Se configuró el Tamaño de Papel: ${width}")
        println("Se configuró el Tipo de Impresora: ${typePrint}")

        if (documentoPrint == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        var textData: ByteArray = byteArrayOf() // Inicialización vacía
        val blueToothWrapper = this.setupPrinter()
        println("Datos: ${documentoPrint}")
        try {

            if (blueToothWrapper != null) {
                /*val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                val setUtf8  = getCodePageCommandSunmi(16)
                blueToothWrapper.outputStream.write(setMulti)
                blueToothWrapper.outputStream.write(setUtf8)
*/
                when(typePrint) {
                    "HIOPOS" -> {
                        //Normal
                        println("Usando modo GENERIC (Code Page/IBM850)")
                        // Comandos que funcionaron para la impresora TongLiang
                        val initPrinter = byteArrayOf(0x1B, 0x40)
                        val selectCodePage = byteArrayOf(0x1B, 0x74, 0x13) // PC858 (Euro)
                        // NUEVO: Comando para establecer el interlineado por defecto (ESC 2)
                        //val setDefaultLineSpacing = byteArrayOf(0x1B, 0x32)
                        val setCompactLineSpacing = byteArrayOf(0x1B, 0x33, 15)
                        // Codificar el texto completo una sola vez.

                        // 1. Enviar comandos de inicialización y configuración.
                        blueToothWrapper.outputStream.write(initPrinter)
                        blueToothWrapper.outputStream.write(selectCodePage)
                        blueToothWrapper.outputStream.write(setCompactLineSpacing) // Aplicamos el interlineado estándar
                        blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC !
                        textData = documentoPrint.toByteArray(Charset.forName("IBM850"))
                    }
                    "SUNMI" -> {
                        //Doble Alto
                        val resetFontMode = byteArrayOf(0x1B, 0x21, 0x00)
                        blueToothWrapper.outputStream.write(resetFontMode)
                        val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                        val setUtf8  = getCodePageCommandSunmi(16)
                        blueToothWrapper.outputStream.write(setMulti)
                        blueToothWrapper.outputStream.write(setUtf8)
                        textData = documentoPrint.toByteArray(Charsets.UTF_8)
                    }
                    "GENERIC" -> {
                        //Doble Ancho
                        println("Usando modo GENERIC (UTF-8)")
                        // Para Sunmi, enviamos directamente en UTF-8 sin comandos extraños.
                        // La impresora Sunmi debería interpretar UTF-8 de forma nativa.
                        val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                        val setUtf8  = getCodePageCommand(16)
                        blueToothWrapper.outputStream.write(setMulti)
                        blueToothWrapper.outputStream.write(setUtf8)
                        textData = documentoPrint.toByteArray(Charsets.UTF_8)
                    }
                    "POSD" -> {
                        //Doble Alto y Ancho
                        println("Usando modo POSD (Code Page/IBM850)")
                        // Probamos con la página de códigos PC850 (Multilingual), muy común.
                        val selectCodePage = byteArrayOf(0x1B, 0x74, 0x02) // PC850
                        val setCompactLineSpacing = byteArrayOf(0x1B, 0x33, 18)
                        blueToothWrapper.outputStream.write(selectCodePage)
                        blueToothWrapper.outputStream.write(setCompactLineSpacing)
                        textData = documentoPrint.toByteArray(Charset.forName("IBM850"))
                    }
                }
                when (width) {
                    "80mm"-> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                    }
                    "58mm" -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                        //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x10)) // Texto mediano
                        //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x08)) // Margen izquierdo reducido
                    }
                    "50mm" -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")

                    }
                    "48mm" -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")

                    }
                    else -> {
                        printOnSnackBar("Tamaño de papel no soportado: $width")
                        Log.e(CancelActivity.TAG, "Tamaño de papel no soportado: $width")
                       // return false
                    }
                }
                // 2. Enviar el texto en trozos (chunks) para evitar desbordamiento del búfer.

                val chunkSize = 512 // Tamaño del trozo en bytes. Puedes ajustar este valor.
                var offset = 0
                while (offset < textData.size) {
                    val size = min(chunkSize, textData.size - offset)
                    blueToothWrapper.outputStream.write(textData, offset, size)
                    // Pequeña pausa para que la impresora procese el trozo.
                    Thread.sleep(50)
                    offset += size
                }
               // blueToothWrapper.outputStream.write(documentoPrint.toByteArray(Charsets.UTF_8))
                blueToothWrapper.outputStream.write(byteArrayOf(0x0A, 0x0A))
                blueToothWrapper.outputStream.flush()
                Thread.sleep(1500)

                return true
            } else {
                printOnSnackBar(getString(R.string.printer_error))
            }
        } catch (ex: Exception) {
            Log.e(CancelActivity.TAG,"ley: "+ ex.message)
            printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
            return false
        } finally {
                Thread.sleep(100)
                blueToothWrapper?.outputStream?.close()
                Thread.sleep(100)
                blueToothWrapper?.inputStream?.close()
                blueToothWrapper?.close()
            println("Socket de impresora cerrado.")
        }
        return false
    }
    protected fun performPrinting(documentoPrint: String, callback: PrintingCallback) {
        // 1. Inicia un nuevo hilo para hacer todo el trabajo en segundo plano.
        Thread {
            var blueToothWrapper:  BluetoothConnector.BluetoothSocketWrapper? = null // Usa tu clase BlueToothWrapper
            try {
                val settings = getSettings()
                val width = settings.pageSize
                val typePrint = settings.typePrint

                if (documentoPrint.isEmpty()) {
                    runOnUiThread { callback.onPrintingError("El documento está vacío.") }
                    return@Thread
                }

                 blueToothWrapper = this.setupPrinter()

                if (blueToothWrapper != null) {
                    val outputStream = blueToothWrapper.outputStream
                    var textData: ByteArray

                    // Lógica de configuración de la impresora
                    when(typePrint) {
                        "HIOPOS" -> {
                            //Normal
                            println("Usando modo GENERIC (Code Page/IBM850)")
                            // Comandos que funcionaron para la impresora TongLiang
                            val initPrinter = byteArrayOf(0x1B, 0x40)
                            val selectCodePage = byteArrayOf(0x1B, 0x74, 0x13) // PC858 (Euro)
                            // NUEVO: Comando para establecer el interlineado por defecto (ESC 2)
                            //val setDefaultLineSpacing = byteArrayOf(0x1B, 0x32)
                            val setCompactLineSpacing = byteArrayOf(0x1B, 0x33, 15)
                            // Codificar el texto completo una sola vez.

                            // 1. Enviar comandos de inicialización y configuración.
                            blueToothWrapper.outputStream.write(initPrinter)
                            blueToothWrapper.outputStream.write(selectCodePage)
                            blueToothWrapper.outputStream.write(setCompactLineSpacing) // Aplicamos el interlineado estándar
                            blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC !
                            textData = documentoPrint.toByteArray(Charset.forName("IBM850"))
                        }
                        "SUNMI" -> {
                            //Doble Alto
                            val resetFontMode = byteArrayOf(0x1B, 0x21, 0x00)
                            blueToothWrapper.outputStream.write(resetFontMode)
                            val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                            val setUtf8  = getCodePageCommandSunmi(16)
                            blueToothWrapper.outputStream.write(setMulti)
                            blueToothWrapper.outputStream.write(setUtf8)
                            textData = documentoPrint.toByteArray(Charsets.UTF_8)
                        }
                        "GENERIC" -> {
                            //Doble Ancho
                            println("Usando modo GENERIC (UTF-8)")
                            // Para Sunmi, enviamos directamente en UTF-8 sin comandos extraños.
                            // La impresora Sunmi debería interpretar UTF-8 de forma nativa.
                            val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                            val setUtf8  = getCodePageCommand(16)
                            blueToothWrapper.outputStream.write(setMulti)
                            blueToothWrapper.outputStream.write(setUtf8)
                            textData = documentoPrint.toByteArray(Charsets.UTF_8)
                        }
                        "POSD" -> {
                            //Doble Alto y Ancho
                            println("Usando modo POSD (Code Page/IBM850)")
                            // Probamos con la página de códigos PC850 (Multilingual), muy común.
                            val selectCodePage = byteArrayOf(0x1B, 0x74, 0x02) // PC850
                            val setCompactLineSpacing = byteArrayOf(0x1B, 0x33, 18)
                            blueToothWrapper.outputStream.write(selectCodePage)
                            blueToothWrapper.outputStream.write(setCompactLineSpacing)
                            textData = documentoPrint.toByteArray(Charset.forName("IBM850"))
                        } else -> { // GENERIC y otros casos
                            val initPrinter = byteArrayOf(0x1B, 0x40)
                            outputStream.write(initPrinter)
                            textData = documentoPrint.toByteArray(Charset.forName("IBM850"))
                        }
                    }

                    // Lógica de envío de datos (chunks)
                    val chunkSize = 512
                    var offset = 0
                    while (offset < textData.size) {
                        val size = min(chunkSize, textData.size - offset)
                        outputStream.write(textData, offset, size)
                        Thread.sleep(50)
                        offset += size
                    }

                    outputStream.write(byteArrayOf(0x0A, 0x0A))
                    outputStream.flush()
                    Thread.sleep(1500)

                    // 2. Si todo sale bien, notifica el éxito.
                    runOnUiThread { callback.onPrintingSuccess() }

                } else {
                    runOnUiThread { callback.onPrintingError(getString(R.string.printer_error)) }
                }
            } catch (ex: Exception) {
                Log.e(CancelActivity.TAG, "Error en hilo de impresión: " + ex.message)
                // 3. Si hay un error, notifica el fallo.
                runOnUiThread { callback.onPrintingError(ex.message) }
            } finally {
                // 4. Cierra la conexión de forma segura en el hilo de fondo.
                try {
                    Thread.sleep(100)
                    blueToothWrapper?.outputStream?.close()
                    Thread.sleep(100)
                    blueToothWrapper?.close()
                    println("Socket de impresora cerrado.")
                } catch (e: Exception) {
                    Log.e(CancelActivity.TAG, "Error al cerrar socket: " + e.message)
                }
            }
        }.start() // No olvides .start() para que el hilo comience.
    }

        protected fun performPrintingQr(qrPrint: String): Boolean {
            val settings = getSettings()
            val width = settings.pageSize
            if (qrPrint == "") {
                Log.i(CancelActivity.TAG, "no existe valor en el documento")
                printOnSnackBar(getString(R.string.payment_no_receipt))
                return false
            }
            try {

                val blueToothWrapper = this.setupPrinter()

                if (blueToothWrapper != null) {
                    //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC !
                    //blueToothWrapper.outputStream.write(byteArrayOf(0x1D, 0x21, 0x00)) // GS !
                    //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x12))       // Descondensar
                    when (width) {
                        PaperWidth.WIDTH_80MM.widthValue -> {
                            //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                            println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                            //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x20)) // Texto grande
                            //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x10)) // Margen izquierdo más amplio
                        }

                        PaperWidth.WIDTH_58MM.widthValue -> {
                            //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                            println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                            // blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x10)) // Texto mediano
                            //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x08)) // Margen izquierdo reducido
                        }

                        PaperWidth.WIDTH_50_8MM.widthValue -> {
                            //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")
                            println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")

                        }

                        PaperWidth.WIDTH_48MM.widthValue -> {
                            //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")
                            println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")

                        }

                        else -> {
                            printOnSnackBar("Tamaño de papel no soportado: $width")
                            Log.e(CancelActivity.TAG, "Tamaño de papel no soportado: $width")
                            // return false
                        }
                    }

                    var documentPrint: ByteArray? = byteArrayOf()
                    val qrByte = Base64.decode(qrPrint, Base64.DEFAULT)
                    val qrBitmap = BitmapFactory.decodeByteArray(qrByte, 0, qrByte.size)
                    documentPrint = Extensions().decodeBitmap(qrBitmap)

                    blueToothWrapper.outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x01))

                    val chunkSize = 512 // Tamaño del trozo en bytes. Puedes ajustar este valor.
                    var offset = 0
                    while (offset < documentPrint!!.size) {
                        val size = min(chunkSize, documentPrint!!.size - offset)
                        blueToothWrapper.outputStream.write(documentPrint!!, offset, size)
                        // Pequeña pausa para que la impresora procese el trozo.
                        Thread.sleep(50)
                        offset += size
                    }

                    // 3. Agregar saltos de línea al final y asegurar que todo se envíe.
                    //blueToothWrapper.outputStream.write(byteArrayOf(0x0A, 0x0A,0x0A))
                    blueToothWrapper.outputStream.write(byteArrayOf(0x0A, 0x0A))
                    //blueToothWrapper.outputStream.write(documentPrint)

                    blueToothWrapper.outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x01))
                    //blueToothWrapper.outputStream.flush()
                    // blueToothWrapper.outputStream.write(documentPrint)
                    Thread.sleep(1800)
                    blueToothWrapper.outputStream.close()
                    blueToothWrapper.inputStream.close()
                    blueToothWrapper.close()
                    return true
                } else {
                    printOnSnackBar(getString(R.string.printer_error))
                }
            } catch (ex: Exception) {
                Log.e(CancelActivity.TAG, "ley : " + ex.message)

                //printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
            }

            return false
           //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }
    // Asumo que la interfaz PrintingCallback ya está definida en tu archivo.

    protected fun performPrintingQr(qrPrint: String, callback: PrintingCallback) {
        // 1. Inicia un hilo en segundo plano para no bloquear la app.
        Thread {
            var blueToothWrapper: BluetoothConnector.BluetoothSocketWrapper? = null
            try {
                if (qrPrint.isEmpty()) {
                    runOnUiThread { callback.onPrintingError("La cadena del QR está vacía.") }
                    return@Thread
                }

                blueToothWrapper = this.setupPrinter()

                if (blueToothWrapper != null) {
                    val outputStream = blueToothWrapper.outputStream

                    // --- INICIO DE TU LÓGICA DE QR ---
                    // Decodificar la cadena Base64 a un array de bytes
                    val qrByte = Base64.decode(qrPrint, Base64.DEFAULT)
                    // Convertir el array de bytes a un Bitmap de Android
                    val qrBitmap = BitmapFactory.decodeByteArray(qrByte, 0, qrByte.size)
                    // Usar tu función de ayuda para convertir el Bitmap al formato de bytes de la impresora
                    val documentPrint = Extensions().decodeBitmap(qrBitmap)
                    // --- FIN DE TU LÓGICA DE QR ---

                    // Comando para centrar el contenido (muy importante para un QR)
                    outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x01))

                    // Enviar la imagen en trozos para no saturar el buffer
                    val chunkSize = 512
                    var offset = 0
                    if (documentPrint != null) {
                        while (offset < documentPrint.size) {
                            val size = min(chunkSize, documentPrint.size - offset)
                            outputStream.write(documentPrint, offset, size)
                            Thread.sleep(50) // Pausa para que la impresora procese
                            offset += size
                        }
                    }

                    // Añadir espacio al final y resetear la alineación a la izquierda
                    outputStream.write(byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A))
                    outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x00)) // Alineación a la izquierda

                    outputStream.flush()
                    Thread.sleep(1800) // Pausa final para asegurar la impresión completa de la imagen

                    // 2. Notificar que la impresión fue exitosa.
                    runOnUiThread { callback.onPrintingSuccess() }

                } else {
                    runOnUiThread { callback.onPrintingError(getString(R.string.printer_error)) }
                }
            } catch (ex: Exception) {
                Log.e(CancelActivity.TAG, "Error en hilo de impresión QR: " + ex.message)
                // 3. Notificar si hubo un error.
                runOnUiThread { callback.onPrintingError(ex.message) }
            } finally {
                // 4. Cerrar la conexión de forma segura, pase lo que pase.
                try {
                    Thread.sleep(100)
                    blueToothWrapper?.outputStream?.close()
                    Thread.sleep(100)
                    blueToothWrapper?.close()
                    println("Socket de impresora QR cerrado.")
                } catch (e: Exception) {
                    Log.e(CancelActivity.TAG, "Error al cerrar socket de QR: " + e.message)
                }
            }
        }.start()
    }


    protected fun performPrintingCotizacion(documentoPrint: String,tipo: Int): Boolean {
        val settings = getSettings()
        val width = settings.pageSize
        println("Tamaño de Papel: $width")
        if (documentoPrint == "") {
             Log.i(CancelActivity.TAG, "no existe valor en el documento")
             printOnSnackBar(getString(R.string.payment_no_receipt))
             return false
         }
         try {
             val blueToothWrapper = this.setupPrinter()
             if (blueToothWrapper == null) {
                 printOnSnackBar(getString(R.string.printer_error))
                 return false
             }
             //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC !
             //blueToothWrapper.outputStream.write(byteArrayOf(0x1D, 0x21, 0x00)) // GS !
             //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x12))       // Descondensar
             when (width) {

                 PaperWidth.WIDTH_80MM.widthValue -> {
                     //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                     println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                     //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x20)) // Texto grande
                     //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x10)) // Margen izquierdo más amplio
                 }
                 PaperWidth.WIDTH_58MM.widthValue -> {
                     //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                     println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                     //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x10)) // Texto mediano
                     //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x08)) // Margen izquierdo reducido
                 }
                 PaperWidth.WIDTH_50_8MM.widthValue -> {
                     //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")
                     println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")
                     //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x08))  // Texto pequeño
                     //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x04)) // Margen izquierdo mínimo
                     // 1. Establecer modo página estrecha
                     blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x57, 0x00, 0x00, 0x32, 0x00))

                     // 2. Fuente ultra condensada
                     blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x0F))

                     // 3. Reducción al 60%
                     blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x58, 0x32, 0x33))

                     // 4. Márgenes mínimos
                     blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x51, 0x00)) // Margen derecho 0
                 }
                 PaperWidth.WIDTH_48MM.widthValue -> {
                     //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")
                     println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")

                 }
             }

             if (blueToothWrapper != null) {
                 val documentoPrintByte = Base64.decode(documentoPrint,Base64.DEFAULT)
                 blueToothWrapper.outputStream.write(documentoPrintByte)
                 Thread.sleep(2000)
                 blueToothWrapper.outputStream.close()
                 blueToothWrapper.inputStream.close()
                 blueToothWrapper.close()
                 return true
             } else {
                 printOnSnackBar(getString(R.string.printer_error))
             }
         } catch (ex: Exception) {
             Log.e(CancelActivity.TAG,"ley: "+ ex.message)
             printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
         }

         return false

            //printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
     }


        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
// Función auxiliar para configuración de 48mm

    protected fun performPrintingCotizacionNormal(cotizacionPrint: CotizacionPrint): Boolean {
        val settings = getSettings()
        val width = settings.pageSize
        val typePrint = settings.typePrint
        println("Tamaño de Papel: $width")
        if (cotizacionPrint.cotiCabecera == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento: Cabecera de Cotizacion")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        if (cotizacionPrint.cotiBarraNumero == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento: Barra Numero")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        if (cotizacionPrint.cotiCuerpo == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento: Cuerpo de Cotizacion")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        if (cotizacionPrint.cotiBarraCliente == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento: Barra Cliente")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        if (cotizacionPrint.cotiPie == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento: Pie de Cotizacion")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }

        try {
            val blueToothWrapper = this.setupPrinter()
            if (blueToothWrapper != null) {
                //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC !
                //blueToothWrapper.outputStream.write(byteArrayOf(0x1D, 0x21, 0x00)) // GS !
                //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x12))       // Descondensar
                when (width) {

                    PaperWidth.WIDTH_80MM.widthValue -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_80MM}")
                        //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x20)) // Texto grande
                        //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x10)) // Margen izquierdo más amplio
                    }
                    PaperWidth.WIDTH_58MM.widthValue -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_58MM}")
                        //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x10)) // Texto mediano
                        //blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x4C, 0x08)) // Margen izquierdo reducido
                    }
                    PaperWidth.WIDTH_50_8MM.widthValue -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_50_8MM}")

                    }
                    PaperWidth.WIDTH_48MM.widthValue -> {
                        //printOnSnackBar("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")
                        println("Se eligió el Tamaño de Papel: ${PaperWidth.WIDTH_48MM}")

                    }
                }

                when(typePrint) {
                    "HIOPOS" -> {
                        //Normal
                        println("Usando modo GENERIC (Code Page/IBM850)")
                        // Comandos que funcionaron para la impresora TongLiang
                        val initPrinter = byteArrayOf(0x1B, 0x40)
                        val selectCodePage = byteArrayOf(0x1B, 0x74, 0x13) // PC858 (Euro)
                        // NUEVO: Comando para establecer el interlineado por defecto (ESC 2)
                        //val setDefaultLineSpacing = byteArrayOf(0x1B, 0x32)
                        val setCompactLineSpacing = byteArrayOf(0x1B, 0x33, 15)
                        // Codificar el texto completo una sola vez.
                       // val textData = textToPrint.toByteArray(Charset.forName("IBM850"))
                        // 1. Enviar comandos de inicialización y configuración.
                        blueToothWrapper.outputStream.write(initPrinter)
                        blueToothWrapper.outputStream.write(selectCodePage)
                        blueToothWrapper.outputStream.write(setCompactLineSpacing) // Aplicamos el interlineado estándar
                        blueToothWrapper.outputStream.write(byteArrayOf(0x1B, 0x21, 0x00)) // ESC !
                    }
                    "SUNMI" -> {
                        //Doble Alto
                        val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                        val setUtf8  = getCodePageCommandSunmi(16)
                        blueToothWrapper.outputStream.write(setMulti)
                        blueToothWrapper.outputStream.write(setUtf8)
                    }
                    "GENERIC" -> {
                        //Doble Ancho
                        println("Usando modo GENERIC (UTF-8)")
                        // Para Sunmi, enviamos directamente en UTF-8 sin comandos extraños.
                        // La impresora Sunmi debería interpretar UTF-8 de forma nativa.
                        val setMulti = byteArrayOf(0x1C.toByte(), 0x26.toByte())
                        val setUtf8  = getCodePageCommand(16)
                        blueToothWrapper.outputStream.write(setMulti)
                        blueToothWrapper.outputStream.write(setUtf8)
                        //textData = textToPrint.toByteArray(Charsets.UTF_8)
                    }
                    "POSD" -> {
                        //Doble Alto y Ancho
                        println("Usando modo POSD (Code Page/IBM850)")
                        // Probamos con la página de códigos PC850 (Multilingual), muy común.
                        val selectCodePage = byteArrayOf(0x1B, 0x74, 0x02) // PC850
                        val setCompactLineSpacing = byteArrayOf(0x1B, 0x33, 18)
                        blueToothWrapper.outputStream.write(selectCodePage)
                        blueToothWrapper.outputStream.write(setCompactLineSpacing)
                        //textData = textToPrint.toByteArray(Charset.forName("IBM850"))
                    }

                }

                val cotiCabecera = Base64.decode(cotizacionPrint.cotiCabecera,Base64.DEFAULT)
                blueToothWrapper.outputStream.write(cotiCabecera)


                val qrByte = Base64.decode(cotizacionPrint.cotiBarraNumero,Base64.DEFAULT)
                val qrBitmap = BitmapFactory.decodeByteArray(qrByte,0, qrByte.size)
                val cotiBarraNumero = Extensions().decodeBitmap(qrBitmap)
                blueToothWrapper.outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x01))
                blueToothWrapper.outputStream.write(cotiBarraNumero)


                val cotiCuerpo = Base64.decode(cotizacionPrint.cotiCuerpo,Base64.DEFAULT)
                blueToothWrapper.outputStream.write(cotiCuerpo)


                val qrByteClient = Base64.decode(cotizacionPrint.cotiBarraCliente,Base64.DEFAULT)
                val qrBitmapClient = BitmapFactory.decodeByteArray(qrByteClient,0, qrByteClient.size)
                val cotiBarraCliente = Extensions().decodeBitmap(qrBitmapClient)
                blueToothWrapper.outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x01))
                blueToothWrapper.outputStream.write(cotiBarraCliente)


                val cotiPie =Base64.decode(cotizacionPrint.cotiPie,Base64.DEFAULT)

                blueToothWrapper.outputStream.write(cotiPie)
                Thread.sleep(2000)
                blueToothWrapper.outputStream.close()
                blueToothWrapper.inputStream.close()
                blueToothWrapper.close()

                return true

            } else {
                printOnSnackBar(getString(R.string.printer_error))
            }
        } catch (ex: Exception) {
            Log.e(CancelActivity.TAG,"ley: "+ ex.message)
            printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
            return false
        }
        return false
    }
    protected fun saveAndShareFile(bytes: ByteArray, fileName: String) {
        //delete all files
        val file = File.createTempFile(fileName, ".pdf", cacheDir)
        val fos = FileOutputStream(file, false)
        fos.write(bytes)
        fos.flush()
        fos.close()

        //shareFile(file)
        externalViewFile(file)
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(this, this.packageName + ".provider", file)
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun externalViewFile(file: File) {
        val uri = FileProvider.getUriForFile(this, this.packageName + ".provider", file)
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            trimCache(this)
        } catch (e: Exception) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    private fun trimCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory) {
                deleteDir(dir)
            }
        } catch (e: Exception) {
            // TODO: handle exception
        }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()

            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete()
    }

}