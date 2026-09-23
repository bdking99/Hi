package com.example.ui.screens.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameHistoryItem
import com.example.data.model.GameItem
import com.example.data.model.GameLeaderboardEntry
import com.example.ui.theme.GoldPremium
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHistorySheet(
    historyItems: List<GameHistoryItem>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF140D26),
        dragHandle = { BottomSheetDefaults.DragHandle(color = GoldPremium) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .testTag("game_history_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📜 Game Bet History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPremium
                )
                Text(
                    text = "${historyItems.size} Records",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (historyItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎮", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No game history yet",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.7f)
                ) {
                    items(historyItems) { item ->
                        GameHistoryRow(item)
                    }
                }
            }
        }
    }
}

@Composable
fun GameHistoryRow(item: GameHistoryItem) {
    val isWin = item.rewardCoins > 0
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF22163D)),
        border = BorderStroke(1.dp, if (isWin) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.gameName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = "Round #${item.roundNumber} • Target: ${item.betTarget} • Outcome: ${item.outcome}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = dateFormat.format(Date(item.createdAt)),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isWin) "+%,d 🪙".format(item.rewardCoins) else "-%,d 🪙".format(item.betAmount),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (isWin) Color(0xFF4CAF50) else Color(0xFFFF5252)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isWin) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (isWin) "WON" else "LOST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWin) Color(0xFF4CAF50) else Color(0xFFFF5252),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLeaderboardSheet(
    leaderboardEntries: List<GameLeaderboardEntry>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF100826),
        dragHandle = { BottomSheetDefaults.DragHandle(color = GoldPremium) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .testTag("game_leaderboard_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Season 1 Gaming Champions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPremium
                )
                Text(
                    text = "XP Rank",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (leaderboardEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏅", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No leaderboard players yet",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.7f)
                ) {
                    items(leaderboardEntries) { entry ->
                        LeaderboardRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardRow(entry: GameLeaderboardEntry) {
    val rankBadge = when (entry.rank) {
        1 -> "🥇 #1"
        2 -> "🥈 #2"
        3 -> "🥉 #3"
        else -> "#${entry.rank}"
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.rank <= 3) Color(0xFF2D1B4E) else Color(0xFF1C1333)
        ),
        border = BorderStroke(
            1.dp,
            if (entry.rank == 1) GoldPremium else Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rankBadge,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = if (entry.rank <= 3) GoldPremium else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.width(44.dp)
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF3F2B66),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = entry.displayName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${entry.publicUserId} • ${entry.tier}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "%,d XP".format(entry.score),
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = GoldPremium
                )
                Text(
                    text = "${entry.gamesWon} Wins",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun GameAdminDialog(
    games: List<GameItem>,
    onToggleStatus: (gameId: String, newStatus: String, isEnabled: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B112E),
        title = {
            Text(
                text = "⚙️ Game Center Administration",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = GoldPremium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Server-Side Game Configuration & Maintenance Controls",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(games) { game ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF281A45), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${game.emoji} ${game.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Status: ${game.status}",
                                    fontSize = 10.sp,
                                    color = if (game.status == "ACTIVE") Color.Green else Color.Yellow
                                )
                            }

                            Row {
                                Button(
                                    onClick = {
                                        val newStatus = if (game.status == "ACTIVE") "MAINTENANCE" else "ACTIVE"
                                        onToggleStatus(game.gameId, newStatus, newStatus == "ACTIVE")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (game.status == "ACTIVE") Color(0xFFFF9800) else Color(0xFF4CAF50)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (game.status == "ACTIVE") "Pause" else "Activate",
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GoldPremium, fontWeight = FontWeight.Bold)
            }
        }
    )
}
