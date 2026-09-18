package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Built with Material 3 Card, custom rounded squircle shapes, and dynamic elevation.
 * Renders avatar images, live speaking pulse border animations, mic status icons (active / muted),
 * and supports tap actions to take seat or view profile.
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Speaking ripple animation
    val infiniteTransition = rememberInfiniteTransition(label = "speaking_pulse")
    val pulseElevation by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_elevation"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("speaker_seat_$seatNumber"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied) {
                if (isSpeaking) Color(0xFF231E3D) else Color(0xFF1B162C)
            } else {
                Color.White.copy(alpha = 0.05f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSpeaking) pulseElevation.dp else if (isOccupied) 4.dp else 1.dp
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (isSpeaking) {
                Brush.sweepGradient(listOf(GoldPremium, TealPremium, GoldPremium))
            } else if (isOccupied) {
                Brush.linearGradient(listOf(TealPremium.copy(alpha = 0.4f), Color(0xFF3B2D60)))
            } else {
                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f)))
            },
            width = if (isSpeaking) 2.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Container with Mic status icon
            Box(
                modifier = Modifier
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isOccupied && !avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = userName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(
                                width = if (isSpeaking) 2.5.dp else 1.5.dp,
                                color = if (isSpeaking) GoldPremium else TealPremium.copy(alpha = 0.6f),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(TealPremium.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Empty Seat $seatNumber",
                            tint = TealPremium,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Mic Status Badge (Live speaking, muted, or active)
                if (isOccupied) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(
                                if (isMuted) Color(0xFF1E1E1E) else if (isSpeaking) Color(0xFF00C853) else Color(0xFF0D47A1)
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
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // User name / Empty label
            Text(
                text = if (isOccupied) userName else "Empty",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = if (isOccupied) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isOccupied) Color.White else Color.White.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Seat Label (e.g. Host 👑 or Seat 1..7)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = if (seatNumber == 0) GoldPremium else Color.White.copy(alpha = 0.45f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
