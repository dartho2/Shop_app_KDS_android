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
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.itsorderkds.LoginActivity
import com.itsorderkds.R
import com.itsorderkds.data.socket.SocketAction
import com.itsorderkds.ui.order.DeliveryStatusEnum
import com.itsorderkds.ui.theme.home.HomeActivity
import timber.log.Timber

object NotificationHelper {

    // ✅ v4: Zmiana ID aby wymusić utworzenie nowego kanału BEZ dźwięku
    // (poprzednie wersje v2, v3 mogły mieć dźwięk ustawiony przez NotificationHelper)
    const val ORDER_ALARM_CHANNEL_ID = "order_alarm_channel_v4"
    private const val ORDER_ALARM_CHANNEL_NAME = "Alerty nowych zamówień"
    private const val ORDER_ALARM_CHANNEL_DESC = "Nalegające powiadomienia o nowych zamówieniach"

    private const val WS_ALERT_CHANNEL_ID = "ws_disconnect_alert_v1"
    private const val WS_ALERT_CHANNEL_NAME = "Alarm połączenia WS"
    private const val WS_ALERT_CHANNEL_DESC = "Alarmuje, gdy aplikacja traci połączenie z serwerem"
    private const val ID_WS_DISCONNECT = 1997

    //Kanał logout

    private const val CHANNEL_ID_ALERTS = "service_logout"
    // Kanał dla usługi w tle (WS)
    private const val SERVICE_CHANNEL_ID = "socket_service_channel"
    private const val SERVICE_CHANNEL_NAME = "Usługa WebSocket"
    private const val SERVICE_CHANNEL_DESC = "Informacje o połączeniu WebSocket w tle"

    // Kanał aktualizacji trasy
    private const val ROUTE_UPDATES_CHANNEL_ID = "route_updates_channel"
    private const val ROUTE_UPDATES_CHANNEL_NAME = "Aktualizacje Trasy"
    private const val ROUTE_UPDATES_CHANNEL_DESC = "Powiadomienia o zmianach w trasie kuriera"

    // Kanał alertów działania FGS (heads-up)
    private const val ALERTS_CHANNEL_ID = "fgs_alerts_channel"
    private const val ALERTS_CHANNEL_NAME = "Alerty działania w tle"
    private const val ALERTS_CHANNEL_DESC =
        "Komunikaty o ograniczeniach usługi w tle i wymaganych akcjach"

    // Stałe ID dla alertów
    private const val NOTIFICATION_ID_SESSION = 1003
    private const val ID_ALERT_NEEDS_FOREGROUND = 1001
    private const val ID_ALERT_QUOTA_TIMEOUT = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Usuń stare kanały z dźwiękiem (v2, v3)
        listOf("orders_channel", "order_alarm_channel_v2", "order_alarm_channel_v3").forEach { old ->
            mgr.getNotificationChannel(old)?.let {
                mgr.deleteNotificationChannel(old)
                Timber.d("NotificationHelper: Usunięto stary kanał: $old")
            }
        }
        mgr.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = SERVICE_CHANNEL_DESC
                setSound(null, null)
            }
        )
        // Kanał dla usługi (cichy)
        mgr.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                SERVICE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = SERVICE_CHANNEL_DESC
                setSound(null, null)
            }
        )

        // Kanał alarmowy (z dźwiękiem)
        createOrderAlarmChannel(context, mgr)
        createWsAlertChannel(context, mgr)
        // Kanał aktualizacji trasy
        mgr.createNotificationChannel(
            NotificationChannel(
                ROUTE_UPDATES_CHANNEL_ID,
                ROUTE_UPDATES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = ROUTE_UPDATES_CHANNEL_DESC }
        )

        // Kanał alertów FGS
        mgr.createNotificationChannel(
            NotificationChannel(
                ALERTS_CHANNEL_ID,
                ALERTS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ALERTS_CHANNEL_DESC
                enableVibration(true)
            }
        )
    }

    private fun createWsAlertChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val soundUri = android.media.RingtoneManager.getDefaultUri(
            android.media.RingtoneManager.TYPE_ALARM
        )
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            WS_ALERT_CHANNEL_ID,
            WS_ALERT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = WS_ALERT_CHANNEL_DESC
            enableVibration(true)
            setSound(soundUri, attrs)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun buildWsDisconnectAlert(context: Context): Notification {
        val openIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, ID_WS_DISCONNECT, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, WS_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo) // podmień na swoją ikonę
            .setContentTitle("Brak połączenia z serwerem")
            .setContentText("Sprawdź internet/Wi-Fi/zasilanie. Alarm wyłączy się po połączeniu.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)         // utrzymuj, aż wróci połączenie
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)  // pozwól odegrać dźwięk przy ponownym zgłoszeniu
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Aplikacja utraciła połączenie z serwerem. " +
                            "Sprawdź internet, Wi-Fi lub router. Po powrocie połączenia alarm zgaśnie."
                )
            )
            .setContentIntent(openPending)

        // Dla < Android 8.0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val uri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM
            )
            builder.setSound(uri)
        }

        val n = builder.build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        return n
    }
    fun showSessionExpiredAlert(context: Context) {
        // Tworzymy Intent prosto do ekranu logowania
        val intent = Intent(context, LoginActivity::class.java).apply {
            // Te flagi czyszczą historię - cofnięcie nie wróci do starego ekranu
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS) // Twój kanał alertów
            .setContentTitle("Sesja wygasła")
            .setContentText("Musisz zalogować się ponownie.")
            .setSmallIcon(R.drawable.logo) // Podstaw swoją ikonę
            .setPriority(NotificationCompat.PRIORITY_MAX) // Maksymalny priorytet
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true) // Próba pokazania od razu na ekranie
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(999, notification) // ID 999
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showWsDisconnectAlert(context: Context) {
        NotificationManagerCompat.from(context)
            .notify(ID_WS_DISCONNECT, buildWsDisconnectAlert(context))
    }

    fun hideWsDisconnectAlert(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_WS_DISCONNECT)
    }

    /**
     * Tworzy kanał alarmowy z dźwiękiem.
     */
    private fun createOrderAlarmChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // ❌ USUNIĘTO DŹWIĘK - odtwarzany przez MediaPlayer w OrderAlarmService
        // val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.order_iphone}")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            ORDER_ALARM_CHANNEL_ID,
            ORDER_ALARM_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ORDER_ALARM_CHANNEL_DESC
            enableVibration(true)
            setSound(null, attrs) // ✅ BRAK DŹWIĘKU - odtwarzany przez MediaPlayer
        }
        manager.createNotificationChannel(channel)
    }

    fun buildServiceNotification(context: Context): Notification =
        NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Połączenie WS aktywne")
            .setContentText("Odbieram zamówienia w tle")
            .setSmallIcon(R.drawable.ic_chat)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    /**
     * Buduje powiadomienie alarmowe (do użycia w startForeground()).
     */
//    fun buildOrderAlarmNotification(
//        context: Context,
//        orderJson: String,
//        notificationId: Int
//    ): Notification {
//        val fullIntent = Intent(context, HomeActivity::class.java).apply {
//            addFlags(
//                Intent.FLAG_ACTIVITY_NEW_TASK or
//                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
//                        Intent.FLAG_ACTIVITY_SINGLE_TOP
//            )
//            putExtra(SocketAction.Extra.ORDER_JSON, orderJson)
//        }
//        val fullPending = PendingIntent.getActivity(
//            context, notificationId, fullIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val builder = NotificationCompat.Builder(context, ORDER_ALARM_CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_log)
//            .setContentTitle("Nowe zamówienie!")
//            .setContentText("Kliknij, by zobaczyć szczegóły")
//            .setCategory(NotificationCompat.CATEGORY_ALARM)
//            .setPriority(NotificationCompat.PRIORITY_MAX) // < 26
//            .setFullScreenIntent(fullPending, true)
//            .setOngoing(true)
//            .setAutoCancel(false)
//            .setOnlyAlertOnce(false) // pozwól zagrać dźwięk przy ponownym alertowaniu
//
//        // Dla < Android 8.0 kanały nie istnieją – ustaw dźwięk bezpośrednio w powiadomieniu
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.order_iphone}")
//            builder.setSound(uri)
//        }
//
//        val notification = builder.build()
//        // Opcjonalnie wymuś "natarczywość" (nie wszędzie działa, ale nie szkodzi)
//        notification.flags = notification.flags or android.app.Notification.FLAG_INSISTENT
//        return notification
//    }

    /**
     * Proste powiadomienie o nowej trasie.
     */
    fun showSimpleNotification(context: Context, stopsCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.new_route_notification_title)
        val message = context.getString(R.string.new_route_notification_message, stopsCount)

        val builder = NotificationCompat.Builder(context, ROUTE_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(2, builder.build())
    }

    // =========================
    // Alerty FGS
    // =========================

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNeedsForeground(context: Context) {
        if (!canPostNotifications(context)) return

        val openIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            10,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.fgs_needs_foreground_title)
        val message = context.getString(R.string.fgs_needs_foreground_message)

        val notif = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_chat,
                context.getString(R.string.action_open_app),
                openPending
            )
            .setContentIntent(openPending)
            .build()

        NotificationManagerCompat.from(context).notify(ID_ALERT_NEEDS_FOREGROUND, notif)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showQuotaOrTimeout(context: Context) {
        if (!canPostNotifications(context)) return

        val openIntent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            11,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.fgs_timeout_title)
        val message = context.getString(R.string.fgs_timeout_message)

        val notif = NotificationCompat.Builder(context, ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_chat,
                context.getString(R.string.action_open_app),
                openPending
            )
            .setContentIntent(openPending)
            .build()

        NotificationManagerCompat.from(context).notify(ID_ALERT_QUOTA_TIMEOUT, notif)
    }

    // =========================
    // Pomocnicze
    // =========================

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Powiadomienie o pomyślnym wysłaniu zamówienia do zewnętrznej platformy dostawy
     */
    fun showExternalDeliverySuccess(
        context: Context,
        orderId: String?,
        orderNumber: String?,
        status: DeliveryStatusEnum? = null
    ) {
        if (!canPostNotifications(context)) return

        // Bezpieczne ID do PendingIntent / notify:
        // - jeśli mamy orderId → używamy go,
        // - jeśli nie → używamy timestampu, żeby było jakieś stabilne Int.
        val notificationKey = orderId ?: System.currentTimeMillis().toString()
        val notificationId = notificationKey.hashCode()

        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // ORDER_ID tylko jeśli mamy realne orderId
            if (!orderId.isNullOrBlank()) {
                putExtra("ORDER_ID", orderId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_external_delivery_title)

        // Jeśli orderNumber jest null/puste → użyj jakiegoś sensownego tekstu zastępczego
        val safeOrderNumber = orderNumber
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.notification_external_delivery_order_number_unknown)

        val statusText = status?.let { context.getString(it.toStringRes()) }

        val message = if (statusText != null) {
            context.getString(
                R.string.notification_external_delivery_message_with_status,
                safeOrderNumber,
                statusText
            )
        } else {
            context.getString(
                R.string.notification_external_delivery_message_generic,
                safeOrderNumber
            )
        }

        val builder = NotificationCompat.Builder(context, ROUTE_UPDATES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Brak uprawnień - cicho ignorujemy
        }
    }



    private fun DeliveryStatusEnum.toStringRes(): Int = when (this) {
        DeliveryStatusEnum.REQUESTED -> R.string.delivery_status_requested
        DeliveryStatusEnum.ACCEPTED -> R.string.delivery_status_accepted
        DeliveryStatusEnum.IN_TRANSIT -> R.string.delivery_status_in_transit
        DeliveryStatusEnum.DELIVERED -> R.string.delivery_status_delivered
        DeliveryStatusEnum.REJECTED -> R.string.delivery_status_rejected
        DeliveryStatusEnum.CANCELLED -> R.string.delivery_status_cancelled
        DeliveryStatusEnum.FAILED -> R.string.delivery_status_failed
    }
}
