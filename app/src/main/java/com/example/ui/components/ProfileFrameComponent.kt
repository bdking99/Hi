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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ProfileFrame

@Composable
fun ProfileAvatarWithFrame(
    avatarUrl: String?,
    frame: ProfileFrame?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isOnline: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    val primaryColor = frame?.glowColorHex?.let { Color(it.toInt()) } ?: Color(0xFFFFD700)
    val secondaryColor = frame?.secondaryColorHex?.let { Color(it.toInt()) } ?: Color(0xFFFFA000)

    // Subtle breathing pulse animation for the frame glow
    val infiniteTransition = rememberInfiniteTransition(label = "FrameGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaPulse"
    )

    val framePadding = if (frame != null) (size.value * 0.12f).dp else 0.dp
    val avatarInnerSize = size - (framePadding * 2)

    Box(
        modifier = modifier
            .size(size)
            .testTag("profile_avatar_with_frame")
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // 1. VIP Frame Decorative Outer Ring & Glow
        if (frame != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = (size.toPx() * 0.05f).coerceIn(4f, 16f)
                val glowBrush = Brush.sweepGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = pulseAlpha),
                        secondaryColor.copy(alpha = pulseAlpha * 0.8f),
                        primaryColor.copy(alpha = pulseAlpha)
                    )
                )
                // Draw outer decorative ring
                drawCircle(
                    brush = glowBrush,
                    radius = (size.toPx() / 2f) - (strokeWidth / 2f),
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Draw 4 corner ornamental notches
                val notchRadius = strokeWidth * 0.8f
                val r = (size.toPx() / 2f) - (strokeWidth / 2f)
                val angles = listOf(0.0, Math.PI / 2, Math.PI, 3 * Math.PI / 2)
                for (angle in angles) {
                    val cx = center.x + (r * Math.cos(angle)).toFloat()
                    val cy = center.y + (r * Math.sin(angle)).toFloat()
                    drawCircle(
                        color = primaryColor,
                        radius = notchRadius,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Crown / Emblem crest on top of the frame
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-4).dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF130826),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = when (frame.id) {
                            "frame_gold_crown" -> "👑"
                            "frame_royal_ruby" -> "💎"
                            "frame_neon_cyber" -> "⚡"
                            "frame_dragon_fire" -> "🔥"
                            "frame_emerald_king" -> "🌿"
                            "frame_celestial_star" -> "⭐"
                            else -> "👑"
                        },
                        fontSize = (size.value * 0.2f).sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 2. User Avatar Image (Sitting inside the frame)
        Box(
            modifier = Modifier
                .size(avatarInnerSize)
                .clip(CircleShape)
                .background(Color(0xFF22153B)),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar Placeholder",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(avatarInnerSize * 0.6f)
                )
            }
        }

        // 3. Online/Offline Presence Dot
        if (isOnline != null) {
            val statusColor = if (isOnline) Color(0xFF00E676) else Color(0xFF757575)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size((size.value * 0.22f).coerceIn(12f, 20f).dp)
                    .border(2.dp, Color(0xFF090314), CircleShape)
                    .background(statusColor, CircleShape)
            )
        }
    }
}

@Composable
fun ProfileFrameComponent(
    frameId: String,
    avatarUrl: String,
    size: Dp = 50.dp,
    glowActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val frame = remember(frameId) {
        when (frameId) {
            "frame_dragon_gold" -> ProfileFrame(id = frameId, name = "Dragon Gold", rarity = "Legendary", glowColorHex = 0xFFFFD700, secondaryColorHex = 0xFFFF4500)
            "frame_cyber_neon" -> ProfileFrame(id = frameId, name = "Cyber Neon", rarity = "Epic", glowColorHex = 0xFF00E5FF, secondaryColorHex = 0xFFE040FB)
            "frame_royal_amethyst" -> ProfileFrame(id = frameId, name = "Amethyst", rarity = "Rare", glowColorHex = 0xFF9C27B0, secondaryColorHex = 0xFFE91E63)
            "frame_ocean_emperor" -> ProfileFrame(id = frameId, name = "Ocean Emperor", rarity = "Epic", glowColorHex = 0xFF00B0FF, secondaryColorHex = 0xFF00E676)
            "frame_phoenix_fire" -> ProfileFrame(id = frameId, name = "Phoenix Fire", rarity = "Legendary", glowColorHex = 0xFFFF5722, secondaryColorHex = 0xFFFFD700)
            else -> ProfileFrame(id = frameId, name = "Default", rarity = "Common", glowColorHex = 0xFFFFD700, secondaryColorHex = 0xFFFFA000)
        }
    }
    ProfileAvatarWithFrame(
        avatarUrl = avatarUrl,
        frame = frame,
        modifier = modifier,
        size = size
    )
}
