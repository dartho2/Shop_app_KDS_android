package com.itsorderkds.ui.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


@SuppressLint("MissingPermission")
private suspend fun Context.getCurrentLocation(): Location? {
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    // Sprawdzenie uprawnień jest wymagane przez API, ale my już to robimy przed wywołaniem
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return null
    }
    return suspendCancellableCoroutine { continuation ->
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            continuation.resume(location)
        }.addOnFailureListener {
            continuation.resume(null)
        }.addOnCanceledListener {
            continuation.cancel()
        }
    }
}