package com.itsorderkds

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.itsorderkds.util.AppPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var registerDevice: RegisterDevice
    private val job = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Lekko – max kilka sekund pracy. Dłuższe rzeczy → WorkManager.
        remoteMessage.data["note_full_data"]?.let { sendNotification(it) }

        // (Opcjonalnie) lekki „ping” do Twojej logiki: np. pobierz minimalny snapshot zamówień.
        // Jeśli potrzebujesz większej roboty → WorkManager.enqueue(...)
    }

    override fun onNewToken(token: String) {
        try {
            if (AppPrefs.isInitialized()) AppPrefs.setFCMTokenChat(token)
            appPrefsWrapper.setFCMToken(token)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error in onNewToken", e)
        }
        serviceScope.launch { registerDevice(RegisterDevice.Mode.FORCEFULLY) }
    }

    private fun sendNotification(noteFullData: String) {
        try {
            val json = JSONObject(noteFullData)
            val title = json.optString("title", "Nowe zamówienie")
            val message = json.optJSONArray("subject")
                ?.optJSONObject(1)?.optString("text", "Masz nowe zamówienie")

            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "orders_channel"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(
                    channelId,
                    "Zamówienia",
                    NotificationManager.IMPORTANCE_HIGH
                )
                nm.createNotificationChannel(ch)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // dla < 26
                .setContentIntent(pending)
                .build()

            nm.notify(1001, notification)
        } catch (e: Exception) {
            Log.e("FCM", "Error parsing notification data", e)
        }
    }

    override fun onDestroy() { job.cancel(); super.onDestroy() }
}
