package com.pedidos.android.persistence.ui.sale.fragment

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.model.cotizacion.CotizacionCab
import com.pedidos.android.persistence.model.guide.DataResponse
import com.pedidos.android.persistence.model.sale.VentaProductoResponse
import com.pedidos.android.persistence.ui.guide.fragment.OnClickListenerCustom
import com.pedidos.android.persistence.ui.sale.fragment.QuestionPopUpFragment.newDialoglistenerQuestion

class CotizacionPopUpFragment : DialogFragment,OnClickListenerCustom<CotizacionCab> {


    constructor()

    var optionQuestion: String = ""
    lateinit var imageButtonSearch: ImageButton
    lateinit var imageButtonCamera: ImageButton
    lateinit var editTextCliente: EditText
    lateinit var texto_dialogo: TextView
    lateinit var recyclerViewTickets: RecyclerView
    lateinit var cotizacionAdapter: CotizacionAdapter
    var listCotizacionCab: List<CotizacionCab> = listOf()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_cotizacion, container, false)
        //listCotizacionCab = arguments?.get("DataList") as List<CotizacionCab>
        optionQuestion = arguments?.getString("OptionQuestion").toString()
        if (optionQuestion.equals("search")){
            listCotizacionCab = arguments?.get("DataList") as List<CotizacionCab>
            println("listCotizacionCab: ${listCotizacionCab.size}")
            println("listaCotizacion: ${Gson().toJson(listCotizacionCab)}")
        }
        recyclerViewTickets = view.findViewById(R.id.recyclerViewTickets)
        imageButtonSearch  = view.findViewById(R.id.imageButtonSearch)
        imageButtonCamera = view.findViewById(R.id.imageButtonCamera)
        editTextCliente = view.findViewById(R.id.editTextCliente)
        cotizacionAdapter = CotizacionAdapter()
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerViewTickets.layoutManager = linearLayoutManager
        cotizacionAdapter.setDataStorage(listCotizacionCab)
        recyclerViewTickets.adapter = cotizacionAdapter
        cotizacionAdapter.setOnItemClickListener(this)
        imageButtonSearch.setOnClickListener {
            val searchText = editTextCliente.text.toString().trim()
            val activity = activity as newDialoglistenerCotizacion
            activity.closeDialogQuestion(searchText,null)
            this.dismiss()
        }
        imageButtonCamera.setOnClickListener {
            val searchText = "scanner"
            val activity = activity as newDialoglistenerCotizacion
            activity.closeDialogQuestion(searchText,null)
            this.dismiss()
        }
        return view
    }
    override fun onStart() {
        super.onStart()
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        // Por ejemplo, que el diálogo ocupe el 80% de la altura de la pantalla
        val dialogHeight = (screenHeight * 0.8).toInt()

        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight)
            window.setGravity(Gravity.CENTER)
        }
    }

    override fun onClickDataListener(objectData: CotizacionCab) {
        val activity = activity as newDialoglistenerCotizacion

        if (optionQuestion != "NO"){
            activity.closeDialogQuestion("item",objectData)
            this.dismiss()
        }
    }

    // You can add more functionality here if needed

    interface newDialoglistenerCotizacion {
        fun closeDialogQuestion(data : String, optionData : CotizacionCab?)
    }
}