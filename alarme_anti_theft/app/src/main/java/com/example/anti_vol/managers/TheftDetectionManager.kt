package com.example.anti_vol.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.anti_vol.services.TelegramService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TheftDetectionManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: TheftDetectionManager? = null

        fun getInstance(context: Context): TheftDetectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TheftDetectionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val cameraManager = CameraManager.getInstance(context)
    private val locationManager = LocationManager.getInstance(context)

    fun handleTheftDetection(theftType: String) {
        Thread {
            try {
                var photoPath: String? = null
                var locationInfo: String? = null

                try {
                    val photoLatch = CountDownLatch(1)
                    cameraManager.captureSelfieSilently(object : CameraManager.CaptureCallback {
                        override fun onCaptureSuccess(photoFile: File) {
                            photoPath = photoFile.absolutePath
                            photoLatch.countDown()
                        }

                        override fun onCaptureError(error: String) {
                            photoLatch.countDown()
                        }
                    })

                    photoLatch.await(8, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    // Photo capture failed
                }

                try {
                    val locationLatch = CountDownLatch(1)
                    locationManager.getCurrentLocation(object : LocationManager.LocationCallback {
                        override fun onLocationReceived(locationInfo_: LocationManager.LocationInfo) {
                            locationInfo = locationInfo_.getFormattedLocation()
                            locationLatch.countDown()
                        }

                        override fun onLocationError(error: String) {
                            locationInfo = "📍 Location: $error\n🕐 Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                            locationLatch.countDown()
                        }
                    })

                    val locationSuccess = locationLatch.await(6, TimeUnit.SECONDS)
                    if (!locationSuccess && locationInfo == null) {
                        locationInfo = "📍 Location: Timeout\n🕐 Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                    }
                } catch (e: Exception) {
                    locationInfo = "📍 Location: Error - ${e.message}"
                }

                sendCompleteTheftAlert(theftType, photoPath, locationInfo)

            } catch (e: Exception) {
                sendCompleteTheftAlert(theftType, null, "Evidence collection failed: ${e.message}")
            }
        }.start()
    }

    private fun sendCompleteTheftAlert(theftType: String, photoPath: String?, locationInfo: String?) {
        try {
            val intent = Intent(context, TelegramService::class.java).apply {
                action = TelegramService.ACTION_SEND_THEFT_ALERT
                putExtra(TelegramService.EXTRA_THEFT_TYPE, theftType)
                putExtra(TelegramService.EXTRA_PHOTO_PATH, photoPath)
                putExtra(TelegramService.EXTRA_LOCATION_INFO, locationInfo)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // Telegram service failed
        }
    }

    fun handleEmergencyTheft(theftType: String) {
        handleTheftDetection(theftType)
    }

    fun testTheftResponse() {
        handleTheftDetection("TEST_THEFT")
    }

    fun cleanup() {
    }
}