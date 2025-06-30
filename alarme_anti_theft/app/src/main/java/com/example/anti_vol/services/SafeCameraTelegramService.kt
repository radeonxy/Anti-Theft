package com.example.anti_vol.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.anti_vol.managers.CameraManager
import com.example.anti_vol.managers.LocationManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SafeCameraTelegramService : Service() {

    companion object {
        const val ACTION_CAPTURE_AND_SEND = "CAPTURE_AND_SEND"
        const val EXTRA_THEFT_TYPE = "theft_type"
    }

    private lateinit var cameraManager: CameraManager
    private lateinit var locationManager: LocationManager

    override fun onCreate() {
        super.onCreate()
        try {
            cameraManager = CameraManager.getInstance(this)
            locationManager = LocationManager.getInstance(this)
        } catch (e: Exception) {
            // Initialization failed
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CAPTURE_AND_SEND -> {
                val theftType = intent.getStringExtra(EXTRA_THEFT_TYPE) ?: "UNKNOWN"
                startCapture(theftType)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCapture(theftType: String) {
        Thread {
            try {
                var photoPath: String? = null
                var locationInfo: String? = null

                val photoThread = Thread {
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
                        photoLatch.await(6, TimeUnit.SECONDS)
                    } catch (e: Exception) {
                        // Photo capture failed
                    }
                }

                val locationThread = Thread {
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

                        val locationSuccess = locationLatch.await(5, TimeUnit.SECONDS)
                        if (!locationSuccess && locationInfo == null) {
                            locationInfo = "📍 Location: Timeout\n🕐 Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
                        }
                    } catch (e: Exception) {
                        locationInfo = "📍 Location: Error - ${e.message}"
                    }
                }

                photoThread.apply {
                    name = "PhotoThread"
                    priority = Thread.NORM_PRIORITY
                }.start()

                locationThread.apply {
                    name = "LocationThread"
                    priority = Thread.NORM_PRIORITY
                }.start()

                try {
                    photoThread.join(7000)
                    locationThread.join(7000)
                } catch (e: Exception) {
                    // Thread join failed
                }

                sendTelegram(theftType, photoPath, locationInfo)

            } catch (e: Exception) {
                sendTelegram(theftType, null, "Collection failed: ${e.message}")
            } finally {
                stopSelf()
            }
        }.apply {
            name = "CaptureThread"
            priority = Thread.NORM_PRIORITY
            isDaemon = true
        }.start()
    }

    private fun sendTelegram(theftType: String, photoPath: String?, locationInfo: String?) {
        try {
            val telegramIntent = Intent(this, TelegramService::class.java).apply {
                action = TelegramService.ACTION_SEND_THEFT_ALERT
                putExtra(TelegramService.EXTRA_THEFT_TYPE, theftType)
                putExtra(TelegramService.EXTRA_PHOTO_PATH, photoPath)
                putExtra(TelegramService.EXTRA_LOCATION_INFO, locationInfo)
            }
            startService(telegramIntent)
        } catch (e: Exception) {
            // Telegram service failed
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}