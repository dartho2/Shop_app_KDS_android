package com.itsorderkds.ui.settings.print

import android.content.Context
import com.itsorderkds.R
import com.itsorderkds.data.model.KdsTicketItem
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderProduct
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
    MINIMAL("template_minimal", "Minimalny"),
    /**
     * Kuchenny — drukuje TYLKO pozycje z printer=KITCHEN.
     * Używany gdy API zwraca różne typy pozycji (KITCHEN vs STANDARD).
     * Nie zawiera cen ani sum — tylko pozycje do przygotowania.
     */
    KITCHEN_ONLY("template_kitchen_only", "Kuchenny (tylko KITCHEN)");

    companion object {
        fun fromId(id: String?): PrintTemplate = entries.find { it.id == id } ?: STANDARD
        fun getAll(): List<PrintTemplate> = entries.toList()
    }
}

/**
 * Builder paragonu - wybiera szablon na podstawie preferencji.
 */
object PrintTemplateFactory {

    /**
     * Standardowy builder — używa modelu Order (pełny paragon / bloczek).
     * Brak filtrowania po printer= bo model Order nie ma tej informacji.
     */
    fun buildTicket(
        ctx: Context,
        order: Order,
        template: PrintTemplate = PrintTemplate.STANDARD,
        useDeliveryInterval: Boolean = false
    ): String {
        return when (template) {
            PrintTemplate.STANDARD    -> buildStandardTicket(ctx, order, useDeliveryInterval)
            PrintTemplate.COMPACT     -> buildCompactTicket(ctx, order, useDeliveryInterval)
            PrintTemplate.DETAILED    -> buildDetailedTicket(ctx, order, useDeliveryInterval)
            PrintTemplate.MINIMAL     -> buildMinimalTicket(order)
            // KITCHEN_ONLY bez items → fallback na standardowy kuchenny
            PrintTemplate.KITCHEN_ONLY -> buildStandardTicket(ctx, order, useDeliveryInterval)
        }
    }

    /**
     * Builder kuchenny z filtrowaniem po printer=KITCHEN.
     *
     * Przyjmuje [items] (z API ticketu KDS) i drukuje wyłącznie pozycje
     * które mają [KdsTicketItem.printer] == "KITCHEN".
     *
     * Gdy [items] jest puste lub żadna pozycja nie ma printer=KITCHEN,
     * drukowane są wszystkie pozycje (fallback — kompatybilność wsteczna,
     * np. gdy API jeszcze nie zwraca pola printer).
     *
     * @param ctx Context aplikacji
     * @param order Zamówienie (nagłówek, adres, czas)
     * @param items Lista pozycji ticketu KDS (z polem printer)
     * @param useDeliveryInterval jeśli true używa deliveryInterval zamiast deliveryTime
     */
    fun buildKitchenOnlyTicket(
        ctx: Context,
        order: Order,
        items: List<KdsTicketItem>,
        useDeliveryInterval: Boolean = false
    ): String {
        // Filtruj: tylko KITCHEN. Jeśli żadna nie ma printer=KITCHEN → drukuj wszystkie (fallback)
        val kitchenItems = items.filter { it.isKitchenItem() }
        val effectiveItems = kitchenItems.ifEmpty {
            // Fallback: brak pola printer w API lub wszystkie bez oznaczenia → drukuj wszystko
            android.util.Log.w(
                "KITCHEN_ONLY",
                "⚠️ Żadna pozycja nie ma printer=KITCHEN " +
                "(total=${items.size}) — fallback: drukuję wszystkie"
            )
            items
        }

        android.util.Log.d(
            "KITCHEN_ONLY",
            "🖨️ KITCHEN_ONLY: ${effectiveItems.size}/${items.size} pozycji " +
            "(orderId=${order.orderId})"
        )

        // Konwertuj KdsTicketItem → OrderProduct (tylko pola potrzebne do wydruku)
        val kitchenProducts: List<OrderProduct> = effectiveItems
            .sortedBy { it.sequence }
            .map { item ->
                OrderProduct(
                    name        = item.displayName,
                    quantity    = item.qty,
                    price       = 0.0,   // brak cen na bloczku kuchennym
                    salePrice   = 0.0,
                    discount    = 0.0,
                    comment     = null,
                    addonsGroup = emptyList(),
                    note        = item.notes.filter { it.isNotBlank() }
                )
            }

        // Zbuduj uproszczone zamówienie tylko z kuchennymi pozycjami
        val kitchenOrder = order.copy(
            products = kitchenProducts,
            // Brak sum na bloczku kuchennym
            total         = 0.0,
            shippingTotal = 0.0,
            additionalFeeTotal = null
        )

        return buildKitchenTicketBody(kitchenOrder, useDeliveryInterval)
    }

    /**
     * Kuchenny layout bloczka — bez cen, bez sum, skupiony na pozycjach.
     * Nagłówek: numer, typ zamówienia, czas, źródło.
     * Body: lista pozycji z ilościami i notatkami.
     * Brak stopki z sumami i metodą płatności.
     */
    private fun buildKitchenTicketBody(
        order: Order,
        useDeliveryInterval: Boolean = false
    ): String {
        val now = SimpleDateFormat("HH:mm dd.MM", Locale.getDefault()).format(Date())

        val deliveryLabel = when (order.deliveryType) {
            OrderDelivery.DELIVERY,
            OrderDelivery.DELIVERY_EXTERNAL,
            OrderDelivery.FLAT_RATE -> "DOSTAWA"
            OrderDelivery.PICKUP,
            OrderDelivery.PICK_UP,
            OrderDelivery.LOCAL_PICKUP -> "WYNOS"
            OrderDelivery.DINE_IN,
            OrderDelivery.ROOM_SERVICE -> "NA MIEJSCU"
            else -> ""
        }

        // Czas zamówienia / zaplanowania
        val timeDisplay = when {
            useDeliveryInterval && !order.deliveryInterval.isNullOrBlank() -> order.deliveryInterval!!
            !order.deliveryTime.isNullOrBlank() -> order.deliveryTime!!
            !order.deliveryInterval.isNullOrBlank() -> order.deliveryInterval!!
            order.isAsap == true -> "NA JUZ"
            else -> ""
        }

        // Źródło zamówienia
        val sourceLabel = getKitchenSourceLabel(order.source?.name)

        val header = buildString {
            appendLine("[C]<font size='wide'><b>${order.orderNumber ?: "B/N"}</b></font>")
            if (sourceLabel.isNotBlank()) {
                appendLine("[C]<b>$sourceLabel</b>")
            }
            if (deliveryLabel.isNotBlank()) {
                appendLine("[C]<font size='wide'><b>$deliveryLabel</b></font>")
            }
            appendLine("[L]--------------------------------")
            appendLine("[L]$now")
            if (timeDisplay.isNotBlank()) {
                appendLine("[C]<font size='wide'><b>$timeDisplay</b></font>")
            }
            append("[L]--------------------------------")
        }

        // Pozycje — bez cen, z notatkami pogrubionymi
        val body = buildString {
            for (p in order.products) {
                // Linia produktu: ilość × nazwa
                val qtyStr = "${p.quantity}x "
                appendLine("[L]<b>$qtyStr${p.name ?: "?"}</b>")

                // Notatki / modyfikacje pogrubione pod produktem
                p.note?.forEach { note ->
                    if (note.isNotBlank()) {
                        appendLine("[L]    <b>! $note</b>")
                    }
                }
            }
        }.trimEnd()

        // Uwaga ogólna do zamówienia
        val notesLine = order.note?.takeIf { it.isNotBlank() }?.let {
            "\n[L]--------------------------------\n[L]<b>UWAGA: $it</b>"
        } ?: ""

        return "$header\n\n$body$notesLine\n[L]================================\n\n\n"
    }

    private fun getKitchenSourceLabel(source: com.itsorderkds.ui.order.SourceEnum?): String =
        when (source) {
            SourceEnum.UBER       -> "UBER EATS"
            SourceEnum.WOLT       -> "WOLT"
            SourceEnum.GLOVO      -> "GLOVO"
            SourceEnum.BOLT       -> "BOLT FOOD"
            SourceEnum.TAKEAWAY   -> "PYSZNE"
            SourceEnum.WOOCOMMERCE,
            SourceEnum.WOO        -> "WWW"
            SourceEnum.GOPOS      -> "GOPOS"
            SourceEnum.ITS        -> "ITS ORDER"
            else                  -> ""
        }

    // ─── Pozostałe szablony (bez zmian) ─────────────────────────────────────

    private fun buildStandardTicket(ctx: Context, order: Order, useDeliveryInterval: Boolean = false): String {
        return com.itsorderkds.ui.settings.print.buildTicket(
            ctx = ctx,
            order = order,
            config = com.itsorderkds.data.model.TemplateConfig.standard(),
            useDeliveryInterval = useDeliveryInterval
        )
    }

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

        val sourceLabel = getSourceLabel(order.source?.name)

        val header = buildString {
            appendLine("[C]<font size='wide'><b>${order.orderNumber}</b></font>")
            if (sourceLabel.isNotBlank()) appendLine("[C]<font size='tall'><b>$sourceLabel</b></font>")
            appendLine("[C]<font size='wide'>$deliveryLabel</font>")
            appendLine("[L]--------------------------------")
            appendLine("[L]$now")
            if (useDeliveryInterval) appendLine("[L]${ctx.getString(R.string.customer_waiting)}")
            append("[L]--------------------------------")
        }

        val body = buildString {
            for (p in order.products) {
                val priceStr = formatMoney(p.price, currency)
                appendLine("[L]${p.quantity}x ${p.name} $priceStr")
            }
        }.trimEnd()

        val total = order.total + (order.shippingTotal ?: 0.0)
        val totals = buildString {
            appendLine("[L]--------------------------------")
            appendLine("[L]<b>${ctx.getString(R.string.line_total)}: ${formatMoney(total, currency)}</b>")
            val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
            if (paymentMethodLabel.isNotBlank()) appendLine("[L]<b>${ctx.getString(R.string.label_payment)}: $paymentMethodLabel</b>")
            append("[L]--------------------------------")
        }

        val footer = "[C]${ctx.getString(R.string.footer_thanks)}\n[C]"
        val notesLine = order.note?.takeIf { it.isNotBlank() }?.let {
            "\n[L]--------------------------------\n[L]${ctx.getString(R.string.label_notes)}: $it"
        } ?: ""

        return "$header\n\n$body\n\n$totals\n\n$footer$notesLine"
    }

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
            appendLine("[L]================================")
            append("")
        }
        return baseTicket + details
    }

    private fun buildMinimalTicket(order: Order): String {
        val currency = AppPrefs.getCurrency()
        val sourceLabel = getSourceLabel(order.source?.name)
        return buildString {
            appendLine("[C]<font size='wide'><b>${order.orderNumber}</b></font>")
            if (sourceLabel.isNotBlank()) appendLine("[C]<font size='tall'><b>$sourceLabel</b></font>")
            appendLine("[L]---")
            for (p in order.products) {
                val priceStr = formatMoney(p.price, currency)
                appendLine("[L]${p.quantity}x ${p.name ?: "?"} $priceStr")
            }
            val total = order.total + (order.shippingTotal ?: 0.0)
            appendLine("[L]---")
            appendLine("[L]<b>${formatMoney(total, currency)}</b>")
            val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
            if (paymentMethodLabel.isNotBlank()) appendLine("[L]<b>$paymentMethodLabel</b>")
            if (!order.note.isNullOrBlank()) {
                appendLine("[L]---")
                appendLine("[L]${order.note}")
            }
        }
    }

    private fun formatMoney(value: Double, currency: String): String {
        val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
        val cents = bd.remainder(BigDecimal.ONE).movePointRight(2).abs().toInt()
        val decSym = DecimalFormatSymbols(Locale.getDefault())
        val fmt = if (cents == 0) DecimalFormat("#,##0", decSym) else DecimalFormat("#,##0.00", decSym)
        val base = fmt.format(bd)
        return if (currency.isBlank()) base else "$base $currency"
    }

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
            null -> ""
        }
    }

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
            null -> ""
        }
    }
}

