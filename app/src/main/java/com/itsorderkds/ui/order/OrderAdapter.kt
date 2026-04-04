package com.itsorderkds.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.util.OrderUtils
import com.itsorderkds.databinding.ItemOrderBinding
import com.itsorderkds.util.extensions.orDash
import java.util.Locale

class OrdersAdapter(
    private val onOrderAction: (Order, OrderAction) -> Unit = { _, _ -> }
) : ListAdapter<Order, OrdersAdapter.OrderViewHolder>(OrderDiffCallback()) {

    private var expandedOrderId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = getItem(position)
        holder.bind(order)
        holder.itemView.setOnClickListener {
            val prevExpandedOrderId = expandedOrderId
            expandedOrderId = if (expandedOrderId == order.orderId) null else order.orderId

            // Odśwież stare i nowe pozycje
            prevExpandedOrderId?.let { prevId ->
                val prevIndex = currentList.indexOfFirst { it.orderId == prevId }
                if (prevIndex != -1) notifyItemChanged(prevIndex)
            }
            notifyItemChanged(holder.bindingAdapterPosition)
        }
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val context = binding.root.context

            // Konwersja slug z String? na OrderStatusEnum?
            val slugEnum = order.orderStatus.slug?.let {
                runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
            }

            val bgColor = OrderUtils.getOrderStatusColor(context, slugEnum)
            binding.cardOrderContent.setBackgroundColor(bgColor)
            binding.root.visibility =
                if (slugEnum == OrderStatusEnum.FAILED) View.GONE else View.VISIBLE

            binding.tvOrderNumber.text = order.orderNumber.orDash()
            binding.tvOrderName.text = order.consumer?.name.orDash()
            val total = order.total
            binding.tvOrderTotal.text = String.format(Locale.getDefault(), "%.2f", total)

            binding.cardOrderContent.setOnClickListener { onOrderAction(order, OrderAction.DETAILS) }
        }
    }
}
private class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
    override fun areItemsTheSame(old: Order, new: Order): Boolean = old.orderId == new.orderId
    override fun areContentsTheSame(old: Order, new: Order): Boolean = old == new
}
