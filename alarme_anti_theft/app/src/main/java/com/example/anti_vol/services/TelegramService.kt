package com.example.anti_vol.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.anti_vol.data.PreferencesManager
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class TelegramService : Service() {

    companion object {
        private const val BOT_TOKEN = "7850144991:AAEJN6LBJ3AS3_zEKCy3AFTF884exy9qZ8c"
        private const val TELEGRAM_API_URL = "https://api.telegram.org/bot$BOT_TOKEN"

        const val ACTION_SEND_THEFT_ALERT = "SEND_THEFT_ALERT"
        const val EXTRA_PHOTO_PATH = "photo_path"
        const val EXTRA_LOCATION_INFO = "location_info"
        const val EXTRA_THEFT_TYPE = "theft_type"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SEND_THEFT_ALERT -> {
                val photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
                val locationInfo = intent.getStringExtra(EXTRA_LOCATION_INFO)
                val theftType = intent.getStringExtra(EXTRA_THEFT_TYPE) ?: "UNKNOWN"
                sendCompleteAlert(photoPath, locationInfo, theftType)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun sendCompleteAlert(photoPath: String?, locationInfo: String?, theftType: String) {
        val chatId = preferencesManager.getTelegramChatId()

        if (chatId.isNullOrEmpty()) {
            return
        }

        serviceScope.launch {
            try {
                if (!photoPath.isNullOrEmpty()) {
                    val photoFile = File(photoPath)
                    if (photoFile.exists()) {
                        val completeCaption = createCompleteMessage(theftType, locationInfo)
                        sendPhoto(chatId, photoFile, completeCaption)
                    } else {
                        sendTextMessage(chatId, createCompleteMessage(theftType, locationInfo))
                    }
                } else {
                    sendTextMessage(chatId, createCompleteMessage(theftType, locationInfo))
                }
            } catch (e: Exception) {
                // Alert sending failed
            } finally {
                stopSelf()
            }
        }
    }

    private fun createCompleteMessage(theftType: String, locationInfo: String?): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        return buildString {
            appendLine("🚨 **ANTI-THEFT ALERT** 🚨")
            appendLine()
            appendLine("⚠️ **THEFT DETECTED!**")
            appendLine("📅 Time: $timestamp")
            appendLine("🔍 Type: ${getTheftTypeDescription(theftType)}")
            appendLine()

            appendLine("📍 **LOCATION:**")
            if (!locationInfo.isNullOrEmpty()) {
                appendLine(locationInfo)
            } else {
                appendLine("📍 Location: Not available")
                appendLine("⚠️ GPS may be disabled or permission denied")
            }
            appendLine()

            appendLine("🛡️ Your device has been secured!")
            appendLine("🔊 Alarm is now active")
            appendLine()
            appendLine("⚡ Powered by Anti-Theft App")
        }
    }

    private fun getTheftTypeDescription(theftType: String): String {
        return when (theftType) {
            "CHARGER_DISCONNECTED" -> "Charger disconnected"
            "DEVICE_MOVED" -> "Device moved/shaken"
            "UNAUTHORIZED_ACCESS" -> "Unauthorized access attempt"
            "MANUAL_TEST" -> "Manual test (not real theft)"
            "TEST_THEFT" -> "Test alert (not real theft)"
            "CAMERA_TEST" -> "Camera test only"
            else -> "Security breach detected"
        }
    }

    private suspend fun sendTextMessage(chatId: String, message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$TELEGRAM_API_URL/sendMessage")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val postData = buildString {
                    append("chat_id=").append(URLEncoder.encode(chatId, "UTF-8"))
                    append("&text=").append(URLEncoder.encode(message, "UTF-8"))
                    append("&parse_mode=").append(URLEncoder.encode("Markdown", "UTF-8"))
                }

                connection.outputStream.use { os ->
                    os.write(postData.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                val success = responseCode == HttpURLConnection.HTTP_OK

                if (!success) {
                    val errorStream = connection.errorStream
                    errorStream?.bufferedReader()?.readText()
                }

                connection.disconnect()
                success

            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun sendPhoto(chatId: String, photoFile: File, caption: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
                val url = URL("$TELEGRAM_API_URL/sendPhoto")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                connection.outputStream.use { os ->
                    val writer = PrintWriter(OutputStreamWriter(os, "UTF-8"), true)

                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
                    writer.append(chatId).append("\r\n")

                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"caption\"\r\n\r\n")
                    writer.append(caption).append("\r\n")

                    writer.append("--$boundary\r\n")
                    writer.append("Content-Disposition: form-data; name=\"photo\"; filename=\"${photoFile.name}\"\r\n")
                    writer.append("Content-Type: image/jpeg\r\n\r\n")
                    writer.flush()

                    photoFile.inputStream().use { fileInput ->
                        fileInput.copyTo(os)
                    }

                    writer.append("\r\n--$boundary--\r\n")
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val success = responseCode == HttpURLConnection.HTTP_OK

                if (success) {
                    try {
                        photoFile.delete()
                    } catch (e: Exception) {
                        // Photo deletion failed
                    }
                } else {
                    val errorStream = connection.errorStream
                    errorStream?.bufferedReader()?.readText()
                }

                connection.disconnect()
                success

            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}