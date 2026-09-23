package com.example.data.model

// 👑 VIP / SVIP CONFIGURATION - Stored in Firestore `vipLevels/{levelId}`
data class VipLevelConfig(
    val levelId: String = "",
    val type: String = "VIP", // "VIP" or "SVIP"
    val level: Int = 1,
    val name: String = "VIP 1",
    val title: String = "VIP Star",
    val requiredRechargeAmount: Long = 10000L,
    val requiredLifetimeCoins: Long = 10000L,
    val badgeImageUrl: String = "",
    val badgeEmoji: String = "👑",
    val frameId: String = "frame_vip_1",
    val animationId: String = "anim_vip_1",
    val iconUrl: String = "",
    val benefits: List<String> = emptyList(),
    val isActive: Boolean = true,
    val sortOrder: Int = 1,
    val durationType: String = "PERMANENT", // "PERMANENT", "MONTHLY", "CUSTOM"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// 📊 AUTHORITATIVE USER VIP STATUS - Computed from real verified recharge / wallet transactions
data class UserVipStatus(
    val uid: String = "",
    val publicUserId: String = "1000001",
    val vipType: String = "NONE", // "NONE", "VIP", "SVIP"
    val vipLevel: Int = 0,
    val svipLevel: Int = 0,
    val verifiedRechargeAmount: Long = 0L,
    val currentLevelRequirement: Long = 0L,
    val nextLevelRequirement: Long = 10000L,
    val progressPercent: Float = 0f,
    val coinsToNextLevel: Long = 10000L,
    val unlockedFrameId: String? = null,
    val unlockedEntryEffect: String? = null,
    val expiresAt: Long? = null,
    val isExpired: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

// 🌟 USER NORMAL LEVEL PROGRESSION - Stored in Firestore `userLevels/{uid}`
data class UserLevelInfo(
    val uid: String = "",
    val publicUserId: String = "1000001",
    val level: Int = 1,
    val currentXp: Long = 0L,
    val totalXp: Long = 0L,
    val nextLevelXp: Long = 500L,
    val progressPercent: Float = 0f,
    val xpToNextLevel: Long = 500L,
    val badgeTier: String = "Bronze", // Bronze, Silver, Gold, Platinum, Diamond, Legendary
    val badgeEmoji: String = "🥉",
    val title: String = "Novice Voice Member",
    val updatedAt: Long = System.currentTimeMillis()
)

// 🏆 NORMAL LEVEL CONFIGURATION - Stored in Firestore `levelConfigs/{level}`
data class LevelConfig(
    val level: Int = 1,
    val requiredXp: Long = 500L,
    val totalXpRequired: Long = 0L,
    val rewardFrameId: String? = null,
    val rewardCoins: Long = 0L,
    val badgeTier: String = "Bronze",
    val badgeEmoji: String = "🥉",
    val title: String = "Voice Explorer"
)

// 📜 VIP STATUS AUDIT HISTORY - Stored in Firestore `vipHistory/{historyId}`
data class VipHistoryItem(
    val historyId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val previousType: String = "NONE",
    val previousLevel: Int = 0,
    val newType: String = "VIP",
    val newLevel: Int = 1,
    val qualifyingAmount: Long = 0L,
    val reason: String = "LEVEL_UP", // "LEVEL_UP", "CONFIGURATION_UPDATE", "ADMIN_CORRECTION", "EXPIRATION", "REVOCATION"
    val createdAt: Long = System.currentTimeMillis()
)

// 🎁 IDEMPOTENT LEVEL-UP REWARD RECORD - Stored in Firestore `levelUpRewards/{rewardId}`
// rewardId = "${uid}_${levelType}_${level}"
data class LevelUpRewardRecord(
    val rewardId: String = "",
    val uid: String = "",
    val levelType: String = "VIP", // "VIP", "SVIP", "NORMAL"
    val level: Int = 1,
    val grantedFrameId: String? = null,
    val grantedCoins: Long = 0L,
    val grantedAt: Long = System.currentTimeMillis()
)

// ⚡ SERVER-CONTROLLED XP EVENT
data class XpEventRecord(
    val eventId: String = "",
    val uid: String = "",
    val eventType: String = "ROOM_JOINED", // "ROOM_JOINED", "ROOM_ACTIVITY", "MESSAGE_SENT", "GIFT_SENT", "GIFT_RECEIVED", "GAME_COMPLETED"
    val xpAwarded: Long = 10L,
    val timestamp: Long = System.currentTimeMillis()
)

// 🚀 VIP VOICE ROOM ENTRY BANNER EVENT - Broadcasted to room members when a VIP joins
data class VipRoomEntryEvent(
    val eventId: String = "",
    val roomId: String = "",
    val uid: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val vipType: String = "VIP",
    val vipLevel: Int = 1,
    val frameId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ⚙️ USER ANIMATION & ACCESSIBILITY PREFERENCES
data class UserAnimationPreferences(
    val showVipAnimations: Boolean = true,
    val showRoomEntryAnimations: Boolean = true,
    val showGiftAnimations: Boolean = true,
    val reduceMotion: Boolean = false
)
