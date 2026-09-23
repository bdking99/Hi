package com.example.ui.screens.vip

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.VipLevelConfig
import com.example.ui.components.LevelUpCelebrationDialog
import com.example.ui.components.UserLevelBadge
import com.example.ui.components.VipAvatarFrameWrapper
import com.example.ui.components.VipBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipCenterScreen(
    viewModel: VipViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWallet: () -> Unit = {}
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val vipLevels by viewModel.vipLevels.collectAsState()
    val vipStatus by viewModel.userVipStatus.collectAsState()
    val levelInfo by viewModel.userLevelInfo.collectAsState()
    val vipHistory by viewModel.vipHistory.collectAsState()
    val animationPrefs by viewModel.animationPrefs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val selectedTier by viewModel.selectedTier.collectAsState()
    val celebrationReward by viewModel.showCelebration.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var adminEditingTier by remember { mutableStateOf<VipLevelConfig?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👑 VIP & Privilege Center",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.syncAuthoritativeVipStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFFFFD54F)
                        )
                    }
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF13131D)
                )
            )
        },
        containerColor = Color(0xFF0D0D14)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ==========================================
            // 1. VIP HEADER STATUS CARD
            // ==========================================
            item {
                VipHeaderCard(
                    userProfile = userProfile,
                    vipStatus = vipStatus,
                    levelInfo = levelInfo,
                    reduceMotion = animationPrefs.reduceMotion,
                    onNavigateToWallet = onNavigateToWallet
                )
            }

            // ==========================================
            // 2. VIP / SVIP / LEVEL TAB SELECTOR
            // ==========================================
            item {
                TabRow(
                    selectedTabIndex = when (activeTab) {
                        "VIP" -> 0
                        "SVIP" -> 1
                        else -> 2
                    },
                    containerColor = Color(0xFF1A1A26),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                tabPositions[when (activeTab) {
                                    "VIP" -> 0
                                    "SVIP" -> 1
                                    else -> 2
                                }]
                            ),
                            color = if (activeTab == "SVIP") Color(0xFF00E5FF) else Color(0xFFFFB300)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTab == "VIP",
                        onClick = { viewModel.setTab("VIP") },
                        text = {
                            Text(
                                "👑 VIP Tiers",
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "VIP") Color(0xFFFFD54F) else Color.Gray
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == "SVIP",
                        onClick = { viewModel.setTab("SVIP") },
                        text = {
                            Text(
                                "💎 SVIP Cosmic",
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "SVIP") Color(0xFF00E5FF) else Color.Gray
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == "LEVEL",
                        onClick = { viewModel.setTab("LEVEL") },
                        text = {
                            Text(
                                "🌟 Level XP",
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "LEVEL") Color(0xFFB388FF) else Color.Gray
                            )
                        }
                    )
                }
            }

            // ==========================================
            // 3. TAB CONTENT
            // ==========================================
            if (activeTab == "VIP" || activeTab == "SVIP") {
                val filteredTiers = vipLevels.filter { it.type == activeTab }

                // Horizontal Carousel of Tiers
                item {
                    Text(
                        text = if (activeTab == "VIP") "Select VIP Tier (1-10)" else "Select SVIP Tier (1-5)",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredTiers) { tier ->
                            val isSelected = selectedTier?.levelId == tier.levelId
                            val isUnlocked = (activeTab == "VIP" && vipStatus.vipLevel >= tier.level) ||
                                    (activeTab == "SVIP" && vipStatus.svipLevel >= tier.level)

                            VipTierItemCard(
                                tier = tier,
                                isSelected = isSelected,
                                isUnlocked = isUnlocked,
                                onClick = { viewModel.selectTier(tier) }
                            )
                        }
                    }
                }

                // Selected Tier Details & Benefits
                item {
                    selectedTier?.let { tier ->
                        val isUnlocked = (activeTab == "VIP" && vipStatus.vipLevel >= tier.level) ||
                                (activeTab == "SVIP" && vipStatus.svipLevel >= tier.level)

                        VipTierDetailsCard(
                            tier = tier,
                            isUnlocked = isUnlocked,
                            reduceMotion = animationPrefs.reduceMotion,
                            onEquipFrame = { viewModel.equipVipFrame(tier.frameId) },
                            onAdminEdit = { adminEditingTier = tier }
                        )
                    }
                }
            } else {
                // USER LEVEL PROGRESSION TAB
                item {
                    UserLevelProgressionCard(
                        levelInfo = levelInfo,
                        onSimulateActivity = { eventType, xp ->
                            viewModel.awardXp(eventType, xp)
                        }
                    )
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        VipAnimationSettingsDialog(
            preferences = animationPrefs,
            onSave = { showVip, showEntry, showGift, reduceMotion ->
                viewModel.updatePreferences(showVip, showEntry, showGift, reduceMotion)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // History Dialog
    if (showHistoryDialog) {
        VipHistoryDialog(
            historyList = vipHistory,
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Admin Config Dialog
    adminEditingTier?.let { tier ->
        AdminVipConfigDialog(
            levelConfig = tier,
            onSave = { newThreshold, benefits, isActive ->
                viewModel.adminUpdateThreshold(tier.levelId, newThreshold, benefits, isActive)
            },
            onDismiss = { adminEditingTier = null }
        )
    }

    // Celebration Popup
    celebrationReward?.let { reward ->
        LevelUpCelebrationDialog(
            reward = reward,
            onDismiss = { viewModel.dismissCelebration() },
            onEquipFrame = { frameId -> viewModel.equipVipFrame(frameId) }
        )
    }
}

// 👑 HEADER VIP STATUS OVERVIEW
@Composable
private fun VipHeaderCard(
    userProfile: com.example.data.model.FirebaseUserProfile?,
    vipStatus: com.example.data.model.UserVipStatus,
    levelInfo: com.example.data.model.UserLevelInfo,
    reduceMotion: Boolean,
    onNavigateToWallet: () -> Unit
) {
    val isSvip = vipStatus.svipLevel > 0
    val isVip = vipStatus.vipLevel > 0

    val gradientBrush = if (isSvip) {
        Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF006064), Color(0xFF1A237E)))
    } else if (isVip) {
        Brush.linearGradient(listOf(Color(0xFF4E342E), Color(0xFFE65100), Color(0xFF263238)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1E1E2C), Color(0xFF2B2B3D)))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .border(
                1.5.dp,
                if (isSvip) Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFE040FB)))
                else if (isVip) Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8F00)))
                else Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF263238))),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Top Row: Avatar + Name + Badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VipAvatarFrameWrapper(
                        frameId = userProfile?.profileFrameId,
                        vipLevel = vipStatus.vipLevel,
                        svipLevel = vipStatus.svipLevel,
                        reduceMotion = reduceMotion,
                        size = 56.dp
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userProfile?.avatar?.ifBlank { "https://picsum.photos/seed/${vipStatus.uid}/200" })
                                .crossfade(true)
                                .build(),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.displayName?.ifBlank { "Voice Club Member" } ?: "Player",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VipBadge(
                                vipType = vipStatus.vipType,
                                vipLevel = vipStatus.vipLevel,
                                svipLevel = vipStatus.svipLevel
                            )
                            UserLevelBadge(level = levelInfo.level)
                        }
                        Text(
                            text = "ID: ${vipStatus.publicUserId}",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }

                // Progression Bar & Qualifying Metric
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSvip) "💎 SVIP Prestige Progress" else "👑 VIP Qualifying Progress",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "%,d / %,d Coins".format(vipStatus.verifiedRechargeAmount, vipStatus.nextLevelRequirement),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { vipStatus.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isSvip) Color(0xFF00E5FF) else Color(0xFFFFB300),
                        trackColor = Color(0xFF14141E)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (vipStatus.coinsToNextLevel > 0)
                                "%,d more coins needed for next level".format(vipStatus.coinsToNextLevel)
                            else "Max Tier Reached! 🎉",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )

                        TextButton(
                            onClick = onNavigateToWallet,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Recharge Coins ➔",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// 🎴 VIP TIER SELECTOR CARD
@Composable
private fun VipTierItemCard(
    tier: VipLevelConfig,
    isSelected: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val isSvip = tier.type == "SVIP"

    val borderColor = if (isSelected) {
        if (isSvip) Color(0xFF00E5FF) else Color(0xFFFFD700)
    } else if (isUnlocked) {
        Color(0xFF388E3C)
    } else {
        Color(0xFF2C2C3E)
    }

    val containerColor = if (isSelected) {
        if (isSvip) Color(0xFF26183B) else Color(0xFF362816)
    } else {
        Color(0xFF181824)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .width(85.dp)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tier.badgeEmoji,
                fontSize = 20.sp
            )
            Text(
                text = tier.name,
                color = if (isSelected) Color.White else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isUnlocked) "✓ Unlocked" else "%,d 🪙".format(tier.requiredRechargeAmount),
                color = if (isUnlocked) Color(0xFF81C784) else Color.Gray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 📜 TIER DETAILS & EXCLUSIVE BENEFITS CARD
@Composable
private fun VipTierDetailsCard(
    tier: VipLevelConfig,
    isUnlocked: Boolean,
    reduceMotion: Boolean,
    onEquipFrame: () -> Unit,
    onAdminEdit: () -> Unit
) {
    val isSvip = tier.type == "SVIP"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181824)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2A2A3E), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${tier.name} — ${tier.title}",
                        color = if (isSvip) Color(0xFF80D8FF) else Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Threshold: %,d Qualifying Coins".format(tier.requiredRechargeAmount),
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                if (isUnlocked) {
                    AssistChip(
                        onClick = {},
                        label = { Text("UNLOCKED", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF1B5E20),
                            labelColor = Color(0xFFA5D6A7)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF262638))

            // Frame Showcase & Equip Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF12121B), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VipAvatarFrameWrapper(
                    frameId = tier.frameId,
                    vipLevel = if (!isSvip) tier.level else 0,
                    svipLevel = if (isSvip) tier.level else 0,
                    reduceMotion = reduceMotion,
                    size = 46.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A2A3C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tier.badgeEmoji, fontSize = 20.sp)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exclusive Avatar Frame",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Frame ID: ${tier.frameId}",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                if (isUnlocked) {
                    Button(
                        onClick = onEquipFrame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSvip) Color(0xFF00ACC1) else Color(0xFFFFB300)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Equip", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Benefits Checklist
            Text(
                text = "✨ Tier Privileges & Privileged Access:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (benefit in tier.benefits) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓", color = if (isSvip) Color(0xFF00E5FF) else Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = benefit, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            // Admin configuration trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAdminEdit) {
                    Text("Admin Edit Tier Threshold ➔", color = Color(0xFFB388FF), fontSize = 11.sp)
                }
            }
        }
    }
}

// 🌟 USER LEVEL & XP PROGRESSION OVERVIEW
@Composable
private fun UserLevelProgressionCard(
    levelInfo: com.example.data.model.UserLevelInfo,
    onSimulateActivity: (eventType: String, xp: Long) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181824)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF33294E), RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🌟 Level ${levelInfo.level} — ${levelInfo.title}",
                        color = Color(0xFFB388FF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tier: ${levelInfo.badgeTier} ${levelInfo.badgeEmoji}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                UserLevelBadge(level = levelInfo.level)
            }

            // XP Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Level Experience Points (XP)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "%,d / %,d XP".format(levelInfo.currentXp, levelInfo.nextLevelXp),
                        color = Color(0xFFB388FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { levelInfo.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF7C4DFF),
                    trackColor = Color(0xFF12121E)
                )

                Text(
                    text = "%,d more XP needed to reach Level %d".format(levelInfo.xpToNextLevel, levelInfo.level + 1),
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            HorizontalDivider(color = Color(0xFF262638))

            // XP Activity Sources
            Text(
                text = "⚡ Verified Ways to Earn Experience XP:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                XpActivityRow(
                    emoji = "🎙️",
                    title = "Voice Room Speaking & Participation",
                    xp = "+10 XP / min",
                    onTrigger = { onSimulateActivity("ROOM_ACTIVITY", 50L) }
                )
                XpActivityRow(
                    emoji = "🎁",
                    title = "Sending Gifts in Voice Clubs",
                    xp = "+1 XP per 10 Coins Sent",
                    onTrigger = { onSimulateActivity("GIFT_SENT", 100L) }
                )
                XpActivityRow(
                    emoji = "🎮",
                    title = "Playing Game Center Rounds",
                    xp = "+25 XP / Game Round",
                    onTrigger = { onSimulateActivity("GAME_COMPLETED", 25L) }
                )
                XpActivityRow(
                    emoji = "💬",
                    title = "Daily Community Chat Engagement",
                    xp = "+5 XP / Message",
                    onTrigger = { onSimulateActivity("MESSAGE_SENT", 5L) }
                )
            }
        }
    }
}

@Composable
private fun XpActivityRow(
    emoji: String,
    title: String,
    xp: String,
    onTrigger: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13131D), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(emoji, fontSize = 18.sp)
            Column {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(xp, color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        FilledTonalButton(
            onClick = onTrigger,
            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF28283E)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Test XP", color = Color.White, fontSize = 10.sp)
        }
    }
}
