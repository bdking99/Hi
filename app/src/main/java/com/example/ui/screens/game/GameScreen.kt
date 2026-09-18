package com.example.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MiniGame(
    val id: String,
    val name: String,
    val subtitle: String,
    val jackpot: String,
    val playersCount: String,
    val emoji: String,
    val themeGradient: List<Color>,
    val badge: String
)

data class WheelPrize(
    val label: String,
    val rewardCoins: Long,
    val rewardDiamonds: Long,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    currentCoins: Long = 158400L,
    vipLevel: Int = 3,
    onAddCoinsClick: () -> Unit = {},
    onCoinWon: (Long) -> Unit = {}
) {
    var coinBalance by remember { mutableLongStateOf(currentCoins) }
    var diamondBalance by remember { mutableLongStateOf(4820L) }
    val coroutineScope = rememberCoroutineScope()

    // Interactive Roulette Wheel State
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var wonPrize by remember { mutableStateOf<WheelPrize?>(null) }
    var showWonDialog by remember { mutableStateOf(false) }
    var activeMiniGame by remember { mutableStateOf<MiniGame?>(null) }
    var showRechargeModal by remember { mutableStateOf(false) }

    val wheelPrizes = remember {
        listOf(
            WheelPrize("500 Coins", 500, 0, Color(0xFFFFB300)),
            WheelPrize("50 Diamonds", 0, 50, Color(0xFF00E5FF)),
            WheelPrize("1,000 Coins", 1000, 0, Color(0xFFFF7043)),
            WheelPrize("Free Spin", 200, 0, Color(0xFFAB47BC)),
            WheelPrize("2,500 Coins", 2500, 0, Color(0xFF26A69A)),
            WheelPrize("100 Diamonds", 0, 100, Color(0xFF29B6F6)),
            WheelPrize("10,000 JACKPOT", 10000, 500, GoldPremium),
            WheelPrize("Mystery Box", 1500, 20, Color(0xFFEC407A))
        )
    }

    val miniGames = remember {
        listOf(
            MiniGame(
                id = "g1",
                name = "Zeus Slot",
                subtitle = "Olympus Lightning Jackpot",
                jackpot = "4,850,200",
                playersCount = "🔥 4.2k",
                emoji = "⚡",
                themeGradient = listOf(Color(0xFF200122), Color(0xFF6f0000)),
                badge = "HOT JACKPOT"
            ),
            MiniGame(
                id = "g2",
                name = "Luxury Car",
                subtitle = "Supercar Roulette",
                jackpot = "2,380,000",
                playersCount = "🏎️ 3.1k",
                emoji = "🏎️",
                themeGradient = listOf(Color(0xFF0f0c29), Color(0xFF302b63), Color(0xFF24243e)),
                badge = "LIVE BET"
            ),
            MiniGame(
                id = "g3",
                name = "Cleopatra",
                subtitle = "Pharaoh's Treasure Tomb",
                jackpot = "3,120,400",
                playersCount = "👑 2.8k",
                emoji = "👑",
                themeGradient = listOf(Color(0xFFb29f06), Color(0xFF000000)),
                badge = "POPULAR"
            ),
            MiniGame(
                id = "g4",
                name = "Lucky 777 Fruit",
                subtitle = "Classic Vegas 3-Reel Slots",
                jackpot = "1,940,000",
                playersCount = "🍒 2.2k",
                emoji = "🍒",
                themeGradient = listOf(Color(0xFF8E0E00), Color(0xFF1F1C18)),
                badge = "CLASSIC"
            ),
            MiniGame(
                id = "g5",
                name = "Greedy Pirate",
                subtitle = "Pirate Island Dice & Chest",
                jackpot = "980,000",
                playersCount = "🏴‍☠️ 1.7k",
                emoji = "🏴‍☠️",
                themeGradient = listOf(Color(0xFF16222A), Color(0xFF3A6073)),
                badge = "x50 BONUS"
            ),
            MiniGame(
                id = "g6",
                name = "Dragon vs Tiger",
                subtitle = "High-Stakes Live Duel",
                jackpot = "5,400,000",
                playersCount = "🐉 5.6k",
                emoji = "🐉",
                themeGradient = listOf(Color(0xFF4b1248), Color(0xFFf0c27b)),
                badge = "VIP HIGH ROLLER"
            )
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Header: Coin balance with Add (+) button and VIP level badge
            GameHeader(
                coinBalance = coinBalance,
                vipLevel = vipLevel,
                onAddCoins = { showRechargeModal = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("game_screen_list")
        ) {
            // Live Winner Announcement Ticker
            item {
                LiveWinnerTicker()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Attraction: 3D Roulette Wheel
            item {
                Text(
                    text = "🎰 3D Lucky Roulette Wheel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = "Spin now for guaranteed Coins, Diamonds & Mega Jackpots!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Center Roulette Wheel Container
                RouletteWheelSection(
                    rotation = rotation.value,
                    isSpinning = isSpinning,
                    onSpinClick = {
                        if (!isSpinning) {
                            coroutineScope.launch {
                                isSpinning = true
                                // Random winning index
                                val winningIndex = (0 until wheelPrizes.size).random()
                                val targetDegrees = 360f * 6 + (winningIndex * (360f / wheelPrizes.size))
                                rotation.snapTo(rotation.value % 360f)
                                rotation.animateTo(
                                    targetValue = rotation.value + targetDegrees,
                                    animationSpec = tween(
                                        durationMillis = 4000,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                val prize = wheelPrizes[winningIndex]
                                wonPrize = prize
                                coinBalance += prize.rewardCoins
                                diamondBalance += prize.rewardDiamonds
                                onCoinWon(prize.rewardCoins)
                                isSpinning = false
                                showWonDialog = true
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Featured Mini Games Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎮 Voice Arcade Games",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time Multiplayers",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealPremium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 2-column Games Grid
            val chunkedGames = miniGames.chunked(2)
            items(chunkedGames) { rowGames ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (game in rowGames) {
                        GameCard(
                            game = game,
                            modifier = Modifier.weight(1f),
                            onPlayClick = { activeMiniGame = game }
                        )
                    }
                    if (rowGames.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Won Prize Celebration Dialog
    if (showWonDialog && wonPrize != null) {
        AlertDialog(
            onDismissRequest = { showWonDialog = false },
            title = {
                Text(
                    "🎉 Congratulations!",
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldPremium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🎰", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You won ${wonPrize!!.label}!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your updated balance is ${coinBalance} Coins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showWonDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
                ) {
                    Text("Claim Reward 💰", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Playable Mini-Game Dialog (e.g. Zeus Slot Machine)
    if (activeMiniGame != null) {
        MiniGamePlayDialog(
            game = activeMiniGame!!,
            coinBalance = coinBalance,
            onDismiss = { activeMiniGame = null },
            onWin = { coinsWon ->
                coinBalance += coinsWon
                onCoinWon(coinsWon)
            }
        )
    }

    // Recharge / Add Coins Dialog
    if (showRechargeModal) {
        RechargeCoinsDialog(
            currentCoins = coinBalance,
            onDismiss = { showRechargeModal = false },
            onRecharge = { addedCoins ->
                coinBalance += addedCoins
                onCoinWon(addedCoins)
                showRechargeModal = false
            }
        )
    }
}

// ----------------------------------------------------
// Game Header: Coin Balance + Add (+) & VIP Badge
// ----------------------------------------------------
@Composable
fun GameHeader(
    coinBalance: Long,
    vipLevel: Int,
    onAddCoins: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coin Balance with Add (+) Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onAddCoins() }
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%,d".format(coinBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldPremium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(GoldPremium),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Coins",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // VIP Level Badge + Rule Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF7928CA), Color(0xFFFF0080))))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👑", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIP $vipLevel",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = { /* Rules */ }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Rules",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Live Winner Ticker
// ----------------------------------------------------
@Composable
fun LiveWinnerTicker() {
    val winners = remember {
        listOf(
            "🎉 Queen Sophia just won 10,000 Coins in Lucky Roulette!",
            "⚡ Leo hit the 50x Mega Jackpot in Zeus Slot!",
            "🏎️ CyberGhost took home 8,000 Coins on Luxury Car!",
            "💎 Tariq unlocked 500 Diamonds on Treasure Chest!"
        )
    }
    var currentWinnerIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentWinnerIndex = (currentWinnerIndex + 1) % winners.size
        }
    }

    Surface(
        color = GoldPremium.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📢", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedContent(
                targetState = winners[currentWinnerIndex],
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = "ticker"
            ) { winnerText ->
                Text(
                    text = winnerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPremium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ----------------------------------------------------
// 3D Roulette Wheel Section
// ----------------------------------------------------
@Composable
fun RouletteWheelSection(
    rotation: Float,
    isSpinning: Boolean,
    onSpinClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glowing Halo Rim
        Box(
            modifier = Modifier
                .size(290.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2A1B4E),
                            Color(0xFF140D27),
                            Color(0xFF0D061A)
                        )
                    )
                )
                .border(
                    width = 6.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(GoldPremium, Color.White, GoldPremium, Color(0xFFFFA000))
                    ),
                    shape = CircleShape
                )
                .shadow(16.dp, CircleShape, spotColor = GoldPremium)
        )

        // Spinning Wheel Surface with 8 Multiplier Segments
        Box(
            modifier = Modifier
                .size(265.dp)
                .clip(CircleShape)
                .rotate(rotation)
                .background(Color(0xFF1F1235))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val numSegments = 8
                val sweepAngle = 360f / numSegments
                val colors = listOf(
                    Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFFBC02D),
                    Color(0xFF7B1FA2), Color(0xFFE64A19), Color(0xFF0097A7), Color(0xFFC2185B)
                )
                for (i in 0 until numSegments) {
                    drawArc(
                        color = colors[i % colors.size],
                        startAngle = i * sweepAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                }
                // Outer ring divider
                drawCircle(
                    color = GoldPremium,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Segment Label Icons
            val labels = listOf("500🪙", "50💎", "1000🪙", "🔄 Free", "2500🪙", "100💎", "💰10K", "🎁 Box")
            labels.forEachIndexed { index, label ->
                val angleRad = (index * 45f + 22.5f) * (PI / 180.0)
                val radius = 95.0
                val x = (132.5 + radius * cos(angleRad)).dp
                val y = (132.5 + radius * sin(angleRad)).dp
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier
                        .offset(x - 18.dp, y - 8.dp)
                        .shadow(2.dp)
                )
            }
        }

        // Center 3D Spin Button
        val infinitePulse = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infinitePulse.animateFloat(
            initialValue = 1f, targetValue = 1.06f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )

        Button(
            onClick = onSpinClick,
            enabled = !isSpinning,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = GoldPremium),
            modifier = Modifier
                .size(76.dp)
                .shadow(10.dp, CircleShape, spotColor = GoldPremium)
                .border(3.dp, Color.White, CircleShape),
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isSpinning) "..." else "SPIN",
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    fontSize = 15.sp
                )
            }
        }

        // Top Gold Indicator Pointer
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 4.dp)
                .size(24.dp)
        ) {
            Text("🔻", fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
        }
    }
}

// ----------------------------------------------------
// Game Card (Voice Arcade Games)
// ----------------------------------------------------
@Composable
fun GameCard(
    game: MiniGame,
    modifier: Modifier = Modifier,
    onPlayClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(175.dp)
            .clickable { onPlayClick() }
            .shadow(6.dp, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(game.themeGradient))
                .padding(12.dp)
        ) {
            // Badge on Top Right
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = game.badge,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPremium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Game Emoji Icon
            Text(
                text = game.emoji,
                fontSize = 42.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
            )

            // Bottom Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Pool: 🪙 ${game.jackpot}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldPremium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = game.playersCount,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPremium,
                        modifier = Modifier.clickable { onPlayClick() }
                    ) {
                        Text(
                            text = "PLAY",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Playable Mini-Game Dialog (Slot Machine)
// ----------------------------------------------------
@Composable
fun MiniGamePlayDialog(
    game: MiniGame,
    coinBalance: Long,
    onDismiss: () -> Unit,
    onWin: (Long) -> Unit
) {
    val reelItems = listOf("⚡", "👑", "🍒", "💎", "7️⃣", "🔔")
    var reel1 by remember { mutableStateOf("⚡") }
    var reel2 by remember { mutableStateOf("⚡") }
    var reel3 by remember { mutableStateOf("⚡") }
    var isSpinning by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("Match 3 symbols to win up to 5,000 Coins!") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(game.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(game.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Current Pool: 🪙 ${game.jackpot}",
                    color = GoldPremium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 3 Reels Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(2.dp, GoldPremium, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(reel1, fontSize = 42.sp)
                    Text(reel2, fontSize = 42.sp)
                    Text(reel3, fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = resultMessage,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSpinning) {
                        coroutineScope.launch {
                            isSpinning = true
                            resultMessage = "Spinning reels..."
                            repeat(8) {
                                reel1 = reelItems.random()
                                reel2 = reelItems.random()
                                reel3 = reelItems.random()
                                delay(120)
                            }
                            // Final result
                            reel1 = reelItems.random()
                            reel2 = reelItems.random()
                            reel3 = reelItems.random()

                            if (reel1 == reel2 && reel2 == reel3) {
                                resultMessage = "🎉 JACKPOT! 3x $reel1 MATCH! Won 5,000 Coins!"
                                onWin(5000)
                            } else if (reel1 == reel2 || reel2 == reel3) {
                                resultMessage = "✨ Pair Match! Won 500 Coins!"
                                onWin(500)
                            } else {
                                resultMessage = "Almost! Spin again to win!"
                            }
                            isSpinning = false
                        }
                    }
                },
                enabled = !isSpinning,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPremium)
            ) {
                Text(if (isSpinning) "Spinning..." else "Spin (100 Coins)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ----------------------------------------------------
// Recharge Coins Dialog
// ----------------------------------------------------
@Composable
fun RechargeCoinsDialog(
    currentCoins: Long,
    onDismiss: () -> Unit,
    onRecharge: (Long) -> Unit
) {
    val packages = listOf(
        Pair("10,000 Coins", 10000L to "$0.99"),
        Pair("50,000 Coins (+10% Bonus)", 55000L to "$4.99"),
        Pair("120,000 Coins (+20% Bonus)", 144000L to "$9.99"),
        Pair("300,000 Coins (VIP Best Deal)", 360000L to "$24.99")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💰 Coin Store & Top-Up") },
        text = {
            Column {
                Text("Select an instant package to top up your account balance:")
                Spacer(modifier = Modifier.height(12.dp))
                packages.forEach { (title, rewardAndPrice) ->
                    val (coins, price) = rewardAndPrice
                    OutlinedButton(
                        onClick = { onRecharge(coins) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(price, color = GoldPremium, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
