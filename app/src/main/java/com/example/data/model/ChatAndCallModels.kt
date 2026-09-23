package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Keep
@IgnoreExtraProperties
data class DirectMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val senderPublicUserId: String = "",
    val messageType: String = "text", // "text", "call_log", "image", "system"
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val replyToMessageId: String? = null,
    val deleted: Boolean = false,
    val deletedAt: Long? = null,
    val status: String = "sent" // "sending", "sent", "delivered", "seen"
)

@Keep
@IgnoreExtraProperties
data class DirectConversation(
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    val lastMessageSenderId: String = "",
    val unreadCounts: Map<String, Long> = emptyMap()
)

data class DirectConversationItem(
    val conversation: DirectConversation,
    val otherUser: FirebaseUserProfile?,
    val unreadCount: Int = 0
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    val callId: String = "",
    val callerUid: String = "",
    val receiverUid: String = "",
    val callerPublicUserId: String = "",
    val receiverPublicUserId: String = "",
    val callerDisplayName: String = "",
    val callerAvatarUrl: String = "",
    val callerFrameId: String = "",
    val receiverDisplayName: String = "",
    val receiverAvatarUrl: String = "",
    val status: String = "ringing", // "ringing", "accepted", "rejected", "busy", "missed", "ended", "failed", "cancelled"
    val createdAt: Long = System.currentTimeMillis(),
    val acceptedAt: Long? = null,
    val endedAt: Long? = null,
    val endedBy: String? = null,
    val duration: Long = 0L, // in seconds
    val sdpOffer: String? = null,
    val sdpAnswer: String? = null
)

@Keep
@IgnoreExtraProperties
data class IceCandidatePayload(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class UserBlockRecord(
    val blockerUid: String = "",
    val blockedUid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
@IgnoreExtraProperties
data class UserReportRecord(
    val reportId: String = "",
    val reporterUid: String = "",
    val reportedUid: String = "",
    val conversationId: String? = null,
    val messageId: String? = null,
    val reason: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending"
)
