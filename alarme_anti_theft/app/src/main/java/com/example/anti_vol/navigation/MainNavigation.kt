package com.example.anti_vol.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.anti_vol.data.PreferencesManager
import com.example.anti_vol.screens.*
import com.example.anti_vol.screens.onboarding.IntroductionScreen1
import com.example.anti_vol.screens.onboarding.IntroductionScreen2
import com.example.anti_vol.screens.onboarding.IntroductionScreen3

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager.getInstance(context) }

    // Déterminer la destination initiale
    val startDestination = remember {
        if (preferencesManager.isInitialSetupCompleted()) {
            "detection" // Si setup initial complet → Directement à la détection
        } else {
            "home" // Si pas de setup initial → Flux normal depuis le début
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Écran d'accueil (seulement pour première utilisation)
        composable("home") {
            HomeScreen(navController)
        }

        // Écrans d'introduction (seulement pour première utilisation)
        composable("intro1") {
            IntroductionScreen1(navController)
        }
        composable("intro2") {
            IntroductionScreen2(navController)
        }
        composable("intro3") {
            IntroductionScreen3(navController)
        }

        // Configuration PIN - Version simple (flow normal)
        composable("pin_setup") {
            PinSetupScreen(navController, fromSettings = false)
        }

        // Configuration PIN - Version avec paramètre fromSettings
        composable(
            "pin_setup?fromSettings={fromSettings}",
            arguments = listOf(navArgument("fromSettings") { defaultValue = "false" })
        ) { backStackEntry ->
            val fromSettings = backStackEntry.arguments?.getString("fromSettings") == "true"
            PinSetupScreen(navController, fromSettings)
        }

        // Écran de saisie PIN pour arrêter l'alarme
        composable("pin_entry") {
            PinEntryScreen(navController)
        }

        // Écran des permissions
        composable("permissions") {
            PermissionsScreen(navController)
        }

        // Contact d'urgence - Version simple (flow normal)
        composable("emergency_contact") {
            EmergencyContactScreen(navController, fromSettings = false)
        }

        // Contact d'urgence - Version avec paramètre fromSettings
        composable(
            "emergency_contact?fromSettings={fromSettings}",
            arguments = listOf(navArgument("fromSettings") { defaultValue = "false" })
        ) { backStackEntry ->
            val fromSettings = backStackEntry.arguments?.getString("fromSettings") == "true"
            EmergencyContactScreen(navController, fromSettings)
        }

        // Sélection d'alarme - Version simple (flow normal)
        composable("alarm_selection") {
            AlarmesScreen(navController, fromSettings = false)
        }

        // Sélection d'alarme - Version avec paramètre fromSettings
        composable(
            "alarm_selection?fromSettings={fromSettings}",
            arguments = listOf(navArgument("fromSettings") { defaultValue = "false" })
        ) { backStackEntry ->
            val fromSettings = backStackEntry.arguments?.getString("fromSettings") == "true"
            AlarmesScreen(navController, fromSettings)
        }

        // Écran des paramètres
        composable("settings") {
            SettingsScreen(navController)
        }

        // Écran de détection (destination principale pour utilisateurs existants)
        composable("detection") {
            DetectionScreen(navController)
        }
    }
}