package com.example.anti_vol.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.anti_vol.R
import com.example.anti_vol.data.PreferencesManager

class DetectionService : Service(), SensorEventListener {

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "anti_theft_detection"

        const val ACTION_THEFT_DETECTED = "com.example.anti_vol.THEFT_DETECTED"
        const val EXTRA_THEFT_TYPE = "theft_type"

        const val ACTION_START_PROTECTION = "START_PROTECTION"
        const val ACTION_STOP_PROTECTION = "STOP_PROTECTION"
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var isProtectionActive = false
    private var isChargerConnected = false
    private var isChargerBeingMonitored = false

    private var lastAccelerometerValues = FloatArray(3)
    private var lastMovementTime = 0L
    private var isInCooldown = false
    private val movementCooldownMs = 10000L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        isChargerConnected = true
                        if (isProtectionActive) {
                            isChargerBeingMonitored = true
                        }
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        isChargerConnected = false
                        if (isProtectionActive && isChargerBeingMonitored) {
                            handleTheftDetected("CHARGER_DISCONNECTED")
                        }
                        isChargerBeingMonitored = false
                    }
                }
            } catch (e: Exception) {
                // Battery receiver error
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            preferencesManager = PreferencesManager.getInstance(this)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            setupWakeLock()
            createNotificationChannel()
            setupBatteryMonitoring()

        } catch (e: Exception) {
            // Service creation error
        }
    }

    private fun setupWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AntiTheft::DetectionWakeLock"
            )
        } catch (e: Exception) {
            // Wake lock setup error
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START_PROTECTION -> startProtection()
                ACTION_STOP_PROTECTION -> stopProtection()
                else -> {
                    if (preferencesManager.isProtectionEnabled()) {
                        startProtection()
                    } else {
                        stopSelf()
                    }
                }
            }
        } catch (e: Exception) {
            // Start command error
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Anti-Theft Detection",
                    NotificationManager.IMPORTANCE_LOW
                )
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            // Notification channel error
        }
    }

    private fun setupBatteryMonitoring() {
        try {
            val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            isChargerConnected = chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
                    chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                    chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(batteryReceiver, filter)
            }
        } catch (e: Exception) {
            // Battery monitoring setup error
        }
    }

    private fun startProtection() {
        try {
            if (isProtectionActive) return
            isProtectionActive = true

            if (isChargerConnected) {
                isChargerBeingMonitored = true
            }

            startForeground(NOTIFICATION_ID, createDetectionNotification())
            wakeLock?.acquire(2 * 60 * 60 * 1000L)
            startMovementDetection()

        } catch (e: Exception) {
            // Protection start error
        }
    }

    private fun startMovementDetection() {
        try {
            accelerometer?.let { sensor ->
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: Exception) {
            // Movement detection error
        }
    }

    private fun stopProtection() {
        try {
            isProtectionActive = false
            isChargerBeingMonitored = false
            sensorManager.unregisterListener(this)
            wakeLock?.let { if (it.isHeld) it.release() }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            // Protection stop error
        }
    }

    private fun handleTheftDetected(theftType: String) {
        try {
            // Start alarm service immediately
            try {
                val alarmIntent = Intent(this, AlarmService::class.java).apply {
                    putExtra(EXTRA_THEFT_TYPE, theftType)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(alarmIntent)
                } else {
                    startService(alarmIntent)
                }
            } catch (e: Exception) {
                return
            }

            // Start camera/telegram service immediately
            try {
                val safeCameraIntent = Intent(this, SafeCameraTelegramService::class.java).apply {
                    action = SafeCameraTelegramService.ACTION_CAPTURE_AND_SEND
                    putExtra(SafeCameraTelegramService.EXTRA_THEFT_TYPE, theftType)
                }
                startService(safeCameraIntent)
            } catch (e: Exception) {
                // Camera service error - alarm continues
            }

            // Send broadcast
            try {
                sendBroadcast(Intent(ACTION_THEFT_DETECTED).apply {
                    putExtra(EXTRA_THEFT_TYPE, theftType)
                })
            } catch (e: Exception) {
                // Broadcast error
            }

        } catch (e: Exception) {
            // Theft detection error
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        try {
            if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER || !isProtectionActive || isInCooldown) {
                return
            }

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val deltaX = Math.abs(x - lastAccelerometerValues[0])
            val deltaY = Math.abs(y - lastAccelerometerValues[1])
            val deltaZ = Math.abs(z - lastAccelerometerValues[2])
            val totalDelta = deltaX + deltaY + deltaZ

            if (totalDelta > 25.0f) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastMovementTime > movementCooldownMs) {
                    lastMovementTime = currentTime
                    startMovementCooldown()
                    handleTheftDetected("DEVICE_MOVED")
                }
            }

            lastAccelerometerValues[0] = x
            lastAccelerometerValues[1] = y
            lastAccelerometerValues[2] = z

        } catch (e: Exception) {
            // Sensor error
        }
    }

    private fun startMovementCooldown() {
        isInCooldown = true
        Thread {
            try {
                Thread.sleep(movementCooldownMs)
                isInCooldown = false
            } catch (e: Exception) {
                // Cooldown error
            }
        }.start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createDetectionNotification(): android.app.Notification {
        return try {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Anti-Theft Active")
                .setContentText("Device protection enabled")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        } catch (e: Exception) {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Anti-Theft Active")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
            stopProtection()
        } catch (e: Exception) {
            // Destroy error
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (isProtectionActive) {
            try {
                val restartIntent = Intent(this, DetectionService::class.java).apply {
                    action = ACTION_START_PROTECTION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent)
                } else {
                    startService(restartIntent)
                }
            } catch (e: Exception) {
                // Restart error
            }
        }
    }
}