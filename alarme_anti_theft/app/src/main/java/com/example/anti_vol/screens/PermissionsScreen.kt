package com.example.anti_vol.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current

    // Permission states
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PermissionChecker.PERMISSION_GRANTED
        )
    }

    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PermissionChecker.PERMISSION_GRANTED
        )
    }

    // Permission launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraGranted = isGranted
    }



    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        locationGranted = isGranted
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

            // Title
            Text(
                text = "Required Permissions",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "To protect your phone, we need these permissions",
                fontSize = 16.sp,
                color = AppColors.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Permission items
            PermissionItem(
                title = "Camera",
                isGranted = cameraGranted,
                onClick = {
                    if (!cameraGranted) {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))



            Spacer(modifier = Modifier.height(20.dp))

            PermissionItem(
                title = "Location",
                isGranted = locationGranted,
                onClick = {
                    if (!locationGranted) {
                        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    // Navigate to emergency contact screen
                    navController.navigate("emergency_contact")
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
                    text = "save",
                    color = AppColors.DarkBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Note",
                    fontSize = 14.sp,
                    color = AppColors.NoteRed,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " : Without these permissions, the app will not be able to",
                    fontSize = 14.sp,
                    color = AppColors.White
                )
            }
            Text(
                text = "effectively protect you in the event of theft.",
                fontSize = 14.sp,
                color = AppColors.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(79.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x33797583), // rgba(121, 117, 131, 0.2)
                        Color(0x33363567)  // rgba(54, 53, 103, 0.2)
                    )
                )
            )
            .background(Color(0x4D313036)) // rgba(49, 48, 54, 0.3)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.White
            )

            // Permission status icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF5555),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isGranted) "✓" else "✕",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}