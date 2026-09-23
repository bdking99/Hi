package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class SystemMetrics(
    val totalUsers: Long = 0L,
    val onlineUsers: Long = 0L,
    val activeRooms: Long = 0L,
    val activeVoiceSessions: Long = 0L,
    val totalTransactions: Long = 0L,
    val transactionVolume: Long = 0L,
    val giftsSentToday: Long = 0L,
    val pendingReports: Long = 0L,
    val activeGames: Long = 0L,
    val totalAppeals: Long = 0L,
    val serverStatus: String = "OPERATIONAL", // OPERATIONAL, MAINTENANCE, DEGRADED
    val lastAuditTimestamp: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class FeatureFlag(
    val featureId: String = "",
    val name: String = "",
    val description: String = "",
    val isEnabled: Boolean = true,
    val lastUpdatedBy: String = "",
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

object DefaultFeatureFlags {
    const val VOICE_ROOMS = "VOICE_ROOMS"
    const val PRIVATE_CALLS = "PRIVATE_CALLS"
    const val GIFTS = "GIFTS"
    const val WALLET = "WALLET"
    const val GAMES = "GAMES"
    const val VIP = "VIP"
    const val SVIP = "SVIP"
    const val FOLLOW = "FOLLOW"
    const val PUSH_NOTIFICATIONS = "PUSH_NOTIFICATIONS"

    val DEFAULT_LIST = listOf(
        FeatureFlag(VOICE_ROOMS, "Real-Time Voice Rooms", "Multi-seat WebRTC audio rooms and stage controls", true),
        FeatureFlag(PRIVATE_CALLS, "1-on-1 Voice Calling", "Direct encrypted voice calling between users", true),
        FeatureFlag(GIFTS, "Virtual Gift Economy", "Real-time gifts and room animated showcases", true),
        FeatureFlag(WALLET, "Coin Wallet & Recharge", "Virtual coin balance and transaction ledger", true),
        FeatureFlag(GAMES, "Game Center", "Lucky Wheel, Dice, and interactive voice room games", true),
        FeatureFlag(VIP, "VIP Membership Tiers", "Exclusive benefits, badges, and privilege perks", true),
        FeatureFlag(SVIP, "Super VIP (SVIP)", "Ultra luxury tier status and custom animations", true),
        FeatureFlag(FOLLOW, "Follow & Social Graph", "Following, followers, and private follow requests", true),
        FeatureFlag(PUSH_NOTIFICATIONS, "Push Notifications", "Firebase Cloud Messaging alerts and push tokens", true)
    )
}

@Keep
@IgnoreExtraProperties
data class SystemSettingsRecord(
    val maintenanceMode: Boolean = false,
    val allowNewRegistrations: Boolean = true,
    val defaultWelcomeCoins: Long = 2500L,
    val serverNotice: String = "",
    val minSupportedAppVersion: String = "1.0.0",
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLogRecord(
    val logId: String = "",
    val actorUid: String = "",
    val actorRole: String = "ADMIN",
    val actorDisplayName: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val metadata: Map<String, Any?> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
)

object AdminActionTypes {
    const val ROLE_CHANGE = "ROLE_CHANGE"
    const val WALLET_ADJUSTMENT = "WALLET_ADJUSTMENT"
    const val USER_SANCTION = "USER_SANCTION"
    const val USER_RESTORE = "USER_RESTORE"
    const val GIFT_MANAGED = "GIFT_MANAGED"
    const val FRAME_MANAGED = "FRAME_MANAGED"
    const val FEATURE_FLAG_TOGGLE = "FEATURE_FLAG_TOGGLE"
    const val SETTINGS_UPDATE = "SETTINGS_UPDATE"
    const val ROOM_TERMINATED = "ROOM_TERMINATED"
    const val GAME_CONFIG_UPDATE = "GAME_CONFIG_UPDATE"
}
