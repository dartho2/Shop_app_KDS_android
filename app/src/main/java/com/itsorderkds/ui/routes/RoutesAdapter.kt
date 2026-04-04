package com.itsorderkds.ui.routes

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.itsorderkds.R
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.databinding.ItemRouteBinding
import com.itsorderkds.ui.order.OrderStatusEnum
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.abs

class RoutesAdapter(
    private val onSelectionChanged: (selectedCount: Int, selectedItems: OrderStatusEnum?) -> Unit
) : ListAdapter<OrderTras, RoutesAdapter.RouteViewHolder>(DiffCallback()) {

    private val selectedItems = mutableSetOf<String>()
    private var currentSelection: Set<String> = emptySet()

    companion object {
        private val inputFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
        private val outputTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private const val UNAVAILABLE_TIME = "N/A"
        private const val FORMAT_ERROR = "Błąd"
    }
    fun getSelectedItems(): Set<String> = selectedItems

    /** Zwraca zbiór `orderId` dla zaznaczonych elementów, gotowy do wysłania do API. */
    fun getSelectedOrderIds(): Set<String> {
        return currentList
            .filter { selectedItems.contains(it.orderId) }
            .map { it.orderId }
            .toSet()
    }

    /** Czyści stan zaznaczenia w adapterze. */
    fun clearSelections() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    /**  Zwraca listę orderId, które trzeba odświeżyć  */
    private fun toggleSelection(item: OrderTras): List<String> {
        if (item.orderNumber.equals("RETURN", true) || item.status == OrderStatusEnum.COMPLETED)
            return emptyList()

        val touched = mutableListOf<String>()
        val isSelected = selectedItems.contains(item.orderId)

        if (isSelected) {
            selectedItems.remove(item.orderId)
            touched += item.orderId
        } else {
            val representative = selectedItems.firstOrNull()
                ?.let { id -> currentList.find { it.orderId == id }?.status }

            if (representative == null || representative == item.status) {
                selectedItems.add(item.orderId)
                touched += item.orderId
            } else {
                // wyczyść poprzednią grupę – trzeba odświeżyć WSZYSTKIE stare
                touched += selectedItems
                selectedItems.clear()
                selectedItems.add(item.orderId)
                touched += item.orderId
            }
        }
        return touched
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = getItem(position)
        // Sprawdzamy, czy ID elementu jest w zbiorze, który otrzymaliśmy z zewnątrz
        holder.bind(route, position, itemCount, currentSelection.contains(route.orderId))
    }

    inner class RouteViewHolder(private val binding: ItemRouteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: OrderTras, position: Int, totalItems: Int, isSelected: Boolean) {
            val isSelected = selectedItems.contains(route.orderId)   // ← ZAWSZE patrzymy w selectedItems
            binding.selectionCheckmark.apply {
                visibility = if (isSelected) View.VISIBLE else View.GONE

                // zmiana koloru
                val tintColor = if (isSelected)
                    R.color.order_failed_bg
                else
                    R.color.order_failed_bg

                ImageViewCompat.setImageTintList(
                    this,
                    ColorStateList.valueOf(
                        ContextCompat.getColor(context, tintColor)
                    )
                )
            }
            binding.cardOrder.setOnClickListener {
                val changedIds = toggleSelection(route)

                // Odmaluj tylko dotknięte wiersze
                changedIds.forEach { id ->
                    currentList.indexOfFirst { it.orderId == id }
                        .takeIf { it != -1 }
                        ?.let { pos -> notifyItemChanged(pos) }
                }

                val currentStatus = selectedItems.firstOrNull()
                    ?.let { id -> currentList.find { it.orderId == id }?.status }

                onSelectionChanged.invoke(selectedItems.size, currentStatus)
            }

            binding.timelineLineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            binding.timelineLineBottom.visibility = if (position == totalItems - 1) View.INVISIBLE else View.VISIBLE

            if (route.orderNumber.equals("RETURN", ignoreCase = true)) {
                bindReturnState(route)
            } else {
                bindOrderState(route)
            }
        }

        private fun bindReturnState(route: OrderTras) = with(binding) {
            val ctx = itemView.context

            textViewOrderNumber.text =
                ctx.getString(R.string.route_return_to_restaurant)

            timelineMarker.setImageResource(R.drawable.storefront)
            timelineMarker.setColorFilter(
                ContextCompat.getColor(ctx, R.color.status_black))

            // widoczność
            listOf(textViewAddress, textPlannedTime, textDelayMinutes,
                textViewDistance, textViewStatus).forEach { it.visibility = View.GONE }

            textViewEta.visibility              = View.VISIBLE
            textEstimatedStartTime.visibility   = View.VISIBLE

            textViewEta.text        = ctx.getString(
                R.string.route_eta, formatDateToTime(route.eta))

            textEstimatedStartTime.text = ctx.getString(
                R.string.route_start, formatDateToTime(route.estimatedStartTime))
        }

        private fun bindOrderState(route: OrderTras) = with(binding) {
            val ctx = itemView.context

            // widoczność
            listOf(textViewAddress, textPlannedTime, textDelayMinutes,
                textViewDistance, textViewStatus,
                textViewEta, textEstimatedStartTime)
                .forEach { it.visibility = View.VISIBLE }

            textViewOrderNumber.text = route.orderNumber

            textViewAddress.text = route.address

            textViewDistance.text = route.distance.toString()

            textViewStatus.text =  route.status.name

            textViewEta.text = ctx.getString(
                R.string.route_eta, formatDateToTime(route.eta))

            if( !route.isAsap) {
                textPlannedTime.text = ctx.getString(
                    R.string.route_planned, formatDateToTime(route.plannedDeliveryTime)
                )
            }   else {
                textPlannedTime.text = ctx.getString(
                    R.string.asap, formatDateToTime(route.plannedDeliveryTime) )
            }

            textEstimatedStartTime.text = ctx.getString(
                R.string.route_start, formatDateToTime(route.estimatedStartTime))

            textDelayMinutes.text = when {
                route.delayMinutes > 0 ->
                    ctx.getString(R.string.route_delay_late, route.delayMinutes)
                route.delayMinutes < 0 ->
                    ctx.getString(R.string.route_delay_early, abs(route.delayMinutes))
                else ->
                    ctx.getString(R.string.route_delay_ontime)
            }

            // marker kolor/ikona wg statusu
            when (route.status) {
                OrderStatusEnum.ACCEPTED -> {
                    timelineMarker.setImageResource(R.drawable.ic_timeline_marker_out)
                    timelineMarker.setColorFilter(ContextCompat.getColor(itemView.context, R.color.order_processing_bg))
                }
                OrderStatusEnum.OUT_FOR_DELIVERY -> {
                    timelineMarker.setImageResource(R.drawable.ic_timeline_marker)
                    timelineLineTop.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.order_processing_bg))
                    timelineLineBottom.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.order_processing_bg))
                    timelineMarker.setColorFilter(ContextCompat.getColor(itemView.context, R.color.order_processing_bg))
                }
                OrderStatusEnum.COMPLETED -> {
                    timelineMarker.setImageResource(R.drawable.ic_timeline_marker)
                    timelineMarker.setColorFilter(ContextCompat.getColor(itemView.context, R.color.order_default_bg))
                }
                else -> { // Dla PENDING, PROCESSING i null
                    timelineMarker.setImageResource(R.drawable.ic_timeline_marker)
                    timelineMarker.setColorFilter(ContextCompat.getColor(itemView.context, R.color.order_default_bg))
                }
            }
        }
    }


    private fun formatDateToTime(dateString: String?): String {
        if (dateString.isNullOrBlank()) return UNAVAILABLE_TIME
        return try {
            ZonedDateTime.parse(dateString, inputFormatter).format(outputTimeFormatter)
        } catch (e: DateTimeParseException) {
            FORMAT_ERROR
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrderTras>() {
        override fun areItemsTheSame(oldItem: OrderTras, newItem: OrderTras): Boolean = oldItem.orderId == newItem.orderId
        override fun areContentsTheSame(oldItem: OrderTras, newItem: OrderTras): Boolean = oldItem == newItem
    }
}