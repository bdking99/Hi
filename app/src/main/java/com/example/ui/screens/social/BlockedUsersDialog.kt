package com.example.ui.screens.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.BlockedUserRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersDialog(
    viewModel: SocialViewModel,
    onDismiss: () -> Unit
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Blocked Users (${blockedUsers.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (blockedUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No blocked users", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "When you block users, they won't be able to message, call, or interact with you.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(blockedUsers, key = { it.targetUid }) { record ->
                                BlockedUserCard(
                                    record = record,
                                    onUnblock = {
                                        viewModel.unblockUser(record.targetUid, record.targetDisplayName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUserCard(
    record: BlockedUserRecord,
    onUnblock: () -> Unit
) {
    val dateFormatted = remember(record.blockedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(record.blockedAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (record.targetAvatar.isNotBlank()) {
                AsyncImage(
                    model = record.targetAvatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            record.targetDisplayName.take(1).uppercase().ifBlank { "U" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.targetDisplayName.ifBlank { "Blocked User" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    "ID: ${record.targetPublicUserId} • Blocked $dateFormatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onUnblock,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("unblock_btn_${record.targetUid}")
            ) {
                Text("Unblock", fontSize = 12.sp)
            }
        }
    }
}
