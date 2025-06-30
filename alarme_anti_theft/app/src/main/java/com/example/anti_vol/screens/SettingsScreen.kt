package com.example.anti_vol.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun SettingsScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Titre
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 60.dp)
            )

            SettingsButton(
                text = "Change Music",
                onClick = {
                    navController.navigate("alarm_selection?fromSettings=true")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsButton(
                text = "Change urgence phone",
                onClick = {
                    navController.navigate("emergency_contact?fromSettings=true")
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingsButton(
                text = "Change code Pin",
                onClick = {
                    navController.navigate("pin_setup?fromSettings=true")
                }
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Bouton retour
            OutlinedButton(
                onClick = {
                    navController.navigate("detection")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = AppColors.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Back",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SettingsButton(
    text: String,
    backgroundColor: Color = Color(0xFF4A4458),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = AppColors.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}