package com.example.anti_vol.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.anti_vol.R
import com.example.anti_vol.ui.theme.AppColors

@Composable
fun IntroductionScreen1(navController: NavController) {
    IntroductionScreenTemplate(
        imageRes = R.drawable.alarme,
        imageWidth = 345,
        imageHeight = 267,
        title = "Loud Alarm is Triggered",
        subtitle = "When charger is disconnected from phone",
        currentPage = 0,
        totalPages = 3,
        buttonText = "Next",
        onButtonClick = { navController.navigate("intro2") },
        showSkip = true, // Show skip on screen 1 (alarme)
        navController = navController
    )
}

@Composable
fun IntroductionScreen2(navController: NavController) {
    IntroductionScreenTemplate(
        imageRes = R.drawable.password,
        imageWidth = 364,
        imageHeight = 293,
        title = "Protect By Password",
        subtitle = "Thief cannot close the app or reduce the alarm volume without knowing you password",
        currentPage = 1,
        totalPages = 3,
        buttonText = "Next",
        onButtonClick = { navController.navigate("intro3") },
        showSkip = true, // Show skip on screen 2 (password)
        navController = navController
    )
}

@Composable
fun IntroductionScreen3(navController: NavController) {
    IntroductionScreenTemplate(
        imageRes = R.drawable.camera,
        imageWidth = 402,
        imageHeight = 228,
        title = "Capture a Intruder Selfie",
        subtitle = "Allows you to easily see who has tried to disconnect your device from charging without your authorization.",
        currentPage = 2,
        totalPages = 3,
        buttonText = "Get started",
        onButtonClick = { navController.navigate("pin_setup") },
        showSkip = false, // NO skip on screen 3 (camera)
        navController = navController
    )
}

@Composable
fun IntroductionScreenTemplate(
    imageRes: Int,
    imageWidth: Int,
    imageHeight: Int,
    title: String,
    subtitle: String,
    currentPage: Int,
    totalPages: Int,
    buttonText: String,
    onButtonClick: () -> Unit,
    showSkip: Boolean,
    navController: NavController
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.DarkBackground)
    ) {
        // Main content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top section (50%) - empty space for image
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Bottom section (50%) - square
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                    .background(AppColors.BottomContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))

                    Text(
                        text = title,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 45.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = subtitle,
                        fontSize = 16.sp,
                        color = AppColors.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        repeat(totalPages) { index ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = if (index == currentPage) AppColors.White
                                        else AppColors.White.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(5.dp)
                                    )
                            )
                            if (index < totalPages - 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    Button(
                        onClick = onButtonClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = buttonText,
                            color = AppColors.DarkBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (buttonText == "Next") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "→",
                                color = AppColors.DarkBackground,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Image(
            painter = painterResource(id = imageRes),
            contentDescription = title,
            modifier = Modifier
                .width(imageWidth.dp)
                .height(imageHeight.dp)
                .align(Alignment.Center)
                .offset(y = (-50).dp),
            contentScale = ContentScale.Fit
        )

        // Skip button (if needed)
        if (showSkip) {
            Text(
                text = "Skip",
                color = AppColors.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 50.dp, end = 28.dp)
                    .clickable { navController.navigate("pin_setup") }
            )
        }
    }
}