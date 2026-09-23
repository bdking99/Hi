package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.VipRoomEntryEvent
import kotlinx.coroutines.delay

@Composable
fun VipRoomEntryBanner(
    event: VipRoomEntryEvent?,
    reduceMotion: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (event == null) return

    val isSvip = event.vipType.equals("SVIP", ignoreCase = true)
    var isVisible by remember(event.eventId) { mutableStateOf(true) }

    LaunchedEffect(event.eventId) {
        isVisible = true
        delay(4000)
        isVisible = false
        delay(400)
        onDismiss()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (reduceMotion) fadeIn(tween(300)) else slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(),
        exit = if (reduceMotion) fadeOut(tween(300)) else slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(400)
        ) + fadeOut(),
        modifier = modifier
    ) {
        val backgroundBrush = if (isSvip) {
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF4A148C).copy(alpha = 0.92f),
                    Color(0xFF006064).copy(alpha = 0.92f),
                    Color(0xFF880E4F).copy(alpha = 0.92f)
                )
            )
        } else {
            Brush.horizontalGradient(
                listOf(
                    Color(0xFF3E2723).copy(alpha = 0.92f),
                    Color(0xFFF57F17).copy(alpha = 0.92f),
                    Color(0xFF212121).copy(alpha = 0.92f)
                )
            )
        }

        val borderBrush = if (isSvip) {
            Brush.horizontalGradient(
                listOf(Color(0xFF00E5FF), Color(0xFFE040FB), Color(0xFFFFD700))
            )
        } else {
            Brush.horizontalGradient(
                listOf(Color(0xFFFFD700), Color(0xFFFF8F00), Color(0xFFFFF9C4))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .shadow(12.dp, RoundedCornerShape(24.dp))
                .background(backgroundBrush, RoundedCornerShape(24.dp))
                .border(1.5.dp, borderBrush, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // User Avatar with frame
            VipAvatarFrameWrapper(
                frameId = event.frameId,
                vipLevel = if (!isSvip) event.vipLevel else 0,
                svipLevel = if (isSvip) event.vipLevel else 0,
                reduceMotion = reduceMotion,
                size = 38.dp
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(event.avatarUrl.ifBlank { "https://picsum.photos/seed/${event.uid}/200" })
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VipBadge(
                        vipType = event.vipType,
                        vipLevel = if (!isSvip) event.vipLevel else 0,
                        svipLevel = if (isSvip) event.vipLevel else 0
                    )
                    Text(
                        text = event.displayName.ifBlank { "VIP Guest" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (isSvip) "⚡ Grand SVIP entered the room in cosmic glory!" else "✨ Honored VIP joined the room!",
                    color = if (isSvip) Color(0xFF80D8FF) else Color(0xFFFFE082),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = if (isSvip) "💎✨" else "👑✨",
                fontSize = 18.sp
            )
        }
    }
}
