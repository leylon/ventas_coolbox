package com.pedidos.android.persistence.ui.reports

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.db.entity.CashBalanceEntity
import com.pedidos.android.persistence.db.entity.GeneratedDocumentEntity
import com.pedidos.android.persistence.db.entity.PaymentResponseEntity
import com.pedidos.android.persistence.db.entity.ReceiptEntity
import com.pedidos.android.persistence.ui.menu.MenuActivity
import com.pedidos.android.persistence.ui.payment.PaymentActivity
import com.pedidos.android.persistence.ui.payment.PaymentActivity.Companion
import com.pedidos.android.persistence.utils.PrintingCallback
import com.pedidos.android.persistence.viewmodel.GeneratedDocumentsViewModel
import kotlinx.android.synthetic.main.generated_documents_activity.*
import kotlinx.android.synthetic.main.generated_documents_activity.toolbar

class GeneratedDocumentsActivity : MenuActivity() {

    private lateinit var viewModel: GeneratedDocumentsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithMenu(R.layout.generated_documents_activity)
        setSupportActionBarMenu(toolbar)
        checkSession()

        val factory = GeneratedDocumentsViewModel.Companion.Factory(application, getSettings().urlbase)

        viewModel = ViewModelProviders.of(this, factory)[GeneratedDocumentsViewModel::class.java]

        viewModel.operationReportResult.observe(this, Observer {
            if (it != null) {
                showResults(it)
            }
        })

        viewModel.errorMessages.observe(this, Observer { showError(it) })
        viewModel.showProgress.observe(this, Observer { showProgress(it!!) })
        viewModel.receiptResult.observe(this, Observer {
            if (it != null) {
                printDocument(it)
            }
        })

        btnSearch.setOnClickListener { search() }
        rvwProducts.layoutManager = LinearLayoutManager(this)
        rvwProducts.adapter = GeneratedDocumentsAdapter(mutableListOf()) { viewModel.getDocumentToPrint(it.documentTypeTemp, it.documentNumberTemp)}
    }

    override fun onBackPressed(){}

    private fun search() {
        viewModel.showProgress.postValue(true)
        val date = "${datePickerGeneratedDocument.year}${"%02d".format(datePickerGeneratedDocument.month + 1)}${"%02d".format(datePickerGeneratedDocument.dayOfMonth)}"
        viewModel.getGeneratedDocuments(CashBalanceEntity().apply {
            this.date = date
            this.username = getSession().usuario
        })
    }

    private fun showResults(sales: List<GeneratedDocumentEntity>) {
        (rvwProducts.adapter as GeneratedDocumentsAdapter).updateList(sales)
    }

    private fun printDocument(document: ReceiptEntity){
        println("DOCUMENTO A IMPRIMIR: $document")
        /*performPrinting(document.documentoPrint)
        performPrintingQr(document.imagenqr)
        performPrinting(document.piedocumentoprint)
        if (document.imagenqr2.trim().isNotEmpty()){
            performPrintingQr(document.imagenqr2)
        }*/
        performAfterOperationsQueueQR(document)
    }

    // 3. LA FUNCIÓN PRINCIPAL OPTIMIZADA
    private fun performAfterOperationsQueueQR(entity: ReceiptEntity?) {
        if (entity == null) {
            showPrintingErrorDialog("Error al obtener el recibo.")
            Log.e(PaymentActivity.TAG, "La entidad de respuesta de pago es nula.")
            return
        }

        // Creamos una "cola" con todos los documentos que necesitamos imprimir en orden.
        // Usamos un par (Pair) para guardar el texto a imprimir y un nombre para identificarlo.
        val printQueue = mutableListOf<Pair<String, String>>()

        // Añadimos los documentos a la cola en el orden correcto.
        if (entity.documentoPrint.trim().isNotEmpty()) {
            printQueue.add(Pair(entity.documentoPrint, "cuerpo del recibo"))
        }
        if (entity.imagenqr.trim().isNotEmpty()) {
            printQueue.add(Pair(entity.imagenqr, "código QR"))
        }
        if (entity.piedocumentoprint.trim().isNotEmpty()) {
            printQueue.add(Pair(entity.piedocumentoprint, "pie del recibo"))
        }
        if (entity.imagenqr2.trim().isNotEmpty()) {
            printQueue.add(Pair(entity.imagenqr2, "segundo código QR"))
        }
        var messagePrinted = if (entity.serviceResultMessage.isNullOrBlank()) "Se imprimió con éxito" else entity.serviceResultMessage
        // Creamos una función interna que procesará la cola, un elemento a la vez.
        fun processPrintQueue(index: Int) {
            // CASO BASE: Si ya procesamos todos los elementos, hemos terminado con la impresión principal.
            if (index >= printQueue.size) {
                // Todos los documentos principales se imprimieron con éxito.

                confirmResultMessage(messagePrinted, onOk = { dialog ->
                        dialog.dismiss()
                })
                return
            }

            // Tomamos el documento actual de la cola.
            val currentDocument = printQueue[index]
            val dataToPrint = currentDocument.first
            val documentName = currentDocument.second

            // Creamos un callback genérico para manejar el resultado de la impresión.
            val printingCallback = object : PrintingCallback {
                override fun onPrintingSuccess() {
                    println("Éxito al imprimir: $documentName")
                    // Si la impresión fue exitosa, procesamos el SIGUIENTE elemento de la cola.
                    processPrintQueue(index + 1)
                }

                override fun onPrintingError(errorMessage: String?) {
                    Log.e(PaymentActivity.TAG, "Error al imprimir $documentName: $errorMessage")
                    // Si algo falla, mostramos un error y detenemos la secuencia.
                    showPrintingErrorDialog("Error al imprimir el $documentName.")
                }
            }

            // Decidimos qué función de impresión usar basándonos en el nombre del documento.
            if (documentName.contains("QR", ignoreCase = true)) {
                performPrintingQr(dataToPrint, printingCallback)
            } else {
                performPrinting(dataToPrint, printingCallback)
            }
        }

        // Iniciamos el proceso de impresión con el primer elemento de la cola (índice 0).
        processPrintQueue(0)
    }
    private fun confirmResultMessage(message: String, onOk: (alert: DialogInterface) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { it, _ ->
                onOk(it)
            }.show()
    }

    // 2. El helper para mostrar diálogos de error de forma limpia.
    private fun showPrintingErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(message)
            .setPositiveButton(R.string.aceptar) { d, _ -> d.dismiss() }
            .setCancelable(false)
            .create().show()
    }

    private fun showError(error: String?) {
        fltLoading.visibility = View.GONE
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(error)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    private fun showProgress(show: Boolean) {
        fltLoading.visibility = if (show) View.VISIBLE else View.GONE
    }
}
