package com.example.ui.screens.message

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DirectMessage
import com.example.data.model.FirebaseUserProfile
import com.example.ui.components.ProfileFrameComponent
import com.example.ui.screens.call.CallViewModel
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatSheet(
    chatViewModel: ChatViewModel,
    callViewModel: CallViewModel,
    currentUserId: String,
    onDismiss: () -> Unit
) {
    val otherUser by chatViewModel.activeOtherUser.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val typedText by chatViewModel.typedMessage.collectAsState()
    val replyTo by chatViewModel.replyTo.collectAsState()
    val isSending by chatViewModel.isSending.collectAsState()
    val statusNotice by chatViewModel.statusNotice.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("Harassment") }
    var reportDetails by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<DirectMessage?>(null) }

    // Display status notice if any
    LaunchedEffect(statusNotice) {
        statusNotice?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearNotice()
        }
    }

    // Auto-scroll to latest message when sent
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    if (otherUser == null) return

    ModalBottomSheet(
        onDismissRequest = {
            chatViewModel.closeActiveConversation()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Chat Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                chatViewModel.closeActiveConversation()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }

                        // Avatar with VIP frame
                        ProfileFrameComponent(
                            frameId = otherUser!!.profileFrameId,
                            avatarUrl = otherUser!!.avatar,
                            size = 46.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = otherUser!!.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (otherUser!!.vipLevel > 0) {
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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ID: ${otherUser!!.publicUserId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealPremium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("•", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (otherUser!!.isOnline) "🟢 Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (otherUser!!.isOnline) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Action buttons: 1-Tap Voice Call & Options Menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                callViewModel.initiateCall(otherUser!!)
                            },
                            modifier = Modifier.testTag("chat_voice_call_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Voice Call",
                                tint = TealPremium
                            )
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Block User", color = Color(0xFFFF5252)) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF5252)) },
                                    onClick = {
                                        showMenu = false
                                        showBlockDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report User / Message") },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showReportDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            // Message List
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("👋", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Say hello to ${otherUser!!.displayName}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Messages are synchronized in real-time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("chat_message_list")
                    ) {
                        items(messages, key = { it.messageId }) { msg ->
                            val isFromMe = msg.senderUid == currentUserId
                            DirectMessageBubble(
                                message = msg,
                                isFromMe = isFromMe,
                                onLongClick = {
                                    selectedMessageForAction = msg
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Reply banner if replying to a specific message
            AnimatedVisibility(visible = replyTo != null) {
                replyTo?.let { replyMsg ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${if (replyMsg.senderUid == currentUserId) "yourself" else otherUser!!.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TealPremium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = replyMsg.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { chatViewModel.setReplyTo(null) }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Emojis
                    IconButton(onClick = { chatViewModel.updateTypedMessage(typedText + " 🌹") }) {
                        Text("🌹", fontSize = 20.sp)
                    }

                    OutlinedTextField(
                        value = typedText,
                        onValueChange = { chatViewModel.updateTypedMessage(it) },
                        placeholder = { Text("Write a message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPremium,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { chatViewModel.sendMessage() },
                        enabled = typedText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (typedText.isNotBlank()) TealPremium else Color.Gray.copy(alpha = 0.3f))
                            .testTag("chat_send_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (typedText.isNotBlank()) Color.Black else Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Message action options dialog (Reply, Copy, Delete)
    if (selectedMessageForAction != null) {
        val msg = selectedMessageForAction!!
        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("Message Options") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            chatViewModel.setReplyTo(msg)
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Reply, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Reply")
                        }
                    }

                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Copy Text")
                        }
                    }

                    if (msg.senderUid == currentUserId && !msg.deleted) {
                        TextButton(
                            onClick = {
                                chatViewModel.deleteMessage(msg.messageId)
                                selectedMessageForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Delete for Everyone", color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block Confirmation Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${otherUser!!.displayName}?") },
            text = {
                Text("They will no longer be able to send you private messages or initiate voice calls.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.blockCurrentChatUser()
                        showBlockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Block", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Dialog
    if (showReportDialog) {
        val reasons = listOf("Harassment", "Spam", "Scam or Fraud", "Inappropriate Content", "Underage")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report User") },
            text = {
                Column {
                    Text("Select a violation reason:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = reportReason == reason,
                                onClick = { reportReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportDetails,
                        onValueChange = { reportDetails = it },
                        placeholder = { Text("Provide details (optional)...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        chatViewModel.reportCurrentChat(reportReason, reportDetails)
                        showReportDialog = false
                        reportDetails = ""
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectMessageBubble(
    message: DirectMessage,
    isFromMe: Boolean,
    onLongClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.createdAt) { timeFormat.format(Date(message.createdAt)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            ),
            color = if (isFromMe) {
                if (message.deleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else TealPremium
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .widthIn(max = 290.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.deleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "This message was deleted",
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Text(
                        text = message.text,
                        color = if (isFromMe) Color.Black else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        color = if (isFromMe) Color.Black.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        fontSize = 9.sp
                    )

                    if (isFromMe && !message.deleted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val tickText = when {
                            message.isRead -> "✓✓"
                            message.status == "delivered" -> "✓✓"
                            message.status == "sent" -> "✓"
                            else -> "🕒"
                        }
                        val tickColor = if (message.isRead) Color(0xFF004D40) else Color.Black.copy(alpha = 0.65f)

                        Text(
                            text = tickText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = tickColor
                        )
                    }
                }
            }
        }
    }
}
