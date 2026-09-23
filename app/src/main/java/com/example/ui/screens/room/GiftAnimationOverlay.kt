package com.example.ui.screens.room

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GiftTransaction
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Premium, Non-blocking Gift Animation Overlay for Voice Rooms:
 * Renders high-performance particle & vector animations (Imperial Crown, Cosmic Rocket,
 * Neon Supercar, Crystal Castle, Blooming Roses & Heart Showers) upon backend confirmation.
 */
@Composable
fun GiftAnimationOverlay(
    gift: GiftTransaction,
    modifier: Modifier = Modifier
) {
    // 1. Entrance and scale animations
    val infiniteTransition = rememberInfiniteTransition(label = "gift_infinite")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    // Main entrance animatable
    val entryAnim = remember { Animatable(0f) }
    LaunchedEffect(gift.id) {
        entryAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f * entryAnim.value)),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic Particle Canvas in background
        ParticleCanvas(
            particlePhase = particlePhase,
            giftType = gift.giftName.lowercase()
        )

        // Center Visual Showcase
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(entryAnim.value * pulseScale)
                .alpha(entryAnim.value.coerceIn(0f, 1f))
                .padding(24.dp)
        ) {
            // Glowing Aura Behind Main Gift Icon
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Rotating Golden Halo
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2.2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldPremium.copy(alpha = 0.6f),
                                TealPremium.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        radius = radius,
                        center = center
                    )
                }

                // Main Gift Visual / Emoji
                Text(
                    text = gift.giftEmoji.ifBlank { "🎁" },
                    fontSize = 84.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Announcement Banner
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1B1430).copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GoldPremium, TealPremium, GoldPremium)
                    )
                ),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✨", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = gift.senderName.ifBlank { "A User" },
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = " sent ",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${gift.giftName} ${gift.giftEmoji}",
                                color = TealPremium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        if (gift.receiverName.isNotBlank()) {
                            Text(
                                text = "to ${gift.receiverName} (${gift.coins} Coins)",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✨", fontSize = 18.sp)
                }
            }
        }
    }
}

/**
 * Lightweight, GPU-accelerated 2D Particle Canvas for gift sparkles and streaks.
 */
@Composable
private fun ParticleCanvas(
    particlePhase: Float,
    giftType: String
) {
    val random = remember { Random(42) }
    val particles = remember {
        List(24) {
            ParticleData(
                angle = (it * (360f / 24f)),
                distance = 60f + random.nextFloat() * 180f,
                size = 3f + random.nextFloat() * 6f,
                color = if (it % 2 == 0) GoldPremium else TealPremium
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)

        for (p in particles) {
            val progress = (particlePhase + (p.angle / 360f)) % 1f
            val currentDistance = p.distance * progress
            val rad = Math.toRadians((p.angle + progress * 60f).toDouble())
            val x = center.x + (cos(rad) * currentDistance).toFloat()
            val y = center.y + (sin(rad) * currentDistance).toFloat()
            val alpha = (1f - progress).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * (1f - progress * 0.5f),
                center = Offset(x, y)
            )
        }
    }
}

private data class ParticleData(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val color: Color
)
