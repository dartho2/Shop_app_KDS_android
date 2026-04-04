package com.itsorderkds

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.itsorderkds.ui.theme.MyForegroundService
import com.itsorderkds.util.AppPrefs
import com.itsorderkds.util.FileLoggingTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber
import java.lang.Thread.UncaughtExceptionHandler
import android.app.AlarmManager
import android.content.Context

@HiltAndroidApp
class ItsChat : Application() {


    // Log do pliku (masz już tę klasę)
    private val fileLoggingTree by lazy { FileLoggingTree(this) }

    // Globalny handler błędów korutyn – używaj go w swoich globalnych scope’ach, jeśli tworzysz takie ręcznie.
    // (viewModelScope i lifecycleScope i tak przekażą nieobsłużone wyjątki do domyślnego handlera wątku,
    // który łapiemy niżej przez Thread.setDefaultUncaughtExceptionHandler)
    val coroutineErrorHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Uncaught coroutine exception")
        FirebaseCrashlytics.getInstance().recordException(throwable)
    }

    override fun onCreate() {
        super.onCreate()

        // Preferuj jak NAJWCZEŚNIEJ
        AppPrefs.init(this)

        // 1) Timber – logcat + plik
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.plant(fileLoggingTree)

        // 2) Crashlytics + drzewo Crashlytics (zbiera >= WARN)
        FirebaseApp.initializeApp(this)

        // Logika kontroli Crashlytics:
        // - W RELEASE: zawsze enabled (BuildConfig.DEBUG = false)
        // - W DEBUG: zależy od flagi gradle.properties (wc.crashlytics.enabled.in.debug)
        val crashlyticsEnabled = if (BuildConfig.DEBUG) {
            // Czytaj flagę z gradle.properties
            try {
                val value = try {
                    BuildConfig::class.java.getField("CRASHLYTICS_ENABLED_IN_DEBUG").get(null).toString()
                } catch (_: Exception) {
                    "true"  // Default na true jeśli brak flagi
                }
                value.toBoolean()
            } catch (_: Exception) {
                false  // fallback na false
            }
        } else {
            true  // zawsze włącz w RELEASE
        }

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashlyticsEnabled)
        Timber.plant(CrashlyticsTree())

        if (BuildConfig.DEBUG) {
            Timber.i("🔍 Crashlytics in DEBUG mode: enabled=$crashlyticsEnabled")
        }

        // 3) Czyść stare logi przy starcie
        fileLoggingTree.deleteOldLogs()

        // 4) Globalny przechwyt wszystkich nieobsłużonych wyjątków -> Timber (+ Crashlytics) + AUTO-RESTART
        val systemHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            AutoRestartExceptionHandler(this, systemHandler)
        )

        // --- Twoje rzeczy poniżej (bez zmian) ---
        // Start foreground service
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Jednorazowy worker
        val workRequest = OneTimeWorkRequest.Builder(MyWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}

/**
 * Drzewo Timber, które mostkuje logi do Crashlytics (bez „spamowania” – INFO/DEBUG tylko do breadcrumbów).
 */
private class CrashlyticsTree : Timber.Tree() {
    private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Dodaj tag do breadcrumbs
        val line = (if (tag != null) "[$tag] " else "") + message
        crashlytics.log("${priorityToText(priority)} $line")

        // Poważniejsze rzeczy wyślij jako exception
        if (t != null && (priority >= Log.WARN)) {
            crashlytics.recordException(t)
        }
    }

    private fun priorityToText(@LogPriority p: Int) = when (p) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG   -> "DEBUG"
        Log.INFO    -> "INFO"
        Log.WARN    -> "WARN"
        Log.ERROR   -> "ERROR"
        Log.ASSERT  -> "ASSERT"
        else        -> "LOG($p)"
    }
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LogPriority
}

/**
 * Handler do automatycznego restartu aplikacji po crash'u.
 * Loguje błąd do Timber (+ Crashlytics), czeka 2 sekundy, potem restartuje app.
 */
private class AutoRestartExceptionHandler(
    private val context: Application,
    private val defaultHandler: UncaughtExceptionHandler?
) : UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            // 1) Log do Timber (i przez CrashlyticsTree do Crashlytics)
            Timber.e(e, "🔥 CRASH na wątku: ${t.name}")
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("thread", t.name)
                setCustomKey("crash_handled_at", System.currentTimeMillis().toString())
            }

            // 2) Czekaj 2 sekundy żeby logi zostały zapisane
            Thread.sleep(2000)

            // 3) Zaplanuj restart aplikacji
            Timber.i("🔄 Planując restart aplikacji za 1 sekundę...")
            scheduleAppRestart()

        } catch (ex: Throwable) {
            // Ignoruj błędy w samym handleru
            Timber.e(ex, "❌ Błąd w AutoRestartExceptionHandler")
        } finally {
            // ZAWSZE pozwól systemowi dokończyć (aby nie zablokowało)
            defaultHandler?.uncaughtException(t, e)
        }
    }

    /**
     * Planuje restart aplikacji za 1 sekundę używając AlarmManager.
     * Po crash'u system zwykle zabija proces, ale ta metoda się uruchomi w nowym procesie.
     */
    private fun scheduleAppRestart() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Timber.e("❌ AlarmManager niedostępny")
                return
            }

            // Intent do MainActivity (główna aktywność)
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.action = "RESTART_ACTION"

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Zaplanuj na 1 sekundę w przyszłości
            val triggerAtMs = System.currentTimeMillis() + 1000L

            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                Timber.i("✅ Restart zaplanowany na za 1s")
            } catch (ex: Exception) {
                // Fallback na setExactAndAllowWhileIdle
                Timber.w("⚠️ setAndAllowWhileIdle failed, próbuję fallback")
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                    Timber.i("✅ Restart zaplanowany (fallback)")
                } catch (ex2: Exception) {
                    Timber.e(ex2, "❌ Nie mogę zaplanować restartu")
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex, "❌ Błąd podczas planowania restartu")
        }
    }
}

