package com.example.ui.screens.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationViewModel,
    onNavigateBack: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Push Notification Toggle
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Push Notifications", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "Receive notifications even when the app is in the background",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = prefs.pushNotificationsEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updatePreferences(prefs.copy(pushNotificationsEnabled = isChecked))
                        },
                        modifier = Modifier.testTag("push_notifications_toggle")
                    )
                }
            }

            Text(
                "Activity Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PreferenceToggleRow(
                        icon = Icons.Default.Chat,
                        title = "Messages",
                        subtitle = "Direct chat messages and replies",
                        checked = prefs.messages,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(messages = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.Call,
                        title = "Voice Calls",
                        subtitle = "Incoming and missed voice call alerts",
                        checked = prefs.calls,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(calls = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.CardGiftcard,
                        title = "Gifts",
                        subtitle = "Received gifts and token rewards",
                        checked = prefs.gifts,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(gifts = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.PersonAdd,
                        title = "Followers & Requests",
                        subtitle = "New followers and private follow requests",
                        checked = prefs.followers,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(followers = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.Groups,
                        title = "Voice Room Invitations",
                        subtitle = "Invitations from hosts and room speakers",
                        checked = prefs.roomInvitations,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(roomInvitations = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.SportsEsports,
                        title = "Game Invitations",
                        subtitle = "Dragon Tiger and JDI Lion arena invites",
                        checked = prefs.gameInvitations,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(gameInvitations = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.WorkspacePremium,
                        title = "VIP & Level Updates",
                        subtitle = "VIP level upgrades and frame unlocks",
                        checked = prefs.vipLevelUpdates,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(vipLevelUpdates = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    PreferenceToggleRow(
                        icon = Icons.Default.Shield,
                        title = "Moderation & Reports",
                        subtitle = "Report status updates and security notices",
                        checked = prefs.moderationUpdates,
                        onCheckedChange = { viewModel.updatePreferences(prefs.copy(moderationUpdates = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
