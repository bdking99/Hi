package com.example.ui.screens.room

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.room.RoomViewModel
import com.example.data.model.*
import com.example.utils.AppViewModelFactory
import com.example.ui.components.SpeakerSeat
import com.example.ui.components.RequestMicrophonePermission
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LiveRoom(
    val id: String,
    val title: String,
    val hostName: String,
    val hostAvatar: String,
    val agencyName: String,
    val hostLevel: Int,
    val viewerCount: String,
    val countryFlag: String,
    val tag: String,
    val bgGradient: List<Color>
)

data class BannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val gradient: List<Color>,
    val emoji: String
)

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val charmScore: String,
    val avatarUrl: String,
    val countryFlag: String,
    val badge: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    viewModel: RoomViewModel? = null,
    onNavigateWallet: () -> Unit = {}
) {
    val activeFirestoreRooms by viewModel?.activeRooms?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    var selectedTopTab by remember { mutableIntStateOf(1) } // 0: Mine, 1: Popular, 2: Posts
    val topTabs = listOf("Mine", "Popular", "Posts")
    
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = listOf(
        "🔥 Popular", "🇧🇩 Bangladesh", "🇮🇳 India", "🇵🇰 Pakistan",
        "🇸🇦 Saudi Arabia", "🇮🇩 Indonesia", "🇺🇸 USA", "🎵 Music", "🎤 Singing", "🎮 Gaming"
    )

    // State for interactive dialogs
    var selectedRoomForLive by remember { mutableStateOf<LiveRoom?>(null) }
    var showLeaderboardSheet by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Sample Banner Data
    val banners = remember {
        listOf(
            BannerItem(
                id = "b1",
                title = "🇧🇷 Happy Brazilian Independence Day!",
                subtitle = "Join the Mega Party Carnival • 1,000,000 Coins Pool",
                tag = "FESTIVAL",
                gradient = listOf(Color(0xFF009B3A), Color(0xFF002776), Color(0xFFFFCC29)),
                emoji = "🎉"
            ),
            BannerItem(
                id = "b2",
                title = "👑 Global Voice Gala 2026",
                subtitle = "Vote for your favourite Host & win Diamond Wings",
                tag = "SUPER CUP",
                gradient = listOf(Color(0xFF7928CA), Color(0xFFFF0080)),
                emoji = "🏆"
            ),
            BannerItem(
                id = "b3",
                title = "💎 Diamond Rush Festival",
                subtitle = "20% Extra Diamonds on all instant recharges",
                tag = "PROMO",
                gradient = listOf(Color(0xFF0070F3), Color(0xFF00DFD8)),
                emoji = "💎"
            )
        )
    }

    // Top 3 Leaderboard
    val top3Users = remember {
        listOf(
            LeaderboardUser(
                rank = 2,
                name = "Prince Leo",
                charmScore = "1.8M",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                countryFlag = "🇺🇸",
                badge = "🥈 Top 2"
            ),
            LeaderboardUser(
                rank = 1,
                name = "Queen Sophia",
                charmScore = "2.4M",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                countryFlag = "🇧🇷",
                badge = "👑 Champion"
            ),
            LeaderboardUser(
                rank = 3,
                name = "Elena Rose",
                charmScore = "1.2M",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                countryFlag = "🇮🇳",
                badge = "🥉 Top 3"
            )
        )
    }

    // Live Rooms Grid Data
    val liveRooms = remember {
        listOf(
            LiveRoom(
                id = "r1",
                title = "Midnight Acoustic Jam 🎸",
                hostName = "Aria Melody",
                hostAvatar = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                agencyName = "👑 Star Agency",
                hostLevel = 34,
                viewerCount = "🔥 3.8k",
                countryFlag = "🇧🇷",
                tag = "Music",
                bgGradient = listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
            ),
            LiveRoom(
                id = "r2",
                title = "Late Night Confessions 🌙",
                hostName = "Samir Khan",
                hostAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                agencyName = "💎 Elite Club",
                hostLevel = 28,
                viewerCount = "👥 2.4k",
                countryFlag = "🇧🇩",
                tag = "Chat",
                bgGradient = listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF))
            ),
            LiveRoom(
                id = "r3",
                title = "Bollywood Karaoke Night 🎤",
                hostName = "Pooja Sharma",
                hostAvatar = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=150",
                agencyName = "⚡ Titan Stars",
                hostLevel = 45,
                viewerCount = "🔥 5.1k",
                countryFlag = "🇮🇳",
                tag = "Singing",
                bgGradient = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
            ),
            LiveRoom(
                id = "r4",
                title = "Gaming Lounge: Warzone & Chill 🎮",
                hostName = "CyberGhost",
                hostAvatar = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=150",
                agencyName = "🔥 Nexus Live",
                hostLevel = 22,
                viewerCount = "👥 1.9k",
                countryFlag = "🇵🇰",
                tag = "Gaming",
                bgGradient = listOf(Color(0xFF141E30), Color(0xFF243B55))
            ),
            LiveRoom(
                id = "r5",
                title = "Arabic Melodies & Poetry 🪕",
                hostName = "Tariq Al-Mansoor",
                hostAvatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                agencyName = "👑 Royal Club",
                hostLevel = 38,
                viewerCount = "🔥 4.2k",
                countryFlag = "🇸🇦",
                tag = "Poetry",
                bgGradient = listOf(Color(0xFF3A1C71), Color(0xFFD76D77), Color(0xFFFFAF7B))
            ),
            LiveRoom(
                id = "r6",
                title = "Indonesian Pop Acoustic 🎶",
                hostName = "Dewi Citra",
                hostAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                agencyName = "🌟 Nusantara Stars",
                hostLevel = 29,
                viewerCount = "👥 1.5k",
                countryFlag = "🇮🇩",
                tag = "Acoustic",
                bgGradient = listOf(Color(0xFF134E5E), Color(0xFF71B280))
            )
        )
    }

    val combinedRooms = remember(liveRooms, activeFirestoreRooms) {
        val firestoreMapped = activeFirestoreRooms.map { data ->
            val colorStart = try { Color(android.graphics.Color.parseColor(data.bgGradientStart)) } catch (e: Exception) { Color(0xFF1F1C2C) }
            val colorEnd = try { Color(android.graphics.Color.parseColor(data.bgGradientEnd)) } catch (e: Exception) { Color(0xFF928DAB) }
            
            LiveRoom(
                id = data.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                title = data.title,
                hostName = data.hostName,
                hostAvatar = data.hostAvatar,
                agencyName = data.agencyName,
                hostLevel = data.hostLevel,
                viewerCount = data.viewerCount,
                countryFlag = data.countryFlag,
                tag = data.tag,
                bgGradient = listOf(colorStart, colorEnd)
            )
        }
        firestoreMapped + liveRooms
    }

    val filteredRooms = remember(selectedCategoryIndex, searchQuery, combinedRooms) {
        val cat = categories[selectedCategoryIndex]
        combinedRooms.filter { room ->
            val matchesCategory = if (cat.contains("Popular")) true else {
                val cleanCat = cat.replace(Regex("[^a-zA-Z]"), "").trim().lowercase()
                room.title.lowercase().contains(cleanCat) ||
                room.tag.lowercase().contains(cleanCat) ||
                room.countryFlag.contains(cat.take(2))
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                room.title.contains(searchQuery, ignoreCase = true) ||
                room.hostName.contains(searchQuery, ignoreCase = true) ||
                room.agencyName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            RoomTopBar(
                tabs = topTabs,
                selectedIndex = selectedTopTab,
                onTabSelected = { selectedTopTab = it },
                onLeaderboardClick = { showLeaderboardSheet = true },
                onSearchClick = { showSearchDialog = true }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateRoomDialog = true },
                containerColor = TealPremium,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Room") },
                text = { Text("Create Room", fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .padding(bottom = 68.dp)
                    .testTag("create_room_fab")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("room_screen_list")
        ) {
            // 1. Auto-sliding Banner Slider
            item {
                Spacer(modifier = Modifier.height(8.dp))
                BannerCarousel(banners = banners)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Leaderboard Showcase (Top 3 in 3D frames)
            item {
                Text(
                    text = "🏆 Daily Hall of Fame",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LeaderboardTop3Showcase(
                    users = top3Users,
                    onUserClick = { user ->
                        // Preview
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Category Filter Chips
            item {
                CategoryFilterRow(
                    categories = categories,
                    selectedIndex = selectedCategoryIndex,
                    onCategorySelected = { selectedCategoryIndex = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 4. Live Broadcast Header with Live Count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3366))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Live Rooms",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "${filteredRooms.size} Active Broadcasts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 5. Room Cards Grid (2 columns)
            val chunkedRooms = filteredRooms.chunked(2)
            items(chunkedRooms) { rowRooms ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (room in rowRooms) {
                        RoomCard(
                            room = room,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedRoomForLive = room
                                viewModel?.selectRoom(room.id)
                            }
                        )
                    }
                    if (rowRooms.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Create Room Dialog
    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onCreate = { title, category, description ->
                showCreateRoomDialog = false
                viewModel?.createRoom(title, description, category) { newRoomId ->
                    val newLiveRoom = LiveRoom(
                        id = newRoomId,
                        title = title,
                        hostName = "Me (Host)",
                        hostAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                        agencyName = "Diamond VIP",
                        hostLevel = 1,
                        viewerCount = "1",
                        countryFlag = "🇺🇸",
                        tag = category,
                        bgGradient = listOf(Color(0xFF2D124D), Color(0xFF1B1035))
                    )
                    selectedRoomForLive = newLiveRoom
                    viewModel.selectRoom(newRoomId)
                }
            }
        )
    }

    // Interactive Live Voice Room Sheet
    if (selectedRoomForLive != null) {
        LiveVoiceRoomBottomSheet(
            room = selectedRoomForLive!!,
            viewModel = viewModel,
            onNavigateWallet = onNavigateWallet,
            onDismiss = {
                viewModel?.closeCurrentRoom()
                selectedRoomForLive = null
            }
        )
    }

    // Leaderboard Full Details Sheet
    if (showLeaderboardSheet) {
        LeaderboardDetailSheet(
            topUsers = top3Users,
            onDismiss = { showLeaderboardSheet = false }
        )
    }

    // Search Dialog
    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Live Rooms") },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title, host, or agency...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { showSearchDialog = false }) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    searchQuery = ""
                    showSearchDialog = false
                }) {
                    Text("Clear & Close")
                }
            }
        )
    }
}

// ----------------------------------------------------
// Top Bar Component
// ----------------------------------------------------
@Composable
fun RoomTopBar(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onLeaderboardClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 3 Top Navigation Tabs: "Mine", "Popular", "Posts"
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedIndex == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onTabSelected(index) }
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = if (isSelected) 19.sp else 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(GoldPremium, TealPremium)
                                        )
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                    }
                }
            }

            // Top Right: Leaderboard and Search Icons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onLeaderboardClick,
                    modifier = Modifier.testTag("leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Leaderboard",
                        tint = GoldPremium,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.testTag("search_rooms_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Banner Carousel with Auto-slide
// ----------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerCarousel(banners: List<BannerItem>) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll every 3.5 seconds
    LaunchedEffect(pagerState.currentPage) {
        delay(3500)
        val nextPage = (pagerState.currentPage + 1) % banners.size
        pagerState.animateScrollToPage(nextPage)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.height(130.dp)
        ) { page ->
            val banner = banners[page]
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, RoundedCornerShape(18.dp), spotColor = banner.gradient.first()),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = banner.gradient
                            )
                        )
                        .padding(16.dp)
                ) {
                    // Decorative glow & emoji
                    Text(
                        text = banner.emoji,
                        fontSize = 58.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.75f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = banner.tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Column {
                            Text(
                                text = banner.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = banner.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Pager Indicator Dots
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(banners.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (isSelected) 16.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isSelected) GoldPremium else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

// ----------------------------------------------------
// Leaderboard Top 3 Showcase (3D Metallic Frames)
// ----------------------------------------------------
@Composable
fun LeaderboardTop3Showcase(
    users: List<LeaderboardUser>,
    onUserClick: (LeaderboardUser) -> Unit
) {
    val top1 = users.firstOrNull { it.rank == 1 }
    val top2 = users.firstOrNull { it.rank == 2 }
    val top3 = users.firstOrNull { it.rank == 3 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // Rank 2 (Silver)
        if (top2 != null) {
            LeaderboardTopItem(
                user = top2,
                frameColor = Color(0xFFC0C0C0),
                crownIcon = "🥈",
                avatarSize = 58.dp,
                onClick = { onUserClick(top2) }
            )
        }

        // Rank 1 (Gold, Center, Taller)
        if (top1 != null) {
            LeaderboardTopItem(
                user = top1,
                frameColor = GoldPremium,
                crownIcon = "👑",
                avatarSize = 74.dp,
                isTop1 = true,
                onClick = { onUserClick(top1) }
            )
        }

        // Rank 3 (Bronze)
        if (top3 != null) {
            LeaderboardTopItem(
                user = top3,
                frameColor = Color(0xFFCD7F32),
                crownIcon = "🥉",
                avatarSize = 58.dp,
                onClick = { onUserClick(top3) }
            )
        }
    }
}

@Composable
fun LeaderboardTopItem(
    user: LeaderboardUser,
    frameColor: Color,
    crownIcon: String,
    avatarSize: androidx.compose.ui.unit.Dp,
    isTop1: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(bottom = if (isTop1) 0.dp else 8.dp)
    ) {
        Text(text = crownIcon, fontSize = if (isTop1) 28.sp else 22.sp)

        // 3D Metallic Ring Frame
        Box(
            modifier = Modifier
                .size(avatarSize + 10.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(frameColor.copy(alpha = 0.8f), frameColor, Color.Transparent)
                    )
                )
                .border(
                    width = if (isTop1) 3.5.dp else 2.5.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(frameColor, Color.White, frameColor, frameColor.copy(alpha = 0.5f))
                    ),
                    shape = CircleShape
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
            )

            // Country Flag Badge
            Text(
                text = user.countryFlag,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = GoldPremium, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = user.charmScore,
                style = MaterialTheme.typography.labelSmall,
                color = GoldPremium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ----------------------------------------------------
// Category Filter Row
// ----------------------------------------------------
@Composable
fun CategoryFilterRow(
    categories: List<String>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(categories) { index, category ->
            val isSelected = selectedIndex == index
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(index) },
                label = {
                    Text(
                        text = category,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealPremium.copy(alpha = 0.2f),
                    selectedLabelColor = TealPremium
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isSelected) TealPremium else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        }
    }
}

// ----------------------------------------------------
// Room Card (Grid Item)
// ----------------------------------------------------
@Composable
fun RoomCard(
    room: LiveRoom,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(235.dp)
            .clickable { onClick() }
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image / Gradient
            AsyncImage(
                model = room.hostAvatar,
                contentDescription = room.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay for Readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top Badges: Live pulsating tag + Viewer count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live indicator with pulsating animation
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alphaPulse by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF3366).copy(alpha = alphaPulse)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Viewer count
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = room.viewerCount,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom Content: Title, Host name, Agency, Host Level, Equalizer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = room.hostName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = room.countryFlag, fontSize = 10.sp)
                    }

                    // Host Level Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldPremium.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "Lv.${room.hostLevel}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.agencyName,
                        fontSize = 10.sp,
                        color = TealPremium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    // Animated Equalizer Visualizer
                    EqualizerVisualizer()
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Explicit Join Button
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("join_button_${room.id}")
                ) {
                    Text(
                        text = "Join",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Equalizer Visualizer Bars
// ----------------------------------------------------
@Composable
fun EqualizerVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "h3"
    )

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(modifier = Modifier.width(2.5.dp).height(h1.dp).background(TealPremium, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.5.dp).height(h2.dp).background(GoldPremium, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(2.5.dp).height(h3.dp).background(TealPremium, RoundedCornerShape(1.dp)))
    }
}

data class VoiceSeatState(
    val seatIndex: Int,
    val name: String? = null,
    val avatar: String? = null,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val userId: String? = null
)

// ----------------------------------------------------
// Interactive Live Voice Room BottomSheet (Real-Time Firestore)
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveVoiceRoomBottomSheet(
    room: LiveRoom,
    viewModel: RoomViewModel? = null,
    onNavigateWallet: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val currentRoomData by viewModel?.currentRoom?.collectAsState() ?: remember { mutableStateOf(null) }
    val realChatMessages by viewModel?.roomChat?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val latestGift by viewModel?.latestRoomGift?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentUser by viewModel?.currentUserProfile?.collectAsState() ?: remember { mutableStateOf(FirebaseUserProfile()) }
    val visitedProfile by viewModel?.visitedUserProfile?.collectAsState() ?: remember { mutableStateOf(null) }
    val visitedGifts by viewModel?.visitedUserGifts?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val uiNotice by viewModel?.uiNotice?.collectAsState() ?: remember { mutableStateOf(null) }

    var inputComment by remember { mutableStateOf("") }
    var showGiftDialog by remember { mutableStateOf(false) }
    var giftRecipientId by remember { mutableStateOf(room.id) }
    var giftRecipientName by remember { mutableStateOf(room.hostName) }
    var showParticipantList by remember { mutableStateOf(false) }
    var celebrationGift by remember { mutableStateOf<GiftTransaction?>(null) }

    // Microphone runtime permission state
    val context = LocalContext.current
    var showMicPermissionRationale by remember { mutableStateOf(false) }
    var pendingMicAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun checkAndExecuteWithMicPermission(action: () -> Unit) {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            action()
        } else {
            pendingMicAction = action
            showMicPermissionRationale = true
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingMicAction?.invoke()
        }
        pendingMicAction = null
    }

    if (showMicPermissionRationale) {
        AlertDialog(
            onDismissRequest = {
                showMicPermissionRationale = false
                pendingMicAction = null
            },
            icon = {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Microphone Permission",
                    tint = TealPremium,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Microphone Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Great Voice Room needs microphone access so you can speak on the live stage and stream your voice to everyone in the room in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMicPermissionRationale = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
                ) {
                    Text("Allow Microphone", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMicPermissionRationale = false
                        pendingMicAction = null
                    }
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1E2430),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Floating celebration banner listener
    LaunchedEffect(latestGift) {
        if (latestGift != null) {
            celebrationGift = latestGift
            kotlinx.coroutines.delay(4500)
            celebrationGift = null
        }
    }

    // Determine seat states (Firestore or mapped fallback)
    val seats = remember(currentRoomData) {
        if (currentRoomData != null && currentRoomData!!.seats.isNotEmpty()) {
            currentRoomData!!.seats
        } else {
            listOf(
                SeatData(seatIndex = 0, userId = "HOST_1001", userName = room.hostName, userAvatar = room.hostAvatar, isSpeaking = true, isMuted = false),
                SeatData(seatIndex = 1, userId = "USER_2002", userName = "Sophia", userAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", isSpeaking = false, isMuted = false),
                SeatData(seatIndex = 2, userId = "USER_3003", userName = "Leo", userAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", isSpeaking = true, isMuted = false),
                SeatData(seatIndex = 3),
                SeatData(seatIndex = 4, userId = "USER_4004", userName = "Elena", userAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", isSpeaking = false, isMuted = true),
                SeatData(seatIndex = 5),
                SeatData(seatIndex = 6),
                SeatData(seatIndex = 7)
            )
        }
    }

    val mySeat = seats.firstOrNull { it.userId == currentUser.userId }
    val mySeatIndex = mySeat?.seatIndex
    val isMyMicMuted = mySeat?.isMuted ?: false
    val isHost = (currentRoomData?.hostId == currentUser.userId) || (room.hostName == currentUser.displayName)

    // 1. Visited User Profile Dialog
    if (visitedProfile != null) {
        UserProfileVisitDialog(
            user = visitedProfile!!,
            isHost = isHost,
            isMe = visitedProfile!!.userId == currentUser.userId,
            receivedGifts = visitedGifts,
            onDismiss = { viewModel?.clearVisitedUser() },
            onSendGiftClick = {
                giftRecipientId = visitedProfile!!.userId
                giftRecipientName = visitedProfile!!.displayName
                viewModel?.clearVisitedUser()
                showGiftDialog = true
            },
            onMuteSpeakerClick = {
                val seat = seats.firstOrNull { it.userId == visitedProfile!!.userId }
                if (seat != null) {
                    viewModel?.hostControlSeat(room.id, seat.seatIndex, kick = false, mute = !seat.isMuted)
                }
                viewModel?.clearVisitedUser()
            },
            onKickSpeakerClick = {
                val seat = seats.firstOrNull { it.userId == visitedProfile!!.userId }
                if (seat != null) {
                    viewModel?.hostControlSeat(room.id, seat.seatIndex, kick = true, mute = false)
                }
                viewModel?.clearVisitedUser()
            }
        )
    }

    // 2. Send Gift Dialog
    if (showGiftDialog) {
        SendGiftDialog(
            recipientName = giftRecipientName,
            recipientId = giftRecipientId,
            userCoins = currentUser.coinBalance,
            onDismiss = { showGiftDialog = false },
            onSend = { gift ->
                viewModel?.sendGift(
                    roomId = room.id,
                    recipientId = giftRecipientId,
                    recipientName = giftRecipientName,
                    giftId = gift.id,
                    giftName = gift.name,
                    giftEmoji = gift.emoji,
                    coins = gift.costCoins
                )
                showGiftDialog = false
            },
            onRechargeClick = {
                showGiftDialog = false
                onNavigateWallet()
            }
        )
    }

    // 3. Participant List Dialog
    if (showParticipantList) {
        val participants: List<RoomParticipant> = remember(seats, currentUser) {
            val speakers = seats.filter { it.userId != null }.map { seat ->
                RoomParticipant(
                    userId = seat.userId ?: "",
                    displayName = seat.userName ?: "Speaker",
                    avatarUrl = seat.userAvatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    isMuted = seat.isMuted,
                    isSpeaking = seat.isSpeaking,
                    role = if (seat.seatIndex == 0) "Host" else "Speaker",
                    level = 10,
                    vipLevel = 2
                )
            }
            val audience = listOf(
                RoomParticipant(userId = "AUD_1", displayName = "Marcus Aurelius", avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150", role = "VIP", level = 15, vipLevel = 3),
                RoomParticipant(userId = "AUD_2", displayName = "Aria Vance", avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150", role = "Listener", level = 6, vipLevel = 1),
                RoomParticipant(userId = "AUD_3", displayName = "Tariq Zaman", avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", role = "Listener", level = 4, vipLevel = 0)
            )
            speakers + audience
        }

        AlertDialog(
            onDismissRequest = { showParticipantList = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(480.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF161426),
                border = BorderStroke(1.dp, TealPremium.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Room Participants (${participants.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(onClick = { showParticipantList = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ParticipantList(
                        participants = participants,
                        onUserClick = { user ->
                            showParticipantList = false
                            viewModel?.visitUserProfile(user.userId)
                        },
                        onSendGiftClick = { user ->
                            showParticipantList = false
                            giftRecipientId = user.userId
                            giftRecipientName = user.displayName
                            showGiftDialog = true
                        }
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF131022),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .testTag("live_voice_room_sheet")
        ) {
            // 1. Room Header (Host info, Tag, Online count, Participants button, Leave button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, GoldPremium, CircleShape)
                            .clickable {
                                val hostSeat = seats.firstOrNull { it.seatIndex == 0 }
                                val hostId = hostSeat?.userId ?: currentRoomData?.hostId ?: "HOST_1001"
                                viewModel?.visitUserProfile(hostId)
                            }
                    ) {
                        AsyncImage(
                            model = room.hostAvatar,
                            contentDescription = room.hostName,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentRoomData?.title ?: room.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${room.hostName} • ${room.tag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPremium
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Participants Button
                    IconButton(
                        onClick = { showParticipantList = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .testTag("open_participants_button")
                    ) {
                        Icon(Icons.Default.People, contentDescription = "Participants", tint = TealPremium)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Leave Room
                    IconButton(
                        onClick = {
                            if (mySeatIndex != null) {
                                viewModel?.leaveSeat(room.id, mySeatIndex)
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF3B30).copy(alpha = 0.15f))
                            .testTag("leave_room_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Leave", tint = Color(0xFFFF5252))
                    }
                }
            }

            // Notice Banner
            if (uiNotice != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TealPremium.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, TealPremium.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiNotice!!,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Floating Celebration Luxury Gift Banner
            AnimatedVisibility(
                visible = celebrationGift != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (celebrationGift != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.5.dp, GoldPremium),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF5D3004), Color(0xFF1E1236), Color(0xFF044843))
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(celebrationGift!!.giftEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "✨ ${celebrationGift!!.senderName} gifted ${celebrationGift!!.giftName} to ${celebrationGift!!.receiverName}! ✨",
                                    color = GoldPremium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 8 Stage Voice Seats Responsive Grid (Material 3 SpeakerSeat Cards with Custom Shapes & Elevation)
            Text(
                text = "🎙️ Live Stage Seats (Tap empty seat to speak, tap user to visit)",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Responsive 4-Column Grid for the 8 Stage Speaker Seats (0..7)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Row of 4 SpeakerSeats (Seats 0..3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 0..3) {
                        val seat = seats.getOrNull(i) ?: SeatData(seatIndex = i)
                        val isOccupied = seat.userId != null
                        SpeakerSeat(
                            seatNumber = i,
                            label = if (i == 0) "Host 👑" else "Seat ${i + 1}",
                            userName = if (isOccupied) (seat.userName ?: "Speaker") else "Empty",
                            avatarUrl = seat.userAvatar,
                            isOccupied = isOccupied,
                            isSpeaking = seat.isSpeaking,
                            isMuted = seat.isMuted,
                            onClick = {
                                if (isOccupied) {
                                    viewModel?.visitUserProfile(seat.userId ?: "")
                                } else {
                                    checkAndExecuteWithMicPermission {
                                        viewModel?.takeSeat(room.id, i)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Bottom Row of 4 SpeakerSeats (Seats 4..7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 4..7) {
                        val seat = seats.getOrNull(i) ?: SeatData(seatIndex = i)
                        val isOccupied = seat.userId != null
                        SpeakerSeat(
                            seatNumber = i,
                            label = "Seat ${i + 1}",
                            userName = if (isOccupied) (seat.userName ?: "Speaker") else "Empty",
                            avatarUrl = seat.userAvatar,
                            isOccupied = isOccupied,
                            isSpeaking = seat.isSpeaking,
                            isMuted = seat.isMuted,
                            onClick = {
                                if (isOccupied) {
                                    viewModel?.visitUserProfile(seat.userId ?: "")
                                } else {
                                    checkAndExecuteWithMicPermission {
                                        viewModel?.takeSeat(room.id, i)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Live Chat Messages Stream
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White.copy(alpha = 0.04f)
            ) {
                val chatListState = rememberLazyListState()
                val displayMessages = if (realChatMessages.isNotEmpty()) {
                    realChatMessages
                } else {
                    listOf(
                        ChatMessage(id = "c1", senderName = "System", text = "Welcome to ${room.title}! 🎙️ Follow room rules.", isSystem = true),
                        ChatMessage(id = "c2", senderName = "Sophia", text = "Hey room! Glad to be here ❤️"),
                        ChatMessage(id = "c3", senderName = "Leo", text = "Awesome music stream!")
                    )
                }

                LaunchedEffect(displayMessages.size) {
                    if (displayMessages.isNotEmpty()) {
                        chatListState.animateScrollToItem(displayMessages.size - 1)
                    }
                }

                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayMessages) { msg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (msg.senderId.isNotBlank()) {
                                        viewModel?.visitUserProfile(msg.senderId)
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${msg.senderName}: ",
                                color = if (msg.isSystem) TealPremium else Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = msg.text,
                                color = if (msg.isSystem) TealPremium else Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Interactive Bottom Controls:
            // - If user is on stage: Mute/Unmute Mic + Step Down
            // - Comment text field + Send
            // - Quick gifts (🌹 10, 👑 500, 🚀 1,000)
            // - Gift Catalog button 🎁
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speaker mic toggle button
                if (mySeatIndex != null) {
                    IconButton(
                        onClick = {
                            if (isMyMicMuted) {
                                // Request mic permission if unmuting
                                checkAndExecuteWithMicPermission {
                                    viewModel?.toggleMicMute(room.id, mySeatIndex, false)
                                }
                            } else {
                                viewModel?.toggleMicMute(room.id, mySeatIndex, true)
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isMyMicMuted) Color(0xFFFF3B30).copy(alpha = 0.2f) else TealPremium.copy(alpha = 0.2f))
                            .border(1.dp, if (isMyMicMuted) Color(0xFFFF3B30) else TealPremium, CircleShape)
                            .testTag("toggle_my_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isMyMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMyMicMuted) "Unmute Mic" else "Mute Mic",
                            tint = if (isMyMicMuted) Color(0xFFFF5252) else TealPremium,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Chat Input Field
                OutlinedTextField(
                    value = inputComment,
                    onValueChange = { inputComment = it },
                    placeholder = { Text("Say something...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPremium,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (inputComment.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel?.sendChat(room.id, inputComment)
                                inputComment = ""
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = TealPremium)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Gift 1: Rose (10 coins)
                IconButton(
                    onClick = {
                        viewModel?.sendGift(
                            roomId = room.id,
                            recipientId = room.id,
                            recipientName = room.hostName,
                            giftId = "gift_rose",
                            giftName = "Rose",
                            giftEmoji = "🌹",
                            coins = 10
                        )
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Text("🌹", fontSize = 20.sp)
                }

                // Quick Gift 2: Crown (5000 coins)
                IconButton(
                    onClick = {
                        viewModel?.sendGift(
                            roomId = room.id,
                            recipientId = room.id,
                            recipientName = room.hostName,
                            giftId = "gift_crown",
                            giftName = "Royal Crown",
                            giftEmoji = "👑",
                            coins = 5000
                        )
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Text("👑", fontSize = 20.sp)
                }

                // Full Gift Catalog Button 🎁
                IconButton(
                    onClick = {
                        giftRecipientId = room.id
                        giftRecipientName = room.hostName
                        showGiftDialog = true
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(GoldPremium.copy(alpha = 0.2f))
                        .border(1.dp, GoldPremium, CircleShape)
                        .testTag("open_gift_catalog_btn")
                ) {
                    Text("🎁", fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun VoiceSeatItem(
    label: String,
    name: String,
    avatar: String?,
    isSpeaking: Boolean,
    isMuted: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (avatar != null) Color(0xFF221D38) else Color.White.copy(alpha = 0.08f))
                .border(
                    width = if (isSpeaking) 2.5.dp else 1.dp,
                    color = if (isSpeaking) GoldPremium else if (avatar != null) TealPremium.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatar != null) {
                AsyncImage(
                    model = avatar,
                    contentDescription = name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Empty seat",
                    tint = TealPremium.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isMuted && avatar != null) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .border(0.5.dp, Color.Red, CircleShape)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(11.dp)
                    )
                }
            } else if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                        .border(1.dp, Color.White, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// ----------------------------------------------------
// Leaderboard Detail Sheet
// ----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardDetailSheet(
    topUsers: List<LeaderboardUser>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161922)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "🏆 Global Hall of Fame Rankings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Real-time charm score rankings updated every 10 minutes",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            topUsers.forEach { user ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222634)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${user.rank}", fontWeight = FontWeight.ExtraBold, color = if (user.rank == 1) GoldPremium else Color.White, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            AsyncImage(model = user.avatarUrl, contentDescription = user.name, modifier = Modifier.size(42.dp).clip(CircleShape))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, fontWeight = FontWeight.Bold, color = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(user.countryFlag)
                                }
                                Text(user.badge, color = GoldPremium, fontSize = 11.sp)
                            }
                        }
                        Text("⭐ ${user.charmScore}", fontWeight = FontWeight.Bold, color = TealPremium)
                    }
                }
            }
        }
    }
}
