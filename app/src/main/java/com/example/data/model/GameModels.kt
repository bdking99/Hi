package com.example.data.model

// 🎮 GAME CATALOG ITEM - Stored in Firestore `games/{gameId}`
data class GameItem(
    val gameId: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val bannerUrl: String = "",
    val category: String = "Casino Duel", // "Casino Duel", "Multipliers", "Slots & Lucky", "Board & Mini Games"
    val status: String = "ACTIVE", // "ACTIVE", "MAINTENANCE", "DISABLED", "COMING_SOON"
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
    val minimumPlayers: Int = 1,
    val maximumPlayers: Int = 100,
    val rewardMode: String = "VIRTUAL_COINS", // "VIRTUAL_COINS", "XP_POINTS", "COSMETIC_BADGES"
    val jackpot: String = "1,000,000",
    val badge: String = "🔥 POPULAR",
    val emoji: String = "🎮",
    val minBet: Long = 10L,
    val maxBet: Long = 100000L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ⚙️ GAME CONFIGURATION - Stored in Firestore `gameConfigurations/{gameId}`
data class GameConfiguration(
    val gameId: String = "",
    val enabled: Boolean = true,
    val version: Int = 1,
    val minimumPlayers: Int = 1,
    val maximumPlayers: Int = 100,
    val roundDurationSeconds: Int = 20,
    val bettingDurationSeconds: Int = 12,
    val animationDurationSeconds: Int = 5,
    val resultDurationSeconds: Int = 3,
    val rewardMode: String = "VIRTUAL_COINS",
    val payoutMultipliers: Map<String, Double> = emptyMap(),
    val outcomes: List<String> = emptyList(),
    val houseCommissionPercent: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

// 🏟️ GAME SESSION - Stored in Firestore `gameSessions/{sessionId}`
data class GameSession(
    val sessionId: String = "",
    val gameId: String = "",
    val gameName: String = "",
    val hostUid: String = "",
    val hostPublicUserId: String = "",
    val hostDisplayName: String = "",
    val status: String = "RUNNING", // "WAITING", "RUNNING", "PAUSED", "COMPLETED", "CANCELLED", "MAINTENANCE"
    val currentRoundId: String = "",
    val currentRoundNumber: Int = 1,
    val playerCount: Int = 1,
    val maxPlayers: Int = 100,
    val linkedVoiceRoomId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

// 👥 GAME MEMBER - Stored in Firestore `gameSessions/{sessionId}/members/{uid}`
data class GameMember(
    val uid: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val status: String = "ACTIVE", // "ACTIVE", "READY", "SPECTATING", "LEFT"
    val totalWonCoins: Long = 0L,
    val joinedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)

// 🔄 GAME ROUND - Stored in Firestore `gameSessions/{sessionId}/rounds/{roundId}`
data class GameRound(
    val roundId: String = "",
    val sessionId: String = "",
    val gameId: String = "",
    val roundNumber: Int = 1,
    val status: String = "OPEN", // "OPEN", "LOCKED", "RESULT_GENERATED", "COMPLETED", "CANCELLED"
    val createdAt: Long = System.currentTimeMillis(),
    val roundStartAt: Long = System.currentTimeMillis(),
    val bettingOpenAt: Long = System.currentTimeMillis(),
    val bettingClosedAt: Long = System.currentTimeMillis() + 12000L,
    val resultGeneratedAt: Long? = null,
    val resultAt: Long = System.currentTimeMillis() + 17000L,
    val roundEndAt: Long = System.currentTimeMillis() + 20000L,
    val resultOutcome: String = "",
    val resultData: Map<String, Any?> = emptyMap(),
    val configVersion: Int = 1,
    val totalBetCoins: Long = 0L,
    val totalPayoutCoins: Long = 0L,
    val serverTimestamp: Long = System.currentTimeMillis()
)

// 🎯 GAME BET RECORD - Stored in Firestore `gameSessions/{sessionId}/rounds/{roundId}/bets/{betId}`
data class GameBet(
    val betId: String = "",
    val roundId: String = "",
    val sessionId: String = "",
    val gameId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val betTarget: String = "", // e.g., "DRAGON", "TIGER", "TIE", "LION", "EAGLE", "RED_CAR"
    val amount: Long = 0L,
    val operationId: String = "",
    val status: String = "PENDING", // "PENDING", "WON", "LOST", "REFUNDED", "CANCELLED"
    val multiplier: Double = 1.0,
    val payoutAmount: Long = 0L,
    val placedAt: Long = System.currentTimeMillis()
)

// 📜 GAME HISTORY RECORD - Stored in Firestore `gameHistory/{historyId}`
data class GameHistoryItem(
    val historyId: String = "",
    val gameId: String = "",
    val gameName: String = "",
    val sessionId: String = "",
    val roundId: String = "",
    val roundNumber: Int = 1,
    val uid: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val betTarget: String = "",
    val betAmount: Long = 0L,
    val outcome: String = "",
    val rewardCoins: Long = 0L,
    val netChange: Long = 0L,
    val status: String = "SETTLED", // "SETTLED", "CANCELLED", "REFUNDED"
    val createdAt: Long = System.currentTimeMillis(),
    val serverTimestamp: Long = System.currentTimeMillis()
)

// 📊 PLAYER STATS - Stored in Firestore `users/{uid}/gameStats/{gameId}`
data class PlayerGameStats(
    val uid: String = "",
    val gameId: String = "",
    val gamesPlayed: Int = 0,
    val roundsPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val winRatePercent: Int = 0,
    val totalBetsCoins: Long = 0L,
    val totalRewardsCoins: Long = 0L,
    val netEarningsCoins: Long = 0L,
    val achievements: List<String> = emptyList(),
    val lastPlayedAt: Long = System.currentTimeMillis()
)

// 🏆 LEADERBOARD ENTRY - Stored in Firestore `leaderboards/season_current/players/{uid}`
data class GameLeaderboardEntry(
    val uid: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val score: Long = 0L,
    val rank: Int = 0,
    val tier: String = "Bronze", // "Bronze", "Silver", "Gold", "Platinum", "Diamond", "Crown Master"
    val gamesWon: Int = 0,
    val totalRounds: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

// 🎪 SEASONAL GAME EVENT - Stored in Firestore `gameEvents/{eventId}`
data class GameEvent(
    val eventId: String = "",
    val name: String = "",
    val description: String = "",
    val bannerUrl: String = "",
    val badge: String = "🔥 GRAND EVENT",
    val rewardPoolCoins: Long = 10000000L,
    val startAt: Long = System.currentTimeMillis() - 86400000L,
    val endAt: Long = System.currentTimeMillis() + 86400000L * 7,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isLive: Boolean get() = enabled && System.currentTimeMillis() in startAt..endAt
}

// 📨 GAME INVITATION - Stored in Firestore `gameInvitations/{invitationId}`
data class GameInvitation(
    val invitationId: String = "",
    val senderUid: String = "",
    val senderPublicId: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val recipientUid: String = "",
    val gameId: String = "",
    val gameName: String = "",
    val sessionId: String = "",
    val status: String = "PENDING", // "PENDING", "ACCEPTED", "DECLINED", "EXPIRED"
    val createdAt: Long = System.currentTimeMillis()
)

// Card representation for Dragon Tiger
data class ServerPlayingCard(
    val rankName: String = "A",
    val value: Int = 1,
    val suit: String = "♠",
    val isRed: Boolean = false
)

// Outcome calculation result envelope
data class ServerRoundOutcome(
    val outcomeName: String,
    val outcomeDetails: Map<String, Any?>,
    val winningTargets: List<String>,
    val targetMultipliers: Map<String, Double>
)
