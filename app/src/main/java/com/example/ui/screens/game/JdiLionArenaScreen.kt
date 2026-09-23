package com.example.ui.screens.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class JdiAnimalItem(
    val id: String,
    val name: String,
    val emoji: String,
    val multiplier: String,
    val category: String, // AIR, LAND, SHARK
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JdiLionArenaScreen(
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
    val chips = listOf(20L, 50L, 100L, 500L, 1000L, 5000L)

    val animals = listOf(
        JdiAnimalItem("LION", "Lion King", "🦁", "12X", "LAND", Color(0xFFFF9800)),
        JdiAnimalItem("EAGLE", "Sky Eagle", "🦅", "12X", "AIR", Color(0xFF29B6F6)),
        JdiAnimalItem("PANDA", "Zen Panda", "🐼", "8X", "LAND", Color(0xFFEEEEEE)),
        JdiAnimalItem("PEACOCK", "Peacock", "🦚", "8X", "AIR", Color(0xFF00E676)),
        JdiAnimalItem("MONKEY", "Monkey", "🐒", "8X", "LAND", Color(0xFFFFB300)),
        JdiAnimalItem("PIGEON", "Pigeon", "🕊️", "8X", "AIR", Color(0xFFB0BEC5)),
        JdiAnimalItem("RABBIT", "Rabbit", "🐇", "6X", "LAND", Color(0xFFFF4081)),
        JdiAnimalItem("SWALLOW", "Swallow", "🐦", "6X", "AIR", Color(0xFF7C4DFF)),
        JdiAnimalItem("GOLD_SHARK", "Gold Shark", "🦈", "24X", "SHARK", GoldPremium),
        JdiAnimalItem("SILVER_SHARK", "Silver Shark", "🐬", "24X", "SHARK", Color(0xFFCFD8DC))
    )

    val winningOutcome = currentRound?.resultOutcome ?: ""

    // Spinning Wheel Animation
    val infiniteTransition = rememberInfiniteTransition(label = "spinWheel")
    val spinningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "wheelRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A192F),
                        Color(0xFF0F3460),
                        Color(0xFF16213E),
                        Color(0xFF1A1A2E)
                    )
                )
            )
            .testTag("jdi_lion_arena_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // HEADER
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
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🦁 JDI LION SAFARI 🌴",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = GoldPremium
                    )
                    Text(
                        text = "Round #${currentRound?.roundNumber ?: 1} • ${sessionMembers.size} Playing",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

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
                    }
                }
            }

            // SERVER COUNTDOWN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (roundPhase) {
                        GameRoundPhase.BETTING_OPEN -> Color(0xFF1B5E20).copy(alpha = 0.6f)
                        GameRoundPhase.DEALING_ANIMATION -> Color(0xFFE65100).copy(alpha = 0.6f)
                        GameRoundPhase.RESULT_SHOWCASE -> Color(0xFF4A148C).copy(alpha = 0.7f)
                        else -> Color(0xFF212121).copy(alpha = 0.6f)
                    }
                ),
                border = BorderStroke(1.dp, GoldPremium.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = phaseStatusMessage,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        border = BorderStroke(1.5.dp, GoldPremium),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$secondsRemaining",
                                color = GoldPremium,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // CENTRAL ANIMAL HIGHLIGHT / WHEEL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF112240))
                    .border(1.dp, GoldPremium.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (roundPhase == GameRoundPhase.DEALING_ANIMATION) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎡",
                            fontSize = 38.sp,
                            modifier = Modifier.rotate(spinningRotation)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Spinning Safari Wheel...",
                            color = GoldPremium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else if (roundPhase == GameRoundPhase.RESULT_SHOWCASE && winningOutcome.isNotBlank()) {
                    val winnerAnimal = animals.find { it.id == winningOutcome }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = winnerAnimal?.emoji ?: "🦁",
                            fontSize = 42.sp
                        )
                        Text(
                            text = "${winnerAnimal?.name ?: winningOutcome} WINS! (${winnerAnimal?.multiplier ?: "12X"})",
                            color = GoldPremium,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🦅", fontSize = 28.sp)
                        Text("🦁", fontSize = 36.sp)
                        Text("🦈", fontSize = 28.sp)
                    }
                }
            }

            // 10 ANIMAL BETTING TILES GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(animals) { animal ->
                    val myBet = playerBets.filter { it.betTarget == animal.id }.sumOf { it.amount }
                    val isWinner = winningOutcome == animal.id && roundPhase == GameRoundPhase.RESULT_SHOWCASE

                    Card(
                        onClick = { onPlaceBet(animal.id, selectedChip) },
                        enabled = roundPhase == GameRoundPhase.BETTING_OPEN,
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isWinner) GoldPremium.copy(alpha = 0.35f) else Color(0xFF1E2D4A)
                        ),
                        border = BorderStroke(
                            if (isWinner) 2.dp else if (myBet > 0) 1.5.dp else 1.dp,
                            if (isWinner) GoldPremium else if (myBet > 0) animal.color else animal.color.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .height(84.dp)
                            .testTag("jdi_tile_${animal.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(animal.emoji, fontSize = 20.sp)
                            Text(
                                text = animal.multiplier,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = animal.color
                            )
                            if (myBet > 0) {
                                Text(
                                    text = "${myBet}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GoldPremium
                                )
                            }
                        }
                    }
                }
            }

            // CHIP SELECTOR BAR
            Surface(
                color = Color(0xFF0D1B2A),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SELECT BET AMOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
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
                                color = if (isSelected) GoldPremium else Color(0xFF1B263B),
                                border = BorderStroke(1.5.dp, if (isSelected) Color.White else GoldPremium.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(if (isSelected) 1.08f else 1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$chip",
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
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
