package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties

object NotificationTypes {
    const val FOLLOW = "FOLLOW"
    const val FOLLOW_REQUEST = "FOLLOW_REQUEST"
    const val FOLLOW_ACCEPTED = "FOLLOW_ACCEPTED"
    const val NEW_MESSAGE = "NEW_MESSAGE"
    const val MISSED_CALL = "MISSED_CALL"
    const val INCOMING_CALL = "INCOMING_CALL"
    const val GIFT_RECEIVED = "GIFT_RECEIVED"
    const val VIP_LEVEL_UP = "VIP_LEVEL_UP"
    const val NORMAL_LEVEL_UP = "NORMAL_LEVEL_UP"
    const val FRAME_UNLOCKED = "FRAME_UNLOCKED"
    const val GAME_INVITATION = "GAME_INVITATION"
    const val ROOM_INVITATION = "ROOM_INVITATION"
    const val ROOM_EVENT = "ROOM_EVENT"
    const val MENTION = "MENTION"
    const val SYSTEM = "SYSTEM"
    const val MODERATION = "MODERATION"
    const val REPORT_UPDATE = "REPORT_UPDATE"
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    val notificationId: String = "",
    val recipientUid: String = "",
    val senderUid: String = "",
    val senderPublicUserId: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val type: String = NotificationTypes.SYSTEM,
    val title: String = "",
    val body: String = "",
    val imageUrl: String? = null,
    val referenceId: String? = null,
    val referenceType: String? = null, // "USER", "CONVERSATION", "ROOM", "GAME", "VIP", "REPORT", "CALL"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

@Keep
@IgnoreExtraProperties
data class UserDeviceToken(
    val deviceId: String = "",
    val token: String = "",
    val platform: String = "Android",
    val appVersion: String = "1.0.0",
    val enabled: Boolean = true,
    val lastSeenAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class NotificationPreferences(
    val messages: Boolean = true,
    val calls: Boolean = true,
    val gifts: Boolean = true,
    val followers: Boolean = true,
    val roomInvitations: Boolean = true,
    val gameInvitations: Boolean = true,
    val vipLevelUpdates: Boolean = true,
    val systemNotifications: Boolean = true,
    val moderationUpdates: Boolean = true,
    val pushNotificationsEnabled: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class FollowingRecord(
    val uid: String = "",
    val targetUid: String = "",
    val targetPublicUserId: String = "",
    val targetDisplayName: String = "",
    val targetAvatar: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class FollowerRecord(
    val uid: String = "",
    val followerUid: String = "",
    val followerPublicUserId: String = "",
    val followerDisplayName: String = "",
    val followerAvatar: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class FollowRequest(
    val requestId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val senderPublicUserId: String = "",
    val receiverPublicUserId: String = "",
    val senderDisplayName: String = "",
    val senderAvatarUrl: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, CANCELLED
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class BlockedUserRecord(
    val targetUid: String = "",
    val targetPublicUserId: String = "",
    val targetDisplayName: String = "",
    val targetAvatar: String = "",
    val blockedAt: Long = System.currentTimeMillis(),
    val reason: String? = null
)

object ReportCategories {
    const val SPAM = "Spam"
    const val HARASSMENT = "Harassment"
    const val BULLYING = "Bullying"
    const val IMPERSONATION = "Impersonation"
    const val SCAM_FRAUD = "Scam/Fraud"
    const val INAPPROPRIATE_CONTENT = "Inappropriate Content"
    const val HATE_ABUSIVE = "Hate/Abusive Content"
    const val SEXUAL_CONTENT = "Sexual Content"
    const val VIOLENCE = "Violence"
    const val UNAUTHORIZED_PROMOTION = "Unauthorized Promotion"
    const val FAKE_ACCOUNT = "Fake Account"
    const val OTHER = "Other"

    val ALL_CATEGORIES = listOf(
        SPAM, HARASSMENT, BULLYING, IMPERSONATION, SCAM_FRAUD,
        INAPPROPRIATE_CONTENT, HATE_ABUSIVE, SEXUAL_CONTENT,
        VIOLENCE, UNAUTHORIZED_PROMOTION, FAKE_ACCOUNT, OTHER
    )
}

object ReportStatuses {
    const val PENDING = "PENDING"
    const val UNDER_REVIEW = "UNDER_REVIEW"
    const val ACTION_TAKEN = "ACTION_TAKEN"
    const val NO_ACTION = "NO_ACTION"
    const val DISMISSED = "DISMISSED"
    const val RESOLVED = "RESOLVED"

    val ALL_STATUSES = listOf(PENDING, UNDER_REVIEW, ACTION_TAKEN, NO_ACTION, DISMISSED, RESOLVED)
}

object ModerationActions {
    const val WARN = "WARN"
    const val MUTE = "MUTE"
    const val KICK_ROOM = "KICK_ROOM"
    const val BAN_ROOM = "BAN_ROOM"
    const val RESTRICT = "RESTRICT"
    const val SUSPEND = "SUSPEND"
    const val BAN = "BAN"
    const val UNBAN = "UNBAN"
    const val DISMISS_REPORT = "DISMISS_REPORT"
    const val RESOLVE_REPORT = "RESOLVE_REPORT"

    val ALL_ACTIONS = listOf(WARN, MUTE, KICK_ROOM, BAN_ROOM, RESTRICT, SUSPEND, BAN, UNBAN, DISMISS_REPORT, RESOLVE_REPORT)
}

@Keep
@IgnoreExtraProperties
data class ReportRecord(
    val reportId: String = "",
    val reporterUid: String = "",
    val reporterPublicUserId: String = "",
    val targetType: String = "USER", // "USER", "MESSAGE", "PROFILE", "ROOM", "ROOM_MESSAGE", "GIFT", "GAME", "OTHER"
    val targetId: String = "",
    val targetUid: String = "",
    val targetDisplayName: String = "",
    val reason: String = "",
    val description: String = "",
    val evidence: String = "",
    val status: String = "PENDING", // PENDING, UNDER_REVIEW, ACTION_TAKEN, NO_ACTION, DISMISSED, RESOLVED
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val assignedAdminUid: String? = null,
    val resolutionNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

object ModerationRoles {
    const val USER = "USER"
    const val MODERATOR = "MODERATOR"
    const val SENIOR_MODERATOR = "SENIOR_MODERATOR"
    const val ADMIN = "ADMIN"
    const val SUPER_ADMIN = "SUPER_ADMIN"

    fun isModeratorOrAbove(role: String): Boolean {
        return role in listOf(MODERATOR, SENIOR_MODERATOR, ADMIN, SUPER_ADMIN)
    }

    fun isAdminOrAbove(role: String): Boolean {
        return role in listOf(ADMIN, SUPER_ADMIN)
    }
}

object ModerationActionTypes {
    const val WARN = "WARN"
    const val MUTE = "MUTE"
    const val KICK_ROOM = "KICK_ROOM"
    const val BAN_ROOM = "BAN_ROOM"
    const val RESTRICT = "RESTRICT"
    const val SUSPEND = "SUSPEND"
    const val BAN = "BAN"
    const val UNBAN = "UNBAN"
    const val DISMISS_REPORT = "DISMISS_REPORT"
    const val RESOLVE_REPORT = "RESOLVE_REPORT"
}

@Keep
@IgnoreExtraProperties
data class ModerationAuditLog(
    val logId: String = "",
    val actorUid: String = "",
    val actorRole: String = ModerationRoles.MODERATOR,
    val actorDisplayName: String = "",
    val action: String = "",
    val targetUid: String = "",
    val targetType: String = "USER",
    val targetId: String = "",
    val reason: String = "",
    val duration: Long? = null, // In ms, or null if permanent
    val reportId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class AppealRecord(
    val appealId: String = "",
    val userUid: String = "",
    val userPublicId: String = "",
    val userDisplayName: String = "",
    val actionId: String = "",
    val reason: String = "",
    val message: String = "",
    val status: String = "PENDING", // PENDING, UNDER_REVIEW, ACCEPTED, REJECTED, CLOSED
    val reviewNotes: String? = null,
    val reviewedByUid: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
