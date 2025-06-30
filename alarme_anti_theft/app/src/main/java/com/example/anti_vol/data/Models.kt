package com.example.anti_vol.data.models

// Model pour les paramètres d'alarme
data class AlarmSettings(
    val selectedAlarmIndex: Int = 0,
    val alarmName: String = "",
    val audioResourceId: Int = 0,
    val imageResourceId: Int = 0
)

// Model pour le contact d'urgence
data class EmergencyContact(
    val name: String = "",
    val telegramChatId: String = "",
    val isValid: Boolean = false
) {
    fun validate(): EmergencyContact {
        val nameValid = name.trim().length in 2..20
        val chatIdValid = telegramChatId.isNotEmpty() &&
                telegramChatId.matches(Regex("^-?\\d+$")) &&
                telegramChatId.length >= 5

        return this.copy(isValid = nameValid && chatIdValid)
    }
}

// Model pour les paramètres généraux de l'app
data class AppSettings(
    val pinCode: String = "",
    val isProtectionEnabled: Boolean = false,
    val isSetupCompleted: Boolean = false,
    val emergencyContact: EmergencyContact = EmergencyContact(),
    val alarmSettings: AlarmSettings = AlarmSettings()
)

// Model pour les événements de détection
data class TheftEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: TheftEventType,
    val location: String? = null,
    val photoPath: String? = null
)

// Types d'événements de vol
enum class TheftEventType {
    CHARGER_DISCONNECTED,
    DEVICE_MOVED,
    UNAUTHORIZED_ACCESS,
    ALARM_TRIGGERED,
    ALARM_STOPPED
}