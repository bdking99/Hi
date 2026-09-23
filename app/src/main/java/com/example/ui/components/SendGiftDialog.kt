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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CatalogGift
import com.example.data.model.GiftSendResult
import com.example.data.repository.WalletRepository
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendGiftDialog(
    senderUid: String,
    recipientUid: String,
    recipientPublicId: String,
    recipientName: String,
    recipientAvatar: String,
    roomId: String? = null,
    roomTitle: String? = null,
    walletRepository: WalletRepository = remember { WalletRepository() },
    onDismiss: () -> Unit,
    onGiftSent: (CatalogGift, Int) -> Unit = { _, _ -> },
    onNavigateToRecharge: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Real-time Gifts Catalog from Firestore
    val giftsCatalog by walletRepository.getGiftsCatalogStream().collectAsState(initial = walletRepository.getDefaultGifts())
    
    // 2. Real-time Sender Coin Balance from Firestore
    val senderWallet by walletRepository.getWalletStream(senderUid).collectAsState(initial = null)
    val availableCoins = senderWallet?.coinBalance ?: 0L

    var selectedGift by remember { mutableStateOf<CatalogGift?>(null) }
    var selectedQuantity by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var isSending by remember { mutableStateOf(false) }

    val categories = remember(giftsCatalog) {
        listOf("ALL") + giftsCatalog.map { it.category }.distinct()
    }

    val filteredGifts = remember(giftsCatalog, selectedCategory) {
        if (selectedCategory == "ALL") giftsCatalog
        else giftsCatalog.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LaunchedEffect(filteredGifts) {
        if (selectedGift == null && filteredGifts.isNotEmpty()) {
            selectedGift = filteredGifts.first()
        }
    }

    val quantities = listOf(1, 5, 10, 99, 520, 1314)
    val totalCost = (selectedGift?.coinPrice ?: 0L) * selectedQuantity

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("send_gift_dialog"),
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
                // Header: Title & Recipient Pill & Close
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "To: $recipientName (ID: $recipientPublicId)",
                                color = GoldPremium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { if (!isSending) onDismiss() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Tabs Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory.equals(category, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .clickable { selectedCategory = category }
                                .testTag("gift_category_$category")
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gifts Grid
                if (filteredGifts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No gifts available in this category.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredGifts) { gift ->
                            val isSelected = selectedGift?.giftId == gift.giftId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedGift = gift }
                                    .testTag("gift_item_${gift.giftId}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF381B66) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF5A2A9D).copy(alpha = 0.6f), Color.Transparent)
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (gift.imageUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = gift.imageUrl,
                                                contentDescription = gift.name,
                                                modifier = Modifier.size(44.dp)
                                            )
                                        } else {
                                            Text(gift.emoji, fontSize = 32.sp)
                                        }

                                        if (gift.isFullScreenEffect) {
                                            Surface(
                                                modifier = Modifier.align(Alignment.TopEnd),
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFF1744)
                                            ) {
                                                Text(
                                                    text = "FX",
                                                    color = Color.White,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = gift.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("🪙", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "%,d".format(gift.coinPrice),
                                            color = GoldPremium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quantity Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Qty:", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    quantities.forEach { qty ->
                        val isSelected = selectedQuantity == qty
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TealPremium else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .clickable { selectedQuantity = qty }
                                .testTag("gift_qty_$qty")
                        ) {
                            Text(
                                text = "x$qty",
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Balance & Send Button Bar
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
                        // User Balance
                        Column {
                            Text("My Balance", color = Color.LightGray, fontSize = 11.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToRecharge() }
                            ) {
                                Text("🪙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%,d".format(availableCoins),
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Recharge",
                                    tint = GoldPremium,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Send Button
                        val hasEnough = availableCoins >= totalCost
                        Button(
                            onClick = {
                                val targetGift = selectedGift ?: return@Button
                                if (!hasEnough) {
                                    Toast.makeText(context, "Insufficient coins! Please recharge.", Toast.LENGTH_SHORT).show()
                                    onNavigateToRecharge()
                                    return@Button
                                }

                                isSending = true
                                coroutineScope.launch {
                                    val opId = UUID.randomUUID().toString()
                                    val result = walletRepository.sendGiftAtomic(
                                        senderUid = senderUid,
                                        recipientUid = recipientUid,
                                        giftId = targetGift.giftId,
                                        quantity = selectedQuantity,
                                        roomId = roomId,
                                        roomTitle = roomTitle,
                                        operationId = opId
                                    )

                                    isSending = false
                                    when (result) {
                                        is GiftSendResult.Success -> {
                                            Toast.makeText(
                                                context,
                                                "🎉 Sent ${targetGift.emoji} ${targetGift.name} x$selectedQuantity to $recipientName!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onGiftSent(targetGift, selectedQuantity)
                                            onDismiss()
                                        }
                                        is GiftSendResult.Failure -> {
                                            Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            enabled = !isSending && selectedGift != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasEnough) GoldPremium else Color(0xFFFF5252)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("send_gift_action_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (hasEnough) "Send (%,d 🪙)".format(totalCost) else "Recharge Coins",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
