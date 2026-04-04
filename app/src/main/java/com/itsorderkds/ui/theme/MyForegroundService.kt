package com.itsorderkds.ui.theme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.itsorderkds.R
import com.itsorderkds.ui.theme.home.HomeActivity

class MyForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "foreground_service_channel",
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Stworzenie powiadomienia i uruchomienie usługi w trybie pierwszoplanowym
        val notificationIntent = Intent(this, HomeActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "foreground_service_channel")
            .setContentTitle("Foreground Service") // Tytuł powiadomienia
            .setContentText("This is a foreground service") // Treść powiadomienia
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ikona powiadomienia
            .setContentIntent(pendingIntent) // Akcja po kliknięciu powiadomienia
            .build()

        // Uruchomienie usługi jako pierwszoplanowej
        startForeground(1, notification)

        // Kod wykonywany przez usługę
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup resources if needed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
