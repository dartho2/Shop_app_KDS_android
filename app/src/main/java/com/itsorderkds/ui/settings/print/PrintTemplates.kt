package com.itsorderkds.ui.settings.print

import android.content.Context
import com.itsorderkds.R
import com.itsorderkds.data.model.Order
import com.itsorderkds.ui.order.OrderDelivery
import com.itsorderkds.ui.order.PaymentMethod
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.util.AppPrefs
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Definicja szablonów wydruku (recipes) dla paragonu.
 */
enum class PrintTemplate(val id: String, val displayName: String) {
    STANDARD("template_standard", "Standardowy"),
    COMPACT("template_compact", "Kompaktowy"),
    DETAILED("template_detailed", "Szczegółowy"),
    MINIMAL("template_minimal", "Minimalny");

    companion object {
        fun fromId(id: String?): PrintTemplate = entries.find { it.id == id } ?: STANDARD
        fun getAll(): List<PrintTemplate> = entries.toList()
    }
}

/**
 * Builder paragonu - wybiera szablon na podstawie preferencji.
 */
object PrintTemplateFactory {

    fun buildTicket(ctx: Context, order: Order, template: PrintTemplate = PrintTemplate.STANDARD, useDeliveryInterval: Boolean = false): String {
        return when (template) {
            PrintTemplate.STANDARD -> buildStandardTicket(ctx, order, useDeliveryInterval)
            PrintTemplate.COMPACT -> buildCompactTicket(ctx, order, useDeliveryInterval)
            PrintTemplate.DETAILED -> buildDetailedTicket(ctx, order, useDeliveryInterval)
            PrintTemplate.MINIMAL -> buildMinimalTicket(order)
        }
    }

    /**
     * STANDARDOWY - obecny szablon (pełny, czytelny)
     */
    private fun buildStandardTicket(ctx: Context, order: Order, useDeliveryInterval: Boolean = false): String {
        // Importuj oryginalną funkcję z TicketTemplate.kt
        return com.itsorderkds.ui.settings.print.buildTicket(
            ctx = ctx,
            order = order,
            config = com.itsorderkds.data.model.TemplateConfig.standard(),
            useDeliveryInterval = useDeliveryInterval
        )
    }

    /**
     * KOMPAKTOWY - zmniejszony, bez adresu, nazwy kuriera
     */
    private fun buildCompactTicket(ctx: Context, order: Order, useDeliveryInterval: Boolean = false): String {
        val now = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
        val currency = AppPrefs.getCurrency()

        val deliveryLabel = when (order.deliveryType) {
            OrderDelivery.DELIVERY, OrderDelivery.DELIVERY_EXTERNAL, OrderDelivery.FLAT_RATE ->
                ctx.getString(R.string.delivery_label_delivery)
            OrderDelivery.PICKUP, OrderDelivery.PICK_UP, OrderDelivery.LOCAL_PICKUP ->
                ctx.getString(R.string.delivery_label_pickup)
            else -> ctx.getString(R.string.delivery_label_unknown)
        }

        // Logo źródła zamówienia
        val sourceLabel = getSourceLabel(order.source?.name)

        val header = buildString {
            appendLine("[C]<font size='wide'><b>${order.orderNumber}</b></font>")

            // Dodaj źródło jeśli istnieje (z formatowaniem)
            if (sourceLabel.isNotBlank()) {
                appendLine("[C]<font size='tall'><b>$sourceLabel</b></font>")
            }

            appendLine("[C]<font size='wide'>$deliveryLabel</font>")
            appendLine("[L]--------------------------------")
            appendLine("[L]$now")

            if (useDeliveryInterval) {
                appendLine("[L]${ctx.getString(R.string.customer_waiting)}")
            }
            append("[L]--------------------------------")
        }

        val body = buildString {
            for (p in order.products) {
                val total = p.price
                val priceStr = formatMoney(total, currency)
                appendLine("[L]${p.quantity}x ${p.name} $priceStr")
            }
        }.trimEnd()

        val total = order.total + (order.shippingTotal ?: 0.0)
        val totals = buildString {
            appendLine("[L]--------------------------------")
            appendLine("[L]<b>${ctx.getString(R.string.line_total)}: ${formatMoney(total, currency)}</b>")

            // Metoda płatności - POGRUBIONA pod kwotą
            val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
            if (paymentMethodLabel.isNotBlank()) {
                appendLine("[L]<b>${ctx.getString(R.string.label_payment)}: $paymentMethodLabel</b>")
            }

            append("[L]--------------------------------")
        }

        val footer = "[C]${ctx.getString(R.string.footer_thanks)}\n[C]"

        // Notatki na samym dole
        val notesLine = order.note?.takeIf { it.isNotBlank() }?.let {
            "\n[L]--------------------------------\n[L]${ctx.getString(R.string.label_notes)}: $it"
        } ?: ""

        return "$header\n\n$body\n\n$totals\n\n$footer$notesLine"
    }

    /**
     * SZCZEGÓŁOWY - rozszerzona wersja ze statystykami
     */
    private fun buildDetailedTicket(ctx: Context, order: Order, useDeliveryInterval: Boolean = false): String {
        val baseTicket = com.itsorderkds.ui.settings.print.buildTicket(
            ctx = ctx,
            order = order,
            config = com.itsorderkds.data.model.TemplateConfig.standard(),
            useDeliveryInterval = useDeliveryInterval
        )

        val details = buildString {
            appendLine("\n[L]================================")
            appendLine("[C]<b>SZCZEGÓŁY ZAMÓWIENIA</b>")
            appendLine("[L]================================")
            appendLine("[L]Numer: ${order.orderNumber}")
            appendLine("[L]Ilość pozycji: ${order.products.size}")
            val createdTime = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(order.createdAt ?: Date())
            appendLine("[L]Utworzono: $createdTime")
            // Note jest już na dole w baseTicket, nie duplikujemy
            appendLine("[L]================================")
            append("")
        }

        return baseTicket + details
    }

    /**
     * MINIMALNY - tylko niezbędne informacje
     */
    private fun buildMinimalTicket(order: Order): String {
        val currency = AppPrefs.getCurrency()

        // Logo źródła zamówienia
        val sourceLabel = getSourceLabel(order.source?.name)

        return buildString {
            appendLine("[C]<font size='wide'><b>${order.orderNumber}</b></font>")

            // Dodaj źródło jeśli istnieje (z formatowaniem)
            if (sourceLabel.isNotBlank()) {
                appendLine("[C]<font size='tall'><b>$sourceLabel</b></font>")
            }

            appendLine("[L]---")
            for (p in order.products) {
                val total = p.price
                val priceStr = formatMoney(total, currency)
                appendLine("[L]${p.quantity}x ${p.name ?: "?"} $priceStr")
            }
            val total = order.total + (order.shippingTotal ?: 0.0)
            appendLine("[L]---")
            appendLine("[L]<b>${formatMoney(total, currency)}</b>")

            // Metoda płatności - POGRUBIONA
            val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
            if (paymentMethodLabel.isNotBlank()) {
                appendLine("[L]<b>$paymentMethodLabel</b>")
            }

            // Notatki na samym dole
            if (!order.note.isNullOrBlank()) {
                appendLine("[L]---")
                appendLine("[L]${order.note}")
            }
        }
    }

    /**
     * Helper: formatowanie kwoty
     */
    private fun formatMoney(value: Double, currency: String): String {
        val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        val cents = bd.remainder(BigDecimal.ONE).movePointRight(2).abs().toInt()
        val decSym = DecimalFormatSymbols(Locale.getDefault())
        val fmt = if (cents == 0) DecimalFormat("#,##0", decSym) else DecimalFormat("#,##0.00", decSym)
        val base = fmt.format(bd)
        return if (currency.isBlank()) base else "$base $currency"
    }

    /**
     * Helper: zwraca tekstową nazwę źródła zamówienia dla paragonu
     */
    private fun getSourceLabel(source: SourceEnum?): String {
        return when (source) {
            SourceEnum.UBER -> "[UBER EATS]"
            SourceEnum.WOLT -> "[WOLT]"
            SourceEnum.GLOVO -> "[GLOVO]"
            SourceEnum.BOLT -> "[BOLT FOOD]"
            SourceEnum.TAKEAWAY -> "[TAKEAWAY]"
            SourceEnum.GOPOS -> "[GOPOS]"
            SourceEnum.WOOCOMMERCE,
            SourceEnum.WOO -> "[WOOCOMMERCE]"
            SourceEnum.ITS -> "[ITS ORDER]"
            SourceEnum.OTHER,
            SourceEnum.UNKNOWN,
            null -> "" // Brak źródła - nie wyświetlaj nic
        }
    }

    /**
     * Helper: zwraca czytelną nazwę metody płatności
     */
    private fun getPaymentMethodLabel(paymentMethod: PaymentMethod?): String {
        return when (paymentMethod) {
            PaymentMethod.COD,
            PaymentMethod.CASH_ON_DELIVERY -> "Gotówka przy odbiorze"
            PaymentMethod.CASH -> "Gotówka"
            PaymentMethod.CARD -> "Karta"
            PaymentMethod.ONLINE -> "Płatność online"
            PaymentMethod.PAYNOW -> "PayNow"
            PaymentMethod.BLIK -> "BLIK"
            PaymentMethod.BANK_TRANSFER,
            PaymentMethod.TRANSFER -> "Przelew"
            PaymentMethod.UNKNOWN,
            null -> "" // Brak metody - nie wyświetlaj
        }
    }
}

