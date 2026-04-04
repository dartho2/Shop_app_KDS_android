package com.itsorderkds.ui.theme.home.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.itsorderkds.R
import com.itsorderkds.data.model.Order
import com.itsorderkds.data.model.OrderTras
import com.itsorderkds.ui.order.*
import com.itsorderkds.ui.theme.success
import com.itsorderkds.ui.theme.successContainer
import com.itsorderkds.util.AppPrefs
import com.itsorderkds.util.DateTimeUtils
import timber.log.Timber
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/* ============================== */
/* ==== Stałe i pomocnicze ====== */
/* ============================== */

private val ICON_SIZE: Dp = 32.dp
private val GAP_SMALL = 6.dp
private val GAP = 8.dp
private val GAP_L = 16.dp
private val HOUR_MINUTE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// ====== Pomocnicza funkcja do mapowania ExternalDeliveryStatus ======
data class ExternalDeliveryStatusUi(
    val displayText: String,
    val color: Color,
    val isNegative: Boolean
)

@Composable
private fun getExternalDeliveryStatusUi(status: DeliveryStatusEnum?): ExternalDeliveryStatusUi? {
    val primaryColor = MaterialTheme.colorScheme.primary
    val successColor = MaterialTheme.colorScheme.success
    val errorColor = MaterialTheme.colorScheme.error

    // Pobierz wszystkie stringi PRZED remember
    val requestedText = stringResource(R.string.delivery_status_requested)
    val acceptedText = stringResource(R.string.delivery_status_accepted)
    val inTransitText = stringResource(R.string.delivery_status_in_transit)
    val deliveredText = stringResource(R.string.delivery_status_delivered)
    val cancelledText = stringResource(R.string.delivery_status_cancelled)
    val rejectedText = stringResource(R.string.delivery_status_rejected)

    return remember(
        status,
        primaryColor,
        successColor,
        errorColor,
        requestedText,
        acceptedText,
        inTransitText,
        deliveredText,
        cancelledText,
        rejectedText
    ) {
        when (status) {
            DeliveryStatusEnum.REQUESTED -> ExternalDeliveryStatusUi(
                displayText = requestedText,
                color = primaryColor,
                isNegative = false
            )
            DeliveryStatusEnum.ACCEPTED -> ExternalDeliveryStatusUi(
                displayText = acceptedText,
                color = successColor,
                isNegative = false
            )
            DeliveryStatusEnum.IN_TRANSIT -> ExternalDeliveryStatusUi(
                displayText = inTransitText,
                color = successColor,
                isNegative = false
            )
            DeliveryStatusEnum.DELIVERED -> ExternalDeliveryStatusUi(
                displayText = deliveredText,
                color = successColor,
                isNegative = false
            )
            DeliveryStatusEnum.CANCELLED -> ExternalDeliveryStatusUi(
                displayText = cancelledText,
                color = errorColor,
                isNegative = true
            )
            DeliveryStatusEnum.REJECTED,
            DeliveryStatusEnum.FAILED -> ExternalDeliveryStatusUi(
                displayText = rejectedText,
                color = errorColor,
                isNegative = true
            )
            null -> null
        }
    }
}
@Composable
private fun formatTimeOrNA(date: Date?): String {
    // Pobierz zasoby stringów
    val naString = stringResource(R.string.na)
    val errorString = stringResource(R.string.error_generic)

    // ✅ KROK 2: Użyj `remember`, aby cache'ować wynik.
    // Obliczenia wewnątrz bloku wykonają się tylko wtedy,
    // gdy `date` (klucz) się zmieni.
    return remember(date, naString, errorString) {
        if (date == null) {
            naString
        } else {
            try {
                date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .format(HOUR_MINUTE_FORMATTER) // Użyj globalnej stałej
            } catch (_: Exception) {
                errorString
            }
        }
    }
}

// Przeciążona wersja dla String (używana w RouteListRow dla OrderTras)
@Composable
private fun formatTimeOrNA(dateString: String?): String {
    val naString = stringResource(R.string.na)
    val errorString = stringResource(R.string.error_generic)

    return remember(dateString, naString, errorString) {
        if (dateString.isNullOrBlank()) {
            naString
        } else {
            try {
                DateTimeUtils.parseToZonedDateTime(dateString)
                    ?.withZoneSameInstant(ZoneId.systemDefault())
                    ?.format(HOUR_MINUTE_FORMATTER)
                    ?: naString
            } catch (_: Exception) {
                errorString
            }
        }
    }
}

// Pomocnicze kompozyty do warunkowego renderowania chipów z czasem
@Composable
private fun TimeChipIfValid(labelResId: Int, timeRaw: String?) {
    val naString = stringResource(R.string.na)
    val errorString = stringResource(R.string.error_generic)
    val formatted = formatTimeOrNA(timeRaw)
    if (formatted == naString || formatted == errorString) return
    SmallChip(text = stringResource(labelResId, formatted))
}

@Composable
private fun TimeChipIfValid(labelResId: Int, timeRaw: Date?) {
    val naString = stringResource(R.string.na)
    val errorString = stringResource(R.string.error_generic)
    val formatted = formatTimeOrNA(timeRaw)
    if (formatted == naString || formatted == errorString) return
    SmallChip(text = stringResource(labelResId, formatted))
}

@Composable
private fun SourceIconOrSpace(source: SourceEnum?) {
    val painter = when (source) {
        SourceEnum.UBER -> painterResource(id = R.drawable.ic_uber)
        SourceEnum.GLOVO -> painterResource(id = R.drawable.logo_glovo_80)
        SourceEnum.WOLT -> painterResource(id = R.drawable.logo_wolt_80)
        SourceEnum.BOLT -> painterResource(id = R.drawable.logo_bolt_80)
        SourceEnum.TAKEAWAY -> painterResource(id = R.drawable.ic_takeaway)
        SourceEnum.GOPOS ->  painterResource(id = R.drawable.ic_gopos)
        SourceEnum.WOOCOMMERCE, SourceEnum.WOO -> painterResource(id = R.drawable.ic_woo)
        else -> painterResource(id = R.drawable.logo)
    }
    Icon(
        painter = painter,
        contentDescription = stringResource(
            R.string.source_cd,
            source?.name ?: stringResource(R.string.dash)
        ),
        tint = Color.Unspecified,
        modifier = Modifier.size(ICON_SIZE)
    )
}

@Composable
private fun SmallChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.successContainer,
        contentColor = MaterialTheme.colorScheme.success,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
@Composable
private fun SmallChipSecondary(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/* ============================== */
/* ======= Wiersz trasy ========= */
/* ============================== */

@Composable
fun RouteListRow(
    item: OrderTras,
    isSelected: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    isSelectionModeActive: Boolean,
    onClick: () -> Unit,
    isOrderLocked: (String) -> Boolean
) {
    val isReturn = item.orderNumber.equals("RETURN", ignoreCase = true)
    val context = LocalContext.current
    val cardBg =
        if (isSelected && !isReturn) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    val locked = !isReturn && isOrderLocked(item.orderId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(
                enabled = !isReturn && !locked, // ⬅️ zablokuj klik
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = if (!isLast) 12.dp else 0.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium
        )  {
            Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ikona źródła (lub rezerwacja miejsca)
                SourceIconOrSpace(if (isReturn) null else item.source)
                Spacer(Modifier.width(GAP))

                Column(modifier = Modifier.weight(1f)) {
                    // Tytuł
                    Text(
                        text = if (isReturn)
                            stringResource(R.string.return_to_restaurant)
                        else
                            item.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Adres (tylko dla zamówienia)
                    if (!isReturn) {
                        Text(
                            text = item.address ?: stringResource(R.string.address_missing),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Telefon (klikalny) – tylko dla zamówienia
                    if (!isReturn) {
                        item.phone?.let { phone ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(top = GAP_SMALL)
                                    .clickable {
                                        val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                        context.startActivity(i)
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(GAP_SMALL))
                                Text(
                                    text = phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Jedna linijka: ASAP + Start + ETA
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = GAP)
                    ) {
                        if (!isReturn && item.isAsap == true) {
                            SmallChip(stringResource(R.string.asap_tag))
                            Spacer(Modifier.width(GAP))
                        }
                        TimeChipIfValid(R.string.start_time, item.estimatedStartTime)
                        Spacer(Modifier.width(GAP))
                        TimeChipIfValid(R.string.eta_time, item.eta)
                    }
                }

                Spacer(Modifier.width(GAP_L))

                // Prawa kolumna
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(GAP_SMALL)
                ) {
                    item.distance?.let { stringResource(R.string.distance_km, it) }?.let {
                        DetailItem(
                            icon = Icons.Default.Route,
                            text = it
                        )
                    }
                    item.delayMinutes.let { d -> DelayIndicator(delayMinutes = d) }
                    StatusBadge(status = item.status)
                }
            }
                if (locked) {
                    LockedOverlay() // ⬅️ loader na wierzchu
                }
            }
        }
    }
}

/* ============================== */
/* ======= Lista zamówień ======= */
/* ============================== */

@Composable
fun OrderListItem(order: Order, onClick: () -> Unit) {
    val slugEnum = order.orderStatus.slug?.let {
        runCatching { OrderStatusEnum.valueOf(it) }.getOrNull()
    }
    val isNew = slugEnum == OrderStatusEnum.PROCESSING
    val bg =
        if (isNew) MaterialTheme.colorScheme.successContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh

    val source: SourceEnum? = order.source?.name
    val deliveryIcon = when (order.deliveryType) {
        OrderDelivery.DELIVERY, OrderDelivery.DELIVERY_EXTERNAL -> Icons.Default.LocalShipping
        OrderDelivery.PICKUP, OrderDelivery.PICK_UP -> Icons.Default.ShoppingBag
        else -> null
    }

    // payment_method → etykieta + ikona
    val paymentLabel = when (order.paymentMethod) {
        PaymentMethod.CASH   -> stringResource(R.string.payment_cash)
        PaymentMethod.COD -> stringResource(R.string.payment_cash)
        PaymentMethod.PAYNOW   -> stringResource(R.string.payment_online)
        PaymentMethod.CARD   -> stringResource(R.string.payment_card)
        PaymentMethod.ONLINE -> stringResource(R.string.payment_online)
        null                 -> stringResource(R.string.payment_missing)
        else                 -> order.paymentMethod.name
    }
    val totalText = stringResource(R.string.money_pln, order.total, AppPrefs.getCurrency())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isNew) Modifier.border(1.dp, MaterialTheme.colorScheme.success,  MaterialTheme.shapes.medium)
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SourceIconOrSpace(source)
            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Numer zamówienia + ikona kalendarza
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (order.isScheduled) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = stringResource(R.string.scheduled_order),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = order.orderNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // PREORDER badge tuż pod numerem zamówienia, żeby nie rozjeżdżało układu
                if (order.type == TypeOrderEnum.PREORDER) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.tertiary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.preorder_tag),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Timber.tag("OrderListItem").d("Rendering order ${order.orderNumber} with delivery ${order.deliveryTime}, isScheduled=${order.isScheduled}")

                // Metadane: płatność i czas dostawy
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallChip(text = paymentLabel)
                    Spacer(Modifier.width(8.dp))
                    if (order.isAsap) {
                        SmallChip(text = stringResource(R.string.asap_tag))
                        TimeChipIfValid(R.string.delivery_time_short, order.deliveryTime)
                    } else {
                        TimeChipIfValid(R.string.delivery_time_short, order.deliveryInterval)
                        TimeChipIfValid(R.string.delivery_time_short, order.deliveryTime)
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // ==========================================
            // ==== PRAWA KOLUMNA (Cena + Dostawa) ======
            // ==========================================
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = totalText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Kolumna zawierająca czasy kuriera i status POD nimi + ikona obok
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kolumna z czasami i statusem
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        // Wyświetlaj pickupEta i dropoffEta tylko dla pozytywnych/neutralnych statusów
                        val statusUi = getExternalDeliveryStatusUi(order.externalDelivery?.status)

                        if (statusUi != null && !statusUi.isNegative) {
                            // Wiersz z czasami pickupEta - dropoffEta
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val naText = stringResource(R.string.na)
                                val errText = stringResource(R.string.error_generic)
                                val externalPickupEta = order.externalDelivery?.pickupEta
                                val pickup = externalPickupEta?.let { formatTimeOrNA(it) }
                                if (pickup != null && pickup != naText && pickup != errText) {
                                    Text(
                                        text = pickup,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val externalDropoffEta = order.externalDelivery?.dropoffEta
                                val dropoff = externalDropoffEta?.let { formatTimeOrNA(it) }
                                if (pickup != null && pickup != naText && pickup != errText &&
                                    dropoff != null && dropoff != naText && dropoff != errText
                                ) {
                                    Text(
                                        text = " - ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (dropoff != null && dropoff != naText && dropoff != errText) {
                                    Text(
                                        text = dropoff,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Status POD czasami
                        if (statusUi != null) {
                            Text(
                                text = statusUi.displayText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statusUi.color
                            )
                        }
                    }

                    // Ikona dostawy obok (jeśli dostępna)
                    deliveryIcon?.let {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = it,
                            contentDescription = stringResource(
                                R.string.delivery_type_cd,
                                order.deliveryType?.name ?: "UNKNOWN"
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
