package com.example.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateNext: (Boolean) -> Unit
) {
    val hasSession by viewModel.hasSession.collectAsState()

    // Smooth entry animations
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_stars"
    )

    LaunchedEffect(hasSession) {
        if (hasSession != null) {
            delay(1800) // Optimal splash display duration for luxury entry
            onNavigateNext(hasSession!!)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0B18),
                        Color(0xFF1B1433),
                        Color(0xFF0F0B1E)
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing particles & circular rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldPremium.copy(alpha = 0.18f), Color.Transparent),
                    center = center,
                    radius = size.width * 0.7f
                ),
                center = center,
                radius = size.width * 0.7f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing VIP Golden Logo with Microphone & Crown
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .shadow(30.dp, CircleShape, spotColor = GoldPremium)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF352554),
                                Color(0xFF1C132E)
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                GoldPremium,
                                TealPremium,
                                GoldPremium,
                                Color(0xFFFF80DF),
                                GoldPremium
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Great Voice Room VIP Logo",
                    tint = GoldPremium,
                    modifier = Modifier.size(62.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title with Gold Gradient Look
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "👑", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GREAT VOICE ROOM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle Tag
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldPremium.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPremium.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "✨ VIP Live Voice Club v2.0 ✨",
                    color = GoldPremium,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Shimmering Luxury Loading Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(glowAlpha)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = TealPremium,
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "Connecting to Live Audio Stages...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
