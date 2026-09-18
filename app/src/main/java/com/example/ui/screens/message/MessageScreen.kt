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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean
)

data class ChatConversation(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val lastMessage: String,
    val timestamp: String,
    val distance: String,
    val unreadCount: Int,
    val isOnline: Boolean,
    val isSystem: Boolean,
    val systemIcon: ImageVector? = null,
    val systemIconColor: Color = Color.Unspecified,
    val vipLevel: Int? = null,
    val category: String = "Message" // "Message", "Friends", "Family"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Message", "Friends", "Family")

    // State for interactive chat details
    var activeChatConversation by remember { mutableStateOf<ChatConversation?>(null) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Chat data
    var conversations by remember {
        mutableStateOf(
            listOf(
                // System Notifications
                ChatConversation(
                    id = "sys_1",
                    name = "System Notice",
                    avatarUrl = "",
                    lastMessage = "Security Alert: Welcome to Great Voice Room! Bind your phone for extra protection.",
                    timestamp = "10:30 AM",
                    distance = "Official",
                    unreadCount = 1,
                    isOnline = true,
                    isSystem = true,
                    systemIcon = Icons.Filled.Shield,
                    systemIconColor = Color(0xFF2979FF),
                    category = "Message"
                ),
                ChatConversation(
                    id = "sys_2",
                    name = "Reward Assistant",
                    avatarUrl = "",
                    lastMessage = "Daily Check-in Bonus: +200 Coins deposited into your wallet! 🎁",
                    timestamp = "Yesterday",
                    distance = "System",
                    unreadCount = 2,
                    isOnline = true,
                    isSystem = true,
                    systemIcon = Icons.Filled.CardGiftcard,
                    systemIconColor = GoldPremium,
                    category = "Message"
                ),
                ChatConversation(
                    id = "sys_3",
                    name = "Activity Assistant",
                    avatarUrl = "",
                    lastMessage = "Brazilian Independence Carnival: Ranking table updated! Check your rank.",
                    timestamp = "Sep 16",
                    distance = "Events",
                    unreadCount = 0,
                    isOnline = true,
                    isSystem = true,
                    systemIcon = Icons.Filled.Campaign,
                    systemIconColor = Color(0xFFFF4081),
                    category = "Message"
                ),

                // Personal Chats
                ChatConversation(
                    id = "c1",
                    name = "Sophia Queen ✨",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    lastMessage = "Are you joining the late night voice party tonight? 🎸",
                    timestamp = "12:45 PM",
                    distance = "0.01 Km",
                    unreadCount = 3,
                    isOnline = true,
                    isSystem = false,
                    vipLevel = 5,
                    category = "Friends"
                ),
                ChatConversation(
                    id = "c2",
                    name = "Prince Leo 🦁",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    lastMessage = "Thanks for the rocket gift in the room earlier! 🔥",
                    timestamp = "11:20 AM",
                    distance = "0.4 Km",
                    unreadCount = 1,
                    isOnline = true,
                    isSystem = false,
                    vipLevel = 3,
                    category = "Friends"
                ),
                ChatConversation(
                    id = "c3",
                    name = "Royal Voice Family 👑",
                    avatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150",
                    lastMessage = "Clan Battle starts in 2 hours! Everyone get ready.",
                    timestamp = "09:15 AM",
                    distance = "Clan HQ",
                    unreadCount = 8,
                    isOnline = true,
                    isSystem = false,
                    vipLevel = 4,
                    category = "Family"
                ),
                ChatConversation(
                    id = "c4",
                    name = "Elena Rose 🌹",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    lastMessage = "See you on the mic later!",
                    timestamp = "Yesterday",
                    distance = "1.2 Km",
                    unreadCount = 0,
                    isOnline = false,
                    isSystem = false,
                    vipLevel = 2,
                    category = "Friends"
                ),
                ChatConversation(
                    id = "c5",
                    name = "Samir Khan 🎙️",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    lastMessage = "Let's co-host the weekend acoustic session.",
                    timestamp = "Sep 15",
                    distance = "2.8 Km",
                    unreadCount = 0,
                    isOnline = true,
                    isSystem = false,
                    vipLevel = 1,
                    category = "Friends"
                ),
                ChatConversation(
                    id = "c6",
                    name = "Golden Phoenix Family 🦅",
                    avatarUrl = "https://images.unsplash.com/photo-1543269865-cbf427effbad?w=150",
                    lastMessage = "Weekly contribution rewards are ready to collect!",
                    timestamp = "Sep 14",
                    distance = "Clan Hub",
                    unreadCount = 0,
                    isOnline = false,
                    isSystem = false,
                    category = "Family"
                )
            )
        )
    }

    val filteredConversations = remember(selectedTab, searchQuery, conversations) {
        val currentTabTitle = tabs[selectedTab]
        conversations.filter { conv ->
            val matchesTab = when (currentTabTitle) {
                "Message" -> true
                "Friends" -> conv.category == "Friends" || !conv.isSystem
                "Family" -> conv.category == "Family"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                conv.name.contains(searchQuery, ignoreCase = true) ||
                conv.lastMessage.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesSearch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MessageTopBar(
                tabs = tabs,
                selectedIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                onSearchClick = { showSearchDialog = true },
                onAddContacts = { /* Contacts */ }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("message_screen_list")
        ) {
            // Event Center Banner Pinned Above Chat List
            item {
                Spacer(modifier = Modifier.height(8.dp))
                EventCenterBanner()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Chat List Section
            items(filteredConversations, key = { it.id }) { conv ->
                ChatConversationTile(
                    conversation = conv,
                    onClick = {
                        // Mark as read
                        conversations = conversations.map {
                            if (it.id == conv.id) it.copy(unreadCount = 0) else it
                        }
                        activeChatConversation = conv
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

    // Interactive Chat Details BottomSheet / Modal
    if (activeChatConversation != null) {
        ChatDetailSheet(
            conversation = activeChatConversation!!,
            onDismiss = { activeChatConversation = null }
        )
    }

    // Search Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Messages") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chats or contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showSearchDialog = false }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    searchQuery = ""
                    showSearchDialog = false
                }) {
                    Text("Clear")
                }
            }
        )
    }
}

// ----------------------------------------------------
// Message Top Navigation Bar
// ----------------------------------------------------
@Composable
fun MessageTopBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onSearchClick: () -> Unit,
    onAddContacts: () -> Unit
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
            // 3 Filter Tabs: "Message", "Friends", "Family"
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
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
                                    .width(24.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GoldPremium, TealPremium)
                                        )
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }

            // Right Action Icons: Search + New Chat / Contacts
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onAddContacts) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = "Contacts",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Event Center Banner (Pinned Above Chat List)
// ----------------------------------------------------
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
                    // Glossy Gift Icon
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
                            text = "Voice Carnival Round 3 • Ends in 05h 22m",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoldPremium,
                    modifier = Modifier.clickable { /* Claim */ }
                ) {
                    Text(
                        text = "Claim",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Chat Conversation Tile
// ----------------------------------------------------
@Composable
fun ChatConversationTile(
    conversation: ChatConversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture with 3D Border or System Icon
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.isSystem) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(conversation.systemIconColor.copy(alpha = 0.18f))
                        .border(1.5.dp, conversation.systemIconColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = conversation.systemIcon ?: Icons.Default.Notifications,
                        contentDescription = conversation.name,
                        tint = conversation.systemIconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = conversation.avatarUrl,
                    contentDescription = conversation.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(1.5.dp, if (conversation.vipLevel != null) GoldPremium else Color.Transparent, CircleShape)
                )

                // Online indicator
                if (conversation.isOnline) {
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
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name, Last message preview, Timestamp, Distance, Unread badge
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = conversation.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.vipLevel != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoldPremium
                        ) {
                            Text(
                                text = "VIP ${conversation.vipLevel}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = conversation.timestamp,
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
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Distance Indicator
                    Text(
                        text = "📍 ${conversation.distance}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealPremium,
                        fontSize = 10.sp
                    )

                    // Unread Message Badge
                    if (conversation.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFF3366),
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Interactive Chat Details Sheet
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailSheet(
    conversation: ChatConversation,
    onDismiss: () -> Unit
) {
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("m1", conversation.name, "Hey! How are you doing today?", "10:14 AM", false),
                ChatMessage("m2", "Me", "Great! Just tuning in to the live broadcast.", "10:15 AM", true),
                ChatMessage("m3", conversation.name, conversation.lastMessage, conversation.timestamp, false)
            )
        )
    }
    var typedText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 20.dp)
        ) {
            // Chat Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.isSystem) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(conversation.systemIconColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(conversation.systemIcon ?: Icons.Default.Info, contentDescription = null, tint = conversation.systemIconColor)
                        }
                    } else {
                        AsyncImage(
                            model = conversation.avatarUrl,
                            contentDescription = conversation.name,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(conversation.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (conversation.isOnline) "🟢 Online • ${conversation.distance}" else "Offline • ${conversation.distance}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (conversation.isOnline) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Call buttons
                if (!conversation.isSystem) {
                    Row {
                        IconButton(onClick = { /* Audio Call */ }) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = TealPremium)
                        }
                        IconButton(onClick = { /* Video Call */ }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video", tint = TealPremium)
                        }
                    }
                }
            }

            HorizontalDivider()

            // Chat Messages History
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed(), key = { it.id }) { msg ->
                    ChatBubble(msg)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Write a message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = { typedText += " ❤️" }) {
                            Text("😊", fontSize = 18.sp)
                        }
                    },
                    trailingIcon = {
                        if (typedText.isNotBlank()) {
                            IconButton(onClick = {
                                messages = messages + ChatMessage(
                                    id = "msg_${System.currentTimeMillis()}",
                                    senderName = "Me",
                                    text = typedText,
                                    timestamp = "Just now",
                                    isFromMe = true
                                )
                                typedText = ""
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = TealPremium)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (msg.isFromMe) 16.dp else 4.dp,
                bottomEnd = if (msg.isFromMe) 4.dp else 16.dp
            ),
            color = if (msg.isFromMe) TealPremium else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = msg.text,
                    color = if (msg.isFromMe) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg.timestamp,
                    color = if (msg.isFromMe) Color.Black.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
