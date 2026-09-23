package com.example.ui.screens.call

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallSession
import com.example.ui.components.ProfileFrameComponent
import com.example.ui.theme.TealPremium
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistorySheet(
    viewModel: CallViewModel,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val history by viewModel.callHistory.collectAsState()
    val timeFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Call History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider()

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhoneMissed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No call history yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(history, key = { it.callId }) { call ->
                        val isOutgoing = call.callerUid == currentUserId
                        val partnerName = if (isOutgoing) call.receiverDisplayName else call.callerDisplayName
                        val partnerAvatar = if (isOutgoing) call.receiverAvatarUrl else call.callerAvatarUrl
                        val partnerFrame = if (isOutgoing) "frame_default" else call.callerFrameId
                        val partnerPublicId = if (isOutgoing) call.receiverPublicUserId else call.callerPublicUserId

                        val durationMin = call.duration / 60
                        val durationSec = call.duration % 60
                        val durationStr = String.format("%02d:%02d", durationMin, durationSec)

                        val isMissed = call.status == "missed" || call.status == "rejected"
                        val statusColor = if (isMissed) Color(0xFFFF5252) else TealPremium

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                ProfileFrameComponent(
                                    frameId = partnerFrame,
                                    avatarUrl = partnerAvatar,
                                    size = 46.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = partnerName.ifBlank { "User ID: $partnerPublicId" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when {
                                                isMissed -> Icons.Default.CallMissed
                                                isOutgoing -> Icons.Default.CallMade
                                                else -> Icons.Default.CallReceived
                                            },
                                            contentDescription = null,
                                            tint = statusColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isMissed) "Missed" else durationStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("•", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = timeFormat.format(Date(call.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                            modifier = Modifier.padding(start = 74.dp)
                        )
                    }
                }
            }
        }
    }
}
