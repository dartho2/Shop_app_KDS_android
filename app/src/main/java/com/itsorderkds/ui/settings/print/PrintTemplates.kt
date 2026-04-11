package com.itsorderkds.ui.settings.print

import android.content.Context
import com.itsorderkds.R
import com.itsorderkds.data.model.KdsPrinterEnum
import com.itsorderkds.data.model.KdsTicketItem
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderProduct
import com.itsorderkds.ui.order.OrderDelivery
import com.itsorderkds.ui.order.PaymentMethod
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.util.AppPrefs
import timber.log.Timber
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
     * Builder z filtrowaniem pozycji po roli drukarki z API ([printerRole]).
     *
     * Logika filtrowania:
     *  - [printerRole] == KITCHEN  → items gdzie printer="KITCHEN"
     *  - [printerRole] == SUSHI    → items gdzie printer="SUSHI"
     *  - [printerRole] == BAR      → items gdzie printer="BAR"
     *  - [printerRole] == DESSERT  → items gdzie printer="DESSERT"
     *  - [printerRole] == MAIN     → items gdzie printer="MAIN" ORAZ items gdzie printer=null
     *
     * Zwraca **null** gdy po filtrowaniu nie ma żadnej pozycji → nie drukuj nic.
     * Zwraca **null** gdy [items] puste i [printerRole] != null → nie drukuj nic.
     * Wyjątek: gdy [printerRole] == null → drukuje wszystko (brak filtrowania).
     *
     * @return ESC/POS string lub null gdy brak pozycji do druku
     */
    fun buildFilteredTicket(
        ctx: Context,
        order: Order,
        items: List<KdsTicketItem>,
        printerRole: KdsPrinterEnum?,
        useDeliveryInterval: Boolean = false,
        kdsTicketNumber: String? = null,
        printKitchenMainProduct: Boolean = false
    ): String? {
        // Brak roli → drukuj wszystkie (brak filtrowania)
        if (printerRole == null) {
            Timber.tag("PRINT_FILTER").d("⏭️ buildFilteredTicket: brak roli → drukuję wszystkie przez buildKitchenTicketFromItems")
            return if (items.isNotEmpty()) {
                buildKitchenTicketFromItems(order, items, useDeliveryInterval, kdsTicketNumber, printKitchenMainProduct)
            } else {
                buildKitchenTicketBody(order, useDeliveryInterval)
            }
        }

        // Brak items + jest rola → sprawdź czy wszyscy mają printer=null (stare API)
        if (items.isEmpty()) {
            return if (printerRole == KdsPrinterEnum.MAIN) {
                Timber.tag("PRINT_FILTER").d("⏭️ brak items, rola=MAIN → standardowy bloczek")
                buildKitchenTicketBody(order, useDeliveryInterval)
            } else {
                Timber.tag("PRINT_FILTER").d("⏭️ brak items, rola=${printerRole.apiValue} → nic nie drukuję")
                null
            }
        }

        // Filtruj pozycje według roli drukarki
        val filtered = when (printerRole) {
            KdsPrinterEnum.MAIN ->
                // MAIN dostaje: printer="MAIN" + printer=null (pozycje bez przypisania)
                items.filter { it.printerEnum == KdsPrinterEnum.MAIN || it.printerEnum == null }
            else ->
                items.filter { it.shouldPrintOn(printerRole) }
        }

        return if (filtered.isEmpty()) {
            // Żadna pozycja nie pasuje do tej drukarki → NIE drukuj
            Timber.tag("PRINT_FILTER").d(
                "⏭️ Brak pozycji dla roli ${printerRole.apiValue} " +
                "(sprawdzono ${items.size} items, wszystkie na inne stacje) → pomijam drukowanie"
            )
            null
        } else {
            Timber.tag("PRINT_FILTER").d(
                "✅ buildFilteredTicket: ${filtered.size}/${items.size} pozycji " +
                "dla roli ${printerRole.apiValue} (order=${order.orderNumber})"
            )
            buildKitchenTicketFromItems(order, filtered, useDeliveryInterval, kdsTicketNumber, printKitchenMainProduct)
        }
    }

    /**
     * Legacy: Builder kuchenny z filtrowaniem po printer=KITCHEN.
     * Zachowany dla kompatybilności — używaj [buildFilteredTicket].
     */
    @Deprecated(
        "Use buildFilteredTicket(ctx, order, items, KdsPrinterEnum.KITCHEN, useDeliveryInterval)",
        ReplaceWith("buildFilteredTicket(ctx, order, items, KdsPrinterEnum.KITCHEN, useDeliveryInterval)")
    )
    fun buildKitchenOnlyTicket(
        ctx: Context,
        order: Order,
        items: List<KdsTicketItem>,
        useDeliveryInterval: Boolean = false
    ): String? = buildFilteredTicket(ctx, order, items, KdsPrinterEnum.KITCHEN, useDeliveryInterval)

    /**
     * Buduje bloczek kuchenny z listy [items] z obsługą productions[].
     *
     * Logika drukowania:
     *  - Gdy item ma productions[] z printer=KITCHEN → drukuje KAŻDY task z productions
     *    jako osobną linię (label + qty). Pozycja główna (displayName) jest tytułem/grupą.
     *  - Gdy item nie ma productions (stare API lub brak sekcji) → drukuje jak dotąd
     *    (displayName + qty).
     *
     * Dzięki temu kucharz widzi dokładnie co przygotować dla każdego produktu
     * (np. składniki, podprodukty) a nie tylko ogólną nazwę.
     */
    private fun buildKitchenTicketFromItems(
        order: Order,
        items: List<KdsTicketItem>,
        useDeliveryInterval: Boolean,
        kdsTicketNumber: String? = null,
        printKitchenMainProduct: Boolean = false
    ): String {
        // Budujemy własny body z obsługą productions — nie przez buildKitchenTicketBody
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

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

        val timeDisplayRaw = when {
            useDeliveryInterval && !order.deliveryInterval.isNullOrBlank() -> order.deliveryInterval!!
            !order.deliveryTime.isNullOrBlank() -> order.deliveryTime!!
            !order.deliveryInterval.isNullOrBlank() -> order.deliveryInterval!!
            order.isAsap == true -> "NA JUZ"
            else -> ""
        }
        val timeDisplay = if (timeDisplayRaw.equals("NA JUZ", ignoreCase = true)) {
            timeDisplayRaw
        } else {
            try {
                val zdt = java.time.ZonedDateTime.parse(timeDisplayRaw)
                java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(zdt)
            } catch (e: Exception) {
                timeDisplayRaw
            }
        }

        val sourceLabel = getKitchenSourceLabel(order.source?.name)

        val header = buildString {
            val kdsNum = kdsTicketNumber ?: ""
            val extNum = order.orderNumber ?: "B/N"

            if (kdsNum.isNotBlank()) {
                appendLine("[C]$kdsNum")
            }
            appendLine("[C]<font size='wide'><b>#$extNum</b></font>")

            if (sourceLabel.isNotBlank()) {
                appendLine("[C]$sourceLabel")
            }
            if (deliveryLabel.isNotBlank()) {
                appendLine("[C]<font size='wide'><b>$deliveryLabel</b></font>")
            }
            appendLine("[L]--------------------------------")
            appendLine("[C]$now")
            if (timeDisplay.isNotBlank()) {
                appendLine("[C]<font size='wide'><b>$timeDisplay</b></font>")
            }
            appendLine("[L]--------------------------------")
        }

        // Buduj body z obsługą productions
        val body = buildString {
            for (item in items.sortedBy { it.sequence }) {
                appendLine("[L]") // Odstęp 1 linijki przed każdym itemem

                val kitchenProductions = item.productions
                    ?.filter { task ->
                        task.printerEnum == KdsPrinterEnum.KITCHEN || task.printerEnum == null
                    }
                    ?.filter { task -> task.label?.isNotBlank() == true }

                if (!kitchenProductions.isNullOrEmpty()) {
                    if (printKitchenMainProduct) {
                        appendLine("[C]--- ${item.displayName} ---")
                    }
                    for (task in kitchenProductions) {
                        val taskQty = if (task.qty > 0) "${task.qty}x " else ""
                        appendLine("[L]** $taskQty${task.label}")
                    }
                    item.notes.forEach { note ->
                        if (note.isNotBlank()) appendLine("[L]    ! $note")
                    }
                } else {
                    val qtyStr = "${item.qty}x "
                    appendLine("[L]$qtyStr${item.displayName}")
                    item.notes.forEach { note ->
                        if (note.isNotBlank()) appendLine("[L]    ! $note")
                    }
                }
            }
        }.trimEnd()

        val notesLine = order.note?.takeIf { it.isNotBlank() }?.let {
            "\n[L]--------------------------------\n[L]<b>UWAGA: $it</b>"
        } ?: ""

        return "$header\n$body$notesLine\n[L]================================\n\n\n"
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

        return "$header\n$body$notesLine\n[L]================================\n\n\n"
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

