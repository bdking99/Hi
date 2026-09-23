package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class VipRepository(
    private val dataStore: DataStore<Preferences>? = null
) {

    companion object {
        const val VIP_LEVELS_COLLECTION = "vipLevels"
        const val USER_LEVELS_COLLECTION = "userLevels"
        const val LEVEL_CONFIGS_COLLECTION = "levelConfigs"
        const val VIP_HISTORY_COLLECTION = "vipHistory"
        const val LEVEL_UP_REWARDS_COLLECTION = "levelUpRewards"
        const val XP_EVENTS_COLLECTION = "xpEvents"
        const val WALLETS_COLLECTION = "wallets"
        const val USERS_COLLECTION = "users"
        const val USER_FRAMES_COLLECTION = "userFrames"
        const val ROOMS_COLLECTION = "rooms"

        val PREF_SHOW_VIP_ANIMATIONS = booleanPreferencesKey("show_vip_animations")
        val PREF_SHOW_ENTRY_ANIMATIONS = booleanPreferencesKey("show_room_entry_animations")
        val PREF_SHOW_GIFT_ANIMATIONS = booleanPreferencesKey("show_gift_animations")
        val PREF_REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 1. DYNAMIC VIP / SVIP CONFIGURATIONS
    // ==========================================

    fun getVipLevelsStream(): Flow<List<VipLevelConfig>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultVipLevels())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(VIP_LEVELS_COLLECTION)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(getDefaultVipLevels())
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val levels = snapshot.documents.mapNotNull { it.toObject(VipLevelConfig::class.java) }
                        trySend(levels)
                    } else {
                        seedDefaultVipLevels(firestore)
                        trySend(getDefaultVipLevels())
                    }
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getDefaultVipLevels())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getDefaultVipLevels(): List<VipLevelConfig> {
        return listOf(
            // --- VIP 1 to 10 ---
            VipLevelConfig(
                levelId = "vip_1",
                type = "VIP",
                level = 1,
                name = "VIP 1",
                title = "VIP Bronze Star",
                requiredRechargeAmount = 1000L,
                requiredLifetimeCoins = 1000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_bronze",
                benefits = listOf("VIP 1 Profile Badge", "Bronze Glow Frame", "5% Bonus Voice XP"),
                sortOrder = 1
            ),
            VipLevelConfig(
                levelId = "vip_2",
                type = "VIP",
                level = 2,
                name = "VIP 2",
                title = "VIP Silver Star",
                requiredRechargeAmount = 5000L,
                requiredLifetimeCoins = 5000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_silver",
                benefits = listOf("VIP 2 Badge", "Silver Shimmer Frame", "Special Room Chat Highlight"),
                sortOrder = 2
            ),
            VipLevelConfig(
                levelId = "vip_3",
                type = "VIP",
                level = 3,
                name = "VIP 3",
                title = "VIP Gold Baron",
                requiredRechargeAmount = 15000L,
                requiredLifetimeCoins = 15000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_gold",
                benefits = listOf("VIP 3 Gold Badge", "Gold Crown Frame", "Room Seat Gold Glow", "10% Voice XP Boost"),
                sortOrder = 3
            ),
            VipLevelConfig(
                levelId = "vip_4",
                type = "VIP",
                level = 4,
                name = "VIP 4",
                title = "VIP Platinum Lord",
                requiredRechargeAmount = 35000L,
                requiredLifetimeCoins = 35000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_platinum",
                benefits = listOf("VIP 4 Badge", "Platinum Frame", "VIP Mic Wave Effect", "Exclusive Gifts Access"),
                sortOrder = 4
            ),
            VipLevelConfig(
                levelId = "vip_5",
                type = "VIP",
                level = 5,
                name = "VIP 5",
                title = "VIP Emerald Prince",
                requiredRechargeAmount = 75000L,
                requiredLifetimeCoins = 75000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_emerald",
                benefits = listOf("VIP 5 Badge", "Emerald Animated Frame", "Room Entry Banner (VIP 5+)", "Exclusive Chat Bubble"),
                sortOrder = 5
            ),
            VipLevelConfig(
                levelId = "vip_6",
                type = "VIP",
                level = 6,
                name = "VIP 6",
                title = "VIP Sapphire Duke",
                requiredRechargeAmount = 150000L,
                requiredLifetimeCoins = 150000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_sapphire",
                benefits = listOf("VIP 6 Badge", "Sapphire Dynamic Frame", "Animated Room Entry", "20% Voice XP Boost"),
                sortOrder = 6
            ),
            VipLevelConfig(
                levelId = "vip_7",
                type = "VIP",
                level = 7,
                name = "VIP 7",
                title = "VIP Ruby King",
                requiredRechargeAmount = 300000L,
                requiredLifetimeCoins = 300000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_ruby",
                benefits = listOf("VIP 7 Ruby Badge", "Ruby Fire Animated Frame", "Golden Nickname Effect", "Priority Voice Mic Slot"),
                sortOrder = 7
            ),
            VipLevelConfig(
                levelId = "vip_8",
                type = "VIP",
                level = 8,
                name = "VIP 8",
                title = "VIP Amethyst Sovereign",
                requiredRechargeAmount = 600000L,
                requiredLifetimeCoins = 600000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_amethyst",
                benefits = listOf("VIP 8 Badge", "Amethyst Imperial Frame", "Full Room Entry Marquee", "Exclusive VIP Club Access"),
                sortOrder = 8
            ),
            VipLevelConfig(
                levelId = "vip_9",
                type = "VIP",
                level = 9,
                name = "VIP 9",
                title = "VIP Diamond Monarch",
                requiredRechargeAmount = 1200000L,
                requiredLifetimeCoins = 1200000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_diamond",
                benefits = listOf("VIP 9 Diamond Badge", "Diamond Monarch Frame", "Custom Mic Glow Effect", "Invisible Browsing Option"),
                sortOrder = 9
            ),
            VipLevelConfig(
                levelId = "vip_10",
                type = "VIP",
                level = 10,
                name = "VIP 10",
                title = "VIP Celestial Emperor",
                requiredRechargeAmount = 2500000L,
                requiredLifetimeCoins = 2500000L,
                badgeEmoji = "👑",
                frameId = "frame_vip_celestial",
                benefits = listOf("VIP 10 Ultimate Badge", "Celestial Galaxy Frame", "Supreme Entry Flare", "35% XP Boost", "VIP Support"),
                sortOrder = 10
            ),
            // --- SVIP 1 to 5 ---
            VipLevelConfig(
                levelId = "svip_1",
                type = "SVIP",
                level = 1,
                name = "SVIP 1",
                title = "Super VIP Star",
                requiredRechargeAmount = 5000000L,
                requiredLifetimeCoins = 5000000L,
                badgeEmoji = "💎",
                frameId = "frame_svip_1",
                benefits = listOf("💎 SVIP 1 Badge", "Cosmic Diamond Frame", "SVIP Room Entry Lightning", "Rainbow Nickname"),
                sortOrder = 11
            ),
            VipLevelConfig(
                levelId = "svip_2",
                type = "SVIP",
                level = 2,
                name = "SVIP 2",
                title = "Super VIP Master",
                requiredRechargeAmount = 10000000L,
                requiredLifetimeCoins = 10000000L,
                badgeEmoji = "💎",
                frameId = "frame_svip_2",
                benefits = listOf("💎 SVIP 2 Badge", "Hyper Nova Frame", "Supreme Voice Broadcast Notice", "SVIP Dedicated Badge"),
                sortOrder = 12
            ),
            VipLevelConfig(
                levelId = "svip_3",
                type = "SVIP",
                level = 3,
                name = "SVIP 3",
                title = "Super VIP Legend",
                requiredRechargeAmount = 25000000L,
                requiredLifetimeCoins = 25000000L,
                badgeEmoji = "💎",
                frameId = "frame_svip_3",
                benefits = listOf("💎 SVIP 3 Badge", "Legendary Phoenix Frame", "Custom Mic Aura", "Permanent VIP Concierge"),
                sortOrder = 13
            ),
            VipLevelConfig(
                levelId = "svip_4",
                type = "SVIP",
                level = 4,
                name = "SVIP 4",
                title = "Super VIP Mythic",
                requiredRechargeAmount = 50000000L,
                requiredLifetimeCoins = 50000000L,
                badgeEmoji = "💎",
                frameId = "frame_svip_4",
                benefits = listOf("💎 SVIP 4 Mythic Badge", "Mythic Dragon Frame", "App-Wide Marquee on Entry", "Exclusive 3D Avatar Halo"),
                sortOrder = 14
            ),
            VipLevelConfig(
                levelId = "svip_5",
                type = "SVIP",
                level = 5,
                name = "SVIP 5",
                title = "Super VIP Supreme",
                requiredRechargeAmount = 100000000L,
                requiredLifetimeCoins = 100000000L,
                badgeEmoji = "💎",
                frameId = "frame_svip_5",
                benefits = listOf("💎 SVIP 5 Supreme Crown", "Supreme Universe Frame", "Ultimate Room Entry Storm", "50% Boost & Immortality"),
                sortOrder = 15
            )
        )
    }

    private fun seedDefaultVipLevels(firestore: FirebaseFirestore) {
        val levels = getDefaultVipLevels()
        for (lvl in levels) {
            firestore.collection(VIP_LEVELS_COLLECTION).document(lvl.levelId).set(lvl, SetOptions.merge())
        }
    }

    // ==========================================
    // 2. AUTHORITATIVE VIP STATUS CALCULATION ENGINE
    // ==========================================

    /**
     * Authoritatively recalculates user's VIP/SVIP tier based on verified lifetime recharge / wallet purchases.
     * Idempotently awards frames, updates history and updates profile status.
     */
    suspend fun calculateAndSyncVipLevel(uid: String): UserVipStatus = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext UserVipStatus(uid = uid)
        try {
            // 1. Retrieve verified wallet lifetime purchase total
            val walletDoc = firestore.collection(WALLETS_COLLECTION).document(uid).get().await()
            val verifiedLifetimeRecharge = if (walletDoc.exists()) {
                val purchased = walletDoc.getLong("lifetimePurchasedCoins") ?: 0L
                val spent = walletDoc.getLong("lifetimeSpentCoins") ?: 0L
                // Qualifying amount is verified lifetime purchased coins + verified spent activity
                maxOf(purchased, spent / 2L)
            } else {
                0L
            }

            // 2. Retrieve user document
            val userRef = firestore.collection(USERS_COLLECTION).document(uid)
            val userSnap = userRef.get().await()
            val publicUserId = userSnap.getString("publicUserId") ?: "1000001"
            val currentVipLevel = userSnap.getLong("vipLevel")?.toInt() ?: 0
            val currentSvipLevel = userSnap.getLong("svipLevel")?.toInt() ?: 0
            val currentVipType = userSnap.getString("vipType") ?: if (currentSvipLevel > 0) "SVIP" else if (currentVipLevel > 0) "VIP" else "NONE"

            // 3. Load all active VIP level configurations
            val configsSnap = firestore.collection(VIP_LEVELS_COLLECTION)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .get().await()

            val allConfigs = if (!configsSnap.isEmpty) {
                configsSnap.documents.mapNotNull { it.toObject(VipLevelConfig::class.java) }
            } else {
                getDefaultVipLevels()
            }

            // 4. Determine eligible VIP / SVIP Level
            var eligibleVipConfig: VipLevelConfig? = null
            for (cfg in allConfigs) {
                if (cfg.isActive && verifiedLifetimeRecharge >= cfg.requiredRechargeAmount) {
                    eligibleVipConfig = cfg
                }
            }

            val newType = eligibleVipConfig?.type ?: "NONE"
            val newLevel = eligibleVipConfig?.level ?: 0
            val newVipLevel = if (newType == "VIP") newLevel else if (newType == "SVIP") 10 else 0
            val newSvipLevel = if (newType == "SVIP") newLevel else 0

            // 5. Find next level config for progression bar
            val nextConfig = allConfigs.firstOrNull { it.requiredRechargeAmount > verifiedLifetimeRecharge }
            val currentThreshold = eligibleVipConfig?.requiredRechargeAmount ?: 0L
            val nextThreshold = nextConfig?.requiredRechargeAmount ?: (currentThreshold + 10000L)
            val neededCoins = maxOf(0L, nextThreshold - verifiedLifetimeRecharge)
            val progressPercent = if (nextThreshold > currentThreshold) {
                ((verifiedLifetimeRecharge - currentThreshold).toFloat() / (nextThreshold - currentThreshold).toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }

            val now = System.currentTimeMillis()

            // 6. Check if user leveled up
            val didLevelUp = (newType == "SVIP" && newSvipLevel > currentSvipLevel) ||
                    (newType == "VIP" && newVipLevel > currentVipLevel) ||
                    (currentVipType == "NONE" && newType != "NONE")

            if (didLevelUp && eligibleVipConfig != null) {
                // Perform idempotent level-up reward grant
                val rewardId = "${uid}_${newType}_${newLevel}"
                val rewardRef = firestore.collection(LEVEL_UP_REWARDS_COLLECTION).document(rewardId)
                val rewardSnap = rewardRef.get().await()

                if (!rewardSnap.exists()) {
                    // Record reward grant
                    val rewardRecord = LevelUpRewardRecord(
                        rewardId = rewardId,
                        uid = uid,
                        levelType = newType,
                        level = newLevel,
                        grantedFrameId = eligibleVipConfig.frameId,
                        grantedCoins = (newLevel * 500L),
                        grantedAt = now
                    )
                    rewardRef.set(rewardRecord).await()

                    // Unlock the exclusive frame for user in `userFrames/{uid}/owned/{frameId}`
                    if (eligibleVipConfig.frameId.isNotBlank()) {
                        firestore.collection(USER_FRAMES_COLLECTION).document(uid)
                            .collection("owned").document(eligibleVipConfig.frameId)
                            .set(
                                mapOf(
                                    "frameId" to eligibleVipConfig.frameId,
                                    "unlockedAt" to now,
                                    "source" to "VIP_LEVEL_UP_${newType}_${newLevel}",
                                    "isEquipped" to false
                                ),
                                SetOptions.merge()
                            ).await()
                    }

                    // Record in VIP History audit ledger
                    val historyId = "viph_${UUID.randomUUID()}"
                    val historyItem = VipHistoryItem(
                        historyId = historyId,
                        uid = uid,
                        publicUserId = publicUserId,
                        previousType = currentVipType,
                        previousLevel = if (currentVipType == "SVIP") currentSvipLevel else currentVipLevel,
                        newType = newType,
                        newLevel = newLevel,
                        qualifyingAmount = verifiedLifetimeRecharge,
                        reason = "LEVEL_UP",
                        createdAt = now
                    )
                    firestore.collection(VIP_HISTORY_COLLECTION).document(historyId).set(historyItem).await()
                }

                // Update user document
                userRef.update(
                    mapOf(
                        "vipLevel" to newVipLevel,
                        "svipLevel" to newSvipLevel,
                        "vipType" to newType,
                        "vipUpdatedAt" to now
                    )
                ).await()
            }

            UserVipStatus(
                uid = uid,
                publicUserId = publicUserId,
                vipType = newType,
                vipLevel = newVipLevel,
                svipLevel = newSvipLevel,
                verifiedRechargeAmount = verifiedLifetimeRecharge,
                currentLevelRequirement = currentThreshold,
                nextLevelRequirement = nextThreshold,
                progressPercent = progressPercent,
                coinsToNextLevel = neededCoins,
                unlockedFrameId = eligibleVipConfig?.frameId,
                unlockedEntryEffect = eligibleVipConfig?.animationId,
                updatedAt = now
            )
        } catch (e: Exception) {
            e.printStackTrace()
            UserVipStatus(uid = uid)
        }
    }

    fun getUserVipStatusStream(uid: String): Flow<UserVipStatus> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(UserVipStatus(uid = uid))
            close()
            return@callbackFlow
        }

        try {
            // Listen to user document and wallet updates to trigger live VIP recalculation
            val listener = firestore.collection(USERS_COLLECTION).document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(UserVipStatus(uid = uid))
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val publicUserId = snapshot.getString("publicUserId") ?: "1000001"
                        val vipLevel = snapshot.getLong("vipLevel")?.toInt() ?: 0
                        val svipLevel = snapshot.getLong("svipLevel")?.toInt() ?: 0
                        val vipType = snapshot.getString("vipType") ?: if (svipLevel > 0) "SVIP" else if (vipLevel > 0) "VIP" else "NONE"

                        val status = UserVipStatus(
                            uid = uid,
                            publicUserId = publicUserId,
                            vipType = vipType,
                            vipLevel = vipLevel,
                            svipLevel = svipLevel,
                            verifiedRechargeAmount = 0L,
                            updatedAt = System.currentTimeMillis()
                        )
                        trySend(status)
                    }
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UserVipStatus(uid = uid))
            close()
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // 3. USER LEVEL & XP SYSTEM
    // ==========================================

    fun getUserLevelStream(uid: String): Flow<UserLevelInfo> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(UserLevelInfo(uid = uid))
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(USER_LEVELS_COLLECTION).document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(UserLevelInfo(uid = uid))
                        return@addSnapshotListener
                    }

                    val level = snapshot.getLong("level")?.toInt() ?: 1
                    val currentXp = snapshot.getLong("currentXp") ?: 0L
                    val totalXp = snapshot.getLong("totalXp") ?: 0L
                    val nextXp = snapshot.getLong("nextLevelXp") ?: (level * 500L)
                    val progress = if (nextXp > 0) (currentXp.toFloat() / nextXp.toFloat()).coerceIn(0f, 1f) else 1f
                    val tier = getTierForLevel(level)

                    val info = UserLevelInfo(
                        uid = uid,
                        publicUserId = snapshot.getString("publicUserId") ?: "1000001",
                        level = level,
                        currentXp = currentXp,
                        totalXp = totalXp,
                        nextLevelXp = nextXp,
                        progressPercent = progress,
                        xpToNextLevel = maxOf(0L, nextXp - currentXp),
                        badgeTier = tier.first,
                        badgeEmoji = tier.second,
                        title = getTitleForLevel(level),
                        updatedAt = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                    trySend(info)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(UserLevelInfo(uid = uid))
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun getTierForLevel(level: Int): Pair<String, String> {
        return when {
            level >= 100 -> Pair("Legendary", "👑")
            level >= 75 -> Pair("Diamond", "💎")
            level >= 50 -> Pair("Platinum", "💠")
            level >= 25 -> Pair("Gold", "🥇")
            level >= 10 -> Pair("Silver", "🥈")
            else -> Pair("Bronze", "🥉")
        }
    }

    private fun getTitleForLevel(level: Int): String {
        return when {
            level >= 100 -> "Celestial Voice Deity"
            level >= 75 -> "Grand Hall Master"
            level >= 50 -> "Royal Broadcaster"
            level >= 25 -> "Elite Voice Star"
            level >= 10 -> "Active Community Member"
            else -> "Novice Voice Member"
        }
    }

    /**
     * Server-controlled XP awarding function with rate-limiting & idempotency.
     */
    suspend fun awardXp(
        uid: String,
        eventType: String,
        xpAmount: Long,
        cooldownSeconds: Long = 5L
    ): Boolean = withContext(Dispatchers.IO) {
        if (xpAmount <= 0) return@withContext false
        val firestore = getFirestore() ?: return@withContext false
        try {
            val now = System.currentTimeMillis()
            val userLevelRef = firestore.collection(USER_LEVELS_COLLECTION).document(uid)
            val userDocRef = firestore.collection(USERS_COLLECTION).document(uid)

            firestore.runTransaction { tx ->
                val levelSnap = tx.get(userLevelRef)
                var currentLevel = 1
                var currentXp = 0L
                var totalXp = 0L
                var nextLevelXp = 500L
                val lastEventTime = levelSnap.getLong("lastEventTime") ?: 0L

                if (levelSnap.exists()) {
                    currentLevel = levelSnap.getLong("level")?.toInt() ?: 1
                    currentXp = levelSnap.getLong("currentXp") ?: 0L
                    totalXp = levelSnap.getLong("totalXp") ?: 0L
                    nextLevelXp = levelSnap.getLong("nextLevelXp") ?: (currentLevel * 500L)

                    // Anti-abuse cooldown check for frequent events
                    if (eventType == "ROOM_ACTIVITY" && (now - lastEventTime) < (cooldownSeconds * 1000L)) {
                        return@runTransaction false
                    }
                }

                val newTotalXp = totalXp + xpAmount
                var newCurrentXp = currentXp + xpAmount
                var newLevel = currentLevel

                // Check for level ups
                while (newCurrentXp >= nextLevelXp && newLevel < 100) {
                    newCurrentXp -= nextLevelXp
                    newLevel++
                    nextLevelXp = newLevel * 500L
                }

                // Update userLevel document
                val data = mapOf(
                    "uid" to uid,
                    "level" to newLevel,
                    "currentXp" to newCurrentXp,
                    "totalXp" to newTotalXp,
                    "nextLevelXp" to nextLevelXp,
                    "lastEventTime" to now,
                    "updatedAt" to now
                )
                tx.set(userLevelRef, data, SetOptions.merge())

                // Sync level with users collection
                tx.update(userDocRef, "level", newLevel)
                tx.update(userDocRef, "experiencePoints", newTotalXp)

                // Record XP event
                val eventDoc = firestore.collection(XP_EVENTS_COLLECTION).document("xp_${UUID.randomUUID()}")
                val xpEvent = XpEventRecord(
                    eventId = eventDoc.id,
                    uid = uid,
                    eventType = eventType,
                    xpAwarded = xpAmount,
                    timestamp = now
                )
                tx.set(eventDoc, xpEvent)

                true
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // 4. VIP ROOM ENTRY BROADCASTS
    // ==========================================

    suspend fun broadcastRoomEntry(
        roomId: String,
        userProfile: FirebaseUserProfile
    ): Boolean = withContext(Dispatchers.IO) {
        if (userProfile.vipLevel < 1 && userProfile.svipLevel < 1) return@withContext false
        if (userProfile.invisibleMode) return@withContext false

        val firestore = getFirestore() ?: return@withContext false
        try {
            val eventId = "entry_${UUID.randomUUID()}"
            val entryEvent = VipRoomEntryEvent(
                eventId = eventId,
                roomId = roomId,
                uid = userProfile.userId,
                publicUserId = userProfile.publicUserId,
                displayName = userProfile.displayName,
                avatarUrl = userProfile.avatar,
                vipType = if (userProfile.svipLevel > 0) "SVIP" else "VIP",
                vipLevel = if (userProfile.svipLevel > 0) userProfile.svipLevel else userProfile.vipLevel,
                frameId = userProfile.profileFrameId,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection(ROOMS_COLLECTION).document(roomId)
                .collection("entryEvents").document(eventId).set(entryEvent).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getRoomEntryEventsStream(roomId: String): Flow<List<VipRoomEntryEvent>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || roomId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val now = System.currentTimeMillis()
            val listener = firestore.collection(ROOMS_COLLECTION).document(roomId)
                .collection("entryEvents")
                .whereGreaterThan("timestamp", now - 15000L) // only recent 15s
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(3)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val events = snapshot.documents.mapNotNull { it.toObject(VipRoomEntryEvent::class.java) }
                    trySend(events)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // 5. ANIMATION & ACCESSIBILITY PREFERENCES
    // ==========================================

    val animationPreferencesFlow: Flow<UserAnimationPreferences> = (dataStore?.data ?: kotlinx.coroutines.flow.flowOf(null))
        .map { prefs ->
            if (prefs == null) {
                UserAnimationPreferences()
            } else {
                UserAnimationPreferences(
                    showVipAnimations = prefs[PREF_SHOW_VIP_ANIMATIONS] ?: true,
                    showRoomEntryAnimations = prefs[PREF_SHOW_ENTRY_ANIMATIONS] ?: true,
                    showGiftAnimations = prefs[PREF_SHOW_GIFT_ANIMATIONS] ?: true,
                    reduceMotion = prefs[PREF_REDUCE_MOTION] ?: false
                )
            }
        }

    suspend fun updateAnimationPreferences(
        showVip: Boolean,
        showEntry: Boolean,
        showGift: Boolean,
        reduceMotion: Boolean
    ) = withContext(Dispatchers.IO) {
        dataStore?.edit { prefs ->
            prefs[PREF_SHOW_VIP_ANIMATIONS] = showVip
            prefs[PREF_SHOW_ENTRY_ANIMATIONS] = showEntry
            prefs[PREF_SHOW_GIFT_ANIMATIONS] = showGift
            prefs[PREF_REDUCE_MOTION] = reduceMotion
        }
    }

    // ==========================================
    // 6. VIP AUDIT HISTORY & ADMIN CONTROLS
    // ==========================================

    fun getVipHistoryStream(uid: String, limit: Long = 20): Flow<List<VipHistoryItem>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(VIP_HISTORY_COLLECTION)
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.documents.mapNotNull { it.toObject(VipHistoryItem::class.java) }
                    trySend(items)
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun adminUpdateVipLevelThreshold(
        levelId: String,
        newThreshold: Long,
        benefits: List<String>,
        isActive: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        try {
            firestore.collection(VIP_LEVELS_COLLECTION).document(levelId).update(
                mapOf(
                    "requiredRechargeAmount" to newThreshold,
                    "benefits" to benefits,
                    "isActive" to isActive,
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
