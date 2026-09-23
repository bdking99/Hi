package com.example.ui.screens.game

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.GameItem
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    currentCoins: Long = 2500L,
    vipLevel: Int = 1,
    onAddCoinsClick: () -> Unit = {},
    onCoinWon: (Long) -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State flows from ViewModel
    val games by viewModel.gamesCatalog.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val activeGame by viewModel.activeGame.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val currentRound by viewModel.currentRound.collectAsState()
    val sessionMembers by viewModel.sessionMembers.collectAsState()
    val playerBets by viewModel.playerRoundBets.collectAsState()
    val roundPhase by viewModel.roundPhase.collectAsState()
    val secondsRemaining by viewModel.secondsRemaining.collectAsState()
    val phaseStatusMessage by viewModel.phaseStatusMessage.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val history by viewModel.gameHistory.collectAsState()
    val leaderboard by viewModel.leaderboard.collectAsState()
    val activeEvents by viewModel.activeEvents.collectAsState()
    val incomingInvites by viewModel.incomingInvitations.collectAsState()

    val actualCoinBalance = wallet?.coinBalance ?: currentCoins

    // Modal Sheet / Dialog States
    var showHistorySheet by remember { mutableStateOf(false) }
    var showLeaderboardSheet by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Casino Duel", "Multipliers", "Slots & Lucky", "Board & Mini Games")
    val filteredGames = remember(games, selectedCategory) {
        if (selectedCategory == "All") games
        else games.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    // IF AN ACTIVE GAME IS SELECTED -> RENDER ARENA FULL SCREEN
    if (activeGame != null) {
        when (activeGame?.gameId) {
            "game_dragon_tiger" -> {
                DragonTigerArenaScreen(
                    currentRound = currentRound,
                    roundPhase = roundPhase,
                    secondsRemaining = secondsRemaining,
                    phaseStatusMessage = phaseStatusMessage,
                    playerBets = playerBets,
                    sessionMembers = sessionMembers,
                    userCoinBalance = actualCoinBalance,
                    onPlaceBet = { target, amount ->
                        viewModel.placeBet(
                            betTarget = target,
                            amount = amount,
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Bet placed on $target: %,d 🪙".format(amount))
                                }
                            },
                            onError = { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(err)
                                }
                            }
                        )
                    },
                    onLeaveGame = { viewModel.leaveCurrentGame() },
                    onInviteClick = { showInviteDialog = true },
                    onRechargeClick = onAddCoinsClick
                )
            }
            "game_jdi_lion" -> {
                JdiLionArenaScreen(
                    currentRound = currentRound,
                    roundPhase = roundPhase,
                    secondsRemaining = secondsRemaining,
                    phaseStatusMessage = phaseStatusMessage,
                    playerBets = playerBets,
                    sessionMembers = sessionMembers,
                    userCoinBalance = actualCoinBalance,
                    onPlaceBet = { target, amount ->
                        viewModel.placeBet(
                            betTarget = target,
                            amount = amount,
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Bet placed: %,d 🪙".format(amount))
                                }
                            },
                            onError = { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(err)
                                }
                            }
                        )
                    },
                    onLeaveGame = { viewModel.leaveCurrentGame() },
                    onInviteClick = { showInviteDialog = true },
                    onRechargeClick = onAddCoinsClick
                )
            }
            else -> {
                // Default / Generic duel arena
                DragonTigerArenaScreen(
                    currentRound = currentRound,
                    roundPhase = roundPhase,
                    secondsRemaining = secondsRemaining,
                    phaseStatusMessage = phaseStatusMessage,
                    playerBets = playerBets,
                    sessionMembers = sessionMembers,
                    userCoinBalance = actualCoinBalance,
                    onPlaceBet = { target, amount ->
                        viewModel.placeBet(
                            betTarget = target,
                            amount = amount,
                            onSuccess = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Bet placed: %,d 🪙".format(amount))
                                }
                            },
                            onError = { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(err)
                                }
                            }
                        )
                    },
                    onLeaveGame = { viewModel.leaveCurrentGame() },
                    onInviteClick = { showInviteDialog = true },
                    onRechargeClick = onAddCoinsClick
                )
            }
        }
        return
    }

    // MAIN GAME CENTER LOBBY
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF0F081D),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF160B2E))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎮 GAME CENTER",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = GoldPremium
                        )
                        Text(
                            text = "Real-Time Server Social Games",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Admin button
                        IconButton(
                            onClick = { showAdminDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                .testTag("btn_game_admin")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Admin Config",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Coin Balance Pill
                        Surface(
                            onClick = onAddCoinsClick,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.6f)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text("🪙", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%,d".format(actualCoinBalance),
                                    color = GoldPremium,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.AddCircle,
                                    contentDescription = "Recharge",
                                    tint = GoldPremium,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("game_center_lobby")
        ) {
            // 1. ACTIVE SEASONAL EVENTS BANNER CAROUSEL
            if (activeEvents.isNotEmpty()) {
                item {
                    val event = activeEvents.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF28114B)),
                        border = BorderStroke(1.5.dp, GoldPremium)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldPremium,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = event.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = event.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = event.description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // 2. QUICK ACTION TOOLBAR (History & Leaderboard)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showLeaderboardSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D184E)),
                        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("btn_leaderboard")
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Leaderboard", tint = GoldPremium, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Leaderboard", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = { showHistorySheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D184E)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp).testTag("btn_history")
                    ) {
                        Icon(Icons.Filled.History, contentDescription = "History", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("My History", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 3. CATEGORY CHIPS
            item {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCategoryFilter(category) },
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPremium,
                                containerColor = Color(0xFF22143D)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.White.copy(alpha = 0.2f),
                                selectedBorderColor = GoldPremium
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. GAMES CATALOG LIST
            item {
                Text(
                    text = "AVAILABLE GAMES (${filteredGames.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            items(filteredGames) { game ->
                GameCatalogCard(
                    game = game,
                    onPlayClick = {
                        if (game.status == "ACTIVE" && game.enabled) {
                            viewModel.enterGame(game)
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("${game.name} is currently under maintenance")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // MODAL DIALOGS
    if (showHistorySheet) {
        GameHistorySheet(
            historyItems = history,
            onDismiss = { showHistorySheet = false }
        )
    }

    if (showLeaderboardSheet) {
        GameLeaderboardSheet(
            leaderboardEntries = leaderboard,
            onDismiss = { showLeaderboardSheet = false }
        )
    }

    if (showAdminDialog) {
        GameAdminDialog(
            games = games,
            onToggleStatus = { gameId, status, isEnabled ->
                viewModel.toggleGameStatus(gameId, status, isEnabled)
            },
            onDismiss = { showAdminDialog = false }
        )
    }
}

@Composable
fun GameCatalogCard(
    game: GameItem,
    onPlayClick: () -> Unit
) {
    val isPlayable = game.status == "ACTIVE" && game.enabled

    Card(
        onClick = onPlayClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1038)
        ),
        border = BorderStroke(
            1.dp,
            if (isPlayable) GoldPremium.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("game_card_${game.gameId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game Icon / Emoji Avatar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF331B5E),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f)),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = game.emoji,
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPlayable) GoldPremium else Color(0xFFFF9800)
                    ) {
                        Text(
                            text = if (isPlayable) game.badge else "MAINTENANCE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = game.description,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Min: %,d 🪙".format(game.minBet),
                        fontSize = 11.sp,
                        color = GoldPremium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Jackpot: ${game.jackpot} 🪙",
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play / Enter Action
            Button(
                onClick = onPlayClick,
                enabled = isPlayable,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPremium,
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isPlayable) "PLAY" else "SOON",
                    color = if (isPlayable) Color.Black else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}
