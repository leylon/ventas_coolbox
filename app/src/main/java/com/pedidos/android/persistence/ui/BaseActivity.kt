package com.pedidos.android.persistence.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.support.design.widget.Snackbar
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
import java.io.File
import java.io.FileOutputStream


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

        // Verificar si la versión de Android es 12 o superior
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Verificar si el permiso BLUETOOTH_CONNECT está otorgado
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED  ||
                checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf( android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN), 1)
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

    protected fun performPrinting(documentoPrint: String): Boolean {
        if (documentoPrint == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        try {
            val blueToothWrapper = this.setupPrinter()
            if (blueToothWrapper != null) {
                blueToothWrapper.outputStream.write(documentoPrint.toByteArray(Charsets.ISO_8859_1))
                Thread.sleep(1500)
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
        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }

        protected fun performPrintingQr(qrPrint: String): Boolean {
        if (qrPrint == "") {
            Log.i(CancelActivity.TAG, "no existe valor en el documento")
            printOnSnackBar(getString(R.string.payment_no_receipt))
            return false
        }
        try {

            val blueToothWrapper = this.setupPrinter()
            if (blueToothWrapper != null) {
                val qrByte = Base64.decode(qrPrint,Base64.DEFAULT)
                val qrBitmap = BitmapFactory.decodeByteArray(qrByte,0, qrByte.size)
                val documentPrint = Extensions().decodeBitmap(qrBitmap)

                blueToothWrapper.outputStream.write(byteArrayOf(0x1b, 'a'.toByte(), 0x01))
                blueToothWrapper.outputStream.write(documentPrint)
                Thread.sleep(1500)
                blueToothWrapper.outputStream.close()
                blueToothWrapper.inputStream.close()
                blueToothWrapper.close()
                return true
            } else {
                printOnSnackBar(getString(R.string.printer_error))
            }
        } catch (ex: Exception) {
            Log.e(CancelActivity.TAG, "ley : "+ex.message)

            //printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
        }

        return false
        //saveAndShareFile(Base64.decode(documentoPrint, Base64.DEFAULT), numeroDocumento)
    }

    protected fun performPrintingCotizacion(documentoPrint: String): Boolean {
         if (documentoPrint == "") {
             Log.i(CancelActivity.TAG, "no existe valor en el documento")
             printOnSnackBar(getString(R.string.payment_no_receipt))
             return false
         }
         try {
             val blueToothWrapper = this.setupPrinter()
             if (blueToothWrapper != null) {
                 val documentoPrintByte = Base64.decode(documentoPrint,Base64.DEFAULT)
                 blueToothWrapper.outputStream.write(documentoPrintByte)
                 Thread.sleep(1500)
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

    protected fun performPrintingCotizacionNormal(cotizacionPrint: CotizacionPrint): Boolean {
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
                blueToothWrapper.outputStream.write(cotizacionPrint.cotiCabecera.toByteArray(Charsets.ISO_8859_1))
                blueToothWrapper.outputStream.write(cotizacionPrint.cotiBarraNumero.toByteArray(Charsets.ISO_8859_1))
                blueToothWrapper.outputStream.write(cotizacionPrint.cotiCuerpo.toByteArray(Charsets.ISO_8859_1))
                blueToothWrapper.outputStream.write(cotizacionPrint.cotiBarraCliente.toByteArray(Charsets.ISO_8859_1))
                blueToothWrapper.outputStream.write(cotizacionPrint.cotiPie.toByteArray(Charsets.ISO_8859_1))
                Thread.sleep(1500)
                blueToothWrapper.outputStream.close()
                blueToothWrapper.inputStream.close()
                blueToothWrapper.close()
                return true
            } else {
                printOnSnackBar(getString(R.string.printer_error))
            }
        } catch (ex: Exception) {
            Log.e(CancelActivity.TAG,"ley: "+ ex.message)
            // printOnSnackBar(getString(R.string.printer_error) + ": " + ex.message)
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