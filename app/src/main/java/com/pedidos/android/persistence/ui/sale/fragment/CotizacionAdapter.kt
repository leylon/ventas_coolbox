package com.pedidos.android.persistence.ui.sale.fragment

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.model.cotizacion.CotizacionCab
import com.pedidos.android.persistence.ui.guide.fragment.OnClickListenerCustom
import com.pedidos.android.persistence.utils.Formatter

class CotizacionAdapter: RecyclerView.Adapter<CotizacionAdapter.CotizacionViewHolder>() {

    var listData: List<CotizacionCab>? = null
    var clickListener: OnClickListenerCustom<CotizacionCab>? = null

    fun setDataStorage(listData: List<CotizacionCab>?){
        this.listData = listData
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CotizacionViewHolder {
        clickListener?.let { setOnItemClickListener(it) }
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.item_cotizacion, parent, false)
        return CotizacionViewHolder(v)
    }

    override fun onBindViewHolder(holder: CotizacionViewHolder, position: Int) {
        val dataStorage = listData!![position]
        holder.tvNumeroTicket.text = "${dataStorage.serie}-${dataStorage.numero}"
        holder.tvFecha.text  = Formatter.convertirFecha(dataStorage.fecha)
        holder.tvImporte.text = "${String.format("%.2f",dataStorage.totalNeto)}"
        holder.tvEstado.text = dataStorage.estado ?: "N/A"
        holder.itemView.setOnClickListener {
            if (clickListener != null) {
                clickListener!!.onClickDataListener(dataStorage)
            }
        }
    }

    override fun getItemCount(): Int {
        return listData?.size ?: 0
    }
    fun setOnItemClickListener(clickListener: OnClickListenerCustom<CotizacionCab>){
        this.clickListener = clickListener
    }
    class CotizacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val tvNumeroTicket : TextView = itemView.findViewById(R.id.tvNumeroTicket)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvImporte: TextView = itemView.findViewById(R.id.tvImporte)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
    }
}