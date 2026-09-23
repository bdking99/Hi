package com.example.data.repository

import com.example.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

class GameRepository {

    companion object {
        const val GAMES_COLLECTION = "games"
        const val GAME_CONFIGS_COLLECTION = "gameConfigurations"
        const val SESSIONS_COLLECTION = "gameSessions"
        const val HISTORY_COLLECTION = "gameHistory"
        const val LEADERBOARD_COLLECTION = "leaderboards"
        const val EVENTS_COLLECTION = "gameEvents"
        const val INVITATIONS_COLLECTION = "gameInvitations"
        const val WALLETS_COLLECTION = "wallets"
        const val TRANSACTIONS_COLLECTION = "walletTransactions"
        const val USERS_COLLECTION = "users"
        const val AUDIT_LOGS_COLLECTION = "auditLogs"
    }

    private val secureRandom = SecureRandom()

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 1. DYNAMIC GAME CATALOG & CONFIGURATION
    // ==========================================

    fun getGamesCatalogStream(): Flow<List<GameItem>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultGamesCatalog())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(GAMES_COLLECTION)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(getDefaultGamesCatalog())
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val items = snapshot.documents.mapNotNull { it.toObject(GameItem::class.java) }
                        trySend(items)
                    } else {
                        // Seed catalog in Firestore if empty
                        seedDefaultGames(firestore)
                        trySend(getDefaultGamesCatalog())
                    }
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getDefaultGamesCatalog())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getDefaultGamesCatalog(): List<GameItem> {
        return listOf(
            GameItem(
                gameId = "game_dragon_tiger",
                name = "Dragon vs Tiger",
                description = "Authoritative Live Casino Duel with Real-Time Dealer Cards (2X / 9X)",
                category = "Casino Duel",
                status = "ACTIVE",
                enabled = true,
                sortOrder = 1,
                minimumPlayers = 1,
                maximumPlayers = 500,
                rewardMode = "VIRTUAL_COINS",
                jackpot = "8,450,000",
                badge = "🔥 TOP DUEL",
                emoji = "🐉",
                minBet = 10L,
                maxBet = 100000L
            ),
            GameItem(
                gameId = "game_jdi_lion",
                name = "JDI Lion King",
                description = "Wild Safari Multiplier Wheel with King Lion, Golden Shark & Sky Eagles (Up to 24X)",
                category = "Multipliers",
                status = "ACTIVE",
                enabled = true,
                sortOrder = 2,
                minimumPlayers = 1,
                maximumPlayers = 500,
                rewardMode = "VIRTUAL_COINS",
                jackpot = "5,120,000",
                badge = "🦁 x24 SAFARI",
                emoji = "🦁",
                minBet = 20L,
                maxBet = 50000L
            ),
            GameItem(
                gameId = "game_gadi_racing",
                name = "Gadi Super Drift",
                description = "High-octane Cyber Car Speed Duel with Drift Multipliers (3X to 10X)",
                category = "Multipliers",
                status = "ACTIVE",
                enabled = true,
                sortOrder = 3,
                minimumPlayers = 1,
                maximumPlayers = 300,
                rewardMode = "VIRTUAL_COINS",
                jackpot = "3,900,000",
                badge = "🏎️ HYPER DRIFT",
                emoji = "🏎️",
                minBet = 50L,
                maxBet = 50000L
            ),
            GameItem(
                gameId = "game_tin_patti",
                name = "Tin Patti Royale",
                description = "Classic 3-Card Social Showdown with Real Hand Ranking (Virtual Social Game)",
                category = "Casino Duel",
                status = "ACTIVE",
                enabled = true,
                sortOrder = 4,
                minimumPlayers = 2,
                maximumPlayers = 6,
                rewardMode = "VIRTUAL_COINS",
                jackpot = "2,400,000",
                badge = "👑 3-CARD SHOW",
                emoji = "🃏",
                minBet = 100L,
                maxBet = 20000L
            ),
            GameItem(
                gameId = "game_lucky_spin",
                name = "Lucky VIP Wheel",
                description = "Social Fortune Wheel with XP multipliers, Frames and Coin Jackpots",
                category = "Slots & Lucky",
                status = "ACTIVE",
                enabled = true,
                sortOrder = 5,
                minimumPlayers = 1,
                maximumPlayers = 1000,
                rewardMode = "VIRTUAL_COINS",
                jackpot = "10,000,000",
                badge = "💎 MEGA JACKPOT",
                emoji = "🎡",
                minBet = 50L,
                maxBet = 10000L
            )
        )
    }

    private fun seedDefaultGames(firestore: FirebaseFirestore) {
        val games = getDefaultGamesCatalog()
        for (g in games) {
            firestore.collection(GAMES_COLLECTION).document(g.gameId).set(g, SetOptions.merge())
            // Also seed default config
            val config = GameConfiguration(
                gameId = g.gameId,
                enabled = g.enabled,
                version = 1,
                minimumPlayers = g.minimumPlayers,
                maximumPlayers = g.maximumPlayers,
                roundDurationSeconds = 20,
                bettingDurationSeconds = 12,
                animationDurationSeconds = 5,
                resultDurationSeconds = 3,
                rewardMode = g.rewardMode,
                payoutMultipliers = mapOf(
                    "DRAGON" to 2.0, "TIGER" to 2.0, "TIE" to 9.0,
                    "LION" to 12.0, "EAGLE" to 12.0, "PANDA" to 8.0, "PEACOCK" to 8.0,
                    "MONKEY" to 8.0, "PIGEON" to 8.0, "RABBIT" to 6.0, "SWALLOW" to 6.0,
                    "GOLD_SHARK" to 24.0, "SILVER_SHARK" to 24.0,
                    "RED_CAR" to 3.0, "BLUE_CAR" to 3.0, "GOLD_CAR" to 5.0, "GREEN_CAR" to 3.0, "CYBER_CAR" to 10.0
                ),
                updatedAt = System.currentTimeMillis()
            )
            firestore.collection(GAME_CONFIGS_COLLECTION).document(g.gameId).set(config, SetOptions.merge())
        }
    }

    suspend fun getGameConfiguration(gameId: String): GameConfiguration = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext GameConfiguration(gameId = gameId)
        try {
            val doc = firestore.collection(GAME_CONFIGS_COLLECTION).document(gameId).get().await()
            if (doc.exists()) {
                doc.toObject(GameConfiguration::class.java) ?: GameConfiguration(gameId = gameId)
            } else {
                GameConfiguration(gameId = gameId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GameConfiguration(gameId = gameId)
        }
    }

    // ==========================================
    // 2. CRYPTOGRAPHIC SERVER-SIDE RANDOM OUTCOMES
    // ==========================================

    private val SERVER_DECK = listOf(
        ServerPlayingCard("A", 1, "♠", false), ServerPlayingCard("2", 2, "♠", false),
        ServerPlayingCard("3", 3, "♠", false), ServerPlayingCard("4", 4, "♠", false),
        ServerPlayingCard("5", 5, "♠", false), ServerPlayingCard("6", 6, "♠", false),
        ServerPlayingCard("7", 7, "♠", false), ServerPlayingCard("8", 8, "♠", false),
        ServerPlayingCard("9", 9, "♠", false), ServerPlayingCard("10", 10, "♠", false),
        ServerPlayingCard("J", 11, "♠", false), ServerPlayingCard("Q", 12, "♠", false),
        ServerPlayingCard("K", 13, "♠", false),
        ServerPlayingCard("A", 1, "♥", true), ServerPlayingCard("2", 2, "♥", true),
        ServerPlayingCard("3", 3, "♥", true), ServerPlayingCard("4", 4, "♥", true),
        ServerPlayingCard("5", 5, "♥", true), ServerPlayingCard("6", 6, "♥", true),
        ServerPlayingCard("7", 7, "♥", true), ServerPlayingCard("8", 8, "♥", true),
        ServerPlayingCard("9", 9, "♥", true), ServerPlayingCard("10", 10, "♥", true),
        ServerPlayingCard("J", 11, "♥", true), ServerPlayingCard("Q", 12, "♥", true),
        ServerPlayingCard("K", 13, "♥", true),
        ServerPlayingCard("A", 1, "♦", true), ServerPlayingCard("2", 2, "♦", true),
        ServerPlayingCard("3", 3, "♦", true), ServerPlayingCard("4", 4, "♦", true),
        ServerPlayingCard("5", 5, "♦", true), ServerPlayingCard("6", 6, "♦", true),
        ServerPlayingCard("7", 7, "♦", true), ServerPlayingCard("8", 8, "♦", true),
        ServerPlayingCard("9", 9, "♦", true), ServerPlayingCard("10", 10, "♦", true),
        ServerPlayingCard("J", 11, "♦", true), ServerPlayingCard("Q", 12, "♦", true),
        ServerPlayingCard("K", 13, "♦", true),
        ServerPlayingCard("A", 1, "♣", false), ServerPlayingCard("2", 2, "♣", false),
        ServerPlayingCard("3", 3, "♣", false), ServerPlayingCard("4", 4, "♣", false),
        ServerPlayingCard("5", 5, "♣", false), ServerPlayingCard("6", 6, "♣", false),
        ServerPlayingCard("7", 7, "♣", false), ServerPlayingCard("8", 8, "♣", false),
        ServerPlayingCard("9", 9, "♣", false), ServerPlayingCard("10", 10, "♣", false),
        ServerPlayingCard("J", 11, "♣", false), ServerPlayingCard("Q", 12, "♣", false),
        ServerPlayingCard("K", 13, "♣", false)
    )

    fun generateDragonTigerOutcome(): ServerRoundOutcome {
        val dragonIndex = secureRandom.nextInt(SERVER_DECK.size)
        var tigerIndex = secureRandom.nextInt(SERVER_DECK.size)
        while (tigerIndex == dragonIndex) {
            tigerIndex = secureRandom.nextInt(SERVER_DECK.size)
        }

        val dragonCard = SERVER_DECK[dragonIndex]
        val tigerCard = SERVER_DECK[tigerIndex]

        val winningSide = when {
            dragonCard.value > tigerCard.value -> "DRAGON"
            tigerCard.value > dragonCard.value -> "TIGER"
            else -> "TIE"
        }

        val winners = mutableListOf(winningSide)
        val multipliers = mutableMapOf(
            "DRAGON" to 2.0,
            "TIGER" to 2.0,
            "TIE" to 9.0
        )

        val details = mapOf(
            "dragonCard" to mapOf(
                "rankName" to dragonCard.rankName,
                "value" to dragonCard.value,
                "suit" to dragonCard.suit,
                "isRed" to dragonCard.isRed
            ),
            "tigerCard" to mapOf(
                "rankName" to tigerCard.rankName,
                "value" to tigerCard.value,
                "suit" to tigerCard.suit,
                "isRed" to tigerCard.isRed
            ),
            "winningSide" to winningSide
        )

        return ServerRoundOutcome(
            outcomeName = winningSide,
            outcomeDetails = details,
            winningTargets = winners,
            targetMultipliers = multipliers
        )
    }

    fun generateJdiLionOutcome(): ServerRoundOutcome {
        // Symbols and their weights / payout multipliers
        val animals = listOf(
            Triple("LION", 12.0, 10),
            Triple("EAGLE", 12.0, 10),
            Triple("PANDA", 8.0, 14),
            Triple("PEACOCK", 8.0, 14),
            Triple("MONKEY", 8.0, 14),
            Triple("PIGEON", 8.0, 14),
            Triple("RABBIT", 6.0, 18),
            Triple("SWALLOW", 6.0, 18),
            Triple("GOLD_SHARK", 24.0, 4),
            Triple("SILVER_SHARK", 24.0, 4)
        )

        val totalWeight = animals.sumOf { it.third }
        var rnd = secureRandom.nextInt(totalWeight)
        var selected = animals[0]

        for (item in animals) {
            if (rnd < item.third) {
                selected = item
                break
            }
            rnd -= item.third
        }

        val isAir = selected.first in listOf("EAGLE", "PEACOCK", "PIGEON", "SWALLOW")
        val isLand = selected.first in listOf("LION", "PANDA", "MONKEY", "RABBIT")

        val winners = mutableListOf(selected.first)
        if (isAir) winners.add("AIR")
        if (isLand) winners.add("LAND")

        val multipliers = mapOf(
            selected.first to selected.second,
            "AIR" to 2.0,
            "LAND" to 2.0
        )

        val details = mapOf(
            "selectedAnimal" to selected.first,
            "multiplier" to selected.second,
            "category" to if (isAir) "AIR" else if (isLand) "LAND" else "SHARK"
        )

        return ServerRoundOutcome(
            outcomeName = selected.first,
            outcomeDetails = details,
            winningTargets = winners,
            targetMultipliers = multipliers
        )
    }

    fun generateGadiRacingOutcome(): ServerRoundOutcome {
        val cars = listOf(
            Pair("RED_CAR", 3.0),
            Pair("BLUE_CAR", 3.0),
            Pair("GOLD_CAR", 5.0),
            Pair("GREEN_CAR", 3.0),
            Pair("CYBER_CAR", 10.0)
        )
        val selected = cars[secureRandom.nextInt(cars.size)]
        return ServerRoundOutcome(
            outcomeName = selected.first,
            outcomeDetails = mapOf("winningCar" to selected.first, "speedKmh" to (280 + secureRandom.nextInt(60))),
            winningTargets = listOf(selected.first),
            targetMultipliers = mapOf(selected.first to selected.second)
        )
    }

    // ==========================================
    // 3. GAME SESSIONS & REAL-TIME STATE
    // ==========================================

    fun getActiveSessionStream(sessionId: String): Flow<GameSession?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || sessionId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        trySend(snapshot.toObject(GameSession::class.java))
                    } else {
                        trySend(null)
                    }
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getSessionMembersStream(sessionId: String): Flow<List<GameMember>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || sessionId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .collection("members")
                .orderBy("joinedAt", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val members = snapshot.documents.mapNotNull { it.toObject(GameMember::class.java) }
                    trySend(members)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getLatestRoundStream(sessionId: String): Flow<GameRound?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || sessionId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .collection("rounds")
                .orderBy("roundNumber", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val round = snapshot.documents.firstOrNull()?.toObject(GameRound::class.java)
                    trySend(round)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getPlayerRoundBetsStream(sessionId: String, roundId: String, uid: String): Flow<List<GameBet>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || sessionId.isBlank() || roundId.isBlank() || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .collection("rounds").document(roundId)
                .collection("bets")
                .whereEqualTo("uid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val bets = snapshot.documents.mapNotNull { it.toObject(GameBet::class.java) }
                    trySend(bets)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // 4. SESSION CREATION & LIFECYCLE MANAGEMENT
    // ==========================================

    suspend fun getOrCreateGlobalSession(
        gameId: String,
        gameName: String,
        userUid: String,
        publicUserId: String,
        displayName: String,
        photoUrl: String,
        voiceRoomId: String? = null
    ): GameSession = withContext(Dispatchers.IO) {
        val firestore = getFirestore()
        val defaultSessionId = "session_${gameId}"

        if (firestore == null) {
            return@withContext GameSession(
                sessionId = defaultSessionId,
                gameId = gameId,
                gameName = gameName,
                hostUid = userUid,
                hostPublicUserId = publicUserId,
                hostDisplayName = displayName,
                status = "RUNNING"
            )
        }

        try {
            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(defaultSessionId)
            val snap = sessionRef.get().await()

            if (snap.exists()) {
                val session = snap.toObject(GameSession::class.java)!!
                // Register member
                joinSession(defaultSessionId, userUid, publicUserId, displayName, photoUrl)
                return@withContext session
            } else {
                // Initialize session
                val newSession = GameSession(
                    sessionId = defaultSessionId,
                    gameId = gameId,
                    gameName = gameName,
                    hostUid = userUid,
                    hostPublicUserId = publicUserId,
                    hostDisplayName = displayName,
                    status = "RUNNING",
                    currentRoundId = "round_1",
                    currentRoundNumber = 1,
                    playerCount = 1,
                    linkedVoiceRoomId = voiceRoomId,
                    createdAt = System.currentTimeMillis(),
                    startedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                sessionRef.set(newSession).await()

                // Join member
                joinSession(defaultSessionId, userUid, publicUserId, displayName, photoUrl)

                // Initialize Round 1
                startNewRound(defaultSessionId, gameId, 1)

                return@withContext newSession
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext GameSession(sessionId = defaultSessionId, gameId = gameId, gameName = gameName)
        }
    }

    suspend fun joinSession(
        sessionId: String,
        uid: String,
        publicUserId: String,
        displayName: String,
        photoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        try {
            val memberRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .collection("members").document(uid)
            val member = GameMember(
                uid = uid,
                publicUserId = publicUserId,
                displayName = displayName,
                photoUrl = photoUrl,
                status = "ACTIVE",
                joinedAt = System.currentTimeMillis(),
                lastActiveAt = System.currentTimeMillis()
            )
            memberRef.set(member, SetOptions.merge()).await()

            // Update session player count
            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
            firestore.runTransaction { tx ->
                val snap = tx.get(sessionRef)
                if (snap.exists()) {
                    val count = snap.getLong("playerCount") ?: 1L
                    tx.update(sessionRef, "playerCount", count + 1L)
                    tx.update(sessionRef, "updatedAt", System.currentTimeMillis())
                }
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun leaveSession(sessionId: String, uid: String): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        try {
            val memberRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .collection("members").document(uid)
            memberRef.delete().await()

            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
            firestore.runTransaction { tx ->
                val snap = tx.get(sessionRef)
                if (snap.exists()) {
                    val count = snap.getLong("playerCount") ?: 1L
                    tx.update(sessionRef, "playerCount", maxOf(1L, count - 1L))
                    tx.update(sessionRef, "updatedAt", System.currentTimeMillis())
                }
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // 5. SERVER-AUTHORITATIVE ROUND STATE MACHINE
    // ==========================================

    suspend fun startNewRound(sessionId: String, gameId: String, roundNumber: Int): GameRound? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        try {
            val roundId = "round_${roundNumber}_${System.currentTimeMillis()}"
            val now = System.currentTimeMillis()
            val bettingDuration = 12000L
            val animationDuration = 5000L
            val resultDuration = 3000L

            val round = GameRound(
                roundId = roundId,
                sessionId = sessionId,
                gameId = gameId,
                roundNumber = roundNumber,
                status = "OPEN",
                createdAt = now,
                roundStartAt = now,
                bettingOpenAt = now,
                bettingClosedAt = now + bettingDuration,
                resultAt = now + bettingDuration + animationDuration,
                roundEndAt = now + bettingDuration + animationDuration + resultDuration,
                serverTimestamp = now
            )

            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
            val roundRef = sessionRef.collection("rounds").document(roundId)

            firestore.runTransaction { tx ->
                tx.set(roundRef, round)
                tx.update(sessionRef, "currentRoundId", roundId)
                tx.update(sessionRef, "currentRoundNumber", roundNumber)
                tx.update(sessionRef, "updatedAt", now)
            }.await()

            round
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Authoritatively generates round outcome, locks round, settles all player bets,
     * updates wallet ledgers and game history records atomically.
     */
    suspend fun settleRoundOutcome(
        sessionId: String,
        roundId: String,
        gameId: String
    ): ServerRoundOutcome? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        try {
            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
            val roundRef = sessionRef.collection("rounds").document(roundId)

            val roundSnap = roundRef.get().await()
            if (!roundSnap.exists()) return@withContext null
            val round = roundSnap.toObject(GameRound::class.java) ?: return@withContext null

            if (round.status == "RESULT_GENERATED" || round.status == "COMPLETED") {
                // Already settled
                return@withContext null
            }

            // 1. Authoritative Cryptographic outcome generation based on gameId
            val outcome = when (gameId) {
                "game_dragon_tiger" -> generateDragonTigerOutcome()
                "game_jdi_lion" -> generateJdiLionOutcome()
                "game_gadi_racing" -> generateGadiRacingOutcome()
                else -> generateDragonTigerOutcome()
            }

            val now = System.currentTimeMillis()

            // 2. Fetch all bets placed on this round
            val betsSnap = roundRef.collection("bets").get().await()
            val bets = betsSnap.documents.mapNotNull { it.toObject(GameBet::class.java) }

            var totalPayout = 0L

            // 3. Process payouts and history in a batch/transaction
            val batch = firestore.batch()

            // Mark round settled
            batch.update(
                roundRef,
                mapOf(
                    "status" to "RESULT_GENERATED",
                    "resultGeneratedAt" to now,
                    "resultOutcome" to outcome.outcomeName,
                    "resultData" to outcome.outcomeDetails,
                    "serverTimestamp" to now
                )
            )

            for (bet in bets) {
                val isWinner = outcome.winningTargets.contains(bet.betTarget)
                val multiplier = outcome.targetMultipliers[bet.betTarget] ?: 1.0
                val payout = if (isWinner) (bet.amount * multiplier).toLong() else 0L
                val netChange = if (isWinner) payout - bet.amount else -bet.amount

                totalPayout += payout

                // Update bet document
                val betDocRef = roundRef.collection("bets").document(bet.betId)
                batch.update(
                    betDocRef,
                    mapOf(
                        "status" to if (isWinner) "WON" else "LOST",
                        "multiplier" to multiplier,
                        "payoutAmount" to payout
                    )
                )

                // If won, credit the user's wallet
                if (isWinner && payout > 0) {
                    val walletRef = firestore.collection(WALLETS_COLLECTION).document(bet.uid)
                    batch.update(walletRef, "coinBalance", FieldValue.increment(payout))
                    batch.update(walletRef, "lifetimeReceivedCoins", FieldValue.increment(payout))
                    batch.update(walletRef, "updatedAt", now)

                    val userRef = firestore.collection(USERS_COLLECTION).document(bet.uid)
                    batch.update(userRef, "coinBalance", FieldValue.increment(payout))

                    // Record WIN transaction in ledger
                    val txId = "tx_game_win_${bet.betId}"
                    val ledgerTx = WalletTransaction(
                        transactionId = txId,
                        uid = bet.uid,
                        publicUserId = bet.publicUserId,
                        type = "GAME_WIN_PAYOUT",
                        amount = payout,
                        balanceBefore = 0L,
                        balanceAfter = payout,
                        referenceId = bet.betId,
                        status = "SUCCESS",
                        description = "Won ${payout} Coins in ${round.gameId} (${bet.betTarget} @ ${multiplier}x)",
                        createdAt = now,
                        serverTimestamp = now
                    )
                    batch.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), ledgerTx)
                }

                // Record history item
                val historyId = "hist_${bet.betId}"
                val historyItem = GameHistoryItem(
                    historyId = historyId,
                    gameId = bet.gameId,
                    gameName = when (bet.gameId) {
                        "game_dragon_tiger" -> "Dragon vs Tiger"
                        "game_jdi_lion" -> "JDI Lion King"
                        "game_gadi_racing" -> "Gadi Super Drift"
                        else -> "Mini Game"
                    },
                    sessionId = sessionId,
                    roundId = roundId,
                    roundNumber = round.roundNumber,
                    uid = bet.uid,
                    publicUserId = bet.publicUserId,
                    displayName = bet.displayName,
                    betTarget = bet.betTarget,
                    betAmount = bet.amount,
                    outcome = outcome.outcomeName,
                    rewardCoins = payout,
                    netChange = netChange,
                    status = "SETTLED",
                    createdAt = now,
                    serverTimestamp = now
                )
                batch.set(firestore.collection(HISTORY_COLLECTION).document(historyId), historyItem)

                // Update user game statistics
                val statsRef = firestore.collection(USERS_COLLECTION).document(bet.uid)
                    .collection("gameStats").document(bet.gameId)
                batch.set(
                    statsRef,
                    mapOf(
                        "uid" to bet.uid,
                        "gameId" to bet.gameId,
                        "gamesPlayed" to FieldValue.increment(1),
                        "roundsPlayed" to FieldValue.increment(1),
                        "wins" to FieldValue.increment(if (isWinner) 1 else 0),
                        "losses" to FieldValue.increment(if (isWinner) 0 else 1),
                        "totalBetsCoins" to FieldValue.increment(bet.amount),
                        "totalRewardsCoins" to FieldValue.increment(payout),
                        "netEarningsCoins" to FieldValue.increment(netChange),
                        "lastPlayedAt" to now
                    ),
                    SetOptions.merge()
                )

                // Update leaderboard score (XP / Points based on participation and wins)
                val pointsGained = if (isWinner) (payout / 10L) + 50L else (bet.amount / 50L) + 10L
                val leaderboardRef = firestore.collection(LEADERBOARD_COLLECTION).document("season_current")
                    .collection("players").document(bet.uid)
                batch.set(
                    leaderboardRef,
                    mapOf(
                        "uid" to bet.uid,
                        "publicUserId" to bet.publicUserId,
                        "displayName" to bet.displayName,
                        "photoUrl" to bet.avatarUrl,
                        "score" to FieldValue.increment(pointsGained),
                        "gamesWon" to FieldValue.increment(if (isWinner) 1 else 0),
                        "totalRounds" to FieldValue.increment(1),
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                )
            }

            batch.commit().await()
            outcome
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 6. ATOMIC BET PLACEMENT & IDEMPOTENCY
    // ==========================================

    suspend fun placeGameBet(
        sessionId: String,
        roundId: String,
        gameId: String,
        userUid: String,
        publicUserId: String,
        displayName: String,
        avatarUrl: String,
        betTarget: String,
        amount: Long,
        operationId: String = UUID.randomUUID().toString()
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (amount <= 0) return@withContext Pair(false, "Invalid bet amount")
        val firestore = getFirestore() ?: return@withContext Pair(false, "Database connection unavailable")

        try {
            val sessionRef = firestore.collection(SESSIONS_COLLECTION).document(sessionId)
            val roundRef = sessionRef.collection("rounds").document(roundId)
            val walletRef = firestore.collection(WALLETS_COLLECTION).document(userUid)
            val userRef = firestore.collection(USERS_COLLECTION).document(userUid)
            val betId = "bet_${operationId}"
            val betRef = roundRef.collection("bets").document(betId)

            val success = firestore.runTransaction { tx ->
                // Check if bet was already placed with this operationId (Idempotency)
                val existingBetSnap = tx.get(betRef)
                if (existingBetSnap.exists()) {
                    return@runTransaction true
                }

                // Verify round state and timing
                val roundSnap = tx.get(roundRef)
                if (!roundSnap.exists()) {
                    throw IllegalStateException("Round does not exist")
                }
                val status = roundSnap.getString("status") ?: "OPEN"
                val bettingClosedAt = roundSnap.getLong("bettingClosedAt") ?: 0L
                val now = System.currentTimeMillis()

                if (status != "OPEN" || now > bettingClosedAt) {
                    throw IllegalStateException("Betting is closed for this round")
                }

                // Verify and deduct wallet balance
                val walletSnap = tx.get(walletRef)
                val currentBalance = if (walletSnap.exists()) {
                    walletSnap.getLong("coinBalance") ?: 0L
                } else {
                    val uSnap = tx.get(userRef)
                    uSnap.getLong("coinBalance") ?: 0L
                }

                if (currentBalance < amount) {
                    throw IllegalStateException("Insufficient coins. Please recharge.")
                }

                val newBalance = currentBalance - amount

                // Update wallet
                tx.update(walletRef, "coinBalance", newBalance)
                tx.update(walletRef, "lifetimeSpentCoins", (walletSnap.getLong("lifetimeSpentCoins") ?: 0L) + amount)
                tx.update(walletRef, "updatedAt", now)

                // Update user document
                tx.update(userRef, "coinBalance", newBalance)

                // Update round total bet coins
                val curTotal = roundSnap.getLong("totalBetCoins") ?: 0L
                tx.update(roundRef, "totalBetCoins", curTotal + amount)

                // Create GameBet document
                val gameBet = GameBet(
                    betId = betId,
                    roundId = roundId,
                    sessionId = sessionId,
                    gameId = gameId,
                    uid = userUid,
                    publicUserId = publicUserId,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    betTarget = betTarget,
                    amount = amount,
                    operationId = operationId,
                    status = "PENDING",
                    placedAt = now
                )
                tx.set(betRef, gameBet)

                // Write immutable transaction ledger
                val ledgerTxId = "tx_game_bet_${betId}"
                val ledgerTx = WalletTransaction(
                    transactionId = ledgerTxId,
                    uid = userUid,
                    publicUserId = publicUserId,
                    type = "GAME_BET",
                    amount = -amount,
                    balanceBefore = currentBalance,
                    balanceAfter = newBalance,
                    referenceId = betId,
                    status = "SUCCESS",
                    description = "Placed ${amount} coins on ${betTarget} in ${gameId}",
                    createdAt = now,
                    serverTimestamp = now
                )
                tx.set(firestore.collection(TRANSACTIONS_COLLECTION).document(ledgerTxId), ledgerTx)

                true
            }.await()

            if (success) {
                Pair(true, "Bet placed successfully")
            } else {
                Pair(false, "Failed to place bet")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, e.message ?: "Transaction error occurred")
        }
    }

    // ==========================================
    // 7. GAME HISTORY & LEADERBOARD
    // ==========================================

    fun getGameHistoryStream(uid: String, limit: Long = 30): Flow<List<GameHistoryItem>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(HISTORY_COLLECTION)
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.documents.mapNotNull { it.toObject(GameHistoryItem::class.java) }
                    trySend(items)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getLeaderboardStream(limit: Long = 50): Flow<List<GameLeaderboardEntry>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(LEADERBOARD_COLLECTION).document("season_current")
                .collection("players")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                        val item = doc.toObject(GameLeaderboardEntry::class.java)
                        item?.copy(
                            rank = index + 1,
                            tier = when {
                                index == 0 -> "Crown Master 👑"
                                index in 1..2 -> "Grand Platinum 💎"
                                index in 3..9 -> "Gold Master 🥇"
                                index in 10..24 -> "Silver Star 🥈"
                                else -> "Bronze Challenger 🥉"
                            }
                        )
                    }
                    trySend(entries)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // 8. SEASONAL GAME EVENTS & INVITATIONS
    // ==========================================

    fun getActiveGameEventsStream(): Flow<List<GameEvent>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultGameEvents())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(EVENTS_COLLECTION)
                .whereEqualTo("enabled", true)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(getDefaultGameEvents())
                        return@addSnapshotListener
                    }
                    val now = System.currentTimeMillis()
                    val events = snapshot.documents.mapNotNull { it.toObject(GameEvent::class.java) }
                        .filter { it.enabled && now in it.startAt..it.endAt }
                    trySend(if (events.isNotEmpty()) events else getDefaultGameEvents())
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getDefaultGameEvents())
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun getDefaultGameEvents(): List<GameEvent> {
        val now = System.currentTimeMillis()
        return listOf(
            GameEvent(
                eventId = "event_dragon_championship",
                name = "Dragon vs Tiger Grand Championship",
                description = "Compete for a 10,000,000 Coin Prize Pool & Exclusive VIP Dragon Avatar Frame!",
                badge = "🏆 SEASON CHAMPIONSHIP",
                rewardPoolCoins = 10000000L,
                startAt = now - 86400000L * 2,
                endAt = now + 86400000L * 5,
                enabled = true
            ),
            GameEvent(
                eventId = "event_safari_fever",
                name = "JDI Lion Safari Double XP Days",
                description = "Earn 2X leaderboard score and special Golden Shark badges every hour!",
                badge = "⚡ 2X XP FEVER",
                rewardPoolCoins = 5000000L,
                startAt = now - 86400000L,
                endAt = now + 86400000L * 4,
                enabled = true
            )
        )
    }

    suspend fun sendGameInvitation(
        senderUid: String,
        senderPublicId: String,
        senderDisplayName: String,
        senderAvatarUrl: String,
        recipientUid: String,
        gameId: String,
        gameName: String,
        sessionId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        try {
            val inviteId = "inv_${UUID.randomUUID()}"
            val invite = GameInvitation(
                invitationId = inviteId,
                senderUid = senderUid,
                senderPublicId = senderPublicId,
                senderDisplayName = senderDisplayName,
                senderAvatarUrl = senderAvatarUrl,
                recipientUid = recipientUid,
                gameId = gameId,
                gameName = gameName,
                sessionId = sessionId,
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            firestore.collection(INVITATIONS_COLLECTION).document(inviteId).set(invite).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getIncomingGameInvitationsStream(uid: String): Flow<List<GameInvitation>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(INVITATIONS_COLLECTION)
                .whereEqualTo("recipientUid", uid)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val invites = snapshot.documents.mapNotNull { it.toObject(GameInvitation::class.java) }
                    trySend(invites)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // 9. GAME ADMIN CONTROLS
    // ==========================================

    suspend fun updateGameStatus(gameId: String, newStatus: String, isEnabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        try {
            firestore.collection(GAMES_COLLECTION).document(gameId).update(
                mapOf(
                    "status" to newStatus,
                    "enabled" to isEnabled,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            firestore.collection(GAME_CONFIGS_COLLECTION).document(gameId).update(
                mapOf(
                    "enabled" to isEnabled,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
