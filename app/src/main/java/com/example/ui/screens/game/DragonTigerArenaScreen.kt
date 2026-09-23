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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.GameBet
import com.example.data.model.GameMember
import com.example.data.model.GameRound
import com.example.ui.theme.GoldPremium
import com.example.ui.theme.TealPremium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragonTigerArenaScreen(
    currentRound: GameRound?,
    roundPhase: GameRoundPhase,
    secondsRemaining: Int,
    phaseStatusMessage: String,
    playerBets: List<GameBet>,
    sessionMembers: List<GameMember>,
    userCoinBalance: Long,
    onPlaceBet: (target: String, amount: Long) -> Unit,
    onLeaveGame: () -> Unit,
    onInviteClick: () -> Unit,
    onRechargeClick: () -> Unit
) {
    var selectedChip by remember { mutableLongStateOf(100L) }
    val chips = listOf(10L, 50L, 100L, 500L, 1000L, 5000L)

    // Calculate user's bets per target on current round
    val myDragonBet = playerBets.filter { it.betTarget == "DRAGON" }.sumOf { it.amount }
    val myTieBet = playerBets.filter { it.betTarget == "TIE" }.sumOf { it.amount }
    val myTigerBet = playerBets.filter { it.betTarget == "TIGER" }.sumOf { it.amount }

    // Parse card data from server round resultData if available
    val resultData = currentRound?.resultData
    val dragonCardRank = (resultData?.get("dragonCard") as? Map<*, *>)?.get("rankName")?.toString() ?: "?"
    val dragonCardSuit = (resultData?.get("dragonCard") as? Map<*, *>)?.get("suit")?.toString() ?: "♠"
    val isDragonCardRed = (resultData?.get("dragonCard") as? Map<*, *>)?.get("isRed") as? Boolean ?: false

    val tigerCardRank = (resultData?.get("tigerCard") as? Map<*, *>)?.get("rankName")?.toString() ?: "?"
    val tigerCardSuit = (resultData?.get("tigerCard") as? Map<*, *>)?.get("suit")?.toString() ?: "♣"
    val isTigerCardRed = (resultData?.get("tigerCard") as? Map<*, *>)?.get("isRed") as? Boolean ?: false

    val winningSide = currentRound?.resultOutcome ?: ""

    // Card Deal Flip Animation
    val dragonCardRotation by animateFloatAsState(
        targetValue = if (roundPhase == GameRoundPhase.DEALING_ANIMATION || roundPhase == GameRoundPhase.RESULT_SHOWCASE) 360f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "dragonCardFlip"
    )

    val tigerCardRotation by animateFloatAsState(
        targetValue = if (roundPhase == GameRoundPhase.DEALING_ANIMATION || roundPhase == GameRoundPhase.RESULT_SHOWCASE) 360f else 0f,
        animationSpec = tween(600, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "tigerCardFlip"
    )

    // Win Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "winPulse")
    val winPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "winScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F081D),
                        Color(0xFF1E0A3C),
                        Color(0xFF2A0845),
                        Color(0xFF0D021A)
                    )
                )
            )
            .testTag("dragon_tiger_arena_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // TOP APP BAR & HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onLeaveGame,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .testTag("btn_leave_game")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave Game",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🐉 DRAGON vs TIGER 🐯",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = GoldPremium
                        )
                    }
                    Text(
                        text = "Round #${currentRound?.roundNumber ?: 1} • ${sessionMembers.size} Players Live",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onInviteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .testTag("btn_invite_game")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = "Invite Friends",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Coin balance pill
                    Surface(
                        onClick = onRechargeClick,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.6f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text("🪙", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%,d".format(userCoinBalance),
                                color = GoldPremium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.AddCircle, contentDescription = "Add", tint = GoldPremium, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // SYNCHRONIZED SERVER STATUS & TIMER BANNER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (roundPhase) {
                        GameRoundPhase.BETTING_OPEN -> Color(0xFF1B5E20).copy(alpha = 0.6f)
                        GameRoundPhase.DEALING_ANIMATION -> Color(0xFFE65100).copy(alpha = 0.6f)
                        GameRoundPhase.RESULT_SHOWCASE -> Color(0xFF4A148C).copy(alpha = 0.7f)
                        else -> Color(0xFF212121).copy(alpha = 0.6f)
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when (roundPhase) {
                        GameRoundPhase.BETTING_OPEN -> Color(0xFF4CAF50)
                        GameRoundPhase.DEALING_ANIMATION -> Color(0xFFFF9800)
                        GameRoundPhase.RESULT_SHOWCASE -> GoldPremium
                        else -> Color.Gray
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (roundPhase == GameRoundPhase.BETTING_OPEN) Color.Green else Color.Yellow,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = phaseStatusMessage,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (roundPhase == GameRoundPhase.BETTING_OPEN || roundPhase == GameRoundPhase.DEALING_ANIMATION) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            border = BorderStroke(1.5.dp, GoldPremium),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$secondsRemaining",
                                    color = GoldPremium,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CARDS BATTLEGROUND ARENA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1E0A3C),
                                Color(0xFF3B104F),
                                Color(0xFF18052B)
                            )
                        )
                    )
                    .border(1.5.dp, GoldPremium.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DRAGON CARD
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(if (winningSide == "DRAGON" && roundPhase == GameRoundPhase.RESULT_SHOWCASE) winPulseScale else 1f)
                    ) {
                        Text("🐉 DRAGON", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlayingCardView(
                            rank = if (roundPhase == GameRoundPhase.BETTING_OPEN) "?" else dragonCardRank,
                            suit = if (roundPhase == GameRoundPhase.BETTING_OPEN) "🐉" else dragonCardSuit,
                            isRed = isDragonCardRed,
                            isRevealed = roundPhase != GameRoundPhase.BETTING_OPEN,
                            rotationAngle = dragonCardRotation,
                            isWinner = winningSide == "DRAGON" && roundPhase == GameRoundPhase.RESULT_SHOWCASE
                        )
                    }

                    // VS BADGE
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD4AF37),
                            modifier = Modifier.size(42.dp),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("VS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                            }
                        }
                        if (roundPhase == GameRoundPhase.RESULT_SHOWCASE) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (winningSide) {
                                    "DRAGON" -> "DRAGON WINS!"
                                    "TIGER" -> "TIGER WINS!"
                                    "TIE" -> "TIE 9X WIN!"
                                    else -> ""
                                },
                                color = GoldPremium,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // TIGER CARD
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(if (winningSide == "TIGER" && roundPhase == GameRoundPhase.RESULT_SHOWCASE) winPulseScale else 1f)
                    ) {
                        Text("🐯 TIGER", color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        PlayingCardView(
                            rank = if (roundPhase == GameRoundPhase.BETTING_OPEN) "?" else tigerCardRank,
                            suit = if (roundPhase == GameRoundPhase.BETTING_OPEN) "🐯" else tigerCardSuit,
                            isRed = isTigerCardRed,
                            isRevealed = roundPhase != GameRoundPhase.BETTING_OPEN,
                            rotationAngle = tigerCardRotation,
                            isWinner = winningSide == "TIGER" && roundPhase == GameRoundPhase.RESULT_SHOWCASE
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 BETTING TARGET TILES (DRAGON 2X, TIE 9X, TIGER 2X)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DRAGON TILE (2X)
                BetTargetTile(
                    title = "DRAGON",
                    multiplierText = "1 : 1 (2X)",
                    emoji = "🐉",
                    myBet = myDragonBet,
                    accentColor = Color(0xFFFF3366),
                    isWinner = winningSide == "DRAGON" && roundPhase == GameRoundPhase.RESULT_SHOWCASE,
                    isEnabled = roundPhase == GameRoundPhase.BETTING_OPEN,
                    modifier = Modifier.weight(1f),
                    onClick = { onPlaceBet("DRAGON", selectedChip) }
                )

                // TIE TILE (9X)
                BetTargetTile(
                    title = "TIE",
                    multiplierText = "1 : 8 (9X)",
                    emoji = "🤝",
                    myBet = myTieBet,
                    accentColor = Color(0xFF00E676),
                    isWinner = winningSide == "TIE" && roundPhase == GameRoundPhase.RESULT_SHOWCASE,
                    isEnabled = roundPhase == GameRoundPhase.BETTING_OPEN,
                    modifier = Modifier.weight(0.9f),
                    onClick = { onPlaceBet("TIE", selectedChip) }
                )

                // TIGER TILE (2X)
                BetTargetTile(
                    title = "TIGER",
                    multiplierText = "1 : 1 (2X)",
                    emoji = "🐯",
                    myBet = myTigerBet,
                    accentColor = Color(0xFFFF9100),
                    isWinner = winningSide == "TIGER" && roundPhase == GameRoundPhase.RESULT_SHOWCASE,
                    isEnabled = roundPhase == GameRoundPhase.BETTING_OPEN,
                    modifier = Modifier.weight(1f),
                    onClick = { onPlaceBet("TIGER", selectedChip) }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // CHIP SELECTOR BAR
            Surface(
                color = Color(0xFF140826),
                tonalElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                    Text(
                        text = "SELECT CHIP AMOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(chips) { chip ->
                            val isSelected = selectedChip == chip
                            Surface(
                                onClick = { selectedChip = chip },
                                shape = CircleShape,
                                color = if (isSelected) GoldPremium else Color(0xFF2E1A47),
                                border = BorderStroke(
                                    2.dp,
                                    if (isSelected) Color.White else GoldPremium.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .size(54.dp)
                                    .scale(if (isSelected) 1.08f else 1f)
                                    .testTag("chip_$chip")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = when {
                                                chip >= 1000L -> "${chip / 1000}K"
                                                else -> "$chip"
                                            },
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "🪙",
                                            fontSize = 9.sp
                                        )
                                    }
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
fun PlayingCardView(
    rank: String,
    suit: String,
    isRed: Boolean,
    isRevealed: Boolean,
    rotationAngle: Float,
    isWinner: Boolean
) {
    Card(
        modifier = Modifier
            .size(width = 75.dp, height = 108.dp)
            .rotate(rotationAngle)
            .shadow(if (isWinner) 16.dp else 6.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRevealed) Color.White else Color(0xFF4A148C)
        ),
        border = BorderStroke(
            if (isWinner) 2.5.dp else 1.dp,
            if (isWinner) GoldPremium else Color(0xFFB388FF)
        )
    ) {
        if (!isRevealed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF7B1FA2), Color(0xFF311B92))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(suit, fontSize = 28.sp)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rank,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = if (isRed) Color(0xFFD50000) else Color.Black
                    )
                    Text(
                        text = suit,
                        fontSize = 14.sp,
                        color = if (isRed) Color(0xFFD50000) else Color.Black
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = suit,
                        fontSize = 24.sp,
                        color = if (isRed) Color(0xFFD50000) else Color.Black
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = rank,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isRed) Color(0xFFD50000) else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun BetTargetTile(
    title: String,
    multiplierText: String,
    emoji: String,
    myBet: Long,
    accentColor: Color,
    isWinner: Boolean,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) accentColor.copy(alpha = 0.35f) else Color(0xFF200F3B)
        ),
        border = BorderStroke(
            if (isWinner) 2.5.dp else if (myBet > 0) 1.5.dp else 1.dp,
            if (isWinner) GoldPremium else if (myBet > 0) accentColor else accentColor.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .height(130.dp)
            .testTag("bet_target_$title")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = emoji,
                fontSize = 22.sp
            )

            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (isWinner) GoldPremium else accentColor
            )

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = multiplierText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (myBet > 0) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldPremium,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "My: %,d 🪙".format(myBet),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "TAP TO BET",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
