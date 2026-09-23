package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 👑 VIP / SVIP BADGE COMPONENT
@Composable
fun VipBadge(
    vipType: String,
    vipLevel: Int,
    svipLevel: Int = 0,
    modifier: Modifier = Modifier
) {
    if (vipLevel <= 0 && svipLevel <= 0 && vipType.equals("NONE", ignoreCase = true)) return

    val isSvip = vipType.equals("SVIP", ignoreCase = true) || svipLevel > 0
    val displayLevel = if (isSvip) svipLevel.coerceAtLeast(1) else vipLevel.coerceAtLeast(1)

    val backgroundBrush = if (isSvip) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF9C27B0),
                Color(0xFF00E5FF),
                Color(0xFFE040FB)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFB300),
                Color(0xFFFF6F00),
                Color(0xFFFFD54F)
            )
        )
    }

    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(backgroundBrush, RoundedCornerShape(12.dp))
            .border(0.8.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isSvip) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "SVIP",
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "SVIP $displayLevel",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        } else {
            Text(
                text = "👑",
                fontSize = 9.sp
            )
            Text(
                text = "VIP $displayLevel",
                color = Color.Black,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// 🌟 USER NORMAL LEVEL BADGE
@Composable
fun UserLevelBadge(
    level: Int,
    modifier: Modifier = Modifier
) {
    val tierColors = when {
        level >= 100 -> listOf(Color(0xFFFF1744), Color(0xFFFFD700))
        level >= 75 -> listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
        level >= 50 -> listOf(Color(0xFFE040FB), Color(0xFF536DFE))
        level >= 25 -> listOf(Color(0xFFFFD700), Color(0xFFFFA000))
        level >= 10 -> listOf(Color(0xFFB0BEC5), Color(0xFF78909C))
        else -> listOf(Color(0xFFCD7F32), Color(0xFF8D6E63))
    }

    Row(
        modifier = modifier
            .background(Brush.horizontalGradient(tierColors), RoundedCornerShape(10.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Level",
            tint = Color.White,
            modifier = Modifier.size(9.dp)
        )
        Text(
            text = "Lv.$level",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ✨ ANIMATED VIP / SVIP AVATAR FRAME WRAPPER
@Composable
fun VipAvatarFrameWrapper(
    frameId: String?,
    vipLevel: Int = 0,
    svipLevel: Int = 0,
    reduceMotion: Boolean = false,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isSvip = svipLevel > 0 || (frameId?.contains("svip", ignoreCase = true) == true)
    val hasVip = vipLevel > 0 || isSvip || (frameId != null && frameId != "frame_default" && frameId.isNotBlank())

    val infiniteTransition = rememberInfiniteTransition(label = "FrameRotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSvip) 3500 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reduceMotion) 1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = modifier.size(size + 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hasVip) {
            val ringBrush = if (isSvip) {
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00E5FF),
                        Color(0xFFE040FB),
                        Color(0xFFFF1744),
                        Color(0xFFFFD700),
                        Color(0xFF00E5FF)
                    )
                )
            } else if (vipLevel >= 7) {
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF1744),
                        Color(0xFFFFD700),
                        Color(0xFFFF6D00),
                        Color(0xFFFF1744)
                    )
                )
            } else {
                Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFAB00),
                        Color(0xFFFFF9C4),
                        Color(0xFFFFD700)
                    )
                )
            }

            // Outer glowing aura
            Box(
                modifier = Modifier
                    .size(size + 12.dp)
                    .rotate(angle)
                    .border(
                        width = if (isSvip) 3.dp else 2.5.dp,
                        brush = ringBrush,
                        shape = CircleShape
                    )
            )

            // Sparkle top badge
            if (isSvip) {
                Text(
                    text = "💎",
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                )
            } else if (vipLevel >= 3) {
                Text(
                    text = "👑",
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                )
            }
        }

        // Inner Avatar Box
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
