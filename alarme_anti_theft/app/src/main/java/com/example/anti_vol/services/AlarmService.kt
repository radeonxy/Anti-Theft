package com.example.anti_vol.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.*
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.anti_vol.MainActivity
import com.example.anti_vol.R
import com.example.anti_vol.data.PreferencesManager

class AlarmService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "anti_theft_alarm"

        const val ACTION_STOP_ALARM = "STOP_ALARM"
        const val ACTION_VALIDATE_PIN = "VALIDATE_PIN"
        const val EXTRA_PIN_CODE = "pin_code"

        @Volatile
        var isAlarmActive = false
            private set
    }

    private lateinit var preferencesManager: PreferencesManager
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var originalVolume = 0
    private var retryCount = 0
    private val maxRetries = 3

    private val alarmSounds = listOf(
        R.raw.police_warning,
        R.raw.police_alarme,
        R.raw.clock,
        R.raw.fbi,
        R.raw.scream,
        R.raw.hacker
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AlarmService created - SIMPLE STABLE VERSION")

        try {
            preferencesManager = PreferencesManager.getInstance(this)
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            setupVibrator()
            setupWakeLock()
            createNotificationChannel()
            Log.d(TAG, "AlarmService setup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
        }
    }

    private fun setupVibrator() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            Log.d(TAG, "Vibrator setup: ${vibrator != null}")
        } catch (e: Exception) {
            Log.e(TAG, "Vibrator setup failed", e)
        }
    }

    private fun setupWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "AntiTheft::AlarmWakeLock"
            )
            Log.d(TAG, "WakeLock setup: ${wakeLock != null}")
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock setup failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        try {
            when (intent?.action) {
                ACTION_STOP_ALARM -> {
                    Log.d(TAG, "Stop alarm action received")
                    stopAlarm()
                    return START_NOT_STICKY
                }
                ACTION_VALIDATE_PIN -> {
                    val enteredPin = intent.getStringExtra(EXTRA_PIN_CODE)
                    handlePinValidation(enteredPin)
                    return START_STICKY
                }
                else -> {
                    val theftType = intent?.getStringExtra("theft_type") ?: "UNKNOWN"
                    Log.d(TAG, "Starting alarm for theft type: $theftType")
                    startSimpleAlarm(theftType)
                    return START_STICKY
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            return START_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Anti-Theft Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical security alarm"
                    enableLights(true)
                    enableVibration(true)
                    setBypassDnd(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel", e)
        }
    }

    private fun startSimpleAlarm(theftType: String) {
        try {
            if (isAlarmActive) {
                Log.w(TAG, "Alarm already active")
                return
            }

            isAlarmActive = true
            retryCount = 0
            Log.w(TAG, "STARTING SIMPLE ALARM - THEFT DETECTED: $theftType")

            // Start foreground service FIRST
            try {
                startForeground(NOTIFICATION_ID, createAlarmNotification(theftType))
                Log.d(TAG, "Foreground service started")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting foreground service", e)
            }

            // Acquire wake lock
            try {
                wakeLock?.acquire(30 * 60 * 1000L)
                Log.d(TAG, "Wake lock acquired")
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring wake lock", e)
            }

            // Start audio
            try {
                startSimpleAudio()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting audio", e)
            }

            // Start vibration
            try {
                startSimpleVibration()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting vibration", e)
            }

            // Launch PIN entry - simple single attempt
            try {
                launchPinEntryActivity()
            } catch (e: Exception) {
                Log.e(TAG, "Error launching PIN entry", e)
            }

            Log.d(TAG, "Simple alarm started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: startSimpleAlarm failed", e)
            // Minimal fallback
            try {
                isAlarmActive = true
                startForeground(NOTIFICATION_ID, createBasicNotification())
            } catch (e2: Exception) {
                Log.e(TAG, "Even fallback failed", e2)
            }
        }
    }

    private fun startSimpleAudio() {
        Thread {
            try {
                Log.d(TAG, "Starting simple audio")

                // Stop any existing sound
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) mp.stop()
                        mp.release()
                    }
                    mediaPlayer = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping existing sound", e)
                }

                // Set max volume
                try {
                    audioManager?.let { am ->
                        originalVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                        am.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
                        Log.d(TAG, "Volume set to max: $maxVolume")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting volume", e)
                }

                // Create and start MediaPlayer
                try {
                    val selectedIndex = preferencesManager.getSelectedAlarm()
                    val soundRes = if (selectedIndex < alarmSounds.size) {
                        alarmSounds[selectedIndex]
                    } else {
                        alarmSounds[0]
                    }

                    val player = MediaPlayer.create(this@AlarmService, soundRes)
                    if (player != null) {
                        player.isLooping = true
                        player.setVolume(1.0f, 1.0f)
                        player.start()
                        mediaPlayer = player
                        Log.d(TAG, "MediaPlayer started successfully")
                    } else {
                        Log.e(TAG, "Failed to create MediaPlayer")
                        startFallbackSound()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error with MediaPlayer", e)
                    startFallbackSound()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in startSimpleAudio", e)
            }
        }.start()
    }

    private fun startFallbackSound() {
        try {
            Log.d(TAG, "Starting fallback sound")
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(this, notification)
            ringtone?.play()
            Log.d(TAG, "Fallback ringtone started")
        } catch (e: Exception) {
            Log.e(TAG, "Fallback sound also failed", e)
        }
    }

    private fun startSimpleVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 1000, 500, 1000, 500)
                vibrator?.vibrate(
                    android.os.VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                val pattern = longArrayOf(0, 1000, 500, 1000, 500)
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
            Log.d(TAG, "Vibration started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    private fun launchPinEntryActivity() {
        try {
            Log.d(TAG, "Launching PIN entry activity")

            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("show_pin_entry", true)
                putExtra("alarm_active", true)
                putExtra("force_show", true)
            }

            startActivity(intent)
            Log.d(TAG, "PIN entry activity launched")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching PIN entry", e)
        }
    }

    private fun handlePinValidation(enteredPin: String?) {
        try {
            if (enteredPin == null) {
                Log.w(TAG, "PIN is null")
                return
            }

            val storedPin = preferencesManager.getPinCode()
            val isValid = enteredPin == storedPin

            Log.d(TAG, "PIN validation: valid=$isValid")

            if (isValid) {
                Log.d(TAG, "Correct PIN - stopping alarm")
                stopAlarm()
            } else {
                Log.d(TAG, "Incorrect PIN")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in PIN validation", e)
        }
    }

    fun validatePin(pin: String): Boolean {
        try {
            val isValid = pin == preferencesManager.getPinCode()
            if (isValid && isAlarmActive) {
                stopAlarm()
            }
            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error validating PIN", e)
            return false
        }
    }

    fun stopAlarm() {
        try {
            if (!isAlarmActive) {
                Log.d(TAG, "Alarm not active")
                return
            }

            Log.d(TAG, "STOPPING SIMPLE ALARM")
            isAlarmActive = false

            // Stop sound
            try {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) mp.stop()
                    mp.release()
                }
                mediaPlayer = null
                Log.d(TAG, "Sound stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping sound", e)
            }

            // Stop vibration
            try {
                vibrator?.cancel()
                Log.d(TAG, "Vibration stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping vibration", e)
            }

            // Restore volume
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, originalVolume, 0)
                Log.d(TAG, "Volume restored")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring volume", e)
            }

            // Release wake lock
            try {
                wakeLock?.let { wl ->
                    if (wl.isHeld) {
                        wl.release()
                    }
                }
                Log.d(TAG, "Wake lock released")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing wake lock", e)
            }

            // Stop service
            try {
                stopForeground(true)
                stopSelf()
                Log.d(TAG, "Service stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping service", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in stopAlarm", e)
        }
    }

    private fun createAlarmNotification(theftType: String): android.app.Notification {
        return try {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("show_pin_entry", true)
                putExtra("alarm_active", true)
                putExtra("force_show", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("THEFT DETECTED!")
                .setContentText("Tap to enter PIN and stop alarm")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            createBasicNotification()
        }
    }

    private fun createBasicNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anti-Theft Alarm")
            .setContentText("Alarm Active")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AlarmService destroyed")

        try {
            if (isAlarmActive) {
                Log.w(TAG, "Service destroyed during alarm - attempting simple restart")
                val restartIntent = Intent(this, AlarmService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(restartIntent)
                    } else {
                        startService(restartIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restart service", e)
                }
            }
            stopAlarm()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            if (isAlarmActive) {
                Log.w(TAG, "Task removed during alarm - keeping service alive")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onTaskRemoved", e)
        }
    }
}