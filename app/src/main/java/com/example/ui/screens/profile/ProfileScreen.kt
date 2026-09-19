package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val DarkSurfaceMenu = Color(0xFF1E212B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    onWalletClick: () -> Unit = {}
) {
    val user by viewModel.user.collectAsState()
    val profile by viewModel.profile.collectAsState()

    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeDialogContent by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showSettingsSheet) {
        AppSettingsSheet(
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showEditProfileDialog) {
        UserProfileSettingsDialog(
            currentDisplayName = user?.displayName ?: "Alex King 👑",
            currentBio = profile?.bio ?: "Voice room enthusiast and active party club member! 🎙️✨",
            currentAvatar = user?.avatar,
            currentCover = user?.coverImage,
            onDismiss = { showEditProfileDialog = false },
            onSave = { displayName, bio, avatar, cover ->
                viewModel.updateProfile(displayName, bio, avatar, cover) {
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile updated successfully! ✨", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // --- 1. PROFILE HEADER SECTION ---
        ProfileHeaderSection(
            displayName = user?.displayName ?: "Alex King 👑",
            username = user?.username ?: "alex_king",
            uid = user?.publicUserId ?: "884920",
            avatarUrl = user?.avatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            vipLevel = profile?.vipLevel ?: 3,
            hostLevel = profile?.level ?: 18,
            followersCount = profile?.followingCount ?: 380,
            fansCount = profile?.followersCount ?: 1420,
            charmValue = (profile?.earnings ?: 4820L) * 4,
            onSettingsClick = {
                showSettingsSheet = true
            },
            onEditClick = {
                showEditProfileDialog = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. WALLET SECTION (Coin & Diamond Cards) ---
        WalletSection(
            coinBalance = profile?.coinBalance ?: 158400L,
            diamondBalance = profile?.earnings ?: 4820L,
            onCoinClick = onWalletClick,
            onDiamondClick = {
                activeDialogTitle = "💎 Diamond Exchange"
                activeDialogContent = "Convert your ${profile?.earnings ?: 4820L} Diamonds into Coins or withdraw earnings to your linked bank account."
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2.5 GIFT SHOWCASE SECTION ---
        GiftShowcaseSection(
            receivedDiamonds = profile?.earnings ?: 4820L,
            onShowcaseClick = {
                activeDialogTitle = "🎁 Luxury Gift Showcase"
                activeDialogContent = "You have received 142 Luxury Gifts across Voice Clubs, generating ${profile?.earnings ?: 4820L} Diamonds!"
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. MENU GRID (4x2 Grid: VIP, Mall, Backpack, Task, Medal, Family, CP Nest, Wealth) ---
        FeatureGridSection(
            onFeatureClick = { title, desc ->
                activeDialogTitle = title
                activeDialogContent = desc
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. ACTION LIST ---
        val showAgency = (profile?.agencyId != null) || (user?.roleId ?: 1) > 1
        ActionListSection(
            showAgency = showAgency,
            agencyName = profile?.agencyId ?: "Star_Talent_Agency",
            onActionClick = { title, desc ->
                activeDialogTitle = title
                activeDialogContent = desc
            },
            onOpenSettings = { showSettingsSheet = true },
            onLogoutPrompt = { showLogoutConfirm = true }
        )
    }

    // Generic Feature / Action Details Dialog
    if (activeDialogTitle != null) {
        AlertDialog(
            onDismissRequest = {
                activeDialogTitle = null
                activeDialogContent = null
            },
            title = { Text(activeDialogTitle ?: "", fontWeight = FontWeight.Bold) },
            text = { Text(activeDialogContent ?: "", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        activeDialogTitle = null
                        activeDialogContent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to log out of Great Voice Room? Your persistent session will be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout(onLogoutClick)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------
// Profile Header Section with 3D Animated Border
// ----------------------------------------------------
@Composable
fun ProfileHeaderSection(
    displayName: String,
    username: String,
    uid: String,
    avatarUrl: String,
    vipLevel: Int,
    hostLevel: Int,
    followersCount: Int,
    fansCount: Int,
    charmValue: Long,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Spring animations
    val headerAlpha = remember { Animatable(0f) }
    val headerOffsetY = remember { Animatable(-35f) }
    val avatarScale = remember { Animatable(0.6f) }
    val avatarAlpha = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(30f) }
    val statsAlpha = remember { Animatable(0f) }
    val statsScale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        launch { headerAlpha.animateTo(1f, tween(350)) }
        launch { headerOffsetY.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)) }
        launch {
            delay(100L)
            avatarAlpha.animateTo(1f, tween(200))
        }
        launch {
            delay(100L)
            avatarScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        }
        launch {
            delay(180L)
            contentAlpha.animateTo(1f, tween(220))
        }
        launch {
            delay(180L)
            contentOffsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
        launch {
            delay(260L)
            statsAlpha.animateTo(1f, tween(220))
        }
        launch {
            delay(260L)
            statsScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
        }
    }

    // 3D border rotation animation for halo effect
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_header_section")
            .graphicsLayer {
                alpha = headerAlpha.value
                translationY = headerOffsetY.value
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TealPremium.copy(alpha = 0.25f), MaterialTheme.colorScheme.background)
                )
            )
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            
            // Top action icons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEditClick, modifier = Modifier.testTag("edit_profile_button")) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings_button")) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            // Profile Picture with 3D Animated Rotating Border Halo
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .testTag("avatar_container")
                    .graphicsLayer {
                        scaleX = avatarScale.value
                        scaleY = avatarScale.value
                        alpha = avatarAlpha.value
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer 3D animated sweep halo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .graphicsLayer { rotationZ = borderRotation }
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(GoldPremium, TealPremium, Color.White, GoldPremium)
                            )
                        )
                )

                // Inner Avatar
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }

                // VIP Badge
                if (vipLevel > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                            .background(GoldPremium, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("VIP $vipLevel", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // User name, Badges, UID & Gender
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = contentOffsetY.value
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = TealPremium, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // UID (with Copy button) + Gender Chip + Level Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Luxury Golden UID Badge with copy action
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF221A38),
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPremium, TealPremium))),
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(uid))
                            Toast.makeText(context, "👑 VIP UID $uid copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👑 ID: $uid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPremium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(13.dp), tint = TealPremium)
                        }
                    }

                    // Gender Badge (♂ Male 24)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2979FF).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(0xFF2979FF).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "♂ 24",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2979FF),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Host Level Badge (Lv.18)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GoldPremium.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, GoldPremium)
                    ) {
                        Text(
                            text = "Lv.$hostLevel ★",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPremium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Stats Bar: Concern (Following), Fan (Followers), Charm Value
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = statsScale.value
                        scaleY = statsScale.value
                        alpha = statsAlpha.value
                    },
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Concern", followersCount.toString())
                StatItem("Fan", fansCount.toString())
                StatItem("Charm Value", "⭐ %,d".format(charmValue))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ----------------------------------------------------
// Wallet Section: Coin Card & Diamond Card
// ----------------------------------------------------
@Composable
fun WalletSection(
    coinBalance: Long,
    diamondBalance: Long,
    onCoinClick: () -> Unit,
    onDiamondClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Coin Card
        Card(
            modifier = Modifier
                .weight(1f)
                .height(95.dp)
                .clickable { onCoinClick() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B182B))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, GoldPremium.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coin", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldPremium
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                    Text(
                        text = "%,d".format(coinBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPremium
                    )
                }
            }
        }

        // 2. Diamond Card
        Card(
            modifier = Modifier
                .weight(1f)
                .height(95.dp)
                .clickable { onDiamondClick() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, TealPremium.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💎", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Diamond", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealPremium
                        ) {
                            Text("Exchange", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "%,d".format(diamondBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPremium
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Feature Grid (4x2 Layout: VIP, Mall, Backpack, Task, Medal, Family, CP Nest, Wealth)
// ----------------------------------------------------
@Composable
fun FeatureGridSection(
    onFeatureClick: (String, String) -> Unit
) {
    val features = listOf(
        Triple("VIP", Icons.Filled.WorkspacePremium, "Unlock custom entry cars, animated text bubbles, and noble avatar frames."),
        Triple("Mall", Icons.Filled.Store, "Browse themed voice room decorations, gifts, and exclusive dynamic profile effects."),
        Triple("Backpack", Icons.Filled.ShoppingBag, "View your saved gift inventory, vehicle garage, and temporary coupons."),
        Triple("Task", Icons.Filled.Assignment, "Complete daily voice party missions to claim free coins and medal EXP."),
        Triple("Medal", Icons.Filled.EmojiEvents, "Showcase your voice contest championships, gifting badges, and streaming milestones."),
        Triple("Family", Icons.Filled.People, "Manage your Voice Clan, clan battle points, and weekly team bonuses."),
        Triple("CP Nest", Icons.Filled.Favorite, "Link with your special companion, raise your CP ring level, and show mutual love space."),
        Triple("Wealth", Icons.Filled.ShowChart, "Check your current noble ranking, wealth privilege title, and level leaderboard.")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("feature_grid"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1 (4 items)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 0..3) {
                    val (title, icon, desc) = features[i]
                    FeatureGridItem(
                        title = title,
                        icon = icon,
                        index = i,
                        onClick = { onFeatureClick(title, desc) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Row 2 (4 items)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (i in 4..7) {
                    val (title, icon, desc) = features[i]
                    FeatureGridItem(
                        title = title,
                        icon = icon,
                        index = i,
                        onClick = { onFeatureClick(title, desc) }
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureGridItem(
    title: String,
    icon: ImageVector,
    index: Int = 0,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "press_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .testTag("feature_item_${title.lowercase().replace(" ", "_")}")
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(TealPremium.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .border(1.dp, GoldPremium.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = GoldPremium, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ----------------------------------------------------
// Action List Section (Host data, Reward Records, Interactive Games Records)
// ----------------------------------------------------
@Composable
fun ActionListSection(
    showAgency: Boolean,
    agencyName: String,
    onActionClick: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    onLogoutPrompt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Column {
            ActionListItem(
                title = "Host Data",
                icon = Icons.Outlined.Analytics,
                onClick = {
                    onActionClick(
                        "📊 Host Data Analytics",
                        "Broadcast Hours: 42.5 hrs this week\nTotal Gifts Received: 14,280\nLive Peak Viewers: 3,890\nPerformance Rating: Grade S Host"
                    )
                }
            )
            if (showAgency) {
                ActionListItem(
                    title = "Agency Center ($agencyName)",
                    icon = Icons.Outlined.Business,
                    onClick = {
                        onActionClick(
                            "🏢 Agency Center",
                            "Current Agency: $agencyName\nContract: Official Creator Partner\nCommission Settlement: Weekly on Friday"
                        )
                    }
                )
            }
            ActionListItem(
                title = "Reward Records",
                icon = Icons.Outlined.CardGiftcard,
                onClick = {
                    onActionClick(
                        "🎁 Reward & Transaction Records",
                        "+200 Coins (Daily Login Bonus)\n+1,000 Coins (Lucky Roulette Spin)\n+500 Coins (Host Performance Target)\n-100 Coins (Zeus Slot Spin)"
                    )
                }
            )
            ActionListItem(
                title = "Interactive Games Records",
                icon = Icons.Outlined.VideogameAsset,
                onClick = {
                    onActionClick(
                        "🎮 Interactive Games Records",
                        "Lucky Roulette: 12 Spins • 2 Jackpots won\nZeus Slot: 8 Spins • 5,000 Coins Won\nLuxury Car: 3 Bets • +800 Coins"
                    )
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu)
    ) {
        Column {
            ActionListItem(
                title = "Voice & Room Settings",
                icon = Icons.Outlined.Settings,
                onClick = onOpenSettings
            )
            ActionListItem(
                title = "Feedback & Help Center",
                icon = Icons.Outlined.HelpOutline,
                onClick = {
                    onActionClick(
                        "💡 Feedback & Help",
                        "Need assistance or have suggestions? Our 24/7 Creator Support team is here to help."
                    )
                }
            )
            ActionListItem(
                title = "Logout",
                icon = Icons.Outlined.ExitToApp,
                onClick = onLogoutPrompt,
                showArrow = false,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ActionListItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
    showArrow: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        if (showArrow) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GiftShowcaseSection(
    receivedDiamonds: Long,
    onShowcaseClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onShowcaseClick() }
            .testTag("gift_showcase_section"),
        color = Color(0xFF1E1A2F),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎁", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Luxury Gift Showcase",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "💎 $receivedDiamonds Diamonds",
                        color = GoldPremium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "View All",
                        tint = GoldPremium,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val showcaseItems = listOf(
                Triple("🌹", "Rose", "x48"),
                Triple("💖", "Heart", "x24"),
                Triple("☕", "Warm Coffee", "x15"),
                Triple("🏎️", "Sports Car", "x8"),
                Triple("🚀", "Rocket", "x5"),
                Triple("👑", "Imperial Crown", "x3")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                showcaseItems.forEach { (emoji, name, count) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = count,
                            color = GoldPremium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
