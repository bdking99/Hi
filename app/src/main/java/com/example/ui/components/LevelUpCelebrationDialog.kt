package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LevelUpRewardRecord

@Composable
fun LevelUpCelebrationDialog(
    reward: LevelUpRewardRecord?,
    onDismiss: () -> Unit,
    onEquipFrame: (String) -> Unit
) {
    if (reward == null) return

    val isSvip = reward.levelType.equals("SVIP", ignoreCase = true)
    val isVip = reward.levelType.equals("VIP", ignoreCase = true)

    val infiniteTransition = rememberInfiniteTransition(label = "Rays")
    val rayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RayRot"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E28)
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    2.dp,
                    Brush.verticalGradient(
                        listOf(
                            if (isSvip) Color(0xFF00E5FF) else Color(0xFFFFD700),
                            if (isSvip) Color(0xFFE040FB) else Color(0xFFFF6D00)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .shadow(16.dp, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebration Title
                Text(
                    text = "✨ CONGRATULATIONS! ✨",
                    color = if (isSvip) Color(0xFF80D8FF) else Color(0xFFFFD700),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                // Rotating Halo & Badge Icon
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(rayRotation)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Transparent,
                                        if (isSvip) Color(0xFF00E5FF).copy(alpha = 0.35f) else Color(0xFFFFD700).copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .shadow(8.dp, CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    if (isSvip) listOf(Color(0xFF7B1FA2), Color(0xFF00ACC1))
                                    else listOf(Color(0xFFFFB300), Color(0xFFFF6F00))
                                ),
                                CircleShape
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSvip) "💎" else if (isVip) "👑" else "🌟",
                            fontSize = 36.sp
                        )
                    }
                }

                // Level Name
                Text(
                    text = if (isSvip) "You reached SVIP ${reward.level}" else if (isVip) "You reached VIP ${reward.level}" else "Level ${reward.level} Unlocked!",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your qualifying account activity has unlocked exclusive rewards and prestige status.",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                // Rewards Unlocked List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14141E), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎁 Rewards Granted (Server Verified):",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (!reward.grantedFrameId.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🖼️", fontSize = 14.sp)
                            Text(
                                text = "Exclusive Avatar Frame: ${reward.grantedFrameId}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (reward.grantedCoins > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🪙", fontSize = 14.sp)
                            Text(
                                text = "+${reward.grantedCoins} Level-up Bonus Coins",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!reward.grantedFrameId.isNullOrBlank()) {
                        Button(
                            onClick = {
                                onEquipFrame(reward.grantedFrameId)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSvip) Color(0xFF00ACC1) else Color(0xFFFFB300)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Equip Frame",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Awesome!",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
