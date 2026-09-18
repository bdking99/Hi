package com.example.ui.screens.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

data class RoomParticipant(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val role: String = "Listener", // "Host", "Speaker", "VIP", "Listener"
    val level: Int = 1,
    val vipLevel: Int = 0
)

@Composable
fun ParticipantList(
    participants: List<RoomParticipant>,
    modifier: Modifier = Modifier,
    onUserClick: (RoomParticipant) -> Unit = {},
    onSendGiftClick: (RoomParticipant) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("participant_list_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A2E)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Room Members",
                        tint = TealPremium,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Room Participants",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealPremium.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPremium.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${participants.size} Online",
                        color = TealPremium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable List of Participants
            if (participants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No participants yet in this room",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .testTag("participant_list_scrollable"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participants, key = { it.userId }) { user ->
                        ParticipantRowItem(
                            user = user,
                            onUserClick = { onUserClick(user) },
                            onSendGiftClick = { onSendGiftClick(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantRowItem(
    user: RoomParticipant,
    onUserClick: () -> Unit,
    onSendGiftClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onUserClick)
            .testTag("participant_row_${user.userId}"),
        color = Color.White.copy(alpha = 0.04f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Names + Role
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with speak indicator
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2342))
                        .border(
                            width = if (user.isSpeaking) 2.5.dp else 1.dp,
                            brush = if (user.isSpeaking) {
                                Brush.linearGradient(listOf(TealPremium, GoldPremium))
                            } else {
                                Brush.linearGradient(listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f)))
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = user.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = user.displayName,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    // Speaking glow badge
                    if (user.isSpeaking) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(TealPremium)
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.vipLevel > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldPremium.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "VIP ${user.vipLevel}",
                                    color = GoldPremium,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Role badge
                        val roleColor = when (user.role) {
                            "Host" -> GoldPremium
                            "Speaker" -> TealPremium
                            else -> Color.White.copy(alpha = 0.5f)
                        }
                        Text(
                            text = user.role,
                            color = roleColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•  Lv.${user.level}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Right: Mic / Mute Status Indicator + Quick Gift Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mic Status
                if (user.role == "Host" || user.role == "Speaker") {
                    Surface(
                        shape = CircleShape,
                        color = if (user.isMuted) Color(0xFFE53935).copy(alpha = 0.2f) else TealPremium.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (user.isMuted) Color(0xFFE53935).copy(alpha = 0.5f) else TealPremium.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (user.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (user.isMuted) "Muted" else "Speaking",
                                tint = if (user.isMuted) Color(0xFFFF5252) else TealPremium,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Listening",
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Quick Send Gift Action Button
                IconButton(
                    onClick = onSendGiftClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("🎁", fontSize = 16.sp)
                }
            }
        }
    }
}
