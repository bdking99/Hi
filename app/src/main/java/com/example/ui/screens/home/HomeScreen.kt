package com.example.ui.screens.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.RoomData
import com.example.ui.screens.room.CreateRoomDialog
import com.example.ui.screens.room.RoomViewModel
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

/**
 * HomeScreen:
 * Modern, responsive home dashboard for discovering active live voice rooms,
 * trending broadcasters, in-room games, promotional banners, and quick room creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RoomViewModel,
    onSelectRoom: (String) -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateGames: () -> Unit,
    onNavigateNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeRooms by viewModel.activeRooms.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "🔥 Popular", "🎤 Singing", "💬 Chat", "🎮 Gaming", "🇧🇩 Bangla", "⚡ Party", "Chill")

    // Filter rooms by category and search
    val filteredRooms = remember(activeRooms, selectedCategory, searchQuery) {
        activeRooms.filter { room ->
            val matchCategory = if (selectedCategory == "All") true
            else if (selectedCategory.contains("Popular")) true
            else if (selectedCategory.contains("Bangla")) room.countryFlag == "🇧🇩" || room.tag.contains("Bangla", ignoreCase = true) || room.tag.contains("Adda", ignoreCase = true)
            else room.tag.contains(selectedCategory.removePrefix("🔥 ").removePrefix("🎤 ").removePrefix("💬 ").removePrefix("🎮 ").removePrefix("⚡ "), ignoreCase = true)

            matchCategory
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0B1A))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                HomeTopBar(
                    coinBalance = currentUser.coinBalance,
                    searchQuery = searchQuery,
                    isSearchExpanded = isSearchExpanded,
                    onSearchToggle = { isSearchExpanded = !isSearchExpanded },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onNavigateWallet = onNavigateWallet,
                    onNavigateNotifications = onNavigateNotifications
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateRoomDialog = true },
                    containerColor = GoldPremium,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    modifier = Modifier.testTag("create_room_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Room")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Room", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. HERO PROMOTIONAL BANNER CAROUSEL
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HeroPromoCarousel(onBannerClick = { banner ->
                        Toast.makeText(context, "Event: ${banner.title}", Toast.LENGTH_SHORT).show()
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 2. CATEGORY PILLS ROW
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) TealPremium else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) TealPremium else Color.White.copy(alpha = 0.12f)
                                ),
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 3. FEATURED ACTIVE HOSTS CAROUSEL
                item {
                    SectionHeader(
                        title = "👑 Top Broadcasters",
                        actionLabel = "Rankings ›",
                        onAction = {}
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TopBroadcastersRow(
                        rooms = activeRooms,
                        onHostClick = { room ->
                            viewModel.selectRoom(room.id, context)
                            onSelectRoom(room.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 4. LIVE VOICE ROOMS GRID
                item {
                    SectionHeader(
                        title = "🎙️ Live Voice Rooms (${filteredRooms.size})",
                        actionLabel = "${activeRooms.size} Active",
                        onAction = {}
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (filteredRooms.isEmpty()) {
                    item {
                        EmptyRoomsState(
                            searchQuery = searchQuery,
                            onCreateRoom = { showCreateRoomDialog = true }
                        )
                    }
                } else {
                    // 2-Column Responsive Room Cards Grid
                    val chunkedRooms = filteredRooms.chunked(2)
                    items(chunkedRooms) { rowRooms ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (room in rowRooms) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ModernRoomCard(
                                        room = room,
                                        onClick = {
                                            viewModel.selectRoom(room.id, context)
                                            onSelectRoom(room.id)
                                        }
                                    )
                                }
                            }
                            // Empty spacer if row has odd number of items
                            if (rowRooms.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 5. QUICK MINI-GAMES BANNER
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader(
                        title = "🎮 Party Mini-Games",
                        actionLabel = "Play All ›",
                        onAction = onNavigateGames
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    MiniGamesRow(onGameClick = onNavigateGames)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Create Room Dialog
    if (showCreateRoomDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateRoomDialog = false },
            onCreate = { title, category, desc, cover, pass, roomType ->
                showCreateRoomDialog = false
                viewModel.createRoom(
                    title = title,
                    description = desc,
                    category = category,
                    coverUrl = cover,
                    password = pass,
                    roomType = roomType,
                    onCreated = { newRoomId ->
                        viewModel.selectRoom(newRoomId, context)
                        onSelectRoom(newRoomId)
                    }
                )
            }
        )
    }
}

// ----------------------------------------------------
// HOME TOP BAR & SEARCH
// ----------------------------------------------------

@Composable
private fun HomeTopBar(
    coinBalance: Long,
    searchQuery: String,
    isSearchExpanded: Boolean,
    onSearchToggle: () -> Unit,
    onSearchChange: (String) -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateNotifications: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF140E24))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Brand Name & Animated Wave Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(TealPremium, GoldPremium))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Great Voice Room",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = "Live Multi-Seat Voice Party",
                        color = TealPremium,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Right: Coin Balance Badge + Search & Notifications
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Coin Pill
                Surface(
                    onClick = onNavigateWallet,
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coinBalance",
                            color = GoldPremium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                // Search Icon
                IconButton(
                    onClick = onSearchToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Notification Bell
                IconButton(
                    onClick = onNavigateNotifications,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expandable Search Field
        AnimatedVisibility(visible = isSearchExpanded) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search room name, ID or host...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPremium,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TealPremium, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// HERO PROMOTIONAL BANNER CAROUSEL
// ----------------------------------------------------

private data class BannerItem(
    val title: String,
    val subtitle: String,
    val tag: String,
    val gradient: List<Color>,
    val emoji: String
)

@Composable
private fun HeroPromoCarousel(onBannerClick: (BannerItem) -> Unit) {
    val banners = listOf(
        BannerItem(
            title = "Grand Voice Gala 2026",
            subtitle = "Win 1,000,000 Coins & Exclusive VIP Badges!",
            tag = "HOT EVENT",
            gradient = listOf(Color(0xFF6A11CB), Color(0xFF2575FC)),
            emoji = "🏆"
        ),
        BannerItem(
            title = "Weekly VIP Star Tournament",
            subtitle = "Climb the leaderboard to unlock animated SVIP crown frames.",
            tag = "VIP REWARD",
            gradient = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
            emoji = "👑"
        ),
        BannerItem(
            title = "Bangla Adda & Music Fest",
            subtitle = "Live singing with top hosts from Bangladesh & Global.",
            tag = "FEATURED",
            gradient = listOf(Color(0xFF00B09B), Color(0xFF96C93D)),
            emoji = "🇧🇩"
        )
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(banners) { banner ->
            Surface(
                onClick = { onBannerClick(banner) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(300.dp)
                    .height(130.dp),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(banner.gradient))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = banner.tag,
                                color = GoldPremium,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        Column {
                            Text(
                                text = banner.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = banner.subtitle,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = banner.emoji,
                        fontSize = 44.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// TOP BROADCASTERS CAROUSEL
// ----------------------------------------------------

@Composable
private fun TopBroadcastersRow(
    rooms: List<RoomData>,
    onHostClick: (RoomData) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(rooms.take(6)) { room ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onHostClick(room) }
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = room.hostAvatar.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                        contentDescription = room.hostName,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, GoldPremium, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Live Wave Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFF1744),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 4.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = room.hostName,
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${room.viewerCount} 👥",
                    color = TealPremium,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ----------------------------------------------------
// MODERN 2-COLUMN ROOM CARD
// ----------------------------------------------------

@Composable
private fun ModernRoomCard(
    room: RoomData,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1B142D),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("room_card_${room.id}")
    ) {
        Column {
            // Cover Image + Live Overlay Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AsyncImage(
                    model = room.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600" },
                    contentDescription = room.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent, Color(0xFF1B142D))
                            )
                        )
                )

                // Top Left: Category Tag & Country Flag
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(room.countryFlag, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = room.tag,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Top Right: Live Viewer Count
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = room.viewerCount,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Left: Host Avatar Mini
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = room.hostAvatar.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                        contentDescription = room.hostName,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(1.dp, GoldPremium, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = room.hostName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Room Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = room.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${room.numericId}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TealPremium.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Join 🎙️",
                            color = TealPremium,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// MINI GAMES ROW
// ----------------------------------------------------

@Composable
private fun MiniGamesRow(onGameClick: () -> Unit) {
    val games = listOf(
        Triple("🐉 Dragon vs Tiger", "Card battle win up to 10x coins", Color(0xFFC62828)),
        Triple("🎲 Lucky Dice War", "Predict numbers & roll the jackpot", Color(0xFF283593)),
        Triple("🎡 Wheel of Fortune", "Spin daily to win diamonds & VIP", Color(0xFF6A1B9A))
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(games) { (name, desc, color) ->
            Surface(
                onClick = onGameClick,
                shape = RoundedCornerShape(16.dp),
                color = color.copy(alpha = 0.85f),
                modifier = Modifier
                    .width(190.dp)
                    .height(95.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(text = desc, color = Color.White.copy(alpha = 0.8f), fontSize = 10.5.sp, maxLines = 2)
                    Text(text = "Play Now ›", color = GoldPremium, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// EMPTY ROOMS STATE
// ----------------------------------------------------

@Composable
private fun EmptyRoomsState(
    searchQuery: String,
    onCreateRoom: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎙️", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No rooms matching '$searchQuery'" else "No active voice rooms currently",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Be the first to launch a room and invite your friends!",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCreateRoom,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPremium)
            ) {
                Text("+ Create Live Room", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String = "",
    onAction: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        if (actionLabel.isNotEmpty()) {
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = TealPremium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
