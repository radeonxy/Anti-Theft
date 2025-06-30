package com.example.anti_vol.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREFS_NAME = "anti_theft_prefs"

        // Keys
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_CONTACT_NAME = "contact_name"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_SELECTED_ALARM = "selected_alarm"
        private const val KEY_PROTECTION_ENABLED = "protection_enabled"
        private const val KEY_FIRST_TIME_SETUP = "first_time_setup"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // PIN Code
    fun savePinCode(pinCode: String) {
        Log.d(TAG, "Saving PIN code")
        prefs.edit().putString(KEY_PIN_CODE, pinCode).apply()
    }

    fun getPinCode(): String? {
        return prefs.getString(KEY_PIN_CODE, null)
    }

    fun isPinCodeSet(): Boolean {
        val pinSet = !getPinCode().isNullOrEmpty()
        Log.d(TAG, "PIN code set: $pinSet")
        return pinSet
    }

    // Emergency Contact
    fun saveEmergencyContact(name: String, chatId: String) {
        Log.d(TAG, "Saving emergency contact: $name")
        prefs.edit()
            .putString(KEY_CONTACT_NAME, name)
            .putString(KEY_TELEGRAM_CHAT_ID, chatId)
            .apply()
    }

    fun getContactName(): String? {
        return prefs.getString(KEY_CONTACT_NAME, null)
    }

    fun getTelegramChatId(): String? {
        return prefs.getString(KEY_TELEGRAM_CHAT_ID, null)
    }

    // Alarm Settings
    fun saveSelectedAlarm(alarmIndex: Int) {
        Log.d(TAG, "Saving selected alarm: $alarmIndex")
        prefs.edit().putInt(KEY_SELECTED_ALARM, alarmIndex).apply()
    }

    fun getSelectedAlarm(): Int {
        return prefs.getInt(KEY_SELECTED_ALARM, 0) // Default to first alarm
    }

    // Protection Status - TOUJOURS par défaut false
    fun setProtectionEnabled(enabled: Boolean) {
        Log.d(TAG, "Setting protection enabled: $enabled")
        prefs.edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }

    fun isProtectionEnabled(): Boolean {
        val enabled = prefs.getBoolean(KEY_PROTECTION_ENABLED, false) // TOUJOURS par défaut false
        Log.d(TAG, "Protection enabled: $enabled")
        return enabled
    }

    // First Time Setup
    fun setFirstTimeSetupCompleted() {
        Log.d(TAG, "Setting first time setup completed")
        prefs.edit().putBoolean(KEY_FIRST_TIME_SETUP, true).apply()
    }

    fun isFirstTimeSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_TIME_SETUP, false)
    }

    // Check if all setup is complete
    fun isSetupComplete(): Boolean {
        val pinSet = isPinCodeSet()
        val contactSet = !getContactName().isNullOrEmpty()
        val telegramSet = !getTelegramChatId().isNullOrEmpty()
        val complete = pinSet && contactSet && telegramSet

        Log.d(TAG, "Setup complete check: pin=$pinSet, contact=$contactSet, telegram=$telegramSet, complete=$complete")
        return complete
    }

    fun isInitialSetupCompleted(): Boolean {
        val firstTimeCompleted = isFirstTimeSetupCompleted()
        val setupCompleted = isSetupComplete()
        val result = firstTimeCompleted && setupCompleted

        Log.d(TAG, "Initial setup completed: firstTime=$firstTimeCompleted, setup=$setupCompleted, result=$result")
        return result
    }

    // Méthode à appeler quand l'utilisateur termine le flow pour la première fois
    fun markInitialSetupCompleted() {
        Log.d(TAG, "Marking initial setup as completed")
        setFirstTimeSetupCompleted()
    }

    // Clear all data (for reset) et s'assurer que la protection est désactivée
    fun clearAllData() {
        Log.d(TAG, "Clearing all data")
        prefs.edit().clear().apply()
        // Explicitement mettre la protection à false après le clearing
        setProtectionEnabled(false)
    }

    //  méthode pour reset seulement l'état de protection (utile pour le debug)
    fun resetProtectionState() {
        Log.d(TAG, "Resetting protection state to false")
        setProtectionEnabled(false)
    }

    // Méthodes pour le flag shutdown (déjà mentionnées plus tôt)
    fun setAlarmWasActiveBeforeShutdown(wasActive: Boolean) {
        prefs.edit().putBoolean("alarm_active_before_shutdown", wasActive).apply()
    }

    fun wasAlarmActiveBeforeShutdown(): Boolean {
        return prefs.getBoolean("alarm_active_before_shutdown", false)
    }
}