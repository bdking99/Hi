package com.example.ui.screens.room

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.AdminBadge
import com.example.ui.components.SpeakerSeat
import com.example.ui.components.VipBadge
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FullScreenVoiceRoom:
 * True full-screen voice room experience replacing bottom sheet constraints.
 * Provides 10 interactive speaker seats, WebRTC live audio feedback, rich chat,
 * speaker profile action sheet, gift animations, audience roster, and host controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenVoiceRoom(
    viewModel: RoomViewModel,
    roomId: String,
    onLeaveRoom: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateDirectCall: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateDirectChat: (String, String, String) -> Unit = { _, _, _ -> },
    onNavigateProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Collect Room State
    val currentRoom by viewModel.currentRoom.collectAsStateWithLifecycle()
    val roomMembers by viewModel.roomMembers.collectAsStateWithLifecycle()
    val speakerRequests by viewModel.speakerRequests.collectAsStateWithLifecycle()
    val roomChat by viewModel.roomChat.collectAsStateWithLifecycle()
    val latestRoomGift by viewModel.latestRoomGift.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val isMicMuted by viewModel.isMicrophoneMuted.collectAsStateWithLifecycle()
    val isSpeakingLocally by viewModel.isSpeakingLocally.collectAsStateWithLifecycle()
    val speakingPeers by viewModel.speakingPeers.collectAsStateWithLifecycle()
    val uiNotice by viewModel.uiNotice.collectAsStateWithLifecycle()

    // Local UI Dialog States
    var showGiftSheet by remember { mutableStateOf(false) }
    var showSpeakerRequestsDialog by remember { mutableStateOf(false) }
    var showModerationDialog by remember { mutableStateOf(false) }
    var showAudienceDialog by remember { mutableStateOf(false) }
    var showGameDialog by remember { mutableStateOf(false) }
    var showRoomInfoDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmationDialog by remember { mutableStateOf(false) }

    // Selected User for Profile Action Sheet or Direct Gift
    var selectedSeatUser by remember { mutableStateOf<SeatUserDetail?>(null) }
    var giftTargetUser by remember { mutableStateOf<Pair<String, String>?>(null) } // (uid, name)

    // Chat input
    var chatInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    // Active Gift Celebration Animation Overlay State
    var activeGiftCelebration by remember { mutableStateOf<GiftTransaction?>(null) }

    LaunchedEffect(latestRoomGift) {
        if (latestRoomGift != null) {
            activeGiftCelebration = latestRoomGift
            delay(4000)
            activeGiftCelebration = null
        }
    }

    // Auto-scroll chat to latest
    LaunchedEffect(roomChat.size) {
        if (roomChat.isNotEmpty()) {
            chatListState.animateScrollToItem(roomChat.size - 1)
        }
    }

    // Display UI Notice
    LaunchedEffect(uiNotice) {
        if (!uiNotice.isNullOrBlank()) {
            Toast.makeText(context, uiNotice, Toast.LENGTH_SHORT).show()
            viewModel.clearNotice()
        }
    }

    // Handle System Back button
    BackHandler {
        val isHost = currentRoom?.hostId == currentUser.userId
        if (isHost) {
            showLeaveConfirmationDialog = true
        } else {
            viewModel.closeCurrentRoom()
            onLeaveRoom()
        }
    }

    val room = currentRoom
    val isHost = room?.hostId == currentUser.userId
    val isCoHost = room?.seats?.any { it.userId == currentUser.userId && it.isCoHost } == true
    val isAdmin = currentUser.role.equals("ADMIN", ignoreCase = true) || currentUser.role.equals("SUPER_ADMIN", ignoreCase = true)
    val isHostOrAdmin = isHost || isCoHost || isAdmin

    val mySeatIndex = room?.seats?.indexOfFirst { it.userId == currentUser.userId } ?: -1
    val isSeated = mySeatIndex != -1

    // Parse Room Background Gradients
    val startColor = parseHexColor(room?.bgGradientStart ?: "#1D1135", Color(0xFF1D1135))
    val endColor = parseHexColor(room?.bgGradientEnd ?: "#0B071A", Color(0xFF0B071A))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(startColor, Color(0xFF130E26), endColor)
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // 1. TOP ROOM HEADER
            RoomTopHeader(
                room = room,
                memberCount = roomMembers.size.coerceAtLeast(1),
                isHostOrAdmin = isHostOrAdmin,
                pendingRequestsCount = speakerRequests.size,
                onBack = {
                    if (isHost) showLeaveConfirmationDialog = true
                    else {
                        viewModel.closeCurrentRoom()
                        onLeaveRoom()
                    }
                },
                onCopyRoomId = {
                    val id = room?.numericId ?: ""
                    clipboardManager.setText(AnnotatedString(id))
                    Toast.makeText(context, "Room ID #$id copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                onOpenRequests = { showSpeakerRequestsDialog = true },
                onOpenModeration = { showModerationDialog = true },
                onOpenInfo = { showRoomInfoDialog = true },
                onShare = {
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "Join my live Voice Room '${room?.title ?: "Party"}' on Great Voice Room! Room ID: ${room?.numericId}")
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Voice Room"))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. SPEAKER STAGE (10 SEATS)
            // 2 rows of 5 seats
            val seatsList = (0..9).map { index ->
                room?.seats?.getOrNull(index) ?: SeatData(seatIndex = index)
            }

            Text(
                text = "🎙️ Speaker Stage (10 Seats)",
                color = GoldPremium,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Row 1: Seats 1 to 5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0..4) {
                    val seat = seatsList[i]
                    val isSeatSpeaking = if (seat.userId == currentUser.userId) isSpeakingLocally else (seat.isSpeaking || (seat.userId != null && speakingPeers.contains(seat.userId)))
                    val isSeatMuted = if (seat.userId == currentUser.userId) isMicMuted else seat.isMuted
                    Box(modifier = Modifier.weight(1f)) {
                        SpeakerSeat(
                            seatNumber = i + 1,
                            label = if (i == 0 || seat.isHost) "👑 Host" else "Seat ${i + 1}",
                            userName = seat.userName ?: "Empty",
                            avatarUrl = seat.userAvatar,
                            isOccupied = seat.userId != null,
                            isSpeaking = isSeatSpeaking,
                            isMuted = isSeatMuted,
                            isLocked = seat.isLocked,
                            isHost = i == 0 || seat.isHost,
                            isCoHost = seat.isCoHost,
                            vipLevel = seat.vipLevel,
                            onClick = {
                                handleSeatClick(
                                    seatIndex = i,
                                    seat = seat,
                                    currentUser = currentUser,
                                    isHostOrAdmin = isHostOrAdmin,
                                    roomId = roomId,
                                    viewModel = viewModel,
                                    onOpenSeatUser = { detail -> selectedSeatUser = detail }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Seats 6 to 10
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 5..9) {
                    val seat = seatsList[i]
                    val isSeatSpeaking = if (seat.userId == currentUser.userId) isSpeakingLocally else (seat.isSpeaking || (seat.userId != null && speakingPeers.contains(seat.userId)))
                    val isSeatMuted = if (seat.userId == currentUser.userId) isMicMuted else seat.isMuted
                    Box(modifier = Modifier.weight(1f)) {
                        SpeakerSeat(
                            seatNumber = i + 1,
                            label = "Seat ${i + 1}",
                            userName = seat.userName ?: "Empty",
                            avatarUrl = seat.userAvatar,
                            isOccupied = seat.userId != null,
                            isSpeaking = isSeatSpeaking,
                            isMuted = isSeatMuted,
                            isLocked = seat.isLocked,
                            isHost = false,
                            isCoHost = seat.isCoHost,
                            vipLevel = seat.vipLevel,
                            onClick = {
                                handleSeatClick(
                                    seatIndex = i,
                                    seat = seat,
                                    currentUser = currentUser,
                                    isHostOrAdmin = isHostOrAdmin,
                                    roomId = roomId,
                                    viewModel = viewModel,
                                    onOpenSeatUser = { detail -> selectedSeatUser = detail }
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. AUDIENCE & ONLINE MEMBERS ROW
            AudienceBar(
                members = roomMembers,
                onViewAll = { showAudienceDialog = true },
                onSelectMember = { member ->
                    selectedSeatUser = SeatUserDetail(
                        userId = member.uid,
                        userName = member.displayName,
                        userAvatar = member.avatarUrl,
                        role = member.role,
                        seatIndex = member.seatIndex ?: -1
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. LIVE CHAT STREAM
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(8.dp)
            ) {
                if (roomChat.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "✨ Welcome! Chat, send gifts, and enjoy the party.",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = chatListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(roomChat) { msg ->
                            RoomChatMessageItem(
                                message = msg,
                                onUserClick = {
                                    selectedSeatUser = SeatUserDetail(
                                        userId = msg.senderId,
                                        userName = msg.senderName,
                                        userAvatar = msg.senderAvatar,
                                        role = "MEMBER",
                                        vipLevel = msg.vipLevel
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. BOTTOM INTERACTIVE CONTROL BAR
            RoomBottomControlBar(
                chatText = chatInputText,
                onChatTextChange = { chatInputText = it },
                onSendChat = {
                    if (chatInputText.isNotBlank()) {
                        viewModel.sendChat(roomId, chatInputText)
                        chatInputText = ""
                    }
                },
                isSeated = isSeated,
                isMicMuted = isMicMuted,
                onToggleMic = {
                    if (isSeated) {
                        viewModel.toggleMicMute(roomId, mySeatIndex, !isMicMuted)
                    } else {
                        Toast.makeText(context, "Take a seat or request to speak first!", Toast.LENGTH_SHORT).show()
                    }
                },
                onSeatAction = {
                    if (isSeated) {
                        viewModel.leaveSeat(roomId, mySeatIndex)
                    } else {
                        // Find first empty unlocked seat or request speak
                        val firstEmpty = room?.seats?.indexOfFirst { it.userId == null && !it.isLocked } ?: -1
                        if (firstEmpty != -1) {
                            viewModel.takeSeat(roomId, firstEmpty)
                        } else {
                            viewModel.requestToSpeak(roomId)
                        }
                    }
                },
                onOpenGift = {
                    giftTargetUser = Pair(room?.hostId ?: "", room?.hostName ?: "Host")
                    showGiftSheet = true
                },
                onOpenGame = { showGameDialog = true }
            )

            Spacer(modifier = Modifier.navigationBarsPadding().height(6.dp))
        }

        // 6. REAL-TIME GIFT CELEBRATION OVERLAY
        activeGiftCelebration?.let { gift ->
            GiftAnimationOverlay(
                gift = gift,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // --- MODALS & DIALOGS ---

    // 1. Speaker Profile Action Sheet
    selectedSeatUser?.let { seatUser ->
        SpeakerProfileActionSheet(
            user = seatUser,
            isMe = seatUser.userId == currentUser.userId,
            isHostOrAdmin = isHostOrAdmin,
            onDismiss = { selectedSeatUser = null },
            onViewProfile = {
                selectedSeatUser = null
                onNavigateProfile(seatUser.userId)
            },
            onFollowToggle = {
                Toast.makeText(context, "Followed ${seatUser.userName}! ✨", Toast.LENGTH_SHORT).show()
            },
            onDirectMessage = {
                selectedSeatUser = null
                onNavigateDirectChat(seatUser.userId, seatUser.userName, seatUser.userAvatar ?: "")
            },
            onDirectCall = {
                selectedSeatUser = null
                onNavigateDirectCall(seatUser.userId, seatUser.userName, seatUser.userAvatar ?: "")
            },
            onSendGift = {
                giftTargetUser = Pair(seatUser.userId, seatUser.userName)
                selectedSeatUser = null
                showGiftSheet = true
            },
            onReport = {
                selectedSeatUser = null
                Toast.makeText(context, "Report submitted to moderators. Thank you.", Toast.LENGTH_SHORT).show()
            },
            onBlock = {
                selectedSeatUser = null
                Toast.makeText(context, "User blocked from interacting with you.", Toast.LENGTH_SHORT).show()
            },
            onMuteSeat = {
                if (seatUser.seatIndex != -1) {
                    viewModel.hostControlSeat(roomId, seatUser.seatIndex, kick = false, mute = true)
                }
                selectedSeatUser = null
            },
            onKickSeat = {
                if (seatUser.seatIndex != -1) {
                    viewModel.hostControlSeat(roomId, seatUser.seatIndex, kick = true, mute = false)
                }
                selectedSeatUser = null
            }
        )
    }

    // 2. Real Gift Sheet
    if (showGiftSheet) {
        val target = giftTargetUser ?: Pair(room?.hostId ?: "", room?.hostName ?: "Host")
        SendGiftDialog(
            recipientName = target.second,
            recipientId = target.first,
            userCoins = currentUser.coinBalance,
            onDismiss = { showGiftSheet = false },
            onRechargeClick = {
                showGiftSheet = false
                onNavigateWallet()
            },
            onSend = { gift ->
                viewModel.sendGift(
                    roomId = roomId,
                    recipientId = target.first,
                    recipientName = target.second,
                    giftId = gift.id,
                    giftName = gift.name,
                    giftEmoji = gift.emoji,
                    coins = gift.costCoins,
                    onSuccess = {
                        showGiftSheet = false
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    // 3. Speaker Requests Dialog
    if (showSpeakerRequestsDialog) {
        SpeakerRequestsDialog(
            requests = speakerRequests,
            onDismiss = { showSpeakerRequestsDialog = false },
            onApprove = { req ->
                viewModel.approveSpeakerRequest(roomId, req)
            },
            onReject = { userId ->
                viewModel.rejectSpeakerRequest(roomId, userId)
            }
        )
    }

    // 4. Room Moderation Dialog
    if (showModerationDialog) {
        RoomModerationDialog(
            isRoomLocked = room?.isLocked == true,
            onDismiss = { showModerationDialog = false },
            onMuteAll = { viewModel.muteAllSeats(roomId, true) },
            onUnmuteAll = { viewModel.muteAllSeats(roomId, false) },
            onToggleLock = { lock, pass -> viewModel.setRoomLock(roomId, lock, pass) }
        )
    }

    // 5. Audience List Dialog
    if (showAudienceDialog) {
        AudienceListDialog(
            members = roomMembers,
            onDismiss = { showAudienceDialog = false },
            onSelectMember = { member ->
                showAudienceDialog = false
                selectedSeatUser = SeatUserDetail(
                    userId = member.uid,
                    userName = member.displayName,
                    userAvatar = member.avatarUrl,
                    role = member.role
                )
            }
        )
    }

    // 6. In-Room Mini Game (Dragon vs Tiger preview)
    if (showGameDialog) {
        InRoomMiniGameDialog(
            userCoins = currentUser.coinBalance,
            onDismiss = { showGameDialog = false }
        )
    }

    // 7. Room Info Dialog
    if (showRoomInfoDialog) {
        RoomInfoDialog(
            room = room,
            onDismiss = { showRoomInfoDialog = false }
        )
    }

    // 8. Leave Confirmation for Host
    if (showLeaveConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmationDialog = false },
            title = { Text("Leave Room?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "You are the Host of this room. Leaving will end the broadcast for all participants.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmationDialog = false
                        viewModel.deleteRoom(roomId) {
                            onLeaveRoom()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("End Room", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmationDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1E1736),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ----------------------------------------------------
// UI SUBCOMPONENTS
// ----------------------------------------------------

@Composable
private fun RoomTopHeader(
    room: RoomData?,
    memberCount: Int,
    isHostOrAdmin: Boolean,
    pendingRequestsCount: Int,
    onBack: () -> Unit,
    onCopyRoomId: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenModeration: () -> Unit,
    onOpenInfo: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Back Arrow + Room Info Pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Leave Room",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Room Pill with Avatar, Title & ID
            Surface(
                onClick = onOpenInfo,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = room?.coverUrl ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150",
                        contentDescription = "Room Cover",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Text(
                            text = room?.title ?: "Live Voice Room",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onCopyRoomId() }
                        ) {
                            Text(
                                text = "ID: ${room?.numericId ?: "000000"}",
                                color = GoldPremium,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = GoldPremium,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Right: Active members count + Host Action buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Live Pulse Member Chip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, TealPremium.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "$memberCount",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Pending Speaker Requests Badge (Host only)
            if (isHostOrAdmin) {
                BadgedBox(
                    badge = {
                        if (pendingRequestsCount > 0) {
                            Badge(containerColor = GoldPremium, contentColor = Color.Black) {
                                Text("$pendingRequestsCount", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = onOpenRequests,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Text("✋", fontSize = 16.sp)
                    }
                }

                // Moderation Tools
                IconButton(
                    onClick = onOpenModeration,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Moderation",
                        tint = GoldPremium,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Share Button
            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AudienceBar(
    members: List<RoomMember>,
    onViewAll: () -> Unit,
    onSelectMember: (RoomMember) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Audience",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(members.take(8)) { member ->
                    AsyncImage(
                        model = member.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                        contentDescription = member.displayName,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .border(1.dp, TealPremium.copy(alpha = 0.5f), CircleShape)
                            .clickable { onSelectMember(member) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        TextButton(
            onClick = onViewAll,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("All (${members.size}) ›", color = TealPremium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RoomChatMessageItem(
    message: ChatMessage,
    onUserClick: () -> Unit
) {
    if (message.isSystem || message.type == MessageType.SYSTEM) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF673AB7).copy(alpha = 0.25f),
            border = BorderStroke(0.8.dp, Color(0xFF9C27B0).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = message.text,
                color = Color(0xFFE1BEE7),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = message.senderAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
            contentDescription = message.senderName,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.senderName,
                    color = GoldPremium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                if (message.vipLevel > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    VipBadge(vipType = "VIP", vipLevel = message.vipLevel)
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RoomBottomControlBar(
    chatText: String,
    onChatTextChange: (String) -> Unit,
    onSendChat: () -> Unit,
    isSeated: Boolean,
    isMicMuted: Boolean,
    onToggleMic: () -> Unit,
    onSeatAction: () -> Unit,
    onOpenGift: () -> Unit,
    onOpenGame: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Chat Input Field
        OutlinedTextField(
            value = chatText,
            onValueChange = onChatTextChange,
            placeholder = { Text("Say something...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("room_chat_input"),
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPremium,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
            ),
            trailingIcon = {
                if (chatText.isNotBlank()) {
                    IconButton(onClick = onSendChat) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = TealPremium,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendChat() })
        )

        // Mic Toggle Button
        IconButton(
            onClick = onToggleMic,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (!isSeated) Color.White.copy(alpha = 0.08f)
                    else if (isMicMuted) Color(0xFFD32F2F)
                    else Color(0xFF00C853)
                )
        ) {
            Icon(
                imageVector = if (isMicMuted || !isSeated) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Toggle Mic",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Take / Leave Seat or Raise Hand
        IconButton(
            onClick = onSeatAction,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isSeated) Color(0xFFFF9800) else Color(0xFF673AB7))
        ) {
            Text(
                text = if (isSeated) "🚪" else "✋",
                fontSize = 18.sp
            )
        }

        // In-Room Game Launcher
        IconButton(
            onClick = onOpenGame,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF283593))
        ) {
            Text("🎮", fontSize = 18.sp)
        }

        // Luxury Gift Button with Golden Pulse
        IconButton(
            onClick = onOpenGift,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(GoldPremium, Color(0xFFFF8F00)))
                )
                .border(1.5.dp, Color.White, CircleShape)
                .testTag("room_gift_action_button")
        ) {
            Text("🎁", fontSize = 20.sp)
        }
    }
}

// ----------------------------------------------------
// SPEAKER PROFILE ACTION SHEET
// ----------------------------------------------------

data class SeatUserDetail(
    val userId: String,
    val userName: String,
    val userAvatar: String? = null,
    val role: String = "USER",
    val vipLevel: Int = 0,
    val seatIndex: Int = -1
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeakerProfileActionSheet(
    user: SeatUserDetail,
    isMe: Boolean,
    isHostOrAdmin: Boolean,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onFollowToggle: () -> Unit,
    onDirectMessage: () -> Unit,
    onDirectCall: () -> Unit,
    onSendGift: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onMuteSeat: () -> Unit,
    onKickSeat: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B1530),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, GoldPremium, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = user.userAvatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    contentDescription = user.userName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name + Role / VIP
            Text(
                text = user.userName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (user.role.contains("ADMIN", ignoreCase = true)) {
                    AdminBadge(role = user.role)
                }
                if (user.vipLevel > 0) {
                    VipBadge(vipType = "VIP", vipLevel = user.vipLevel)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main User Action Buttons (Grid 4 buttons)
            if (!isMe) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionIconPill("Profile", Icons.Default.Person, GoldPremium) { onViewProfile() }
                    ActionIconPill("Follow", Icons.Default.Favorite, Color(0xFFFF4081)) { onFollowToggle() }
                    ActionIconPill("Chat", Icons.Default.ChatBubble, TealPremium) { onDirectMessage() }
                    ActionIconPill("Voice Call", Icons.Default.Call, Color(0xFF00E676)) { onDirectCall() }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Send Gift Primary Action Button
                Button(
                    onClick = onSendGift,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("🎁 Send Luxury Gift", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Moderator / Host Controls for Seat
            if (isHostOrAdmin && !isMe && user.seatIndex != -1) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Text("Host Management", color = GoldPremium, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onMuteSeat,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800)),
                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                    ) {
                        Text("Mute Seat 🔇", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onKickSeat,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                        border = BorderStroke(1.dp, Color(0xFFFF5252))
                    ) {
                        Text("Remove Seat 🚪", fontSize = 12.sp)
                    }
                }
            }

            // Report & Block Secondary Actions
            if (!isMe) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onReport) {
                        Text("Report User 🚩", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    TextButton(onClick = onBlock) {
                        Text("Block User 🚫", color = Color(0xFFFF5252).copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ActionIconPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f))
                .border(1.dp, tint.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

// ----------------------------------------------------
// AUDIENCE LIST DIALOG
// ----------------------------------------------------

@Composable
private fun AudienceListDialog(
    members: List<RoomMember>,
    onDismiss: () -> Unit,
    onSelectMember: (RoomMember) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Room Participants (${members.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onSelectMember(member) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = member.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                            contentDescription = member.displayName,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = member.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Role: ${member.role}", color = TealPremium, fontSize = 11.sp)
                        }
                        Text("Profile ›", color = GoldPremium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = Color(0xFF1B1430),
        shape = RoundedCornerShape(20.dp)
    )
}

// ----------------------------------------------------
// IN-ROOM MINI GAME (DRAGON VS TIGER)
// ----------------------------------------------------

@Composable
private fun InRoomMiniGameDialog(
    userCoins: Long,
    onDismiss: () -> Unit
) {
    var dragonScore by remember { mutableStateOf(8) }
    var tigerScore by remember { mutableStateOf(5) }
    var gameResult by remember { mutableStateOf("Dragon Wins! 🐉") }
    var isPlaying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🐉 Dragon vs Tiger 🐅", color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("In-Room Quick Play", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐉 Dragon", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$dragonScore", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    }
                    Text("VS", color = GoldPremium, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐅 Tiger", color = Color(0xFFFFD600), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("$tigerScore", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = gameResult,
                        color = TealPremium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Button(
                    onClick = {
                        val d = (1..13).random()
                        val t = (1..13).random()
                        dragonScore = d
                        tigerScore = t
                        gameResult = when {
                            d > t -> "Dragon Wins! 🐉"
                            t > d -> "Tiger Wins! 🐅"
                            else -> "It's a TIE! ⚖️"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Deal Cards 🎲", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = Color(0xFF1B1430),
        shape = RoundedCornerShape(20.dp)
    )
}

// ----------------------------------------------------
// ROOM INFO DIALOG
// ----------------------------------------------------

@Composable
private fun RoomInfoDialog(
    room: RoomData?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(room?.title ?: "Room Details", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Host: ${room?.hostName ?: "Host"}", color = GoldPremium, fontSize = 13.sp)
                Text("Category: ${room?.tag ?: "Party"}", color = TealPremium, fontSize = 13.sp)
                Text("Description: ${room?.description ?: "Welcome!"}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("Numeric ID: ${room?.numericId ?: "000000"}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF1B1430),
        shape = RoundedCornerShape(20.dp)
    )
}

// Helper Seat Click Handler
private fun handleSeatClick(
    seatIndex: Int,
    seat: SeatData,
    currentUser: FirebaseUserProfile,
    isHostOrAdmin: Boolean,
    roomId: String,
    viewModel: RoomViewModel,
    onOpenSeatUser: (SeatUserDetail) -> Unit
) {
    if (seat.userId != null) {
        onOpenSeatUser(
            SeatUserDetail(
                userId = seat.userId,
                userName = seat.userName ?: "Speaker",
                userAvatar = seat.userAvatar,
                role = if (seat.isHost) "HOST" else if (seat.isCoHost) "CO_HOST" else "SPEAKER",
                vipLevel = seat.vipLevel,
                seatIndex = seatIndex
            )
        )
    } else {
        if (seat.isLocked && !isHostOrAdmin) {
            return
        }
        viewModel.takeSeat(roomId, seatIndex)
    }
}

private fun parseHexColor(hex: String, defaultColor: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = if (clean.length == 6) {
            (0xFF000000 or clean.toLong(16)).toInt()
        } else if (clean.length == 8) {
            clean.toLong(16).toInt()
        } else {
            return defaultColor
        }
        Color(colorInt)
    } catch (e: Exception) {
        defaultColor
    }
}
