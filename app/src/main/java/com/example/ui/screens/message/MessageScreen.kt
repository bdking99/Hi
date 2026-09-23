package com.example.ui.screens.message

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DirectConversationItem
import com.example.data.model.FirebaseUserProfile
import com.example.data.repository.UserRepository
import com.example.ui.components.ProfileFrameComponent
import com.example.ui.components.UserSearchSheet
import com.example.ui.screens.call.CallHistorySheet
import com.example.ui.screens.call.CallViewModel
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    chatViewModel: ChatViewModel,
    callViewModel: CallViewModel,
    userRepository: UserRepository,
    currentUserId: String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Chats", "Calls", "Notices")

    val conversations by chatViewModel.conversations.collectAsState()
    val isLoadingConversations by chatViewModel.isLoadingConversations.collectAsState()
    val activeConversationId by chatViewModel.activeConversationId.collectAsState()

    var showSearchSheet by remember { mutableStateOf(false) }
    var showCallHistorySheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MessageTopBar(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                onSearchClick = { showSearchSheet = true },
                onCallHistoryClick = { showCallHistorySheet = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: Real-Time Private Chats
                    if (isLoadingConversations && conversations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = TealPremium)
                        }
                    } else if (conversations.isEmpty()) {
                        EmptyChatState(
                            onStartChat = { showSearchSheet = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("conversation_list")
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                EventCenterBanner()
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(conversations, key = { it.conversation.conversationId }) { item ->
                                DirectConversationTile(
                                    item = item,
                                    currentUserId = currentUserId,
                                    onClick = {
                                        chatViewModel.openConversation(
                                            item.conversation.conversationId,
                                            item.otherUser
                                        )
                                    }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(start = 76.dp, end = 16.dp)
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(90.dp))
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Voice Calls
                    CallHistoryList(
                        callViewModel = callViewModel,
                        currentUserId = currentUserId,
                        onNewCall = { showSearchSheet = true }
                    )
                }

                2 -> {
                    // TAB 2: System & Clan Notices
                    SystemNoticesList()
                }
            }
        }
    }

    // Direct Chat BottomSheet when a conversation is active
    if (activeConversationId != null) {
        DirectChatSheet(
            chatViewModel = chatViewModel,
            callViewModel = callViewModel,
            currentUserId = currentUserId,
            onDismiss = {
                chatViewModel.closeActiveConversation()
            }
        )
    }

    // Search User Sheet to start new chat or voice call
    if (showSearchSheet) {
        UserSearchSheet(
            userRepository = userRepository,
            currentUserId = currentUserId,
            onDismiss = { showSearchSheet = false },
            onUserSelected = { targetUser ->
                showSearchSheet = false
                chatViewModel.openConversationWith(targetUser)
            }
        )
    }

    // Dedicated Call History Sheet
    if (showCallHistorySheet) {
        CallHistorySheet(
            viewModel = callViewModel,
            currentUserId = currentUserId,
            onDismiss = { showCallHistorySheet = false }
        )
    }
}

@Composable
fun MessageTopBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onCallHistoryClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tabs Row
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedIndex == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onTabSelected(index) }
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = if (isSelected) 18.sp else 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(GoldPremium, TealPremium))
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }

            // Right Action Icons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Outlined.PersonSearch,
                        contentDescription = "Find User",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onCallHistoryClick) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneInTalk,
                        contentDescription = "Call History",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun DirectConversationTile(
    item: DirectConversationItem,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherUser = item.otherUser
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(item.conversation.updatedAt) {
        timeFormat.format(Date(item.conversation.updatedAt))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar with Real VIP Frame & Online Presence
        Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            ProfileFrameComponent(
                frameId = otherUser?.profileFrameId ?: "frame_default",
                avatarUrl = otherUser?.avatar ?: "",
                size = 50.dp,
                glowActive = otherUser?.isOnline == true
            )

            if (otherUser?.isOnline == true) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content: Name, Last Message, Time, Badges
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = otherUser?.displayName?.ifBlank { "User ${otherUser.publicUserId}" } ?: "Voice User",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if ((otherUser?.vipLevel ?: 0) > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldPremium
                        ) {
                            Text(
                                text = "VIP ${otherUser!!.vipLevel}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.conversation.lastMessage.ifBlank { "Conversation started" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.unreadCount > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (item.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (item.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF3366),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatState(onStartChat: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.MarkChatUnread,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(68.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No private messages yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Find friends by Public ID to start a conversation or voice call.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onStartChat,
                colors = ButtonDefaults.buttonColors(containerColor = TealPremium),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Find Users", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CallHistoryList(
    callViewModel: CallViewModel,
    currentUserId: String,
    onNewCall: () -> Unit
) {
    val history by callViewModel.callHistory.collectAsState()
    val timeFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }

    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PhoneCallback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No call records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Calls made with HD WebRTC are logged here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onNewCall,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Voice Call", color = Color.Black, fontWeight = FontWeight.Bold)
                }
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
                            size = 48.dp
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = partnerName.ifBlank { "ID: $partnerPublicId" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(2.dp))

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

                    // Call Back Button
                    IconButton(
                        onClick = {
                            val target = FirebaseUserProfile(
                                userId = if (isOutgoing) call.receiverUid else call.callerUid,
                                publicUserId = partnerPublicId,
                                displayName = partnerName,
                                avatar = partnerAvatar
                            )
                            callViewModel.initiateCall(target)
                        }
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call back", tint = TealPremium)
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    modifier = Modifier.padding(start = 78.dp, end = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SystemNoticesList() {
    val notices = listOf(
        Pair("Security & Privacy", "Great Voice Room uses end-to-end WebRTC calling with secure Firestore signaling."),
        Pair("Daily VIP Rewards", "Log in daily to claim free diamond gifts and VIP experience points."),
        Pair("Fair Play Policy", "Harassment, hate speech, and spamming in voice rooms or chats result in instant ban.")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(notices) { notice ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = GoldPremium.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldPremium)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(notice.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(notice.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun EventCenterBanner() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF311042), Color(0xFF6B1D78), Color(0xFFE040FB))
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎪", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Event Center",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = GoldPremium
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Voice Carnival Round 3 • Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
