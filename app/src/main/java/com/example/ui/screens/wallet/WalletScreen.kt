package com.example.ui.screens.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoinTransactionItem
import com.example.data.repository.RoomRepository
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RechargePack(
    val id: String,
    val coins: Long,
    val bonusCoins: Long,
    val priceUsd: String,
    val popularBadge: String? = null
)

val RECHARGE_PACKS = listOf(
    RechargePack("pack_1", 100, 0, "$0.99"),
    RechargePack("pack_2", 500, 50, "$4.99", "POPULAR"),
    RechargePack("pack_3", 1200, 200, "$9.99", "BEST VALUE"),
    RechargePack("pack_4", 5000, 1000, "$39.99", "VIP SPECIAL"),
    RechargePack("pack_5", 10000, 2500, "$79.99", "KING PACK")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit,
    roomRepository: RoomRepository = remember { RoomRepository() },
    userId: String = "USER_ME_CURRENT"
) {
    val coroutineScope = rememberCoroutineScope()
    var coinBalance by remember { mutableLongStateOf(5000L) }
    var transactions by remember { mutableStateOf<List<CoinTransactionItem>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recharge, 1: History
    var historyFilter by remember { mutableStateOf("ALL") } // "ALL", "RECHARGE", "GIFT_SENT", "GIFT_RECEIVED"
    var selectedPack by remember { mutableStateOf(RECHARGE_PACKS[1]) }
    var rechargeNotice by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        // Collect real-time balance
        roomRepository.getUserProfileStream(userId).collect { profile ->
            if (profile != null) {
                coinBalance = profile.coinBalance
            }
        }
    }

    LaunchedEffect(userId) {
        // Collect transaction history
        roomRepository.getUserTransactions(userId).collect { items ->
            transactions = items
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "💎 Coin Wallet & Recharge",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF141124))
            )
        },
        containerColor = Color(0xFF141124)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Luxury Gold Wallet Balance Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("wallet_balance_card"),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2C194D), Color(0xFF180E2B), Color(0xFF382313))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            text = "AVAILABLE COIN BALANCE",
                            color = GoldPremium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "%,d".format(coinBalance),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coins", color = GoldPremium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPremium.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "VIP WALLET",
                            color = GoldPremium,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Notice Banner
            if (rechargeNotice != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = TealPremium.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, TealPremium)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(rechargeNotice!!, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        IconButton(onClick = { rechargeNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tab Bar: Recharge vs History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1A33),
                contentColor = GoldPremium,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPremium
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Instant Recharge", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Transaction History", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // RECHARGE SECTION
                Text(
                    text = "Select Coin Package",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(RECHARGE_PACKS) { pack ->
                        val isSelected = selectedPack.id == pack.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedPack = pack }
                                .testTag("recharge_pack_${pack.id}"),
                            color = if (isSelected) GoldPremium.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🪙", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "%,d Coins".format(pack.coins),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            if (pack.bonusCoins > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = TealPremium.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "+${pack.bonusCoins} Bonus",
                                                        color = TealPremium,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (pack.popularBadge != null) {
                                            Text(
                                                text = pack.popularBadge,
                                                color = GoldPremium,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = pack.priceUsd,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Instant Recharge Button
                Button(
                    onClick = {
                        isProcessing = true
                        coroutineScope.launch {
                            val totalToAdd = selectedPack.coins + selectedPack.bonusCoins
                            val res = roomRepository.rechargeCoins(userId, totalToAdd, "${selectedPack.coins} Coins Package")
                            res.onSuccess { newBal ->
                                coinBalance = newBal
                                rechargeNotice = "🎉 Successfully added $totalToAdd Coins! New Balance: %,d Coins".format(newBal)
                            }.onFailure { err ->
                                rechargeNotice = "Recharge Error: ${err.message}"
                            }
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("recharge_instant_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Recharge ${selectedPack.coins + selectedPack.bonusCoins} Coins for ${selectedPack.priceUsd}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

            } else {
                // TRANSACTION HISTORY SECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All", "RECHARGE" to "Recharge", "GIFT_SENT" to "Gifts Sent", "GIFT_RECEIVED" to "Received").forEach { (key, label) ->
                        val isSelected = historyFilter == key
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.clickable { historyFilter = key }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val filtered = transactions.filter {
                    if (historyFilter == "ALL") true else it.type == historyFilter
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found in this category",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { tx ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.04f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = tx.title.ifEmpty { tx.type },
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = tx.description.ifEmpty { dateFormat.format(Date(tx.timestamp)) },
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    val isPositive = tx.amount > 0
                                    Text(
                                        text = "${if (isPositive) "+" else ""}${tx.amount} 🪙",
                                        color = if (isPositive) TealPremium else Color(0xFFFF5252),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
