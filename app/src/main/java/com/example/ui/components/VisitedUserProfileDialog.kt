package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.ProfileFrame
import com.example.data.model.UserReport
import com.example.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitedUserProfileDialog(
    user: FirebaseUserProfile,
    currentUserId: String,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onSendMessage: (FirebaseUserProfile) -> Unit = {},
    onSendGift: (FirebaseUserProfile) -> Unit = {},
    onVoiceCall: (FirebaseUserProfile) -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time follow status
    val isFollowing by userRepository.isFollowingStream(currentUserId, user.userId).collectAsState(initial = false)
    
    // Frames list to lookup frame decoration
    val availableFrames by userRepository.getAvailableFramesStream().collectAsState(initial = userRepository.getDefaultFrames())
    val userFrame = remember(availableFrames, user.profileFrameId) {
        availableFrames.find { it.id == user.profileFrameId } ?: availableFrames.firstOrNull()
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportDetails by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("visited_user_profile_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F061F),
            tonalElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E1952))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp)
                ) {
                    // 1. Cover Banner & Close Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2C0B57), Color(0xFF13042A), Color(0xFF090214))
                                )
                            )
                    ) {
                        // Close button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }

                        // Report / Block More button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            IconButton(
                                onClick = { showOptionsMenu = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false },
                                modifier = Modifier.background(Color(0xFF1C1533))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Report User", color = Color(0xFFFF5252)) },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = Color(0xFFFF5252)) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showReportDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Block User", color = Color(0xFFFF5252)) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF5252)) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showBlockConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // 2. Avatar Inside Frame & Presence
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .offset(y = (-45).dp)
                    ) {
                        ProfileAvatarWithFrame(
                            avatarUrl = user.avatar,
                            frame = userFrame,
                            size = 100.dp,
                            isOnline = user.isOnline
                        )
                    }

                    // 3. User Identity & Permanent Public User ID
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-30).dp)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            if (user.vipLevel > 0) {
                                VipBadge(
                                    vipType = if (user.vipLevel >= 8) "SVIP" else "VIP",
                                    vipLevel = user.vipLevel,
                                    svipLevel = if (user.vipLevel >= 8) user.vipLevel - 7 else 0
                                )
                            }
                            UserLevelBadge(level = user.level.coerceAtLeast(1))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Permanent Numeric Public User ID Badge with Copy Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E1038),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B2068)),
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(user.publicUserId))
                                Toast.makeText(context, "ID ${user.publicUserId} copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ID: ${user.publicUserId}",
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy ID",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Online Status with Formatted Time
                        val statusText = if (user.isOnline) {
                            "🟢 Online Now"
                        } else {
                            val elapsedMins = ((System.currentTimeMillis() - user.lastActive) / (1000 * 60)).coerceAtLeast(1)
                            val timeStr = when {
                                elapsedMins < 60 -> "${elapsedMins}m ago"
                                elapsedMins < 1440 -> "${elapsedMins / 60}h ago"
                                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(user.lastActive))
                            }
                            "⚪ Offline • Last seen $timeStr"
                        }
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = if (user.isOnline) Color(0xFF00E676) else Color.LightGray.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Bio Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF180A2E),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B164C))
                        ) {
                            Text(
                                text = if (user.bio.isNotBlank()) user.bio else "This VIP has not written a bio yet.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(14.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Row (Diamonds, Level, Followers)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(title = "Level", value = "Lv.${user.level}", color = Color(0xFF00E5FF))
                            StatItem(title = "Diamonds", value = "${user.receivedDiamonds}", color = Color(0xFFFF4081))
                            StatItem(title = "Followers", value = "${user.followersCount}", color = Color(0xFFFFD700))
                            StatItem(title = "Country", value = user.country, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons: Follow, Gift, Message, Voice Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Follow / Unfollow
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        userRepository.toggleFollow(currentUserId, user.userId, !isFollowing)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) Color(0xFF2C1947) else Color(0xFFFFD700),
                                    contentColor = if (isFollowing) Color.White else Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFollowing) "Following" else "Follow",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            // Send Gift
                            Button(
                                onClick = {
                                    onDismiss()
                                    onSendGift(user)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF4081),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gift", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Message
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onSendMessage(user)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A2A7D)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message", fontSize = 13.sp)
                            }

                            // Voice Call
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onVoiceCall(user)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Report User Sheet / Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report User", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text(
                        text = "Report ${user.displayName} (ID: ${user.publicUserId}) for violating community guidelines.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf("Harassment / Abuse", "Inappropriate Avatar / Bio", "Spam / Fraud", "Impersonation", "Other").forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = reason }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = reportReason == reason,
                                onClick = { reportReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFD700))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            isSubmittingReport = true
                            coroutineScope.launch {
                                userRepository.reportUser(
                                    UserReport(
                                        reporterUid = currentUserId,
                                        reportedUid = user.userId,
                                        reportedPublicId = user.publicUserId,
                                        reason = reportReason,
                                        details = reportDetails
                                    )
                                )
                                isSubmittingReport = false
                                showReportDialog = false
                                Toast.makeText(context, "Report submitted. Our safety team will review.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    enabled = reportReason.isNotBlank() && !isSubmittingReport
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1038)
        )
    }

    // Block User Confirmation Dialog
    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text("Block ${user.displayName}?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "Blocked users cannot send you messages, start voice calls, send gifts, or see your activity in rooms. Any existing follow relationships will also be removed.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmDialog = false
                        coroutineScope.launch {
                            userRepository.blockUser(currentUserId, user.userId)
                            Toast.makeText(context, "${user.displayName} has been blocked.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1038)
        )
    }
}

@Composable
private fun StatItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, fontSize = 11.sp, color = Color.LightGray.copy(alpha = 0.7f))
    }
}
