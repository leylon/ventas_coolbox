package com.pedidos.android.persistence.ui.payment.fragment

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.support.design.widget.BottomSheetDialogFragment
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.model.SelectedCreditCard
import com.pedidos.android.persistence.ui.payment.CardsAdapter
import kotlinx.android.synthetic.main.payment_credict_cards_selectec_dialog.view.edtAmount
import kotlinx.android.synthetic.main.payment_credict_cards_selectec_dialog.view.edtAmountOther
import kotlinx.android.synthetic.main.payment_credict_cards_selectec_dialog.view.rwCards
import kotlinx.android.synthetic.main.payment_credict_cards_selectec_dialog.view.tvwAccept

class PaymentBottomSheetFragment: BottomSheetDialogFragment() {
    // Define la interfaz para devolver los datos (ver parte 2)
    interface PaymentListener {
        fun onPaymentAccepted(amount: String, reference: String, selectedCardCode: SelectedCreditCard, list: ArrayList<SelectedCreditCard>)
    }
    private var listener: PaymentListener? = null

    // Variable para guardar la lista
    private lateinit var cardList: ArrayList<SelectedCreditCard>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            cardList = it.getSerializable(CARD_LIST_KEY) as ArrayList<SelectedCreditCard>
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is PaymentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement PaymentListener")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.payment_credict_cards_selectec_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Preparamos la lista y configuramos el Adapter
        cardList.forEach { it.isSelected = false }
        if (cardList.isNotEmpty()) {
            cardList[0].isSelected = true
        }
        val adapter = CardsAdapter(cardList)

        // 2. Asignamos el listener al adapter para la selección de tarjetas
        adapter.listener = { position ->
            cardList.forEach { card ->
                card.isSelected = false
            }
            cardList[position].isSelected = true
            adapter.updateList(cardList) // Asumo que este método refresca el adapter
        }

        // 3. Configuramos el RecyclerView
        view.rwCards.adapter = adapter
        // Es buena práctica definir el LayoutManager aquí también
        view.rwCards.layoutManager = GridLayoutManager(context, 3)

        // 4. Añadimos tu validación de decimales al EditText
        view.edtAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && text.contains(".")) {
                    val decimalPart = text.substringAfter(".")
                    if (decimalPart.length > 2) {
                        s?.delete(s.length - 1, s.length)
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 5. Configuramos el click del botón "Aceptar"
        view.tvwAccept.setOnClickListener {
            val amount = view.edtAmount.text.toString()
            val reference = view.edtAmountOther.text.toString()
            val listCard = cardList
            val selectedCard = cardList.firstOrNull { it.isSelected }

            // Devolvemos el resultado a la Actividad a través del listener
            if (selectedCard != null) {
                listener?.onPaymentAccepted(amount, reference, selectedCard,listCard)
            }

            // Cerramos el BottomSheet
            dismiss()
        }
    }


    // --- ESTE ES EL CÓDIGO CLAVE PARA PASAR DATOS ---
    companion object {
        const val TAG = "PaymentBottomSheetFragment"
        private const val CARD_LIST_KEY = "CARD_LIST_KEY"
        fun newInstance(list: ArrayList<SelectedCreditCard>): PaymentBottomSheetFragment {
            val args = Bundle()
            args.putSerializable(CARD_LIST_KEY, list)
            val fragment = PaymentBottomSheetFragment()
            fragment.arguments = args
            return fragment
        }
    }
}