package com.itsorderkds.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.itsorderkds.LoginActivity
import com.itsorderkds.R
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.theme.home.HomeActivity
import timber.log.Timber

object NotificationHelper {

    // ── Kanał: serwis w tle (cichy, tylko ikona w pasku) ─────────────────────
    private const val SERVICE_CHANNEL_ID   = "socket_service_channel"
    private const val SERVICE_CHANNEL_NAME = "Usługa WebSocket"
    private const val SERVICE_CHANNEL_DESC = "Informacje o połączeniu WebSocket w tle"

    // ── Kanał: rozłączenie WS (heads-up, wibracja, brak dźwięku) ─────────────
    private const val WS_ALERT_CHANNEL_ID   = "ws_disconnect_alert_v1"
    private const val WS_ALERT_CHANNEL_NAME = "Alert połączenia"
    private const val WS_ALERT_CHANNEL_DESC = "Informuje gdy aplikacja traci połączenie z serwerem"
    private const val ID_WS_DISCONNECT = 1997

    // ── Kanał: nowe zamówienie KDS — CICHY (dźwięk grany przez Ringtone, nie przez kanał) ──
    private const val KDS_TICKET_CHANNEL_ID   = "kds_new_ticket_silent_v2"
    private const val KDS_TICKET_CHANNEL_NAME = "Nowe zamówienia KDS"
    private const val KDS_TICKET_CHANNEL_DESC = "Powiadomienie o nowym tickecie w kuchni (dźwięk sterowany z ustawień aplikacji)"
    private const val ID_KDS_NEW_TICKET_BASE  = 5000

    // ── Kanał: alerty systemowe FGS / sesja ───────────────────────────────────
    private const val ALERTS_CHANNEL_ID   = "fgs_alerts_channel"
    private const val ALERTS_CHANNEL_NAME = "Alerty systemowe"
    private const val ALERTS_CHANNEL_DESC = "Wygaśnięcie sesji, ograniczenia usługi w tle"
    private const val CHANNEL_ID_ALERTS   = "service_logout"

    private const val NOTIFICATION_ID_SESSION     = 999
    private const val ID_ALERT_NEEDS_FOREGROUND   = 1001
    private const val ID_ALERT_QUOTA_TIMEOUT      = 1002

    /** Aktualnie odtwarzany Ringtone — zatrzymujemy przed kolejnym */
    @Volatile private var currentRingtone: Ringtone? = null

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tworzy wszystkie kanały powiadomień przy starcie aplikacji.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Usuń stare kanały alarmowe i stary kanał KDS z dźwiękiem w kanale
        listOf(
            "orders_channel",
            "order_alarm_channel_v2",
            "order_alarm_channel_v3",
            "order_alarm_channel_v4",
            "route_updates_channel",
            "kds_new_ticket_v1"          // stary kanał — dźwięk był w kanale, niesteorwalny
        ).forEach { old ->
            if (mgr.getNotificationChannel(old) != null) {
                mgr.deleteNotificationChannel(old)
                Timber.d("NotificationHelper: Usunięto stary kanał: $old")
            }
        }

        // 1. Kanał serwisu w tle (cichy)
        mgr.createNotificationChannel(
            NotificationChannel(SERVICE_CHANNEL_ID, SERVICE_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = SERVICE_CHANNEL_DESC
                setSound(null, null)
            }
        )

        // 2. Kanał rozłączenia WS (heads-up, bez dźwięku)
        mgr.createNotificationChannel(
            NotificationChannel(WS_ALERT_CHANNEL_ID, WS_ALERT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = WS_ALERT_CHANNEL_DESC
                enableVibration(true)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )

        // 3. Kanał KDS — CICHY (dźwięk grany przez Ringtone API, niezależnie od kanału)
        //    Dzięki temu zmiana dźwięku w ustawieniach aplikacji ZAWSZE działa.
        mgr.createNotificationChannel(
            NotificationChannel(KDS_TICKET_CHANNEL_ID, KDS_TICKET_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = KDS_TICKET_CHANNEL_DESC
                enableVibration(true)
                setSound(null, null)   // ← brak dźwięku kanału — dźwięk idzie przez Ringtone
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )

        // 4. Kanał alertów systemowych (FGS, sesja)
        mgr.createNotificationChannel(
            NotificationChannel(ALERTS_CHANNEL_ID, ALERTS_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = ALERTS_CHANNEL_DESC
                enableVibration(true)
            }
        )
    }

    // ─── Serwis w tle ────────────────────────────────────────────────────────

    fun buildServiceNotification(context: Context): Notification =
        NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Połączenie aktywne")
            .setContentText("Odbieram zamówienia w tle")
            .setSmallIcon(R.drawable.ic_chat)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    // ─── Alert rozłączenia WS ─────────────────────────────────────────────────

    fun buildWsDisconnectAlert(context: Context): Notification {
        val openPending = PendingIntent.getActivity(
            context, ID_WS_DISCONNECT,
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, WS_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Brak połączenia z serwerem")
            .setContentText("Sprawdź internet/Wi-Fi/zasilanie.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Aplikacja utraciła połączenie z serwerem. Sprawdź internet, Wi-Fi lub router."
            ))
            .setContentIntent(openPending)
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWsDisconnectAlert(context: Context) {
        NotificationManagerCompat.from(context).notify(ID_WS_DISCONNECT, buildWsDisconnectAlert(context))
    }

    fun hideWsDisconnectAlert(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_WS_DISCONNECT)
    }

    // ─── Nowy ticket KDS ──────────────────────────────────────────────────────

    /**
     * Heads-up powiadomienie o nowym zamówieniu w KDS.
     *
     * Dźwięk odtwarzany jest przez [Ringtone] / [android.media.MediaPlayer] —
     * NIE przez kanał Android. Dzięki temu zmiana dźwięku w ustawieniach
     * aplikacji działa natychmiast, bez konieczności reinstalacji.
     *
     * @param soundUri  URI dźwięku z ustawień (null = domyślny dźwięk powiadomienia)
     * @param muted     true → brak dźwięku i wibracji
     */
    fun showNewKdsTicket(
        context: Context,
        orderNumber: String,
        itemCount: Int,
        isRush: Boolean,
        soundUri: String? = null,
        muted: Boolean = false
    ) {
        if (!canPostNotifications(context)) return

        // ── 1. Dźwięk przez Ringtone (niezależny od ustawień kanału) ─────
        if (!muted) {
            playKdsSound(context, soundUri)
        }

        // ── 2. Wizualne heads-up powiadomienie (kanał CICHY) ──────────────
        val openPending = PendingIntent.getActivity(
            context, orderNumber.hashCode(),
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isRush) "⚡ RUSH — Nowe zamówienie!" else "🍳 Nowe zamówienie KDS"
        val itemWord = when {
            itemCount == 1    -> "pozycja"
            itemCount in 2..4 -> "pozycje"
            else              -> "pozycji"
        }
        val text = "$orderNumber · $itemCount $itemWord"

        val notifId = ID_KDS_NEW_TICKET_BASE + (orderNumber.hashCode() and 0xFFFF)
        try {
            NotificationManagerCompat.from(context).notify(
                notifId,
                NotificationCompat.Builder(context, KDS_TICKET_CHANNEL_ID)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(false)
                    .setVibrate(if (muted) null else longArrayOf(0, 250, 100, 250))
                    .setSound(null)  // dźwięk przez Ringtone, nie przez notification
                    .setContentIntent(openPending)
                    .build()
            )
            Timber.tag("NotificationHelper").d(
                "🔔 KDS ticket notif: $orderNumber (id=$notifId, muted=$muted, sound=${soundUri ?: "default"})"
            )
        } catch (_: SecurityException) {
            Timber.tag("NotificationHelper").w("Brak uprawnień do powiadomień")
        }
    }

    /**
     * Odtwarza dźwięk KDS przez Ringtone API — jeden raz, niezależnie od kanału.
     * Jeśli poprzedni dźwięk jeszcze gra, zatrzymuje go przed uruchomieniem nowego.
     */
    private fun playKdsSound(context: Context, soundUri: String?) {
        runCatching {
            // Zatrzymaj poprzedni jeśli gra
            currentRingtone?.let { r ->
                if (r.isPlaying) r.stop()
            }
            currentRingtone = null

            val uri: Uri = soundUri
                ?.let { runCatching { Uri.parse(it) }.getOrNull() }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val ringtone = RingtoneManager.getRingtone(context, uri) ?: run {
                // Fallback do systemowego dźwięku powiadomienia
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, fallback)
            }

            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    ringtone.streamType = AudioManager.STREAM_NOTIFICATION
                }
                ringtone.play()
                currentRingtone = ringtone
                Timber.tag("NotificationHelper").d("🔊 KDS sound played: ${soundUri ?: "default"}")
            }
        }.onFailure {
            Timber.tag("NotificationHelper").w(it, "Failed to play KDS sound")
        }
    }

    /**
     * Zatrzymuje aktualnie odtwarzany dźwięk KDS (np. przy wyciszeniu).
     */
    fun stopKdsSound() {
        runCatching {
            currentRingtone?.let { if (it.isPlaying) it.stop() }
            currentRingtone = null
        }
    }

    /**
     * [updateKdsTicketChannelSound] jest zachowane dla kompatybilności,
     * ale od wersji z Ringtone API nie zmienia już kanału — dźwięk jest sterowany
     * przez [playKdsSound].
     */
    fun updateKdsTicketChannelSound(context: Context, soundUri: String?) {
        // Kanał jest CICHY — zmiana dźwięku odbywa się przez Ringtone w playKdsSound.
        // Nie ma potrzeby usuwania/tworzenia kanału.
        Timber.tag("NotificationHelper").d(
            "updateKdsTicketChannelSound: kanał KDS jest cichy, dźwięk sterowany przez Ringtone API. uri=$soundUri"
        )
    }

    // ─── Alerty systemowe / sesja ─────────────────────────────────────────────

    fun showSessionExpiredAlert(context: Context) {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle("Sesja wygasła")
            .setContentText("Musisz zalogować się ponownie.")
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID_SESSION, notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNeedsForeground(context: Context) {
        if (!canPostNotifications(context)) return
        val openPending = buildHomePending(context, 10)
        val title   = context.getString(R.string.fgs_needs_foreground_title)
        val message = context.getString(R.string.fgs_needs_foreground_message)
        val notif = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_chat, context.getString(R.string.action_open_app), openPending)
            .setContentIntent(openPending)
            .build()
        NotificationManagerCompat.from(context).notify(ID_ALERT_NEEDS_FOREGROUND, notif)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showQuotaOrTimeout(context: Context) {
        if (!canPostNotifications(context)) return
        val openPending = buildHomePending(context, 11)
        val title   = context.getString(R.string.fgs_timeout_title)
        val message = context.getString(R.string.fgs_timeout_message)
        val notif = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_chat, context.getString(R.string.action_open_app), openPending)
            .setContentIntent(openPending)
            .build()
        NotificationManagerCompat.from(context).notify(ID_ALERT_QUOTA_TIMEOUT, notif)
    }

    // ─── Dostawa zewnętrzna ───────────────────────────────────────────────────

    fun showExternalDeliverySuccess(
        context: Context,
        orderId: String?,
        orderNumber: String?,
        status: DeliveryStatusEnum? = null
    ) {
        if (!canPostNotifications(context)) return
        val notificationKey = orderId ?: System.currentTimeMillis().toString()
        val notificationId  = notificationKey.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId,
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (!orderId.isNullOrBlank()) putExtra("ORDER_ID", orderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title          = context.getString(R.string.notification_external_delivery_title)
        val safeOrderNumber = orderNumber?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_external_delivery_order_number_unknown)
        val statusText = status?.let { context.getString(it.toStringRes()) }
        val message = if (statusText != null)
            context.getString(R.string.notification_external_delivery_message_with_status, safeOrderNumber, statusText)
        else
            context.getString(R.string.notification_external_delivery_message_generic, safeOrderNumber)

        try {
            NotificationManagerCompat.from(context).notify(
                notificationId,
                NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_chat)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: SecurityException) {}
    }

    // ─── Trasa kuriera ────────────────────────────────────────────────────────

    fun showSimpleNotification(context: Context, stopsCount: Int) {
        if (!canPostNotifications(context)) return
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title   = context.getString(R.string.new_route_notification_title)
        val message = context.getString(R.string.new_route_notification_message, stopsCount)
        try {
            NotificationManagerCompat.from(context).notify(
                2,
                NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_chat)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: SecurityException) {}
    }

    // ─── Pomocnicze ───────────────────────────────────────────────────────────

    private fun buildHomePending(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun canPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

    private fun DeliveryStatusEnum.toStringRes(): Int = when (this) {
        DeliveryStatusEnum.REQUESTED   -> R.string.delivery_status_requested
        DeliveryStatusEnum.ACCEPTED    -> R.string.delivery_status_accepted
        DeliveryStatusEnum.IN_TRANSIT  -> R.string.delivery_status_in_transit
        DeliveryStatusEnum.DELIVERED   -> R.string.delivery_status_delivered
        DeliveryStatusEnum.REJECTED    -> R.string.delivery_status_rejected
        DeliveryStatusEnum.CANCELLED   -> R.string.delivery_status_cancelled
        DeliveryStatusEnum.FAILED      -> R.string.delivery_status_failed
    }
}
