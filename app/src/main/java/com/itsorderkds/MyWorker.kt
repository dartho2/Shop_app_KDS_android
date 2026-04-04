package com.itsorderkds


import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessaging

class MyWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Subskrybuj na powiadomienia FCM
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("subskrypcja", " sub nie powiaodla sie")
                    // Subskrypcja nie powiodła się
                }
            }
        Log.e("subskrypcja", " sub nie powiaodla sie")
        return Result.success()
    }
}
