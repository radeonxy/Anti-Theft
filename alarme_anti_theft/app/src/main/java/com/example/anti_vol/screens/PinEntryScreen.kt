package com.example.anti_vol.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.anti_vol.managers.AntiTheftManager
import com.example.anti_vol.services.AlarmService
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun PinEntryScreen(navController: NavController) {
    val context = LocalContext.current
    val antiTheftManager = remember { AntiTheftManager.getInstance(context) }

    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    var isValidating by remember { mutableStateOf(false) }
    var isTemporaryBlocked by remember { mutableStateOf(false) }
    var blockTimeRemaining by remember { mutableStateOf(0) }

    // Nouvelle variable pour forcer la validation
    var triggerValidation by remember { mutableStateOf(0) }

    val isAlarmActive by antiTheftManager.isAlarmActive.collectAsState()

    // Navigation quand l'alarme s'arrête
    LaunchedEffect(isAlarmActive) {
        if (!isAlarmActive) {
            android.util.Log.d("PinEntryScreen", "Alarm stopped - navigating to detection")
            navController.navigate("detection") {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Timer pour le blocage temporaire
    LaunchedEffect(isTemporaryBlocked) {
        if (isTemporaryBlocked && blockTimeRemaining > 0) {
            while (blockTimeRemaining > 0) {
                kotlinx.coroutines.delay(1000)
                blockTimeRemaining--
            }
            isTemporaryBlocked = false
            showError = false
        }
    }

    // Validation du PIN -
    LaunchedEffect(triggerValidation) {
        if (triggerValidation > 0 && enteredPin.length == 4 && !isValidating && !isTemporaryBlocked) {
            isValidating = true
            showError = false

            android.util.Log.d("PinEntryScreen", "Validating PIN attempt ${attempts + 1}: $enteredPin")

            // Délai visuel court
            kotlinx.coroutines.delay(300)

            // Validation
            val isValid = antiTheftManager.validatePinCode(enteredPin)

            android.util.Log.d("PinEntryScreen", "PIN result: $isValid")

            if (isValid) {
                android.util.Log.d("PinEntryScreen", "Correct PIN - alarm will stop")
                // La navigation sera gérée par le LaunchedEffect de isAlarmActive
            } else {
                android.util.Log.d("PinEntryScreen", "Incorrect PIN - attempt ${attempts + 1}")

                attempts++
                showError = true

                // Gestion des tentatives
                when {
                    attempts >= 5 -> {
                        android.util.Log.w("PinEntryScreen", "5+ attempts - blocking for 30 seconds")
                        isTemporaryBlocked = true
                        blockTimeRemaining = 30
                    }
                    attempts >= 3 -> {
                        android.util.Log.w("PinEntryScreen", "3+ attempts - blocking for 10 seconds")
                        isTemporaryBlocked = true
                        blockTimeRemaining = 10
                    }
                    else -> {
                        android.util.Log.d("PinEntryScreen", "Attempt $attempts - showing error for 1.5s")
                        kotlinx.coroutines.delay(1500)
                        showError = false
                    }
                }
            }

            // Reset du PIN après validation
            enteredPin = ""
            isValidating = false
        }
    }

    // Fonction pour valider le PIN quand il atteint 4 caractères
    LaunchedEffect(enteredPin) {
        if (enteredPin.length == 4 && !isValidating && !isTemporaryBlocked) {
            // Attendre un court délai pour que l'utilisateur voie le PIN complet
            kotlinx.coroutines.delay(200)
            triggerValidation++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF3333),
                        Color(0xFF990000)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top section (40%) - titre et dots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Titre principal
                    Text(
                        text = if (isAlarmActive) "THEFT DETECTED!" else "Enter PIN",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status message
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when {
                            isTemporaryBlocked -> {
                                Text(
                                    text = "Too many attempts!",
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFDD44),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Wait ${blockTimeRemaining}s",
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFDD44)
                                )
                            }
                            isValidating -> {
                                Text(
                                    text = "Validating...",
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            else -> {
                                Text(
                                    text = "Enter PIN Code to stop alarm",
                                    fontSize = 16.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (attempts > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Failed attempts: $attempts",
                                fontSize = 14.sp,
                                color = Color(0xFFFFDD44),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // PIN dots indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        color = when {
                                            index < enteredPin.length -> Color.White
                                            showError -> Color(0xFFFFDD44)
                                            else -> Color.Gray
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }

                    if (showError && !isTemporaryBlocked) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Incorrect PIN! Try again",
                            fontSize = 16.sp,
                            color = Color(0xFFFFDD44),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Bottom section (60%) - keypad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x33797583),
                                Color(0x33363567)
                            )
                        )
                    )
                    .background(Color(0x4D313036))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Number grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Row 1: 1, 2, 3
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            PinButton("1", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "1"
                            }
                            PinButton("2", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "2"
                            }
                            PinButton("3", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "3"
                            }
                        }

                        // Row 2: 4, 5, 6
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            PinButton("4", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "4"
                            }
                            PinButton("5", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "5"
                            }
                            PinButton("6", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "6"
                            }
                        }

                        // Row 3: 7, 8, 9
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            PinButton("7", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "7"
                            }
                            PinButton("8", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "8"
                            }
                            PinButton("9", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "9"
                            }
                        }

                        // Row 4: 0, backspace
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            Spacer(modifier = Modifier.size(80.dp))
                            PinButton("0", isTemporaryBlocked || isValidating) {
                                if (enteredPin.length < 4) enteredPin += "0"
                            }
                            PinButton("⌫", isTemporaryBlocked || isValidating) {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                    showError = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinButton(
    text: String,
    isDisabled: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable(enabled = !isDisabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDisabled) AppColors.White.copy(alpha = 0.5f) else AppColors.White
        )
    }
}