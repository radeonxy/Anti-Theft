package com.example.anti_vol.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.anti_vol.R
import com.example.anti_vol.managers.AntiTheftManager
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun DetectionScreen(navController: NavController) {
    val context = LocalContext.current
    val antiTheftManager = remember { AntiTheftManager.getInstance(context) }

    // Observer l'état depuis le backend
    val isProtectionActive by antiTheftManager.isProtectionEnabled.collectAsState()
    val isSetupComplete by antiTheftManager.setupComplete.collectAsState()
    val isAlarmActive by antiTheftManager.isAlarmActive.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Header avec titre centré et icône settings
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Titre centré
                Text(
                    text = "Detection",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Icône settings à droite
                IconButton(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Settings",
                        tint = AppColors.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sous-titre dynamique
            Text(
                text = if (isProtectionActive) {
                    "Protection is ACTIVE"
                } else {
                    "Turn on anti-theft mode"
                },
                fontSize = 16.sp,
                color = if (isProtectionActive) {
                    Color(0xFF4CAF50)
                } else {
                    AppColors.White.copy(alpha = 0.8f)
                },
                textAlign = TextAlign.Center,
                fontWeight = if (isProtectionActive) FontWeight.Bold else FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Image du voleur
            Image(
                painter = painterResource(id = R.drawable.thief_illustration),
                contentDescription = "Thief illustration",
                modifier = Modifier
                    .size(350.dp)
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Affichage d'alarme active
            if (isAlarmActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF5555)
                    )
                ) {
                    Text(
                        text = "ALARM ACTIVE!\nEnter PIN to stop",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bouton Start/Stop Protection
            Button(
                onClick = {
                    if (isProtectionActive) {
                        antiTheftManager.disableProtection()
                    } else {
                        if (isSetupComplete) {
                            antiTheftManager.enableProtection()
                        } else {
                            navController.navigate("pin_setup")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isProtectionActive) {
                        Color(0xFFC0A3F0)
                    } else {
                        Color(0xFF313036)
                    }
                ),
                shape = RoundedCornerShape(32.dp),
                enabled = !isAlarmActive
            ) {
                Text(
                    text = if (isProtectionActive) {
                        "Stop Protection"
                    } else {
                        "Start Protection"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White
                )
            }

            // Message d'aide
            if (!isSetupComplete) {
                Text(
                    text = "Complete setup first (PIN, Contact, Alarm)",
                    fontSize = 14.sp,
                    color = Color(0xFFFFAA00),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
