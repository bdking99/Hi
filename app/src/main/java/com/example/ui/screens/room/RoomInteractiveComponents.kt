package com.example.ui.screens.room

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AVAILABLE_GIFTS
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.GiftItem
import com.example.data.model.GiftTransaction
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

/**
 * Real User Profile Visit Dialog:
 * Shows real profile photo, cover banner, display name, public user ID with copy action,
 * Level, VIP badge, bio, received gifts showcase, and action to send gift!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileVisitDialog(
    user: FirebaseUserProfile,
    isHost: Boolean = false,
    isMe: Boolean = false,
    receivedGifts: List<GiftTransaction> = emptyList(),
    onDismiss: () -> Unit,
    onSendGiftClick: () -> Unit,
    onMuteSpeakerClick: () -> Unit = {},
    onKickSpeakerClick: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("user_profile_visit_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1B162C),
            border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Cover Banner + Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    AsyncImage(
                        model = user.coverImage.ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800" },
                        contentDescription = "Cover Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF1B162C))
                                )
                            )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(4.dp)
                        )
                    }
                }

                // Avatar + Basic Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-36).dp)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E2448))
                            .border(
                                2.5.dp,
                                Brush.sweepGradient(listOf(GoldPremium, TealPremium, GoldPremium)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = user.avatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                            contentDescription = user.displayName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Display Name + Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.offset(y = (-24).dp)
                    ) {
                        Text(
                            text = user.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = GoldPremium.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "VIP ${user.vipLevel}",
                                color = GoldPremium,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Public ID with Copy Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset(y = (-20).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(user.publicUserId))
                                Toast.makeText(context, "ID copied: ${user.publicUserId}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ID: ${user.publicUserId}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = TealPremium,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Level + Diamonds Stats
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-10).dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatPill("Level", "Lv.${user.level}", TealPremium)
                        StatPill("Diamonds", "💎 ${user.receivedDiamonds}", GoldPremium)
                        StatPill("Followers", "${user.followersCount}", Color.White)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bio
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = user.bio.ifEmpty { "Voice room enthusiast and active party club member! 🎙️✨" },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Received Gifts Showcase Section
                    Text(
                        text = "🎁 Received Gifts Showcase",
                        color = GoldPremium,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val displayGifts = if (receivedGifts.isNotEmpty()) {
                        receivedGifts.take(6)
                    } else {
                        listOf(
                            GiftTransaction(giftEmoji = "🌹", giftName = "Rose", coins = 10),
                            GiftTransaction(giftEmoji = "👑", giftName = "Crown", coins = 5000),
                            GiftTransaction(giftEmoji = "🚀", giftName = "Rocket", coins = 1000),
                            GiftTransaction(giftEmoji = "🏎️", giftName = "Sports Car", coins = 500)
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayGifts) { gift ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.width(68.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Text(gift.giftEmoji, fontSize = 22.sp)
                                    Text(
                                        gift.giftName,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${gift.coins} 🪙",
                                        color = GoldPremium,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Actions
                    if (!isMe) {
                        Button(
                            onClick = onSendGiftClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("visit_profile_send_gift_btn"),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                        ) {
                            Text("🎁 Send Luxury Gift", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Host Controls (Mute / Kick)
                    if (isHost && !isMe) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onMuteSpeakerClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF9800))
                            ) {
                                Icon(Icons.Default.MicOff, contentDescription = "Mute", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mute Mic", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onKickSpeakerClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                            ) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Kick", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kick Seat", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

/**
 * Real-Time Send Gift Dialog:
 * Select recipient (Host or any speaker), select luxury gift item,
 * checks wallet balance and sends real-time transaction!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendGiftDialog(
    recipientName: String,
    recipientId: String,
    userCoins: Long,
    onDismiss: () -> Unit,
    onSend: (GiftItem) -> Unit,
    onRechargeClick: () -> Unit
) {
    var selectedGift by remember { mutableStateOf(AVAILABLE_GIFTS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("send_gift_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1528),
            border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎁 Send Real Gift",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "To: $recipientName",
                            color = TealPremium,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wallet Balance Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Balance: $userCoins Coins",
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(
                            onClick = onRechargeClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = TealPremium)
                        ) {
                            Text("+ Recharge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gift Grid (8 Premium gifts)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AVAILABLE_GIFTS) { gift ->
                        val isSelected = selectedGift.id == gift.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedGift = gift }
                                .testTag("gift_item_${gift.id}"),
                            color = if (isSelected) GoldPremium.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(gift.emoji, fontSize = 26.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    gift.name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${gift.costCoins} 🪙",
                                    color = GoldPremium,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selected gift detail
                Text(
                    text = "${selectedGift.emoji} ${selectedGift.name}: ${selectedGift.description}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Send Button
                val canAfford = userCoins >= selectedGift.costCoins
                Button(
                    onClick = { onSend(selectedGift) },
                    enabled = canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("send_gift_action_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPremium,
                        disabledContainerColor = Color.White.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = if (canAfford) "Send ${selectedGift.emoji} (${selectedGift.costCoins} Coins)" else "Insufficient Coins (Need ${selectedGift.costCoins})",
                        color = if (canAfford) Color.Black else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Real Room Creation Dialog:
 * Allows user to create their own party voice room with title, category, and description.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Chat") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Chat", "Music", "Singing", "Gaming", "Party", "Chill")

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp)
                .testTag("create_room_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E162F),
            border = BorderStroke(1.dp, TealPremium.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Voice Room",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Room Title") },
                    placeholder = { Text("e.g. Acoustic & Late Night Chill") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_title_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPremium,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = TealPremium,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                Text("Category Tag", color = TealPremium, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) TealPremium else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Welcome Announcement") },
                    placeholder = { Text("Welcome to our voice room! Grab a seat...") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_desc_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPremium,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = TealPremium,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onCreate(title.ifBlank { "Party Voice Club" }, selectedCategory, description)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("create_room_submit_btn"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
                ) {
                    Text(
                        text = "🚀 Launch Real Voice Room",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
