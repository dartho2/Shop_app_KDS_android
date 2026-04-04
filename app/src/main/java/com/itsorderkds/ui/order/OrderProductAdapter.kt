package com.itsorderkds.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.itsorderkds.R
import com.itsorderkds.data.model.OrderProduct

class OrderProductAdapter(
    private val items: List<OrderProduct>
) : RecyclerView.Adapter<OrderProductAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvProductName)
        val qty: TextView = view.findViewById(R.id.tvProductQuantity)
        val price: TextView = view.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_order_product, parent, false
        )
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val p = items[pos]
        holder.name.text = p.name
        holder.qty.text = "x${p.quantity}"

        // Jeśli cena jest w groszach, podziel przez 100.0 i sformatuj
        val priceDouble = try { p.price.toDouble() / 100.0 } catch (e: Exception) { 0.0 }
        holder.price.text = String.format("%.2f", priceDouble)
    }
}
