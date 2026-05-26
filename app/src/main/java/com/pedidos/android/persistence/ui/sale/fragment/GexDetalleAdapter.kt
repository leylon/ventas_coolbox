package com.pedidos.android.persistence.ui.sale.fragment

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pedidos.android.persistence.R
import com.pedidos.android.persistence.model.firma.FirmaDetail

class GexDetalleAdapter(
    private var listaArticulos: MutableList<FirmaDetail>,
    private val alEliminar: (FirmaDetail, Int) -> Unit // Pasamos el modelo real al eliminar
) : RecyclerView.Adapter<GexDetalleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSku: TextView = view.findViewById(R.id.tvSku)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvPrecioItem: TextView = view.findViewById(R.id.tvPrecioItem)
        val tvPrecioGex: TextView = view.findViewById(R.id.tvPrecioGex)
        val ivEliminar: ImageView = view.findViewById(R.id.ivEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detalle_gex, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listaArticulos[position]

        // Mapeamos los datos de FirmaDetail a los TextViews,
        // usando "?: valor_por_defecto" por si vienen nulos

        holder.tvSku.text = "SKU: ${item.coItem ?: "Sin código"}"
        holder.tvDesc.text = item.deItem ?: "Artículo sin descripción"

        val precioItem = item.prVentCimp ?: 0.0
        holder.tvPrecioItem.text = "S/ ${String.format("%.2f", precioItem)}"

        val precioGex = item.prGex ?: 0.0
        holder.tvPrecioGex.text = "S/ ${String.format("%.2f", precioGex)}"

        if(precioGex == 0.0){
            holder.ivEliminar.visibility = View.GONE
        } else {
            holder.ivEliminar.visibility = View.VISIBLE
        }

        holder.ivEliminar.setOnClickListener {
            // Pasamos el item completo a la Activity para que sepa qué borrar
            alEliminar(item, position)
        }
    }

    override fun getItemCount() = listaArticulos.size

    fun actualizarLista(nuevaLista: MutableList<FirmaDetail>) {
        listaArticulos = nuevaLista
        notifyDataSetChanged()
    }
}