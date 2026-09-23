package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FramePurchaseResult
import com.example.data.model.ProfileFrame
import com.example.data.model.ProfileFrameItem
import com.example.data.repository.WalletRepository
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFrameStoreDialog(
    currentUserId: String,
    currentUserAvatar: String,
    currentEquippedFrameId: String,
    walletRepository: WalletRepository = remember { WalletRepository() },
    onDismiss: () -> Unit,
    onEquippedChanged: (String) -> Unit = {},
    onNavigateToRecharge: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Catalog of frames from Firestore
    val framesCatalog by walletRepository.getProfileFramesCatalogStream().collectAsState(initial = walletRepository.getDefaultFrames())

    // 2. User's owned frames from Firestore
    val ownedFrames by walletRepository.getUserOwnedFramesStream(currentUserId).collectAsState(initial = emptyList())
    val ownedFrameIds = remember(ownedFrames) { ownedFrames.map { it.frameId }.toSet() }

    // 3. User's real wallet balance
    val userWallet by walletRepository.getWalletStream(currentUserId).collectAsState(initial = null)
    val availableCoins = userWallet?.coinBalance ?: 0L

    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedFrame by remember { mutableStateOf<ProfileFrameItem?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val categories = remember(framesCatalog) {
        listOf("ALL") + framesCatalog.map { it.type }.distinct()
    }

    val filteredFrames = remember(framesCatalog, selectedCategory) {
        if (selectedCategory == "ALL") framesCatalog
        else framesCatalog.filter { it.type.equals(selectedCategory, ignoreCase = true) }
    }

    LaunchedEffect(filteredFrames, currentEquippedFrameId) {
        if (selectedFrame == null) {
            selectedFrame = framesCatalog.find { it.frameId == currentEquippedFrameId } ?: filteredFrames.firstOrNull()
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("profile_frame_store_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF130924),
            border = BorderStroke(1.dp, Color(0xFF321759)),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "👑 VIP Profile Frame Store",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Unlock luxury avatars & aura glow effects",
                            color = GoldPremium,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { if (!isProcessing) onDismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Live Frame Avatar Preview
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Live Avatar with Animated Frame
                        val previewFrameModel = remember(selectedFrame) {
                            selectedFrame?.let {
                                ProfileFrame(
                                    id = it.frameId,
                                    name = it.name,
                                    imageUrl = it.imageUrl,
                                    type = it.type,
                                    rarity = it.rarity,
                                    glowColorHex = it.glowColorHex,
                                    secondaryColorHex = it.secondaryColorHex
                                )
                            }
                        }

                        ProfileAvatarWithFrame(
                            avatarUrl = currentUserAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                            frame = previewFrameModel,
                            size = 80.dp,
                            isOnline = true
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFrame?.name ?: "Select a Frame",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = selectedFrame?.description ?: "",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = GoldPremium.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = selectedFrame?.rarity ?: "Standard",
                                        color = GoldPremium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🪙 %,d Coins".format(selectedFrame?.requiredCoins ?: 0L),
                                    color = TealPremium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.08f),
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

                Spacer(modifier = Modifier.height(10.dp))

                // Frame Catalog Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFrames) { frame ->
                        val isSelected = selectedFrame?.frameId == frame.frameId
                        val isOwned = ownedFrameIds.contains(frame.frameId)
                        val isEquipped = currentEquippedFrameId == frame.frameId

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedFrame = frame }
                                .testTag("frame_item_${frame.frameId}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF381B66) else Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.08f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Frame Visual Badge
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProfileAvatarWithFrame(
                                        avatarUrl = currentUserAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                                        frame = ProfileFrame(
                                            id = frame.frameId,
                                            name = frame.name,
                                            glowColorHex = frame.glowColorHex,
                                            secondaryColorHex = frame.secondaryColorHex
                                        ),
                                        size = 54.dp
                                    )
                                    if (isEquipped) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd),
                                            shape = CircleShape,
                                            color = TealPremium
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = frame.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isOwned) "Owned" else "🪙 %,d".format(frame.requiredCoins),
                                    color = if (isOwned) TealPremium else GoldPremium,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Button: Buy / Equip / Unequip
                val activeTargetFrame = selectedFrame
                if (activeTargetFrame != null) {
                    val isOwned = ownedFrameIds.contains(activeTargetFrame.frameId)
                    val isEquipped = currentEquippedFrameId == activeTargetFrame.frameId
                    val hasEnoughCoins = availableCoins >= activeTargetFrame.requiredCoins

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Your Wallet Balance", color = Color.LightGray, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🪙", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "%,d".format(availableCoins),
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (isEquipped) {
                                        // Unequip
                                        isProcessing = true
                                        coroutineScope.launch {
                                            walletRepository.unequipFrame(currentUserId)
                                            onEquippedChanged("frame_default")
                                            Toast.makeText(context, "Unequipped profile frame.", Toast.LENGTH_SHORT).show()
                                            isProcessing = false
                                        }
                                    } else if (isOwned) {
                                        // Equip
                                        isProcessing = true
                                        coroutineScope.launch {
                                            walletRepository.equipFrame(currentUserId, activeTargetFrame.frameId)
                                            onEquippedChanged(activeTargetFrame.frameId)
                                            Toast.makeText(context, "Equipped ${activeTargetFrame.name}!", Toast.LENGTH_SHORT).show()
                                            isProcessing = false
                                        }
                                    } else {
                                        // Purchase & Equip
                                        if (!hasEnoughCoins) {
                                            Toast.makeText(context, "Insufficient coins! Please recharge.", Toast.LENGTH_SHORT).show()
                                            onNavigateToRecharge()
                                            return@Button
                                        }
                                        isProcessing = true
                                        coroutineScope.launch {
                                            val res = walletRepository.purchaseFrameAtomic(
                                                uid = currentUserId,
                                                frameId = activeTargetFrame.frameId,
                                                operationId = UUID.randomUUID().toString()
                                            )
                                            isProcessing = false
                                            when (res) {
                                                is FramePurchaseResult.Success -> {
                                                    Toast.makeText(context, "🎉 Purchased & equipped ${res.frame.name}!", Toast.LENGTH_LONG).show()
                                                    onEquippedChanged(res.frame.frameId)
                                                }
                                                is FramePurchaseResult.Failure -> {
                                                    Toast.makeText(context, res.errorMessage, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isEquipped -> Color(0xFFFF5252)
                                        isOwned -> TealPremium
                                        hasEnoughCoins -> GoldPremium
                                        else -> Color(0xFFFF9800)
                                    }
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("frame_store_action_button")
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    val buttonText = when {
                                        isEquipped -> "Unequip Frame"
                                        isOwned -> "Equip Frame"
                                        hasEnoughCoins -> "Buy Frame (%,d 🪙)".format(activeTargetFrame.requiredCoins)
                                        else -> "Recharge Coins"
                                    }
                                    Text(buttonText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
