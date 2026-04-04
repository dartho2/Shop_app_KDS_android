package com.itsorderkds.ui.settings.print

import android.content.Context
import com.itsorderkds.util.AppPrefs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds formatted ticket string per production layout */
class TicketFormatter {
    fun buildTicket(ctx: Context, order: OrderPrintModel, cfg: PrinterConfig): String {
        // ctx is reserved for potential future localization; suppress unused warning
        @Suppress("UNUSED_PARAMETER") val _unused = ctx
        val sb = StringBuilder()
        val currency = AppPrefs.getCurrency()
        val lineChars = cfg.lineChars

        // Header
        sb.append("[C]<font size='wide'><b>${escape(order.orderNumber)}</b></font>\n")
        sb.append(separator(lineChars))
        sb.append("[L]${formatDate(order.createdAt)}\n")
        order.consumer?.let {
            if (!it.name.isNullOrBlank()) sb.append("[L]${maskMiddle(it.name)}\n")
            if (!it.phone.isNullOrBlank()) sb.append("[L]${maskPhone(it.phone)}\n")
        }
        sb.append(separator(lineChars))
        sb.append("[C]${formatDeliveryTime(order.deliveryTime, order.isAsap == true)}\n")
        if (order.deliveryType == DeliveryType.DELIVERY || order.deliveryType == DeliveryType.DELIVERY_EXTERNAL) {
            order.shippingAddress?.let { addr ->
                val line = listOfNotNull(addr.street, addr.numberHome, addr.numberFlat).joinToString(" ")
                if (line.isNotBlank()) sb.append("[L]${escape(line)}\n")
                if (!addr.city.isNullOrBlank()) sb.append("[L]${escape(addr.city)}\n")
            }
            sb.append(separator(lineChars))
        }

        // Items
        var subtotal = 0.0
        order.products.forEach { p ->
            val total = (p.unitPrice ?: 0.0) * (p.quantity ?: 0.0)
            subtotal += total
            val left = "${p.quantity ?: 0.0} x ${escape(p.name ?: "")}".trim()
            val right = fmtMoney(total, currency)
            sb.append(composeLeftRight(left, right, lineChars)).append('\n')
            if (!p.comment.isNullOrBlank()) {
                sb.append("[L]    <u>${escape(p.comment)}</u>\n")
            }
        }

        // Totals
        sb.append(separator(lineChars))
        sb.append(composeLeftRight("Subtotal", fmtMoney(subtotal, currency), lineChars)).append('\n')
        val ship = order.shippingTotal ?: 0.0
        if (ship > 0) sb.append(composeLeftRight("Delivery", fmtMoney(ship, currency), lineChars)).append('\n')
        sb.append(composeLeftRight("Total", "<b>${fmtMoney(order.total ?: (subtotal + ship), currency)}</b>", lineChars)).append('\n')
        sb.append(separator(lineChars))

        if (!order.note.isNullOrBlank()) {
            sb.append("[L]Notes: ${escape(order.note)}\n")
            sb.append(separator(lineChars))
        }

        sb.append("[C]<b>Thank you</b>\n\n")
        return sb.toString()
    }

    // Public helper: left-right alignment within fixed width
    fun composeLeftRight(left: String, right: String, width: Int): String {
        val cleanLeft = left.replace("\n", " ").replace("\r", " ").trim()
        val cleanRight = right.replace("\n", " ").replace("\r", " ").trim()
        if (cleanLeft.length + cleanRight.length + 1 <= width) {
            val spaces = width - cleanLeft.length - cleanRight.length
            return "[L]$cleanLeft" + " ".repeat(spaces) + cleanRight
        }
        // Wrap left part on word boundaries
        val wrapped = wrapWords(cleanLeft, width)
        val first = wrapped.firstOrNull() ?: ""
        val rest = wrapped.drop(1)
        val spacesFirst = (width - first.length - cleanRight.length).coerceAtLeast(1)
        val sb = StringBuilder()
        sb.append("[L]").append(first).append(" ".repeat(spacesFirst)).append(cleanRight)
        for (line in rest) {
            sb.append('\n').append("[L]").append(line)
        }
        return sb.toString()
    }

    // Word wrap respecting width
    private fun wrapWords(text: String, width: Int): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (w in words) {
            val candidate = if (current.isEmpty()) w else current.toString() + " " + w
            if (candidate.length <= width) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    private fun separator(lineChars: Int) = "[L]" + "-".repeat(lineChars) + "\n"
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun maskMiddle(s: String) = when {
        s.length <= 4 -> s
        else -> s.take(2) + "*".repeat(s.length - 4) + s.takeLast(2)
    }
    private fun maskPhone(p: String) = p.replace(Regex("[\n\r\t]"), " ")
    private fun fmtMoney(v: Double, currency: String) = String.format(Locale.getDefault(), "%.2f %s", v, currency)
    private fun formatDate(dateIso: String?): String {
        if (dateIso.isNullOrBlank()) return "-"
        return try {
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(dateIso))
        } catch (_: Exception) {
            dateIso
        }
    }
    private fun formatDeliveryTime(dateIso: String?, asap: Boolean): String {
        return if (asap) "ASAP" else formatDate(dateIso)
    }
}

// Data models simplified for printing
data class PrinterConfig(
    val encodingName: String,
    val codepageNumber: Int?,
    val hasCutter: Boolean,
    val lineChars: Int = 32
)

data class OrderPrintModel(
    val orderNumber: String,
    val createdAt: String?,
    val consumer: Consumer?,
    val deliveryType: DeliveryType?,
    val isAsap: Boolean?,
    val deliveryTime: String?,
    val shippingAddress: Address?,
    val products: List<Product>,
    val shippingTotal: Double?,
    val total: Double?,
    val note: String?
)

data class Consumer(val name: String?, val phone: String?)

data class Address(val street: String?, val numberHome: String?, val numberFlat: String?, val city: String?)

data class Product(val name: String?, val quantity: Double?, val unitPrice: Double?, val comment: String?)

enum class DeliveryType { DELIVERY, DELIVERY_EXTERNAL, PICKUP, PICK_UP, LOCAL_PICKUP, FLAT_RATE, UNKNOWN }
