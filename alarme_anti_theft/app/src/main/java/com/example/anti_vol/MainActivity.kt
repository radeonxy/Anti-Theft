package com.example.anti_vol

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.anti_vol.managers.AntiTheftManager
import com.example.anti_vol.navigation.MainNavigation
import com.example.anti_vol.screens.PinEntryScreen
import com.example.anti_vol.services.AlarmService
import com.example.anti_vol.ui.theme.AntivolTheme
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate called")

        enableEdgeToEdge()

        setContent {
            AntivolTheme {
                MainActivityContent()
            }
        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        setIntent(newIntent)

        Log.d("MainActivity", "onNewIntent called")

        // FORCER LE RAFRAÎCHISSEMENT DE L'ÉTAT
        val antiTheftManager = AntiTheftManager.getInstance(this)
        antiTheftManager.refreshAlarmState()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")

        // RAFRAÎCHIR L'ÉTAT À CHAQUE REPRISE
        val antiTheftManager = AntiTheftManager.getInstance(this)
        antiTheftManager.refreshAlarmState()
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause called")
    }
}

@Composable
fun MainActivityContent() {
    val context = LocalContext.current
    val antiTheftManager = remember { AntiTheftManager.getInstance(context) }
    val navController = rememberNavController()

    // OBSERVER L'ÉTAT D'ALARME GLOBAL
    val isAlarmActive by antiTheftManager.isAlarmActive.collectAsState()

    // RAFRAÎCHISSEMENT PÉRIODIQUE DE L'ÉTAT
    LaunchedEffect(Unit) {
        while (true) {
            antiTheftManager.refreshAlarmState()
            kotlinx.coroutines.delay(2000) // Toutes les 2 secondes
        }
    }

    Log.d("MainActivity", "MainActivityContent: isAlarmActive=$isAlarmActive")

    // LOGIQUE SIMPLIFIÉE - SI ALARME ACTIVE = ÉCRAN PIN
    if (isAlarmActive) {
        Log.d("MainActivity", "Showing PinEntryScreen (alarm active)")
        PinEntryScreen(navController = navController)
    } else {
        Log.d("MainActivity", "Showing MainNavigation (normal mode)")
        MainNavigation()
    }
}