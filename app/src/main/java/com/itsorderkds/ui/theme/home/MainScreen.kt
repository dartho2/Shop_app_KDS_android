package com.itsorderkds.ui.theme.home

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.itsorderkds.R
import com.itsorderkds.ui.order.OrderStatusEnum

// ═════════════════════════════════════════════════════════════════════════
// DEFINICJE ZAKŁADEK DLA DOLNEJ BELKI NAWIGACYJNEJ
// ═════════════════════════════════════════════════════════════════════════

/**
 * Model danych dla pojedynczej zakładki w dolnej belce nawigacyjnej (Staff).
 *
 * @property titleRes ID zasobu string dla nazwy zakładki (np. R.string.order_newOrder)
 * @property icon Ikona Material wyświetlana w dolnej belce
 * @property statuses Zestaw statusów zamówień filtrowanych w tej zakładce
 *                    (pusty zestaw = brak filtrowania po statusie)
 */
internal data class OrderTab(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val statuses: Set<OrderStatusEnum>
)

/**
 * Model danych dla zakładki kuriera.
 *
 * @property titleRes ID zasobu string dla nazwy zakładki
 */
internal data class CourierTab(@StringRes val titleRes: Int)

/**
 * KONFIGURACJA ZAKŁADEK DOLNEJ BELKI DLA PRACOWNIKÓW (STAFF)
 *
 * Struktura:
 * - Indeks 0: "Zamówienia" - Aktywne zamówienia do realizacji
 * - Indeks 1: "Zakończone" - Zakończone i w dostawie
 * - Indeks 2: "Na miejscu" - Zamówienia DINE_IN i ROOM_SERVICE
 * - Indeks 3: "Inne" - Wszystkie pozostałe, grupowane według portalu
 *
 * Każda zakładka definiuje:
 * - Tytuł (wielojęzyczny przez strings.xml)
 * - Ikonę Material Icons
 * - Statusy zamówień do wyświetlenia
 *
 * @see StaffBottomBar Komponent renderujący te zakładki
 * @see StaffView Komponent obsługujący zawartość zakładek
 */
internal val staffTabs = listOf(
    // ─────────────────────────────────────────────────────────────────────
    // ZAKŁADKA 0: ZAMÓWIENIA
    // ─────────────────────────────────────────────────────────────────────
    // Wyświetla aktywne zamówienia w 3 sekcjach:
    // 1. PROCESSING - Do wykonania
    // 2. ACCEPTED - Zaakceptowane
    // 3. OUT_FOR_DELIVERY - W dostawie (zwijana sekcja)
    OrderTab(
        R.string.order_newOrder,           // "Zamówienia"
        Icons.Default.PendingActions,      // Ikona oczekujących zadań
        setOf(
            OrderStatusEnum.PROCESSING,     // Do wykonania
            OrderStatusEnum.OUT_FOR_DELIVERY, // W dostawie
            OrderStatusEnum.ACCEPTED        // Zaakceptowane
        )
    ),

    // ─────────────────────────────────────────────────────────────────────
    // ZAKŁADKA 1: ZAKOŃCZONE
    // ─────────────────────────────────────────────────────────────────────
    // Wyświetla zamówienia zakończone z podzakładkami (PillSwitch):
    // - W dostawie (OUT_FOR_DELIVERY)
    // - Zakończone (COMPLETED)
    OrderTab(
        R.string.order_completed,          // "Zakończone"
        Icons.Default.Done,                // Ikona checkmark
        setOf(
            OrderStatusEnum.OUT_FOR_DELIVERY, // W dostawie
            OrderStatusEnum.COMPLETED       // Zakończone
        )
    ),

    // ─────────────────────────────────────────────────────────────────────
    // ZAKŁADKA 2: NA MIEJSCU (DINE_IN / ROOM_SERVICE)
    // ─────────────────────────────────────────────────────────────────────
    // Wyświetla zamówienia na miejscu i obsługę pokoi:
    // - DINE_IN: Zamówienia na miejscu w restauracji
    // - ROOM_SERVICE: Obsługa pokoi hotelowych
    // Pusty zestaw statusów = wyświetla wszystkie statusy dla tego typu dostawy
    OrderTab(
        R.string.tab_dine_in,              // "Na miejscu"
        Icons.Default.Restaurant,          // Ikona restauracji
        emptySet()                         // Bez filtra statusu - filtrujemy po deliveryType
    ),

    // ─────────────────────────────────────────────────────────────────────
    // ZAKŁADKA 3: INNE
    // ─────────────────────────────────────────────────────────────────────
    // Wyświetla wszystkie zamówienia pogrupowane według portalu:
    // UBER, GLOVO, WOLT, BOLT, TAKEAWAY, WOOCOMMERCE, etc.
    // Pusty zestaw statusów = brak filtrowania po statusie
    OrderTab(
        R.string.status_other,             // "Inne"
        Icons.Default.MoreHoriz,           // Ikona trzech kropek
        emptySet()                         // Bez filtra statusu
    )
)

internal data class SubTab(@StringRes val titleRes: Int, val status: OrderStatusEnum)

internal val completedSubTabs = listOf(
    SubTab(R.string.status_out_for_delivery, OrderStatusEnum.OUT_FOR_DELIVERY),
    SubTab(R.string.status_completed, OrderStatusEnum.COMPLETED)
)
internal val courierTabs = listOf(
    CourierTab(R.string.tab_route),
    CourierTab(R.string.in_delivery),
    CourierTab(R.string.tab_accepted_orders),
    CourierTab(R.string.tab_map)
)
