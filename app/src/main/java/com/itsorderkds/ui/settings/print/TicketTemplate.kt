package com.itsorderkds.ui.settings.print

import android.content.Context
import com.itsorderkds.R
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.TemplateConfig
import com.itsorderkds.ui.order.OrderDelivery
import com.itsorderkds.ui.order.PaymentMethod
import com.itsorderkds.ui.order.SourceEnum
import com.itsorderkds.ui.order.TypeOrderEnum
import com.itsorderkds.util.AppPrefs
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/* ------------------------------- Stałe / formatery ------------------------------- */

private const val LINE_CHARS = 32               // szerokość linii w ZNAKACH (nie bajtach)
private const val QTY_FIELD_CHARS = 4           // "qty× " (np. "2× ") padded do 4
private const val INDENT = "    "               // wcięcie dla zawijanych linii
private val DEC_SYM = DecimalFormatSymbols(Locale.getDefault())

private val MONEY_NO_DEC = DecimalFormat("#,##0", DEC_SYM)
private val MONEY_2_DEC  = DecimalFormat("#,##0.00", DEC_SYM)

/* ------------------------------- Public API ------------------------------------- */

/** Buduje kompletny tekst paragonu dla EscPosPrinter.
 *  @param ctx Context aplikacji
 *  @param order Zamówienie do wydruku
 *  @param config Konfiguracja szablonu wydruku (opcje formatowania)
 *  @param useDeliveryInterval jeśli true, używa delivery_interval zamiast delivery_time (dla druku automatycznego)
 */
fun buildTicket(
    ctx: Context,
    order: Order,
    config: TemplateConfig = TemplateConfig.standard(),
    useDeliveryInterval: Boolean = false
): String {
    val systemowyFormatDatyICzasu = java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT
    )
    val now = systemowyFormatDatyICzasu.format(Date())
    val currency = AppPrefs.getCurrency().orEmpty()

    /* ------ Etykieta dostawy / odbioru ------ */
    val deliveryLabel = when (order.deliveryType) {
        OrderDelivery.DELIVERY,
        OrderDelivery.DELIVERY_EXTERNAL,
        OrderDelivery.FLAT_RATE -> ctx.getString(R.string.delivery_label_delivery)
        OrderDelivery.PICKUP,
        OrderDelivery.PICK_UP,
        OrderDelivery.LOCAL_PICKUP -> ctx.getString(R.string.delivery_label_pickup)
        OrderDelivery.DINE_IN,
        OrderDelivery.ROOM_SERVICE -> ctx.getString(R.string.delivery_label_dine_in)
        OrderDelivery.UNKNOWN -> ctx.getString(R.string.delivery_label_unknown)
        null -> TODO()
    }

    /* ------ Wybierz czas dostawy ------ */
    val deliveryTimeForDisplay = when {
        useDeliveryInterval && !order.deliveryInterval.isNullOrBlank() -> order.deliveryInterval
        !order.deliveryTime.isNullOrBlank() -> order.deliveryTime
        !order.deliveryInterval.isNullOrBlank() -> order.deliveryInterval
        else -> order.createdAt // ostatnia deska ratunku, żeby nie pokazać "-"
    }
    val orderType = order.type
    val isPreorder = orderType == TypeOrderEnum.PREORDER
    val isAsapType = orderType == TypeOrderEnum.ASAP
    val isCustomerWaiting = useDeliveryInterval || isPreorder
    // gdy klient czeka, a czas pusty -> wymuś ASAP, aby nie pokazywać "-"
    val effectiveAsap = isAsapType || (!isPreorder && order.isAsap == true) || (isCustomerWaiting && deliveryTimeForDisplay.isNullOrBlank())

    /* ------ Nagłówek ------ */
    val header = buildString {
        appendLine("[C]<font size='wide'><b>${order.orderNumber ?: "B/N"}</b></font>")

        // Tekstowa nazwa źródła zamówienia (z formatowaniem)
        val sourceLabel = getSourceLabel(order.source?.name)

        // Debug logging
        android.util.Log.d("TICKET_PRINT", "Order #${order.orderNumber}: source=${order.source?.name}, sourceLabel='$sourceLabel'")

        if (sourceLabel.isNotBlank()) {
            appendLine("[C]<font size='tall'>$sourceLabel</font>")  // Duża czcionka dla logo
            android.util.Log.d("TICKET_PRINT", "✅ Source label added to ticket: $sourceLabel")
        } else {
            android.util.Log.d("TICKET_PRINT", "⚠️ Source label is blank - not added to ticket")
        }

        appendLine("[C]<font size='wide'><b>$deliveryLabel</b></font>")
        appendLine(separator())
        appendLine("[L]${ctx.getString(R.string.label_date)}   : $now")
        appendLine("[L]${ctx.getString(R.string.label_client)} : ${order.consumer?.name ?: "-"}")
        appendLine("[L]${ctx.getString(R.string.label_phone)}  : ${order.consumer?.phone ?: "-"}")
        appendLine(separator())

        // Wyświetl czas dostawy + adnotacja "klient oczekuje" + czy inny dzień (POGRUBIONY I POWIĘKSZONY)
        val timeDisplay = formatDeliveryTime(
            deliveryTimeForDisplay,
            isAsap = effectiveAsap,
            asapLabel = ctx.getString(R.string.asap_text),
            isScheduledForFuture = isDifferentDay(deliveryTimeForDisplay)
        )

        if (isCustomerWaiting) {
            appendLine("[C]<font size='wide'><b>$timeDisplay</b></font>")
            appendLine("[C]<font size='small'>${ctx.getString(R.string.customer_waiting)}</font>")
        } else {
            appendLine("[C]<font size='wide'><b>$timeDisplay</b></font>")
        }

        /* Adres tylko przy dostawie */
        if (order.deliveryType in listOf(
                OrderDelivery.DELIVERY,
                OrderDelivery.DELIVERY_EXTERNAL,
                OrderDelivery.FLAT_RATE
            )
        ) {
            order.shippingAddress?.let { addr ->
                val street = addr.street.orEmpty()
                val numberHome = addr.numberHome.orEmpty()
                val numberFlat = addr.numberFlat.orEmpty()

                if (street.isNotBlank() || numberHome.isNotBlank() || numberFlat.isNotBlank()) {
                    appendLine(separator())
                    appendLine("[L]$street $numberHome/$numberFlat")
                    if (!addr.city.isNullOrBlank()) {
                        appendLine("[L]${addr.city}")
                    }
                }
            }
        }
        append(separator())
    }

    /* ------ Pozycje ------ */
    val body = buildString {
        for (p in order.products) {
            val total = p.price
            appendLine(
                formatProductLine(
                    qty = p.quantity,
                    name = p.name ?: "Brak nazwy",
                    comment = p.comment ?: "",
                    note = p.note,  // Dodano note jako List<String>
                    price = total,
                    lineChars = LINE_CHARS,
                    currency = currency
                )
            )
        }
    }.trimEnd()

    /* ------ Sumy ------ */
    val subTot = order.total
    val shipCost = order.shippingTotal ?: 0.0
    val additionalFee = order.additionalFeeTotal ?: 0.0
    val point = order.pointsAmount ?: 0.0
    val discount = order.couponTotalDiscount ?: 0.0
    val totalDiscount = point + discount ?: 0.0
    // WAŻNE: Suma pochodzi z pola order.total (zawiera już wszystkie kalkulacje z API)
    // order.total = kwota_brutto - zniżka + dostawa + dodatkowe_opłaty
    val grand = order.total

    val totals = buildString {
        appendLine(separator())
        // Kwota przed rabatami i opłatami
        val subtotalBeforeDiscounts = subTot + discount
        appendLine("[L]" + composeLeftRight(
            left = " ${ctx.getString(R.string.line_subtotal)}:",
            right = fmtMoneySmart(subtotalBeforeDiscounts, currency),
            width = LINE_CHARS
        ))
        // Dostawa
        if (shipCost > 0.0) {
            appendLine("[L]" + composeLeftRight(
                left = " ${ctx.getString(R.string.line_delivery)}:",
                right = fmtMoneySmart(shipCost, currency),
                width = LINE_CHARS
            ))
        }
        // Dodatkowe opłaty
        if (additionalFee > 0.0) {
            appendLine("[L]" + composeLeftRight(
                left = " ${ctx.getString(R.string.line_additional_fee)}:",
                right = fmtMoneySmart(additionalFee, currency),
                width = LINE_CHARS
            ))
        }
        // Zniżka za punkty
        if (point > 0.0) {
            appendLine("[L]" + composeLeftRight(
                left = " ${ctx.getString(R.string.line_point_use)}:",
                right = "-${fmtMoneySmart(point, currency)}",
                width = LINE_CHARS
            ))
        }
        // Zniżka
        if (totalDiscount > 0.0) {
            appendLine("[L]" + composeLeftRight(
                left = " ${ctx.getString(R.string.line_discount)}:",
                right = "-${fmtMoneySmart(totalDiscount, currency)}",
                width = LINE_CHARS
            ))
        }

        // Razem (pobrane z pola total)
        appendLine("[L]<b>" + composeLeftRight(
            left = " ${ctx.getString(R.string.line_total)}:",
            right = fmtMoneySmart(grand, currency),
            width = LINE_CHARS
        ) + "</b>")

        // Metoda płatności - POGRUBIONA pod kwotą
        val paymentMethodLabel = getPaymentMethodLabel(order.paymentMethod)
        if (paymentMethodLabel.isNotBlank()) {
            appendLine("[C]<b>${ctx.getString(R.string.label_payment)}: $paymentMethodLabel</b>")
        }

        append(separator())
    }

    /* ------ Stopka ------ */
    val footer = """
        [C]<b>${ctx.getString(R.string.footer_thanks)}</b>
        [C]
    """.trimIndent()

    /* ------ Notatki (na samym dole) ------ */
    val notesLine = order.note?.takeIf { it.isNotBlank() }?.let {
        "\n${separator()}\n[L]${ctx.getString(R.string.label_notes)}: $it"
    } ?: ""

    return "$header\n$body\n$totals\n$footer$notesLine"
}

/* ------------------------------- Formatowanie daty dostawy ---------------------- */

data class Parsed(val date: Date, val hasTime: Boolean)

/** Sprawdza czy data dostawy jest innym dniem niż dzisiaj */
fun isDifferentDay(isoOrAny: String?): Boolean {
    if (isoOrAny.isNullOrBlank()) return false
    val parsed = parseAnyDate(isoOrAny, Locale.getDefault()) ?: return false

    val nowCal = Calendar.getInstance()
    val delCal = Calendar.getInstance().apply { time = parsed.date }

    return !(nowCal.get(Calendar.ERA) == delCal.get(Calendar.ERA) &&
        nowCal.get(Calendar.YEAR) == delCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == delCal.get(Calendar.DAY_OF_YEAR))
}

/** Zwraca:
 *  - ASAP: etyketa + (opcjonalnie HH:mm) + (jeśli inny dzień → data i [PRZYSZŁOŚĆ])
 *  - Nie ASAP: dziś HH:mm, inny dzień dd.MM.yyyy HH:mm (gdy mamy godzinę) lub sama data + [PRZYSZŁOŚĆ]
 */
fun formatDeliveryTime(
    isoOrAny: String?,
    isAsap: Boolean = false,
    asapLabel: String = "ASAP",
    isScheduledForFuture: Boolean = false
): String {
    val locale = Locale.getDefault()
    if (isoOrAny.isNullOrBlank()) return if (isAsap) asapLabel else "-"

    val parsed = parseAnyDate(isoOrAny, locale) ?: return if (isAsap) asapLabel else isoOrAny

    val nowCal = Calendar.getInstance()
    val delCal = Calendar.getInstance().apply { time = parsed.date }

    val sameDay =
        nowCal.get(Calendar.ERA) == delCal.get(Calendar.ERA) &&
            nowCal.get(Calendar.YEAR) == delCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == delCal.get(Calendar.DAY_OF_YEAR)

    val timeFmt = SimpleDateFormat("HH:mm", locale)
    val dateFmt = SimpleDateFormat("dd.MM.yyyy", locale)
    val timePart = if (parsed.hasTime) timeFmt.format(parsed.date) else ""

    val futureMarker = if (isScheduledForFuture && !sameDay) " [PRZYSZŁOŚĆ]" else ""

    return if (isAsap) {
        buildString {
            append(asapLabel)
            if (timePart.isNotBlank()) append(" – ").append(timePart)
            if (!sameDay) append(" ").append(dateFmt.format(parsed.date)).append(futureMarker)
        }
    } else {
        if (sameDay) {
            (timePart.ifBlank { dateFmt.format(parsed.date) }) + futureMarker
        } else {
            buildString {
                append(dateFmt.format(parsed.date))
                if (timePart.isNotBlank()) append(" ").append(timePart)
                append(futureMarker)
            }
        }
    }
}

private fun parseAnyDate(input: String, locale: Locale): Parsed? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mmXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, locale).apply {
                if (!p.contains('X') && !p.contains('Z')) timeZone = TimeZone.getDefault()
            }
            val date = sdf.parse(input) ?: continue
            val hasTime = p.contains("HH:mm")
            return Parsed(date, hasTime)
        } catch (_: Exception) { /* next */ }
    }
    return null
}

/* ------------------------------- Formatowanie pozycji --------------------------- */

/** Linia produktu: wrap po słowach, cena DOSUNIĘTA do prawej.
 *  Jeśli zawija się, dzielimy ~pół-na-pół (z przewagą 1. linii), a cenę dajemy w 2. linii.
 */
/** Linia produktu + (opcjonalnie) komentarz pod produktem, podkreślony + uwagi (note) pogrubione. */
private fun formatProductLine(
    qty: Int,
    name: String,
    comment: String?,
    note: List<String>?,
    price: Double,
    lineChars: Int = LINE_CHARS,
    currency: String = "",
    charset: Charset = Charsets.UTF_8
): String {
    fun escapeMarkup(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    val qtyStr = "${qty}× ".padEnd(QTY_FIELD_CHARS)
    val priceStr = fmtMoneySmart(price, currency)

    // Nazwa – normalizacja spacji
    val normalizedName = name.trim().replace(Regex("\\s+"), " ")
    val words = if (normalizedName.isEmpty()) emptyList() else normalizedName.split(' ')

    // Pojemności linii
    val cap1 = (lineChars - qtyStr.length).coerceAtLeast(0)           // linia 1 (z ilością)
    val wrapWidth = (lineChars - INDENT.length).coerceAtLeast(0)      // linie zawijane
    val cap2 = (wrapWidth - priceStr.length - 1).coerceAtLeast(0)     // linia 2 (miejsce na cenę)

    // Jedna linia? -> dosunięcie ceny do prawej
    val oneLineLeft = qtyStr + normalizedName
    val oneLineRight = priceStr
    if (oneLineLeft.length + 1 + oneLineRight.length <= lineChars) {
        val paddingLen = (lineChars - oneLineLeft.length - oneLineRight.length).coerceAtLeast(1)
        val base = "[L]$oneLineLeft${" ".repeat(paddingLen)}$oneLineRight"
        return buildString {
            append(base)
            // --- komentarz pod produktem (podkreślony) ---
            val c = comment?.trim().orEmpty()
            if (c.isNotEmpty()) {
                val cWords = escapeMarkup(c).replace(Regex("\\s+"), " ").split(' ')
                var idx = 0
                while (idx < cWords.size) {
                    val (next, chunk) = takeWordsUpTo(cWords, idx, wrapWidth)
                    append("\n[L]").append(INDENT).append("<u>").append(chunk).append("</u>")
                    if (next == idx) break
                    idx = next
                }
            }
            // --- uwagi do produktu (note) - POGRUBIONE ---
            if (!note.isNullOrEmpty()) {
                for (noteItem in note) {
                    val noteText = noteItem.trim()
                    if (noteText.isNotEmpty()) {
                        val noteWords = escapeMarkup(noteText).replace(Regex("\\s+"), " ").split(' ')
                        var idx = 0
                        while (idx < noteWords.size) {
                            val (next, chunk) = takeWordsUpTo(noteWords, idx, wrapWidth)
                            append("\n[L]").append(INDENT).append("<b>").append(chunk).append("</b>")
                            if (next == idx) break
                            idx = next
                        }
                    }
                }
            }
        }
    }

    // Dwie pierwsze linie ~pół-na-pół z przewagą 1. linii
    val totalTwoLineCap = cap1 + cap2
    val targetTwoLines = minOf(normalizedName.length, totalTwoLineCap)
    val desiredFirstLen = minOf(cap1, (targetTwoLines + 1) / 2)

    val (iAfterFirst, firstChunk) = takeWordsUpTo(words, start = 0, maxLen = desiredFirstLen)
    val (jAfterSecond, secondChunkInitial) = takeWordsUpTo(words, start = iAfterFirst, maxLen = cap2)

    var first = firstChunk
    var second = secondChunkInitial

    // Lekka korekta równowagi między pierwszą a drugą linią
    while (second.isNotEmpty() && second.length > first.length) {
        val parts = second.split(' ').toMutableList()
        val candidate = parts.first()
        val addLen = if (first.isEmpty()) candidate.length else 1 + candidate.length
        if (first.length + addLen <= cap1) {
            first = if (first.isEmpty()) candidate else "$first $candidate"
            parts.removeAt(0)
            second = parts.joinToString(" ")
        } else break
    }

    val sb = StringBuilder()
    // Linia 1: ilość + tekst
    sb.append("[L]").append(qtyStr).append(first)

    // Linia 2: wcięcie + tekst + cena po prawej
    sb.append("\n[L]").append(INDENT).append(composeLeftRight(second, priceStr, wrapWidth))

    // Kolejne linie nazwy (wrap)
    var k = jAfterSecond
    while (k < words.size) {
        val (nextK, chunk) = takeWordsUpTo(words, start = k, maxLen = wrapWidth)
        sb.append("\n[L]").append(INDENT).append(chunk)
        if (nextK == k) break
        k = nextK
    }

    // --- komentarz pod produktem (podkreślony) ---
    val c = comment?.trim().orEmpty()
    if (c.isNotEmpty()) {
        val cWords = escapeMarkup(c).replace(Regex("\\s+"), " ").split(' ')
        var idx = 0
        while (idx < cWords.size) {
            val (next, chunk) = takeWordsUpTo(cWords, idx, wrapWidth)
            sb.append("\n[L]").append(INDENT).append("<u>").append(chunk).append("</u>")
            if (next == idx) break
            idx = next
        }
    }

    // --- uwagi do produktu (note) - POGRUBIONE ---
    if (!note.isNullOrEmpty()) {
        for (noteItem in note) {
            val noteText = noteItem.trim()
            if (noteText.isNotEmpty()) {
                val noteWords = escapeMarkup(noteText).replace(Regex("\\s+"), " ").split(' ')
                var idx = 0
                while (idx < noteWords.size) {
                    val (next, chunk) = takeWordsUpTo(noteWords, idx, wrapWidth)
                    sb.append("\n[L]").append(INDENT).append("<b>").append(chunk).append("</b>")
                    if (next == idx) break
                    idx = next
                }
            }
        }
    }

    return sb.toString()
}
/** Składa lewy tekst + spacje + prawy tekst tak, by prawy kończył się na prawej krawędzi. */
private fun composeLeftRight(left: String, right: String, width: Int): String {
    val w = width.coerceAtLeast(0)
    if (w == 0) return right
    val clippedLeft = if (left.length > w - right.length - 1) {
        left.take((w - right.length - 1).coerceAtLeast(0))
    } else left
    val spaces = (w - clippedLeft.length - right.length).coerceAtLeast(1)
    return clippedLeft + " ".repeat(spaces) + right
}

/** Zwraca największą frazę (po całych słowach) mieszczącą się w maxLen.
 *  @return Pair<indexPo, fraza> gdzie indexPo = indeks kolejnego słowa po wziętym fragmencie. */
private fun takeWordsUpTo(words: List<String>, start: Int, maxLen: Int): Pair<Int, String> {
    if (start >= words.size || maxLen <= 0) return start to ""
    var len = 0
    var idx = start
    val sb = StringBuilder()
    while (idx < words.size) {
        val w = words[idx]
        val add = if (sb.isEmpty()) w.length else 1 + w.length
        if (len + add > maxLen) break
        if (sb.isNotEmpty()) sb.append(' ')
        sb.append(w)
        len += add
        idx++
    }
    if (sb.isEmpty()) {
        // pojedyncze mega długie słowo – techniczny cut
        val w = words[start]
        val cut = w.take(maxLen)
        return (start + 1) to cut
    }
    return idx to sb.toString()
}

/* ------------------------------- Helpery tekstowe/kwotowe ----------------------- */

private fun separator(): String = "[L]--------------------------------"


/** Zwraca czytelną nazwę źródła zamówienia - bez emoji dla kompatybilności z drukarkami termicznymi */
private fun getSourceLabel(source: SourceEnum?): String {
    return when (source) {
        SourceEnum.UBER -> "UBER EATS"
        SourceEnum.WOLT -> "WOLT"
        SourceEnum.GLOVO -> "GLOVO"
        SourceEnum.BOLT -> "BOLT FOOD"
        SourceEnum.TAKEAWAY -> "PYSZNE"
        SourceEnum.GOPOS -> "GOPOS"
        SourceEnum.WOOCOMMERCE,
        SourceEnum.WOO -> "WWW"
        SourceEnum.ITS -> "ITS ORDER"
        SourceEnum.OTHER,
        SourceEnum.UNKNOWN,
        null -> "" // Brak źródła - nie wyświetlaj nic
    }
}

/** Zwraca czytelną nazwę metody płatności */
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

/** „0 lub 2 miejsca” – jeśli kwota ma grosze → 2, jeśli pełna → bez groszy. */
private fun fmtMoneySmart(value: Double, currency: String): String {
    val bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
    val cents = bd.remainder(BigDecimal.ONE).movePointRight(2).abs().toInt()
    val base = if (cents == 0) MONEY_NO_DEC.format(bd) else MONEY_2_DEC.format(bd)
    return if (currency.isBlank()) base else "$base $currency"
}

/** Maskuje środek tekstu, zostawia po 3 znaki na brzegach. */
private fun maskMiddleKeepEdges(text: String?, edge: Int = 3, maskChar: Char = '.'): String {
    val t = text?.trim().orEmpty()
    if (t.isEmpty()) return "-"
    if (t.length <= edge * 2) return t
    val left = t.take(edge)
    val right = t.takeLast(edge)
    val middleLen = t.length - edge * 2
    return left + maskChar.toString().repeat(middleLen) + right
}

/** Maskuje cyfry w telefonie, zachowuje separatory; zostawia 3 pierwsze i 3 ostatnie cyfry. */
private fun maskPhoneKeepSeparators(phone: String?, edgeDigits: Int = 3, maskChar: Char = '.'): String {
    val p = phone?.trim().orEmpty()
    if (p.isEmpty()) return "-"
    val totalDigits = p.count { it.isDigit() }
    if (totalDigits <= edgeDigits * 2) return p

    var seen = 0
    val out = StringBuilder(p.length)
    for (ch in p) {
        if (ch.isDigit()) {
            if (seen in edgeDigits until (totalDigits - edgeDigits)) out.append(maskChar) else out.append(ch)
            seen++
        } else {
            out.append(ch)
        }
    }
    return out.toString()
}
