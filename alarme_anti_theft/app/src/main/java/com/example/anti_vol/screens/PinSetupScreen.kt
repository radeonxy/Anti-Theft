package com.example.anti_vol.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun PinSetupScreen(navController: NavController,
                   fromSettings: Boolean = false) {

    val context = LocalContext.current
    val antiTheftManager = remember { AntiTheftManager.getInstance(context) }

    var pinCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top section (40%) - for title and dots
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
                    // Title
                    Text(
                        text = "Set 4 digit Pin Code",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Note text with different colors
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row {
                            Text(
                                text = "Note",
                                fontSize = 14.sp,
                                color = AppColors.NoteRed
                            )
                            Text(
                                text = " : This Pin is required to stop alarm when any anti theft",
                                fontSize = 14.sp,
                                color = AppColors.White
                            )
                        }
                        Text(
                            text = "alarm is activated",
                            fontSize = 14.sp,
                            color = AppColors.White,
                            textAlign = TextAlign.Center
                        )
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
                                        color = if (index < pinCode.length) AppColors.White else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
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
                                Color(0x33797583), // rgba(121, 117, 131, 0.2)
                                Color(0x33363567)  // rgba(54, 53, 103, 0.2)
                            )
                        )
                    )
                    .background(Color(0x4D313036)) // rgba(49, 48, 54, 0.3)
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
                            PinButton("1") { if (pinCode.length < 4) pinCode += "1" }
                            PinButton("2") { if (pinCode.length < 4) pinCode += "2" }
                            PinButton("3") { if (pinCode.length < 4) pinCode += "3" }
                        }

                        // Row 2: 4, 5, 6
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            PinButton("4") { if (pinCode.length < 4) pinCode += "4" }
                            PinButton("5") { if (pinCode.length < 4) pinCode += "5" }
                            PinButton("6") { if (pinCode.length < 4) pinCode += "6" }
                        }

                        // Row 3: 7, 8, 9
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            PinButton("7") { if (pinCode.length < 4) pinCode += "7" }
                            PinButton("8") { if (pinCode.length < 4) pinCode += "8" }
                            PinButton("9") { if (pinCode.length < 4) pinCode += "9" }
                        }

                        // Row 4: 0, ⌫ (backspace)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            Spacer(modifier = Modifier.size(80.dp))
                            PinButton("0") { if (pinCode.length < 4) pinCode += "0" }
                            PinButton("⌫") {
                                if (pinCode.isNotEmpty()) {
                                    pinCode = pinCode.dropLast(1)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Handle PIN completion
        LaunchedEffect(pinCode) {
            if (pinCode.length == 4) {
                // 🔥 SAUVEGARDER LE PIN AVEC LE BACKEND
                antiTheftManager.savePinCode(pinCode)

                kotlinx.coroutines.delay(500)
                if (fromSettings) {
                    navController.navigate("settings")
                } else {
                    navController.navigate("permissions")
                }
            }
        }
    }
}

@Composable
fun PinButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.White
        )
    }
}