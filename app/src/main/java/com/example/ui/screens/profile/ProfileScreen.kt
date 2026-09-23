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
import androidx.compose.ui.draw.shadow
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
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.ModerationRoles
import com.example.data.model.ProfileFrame
import com.example.ui.components.ProfileAvatarWithFrame
import com.example.ui.components.UserSearchSheet
import com.example.ui.components.VisitedUserProfileDialog
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import com.example.utils.AccompanistPermissionHandler
import com.example.utils.PermissionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val DarkSurfaceMenu = Color(0xFF140F24)
val DarkSurfaceCard = Color(0xFF1C1533)
val GoldAccent = Color(0xFFFFD700)
val CrystalDiamond = Color(0xFF00E5FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogoutClick: () -> Unit,
    onWalletClick: () -> Unit = {},
    onVipClick: () -> Unit = {},
    onNavigateNotifications: () -> Unit = {},
    onNavigateModeration: () -> Unit = {},
    onNavigateAdmin: () -> Unit = {},
    onSendMessage: (FirebaseUserProfile) -> Unit = {},
    onVoiceCall: (FirebaseUserProfile) -> Unit = {}
) {
    val user by viewModel.user.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val publicProfileState by viewModel.publicProfileState.collectAsState()

    // Real-time Firestore profile data (falling back to Room entity)
    val firestoreProfile: FirebaseUserProfile? = when (val state = publicProfileState) {
        is ProfileUiState.Success -> state.data
        else -> null
    }

    val dynamicCoinBalance = firestoreProfile?.coinBalance ?: (profile?.coinBalance ?: 0L)
    val dynamicDiamondBalance = firestoreProfile?.diamondBalance ?: (profile?.earnings ?: 0L)
    val dynamicVipLevel = firestoreProfile?.vipLevel ?: (profile?.vipLevel ?: 1)
    val dynamicLevel = firestoreProfile?.level ?: (profile?.level ?: 1)

    val availableFrames by viewModel.availableFrames.collectAsState()
    val equippedFrame = remember(availableFrames, firestoreProfile?.profileFrameId) {
        availableFrames.find { it.id == firestoreProfile?.profileFrameId } ?: availableFrames.firstOrNull()
    }

    var showFrameSheet by remember { mutableStateOf(false) }
    var showProfileFrameStoreDialog by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var visitedUser by remember { mutableStateOf<FirebaseUserProfile?>(null) }
    var giftingTargetUser by remember { mutableStateOf<FirebaseUserProfile?>(null) }

    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeDialogContent by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showVipDetailDialog by remember { mutableStateOf<VipBadgeInfo?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val context = LocalContext.current

    // Accompanist Permission Handler
    AccompanistPermissionHandler(
        onPermissionsGranted = {
            pendingActionAfterPermission?.invoke()
            pendingActionAfterPermission = null
        },
        showRationaleDialog = showPermissionRationale,
        onDismissRationale = { showPermissionRationale = false }
    )

    fun executeWithPermissionCheck(action: () -> Unit) {
        if (PermissionManager.areAllRequiredGranted(context)) {
            action()
        } else {
            pendingActionAfterPermission = action
            showPermissionRationale = true
        }
    }

    if (showSettingsSheet) {
        AppSettingsSheet(
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showFrameSheet) {
        ProfileFrameSelectionSheet(
            avatarUrl = firestoreProfile?.avatar ?: user?.avatar,
            currentFrameId = equippedFrame?.id ?: "",
            availableFrames = availableFrames,
            onDismiss = { showFrameSheet = false },
            onEquipFrame = { frame ->
                viewModel.selectProfileFrame(frame.id)
                Toast.makeText(context, "${frame.name} equipped!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSearchSheet) {
        UserSearchSheet(
            userRepository = viewModel.userRepository,
            currentUserId = viewModel.currentUserId,
            onDismiss = { showSearchSheet = false },
            onUserSelected = { selected ->
                showSearchSheet = false
                visitedUser = selected
            }
        )
    }

    if (visitedUser != null) {
        VisitedUserProfileDialog(
            user = visitedUser!!,
            currentUserId = viewModel.currentUserId,
            userRepository = viewModel.userRepository,
            onDismiss = { visitedUser = null },
            onSendMessage = { target ->
                visitedUser = null
                onSendMessage(target)
            },
            onVoiceCall = { target ->
                visitedUser = null
                onVoiceCall(target)
            },
            onSendGift = { target ->
                visitedUser = null
                giftingTargetUser = target
            }
        )
    }

    if (giftingTargetUser != null) {
        com.example.ui.components.SendGiftDialog(
            senderUid = viewModel.currentUserId,
            recipientUid = giftingTargetUser!!.userId,
            recipientPublicId = giftingTargetUser!!.publicUserId,
            recipientName = giftingTargetUser!!.displayName,
            recipientAvatar = giftingTargetUser!!.avatar,
            onDismiss = { giftingTargetUser = null },
            onGiftSent = { gift, qty ->
                giftingTargetUser = null
            },
            onNavigateToRecharge = {
                giftingTargetUser = null
                onWalletClick()
            }
        )
    }

    if (showProfileFrameStoreDialog) {
        com.example.ui.components.ProfileFrameStoreDialog(
            currentUserId = viewModel.currentUserId,
            currentUserAvatar = firestoreProfile?.avatar ?: (user?.avatar ?: ""),
            currentEquippedFrameId = firestoreProfile?.profileFrameId ?: "frame_default",
            onDismiss = { showProfileFrameStoreDialog = false },
            onEquippedChanged = { frameId ->
                viewModel.selectProfileFrame(frameId)
                showProfileFrameStoreDialog = false
            },
            onNavigateToRecharge = {
                showProfileFrameStoreDialog = false
                onWalletClick()
            }
        )
    }

    if (showEditProfileDialog) {
        UserProfileSettingsDialog(
            currentDisplayName = firestoreProfile?.displayName ?: (user?.displayName ?: "VIP Member"),
            currentBio = firestoreProfile?.bio ?: (profile?.bio ?: "Voice room enthusiast and active party club member! 🎙️✨"),
            currentAvatar = firestoreProfile?.avatar ?: user?.avatar,
            currentCover = firestoreProfile?.coverImage ?: user?.coverImage,
            onDismiss = { showEditProfileDialog = false },
            onSave = { displayName, bio, avatar, cover ->
                viewModel.updateProfile(displayName, bio, avatar, cover) {
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile updated and synced with Firestore! ✨", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090414),
                        Color(0xFF130A28),
                        Color(0xFF0A0517)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        // --- 1. PROFILE HEADER SECTION ---
        val isModerator = ModerationRoles.isModeratorOrAbove(firestoreProfile?.role ?: "USER")
        ProfileHeaderSection(
            displayName = firestoreProfile?.displayName ?: (user?.displayName ?: "VIP Host"),
            username = firestoreProfile?.username ?: (user?.username ?: "vip_host"),
            uid = firestoreProfile?.publicUserId ?: (user?.publicUserId ?: "Pending"),
            avatarUrl = firestoreProfile?.avatar ?: (user?.avatar ?: ""),
            frame = equippedFrame,
            vipLevel = dynamicVipLevel,
            hostLevel = dynamicLevel,
            followersCount = firestoreProfile?.followersCount ?: (profile?.followingCount ?: 0),
            fansCount = firestoreProfile?.followingCount ?: (profile?.followersCount ?: 0),
            charmValue = dynamicDiamondBalance * 4,
            isOnline = firestoreProfile?.isOnline ?: true,
            onSettingsClick = { showSettingsSheet = true },
            onEditClick = {
                executeWithPermissionCheck {
                    showEditProfileDialog = true
                }
            },
            onFrameClick = { showProfileFrameStoreDialog = true },
            onSearchClick = { showSearchSheet = true },
            onNotificationsClick = onNavigateNotifications
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. VIP BADGE SHOWCASE CAROUSEL ---
        VipBadgeSection(
            currentVipLevel = dynamicVipLevel,
            onBadgeClick = { badge -> onVipClick() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. DYNAMIC WALLET SECTION (Coin & Diamond Cards backed by Firestore) ---
        WalletSection(
            coinBalance = dynamicCoinBalance,
            diamondBalance = dynamicDiamondBalance,
            onCoinClick = onWalletClick,
            onDiamondClick = {
                activeDialogTitle = "💎 Diamond Cashout & Exchange"
                activeDialogContent = "You have %,d Diamonds synced from Firestore. Exchange for coins or request payout to your bank.".format(dynamicDiamondBalance)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. GIFT SHOWCASE SECTION ---
        GiftShowcaseSection(
            receivedDiamonds = dynamicDiamondBalance,
            onShowcaseClick = {
                activeDialogTitle = "🎁 Luxury Gift Showcase"
                activeDialogContent = "You have received 142 Luxury Gifts across Voice Clubs, generating %,d Diamonds!".format(dynamicDiamondBalance)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 5. MENU GRID (4x2 Grid) ---
        FeatureGridSection(
            onFeatureClick = { title, desc ->
                if (title == "VIP") {
                    onVipClick()
                } else {
                    activeDialogTitle = title
                    activeDialogContent = desc
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- 6. ACTION LIST ---
        val showAgency = (firestoreProfile?.agencyId != null) || (profile?.agencyId != null) || (user?.roleId ?: 1) > 1
        val isAdmin = ModerationRoles.isAdminOrAbove(firestoreProfile?.role ?: "USER")
        ActionListSection(
            showAgency = showAgency,
            agencyName = firestoreProfile?.agencyId ?: (profile?.agencyId ?: "Star_Talent_Agency"),
            isModerator = isModerator,
            isAdmin = isAdmin,
            onActionClick = { title, desc ->
                activeDialogTitle = title
                activeDialogContent = desc
            },
            onNotificationsClick = onNavigateNotifications,
            onModerationClick = onNavigateModeration,
            onAdminClick = onNavigateAdmin,
            onOpenSettings = { showSettingsSheet = true },
            onLogoutPrompt = { showLogoutConfirm = true }
        )
    }

    // VIP Detail Dialog
    if (showVipDetailDialog != null) {
        val badge = showVipDetailDialog!!
        AlertDialog(
            onDismissRequest = { showVipDetailDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(badge.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(badge.title, fontWeight = FontWeight.Black, color = GoldPremium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(badge.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Privilege Perks:", fontWeight = FontWeight.Bold, color = TealPremium, fontSize = 13.sp)
                    badge.perks.forEach { perk ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✨", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(perk, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVipDetailDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1B1430),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Generic Feature Dialog
    if (activeDialogTitle != null) {
        AlertDialog(
            onDismissRequest = {
                activeDialogTitle = null
                activeDialogContent = null
            },
            title = { Text(activeDialogTitle ?: "", fontWeight = FontWeight.Bold, color = GoldPremium) },
            text = { Text(activeDialogContent ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.White) },
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
            },
            containerColor = Color(0xFF1B1430),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Logout Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of Great Voice Room? Your persistent session will be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout(onLogoutClick)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1B1430),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ----------------------------------------------------
// VIP Badge Data & Showcase Section
// ----------------------------------------------------
data class VipBadgeInfo(
    val id: String,
    val title: String,
    val icon: String,
    val levelReq: Int,
    val description: String,
    val perks: List<String>,
    val isUnlocked: Boolean
)

@Composable
fun VipBadgeSection(
    currentVipLevel: Int,
    onBadgeClick: (VipBadgeInfo) -> Unit
) {
    val badges = listOf(
        VipBadgeInfo(
            id = "vip_crown",
            title = "VIP $currentVipLevel Aristocrat",
            icon = "👑",
            levelReq = 1,
            description = "Status of distinguished nobility in all voice rooms.",
            perks = listOf("Golden Nameplate", "Special Entry Banner", "Stage Mic Priority"),
            isUnlocked = currentVipLevel >= 1
        ),
        VipBadgeInfo(
            id = "svip_dragon",
            title = "SVIP Dragon Sovereign",
            icon = "🐉",
            levelReq = 5,
            description = "Supreme status for high rollers and room benefactors.",
            perks = listOf("Dragon Mount Entry", "Anti-Mute & Anti-Kick", "Custom Room Frame"),
            isUnlocked = currentVipLevel >= 5
        ),
        VipBadgeInfo(
            id = "top_gifter",
            title = "Diamond Gifter",
            icon = "💎",
            levelReq = 3,
            description = "Awarded to generous gift senders who light up live stages.",
            perks = listOf("Sparkling Chat Bubble", "Marquee Gift Announcement", "Exclusive Rose Aura"),
            isUnlocked = currentVipLevel >= 3
        ),
        VipBadgeInfo(
            id = "star_singer",
            title = "Star Vocalist",
            icon = "🎙️",
            levelReq = 2,
            description = "Verified talent badge for recognized party singers.",
            perks = listOf("HD Studio Audio Enhancement", "Spotlight Equalizer", "Audience Follower Boost"),
            isUnlocked = true
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("vip_badge_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✨", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIP Prestige & Badges",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Tier Lv.$currentVipLevel",
                    color = GoldPremium,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { badge ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBadgeClick(badge) },
                        color = if (badge.isUnlocked) Color(0xFF2E204D) else Color(0xFF181325),
                        border = BorderStroke(
                            1.dp,
                            if (badge.isUnlocked) GoldPremium.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(badge.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = badge.title.split(" ").take(2).joinToString(" "),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) Color.White else Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Profile Header Section with Real VIP Frames and Search
// ----------------------------------------------------
@Composable
fun ProfileHeaderSection(
    displayName: String,
    username: String,
    uid: String,
    avatarUrl: String,
    frame: ProfileFrame?,
    vipLevel: Int,
    hostLevel: Int,
    followersCount: Int,
    fansCount: Int,
    charmValue: Long,
    isOnline: Boolean,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    onFrameClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_header_section")
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF29104D).copy(alpha = 0.6f), Color.Transparent)
                )
            )
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            
            // Top action icons: Notifications, Search, Frame, Edit, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNotificationsClick, modifier = Modifier.testTag("notifications_button")) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = GoldPremium)
                }
                IconButton(onClick = onSearchClick, modifier = Modifier.testTag("search_users_button")) {
                    Icon(Icons.Default.PersonSearch, contentDescription = "Search Users", tint = GoldPremium)
                }
                IconButton(onClick = onFrameClick, modifier = Modifier.testTag("select_frame_button")) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "VIP Frame", tint = GoldPremium)
                }
                IconButton(onClick = onEditClick, modifier = Modifier.testTag("edit_profile_button")) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit Profile", tint = Color.White)
                }
                IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("settings_button")) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Profile Picture with Equipped VIP Frame
            ProfileAvatarWithFrame(
                avatarUrl = avatarUrl,
                frame = frame,
                size = 110.dp,
                isOnline = isOnline,
                onClick = onFrameClick
            )

            Spacer(modifier = Modifier.height(14.dp))

            // User name, Badges & Permanent Numeric UID
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Verified, contentDescription = "Verified", tint = TealPremium, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Permanent Numeric Public User ID Badge with Copy
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF221A38),
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldPremium, TealPremium))),
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(uid))
                            Toast.makeText(context, "ID $uid copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ID: $uid", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPremium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy UID", modifier = Modifier.size(13.dp), tint = TealPremium)
                        }
                    }

                    // Online Presence Badge
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isOnline) "🟢 Online" else "⚪ Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) Color(0xFF00E676) else Color.LightGray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Level Badge
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

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Following", followersCount.toString())
                StatItem("Followers", fansCount.toString())
                StatItem("Charm Value", "⭐ %,d".format(charmValue))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
    }
}

// ----------------------------------------------------
// Wallet Section: Dynamic Coin & Diamond Cards backed by Firestore
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
                .height(100.dp)
                .clickable { onCoinClick() }
                .testTag("coin_wallet_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                            Text("Coin Balance", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GoldPremium
                        ) {
                            Text("+ Top Up", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "%,d".format(coinBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = GoldPremium
                    )
                }
            }
        }

        // 2. Diamond Card
        Card(
            modifier = Modifier
                .weight(1f)
                .height(100.dp)
                .clickable { onDiamondClick() }
                .testTag("diamond_wallet_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            border = BorderStroke(1.dp, CrystalDiamond.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                            Text("Diamond Earn", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CrystalDiamond
                        ) {
                            Text("Exchange", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "%,d".format(diamondBalance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = CrystalDiamond
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Feature Grid (4x2 Layout)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    Brush.linearGradient(
                        colors = listOf(TealPremium.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .border(1.dp, GoldPremium.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = GoldPremium, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
    }
}

// ----------------------------------------------------
// Action List Section
// ----------------------------------------------------
@Composable
fun ActionListSection(
    showAgency: Boolean,
    agencyName: String,
    isModerator: Boolean = false,
    isAdmin: Boolean = false,
    onActionClick: (String, String) -> Unit,
    onNotificationsClick: () -> Unit = {},
    onModerationClick: () -> Unit = {},
    onAdminClick: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onLogoutPrompt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column {
            ActionListItem(
                title = "Notifications & Activity",
                icon = Icons.Outlined.Notifications,
                onClick = onNotificationsClick
            )
            if (isModerator) {
                ActionListItem(
                    title = "🛡️ Moderator Dashboard",
                    icon = Icons.Outlined.Security,
                    tint = GoldPremium,
                    onClick = onModerationClick
                )
            }
            if (isAdmin) {
                ActionListItem(
                    title = "⚡ Platform Admin Panel",
                    icon = Icons.Outlined.AdminPanelSettings,
                    tint = GoldPremium,
                    onClick = onAdminClick
                )
            }
            ActionListItem(
                title = "Host Data Analytics",
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
                title = "Reward & Income Records",
                icon = Icons.Outlined.CardGiftcard,
                onClick = {
                    onActionClick(
                        "🎁 Reward Records",
                        "+200 Coins (Daily Login Bonus)\n+1,000 Coins (Dragon Tiger Game Win)\n+500 Coins (Host Performance Target)"
                    )
                }
            )
            ActionListItem(
                title = "Interactive Games Records",
                icon = Icons.Outlined.VideogameAsset,
                onClick = {
                    onActionClick(
                        "🎮 Interactive Games Records",
                        "Dragon vs Tiger: 18 Rounds • 8,400 Coins Won\nLucky Roulette: 12 Spins • 2 Jackpots won"
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceMenu),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
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
                        "Need assistance? 24/7 Creator Support is available at support@greatvoiceroom.com"
                    )
                }
            )
            ActionListItem(
                title = "Logout",
                icon = Icons.Outlined.ExitToApp,
                onClick = onLogoutPrompt,
                showArrow = false,
                tint = Color(0xFFFF5252)
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
    tint: Color = Color.White
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
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Go", tint = Color.White.copy(alpha = 0.5f))
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
        color = DarkSurfaceCard,
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
                        text = "💎 %,d Diamonds".format(receivedDiamonds),
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
