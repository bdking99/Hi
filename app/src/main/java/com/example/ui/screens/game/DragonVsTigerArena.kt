package com.example.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BetTarget { DRAGON, TIE, TIGER }

data class PlayingCard(
    val rankName: String,
    val value: Int,
    val suit: String,
    val isRed: Boolean
)

val CardDeck = listOf(
    PlayingCard("A", 1, "♠", false), PlayingCard("2", 2, "♠", false),
    PlayingCard("3", 3, "♠", false), PlayingCard("4", 4, "♠", false),
    PlayingCard("5", 5, "♠", false), PlayingCard("6", 6, "♠", false),
    PlayingCard("7", 7, "♠", false), PlayingCard("8", 8, "♠", false),
    PlayingCard("9", 9, "♠", false), PlayingCard("10", 10, "♠", false),
    PlayingCard("J", 11, "♠", false), PlayingCard("Q", 12, "♠", false),
    PlayingCard("K", 13, "♠", false),
    PlayingCard("A", 1, "♥", true), PlayingCard("2", 2, "♥", true),
    PlayingCard("3", 3, "♥", true), PlayingCard("4", 4, "♥", true),
    PlayingCard("5", 5, "♥", true), PlayingCard("6", 6, "♥", true),
    PlayingCard("7", 7, "♥", true), PlayingCard("8", 8, "♥", true),
    PlayingCard("9", 9, "♥", true), PlayingCard("10", 10, "♥", true),
    PlayingCard("J", 11, "♥", true), PlayingCard("Q", 12, "♥", true),
    PlayingCard("K", 13, "♥", true),
    PlayingCard("A", 1, "♦", true), PlayingCard("2", 2, "♦", true),
    PlayingCard("5", 5, "♦", true), PlayingCard("8", 8, "♦", true),
    PlayingCard("10", 10, "♦", true), PlayingCard("J", 11, "♦", true),
    PlayingCard("Q", 12, "♦", true), PlayingCard("K", 13, "♦", true),
    PlayingCard("A", 1, "♣", false), PlayingCard("7", 7, "♣", false),
    PlayingCard("9", 9, "♣", false), PlayingCard("10", 10, "♣", false),
    PlayingCard("J", 11, "♣", false), PlayingCard("Q", 12, "♣", false),
    PlayingCard("K", 13, "♣", false)
)

@Composable
fun DragonVsTigerArena(
    coinBalance: Long,
    onDismiss: () -> Unit,
    onBetPlaced: (Long) -> Unit,
    onWinWon: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var selectedChip by remember { mutableLongStateOf(100L) }
    val chips = listOf(10L, 50L, 100L, 500L, 1000L, 5000L)

    var myDragonBet by remember { mutableLongStateOf(0L) }
    var myTieBet by remember { mutableLongStateOf(0L) }
    var myTigerBet by remember { mutableLongStateOf(0L) }

    var tableDragonBet by remember { mutableLongStateOf(42500L) }
    var tableTieBet by remember { mutableLongStateOf(6800L) }
    var tableTigerBet by remember { mutableLongStateOf(39800L) }

    // Game Phase: "BETTING", "DEALING", "RESULT"
    var gamePhase by remember { mutableStateOf("BETTING") }
    var countdownSeconds by remember { mutableIntStateOf(10) }
    var roundStatusText by remember { mutableStateOf("Place Your Bets!") }

    var dragonCard by remember { mutableStateOf<PlayingCard?>(null) }
    var tigerCard by remember { mutableStateOf<PlayingCard?>(null) }
    var winnerTarget by remember { mutableStateOf<BetTarget?>(null) }
    var lastWinAmount by remember { mutableLongStateOf(0L) }

    // History roadmap
    val history = remember {
        mutableStateListOf(
            BetTarget.DRAGON, BetTarget.TIGER, BetTarget.DRAGON,
            BetTarget.DRAGON, BetTarget.TIE, BetTarget.TIGER, BetTarget.DRAGON
        )
    }

    // Auto game loop
    LaunchedEffect(Unit) {
        while (true) {
            // 1. Betting phase (10 seconds)
            gamePhase = "BETTING"
            roundStatusText = "🔥 PLACE YOUR BETS!"
            dragonCard = null
            tigerCard = null
            winnerTarget = null
            lastWinAmount = 0L

            for (sec in 10 downTo 1) {
                countdownSeconds = sec
                // Random simulate table bets increasing
                tableDragonBet += (200..1500).random()
                tableTigerBet += (200..1500).random()
                if (sec % 3 == 0) tableTieBet += (100..600).random()
                delay(1000)
            }

            // 2. Dealing Cards Phase
            countdownSeconds = 0
            gamePhase = "DEALING"
            roundStatusText = "Dealing Cards..."

            delay(600)
            val dCard = CardDeck.random()
            dragonCard = dCard

            delay(800)
            val tCard = CardDeck.random()
            tigerCard = tCard

            delay(500)

            // 3. Result calculation
            gamePhase = "RESULT"
            val roundWinner = when {
                dCard.value > tCard.value -> BetTarget.DRAGON
                tCard.value > dCard.value -> BetTarget.TIGER
                else -> BetTarget.TIE
            }
            winnerTarget = roundWinner
            history.add(roundWinner)
            if (history.size > 14) history.removeAt(0)

            var wonCoins = 0L
            when (roundWinner) {
                BetTarget.DRAGON -> {
                    roundStatusText = "🐉 DRAGON WINS! (2X)"
                    if (myDragonBet > 0) wonCoins += myDragonBet * 2
                }
                BetTarget.TIGER -> {
                    roundStatusText = "🐯 TIGER WINS! (2X)"
                    if (myTigerBet > 0) wonCoins += myTigerBet * 2
                }
                BetTarget.TIE -> {
                    roundStatusText = "🟢 TIE MATCH! (8X)"
                    if (myTieBet > 0) wonCoins += myTieBet * 8
                    // Return half of dragon and tiger bets on tie
                    if (myDragonBet > 0) wonCoins += myDragonBet / 2
                    if (myTigerBet > 0) wonCoins += myTigerBet / 2
                }
            }

            if (wonCoins > 0) {
                lastWinAmount = wonCoins
                onWinWon(wonCoins)
            }

            delay(4000) // Display results for 4 seconds

            // Reset user bets for next round
            myDragonBet = 0L
            myTieBet = 0L
            myTigerBet = 0L
            tableDragonBet = (25000..45000).random().toLong()
            tableTieBet = (4000..8000).random().toLong()
            tableTigerBet = (25000..45000).random().toLong()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F081D),
                        Color(0xFF1D0E35),
                        Color(0xFF0D061A)
                    )
                )
            )
            .testTag("dragon_vs_tiger_arena")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // --- TOP ARENA HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🐉", fontSize = 18.sp)
                            Text(" VS ", fontWeight = FontWeight.Black, color = GoldPremium, fontSize = 14.sp)
                            Text("🐯", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dragon Tiger Live",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Live Casino VIP Studio #8",
                            color = TealPremium,
                            fontSize = 10.sp
                        )
                    }
                }

                // Balance display
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%,d".format(coinBalance),
                            color = GoldPremium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- RECENT ROADMAP BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROADMAP:",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(history) { result ->
                        val (bg, label) = when (result) {
                            BetTarget.DRAGON -> Color(0xFFD32F2F) to "D"
                            BetTarget.TIGER -> Color(0xFFFFA000) to "T"
                            BetTarget.TIE -> Color(0xFF388E3C) to "Tie"
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- BATTLE ARENA & CARD STAGE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF32145A),
                                Color(0xFF1B0A33),
                                Color(0xFF100520)
                            )
                        )
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(Color(0xFFFF3D00), GoldPremium, Color(0xFF00E5FF))),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background arena art
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dragon Side Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🐉 DRAGON",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CasinoCardView(
                            card = dragonCard,
                            isWinner = winnerTarget == BetTarget.DRAGON,
                            isTie = winnerTarget == BetTarget.TIE
                        )
                    }

                    // VS / Timer center indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        if (gamePhase == "BETTING") {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .border(2.dp, GoldPremium, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$countdownSeconds",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "BETTING",
                                color = GoldPremium,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Text("⚡", fontSize = 32.sp)
                            Text(
                                text = "VS",
                                color = GoldPremium,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Tiger Side Card
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🐯 TIGER",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CasinoCardView(
                            card = tigerCard,
                            isWinner = winnerTarget == BetTarget.TIGER,
                            isTie = winnerTarget == BetTarget.TIE
                        )
                    }
                }

                // Banner overlay for status or victory
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 6.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = roundStatusText,
                        color = if (winnerTarget != null) GoldPremium else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- BETTING ZONES (DRAGON, TIE, TIGER) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. DRAGON BET (2X)
                BetZoneCard(
                    modifier = Modifier.weight(1f),
                    title = "DRAGON",
                    emoji = "🐉",
                    payout = "1 : 1 (2X)",
                    themeColor = Color(0xFFD32F2F),
                    myBet = myDragonBet,
                    totalBet = tableDragonBet,
                    isWinning = winnerTarget == BetTarget.DRAGON,
                    enabled = gamePhase == "BETTING" && coinBalance >= selectedChip,
                    onClick = {
                        if (coinBalance >= selectedChip) {
                            myDragonBet += selectedChip
                            tableDragonBet += selectedChip
                            onBetPlaced(selectedChip)
                        }
                    }
                )

                // 2. TIE BET (8X)
                BetZoneCard(
                    modifier = Modifier.weight(0.85f),
                    title = "TIE",
                    emoji = "🟢",
                    payout = "1 : 8 (8X)",
                    themeColor = Color(0xFF2E7D32),
                    myBet = myTieBet,
                    totalBet = tableTieBet,
                    isWinning = winnerTarget == BetTarget.TIE,
                    enabled = gamePhase == "BETTING" && coinBalance >= selectedChip,
                    onClick = {
                        if (coinBalance >= selectedChip) {
                            myTieBet += selectedChip
                            tableTieBet += selectedChip
                            onBetPlaced(selectedChip)
                        }
                    }
                )

                // 3. TIGER BET (2X)
                BetZoneCard(
                    modifier = Modifier.weight(1f),
                    title = "TIGER",
                    emoji = "🐯",
                    payout = "1 : 1 (2X)",
                    themeColor = Color(0xFFE65100),
                    myBet = myTigerBet,
                    totalBet = tableTigerBet,
                    isWinning = winnerTarget == BetTarget.TIGER,
                    enabled = gamePhase == "BETTING" && coinBalance >= selectedChip,
                    onClick = {
                        if (coinBalance >= selectedChip) {
                            myTigerBet += selectedChip
                            tableTigerBet += selectedChip
                            onBetPlaced(selectedChip)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Win Celebration Announcement
            if (lastWinAmount > 0) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = GoldPremium,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎉", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YOU WON +%,d COINS!".format(lastWinAmount),
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // --- CHIP SELECTOR BAR ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF140A26),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SELECT BETTING CHIP",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        chips.forEach { chipValue ->
                            val isSelected = selectedChip == chipValue
                            CasinoChipButton(
                                value = chipValue,
                                isSelected = isSelected,
                                onClick = { selectedChip = chipValue }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Realistic Playing Card UI View
// ----------------------------------------------------
@Composable
fun CasinoCardView(
    card: PlayingCard?,
    isWinner: Boolean,
    isTie: Boolean
) {
    val borderColor = when {
        isTie -> Color(0xFF00E676)
        isWinner -> GoldPremium
        else -> Color(0xFF424242)
    }

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(100.dp)
            .shadow(8.dp, RoundedCornerShape(8.dp), spotColor = if (isWinner) GoldPremium else Color.Black)
            .clip(RoundedCornerShape(8.dp))
            .background(if (card != null) Color(0xFFFAFAFA) else Color(0xFF1A1A2E))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (card != null) {
            val suitColor = if (card.isRed) Color(0xFFD32F2F) else Color(0xFF212121)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top corner rank + suit
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.rankName,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = suitColor
                    )
                    Text(
                        text = card.suit,
                        fontSize = 12.sp,
                        color = suitColor
                    )
                }

                // Center big suit icon
                Text(
                    text = card.suit,
                    fontSize = 28.sp,
                    color = suitColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Bottom inverted rank
                Text(
                    text = card.rankName,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = suitColor,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        } else {
            // Card Back Design
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(Color(0xFF281845), RoundedCornerShape(4.dp))
                    .border(1.dp, GoldPremium.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("👑", fontSize = 22.sp)
            }
        }
    }
}

// ----------------------------------------------------
// Bet Zone Card (Dragon / Tie / Tiger)
// ----------------------------------------------------
@Composable
fun BetZoneCard(
    modifier: Modifier = Modifier,
    title: String,
    emoji: String,
    payout: String,
    themeColor: Color,
    myBet: Long,
    totalBet: Long,
    isWinning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val winGlow = rememberInfiniteTransition(label = "winglow")
    val glowScale by winGlow.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    Card(
        modifier = modifier
            .height(145.dp)
            .scale(if (isWinning) glowScale else 1f)
            .clickable(enabled = enabled) { onClick() }
            .shadow(
                elevation = if (isWinning) 12.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isWinning) GoldPremium else themeColor
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            themeColor.copy(alpha = 0.45f),
                            Color(0xFF140A26)
                        )
                    )
                )
                .border(
                    width = if (isWinning) 3.dp else 1.5.dp,
                    color = if (isWinning) GoldPremium else themeColor.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag & Emoji
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 24.sp)
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = payout,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = GoldPremium
                    )
                }

                // Center Total Pool
                Text(
                    text = "Pool: %,d".format(totalBet),
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                // User's Bet Chip Display
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (myBet > 0) GoldPremium else Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (myBet > 0) "Bet: %,d🪙".format(myBet) else "Tap to Bet",
                        color = if (myBet > 0) Color.Black else Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// Casino Chip Button Component
// ----------------------------------------------------
@Composable
fun CasinoChipButton(
    value: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (chipBg, chipBorder) = when (value) {
        10L -> Color(0xFF1976D2) to Color(0xFF90CAF9)
        50L -> Color(0xFF388E3C) to Color(0xFFA5D6A7)
        100L -> Color(0xFFD32F2F) to Color(0xFFEF9A9A)
        500L -> Color(0xFF7B1FA2) to Color(0xFFCE93D8)
        1000L -> Color(0xFFF57C00) to Color(0xFFFFCC80)
        else -> GoldPremium to Color.White
    }

    Box(
        modifier = Modifier
            .size(46.dp)
            .scale(if (isSelected) 1.15f else 1f)
            .clip(CircleShape)
            .background(chipBg)
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) Color.White else chipBorder,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (value >= 1000) "${value / 1000}K" else "$value",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = if (value == 5000L) Color.Black else Color.White
            )
        }
    }
}
