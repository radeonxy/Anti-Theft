package com.example.anti_vol.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.anti_vol.data.models.EmergencyContact
import com.example.anti_vol.managers.AntiTheftManager
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun EmergencyContactScreen(
    navController: NavController,
    fromSettings: Boolean = false) {

    val context = LocalContext.current
    val antiTheftManager = remember { AntiTheftManager.getInstance(context) }

    // Charger les données existantes
    val existingContact = remember { antiTheftManager.getEmergencyContact() }

    var contactName by remember { mutableStateOf(existingContact.name) }
    var chatId by remember { mutableStateOf(existingContact.telegramChatId) }
    var showNameError by remember { mutableStateOf(false) }
    var showChatIdError by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Emergency Contact",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "If a theft is detected, we will immediately send a photo alert via Telegram to this contact",
                fontSize = 16.sp,
                color = AppColors.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Contact name section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Contact Name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.White
                    )
                    Text(
                        text = "${contactName.length}/20",
                        fontSize = 12.sp,
                        color = AppColors.White.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = contactName,
                    onValueChange = { newValue ->
                        if (newValue.length <= 20 && !newValue.contains('\n')) {
                            contactName = newValue
                            showNameError = false
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            color = Color(0xFFB8A9E8),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )

                if (showNameError) {
                    Text(
                        text = "Contact name must be 2-20 characters",
                        fontSize = 12.sp,
                        color = AppColors.NoteRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Telegram Chat ID section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Telegram Chat ID",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                BasicTextField(
                    value = chatId,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.matches(Regex("^-?\\d+$")) && newValue.length <= 15)) {
                            chatId = newValue
                            showChatIdError = false
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            color = Color(0xFFB8A9E8),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                )

                if (showChatIdError) {
                    Text(
                        text = "Please enter a valid Telegram Chat ID",
                        fontSize = 12.sp,
                        color = AppColors.NoteRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Helper text for Chat ID
                Text(
                    text = "To get your Chat ID: Start @userinfobot on Telegram",
                    fontSize = 14.sp,
                    color = AppColors.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sending method - Only Telegram
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Sending Method",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Telegram button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color = Color(0xFF229ED9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Telegram",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Next button
            Button(
                onClick = {
                    val contact = EmergencyContact(
                        name = contactName.trim(),
                        telegramChatId = chatId.trim()
                    )

                    val validatedContact = contact.validate()
                    showNameError = contactName.trim().length < 2 || contactName.length > 20
                    showChatIdError = !validatedContact.isValid

                    if (validatedContact.isValid) {
                        val saved = antiTheftManager.saveEmergencyContact(validatedContact)

                        if (saved) {
                            if (fromSettings) {
                                navController.navigate("settings")
                            } else {
                                navController.navigate("alarm_selection")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Next",
                    color = AppColors.DarkBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}