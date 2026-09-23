package com.example.ui.screens.wallet

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.RoomRepository
import com.example.data.repository.WalletRepository
import com.example.ui.components.ProfileAvatarWithFrame
import com.example.ui.components.ProfileFrameStoreDialog
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Default packages with official Bangladeshi Taka (BDT) rates
val DEFAULT_COIN_PACKS = listOf(
    CoinPackage("pack_100", 100L, 0L, 10.0, "BDT", true, 1, null),
    CoinPackage("pack_550", 500L, 50L, 50.0, "BDT", true, 2, "POPULAR"),
    CoinPackage("pack_1200", 1000L, 200L, 100.0, "BDT", true, 3, "BEST VALUE"),
    CoinPackage("pack_6500", 5500L, 1000L, 500.0, "BDT", true, 4, "VIP FAVORITE"),
    CoinPackage("pack_14000", 12000L, 2000L, 1000.0, "BDT", true, 5, "KING PACK"),
    CoinPackage("pack_75000", 65000L, 10000L, 5000.0, "BDT", true, 6, "ROYAL SUPREME")
)

private val BkashPink = Color(0xFFE2136E)
private val NagadOrange = Color(0xFFF7931E)
private val RocketPurple = Color(0xFF8C3494)
private val UpayCyan = Color(0xFF0099DA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBackClick: () -> Unit,
    roomRepository: RoomRepository = remember { RoomRepository() },
    walletRepository: WalletRepository = remember { WalletRepository() },
    userId: String = "USER_ME_CURRENT"
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val effectiveUserId = remember(userId) {
        if (userId.isNotBlank() && userId != "USER_ME_CURRENT") userId
        else FirebaseAuth.getInstance().currentUser?.uid ?: "USER_ME_CURRENT"
    }

    // 1. Real-time authoritative wallet from Firestore
    val userWallet by walletRepository.getWalletStream(effectiveUserId).collectAsState(initial = null)
    val coinBalance = userWallet?.coinBalance ?: 2500L
    val lifetimePurchased = userWallet?.lifetimePurchasedCoins ?: 0L
    val lifetimeSpent = userWallet?.lifetimeSpentCoins ?: 0L
    val lifetimeReceived = userWallet?.lifetimeReceivedCoins ?: 0L
    val lifetimeGiftVal = userWallet?.lifetimeGiftValue ?: 0L
    val publicId = userWallet?.publicUserId ?: effectiveUserId.take(7)

    // 2. Real-time dynamic payment configuration & coin packages from Firestore
    val paymentConfig by walletRepository.getPaymentSystemConfigStream().collectAsState(initial = PaymentSystemConfig())
    val streamedPacks by walletRepository.getCoinPackagesStream().collectAsState(initial = emptyList())
    val coinPackages = if (streamedPacks.isNotEmpty()) streamedPacks else DEFAULT_COIN_PACKS

    // 3. User purchases and transactions
    var historyFilter by remember { mutableStateOf("ALL") }
    val transactions by walletRepository.getWalletTransactionsStream(effectiveUserId, historyFilter).collectAsState(initial = emptyList())
    val userPurchases by walletRepository.getUserPurchaseRequestsStream(effectiveUserId).collectAsState(initial = emptyList())
    val giftsSent by walletRepository.getGiftsSentStream(effectiveUserId).collectAsState(initial = emptyList())
    val giftsReceived by walletRepository.getGiftsReceivedStream(effectiveUserId).collectAsState(initial = emptyList())
    val ownedFrames by walletRepository.getUserOwnedFramesStream(effectiveUserId).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recharge, 1: My Purchases, 2: Cashout, 3: Ledger, 4: Gifts Sent, 5: Frames
    var selectedPack by remember { mutableStateOf(coinPackages[1]) }
    var rechargeNotice by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showFrameStore by remember { mutableStateOf(false) }

    // Dialog states
    var showBanglaCheckoutDialog by remember { mutableStateOf(false) }
    var showExchangeDialog by remember { mutableStateOf(false) }
    var showCashoutDialog by remember { mutableStateOf(false) }

    val tabTitles = listOf("Recharge", "Purchases (${userPurchases.size})", "Cashout", "Ledger", "Sent", "Frames")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🪙 Real Coin Wallet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showFrameStore = true }) {
                        Icon(Icons.Default.Star, contentDescription = "Frame Store", tint = GoldPremium)
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
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Luxury Gold Wallet Balance Card with Real-Time Stats
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .testTag("wallet_balance_card"),
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, GoldPremium.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF2A2010),
                                    Color(0xFF1B1530)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Available Balance",
                                    color = Color.LightGray.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🪙 %,d".format(coinBalance),
                                        color = GoldPremium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Coins", color = GoldPremium, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick Action Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { showExchangeDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Exchange", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Mini Stat Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniItem("User ID", publicId, GoldPremium)
                            StatMiniItem("Purchased", "%,d".format(lifetimePurchased), Color(0xFF81C784))
                            StatMiniItem("Spent", "%,d".format(lifetimeSpent), Color(0xFFFF8A80))
                            StatMiniItem("Earned", "%,d 💎".format(lifetimeGiftVal), TealPremium)
                        }
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
                        Text(rechargeNotice!!, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { rechargeNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tab Bar: Scrollable
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1A33),
                contentColor = GoldPremium,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = GoldPremium
                        )
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: RECHARGE COIN PACKAGES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Coin Top-Up Package",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "bKash • Nagad • Rocket",
                            color = TealPremium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(coinPackages, key = { it.packageId }) { pack ->
                            val isSelected = selectedPack.packageId == pack.packageId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedPack = pack }
                                    .testTag("recharge_pack_${pack.packageId}"),
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
                                                    text = "%,d Coins".format(pack.coinAmount),
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
                                            text = "৳%.0f BDT".format(pack.priceBDT),
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

                    // Purchase Button - Opens Bangla Payment Checkout
                    Button(
                        onClick = {
                            showBanglaCheckoutDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("recharge_instant_button"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                    ) {
                        Text(
                            text = "Purchase ${selectedPack.coinAmount + selectedPack.bonusCoins} Coins for ৳%.0f".format(selectedPack.priceBDT),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                1 -> {
                    // TAB 1: USER'S SUBMITTED RECHARGE REQUESTS & STATUSES
                    if (userPurchases.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🧾", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No purchase requests yet", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Your top-up requests and verification status will appear here.", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userPurchases, key = { it.requestId }) { req ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1A35)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${req.paymentMethod} • ৳%.0f".format(req.priceBDT),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                            val badgeColor = when (req.status) {
                                                "APPROVED" -> Color(0xFF10B981)
                                                "REJECTED" -> Color(0xFFEF4444)
                                                else -> Color(0xFFFFB300)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeColor.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = req.status,
                                                    color = badgeColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "TrxID: ${req.paymentReference}",
                                            color = TealPremium,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Coins: %,d (+%,d Bonus)".format(req.coinAmount, req.bonusCoins),
                                            color = GoldPremium,
                                            fontSize = 12.sp
                                        )

                                        if (!req.rejectReason.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Rejection Reason: ${req.rejectReason}",
                                                color = Color(0xFFFF8A80),
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(req.createdAt)),
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: CASHOUT & DIAMOND EXCHANGE
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1630)),
                            border = BorderStroke(1.dp, TealPremium.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("💎 Host Earnings & Diamonds", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("Gifts received during voice chat generate diamonds that can be cashed out or converted.", color = Color.LightGray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Available Diamonds", color = Color.Gray, fontSize = 11.sp)
                                        Text("%,d 💎".format(lifetimeGiftVal), color = TealPremium, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Estimated Value", color = Color.Gray, fontSize = 11.sp)
                                        Text("৳%.2f BDT".format(lifetimeGiftVal * paymentConfig.coinExchangeRateBDT), color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { showExchangeDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
                                    ) {
                                        Text("Exchange to Coins", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { showCashoutDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                                    ) {
                                        Text("Request Cashout", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Cashout Rules & Processing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF140F24),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("• Minimum cashout: 50,000 Diamonds", color = Color.LightGray, fontSize = 12.sp)
                                Text("• Supported payout methods: bKash, Nagad, Bank", color = Color.LightGray, fontSize = 12.sp)
                                Text("• Processing time: 24 - 48 business hours with verified audit trace", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: IMMUTABLE TRANSACTION LEDGER
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All",
                            "COIN_RECHARGE" to "Recharges",
                            "GIFT_SENT" to "Gifts Sent",
                            "GIFT_RECEIVED" to "Received",
                            "FRAME_PURCHASE" to "Frames"
                        ).forEach { (key, label) ->
                            val isSelected = historyFilter == key
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) GoldPremium else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.clickable { historyFilter = key }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No transactions recorded", color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions, key = { it.transactionId }) { tx ->
                                val isCredit = tx.type in listOf("COIN_RECHARGE", "GIFT_RECEIVED", "ADMIN_CREDIT")
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1933)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(tx.description, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(
                                                SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.createdAt)),
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = (if (isCredit) "+" else "-") + "%,d 🪙".format(tx.amount),
                                            color = if (isCredit) Color(0xFF10B981) else Color(0xFFEF4444),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: GIFTS SENT
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(giftsSent) { gift ->
                            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1933))) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Sent ${gift.giftName} to Room ${gift.roomId?.take(6) ?: "Global"}", color = Color.White, fontSize = 13.sp)
                                    Text("-%,d 🪙".format(gift.coinPrice * gift.quantity), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // TAB 5: OWNED FRAMES
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ownedFrames) { owned ->
                            Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1933))) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(owned.frameId, color = Color.White, fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                walletRepository.equipFrame(effectiveUserId, owned.frameId)
                                                Toast.makeText(context, "Frame equipped!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (owned.isEquipped) TealPremium else GoldPremium),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (owned.isEquipped) "Equipped" else "Equip", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // 🇧🇩 BANGLA PAYMENT CHECKOUT DIALOG
    // ==========================================
    if (showBanglaCheckoutDialog) {
        var selectedMethod by remember { mutableStateOf("bKash") }
        var senderPhone by remember { mutableStateOf("") }
        var transactionIdInput by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var checkoutError by remember { mutableStateOf<String?>(null) }

        val activeTargetNumber = when (selectedMethod) {
            "Nagad" -> paymentConfig.nagadNumber
            "Rocket" -> paymentConfig.rocketNumber
            "Upay" -> paymentConfig.upayNumber
            else -> paymentConfig.bkashNumber
        }

        val activeColor = when (selectedMethod) {
            "Nagad" -> NagadOrange
            "Rocket" -> RocketPurple
            "Upay" -> UpayCyan
            else -> BkashPink
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) showBanglaCheckoutDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🇧🇩 Top-Up Checkout", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Package Summary Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPremium.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "%,d Coins".format(selectedPack.coinAmount + selectedPack.bonusCoins),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text("Instant Credit upon verification", color = Color.LightGray, fontSize = 11.sp)
                            }
                            Text(
                                text = "৳%.0f BDT".format(selectedPack.priceBDT),
                                color = GoldPremium,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Payment Gateway:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Gateway Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("bKash" to BkashPink, "Nagad" to NagadOrange, "Rocket" to RocketPurple, "Upay" to UpayCyan).forEach { (method, color) ->
                            val isSelected = selectedMethod == method
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) color.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color.Gray.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMethod = method }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = method,
                                        color = if (isSelected) color else Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Instructions Card with ONE-TAP COPY (Requirement 8)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF130E26)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Official $selectedMethod Number:", color = Color.Gray, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeTargetNumber,
                                    color = activeColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(activeTargetNumber))
                                        Toast.makeText(context, "$selectedMethod number copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Your Reference ID:", color = Color.Gray, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = publicId, color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(publicId))
                                        Toast.makeText(context, "User ID copied!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E244D)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp), tint = GoldPremium)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", color = GoldPremium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "1. $selectedMethod অ্যাপে 'Send Money' করুন।\n2. রেফারেন্সে আপনার User ID দিন।\n3. পেমেন্টের পর প্রাপ্ত TrxID এবং প্রেরক নম্বর নিচে লিখে সাবমিট করুন।",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input: Sender Phone Number
                    OutlinedTextField(
                        value = senderPhone,
                        onValueChange = { senderPhone = it },
                        label = { Text("Sender Mobile Number (প্রেরক নম্বর)", color = Color.Gray) },
                        placeholder = { Text("017xxxxxxxx", color = Color.DarkGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color(0xFF382E5C)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input: Transaction ID (TrxID)
                    OutlinedTextField(
                        value = transactionIdInput,
                        onValueChange = { transactionIdInput = it.uppercase().trim() },
                        label = { Text("Transaction ID (TrxID)", color = Color.Gray) },
                        placeholder = { Text("e.g. 9J82KS1A", color = Color.DarkGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = activeColor,
                            unfocusedBorderColor = Color(0xFF382E5C)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (checkoutError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(checkoutError!!, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        checkoutError = null
                        if (senderPhone.isBlank() || senderPhone.length < 10) {
                            checkoutError = "অনুগ্রহ করে সঠিক প্রেরক মোবাইল নম্বর দিন।"
                            return@Button
                        }
                        if (transactionIdInput.isBlank() || transactionIdInput.length < 5) {
                            checkoutError = "সঠিক Transaction ID (TrxID) প্রদান করুন (কমপক্ষে ৫ অক্ষর)।"
                            return@Button
                        }

                        isSubmitting = true
                        coroutineScope.launch {
                            val res = walletRepository.submitCoinPurchaseRequest(
                                uid = effectiveUserId,
                                publicUserId = publicId,
                                packageId = selectedPack.packageId,
                                paymentMethod = selectedMethod,
                                paymentReference = transactionIdInput,
                                senderAccountPhone = senderPhone.trim()
                            )
                            isSubmitting = false
                            res.onSuccess { req ->
                                showBanglaCheckoutDialog = false
                                rechargeNotice = "🎉 আপনার পেমেন্ট রিকোয়েস্ট জমা হয়েছে (TrxID: ${req.paymentReference})। এডমিন ভেরিফাই করলেই কয়েন যুক্ত হবে।"
                                selectedTab = 1 // Switch to Purchases tab
                            }.onFailure { e ->
                                checkoutError = e.message ?: "পেমেন্ট সাবমিট করতে সমস্যা হয়েছে।"
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Submit Verification", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBanglaCheckoutDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel", color = Color.LightGray)
                }
            },
            containerColor = Color(0xFF1E1736)
        )
    }

    // ==========================================
    // 💎 DIAMOND TO COIN EXCHANGE DIALOG
    // ==========================================
    if (showExchangeDialog) {
        var exchangeAmountText by remember { mutableStateOf("") }
        var exchangeError by remember { mutableStateOf<String?>(null) }
        var isExchanging by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isExchanging) showExchangeDialog = false },
            title = { Text("🔄 Exchange Diamonds to Coins", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Convert your earned diamonds directly to spending coins (1 Diamond = 1 Coin).", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Available Diamonds: %,d 💎".format(lifetimeGiftVal), color = TealPremium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exchangeAmountText,
                        onValueChange = { exchangeAmountText = it.filter { c -> c.isDigit() } },
                        label = { Text("Amount of Diamonds", color = Color.Gray) },
                        placeholder = { Text("e.g. 500", color = Color.DarkGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (exchangeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(exchangeError!!, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = exchangeAmountText.toLongOrNull() ?: 0L
                        if (amt <= 0L) {
                            exchangeError = "Please enter an amount greater than zero."
                            return@Button
                        }
                        if (amt > lifetimeGiftVal) {
                            exchangeError = "Insufficient diamond balance."
                            return@Button
                        }
                        isExchanging = true
                        coroutineScope.launch {
                            val res = walletRepository.exchangeDiamondsToCoins(effectiveUserId, publicId, amt)
                            isExchanging = false
                            res.onSuccess {
                                showExchangeDialog = false
                                rechargeNotice = "Successfully exchanged %,d Diamonds to Coins!".format(amt)
                            }.onFailure { e ->
                                exchangeError = e.message ?: "Exchange failed."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
                ) {
                    Text("Exchange Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExchangeDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1736)
        )
    }

    // ==========================================
    // 💸 HOST CASHOUT WITHDRAWAL DIALOG
    // ==========================================
    if (showCashoutDialog) {
        var cashoutMethod by remember { mutableStateOf("bKash") }
        var cashoutAccount by remember { mutableStateOf("") }
        var cashoutCoinsText by remember { mutableStateOf("50000") }
        var isSubmittingCashout by remember { mutableStateOf(false) }
        var cashoutError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingCashout) showCashoutDialog = false },
            title = { Text("💸 Host Cashout Request", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Submit withdrawal request to your mobile banking or bank account.", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("bKash", "Nagad", "Bank").forEach { m ->
                            val sel = cashoutMethod == m
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) GoldPremium else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.weight(1f).clickable { cashoutMethod = m }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text(m, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cashoutAccount,
                        onValueChange = { cashoutAccount = it },
                        label = { Text("$cashoutMethod Account Number", color = Color.Gray) },
                        placeholder = { Text("017xxxxxxxx", color = Color.DarkGray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cashoutCoinsText,
                        onValueChange = { cashoutCoinsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Diamonds to withdraw (Min 50,000)", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val reqCoins = cashoutCoinsText.toLongOrNull() ?: 0L
                    val payoutBdt = reqCoins * paymentConfig.coinExchangeRateBDT
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Estimated Payout: ৳%.2f BDT".format(payoutBdt), color = GoldPremium, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    if (cashoutError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cashoutError!!, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coins = cashoutCoinsText.toLongOrNull() ?: 0L
                        if (coins < 50000L) {
                            cashoutError = "Minimum withdrawal is 50,000 diamonds."
                            return@Button
                        }
                        if (cashoutAccount.isBlank() || cashoutAccount.length < 10) {
                            cashoutError = "Please enter a valid account number."
                            return@Button
                        }
                        isSubmittingCashout = true
                        coroutineScope.launch {
                            val res = walletRepository.submitWithdrawalRequest(
                                uid = effectiveUserId,
                                publicUserId = publicId,
                                requestedCoins = coins,
                                paymentMethod = cashoutMethod,
                                accountNumber = cashoutAccount.trim()
                            )
                            isSubmittingCashout = false
                            res.onSuccess {
                                showCashoutDialog = false
                                rechargeNotice = "Withdrawal request submitted! Admin will verify and transfer the funds."
                            }.onFailure { e ->
                                cashoutError = e.message ?: "Submission failed."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("Submit Request", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashoutDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1736)
        )
    }

    // Profile Frame Store Dialog
    if (showFrameStore) {
        ProfileFrameStoreDialog(
            currentUserId = effectiveUserId,
            currentUserAvatar = "",
            currentEquippedFrameId = "frame_default",
            walletRepository = walletRepository,
            onDismiss = { showFrameStore = false },
            onEquippedChanged = {
                showFrameStore = false
            },
            onNavigateToRecharge = {
                showFrameStore = false
                selectedTab = 0
            }
        )
    }
}

@Composable
private fun StatMiniItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(label, color = Color.LightGray.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}
