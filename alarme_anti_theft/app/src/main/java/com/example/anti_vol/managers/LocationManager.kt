package com.example.anti_vol.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class LocationManager private constructor(private val context: Context) {

    companion object {
        private const val LOCATION_TIMEOUT = 8000L

        @Volatile
        private var INSTANCE: LocationManager? = null

        fun getInstance(context: Context): LocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val locationManager: AndroidLocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
    }

    data class LocationInfo(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val address: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun getGoogleMapsUrl(): String {
            return "https://www.google.com/maps?q=$latitude,$longitude"
        }

        fun getFormattedLocation(): String {
            return "📍 Location: $address\n" +
                    "🌐 Coordinates: $latitude, $longitude\n" +
                    "🎯 Accuracy: ${accuracy}m\n" +
                    "🔗 Maps: ${getGoogleMapsUrl()}"
        }
    }

    interface LocationCallback {
        fun onLocationReceived(locationInfo: LocationInfo)
        fun onLocationError(error: String)
    }

    fun getCurrentLocation(callback: LocationCallback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Location permission not granted")
            return
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled")
            return
        }

        val lastKnownLocation = getLastKnownLocation()
        if (lastKnownLocation != null) {
            createLocationInfo(lastKnownLocation, callback)
            return
        }

        val scope = CoroutineScope(Dispatchers.Main)
        var locationReceived = false

        val timeoutJob = scope.launch {
            delay(LOCATION_TIMEOUT)
            if (!locationReceived) {
                callback.onLocationError("Location request timed out")
            }
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!locationReceived) {
                    locationReceived = true
                    timeoutJob.cancel()

                    try {
                        locationManager.removeUpdates(this)
                    } catch (e: Exception) {
                    }

                    createLocationInfo(location, callback)
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                if (locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.GPS_PROVIDER,
                        1000L,
                        0f,
                        locationListener
                    )
                } else if (locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        AndroidLocationManager.NETWORK_PROVIDER,
                        1000L,
                        0f,
                        locationListener
                    )
                } else {
                    timeoutJob.cancel()
                    callback.onLocationError("No location providers available")
                }
            }
        } catch (e: Exception) {
            timeoutJob.cancel()
            callback.onLocationError("Error requesting location: ${e.message}")
        }
    }

    private fun getLastKnownLocation(): Location? {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                val gpsLocation = locationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
                val networkLocation = locationManager.getLastKnownLocation(AndroidLocationManager.NETWORK_PROVIDER)

                when {
                    gpsLocation != null && networkLocation != null -> {
                        if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                    }
                    gpsLocation != null -> gpsLocation
                    networkLocation != null -> networkLocation
                    else -> null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createLocationInfo(location: Location, callback: LocationCallback) {
        val simpleAddress = "Coordinates: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"

        val locationInfo = LocationInfo(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            address = simpleAddress
        )

        callback.onLocationReceived(locationInfo)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    fun getQuickLocationInfo(): String {
        return try {
            if (!hasLocationPermission()) {
                "Location permission not available"
            } else if (!isLocationEnabled()) {
                "Location services disabled"
            } else {
                val gpsEnabled = locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER)
                val networkEnabled = locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)

                "GPS: ${if (gpsEnabled) "✓" else "✗"}, Network: ${if (networkEnabled) "✓" else "✗"}"
            }
        } catch (e: Exception) {
            "Location info unavailable"
        }
    }
}