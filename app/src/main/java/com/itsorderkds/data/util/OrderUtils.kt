package com.itsorderkds.data.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.itsorderkds.R
import com.itsorderkds.ui.order.OrderStatusEnum
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object OrderUtils {
        fun getOrderStatusColor(context: Context, statusSlug: OrderStatusEnum?): Int {
            return when (statusSlug) {
                OrderStatusEnum.PROCESSING -> ContextCompat.getColor(context, R.color.order_processing_bg)
                OrderStatusEnum.FAILED -> ContextCompat.getColor(context, R.color.order_failed_bg)
                null -> ContextCompat.getColor(context, R.color.order_default_bg)
                else -> ContextCompat.getColor(context, R.color.order_default_bg)
            }
    }

    fun parseIsoDateToDisplay(iso: String): String {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (f in formats) {
            try {
                val parser = SimpleDateFormat(f, Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(iso)
                if (date != null) {
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    return formatter.format(date)
                }
            } catch (_: Exception) {
            }
        }
        return iso
    }

//    fun filterOtherStatus(orders: List<Order>, statusTabs: List<OrderStatus>): List<Order> {
//        return orders.filterNot { order ->
//            statusTabs.filter { it != OrderStatusEnum.OTHER }
//                .any { it.name == order.orderStatus.slug }
//        }
//    }

}
