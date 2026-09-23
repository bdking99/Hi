package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

/**
 * SpeakerSeat Component:
 * Modern 10-seat speaker layout with avatar border animations,
 * real-time voice ripple effects, mic states, VIP frames, and admin/host role badges.
 */
@Composable
fun SpeakerSeat(
    seatNumber: Int,
    label: String,
    userName: String,
    avatarUrl: String?,
    isOccupied: Boolean,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isLocked: Boolean = false,
    isHost: Boolean = false,
    isCoHost: Boolean = false,
    vipLevel: Int = 0,
    role: String = "USER",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Multi-ring ripple wave animation when speaking
    val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse_$seatNumber")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_scale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_alpha"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("speaker_seat_$seatNumber"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied) {
                if (isSpeaking) Color(0xFF261D42) else Color(0xFF1B162C).copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.04f)
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isSpeaking) {
                Brush.sweepGradient(listOf(GoldPremium, TealPremium, GoldPremium))
            } else if (isOccupied) {
                if (isHost) Brush.linearGradient(listOf(GoldPremium.copy(alpha = 0.6f), Color(0xFF3B2D60)))
                else Brush.linearGradient(listOf(TealPremium.copy(alpha = 0.3f), Color.White.copy(alpha = 0.08f)))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.03f)))
            },
            width = if (isSpeaking) 2.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Container with ripple animations and mic badge
            Box(
                modifier = Modifier
                    .size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                // Speaking Soundwave Ripple
                if (isOccupied && isSpeaking && !isMuted) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val baseRadius = (size.minDimension / 2f)
                        drawCircle(
                            color = TealPremium.copy(alpha = waveAlpha),
                            radius = baseRadius * waveScale,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = GoldPremium.copy(alpha = (waveAlpha * 0.7f)),
                            radius = baseRadius * (1f + (waveScale - 1f) * 0.5f),
                            center = center,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                if (isOccupied && !avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = userName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(
                                width = if (isSpeaking) 2.5.dp else if (isHost) 2.dp else 1.5.dp,
                                color = if (isSpeaking) GoldPremium else if (isHost) GoldPremium.copy(alpha = 0.8f) else TealPremium.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else if (isLocked) {
                    // Locked seat icon
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF261828))
                            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Seat Locked",
                            tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    // Empty seat
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(TealPremium.copy(alpha = 0.2f), Color.Transparent)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Take Seat $seatNumber",
                            tint = TealPremium,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Role badge on top (Host 👑 / Co-Host ⭐)
                if (isOccupied && (isHost || isCoHost)) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-6).dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isHost) GoldPremium else Color(0xFF673AB7)
                    ) {
                        Text(
                            text = if (isHost) "👑 Host" else "⭐ Co-Host",
                            color = if (isHost) Color.Black else Color.White,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Mic Status Badge (Bottom End)
                if (isOccupied) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(
                                if (isMuted) Color(0xFF1F1F1F) else if (isSpeaking) Color(0xFF00C853) else Color(0xFF0D47A1)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isMuted) Color(0xFFFF5252) else Color.White,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Mic Muted" else "Mic Active",
                            tint = if (isMuted) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // User name / Empty label
            Text(
                text = if (isOccupied) userName else if (isLocked) "Locked" else "Seat $seatNumber",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                fontWeight = if (isOccupied) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isOccupied) Color.White else Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // VIP / Seat Label
            if (isOccupied && vipLevel > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = GoldPremium.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldPremium.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "VIP $vipLevel",
                        color = GoldPremium,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    color = if (seatNumber == 1 || isHost) GoldPremium else Color.White.copy(alpha = 0.4f),
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
