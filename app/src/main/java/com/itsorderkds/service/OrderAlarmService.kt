package com.itsorderkds.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.itsorderkds.R
import com.itsorderkds.data.preferences.AppPreferencesManager
import com.itsorderkds.data.socket.SocketAction
import com.itsorderkds.ui.theme.home.HomeActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class OrderAlarmService : Service() {

    @Inject lateinit var prefs: AppPreferencesManager

    private var player: MediaPlayer? = null
    private var currentNotificationId: Int? = null
    private var isAlarmActive: Boolean = false  // ✅ FIX: Guard przed podwójnym startem dźwięku

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createAlarmNotificationChannel()
    }

    private fun createAlarmNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Użyj zasobów zamiast twardych tekstów
        val channelName = getString(R.string.order_alarm_channel_name)
        val channelDesc = getString(R.string.order_alarm_channel_desc)
        val importance = NotificationManager.IMPORTANCE_HIGH

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(ALARM_CHANNEL_ID, channelName, importance).apply {
            description = channelDesc
            enableVibration(true)
            setBypassDnd(true)
            setSound(null, attrs) // dźwięk odtwarzamy ręcznie (MediaPlayer)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("ALARM START").w("🎯 [OrderAlarmService] onStartCommand")
        Timber.tag("ALARM START").w("   ├─ action: ${intent?.action}")
        Timber.tag("ALARM START").w("   ├─ startId: $startId")
        Timber.tag("ALARM START").w("   ├─ currentNotificationId: $currentNotificationId")
        Timber.tag("ALARM START").w("   ├─ currentOrderId: $currentOrderId")
        Timber.tag("ALARM START").w("   ├─ isAlarmActive: $isAlarmActive")
        Timber.tag("ALARM START").w("   ├─ player?.isPlaying: ${player?.isPlaying}")
        Timber.tag("ALARM START").w("   ├─ Thread: ${Thread.currentThread().name}")
        Timber.tag("ALARM START").w("   └─ Timestamp: ${System.currentTimeMillis()}")

        Timber.d("OrderAlarmService: onStartCommand, action=${intent?.action}, startId=$startId")

        val action = intent?.action

        // 0) Android 13+: brak zgody → „dotknięcie” FG i wyjście
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        // 1) STOP – tylko jeśli dotyczy bieżącego alarmu (gdy podano orderId)
        if (action == ACTION_STOP_ALARM) {
            val requestedOrderId = intent.getStringExtra(EXTRA_ORDER_ID)
            if (!requestedOrderId.isNullOrBlank() && currentOrderId != null && requestedOrderId != currentOrderId) {
                Timber.w("ACTION_STOP_ALARM pominięty: requested=$requestedOrderId, current=$currentOrderId")
                return START_STICKY
            }
            tryEnsurePlaceholderForeground()
            handleStopAction(startId)
            return START_NOT_STICKY
        }

        // 2) Restart STICKY z pustym intentem — KDS nie wznawia dźwięku w pętli
        if (intent == null) {
            tryEnsurePlaceholderForeground()
            handleStopAction(startId)
            return START_NOT_STICKY
        }

        // 3) Natychmiast wejdź do FG (placeholder) – chroni przed 5s timeoutem
        ensurePlaceholderForeground()

        // 4) Payload
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID)
        val orderJson = intent.getStringExtra(EXTRA_ORDER_JSON)
        if (orderId.isNullOrEmpty() || orderJson.isNullOrEmpty()) {
            Timber.e("Brak EXTRA_ORDER_ID/EXTRA_ORDER_JSON. Zatrzymuję (startId=$startId).")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val notificationId = orderId.hashCode()

        when (action) {
            ACTION_RING -> {
                val notif = buildAlarmNotification(orderJson, useFullScreen = false)
                if (currentNotificationId == null) {
                    currentNotificationId = notificationId
                    currentOrderId = orderId
                    updateForeground(notificationId, notif)
                } else {
                    currentOrderId = orderId
                    NotificationManagerCompat.from(this).notify(currentNotificationId!!, notif)
                }
                restartAlarmSound()
                return START_NOT_STICKY  // KDS: nie restartuj serwisu automatycznie
            }

            ACTION_START, null -> {
                if (currentNotificationId != null) {
                    Timber.w("Alarm ($currentNotificationId) już aktywny. Ignoruję nowe $orderId (startId=$startId).")
                    return START_NOT_STICKY
                }
                currentNotificationId = notificationId
                currentOrderId = orderId
                Timber.i("Aktywuję nowy alarm dla orderId: $orderId (notifId: $notificationId)")
                val notif = buildAlarmNotification(orderJson, useFullScreen = true)
                updateForeground(notificationId, notif)
                startAlarmSound()
                return START_NOT_STICKY  // KDS: nie restartuj serwisu automatycznie
            }

            else -> {
                Timber.w("Nieznana akcja: $action – traktuję jak RING.")
                val notif = buildAlarmNotification(orderJson, useFullScreen = false)
                if (currentNotificationId == null) {
                    currentNotificationId = notificationId
                    updateForeground(notificationId, notif)
                } else {
                    NotificationManagerCompat.from(this).notify(currentNotificationId!!, notif)
                }
                restartAlarmSound()
                return START_NOT_STICKY  // KDS: nie restartuj serwisu automatycznie
            }
        }
    }

    private fun buildAlarmNotification(orderJson: String, useFullScreen: Boolean): Notification {
        val fullPending = buildFullScreenPending(orderJson)
        val stopPending = buildStopPending()
        val channelSettingsPending = buildChannelSettingsPending()

        return NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_log)
            .setContentTitle(getString(R.string.order_alarm_title))
            .setContentText(getString(R.string.order_alarm_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullPending, useFullScreen)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_log, getString(R.string.order_alarm_mute), stopPending)
            .addAction(R.drawable.ic_log, getString(R.string.order_alarm_settings), channelSettingsPending)
            .build()
    }

    private fun buildFullScreenPending(orderJson: String): PendingIntent {
        val fullIntent = Intent(this, HomeActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(SocketAction.Extra.ORDER_JSON, orderJson)
        }
        return PendingIntent.getActivity(
            this, (currentNotificationId ?: PLACEHOLDER_ID), fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildStopPending(): PendingIntent {
        val stopIntent = Intent(this, OrderAlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        return PendingIntent.getService(
            this, 1001, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildChannelSettingsPending(): PendingIntent {
        val channelSettingsIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, ALARM_CHANNEL_ID)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            this, 1002, channelSettingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Placeholder, który natychmiast podnosi foreground. */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun ensurePlaceholderForeground() {
        val placeholder = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_log)
            .setContentTitle(getString(R.string.order_alarm_boot_title))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    PLACEHOLDER_ID,
                    placeholder,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(PLACEHOLDER_ID, placeholder)
            }
            Timber.i("Foreground placeholder wystartował.")
        } catch (e: Exception) {
            Timber.e(e, "startForeground(placeholder) nie powiódł się")
            NotificationManagerCompat.from(this).notify(PLACEHOLDER_ID, placeholder)
        }
    }

    /** Bezpieczna próba wejścia do foreground – ignoruje wyjątki i brak uprawnień. */
    private fun tryEnsurePlaceholderForeground() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                ensurePlaceholderForeground()
            }
        } catch (t: Throwable) {
            Timber.w(t, "ensurePlaceholderForeground() – pominięte (brak uprawnień lub już w FG)")
        }
    }

    /** Podmienia placeholder na właściwą notyfikację foreground. */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateForeground(id: Int, notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                @Suppress("DEPRECATION")
                startForeground(id, notification)
            }

            if (id != PLACEHOLDER_ID) {
                NotificationManagerCompat.from(this).cancel(PLACEHOLDER_ID)
            }
            Timber.i("Foreground przełączony na docelowe powiadomienie (id=$id).")
        } catch (e: Exception) {
            Timber.e(e, "Nie udało się zaktualizować powiadomienia foreground")
            NotificationManagerCompat.from(this).notify(id, notification)
        }
    }

    private fun startAlarmSound() {
        Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("ALARM START").w("🔊 [OrderAlarmService] startAlarmSound()")
        Timber.tag("ALARM START").w("   ├─ isAlarmActive: $isAlarmActive")
        Timber.tag("ALARM START").w("   ├─ player?.isPlaying: ${player?.isPlaying}")
        Timber.tag("ALARM START").w("   ├─ currentOrderId: $currentOrderId")
        Timber.tag("ALARM START").w("   ├─ Thread: ${Thread.currentThread().name}")
        Timber.tag("ALARM START").w("   └─ Timestamp: ${System.currentTimeMillis()}")

        // ✅ FIX: Jeśli alarm już gra, pomiń wszystko
        if (isAlarmActive && player?.isPlaying == true) {
            Timber.tag("ALARM START").e("❌ ALARM BLOCKED: Alarm już aktywny i gra!")
            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.d("startAlarmSound: Alarm już aktywny i gra. Pomiń.")
            return
        }

        if (player?.isPlaying == true) {
            Timber.tag("ALARM START").e("❌ ALARM BLOCKED: Dźwięk już gra!")
            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.d("startAlarmSound: Dźwięk już gra.")
            return
        }

        // ✅ Zaznacz alarm jako aktywny, ale NIE zatrzymuj poprzedniego playera jeśli gra
        if (!isAlarmActive) {
            stopAlarmSound()
        }
        isAlarmActive = true

        try {
            // Pobierz wybrany dźwięk z preferencji (najpierw per-typ 'order_alarm', potem globalny)
            val selected = runBlocking { prefs.getNotificationSoundUri("order_alarm") } ?: runBlocking { prefs.getNotificationSoundUri() } ?: "android.resource://$packageName/${R.raw.alarm1}"
            val customUri = selected.toUri()
            Timber.tag("ALARM START").d("🎵 Wybrany dźwięk: $selected")

            player = MediaPlayer().apply {
                setDataSource(applicationContext, customUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false   // KDS: dźwięk tylko raz — nie alarm w pętli
                setOnCompletionListener {
                    // Po zakończeniu dźwięku zatrzymaj serwis — nie ma po co działać dalej
                    Timber.d("Dźwięk nowego zamówienia zakończony — zatrzymuję serwis.")
                    stopAlarmSound()
                    stopSelf()
                }
                prepare()
                start()
            }
            Timber.tag("ALARM START").w("✅ Odtwarzanie alarmu rozpoczęte (loop)!")
            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.d("Odtwarzanie alarmu (loop) rozpoczęte.")
        } catch (e: Exception) {
            Timber.tag("ALARM START").e(e, "❌ Nie można odtworzyć dzwonka alarmu!")
            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.e(e, "Nie można odtworzyć dzwonka alarmu.")
            isAlarmActive = false
        }
    }

    private fun restartAlarmSound() {
        // KDS: nie restartujemy dźwięku w pętli.
        // Jeśli dźwięk już gra — nic nie rób.
        // Jeśli nie gra (nowe zamówienie) — zagraj raz od początku.
        if (player?.isPlaying == true) {
            Timber.d("restartAlarmSound: dźwięk już gra — pomijam.")
            return
        }
        stopAlarmSound()
        startAlarmSound()
    }

    private fun stopAlarmSound() {
        if (player == null) return
        Timber.d("stopAlarmSound: Zatrzymuję i zwalniam MediaPlayer.")
        isAlarmActive = false  // ✅ FIX: Zaznacz alarm jako nieaktywny
        try {
            if (player?.isPlaying == true) {
                player?.stop()
            }
            player?.release()
        } catch (e: IllegalStateException) {
            Timber.w(e, "MediaPlayer w złym stanie podczas stop/release.")
        }
        player = null
    }

    private fun handleStopAction(startId: Int?) {
        Timber.d("handleStopAction: wyciszam i zamykam alarm")

        stopAlarmSound()

        currentNotificationId?.let {
            NotificationManagerCompat.from(this).cancel(it)
        }
        currentNotificationId = null
        currentOrderId = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        NotificationManagerCompat.from(this).cancel(PLACEHOLDER_ID)

        if (startId != null) {
            stopSelf(startId)
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("OrderAlarmService: onDestroy – cleanup.")
        stopAlarmSound()
        currentNotificationId?.let {
            NotificationManagerCompat.from(this).cancel(it)
            currentNotificationId = null
        }
        currentOrderId = null
        NotificationManagerCompat.from(this).cancel(PLACEHOLDER_ID)
    }

    companion object {
        const val EXTRA_ORDER_JSON = "EXTRA_ORDER_JSON"
        const val EXTRA_ORDER_ID = "EXTRA_ORDER_ID"

        const val ACTION_START = "ACTION_START_ALARM"
        const val ACTION_RING  = "ACTION_RING_ALARM"
        const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"

        // ✅ v4: Zmiana ID aby wymusić nowy kanał BEZ dźwięku (dźwięk z MediaPlayer)
        private const val ALARM_CHANNEL_ID = "order_alarm_channel_v4"
        private const val PLACEHOLDER_ID = 999_001
    }

    private var currentOrderId: String? = null
}
