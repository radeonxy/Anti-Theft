package com.example.anti_vol.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.anti_vol.data.PreferencesManager
import com.example.anti_vol.data.models.AlarmSettings
import com.example.anti_vol.data.models.EmergencyContact
import com.example.anti_vol.services.AlarmService
import com.example.anti_vol.services.DetectionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AntiTheftManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AntiTheftManager"

        @Volatile
        private var INSTANCE: AntiTheftManager? = null

        fun getInstance(context: Context): AntiTheftManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AntiTheftManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val preferencesManager = PreferencesManager.getInstance(context)

    // STATE FLOWS AVEC VALEURS CORRECTES PAR DÉFAUT - TOUJOURS COMMENCER PAR FALSE
    private val _isProtectionEnabled = MutableStateFlow(false)
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()

    // ÉTAT D'ALARME BASÉ SUR LE SERVICE
    private val _isAlarmActive = MutableStateFlow(false)
    val isAlarmActive: StateFlow<Boolean> = _isAlarmActive.asStateFlow()

    private val _setupComplete = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean> = _setupComplete.asStateFlow()

    init {
        Log.d(TAG, "AntiTheftManager initialized")

        // Charger les états APRÈS l'initialisation
        loadInitialStates()

        // VÉRIFICATION PÉRIODIQUE DE L'ÉTAT D'ALARME
        startAlarmStateChecker()
    }

    private fun loadInitialStates() {
        try {
            val actualProtectionState = preferencesManager.isProtectionEnabled()
            val actualSetupState = preferencesManager.isSetupComplete()

            Log.d(TAG, "Loading initial states: protection=$actualProtectionState, setup=$actualSetupState")

            // Si la protection était activée mais qu'il n'y a pas d'alarme active, désactiver
            if (actualProtectionState && !AlarmService.isAlarmActive) {
                Log.d(TAG, "Protection was enabled but no active alarm - disabling")
                preferencesManager.setProtectionEnabled(false)
                _isProtectionEnabled.value = false
            } else {
                _isProtectionEnabled.value = actualProtectionState
            }

            _setupComplete.value = actualSetupState

        } catch (e: Exception) {
            Log.e(TAG, "Error loading initial states", e)

        }
    }

    // VÉRIFICATION PÉRIODIQUE DE L'ÉTAT AMÉLIORÉE
    private fun startAlarmStateChecker() {
        Thread {
            while (true) {
                try {
                    val currentAlarmState = AlarmService.isAlarmActive
                    if (_isAlarmActive.value != currentAlarmState) {
                        _isAlarmActive.value = currentAlarmState
                        Log.d(TAG, "Alarm state updated: $currentAlarmState")

                        // Si l'alarme s'arrête et que la protection était activée, désactiver la protection
                        if (!currentAlarmState && _isProtectionEnabled.value) {
                            Log.d(TAG, "Alarm stopped - disabling protection")
                            preferencesManager.setProtectionEnabled(false)
                            _isProtectionEnabled.value = false
                        }
                    }
                    Thread.sleep(1000) // Vérifier chaque seconde
                } catch (e: Exception) {
                    Log.e(TAG, "Error in alarm state checker", e)
                    Thread.sleep(5000) // Attendre plus longtemps en cas d'erreur
                }
            }
        }.start()
    }

    // PROTECTION CONTROL SIMPLIFIÉ
    fun enableProtection(): Boolean {
        if (!preferencesManager.isSetupComplete()) {
            Log.w(TAG, "Cannot enable protection - setup not complete")
            return false
        }

        Log.d(TAG, "Enabling protection...")

        // Sauvegarder état
        preferencesManager.setProtectionEnabled(true)
        _isProtectionEnabled.value = true

        // DÉMARRER SERVICE DE DÉTECTION
        val intent = Intent(context, DetectionService::class.java).apply {
            action = DetectionService.ACTION_START_PROTECTION
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Detection service started")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting detection service", e)
            // Revenir en arrière en cas d'échec
            preferencesManager.setProtectionEnabled(false)
            _isProtectionEnabled.value = false
            return false
        }
    }

    fun disableProtection() {
        Log.d(TAG, "Disabling protection...")

        // Sauvegarder état
        preferencesManager.setProtectionEnabled(false)
        _isProtectionEnabled.value = false

        // ARRÊTER SERVICE DE DÉTECTION
        val intent = Intent(context, DetectionService::class.java).apply {
            action = DetectionService.ACTION_STOP_PROTECTION
        }

        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping detection service", e)
        }

        // ARRÊTER ALARME SI ACTIVE
        if (_isAlarmActive.value) {
            stopAlarm()
        }
    }

    // PIN MANAGEMENT SIMPLIFIÉ
    fun savePinCode(pinCode: String) {
        Log.d(TAG, "Saving PIN code")
        preferencesManager.savePinCode(pinCode)
        updateSetupStatus()
    }

    fun validatePinCode(enteredPin: String): Boolean {
        val storedPin = preferencesManager.getPinCode()
        val isValid = enteredPin == storedPin

        Log.d(TAG, "PIN validation: valid=$isValid")

        if (isValid) {
            Log.d(TAG, "Correct PIN entered")

            if (_isAlarmActive.value) {
                Log.d(TAG, "Alarm was active - stopping alarm AND disabling protection")


                disableProtection()

                Log.d(TAG, "Protection disabled - user has control back")
            }
        }

        return isValid
    }

    // ARRÊT D'ALARME DIRECT
    private fun stopAlarm() {
        Log.d(TAG, "Stopping alarm via service...")

        val intent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }

        try {
            context.startService(intent)
            Log.d(TAG, "Stop alarm command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping alarm", e)
        }
    }

    // Emergency Contact Management
    fun saveEmergencyContact(contact: EmergencyContact): Boolean {
        val validatedContact = contact.validate()

        if (!validatedContact.isValid) {
            Log.w(TAG, "Invalid emergency contact data")
            return false
        }

        Log.d(TAG, "Saving emergency contact: ${validatedContact.name}")
        preferencesManager.saveEmergencyContact(
            validatedContact.name,
            validatedContact.telegramChatId
        )
        updateSetupStatus()
        return true
    }

    fun getEmergencyContact(): EmergencyContact {
        return EmergencyContact(
            name = preferencesManager.getContactName() ?: "",
            telegramChatId = preferencesManager.getTelegramChatId() ?: ""
        ).validate()
    }


    fun markInitialSetupCompleted() {
        Log.d(TAG, "Marking initial setup as completed")
        preferencesManager.markInitialSetupCompleted()
        updateSetupStatus()
    }

    // Alarm Settings Management
    fun saveAlarmSettings(alarmIndex: Int) {
        Log.d(TAG, "Saving alarm settings: index $alarmIndex")
        preferencesManager.saveSelectedAlarm(alarmIndex)
    }

    fun getAlarmSettings(): AlarmSettings {
        val selectedIndex = preferencesManager.getSelectedAlarm()
        return AlarmSettings(selectedAlarmIndex = selectedIndex)
    }

    // Setup Status Management
    private fun updateSetupStatus() {
        val isComplete = preferencesManager.isSetupComplete()
        _setupComplete.value = isComplete
        Log.d(TAG, "Setup complete status: $isComplete")
    }

    // Utility Methods
    fun isSetupComplete(): Boolean = preferencesManager.isSetupComplete()
    fun isPinCodeSet(): Boolean = preferencesManager.isPinCodeSet()
    fun getCurrentProtectionStatus(): Boolean = preferencesManager.isProtectionEnabled()

    // MÉTHODE POUR FORCER LA MISE À JOUR DE L'ÉTAT
    fun refreshAlarmState() {
        val currentAlarmState = AlarmService.isAlarmActive
        if (_isAlarmActive.value != currentAlarmState) {
            _isAlarmActive.value = currentAlarmState
            Log.d(TAG, "Forced alarm state refresh: ${_isAlarmActive.value}")
        }

        // Également actualiser l'état de protection pour assurer la cohérence
        val currentProtectionState = preferencesManager.isProtectionEnabled()
        if (_isProtectionEnabled.value != currentProtectionState) {
            _isProtectionEnabled.value = currentProtectionState
            Log.d(TAG, "Protection state synchronized: $currentProtectionState")
        }
    }

    // Reset/Clear Data
    fun resetAllSettings() {
        Log.w(TAG, "Resetting all settings...")

        disableProtection()
        preferencesManager.clearAllData()

        _isProtectionEnabled.value = false
        _isAlarmActive.value = false
        _setupComplete.value = false
    }

    // 🆕 NEW: Test Telegram Integration
    fun testTelegramIntegration() {
        Log.d(TAG, "🧪 Testing Telegram integration...")

        val theftDetectionManager = TheftDetectionManager.getInstance(context)
        theftDetectionManager.testTheftResponse()
    }

    // 🆕 NEW: Manual theft simulation (for testing)
    fun simulateTheft(theftType: String = "MANUAL_TEST") {
        Log.d(TAG, "🧪 Simulating theft for testing: $theftType")

        val theftDetectionManager = TheftDetectionManager.getInstance(context)
        theftDetectionManager.handleTheftDetection(theftType)
    }

    // Status Information
    fun getStatusInfo(): Map<String, Any> {
        return mapOf(
            "protectionEnabled" to _isProtectionEnabled.value,
            "alarmActive" to _isAlarmActive.value,
            "setupComplete" to _setupComplete.value,
            "pinCodeSet" to isPinCodeSet(),
            "emergencyContactSet" to (preferencesManager.getContactName() != null),
            "selectedAlarm" to preferencesManager.getSelectedAlarm()
        )
    }

    fun cleanup() {
        Log.d(TAG, "Cleanup called")
        // Plus de broadcast receivers à nettoyer
    }
}