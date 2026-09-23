package com.example.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

private val DarkNavyBg = Color(0xFF0F172A)
private val CardNavyBg = Color(0xFF1E293B)
private val GoldAccent = Color(0xFFFFD700)
private val CyanAccent = Color(0xFF00E5FF)
private val PurpleAccent = Color(0xFFA855F7)
private val GreenAccent = Color(0xFF10B981)
private val RedAccent = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    var showRoleDialogUser by remember { mutableStateOf<FirebaseUserProfile?>(null) }
    var showCoinDialogUser by remember { mutableStateOf<FirebaseUserProfile?>(null) }
    var showSanctionDialogUser by remember { mutableStateOf<FirebaseUserProfile?>(null) }
    var showEditGiftDialog by remember { mutableStateOf<CatalogGift?>(null) }
    var showEditFrameDialog by remember { mutableStateOf<ProfileFrameItem?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Platform Administration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                "Role: ${uiState.currentUserProfile?.role ?: "ADMIN"}",
                                fontSize = 12.sp,
                                color = GoldAccent
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshMetrics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkNavyBg,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkNavyBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Horizontal Admin Tab Bar
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = CardNavyBg,
                contentColor = GoldAccent,
                edgePadding = 12.dp
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (uiState.selectedTab == tab) GoldAccent else Color.LightGray
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState.selectedTab) {
                    AdminTab.OVERVIEW -> MetricsOverviewSection(uiState.metrics)
                    AdminTab.USERS -> UsersManagementSection(
                        users = uiState.userSearchResults,
                        searchQuery = uiState.searchQuery,
                        onSearch = { viewModel.searchUsers(it) },
                        onRoleChange = { showRoleDialogUser = it },
                        onAdjustCoins = { showCoinDialogUser = it },
                        onSanction = { showSanctionDialogUser = it }
                    )
                    AdminTab.WALLET -> WalletEconomySection(
                        users = uiState.userSearchResults,
                        searchQuery = uiState.searchQuery,
                        onSearch = { viewModel.searchUsers(it) },
                        onAdjustCoins = { showCoinDialogUser = it },
                        purchaseRequests = uiState.pendingPurchaseRequests,
                        onApprovePurchase = { viewModel.approveCoinPurchase(it) },
                        onRejectPurchase = { reqId, reason -> viewModel.rejectCoinPurchase(reqId, reason) },
                        withdrawalRequests = uiState.pendingWithdrawalRequests,
                        onApproveWithdrawal = { reqId, trxId -> viewModel.reviewWithdrawal(reqId, true, null, trxId) },
                        onRejectWithdrawal = { reqId, reason -> viewModel.reviewWithdrawal(reqId, false, reason, null) },
                        paymentConfig = uiState.paymentSystemConfig,
                        onUpdatePaymentConfig = { viewModel.updatePaymentSystemConfig(it) }
                    )
                    AdminTab.GIFTS -> GiftsCatalogSection(
                        gifts = uiState.giftsCatalog,
                        onAddGift = { showEditGiftDialog = CatalogGift(isActive = true) },
                        onEditGift = { showEditGiftDialog = it }
                    )
                    AdminTab.FRAMES -> FramesCatalogSection(
                        frames = uiState.framesCatalog,
                        onAddFrame = { showEditFrameDialog = ProfileFrameItem(isActive = true) },
                        onEditFrame = { showEditFrameDialog = it }
                    )
                    AdminTab.FLAGS -> FeatureFlagsSection(
                        flags = uiState.featureFlags,
                        onToggle = { flag, enabled -> viewModel.toggleFeatureFlag(flag.featureId, enabled) }
                    )
                    AdminTab.SETTINGS -> SystemSettingsSection(
                        settings = uiState.systemSettings,
                        onSaveSettings = { viewModel.updateSystemSettings(it) }
                    )
                    AdminTab.AUDIT -> AuditLogsSection(logs = uiState.auditLogs)
                }
            }
        }
    }

    // --- DIALOGS ---
    showRoleDialogUser?.let { targetUser ->
        ChangeRoleDialog(
            user = targetUser,
            currentAdminRole = uiState.currentUserProfile?.role ?: "ADMIN",
            onDismiss = { showRoleDialogUser = null },
            onConfirm = { newRole, reason ->
                viewModel.updateUserRole(targetUser.uid, newRole, reason)
                showRoleDialogUser = null
            }
        )
    }

    showCoinDialogUser?.let { targetUser ->
        AdjustCoinsDialog(
            user = targetUser,
            onDismiss = { showCoinDialogUser = null },
            onConfirm = { delta, reason ->
                viewModel.adjustUserCoins(targetUser.uid, targetUser.publicUserId, delta, reason)
                showCoinDialogUser = null
            }
        )
    }

    showSanctionDialogUser?.let { targetUser ->
        AdminSanctionDialog(
            user = targetUser,
            onDismiss = { showSanctionDialogUser = null },
            onConfirm = { action, reason, durationMs ->
                viewModel.executeUserSanction(targetUser.uid, action, reason, durationMs)
                showSanctionDialogUser = null
            }
        )
    }

    showEditGiftDialog?.let { gift ->
        EditGiftDialog(
            gift = gift,
            onDismiss = { showEditGiftDialog = null },
            onSave = { updated ->
                viewModel.saveGift(updated)
                showEditGiftDialog = null
            }
        )
    }

    showEditFrameDialog?.let { frame ->
        EditFrameDialog(
            frame = frame,
            onDismiss = { showEditFrameDialog = null },
            onSave = { updated ->
                viewModel.saveProfileFrame(updated)
                showEditFrameDialog = null
            }
        )
    }
}

@Composable
private fun MetricsOverviewSection(metrics: SystemMetrics) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Platform Server Status", fontSize = 14.sp, color = Color.Gray)
                        Text(
                            metrics.serverStatus,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenAccent
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = GreenAccent.copy(alpha = 0.2f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent)
                        }
                    }
                }
            }
        }

        item {
            Text("Real-Time Telemetry", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Total Users",
                    value = "${metrics.totalUsers}",
                    icon = Icons.Default.People,
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Online Users",
                    value = "${metrics.onlineUsers}",
                    icon = Icons.Default.WifiTethering,
                    color = GreenAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Active Rooms",
                    value = "${metrics.activeRooms}",
                    icon = Icons.Default.Mic,
                    color = PurpleAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Voice Sessions",
                    value = "${metrics.activeVoiceSessions}",
                    icon = Icons.Default.Headphones,
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Transactions",
                    value = "${metrics.totalTransactions}",
                    icon = Icons.Default.ReceiptLong,
                    color = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Coin Volume",
                    value = "${metrics.transactionVolume} 🪙",
                    icon = Icons.Default.MonetizationOn,
                    color = GoldAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Pending Reports",
                    value = "${metrics.pendingReports}",
                    icon = Icons.Default.Report,
                    color = if (metrics.pendingReports > 0) RedAccent else GreenAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Games",
                    value = "${metrics.activeGames}",
                    icon = Icons.Default.SportsEsports,
                    color = PurpleAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardNavyBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun UsersManagementSection(
    users: List<FirebaseUserProfile>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onRoleChange: (FirebaseUserProfile) -> Unit,
    onAdjustCoins: (FirebaseUserProfile) -> Unit,
    onSanction: (FirebaseUserProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            placeholder = { Text("Search by Public ID or Name...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                unfocusedBorderColor = CardNavyBg,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No users found matching query.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users, key = { it.uid }) { user ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = DarkNavyBg, modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(user.displayName.take(1).uppercase(), color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.displayName, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (user.role == "SUPER_ADMIN" || user.role == "ADMIN") GoldAccent.copy(alpha = 0.2f) else DarkNavyBg
                                        ) {
                                            Text(
                                                user.role,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.role == "SUPER_ADMIN" || user.role == "ADMIN") GoldAccent else Color.LightGray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "ID: ${user.publicUserId} | Coins: ${user.coinBalance} | VIP: ${user.vipLevel}",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        "Status: ${user.accountStatus} (${user.moderationStatus})",
                                        fontSize = 11.sp,
                                        color = if (user.accountStatus == "ACTIVE") GreenAccent else RedAccent
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onRoleChange(user) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Role", fontSize = 12.sp, color = GoldAccent)
                                }
                                OutlinedButton(
                                    onClick = { onAdjustCoins(user) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Coins", fontSize = 12.sp, color = CyanAccent)
                                }
                                Button(
                                    onClick = { onSanction(user) },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.8f))
                                ) {
                                    Text("Sanction", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletEconomySection(
    users: List<FirebaseUserProfile>,
    searchQuery: String,
    onSearch: (String) -> Unit,
    onAdjustCoins: (FirebaseUserProfile) -> Unit,
    purchaseRequests: List<CoinPurchaseRequest>,
    onApprovePurchase: (String) -> Unit,
    onRejectPurchase: (String, String) -> Unit,
    withdrawalRequests: List<WithdrawalRequest>,
    onApproveWithdrawal: (String, String?) -> Unit,
    onRejectWithdrawal: (String, String) -> Unit,
    paymentConfig: PaymentSystemConfig,
    onUpdatePaymentConfig: (PaymentSystemConfig) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Pending Recharges, 1: Pending Withdrawals, 2: Gateway Numbers, 3: User Balances
    var rejectTargetReqId by remember { mutableStateOf<String?>(null) }
    var rejectReasonText by remember { mutableStateOf("") }
    var rejectWithdrawalReqId by remember { mutableStateOf<String?>(null) }
    var rejectWithdrawalReasonText by remember { mutableStateOf("") }
    var approveWithdrawalReqId by remember { mutableStateOf<String?>(null) }
    var payoutTrxIdText by remember { mutableStateOf("") }

    // Gateway numbers local editing state
    var bkashNum by remember(paymentConfig.bkashNumber) { mutableStateOf(paymentConfig.bkashNumber) }
    var nagadNum by remember(paymentConfig.nagadNumber) { mutableStateOf(paymentConfig.nagadNumber) }
    var rocketNum by remember(paymentConfig.rocketNumber) { mutableStateOf(paymentConfig.rocketNumber) }
    var upayNum by remember(paymentConfig.upayNumber) { mutableStateOf(paymentConfig.upayNumber) }
    var manualApproval by remember(paymentConfig.manualApprovalEnabled) { mutableStateOf(paymentConfig.manualApprovalEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Sub-tabs row
        ScrollableTabRow(
            selectedTabIndex = subTab,
            containerColor = CardNavyBg,
            contentColor = GoldAccent,
            edgePadding = 8.dp
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = {
                    Text(
                        "Recharges (${purchaseRequests.size})",
                        fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (purchaseRequests.isNotEmpty()) Color(0xFFFFB300) else Color.LightGray
                    )
                }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = {
                    Text(
                        "Withdrawals (${withdrawalRequests.size})",
                        fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (withdrawalRequests.isNotEmpty()) Color(0xFF00E5FF) else Color.LightGray
                    )
                }
            )
            Tab(
                selected = subTab == 2,
                onClick = { subTab = 2 },
                text = {
                    Text(
                        "Payment Numbers",
                        fontWeight = if (subTab == 2) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
            Tab(
                selected = subTab == 3,
                onClick = { subTab = 3 },
                text = {
                    Text(
                        "User Balances",
                        fontWeight = if (subTab == 3) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (subTab) {
            0 -> {
                // TAB 0: PENDING RECHARGES (bKash, Nagad, etc.)
                if (purchaseRequests.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎉", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No pending recharge requests",
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "All user payments have been reviewed and verified.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(purchaseRequests, key = { it.requestId }) { req ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "User ${req.publicUserId.ifBlank { req.uid.take(6) }}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                "ID: ${req.publicUserId}",
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFFB300).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "${req.paymentMethod} • ৳%.0f".format(req.priceBDT),
                                                color = Color(0xFFFFB300),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkNavyBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("TrxID:", color = Color.Gray, fontSize = 12.sp)
                                                Text(
                                                    req.paymentReference,
                                                    color = Color(0xFF00E5FF),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Sender Mobile:", color = Color.Gray, fontSize = 12.sp)
                                                Text(
                                                    req.senderAccountPhone,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Package:", color = Color.Gray, fontSize = 12.sp)
                                                Text(
                                                    "%,d + %,d Bonus Coins".format(req.coinAmount, req.bonusCoins),
                                                    color = GoldAccent,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onApprovePurchase(req.requestId) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = DarkNavyBg, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Verify & Credit", color = DarkNavyBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                rejectTargetReqId = req.requestId
                                                rejectReasonText = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.85f))
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // TAB 1: PENDING WITHDRAWALS
                if (withdrawalRequests.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💎", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No pending withdrawals",
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Host cashouts are all processed.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(withdrawalRequests, key = { it.requestId }) { req ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                     Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                     ) {
                                        Column {
                                            Text(
                                                "User ${req.publicUserId.ifBlank { req.uid.take(6) }}",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 15.sp
                                            )
                                            Text("Diamonds: %,d 💎".format(req.requestedCoins), color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = GreenAccent.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Payout: ৳%.2f".format(req.withdrawalAmountBDT),
                                                color = GreenAccent,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkNavyBg,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Destination: ${req.paymentMethod} (${req.accountNumber})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Status: ${req.status}", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                approveWithdrawalReqId = req.requestId
                                                payoutTrxIdText = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                                        ) {
                                            Text("Approve & Pay", color = DarkNavyBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = {
                                                rejectWithdrawalReqId = req.requestId
                                                rejectWithdrawalReasonText = ""
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.85f))
                                        ) {
                                            Text("Reject & Refund", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // TAB 2: OFFICIAL PAYMENT NUMBERS CONFIGURATION
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Official Bangla Payment Gateway Numbers",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            "Users will see these phone numbers to send money for recharge.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = bkashNum,
                            onValueChange = { bkashNum = it },
                            label = { Text("bKash Personal / Agent Number", color = Color(0xFFFF4081)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF4081),
                                unfocusedBorderColor = DarkNavyBg
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = nagadNum,
                            onValueChange = { nagadNum = it },
                            label = { Text("Nagad Personal / Merchant Number", color = Color(0xFFFF9800)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = DarkNavyBg
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = rocketNum,
                            onValueChange = { rocketNum = it },
                            label = { Text("Rocket Number", color = Color(0xFF8E24AA)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8E24AA),
                                unfocusedBorderColor = DarkNavyBg
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = upayNum,
                            onValueChange = { upayNum = it },
                            label = { Text("Upay Number", color = Color(0xFF00ACC1)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00ACC1),
                                unfocusedBorderColor = DarkNavyBg
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Require Admin Manual Verification", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = manualApproval,
                                onCheckedChange = { manualApproval = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GoldAccent, checkedTrackColor = CardNavyBg)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onUpdatePaymentConfig(
                                    paymentConfig.copy(
                                        bkashNumber = bkashNum.trim(),
                                        nagadNumber = nagadNum.trim(),
                                        rocketNumber = rocketNum.trim(),
                                        upayNumber = upayNum.trim(),
                                        manualApprovalEnabled = manualApproval
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Text("Save Payment Numbers", color = DarkNavyBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            3 -> {
                // TAB 3: USER BALANCES & MANUAL ADJUSTMENT
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Economy Ledger Supervision", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Search users to issue verified credit adjustments.", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearch,
                    placeholder = { Text("Search by name, ID or username...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = CardNavyBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users, key = { it.uid }) { user ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = CardNavyBg)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(user.displayName, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("ID: ${user.publicUserId} | Balance: ${user.coinBalance} 🪙", fontSize = 12.sp, color = GoldAccent)
                                }
                                Button(
                                    onClick = { onAdjustCoins(user) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                                ) {
                                    Text("Adjust", color = DarkNavyBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Purchase rejection dialog
    rejectTargetReqId?.let { reqId ->
        AlertDialog(
            onDismissRequest = { rejectTargetReqId = null },
            title = { Text("Reject Coin Purchase", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Enter rejection reason for the user (e.g. Invalid TrxID, Amount mismatch):", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectReasonText,
                        onValueChange = { rejectReasonText = it },
                        placeholder = { Text("Reason...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectReasonText.ifBlank { "Invalid transaction ID or payment not received." }
                        onRejectPurchase(reqId, reason)
                        rejectTargetReqId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Confirm Reject", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectTargetReqId = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardNavyBg
        )
    }

    // Withdrawal approval dialog (with optional payout TrxID)
    approveWithdrawalReqId?.let { reqId ->
        AlertDialog(
            onDismissRequest = { approveWithdrawalReqId = null },
            title = { Text("Approve Withdrawal Payout", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Enter the bank or mobile money TrxID sent to the user:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payoutTrxIdText,
                        onValueChange = { payoutTrxIdText = it },
                        placeholder = { Text("e.g. BK9823124", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApproveWithdrawal(reqId, payoutTrxIdText.trim().ifBlank { null })
                        approveWithdrawalReqId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                ) {
                    Text("Confirm & Mark Paid", color = DarkNavyBg, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { approveWithdrawalReqId = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardNavyBg
        )
    }

    // Withdrawal rejection dialog
    rejectWithdrawalReqId?.let { reqId ->
        AlertDialog(
            onDismissRequest = { rejectWithdrawalReqId = null },
            title = { Text("Reject Withdrawal", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("Diamonds will be immediately refunded to user's wallet.", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectWithdrawalReasonText,
                        onValueChange = { rejectWithdrawalReasonText = it },
                        placeholder = { Text("Reason for rejection...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = rejectWithdrawalReasonText.ifBlank { "Invalid payment details." }
                        onRejectWithdrawal(reqId, reason)
                        rejectWithdrawalReqId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                ) {
                    Text("Reject & Refund", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectWithdrawalReqId = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = CardNavyBg
        )
    }
}

@Composable
private fun GiftsCatalogSection(
    gifts: List<CatalogGift>,
    onAddGift: () -> Unit,
    onEditGift: (CatalogGift) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Gift Catalog (${gifts.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Button(
                onClick = onAddGift,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = DarkNavyBg)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Gift", color = DarkNavyBg, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(gifts, key = { it.giftId }) { gift ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(gift.emoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(gift.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${gift.category} • ${gift.coinPrice} Coins", fontSize = 12.sp, color = GoldAccent)
                            }
                        }
                        IconButton(onClick = { onEditGift(gift) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FramesCatalogSection(
    frames: List<ProfileFrameItem>,
    onAddFrame: () -> Unit,
    onEditFrame: (ProfileFrameItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile Frames Store (${frames.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Button(
                onClick = onAddFrame,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = DarkNavyBg)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Frame", color = DarkNavyBg, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(frames, key = { it.frameId }) { frame ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(frame.name, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "${frame.rarity} | Cost: ${frame.requiredCoins} Coins | VIP req: ${frame.requiredVipLevel}",
                                fontSize = 12.sp,
                                color = CyanAccent
                            )
                        }
                        IconButton(onClick = { onEditFrame(frame) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureFlagsSection(
    flags: List<FeatureFlag>,
    onToggle: (FeatureFlag, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("System Subsystem Flags", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Text("Instantly toggle platform modules without redeployment.", color = Color.Gray, fontSize = 12.sp)
        }

        items(flags, key = { it.featureId }) { flag ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavyBg),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(flag.name, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(flag.description, fontSize = 12.sp, color = Color.Gray)
                        if (flag.lastUpdatedBy.isNotBlank()) {
                            Text("Updated by: ${flag.lastUpdatedBy}", fontSize = 10.sp, color = GoldAccent)
                        }
                    }
                    Switch(
                        checked = flag.isEnabled,
                        onCheckedChange = { onToggle(flag, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldAccent,
                            checkedTrackColor = CardNavyBg.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemSettingsSection(
    settings: SystemSettingsRecord,
    onSaveSettings: (SystemSettingsRecord) -> Unit
) {
    var maintenance by remember(settings) { mutableStateOf(settings.maintenanceMode) }
    var allowReg by remember(settings) { mutableStateOf(settings.allowNewRegistrations) }
    var welcomeCoins by remember(settings) { mutableStateOf(settings.defaultWelcomeCoins.toString()) }
    var serverNotice by remember(settings) { mutableStateOf(settings.serverNotice) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("System Configuration & Maintenance", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardNavyBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Maintenance Mode", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Prevents non-admin access during upgrades", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = maintenance,
                            onCheckedChange = { maintenance = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RedAccent)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Allow New Registrations", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Enable or pause new account onboarding", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = allowReg,
                            onCheckedChange = { allowReg = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent)
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = welcomeCoins,
                onValueChange = { welcomeCoins = it },
                label = { Text("Default Welcome Bonus Coins", color = GoldAccent) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = CardNavyBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        item {
            OutlinedTextField(
                value = serverNotice,
                onValueChange = { serverNotice = it },
                label = { Text("Server Broadcast Notice", color = GoldAccent) },
                placeholder = { Text("e.g. Scheduled maintenance at 02:00 UTC", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldAccent,
                    unfocusedBorderColor = CardNavyBg,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        item {
            Button(
                onClick = {
                    val coins = welcomeCoins.toLongOrNull() ?: 2500L
                    onSaveSettings(
                        settings.copy(
                            maintenanceMode = maintenance,
                            allowNewRegistrations = allowReg,
                            defaultWelcomeCoins = coins,
                            serverNotice = serverNotice,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("Save System Settings", color = DarkNavyBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AuditLogsSection(logs: List<AdminAuditLogRecord>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Immutable Audit Trail (${logs.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
        }

        if (logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No administrative audit records logged yet.", color = Color.Gray)
                }
            }
        } else {
            items(logs, key = { it.logId }) { log ->
                val dateFormatted = remember(log.createdAt) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.createdAt))
                }
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardNavyBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.action, fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 13.sp)
                            Text(dateFormatted, fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Actor: ${log.actorDisplayName} (${log.actorRole})", fontSize = 12.sp, color = Color.White)
                        Text("Target: [${log.targetType}] ${log.targetId}", fontSize = 12.sp, color = CyanAccent)
                        Text("Reason: ${log.reason}", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// --- DIALOG IMPLEMENTATIONS ---

@Composable
private fun ChangeRoleDialog(
    user: FirebaseUserProfile,
    currentAdminRole: String,
    onDismiss: () -> Unit,
    onConfirm: (newRole: String, reason: String) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }
    var reason by remember { mutableStateOf("") }

    val availableRoles = if (currentAdminRole == ModerationRoles.SUPER_ADMIN) {
        listOf(ModerationRoles.USER, ModerationRoles.MODERATOR, ModerationRoles.SENIOR_MODERATOR, ModerationRoles.ADMIN, ModerationRoles.SUPER_ADMIN)
    } else {
        listOf(ModerationRoles.USER, ModerationRoles.MODERATOR, ModerationRoles.SENIOR_MODERATOR)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Modify User Role", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Text("Target: ${user.displayName} (${user.publicUserId})", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(14.dp))

                availableRoles.forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRole = role }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(role, color = if (selectedRole == role) GoldAccent else Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Justification Reason", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(selectedRole, reason.ifBlank { "Administrative role assignment" }) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("Apply Role", color = DarkNavyBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustCoinsDialog(
    user: FirebaseUserProfile,
    onDismiss: () -> Unit,
    onConfirm: (delta: Long, reason: String) -> Unit
) {
    var amountString by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Coin Balance Adjustment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Text("Target: ${user.displayName} (Current: ${user.coinBalance} 🪙)", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("Credit (+)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenAccent)
                    )
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("Debit (-)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RedAccent)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Coin Amount", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Audit Justification", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountString.toLongOrNull() ?: 0L
                            if (amt > 0) {
                                val delta = if (isCredit) amt else -amt
                                onConfirm(delta, reason.ifBlank { "Administrative balance adjustment" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("Submit Adjustment", color = DarkNavyBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSanctionDialog(
    user: FirebaseUserProfile,
    onDismiss: () -> Unit,
    onConfirm: (action: String, reason: String, durationMs: Long?) -> Unit
) {
    var selectedAction by remember { mutableStateOf(ModerationActions.WARN) }
    var reason by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf("24") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Enforce User Sanction", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Text("Target: ${user.displayName} (${user.publicUserId})", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                val actions = listOf(
                    ModerationActions.WARN to "Issue Warning",
                    ModerationActions.MUTE to "Mute Chat & Audio",
                    ModerationActions.RESTRICT to "Restrict Features",
                    ModerationActions.SUSPEND to "Temporary Suspension",
                    ModerationActions.BAN to "Permanent Ban",
                    ModerationActions.UNBAN to "Restore / Unban"
                )

                actions.forEach { (act, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAction = act }
                            .padding(vertical = 3.dp)
                    ) {
                        RadioButton(
                            selected = selectedAction == act,
                            onClick = { selectedAction = act },
                            colors = RadioButtonDefaults.colors(selectedColor = RedAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = if (selectedAction == act) RedAccent else Color.White)
                    }
                }

                if (selectedAction in listOf(ModerationActions.MUTE, ModerationActions.RESTRICT, ModerationActions.SUSPEND)) {
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it },
                        label = { Text("Duration (Hours)", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Mandatory Violation Reason", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val durationMs = durationHours.toLongOrNull()?.times(3600 * 1000)
                            onConfirm(selectedAction, reason.ifBlank { "Violation of community standards" }, durationMs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                    ) {
                        Text("Apply Sanction", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditGiftDialog(
    gift: CatalogGift,
    onDismiss: () -> Unit,
    onSave: (CatalogGift) -> Unit
) {
    var name by remember { mutableStateOf(gift.name) }
    var emoji by remember { mutableStateOf(gift.emoji) }
    var category by remember { mutableStateOf(gift.category) }
    var priceString by remember { mutableStateOf(gift.coinPrice.toString()) }
    var isFullEffect by remember { mutableStateOf(gift.isFullScreenEffect) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (gift.giftId.isBlank()) "Create Catalog Gift" else "Edit Gift", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Gift Name", color = Color.Gray) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Classic/Romantic/Luxury/VIP)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceString,
                    onValueChange = { priceString = it },
                    label = { Text("Coin Price", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Full Screen Animation Effect", color = Color.White, fontSize = 13.sp)
                    Switch(checked = isFullEffect, onCheckedChange = { isFullEffect = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceString.toLongOrNull() ?: 100L
                            onSave(
                                gift.copy(
                                    name = name.ifBlank { "Gift" },
                                    emoji = emoji.ifBlank { "🎁" },
                                    category = category.ifBlank { "Classic" },
                                    coinPrice = price,
                                    isFullScreenEffect = isFullEffect
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                    ) {
                        Text("Save Gift", color = DarkNavyBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditFrameDialog(
    frame: ProfileFrameItem,
    onDismiss: () -> Unit,
    onSave: (ProfileFrameItem) -> Unit
) {
    var name by remember { mutableStateOf(frame.name) }
    var rarity by remember { mutableStateOf(frame.rarity) }
    var priceString by remember { mutableStateOf(frame.requiredCoins.toString()) }
    var vipReqString by remember { mutableStateOf(frame.requiredVipLevel.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardNavyBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(if (frame.frameId.isBlank()) "Create Profile Frame" else "Edit Frame", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Frame Name", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rarity,
                    onValueChange = { rarity = it },
                    label = { Text("Rarity (Common, Rare, Epic, Legendary)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceString,
                        onValueChange = { priceString = it },
                        label = { Text("Price Coins", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = vipReqString,
                        onValueChange = { vipReqString = it },
                        label = { Text("Req. VIP Level", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceString.toLongOrNull() ?: 500L
                            val vipReq = vipReqString.toIntOrNull() ?: 0
                            onSave(
                                frame.copy(
                                    name = name.ifBlank { "Custom Frame" },
                                    rarity = rarity.ifBlank { "Rare" },
                                    requiredCoins = price,
                                    requiredVipLevel = vipReq
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("Save Frame", color = DarkNavyBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
