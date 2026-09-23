package com.example.ui.screens.launch

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * LaunchScreen: Displays the Great Voice Room logo with a smooth fade-in and scale animation,
 * glowing VIP equalizer aura, and automatically transitions to the Main Dashboard.
 */
@Composable
fun LaunchScreen(
    onNavigateToMain: () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    
    // Scale and Alpha Animation
    val scaleAnim = remember { Animatable(0.6f) }
    val alphaAnim = remember { Animatable(0f) }
    val glowRotate = rememberInfiniteTransition(label = "glow_rotate")
    val rotationDegrees by glowRotate.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseGlow by glowRotate.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        // Trigger smooth fade-in and scale
        transitionState.targetState = true
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Wait for splash showcase, then transition to Main Dashboard
        delay(2600)
        onNavigateToMain()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D041A),
                        Color(0xFF1B0A35),
                        Color(0xFF090212)
                    )
                )
            )
            .clickable { onNavigateToMain() }
            .testTag("launch_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glowing circles
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(pulseGlow)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            GoldPremium.copy(alpha = 0.15f),
                            Color(0xFF7928CA).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
                .padding(24.dp)
        ) {
            // --- GREAT VOICE ROOM LOGO EMBLEM ---
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .rotate(rotationDegrees)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                GoldPremium,
                                TealPremium,
                                Color(0xFFFF0080),
                                GoldPremium
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF3B1569),
                                    Color(0xFF19062F)
                                )
                            )
                        )
                        .border(1.5.dp, GoldPremium.copy(alpha = 0.5f), CircleShape)
                        .shadow(16.dp, CircleShape, spotColor = GoldPremium),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👑", fontSize = 24.sp)
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Great Voice Room Logo",
                            tint = GoldPremium,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // --- APP TITLE & VIP BRANDING ---
            Text(
                text = "GREAT VOICE ROOM",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = GoldPremium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Live Social Voice & Gaming Club",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // VIP Live Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFD4AF37).copy(alpha = 0.25f),
                                Color(0xFF00E5FF).copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(1.dp, GoldPremium.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIP LIVE STREAMING • HD AUDIO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Smooth luxury loading progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = GoldPremium,
                strokeWidth = 2.5.dp
            )
        }

        // Bottom Powered By Tag & Version
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Powered by Realtime Audio & Cloud Firestore",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Version 1.0.2 (Build 4) • GreatVoiceRoom",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPremium.copy(alpha = 0.85f)
            )
        }
    }
}
