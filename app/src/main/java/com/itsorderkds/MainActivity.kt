package com.itsorderkds

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.itsorderkds.data.network.preferences.TokenProvider
import com.itsorderkds.ui.loading.AppLoadingScreen
import com.itsorderkds.ui.startNewActivity
import com.itsorderkds.ui.theme.ItsOrderChatTheme
import com.itsorderkds.ui.theme.MyForegroundService
import com.itsorderkds.ui.theme.home.HomeActivity
import com.itsorderkds.util.AppPrefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var tokenProvider: TokenProvider

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ItsOrderChatTheme {
                AppLoadingScreen()
            }
        }
        AppPrefs.init(applicationContext)

        // Migracja starych ustawień drukarek do nowego systemu (tylko raz)
        com.itsorderkds.data.preferences.PrinterMigration.migrateOldPrintersToNewSystem(applicationContext)

        // Twoje uruchomienia usług itp.
        startMyServices()
        requestIgnoreBatteryOptimizations()
        checkOverlayPermission()
        setupFCMToken()

        // Obserwuj token i podejmij decyzję nawigacyjną
        observeAccessToken()
    }

    private fun startMyServices() {
        val serviceIntent = Intent(this, MyForegroundService::class.java)
        startForegroundService(serviceIntent)

        val workRequest = OneTimeWorkRequest.Builder(MyWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(workRequest)

        checkLocationPermissionAndStartService()
    }

    private fun setupFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) AppPrefs.setFCMTokenChat(task.result)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    @Suppress("DEPRECATION")
    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun observeAccessToken() {
        tokenProvider.accessTokenFlow.asLiveData().observe(this) { token ->
            if (token == null) {
                startNewActivity(LoginActivity::class.java)
            } else {
                val intent = Intent(this, HomeActivity::class.java)
                    .putExtra(HomeActivity.EXTRA_SKIP_INITIAL_LOADER, true) // ⬅️ NOWE
                startActivity(intent)
                overridePendingTransition(0, 0) // brak animacji, zero „mrugnięcia"
            }
            finish() // żeby Main nie wracał po Back
        }
    }

    // 🎯 ZMIENIONE: Obsługa przywracania aplikacji z minimalizacji
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // W aplikacji restauracyjnej ZAWSZE chcemy świeżego ekranu z aktualnymi zamówieniami
        // Dlatego zawsze otwieramy HomeActivity na nowo (ze świeżym stanem)
        // zamiast przywracać stary stan aplikacji

        Log.d("MainActivity", "📱 Aplikacja przywracana z minimalizacji - otwieranie HomeActivity na nowo")

        val intent = Intent(this, HomeActivity::class.java)
            .putExtra(HomeActivity.EXTRA_SKIP_INITIAL_LOADER, true)
        startActivity(intent)

        // Nie zamykamy MainActivity - pozostaje w tle jako launcher
        // HomeActivity będzie widoczna dla użytkownika
    }


    private fun checkLocationPermissionAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startForegroundService(Intent(this, MyForegroundService::class.java))
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startForegroundService(Intent(this, MyForegroundService::class.java))
            } else {
                Log.w("MainActivity", "Location permission denied")
            }
        }
}
