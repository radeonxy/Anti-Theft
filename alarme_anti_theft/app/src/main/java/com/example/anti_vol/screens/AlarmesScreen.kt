package com.example.anti_vol.screens

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class AlarmType(
    val name: String,
    val audioRes: Int,
    val imageRes: Int
)

@Composable
fun AlarmesScreen(navController: NavController,
                  fromSettings: Boolean = false
) {
    val context = LocalContext.current
    val antiTheftManager = remember { AntiTheftManager.getInstance(context) }

    // Charger l'alarme actuellement sélectionnée
    val currentAlarmSettings = remember { antiTheftManager.getAlarmSettings() }
    var selectedAlarm by remember { mutableStateOf<Int?>(currentAlarmSettings.selectedAlarmIndex) }
    var currentMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val alarmTypes = remember {
        listOf(
            AlarmType("Police", R.raw.police_warning, R.drawable.ic_police),
            AlarmType("Alarme", R.raw.police_alarme, R.drawable.ic_police_alarme),
            AlarmType("Clock", R.raw.clock, R.drawable.ic_clock),
            AlarmType("FBI Agent", R.raw.fbi, R.drawable.ic_fbi),
            AlarmType("Scream", R.raw.scream, R.drawable.ic_scream),
            AlarmType("Hacker", R.raw.hacker, R.drawable.ic_hacker)
        )
    }

    fun playAlarmSound(audioRes: Int) {
        currentMediaPlayer?.stop()
        currentMediaPlayer?.release()

        currentMediaPlayer = MediaPlayer.create(context, audioRes).apply {
            setOnCompletionListener {
                release()
                currentMediaPlayer = null
            }
            start()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentMediaPlayer?.release()
        }
    }

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

            // Titre
            Text(
                text = "Choose the Alarme you like",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                textAlign = TextAlign.Center
            )

            // Afficher l'alarme actuellement sélectionnée
            if (selectedAlarm != null && fromSettings) {
                Text(
                    text = "Currently: ${alarmTypes[selectedAlarm!!].name}",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AlarmButton(
                        alarmType = alarmTypes[0],
                        isSelected = selectedAlarm == 0,
                        onClick = {
                            selectedAlarm = 0
                            playAlarmSound(alarmTypes[0].audioRes)
                        }
                    )
                    AlarmButton(
                        alarmType = alarmTypes[1],
                        isSelected = selectedAlarm == 1,
                        onClick = {
                            selectedAlarm = 1
                            playAlarmSound(alarmTypes[1].audioRes)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AlarmButton(
                        alarmType = alarmTypes[2],
                        isSelected = selectedAlarm == 2,
                        onClick = {
                            selectedAlarm = 2
                            playAlarmSound(alarmTypes[2].audioRes)
                        }
                    )
                    AlarmButton(
                        alarmType = alarmTypes[3],
                        isSelected = selectedAlarm == 3,
                        onClick = {
                            selectedAlarm = 3
                            playAlarmSound(alarmTypes[3].audioRes)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AlarmButton(
                        alarmType = alarmTypes[4],
                        isSelected = selectedAlarm == 4,
                        onClick = {
                            selectedAlarm = 4
                            playAlarmSound(alarmTypes[4].audioRes)
                        }
                    )
                    AlarmButton(
                        alarmType = alarmTypes[5],
                        isSelected = selectedAlarm == 5,
                        onClick = {
                            selectedAlarm = 5
                            playAlarmSound(alarmTypes[5].audioRes)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Arrêter le son en cours
                    currentMediaPlayer?.stop()
                    currentMediaPlayer?.release()

                    selectedAlarm?.let { index ->
                        antiTheftManager.saveAlarmSettings(index)
                    }

                    // Navigation selon le contexte
                    if (fromSettings) {
                        navController.navigate("settings")
                    } else {
                        
                        antiTheftManager.markInitialSetupCompleted()

                        navController.navigate("detection")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.White
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedAlarm != null
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

@Composable
fun AlarmButton(
    alarmType: AlarmType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            Color(0xFFB19DDB)
                        } else {
                            Color(0xFFD4C2F1)
                        }
                    )
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = alarmType.imageRes),
                    contentDescription = alarmType.name,
                    modifier = Modifier.size(90.dp),
                    contentScale = ContentScale.Fit
                )
            }

            if (isHovered) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "►",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = alarmType.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.White,
            textAlign = TextAlign.Center
        )
    }
}