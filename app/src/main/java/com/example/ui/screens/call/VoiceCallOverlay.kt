package com.example.ui.screens.call

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.ProfileFrameComponent
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

@Composable
fun VoiceCallOverlay(
    viewModel: CallViewModel
) {
    val callUiState by viewModel.callUiState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    val statusMessage by viewModel.callStatusMessage.collectAsState()

    if (callUiState == CallUiState.IDLE || activeSession == null) {
        return
    }

    // Format duration MM:SS
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    Dialog(
        onDismissRequest = { /* Cannot dismiss active call by tapping outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F0C20).copy(alpha = 0.96f),
                            Color(0xFF1E1035).copy(alpha = 0.98f),
                            Color(0xFF0A0718)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .testTag("voice_call_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: App Branding & Status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TealPremium,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "End-to-End Encrypted HD Voice",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (callUiState == CallUiState.IN_CALL) formattedDuration else statusMessage,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (callUiState == CallUiState.IN_CALL) TealPremium else GoldPremium
                    )
                }

                // Middle: User Profile with Breathing Glow & VIP Frame
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val displayName = if (callUiState == CallUiState.INCOMING_RINGING) {
                        activeSession!!.callerDisplayName
                    } else {
                        activeSession!!.receiverDisplayName
                    }

                    val publicId = if (callUiState == CallUiState.INCOMING_RINGING) {
                        activeSession!!.callerPublicUserId
                    } else {
                        activeSession!!.receiverPublicUserId
                    }

                    val avatarUrl = if (callUiState == CallUiState.INCOMING_RINGING) {
                        activeSession!!.callerAvatarUrl
                    } else {
                        activeSession!!.receiverAvatarUrl
                    }

                    val frameId = if (callUiState == CallUiState.INCOMING_RINGING) {
                        activeSession!!.callerFrameId
                    } else {
                        "frame_default"
                    }

                    // Pulsing Ring Animation for Ringing and Active Talking
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (callUiState != CallUiState.ENDED) 1.08f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseScale"
                    )

                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .scale(pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient glow backdrop
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            TealPremium.copy(alpha = 0.35f),
                                            GoldPremium.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Avatar with VIP frame
                        ProfileFrameComponent(
                            frameId = frameId,
                            avatarUrl = avatarUrl,
                            size = 130.dp,
                            glowActive = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "ID: $publicId",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldPremium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // Bottom Controls: Call action buttons
                when (callUiState) {
                    CallUiState.INCOMING_RINGING -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decline Button (Red)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { viewModel.rejectIncomingCall() },
                                    containerColor = Color(0xFFFF2A55),
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .testTag("decline_call_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "Decline",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Decline", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            }

                            // Accept Button (Green)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { viewModel.answerIncomingCall() },
                                    containerColor = Color(0xFF00E676),
                                    contentColor = Color.Black,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .testTag("accept_call_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Accept",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Accept", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    CallUiState.OUTGOING_RINGING, CallUiState.IN_CALL -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Secondary controls row (Mute, Speaker)
                            if (callUiState == CallUiState.IN_CALL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Mute Toggle
                                    CallIconButton(
                                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        label = if (isMuted) "Unmute" else "Mute",
                                        isActive = isMuted,
                                        activeColor = Color(0xFFFF5252),
                                        onClick = { viewModel.toggleMute() }
                                    )

                                    // Speaker Toggle
                                    CallIconButton(
                                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                        label = if (isSpeakerOn) "Speaker" else "Earpiece",
                                        isActive = isSpeakerOn,
                                        activeColor = TealPremium,
                                        onClick = { viewModel.toggleSpeaker() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(28.dp))
                            }

                            // End Call Button (Red circular)
                            FloatingActionButton(
                                onClick = { viewModel.endCall() },
                                containerColor = Color(0xFFFF2A55),
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(68.dp)
                                    .testTag("end_call_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "End Call",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("End Call", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        }
                    }

                    CallUiState.ENDED -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusMessage,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
fun CallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isActive) activeColor else Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.size(56.dp)
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color.Black else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
