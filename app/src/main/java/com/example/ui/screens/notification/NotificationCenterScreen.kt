package com.example.ui.screens.notification

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.AppNotification
import com.example.data.model.NotificationTypes
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToRoom: (String) -> Unit,
    onNavigateToVip: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val filteredList = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "SOCIAL" -> notifications.filter {
                it.type in listOf(
                    NotificationTypes.FOLLOW,
                    NotificationTypes.FOLLOW_REQUEST,
                    NotificationTypes.FOLLOW_ACCEPTED,
                    NotificationTypes.NEW_MESSAGE
                )
            }
            "GIFTS_VIP" -> notifications.filter {
                it.type in listOf(
                    NotificationTypes.GIFT_RECEIVED,
                    NotificationTypes.VIP_LEVEL_UP,
                    NotificationTypes.NORMAL_LEVEL_UP,
                    NotificationTypes.FRAME_UNLOCKED
                )
            }
            "SYSTEM_MOD" -> notifications.filter {
                it.type in listOf(
                    NotificationTypes.SYSTEM,
                    NotificationTypes.MODERATION,
                    NotificationTypes.REPORT_UPDATE,
                    NotificationTypes.ROOM_INVITATION,
                    NotificationTypes.GAME_INVITATION
                )
            }
            else -> notifications
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Notifications",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "$unreadCount",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllAsRead() },
                            modifier = Modifier.testTag("mark_all_read_btn")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Read All", fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Notification Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { viewModel.setFilter("ALL") },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == "SOCIAL",
                    onClick = { viewModel.setFilter("SOCIAL") },
                    label = { Text("Social") }
                )
                FilterChip(
                    selected = selectedFilter == "GIFTS_VIP",
                    onClick = { viewModel.setFilter("GIFTS_VIP") },
                    label = { Text("Gifts & VIP") }
                )
                FilterChip(
                    selected = selectedFilter == "SYSTEM_MOD",
                    onClick = { viewModel.setFilter("SYSTEM_MOD") },
                    label = { Text("System") }
                )
            }

            if (isLoading && filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No notifications yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "When you receive gifts, messages, or followers, they will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.notificationId }) { notif ->
                        NotificationItemCard(
                            notification = notif,
                            onClick = {
                                viewModel.markAsRead(notif.notificationId)
                                handleDeepLink(
                                    notification = notif,
                                    onNavigateToProfile = onNavigateToProfile,
                                    onNavigateToChat = onNavigateToChat,
                                    onNavigateToRoom = onNavigateToRoom,
                                    onNavigateToVip = onNavigateToVip,
                                    onNavigateToGame = onNavigateToGame,
                                    onNavigateToReports = onNavigateToReports
                                )
                            },
                            onDelete = {
                                viewModel.deleteNotification(notif.notificationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val iconInfo = getNotificationIcon(notification.type)
    val timeFormatted = formatTimeAgo(notification.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notif_item_${notification.notificationId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notification.isRead) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Avatar
            Box(modifier = Modifier.size(48.dp)) {
                if (!notification.senderAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = notification.senderAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = iconInfo.second.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconInfo.first,
                                contentDescription = null,
                                tint = iconInfo.second,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Small badge icon if avatar is present
                if (!notification.senderAvatarUrl.isNullOrBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = iconInfo.second,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconInfo.first,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Notification content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title.ifBlank { "Notification" },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Unread dot
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

private fun handleDeepLink(
    notification: AppNotification,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToRoom: (String) -> Unit,
    onNavigateToVip: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val refId = notification.referenceId ?: ""
    when (notification.referenceType) {
        "USER" -> if (refId.isNotBlank()) onNavigateToProfile(refId)
        "CONVERSATION" -> if (refId.isNotBlank()) onNavigateToChat(refId)
        "ROOM" -> if (refId.isNotBlank()) onNavigateToRoom(refId)
        "VIP" -> onNavigateToVip()
        "GAME" -> onNavigateToGame()
        "REPORT" -> onNavigateToReports()
        else -> {
            when (notification.type) {
                NotificationTypes.FOLLOW, NotificationTypes.FOLLOW_REQUEST -> if (notification.senderUid.isNotBlank()) onNavigateToProfile(notification.senderUid)
                NotificationTypes.NEW_MESSAGE -> if (refId.isNotBlank()) onNavigateToChat(refId)
                NotificationTypes.VIP_LEVEL_UP, NotificationTypes.NORMAL_LEVEL_UP -> onNavigateToVip()
                NotificationTypes.ROOM_INVITATION, NotificationTypes.ROOM_EVENT -> if (refId.isNotBlank()) onNavigateToRoom(refId)
                NotificationTypes.GAME_INVITATION -> onNavigateToGame()
                NotificationTypes.MODERATION, NotificationTypes.REPORT_UPDATE -> onNavigateToReports()
            }
        }
    }
}

private fun getNotificationIcon(type: String): Pair<ImageVector, Color> {
    return when (type) {
        NotificationTypes.FOLLOW, NotificationTypes.FOLLOW_REQUEST, NotificationTypes.FOLLOW_ACCEPTED ->
            Pair(Icons.Default.PersonAdd, Color(0xFF2196F3))
        NotificationTypes.NEW_MESSAGE ->
            Pair(Icons.Default.Chat, Color(0xFF4CAF50))
        NotificationTypes.GIFT_RECEIVED ->
            Pair(Icons.Default.CardGiftcard, Color(0xFFFF4081))
        NotificationTypes.INCOMING_CALL, NotificationTypes.MISSED_CALL ->
            Pair(Icons.Default.Call, Color(0xFFFF9800))
        NotificationTypes.VIP_LEVEL_UP, NotificationTypes.NORMAL_LEVEL_UP, NotificationTypes.FRAME_UNLOCKED ->
            Pair(Icons.Default.WorkspacePremium, Color(0xFFFFD700))
        NotificationTypes.ROOM_INVITATION, NotificationTypes.ROOM_EVENT ->
            Pair(Icons.Default.Groups, Color(0xFF9C27B0))
        NotificationTypes.GAME_INVITATION ->
            Pair(Icons.Default.SportsEsports, Color(0xFF00BCD4))
        NotificationTypes.MODERATION, NotificationTypes.REPORT_UPDATE ->
            Pair(Icons.Default.Shield, Color(0xFFE91E63))
        else ->
            Pair(Icons.Default.Notifications, Color(0xFF607D8B))
    }
}

private fun formatTimeAgo(timeMs: Long): String {
    val diff = System.currentTimeMillis() - timeMs
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMs))
    }
}
