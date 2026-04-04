package com.itsorderkds.data.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

class LocationException(message: String) : Exception(message)

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Pobiera aktualną lokalizację urządzenia.
     *
     * 1. Sprawdza uprawnienia
     * 2. Sprawdza czy GPS jest włączony w systemie
     * 3. Próbuje pobrać ostatnią znaną lokalizację
     * 4. Jeśli nie ma ostatniej lokalizacji, próbuje pobrać nową (timeout 10s)
     *
     * @return Location? - lokalizacja lub null jeśli nie udało się pobrać
     * @throws LocationException - jeśli brak uprawnień
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        Timber.d("📍 getCurrentLocation() - rozpoczynam...")

        // 1. Sprawdzamy uprawnienia
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Timber.w("📍 Brak uprawnień ACCESS_FINE_LOCATION")
            throw LocationException("Brak uprawnień do lokalizacji. Poproś o nie w UI.")
        }

        Timber.d("📍 Uprawnienia OK")

        // 2. Sprawdzamy czy GPS jest włączony w systemie
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        Timber.d("📍 GPS enabled: $isGpsEnabled, Network enabled: $isNetworkEnabled")

        if (!isGpsEnabled && !isNetworkEnabled) {
            Timber.w("📍 GPS i sieć WYŁĄCZONE w systemie!")
            return null // GPS wyłączony - zwróć null, UI pokaże odpowiedni komunikat
        }

        // 3. Próba pobrania ostatniej znanej lokalizacji
        try {
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                Timber.d("📍 ✅ Pobrano ostatnią znaną lokalizację: lat=${lastLocation.latitude}, lng=${lastLocation.longitude}, accuracy=${lastLocation.accuracy}m")
                return lastLocation
            }

            Timber.d("📍 Ostatnia znana lokalizacja = null, próbuję pobrać nową...")
        } catch (e: Exception) {
            Timber.w(e, "📍 Błąd podczas pobierania lastLocation")
        }

        // 4. Jeśli nie ma ostatniej lokalizacji, spróbuj pobrać NOWĄ (z timeoutem 10s)
        return try {
            Timber.d("📍 Żądam nowej lokalizacji (timeout 10s)...")

            // Timeout 10s - jeśli w tym czasie nie pobierze lokalizacji, zwróć null
            withTimeoutOrNull(10_000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
            }.also { location ->
                if (location != null) {
                    Timber.d("📍 ✅ Pobrano NOWĄ lokalizację: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}m")
                } else {
                    Timber.w("📍 ⚠️ Timeout - nie udało się pobrać nowej lokalizacji w ciągu 10s")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "📍 ❌ Błąd podczas pobierania nowej lokalizacji")
            null
        }
    }
}
