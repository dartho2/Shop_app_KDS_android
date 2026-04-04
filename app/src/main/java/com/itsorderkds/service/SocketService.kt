package com.itsorderkds.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.itsorderkds.data.network.AuthApi
import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.data.preferences.UserPreferences
import com.itsorderkds.data.util.TokenRefreshHelper
import com.itsorderkds.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class SocketService : Service() {

    @Inject
    @Named("auth")
    lateinit var client: OkHttpClient
    @Inject
    lateinit var userPreferences: UserPreferences
    @Inject
    lateinit var tokenProvider: TokenProvider
    @Inject
    lateinit var socketEventsRepo: SocketEventsRepository
    @Inject
    lateinit var socketStaffEventsHandler: SocketStaffEventsHandler
    @Inject
    lateinit var authApi: AuthApi

    // Scope serwisu
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wsAlarmJob: Job? = null
    private var alarmPlayer: MediaPlayer? = null

    // Flagi stanu FGS/UI
    @Volatile
    private var isInFgs = false
    @Volatile
    private var appInForeground = false

    // Do odpinania obserwatora
    private var lifecycleObserver: DefaultLifecycleObserver? = null

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): SocketService = this@SocketService
    }
    // Definicja akcji Broadcastu

    override fun onBind(intent: Intent?): IBinder = binder

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createChannels(this)

        // Globalne listenery socketów
        SocketManager.onConnect = {
            socketEventsRepo.emitConnected()
            cancelWsDisconnectAlarm()  // ← WYŁĄCZ alarm po reconnect
        }
        SocketManager.onDisconnect = {
            socketEventsRepo.emitDisconnected()
            scheduleWsDisconnectAlarm() // ← WŁĄCZ alarm po rozłączeniu
        }
        SocketManager.onAuthExpired = { handleAuthExpiry() }

        // Start połączenia (socket trzymamy zawsze)
        initiateWebSocket()

        // Obserwujemy stan całej aplikacji (foreground/background)
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appInForeground = true
                // UI na wierzchu → zdejmij FGS (limit się resetuje po interakcji użytkownika)
                dropFgs()
            }

            override fun onStop(owner: LifecycleOwner) {
                appInForeground = false
                // UI zniknęło → podnieś do FGS, by słuchać legalnie w tle
                elevateToFgsSafe()
            }
        }
        lifecycleObserver = observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)

        // ⬇️ KLUCZ: jeśli serwis wystartował, gdy app jest już w tle,
        // nie dostaniemy od razu onStop(). Zrób natychmiastową próbę podniesienia do FGS,
        // żeby spełnić 5-sekundowy deadline startForegroundService().
        maybeElevateAtStartup()
    }

    private fun scheduleWsDisconnectAlarm() {
        Timber.tag("ALARM START").d("📡 [SocketService] scheduleWsDisconnectAlarm() - Rozpoczynam 30s countdown")
        if (wsAlarmJob?.isActive == true) return
        wsAlarmJob = serviceScope.launch {
            delay(30_000) // debounce, żeby nie wyć przy krótkich zanikach
            if (!SocketManager.isConnected()) {
                Timber.tag("SocketService").w("WS: brak połączenia > ALARM")
                Timber.tag("ALARM START").w("⚠️ [SocketService] 30s minęło i brak połączenia - uruchamiam alarm WS!")
                if (Build.VERSION.SDK_INT >= 31 && !isInFgs) {
                    elevateToFgsSafe() // stabilność w tle
                }
                NotificationHelper.showWsDisconnectAlert(this@SocketService)
                startAlarmSound()
            }
        }
    }

    private fun cancelWsDisconnectAlarm() {
        wsAlarmJob?.cancel()
        wsAlarmJob = null
        stopAlarmSound()
        NotificationHelper.hideWsDisconnectAlert(this)
    }

    private fun startAlarmSound() {
        Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag("ALARM START").w("🔊 [SocketService] startAlarmSound() - WS DISCONNECT ALARM")
        Timber.tag("ALARM START").w("   ├─ alarmPlayer: ${if (alarmPlayer == null) "null" else "exists"}")
        Timber.tag("ALARM START").w("   ├─ isConnected: ${SocketManager.isConnected()}")
        Timber.tag("ALARM START").w("   ├─ Thread: ${Thread.currentThread().name}")
        Timber.tag("ALARM START").w("   ├─ Stack trace:")
        Thread.currentThread().stackTrace.take(8).forEachIndexed { idx, element ->
            Timber.tag("ALARM START").w("   │  $idx: $element")
        }
        Timber.tag("ALARM START").w("   └─ Timestamp: ${System.currentTimeMillis()}")

        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            alarmPlayer?.release()
            alarmPlayer = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@SocketService, uri)
                isLooping = true
                prepare()
                start()
            }
            Timber.tag("ALARM START").w("✅ [SocketService] Alarm WS DISCONNECT uruchomiony!")
            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (t: Throwable) {
            Timber.tag("ALARM START").e(t, "❌ [SocketService] Nie udało się uruchomić dźwięku alarmu")
            Timber.tag("ALARM START").w("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag("SocketService").e(t, "Nie udało się uruchomić dźwięku alarmu")
        }
    }

    private fun stopAlarmSound() {
        try {
            alarmPlayer?.stop()
        } catch (_: Throwable) {
        }
        try {
            alarmPlayer?.release()
        } catch (_: Throwable) {
        }
        alarmPlayer = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // System może restartować serwis – chcemy wrócić do życia
        return START_STICKY
    }

    private fun initiateWebSocket() {
        serviceScope.launch {
            val namespace = "/staff"
            if (SocketManager.init(namespace, client, tokenProvider)) {
                socketStaffEventsHandler.register()
                SocketManager.connect()
            } else {
                socketEventsRepo.emitDisconnected()
                // Retry po chwili (np. brak tokenu przy starcie)
                delay(5_000)
                if (!SocketManager.isConnected()) initiateWebSocket()
            }
        }
    }

    private fun handleAuthExpiry() {
        serviceScope.launch {
            val result = TokenRefreshHelper.refreshBlocking(tokenProvider, authApi)

            when (result) {
                is TokenRefreshHelper.RefreshResult.Success -> {
                    SocketManager.clear()
                    initiateWebSocket()
                }

                is TokenRefreshHelper.RefreshResult.SessionExpired -> {
                    Timber.tag("SocketService").e("⛔ SESJA WYGASŁA (401). Procedura awaryjna.")
                    Timber.tag("ALARM START").w("⚠️ [SocketService] SESJA WYGASŁA - uruchamiam alarm!")

                    // 1. Czyścimy dane w tle (dla pewności)
                    tokenProvider.clearAccessTokens() // Metoda, którą (chyba) masz
                    userPreferences.clear()     // Metoda z Twojego kodu HomeActivity

                    // 2. Rozłączamy socket
                    try { SocketManager.disconnect() } catch (_: Exception) {}

                    // 3. Uruchamiamy alarm dźwiękowy (tak jak chciałeś)
                    startAlarmSound()

                    // 4. Wysyłamy sygnał do HomeActivity, żeby zamknęło ekran
                    val broadcastIntent = Intent(ACTION_FORCE_LOGOUT)
                    broadcastIntent.setPackage(packageName) // Bezpieczeństwo: tylko nasza apka
                    sendBroadcast(broadcastIntent)

                    // 5. Pokaż powiadomienie, które po kliknięciu otworzy LoginActivity
                    // (W przypadku gdy aplikacja jest w tle i Broadcast nie zadziała natychmiast)
                    NotificationHelper.showSessionExpiredAlert(this@SocketService)

                    // UWAGA: Nie robimy stopSelf(), czekamy aż HomeActivity nas ubije przez logout()
                    // albo użytkownik kliknie w powiadomienie.
                }

                is TokenRefreshHelper.RefreshResult.Failed -> {
                    stopSelfSafely()
                }
            }
        }
    }


    /** Jeśli app już jest w tle w momencie startu serwisu, podnieś od razu do FGS. */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun maybeElevateAtStartup() {
        val state = ProcessLifecycleOwner.get().lifecycle.currentState
        appInForeground = state.isAtLeast(Lifecycle.State.STARTED)
        if (!appInForeground) {
            // Spróbuj natychmiast – to pomaga w scenariuszu startForegroundService() z tła
            elevateToFgsSafe()
        }
    }

    /**
     * Podnosi do FGS (dataSync) tylko gdy ma to sens; łapie budżetowe/bezpieczeństwa wyjątki.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun elevateToFgsSafe() {
        if (isInFgs || appInForeground) return

        val notif = NotificationHelper.buildServiceNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    FOREGROUND_ID,
                    notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(FOREGROUND_ID, notif)
            }
            isInFgs = true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Np. wyczerpany budżet 6h/24h albo system zabrania startu FGS w tej chwili
            NotificationHelper.showNeedsForeground(this)
            // Socket dalej działa, ale system może ubić process – najlepsze wyjście:
            // użytkownik otwiera UI → dropFgs() → licznik FGS resetuje się po interakcji.
        } catch (e: SecurityException) {
            // Brak POST_NOTIFICATIONS (API 33+) lub brak deklaracji FOREGROUND_SERVICE*_ w Manifeście
            // Zaloguj i rozważ poproszenie o zgodę z poziomu UI.
        } catch (t: Throwable) {
            // Defensive: nie pozwól, by wyjątek ubił proces
        }
    }

    /** Zdejmuje FGS, gdy UI jest na wierzchu – socket zostaje. */
    private fun dropFgs() {
        if (!isInFgs) return
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE) // usuń notyfikację
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Throwable) {
            // ignoruj
        } finally {
            isInFgs = false
        }
    }

    /**
     * Android 15 (API 35): po przekroczeniu 6h/24h dla dataSync system woła onTimeout().
     * MUSISZ zatrzymać FGS/serwis – inaczej RemoteServiceException i crash.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @RequiresApi(35)
    override fun onTimeout(foregroundServiceType: Int, reason: Int) {
        NotificationHelper.showQuotaOrTimeout(this)
        if (isInFgs) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            isInFgs = false
        }
        stopSelfSafely()
    }

    private fun stopSelfSafely() {
        try {
            SocketManager.disconnect()
        } catch (_: Throwable) {
        }
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Dzięki START_STICKY system i tak spróbuje nas wznowić
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            lifecycleObserver?.let { ProcessLifecycleOwner.get().lifecycle.removeObserver(it) }
        } catch (_: Throwable) {
        }
        try {
            SocketManager.disconnect()
        } catch (_: Throwable) {
        }
        serviceScope.cancel()
        isInFgs = false
        wsAlarmJob?.cancel()
        stopAlarmSound()
        NotificationHelper.hideWsDisconnectAlert(this)
    }

    companion object {
        private const val FOREGROUND_ID = 1
        const val ACTION_FORCE_LOGOUT = "com.itsorderkds.ACTION_FORCE_LOGOUT"
    }
}
