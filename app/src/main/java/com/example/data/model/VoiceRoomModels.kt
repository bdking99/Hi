package com.example.data.model

// 🪑 SEAT DATA - Added monetization tracking and VIP visual states
data class SeatData(
    val seatIndex: Int = 0,
    val userId: String? = null,
    val userName: String? = null,
    val userAvatar: String? = null,
    val avatarFrameUrl: String? = null, // VIP animated border around the avatar
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val vipLevel: Int = 0,
    val seatEarnings: Long = 0L,        // Track how many coins this specific seat earned this session
    val isHost: Boolean = false,        // Differentiate the main host from co-hosts/guests
    val isCoHost: Boolean = false       // Co-host assigned by room host
)

// 👥 ROOM MEMBER - Stored in Firestore `rooms/{roomId}/members/{uid}`
data class RoomMember(
    val uid: String = "",
    val roomId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val role: String = "AUDIENCE", // "HOST", "CO_HOST", "SPEAKER", "AUDIENCE"
    val seatIndex: Int? = null,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis()
)

// ✋ SPEAKER REQUEST - Stored in Firestore `rooms/{roomId}/speakerRequests/{uid}`
data class SpeakerRequest(
    val id: String = "",
    val roomId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val userLevel: Int = 1,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val targetSeatIndex: Int = -1,
    val requestedAt: Long = System.currentTimeMillis()
)

// 📡 ROOM RTC SIGNAL - Stored in Firestore `rooms/{roomId}/signals/{signalId}`
data class RoomRtcSignal(
    val id: String = "",
    val roomId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val type: String = "OFFER", // "OFFER", "ANSWER", "ICE_CANDIDATE"
    val sdp: String = "",
    val iceCandidateMid: String? = null,
    val iceCandidateIndex: Int? = null,
    val iceCandidateSdp: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// 💬 CHAT MESSAGE - Added rich message types, replies, VIP chat bubbles, and VIP/Level badges
data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val text: String = "",
    val type: MessageType = MessageType.TEXT, // TEXT, GIFT, SYSTEM, ENTRY
    val chatBubbleUrl: String? = null,        // Custom background for VIP chat bubbles
    val vipLevel: Int = 0,
    val svipLevel: Int = 0,
    val userLevel: Int = 1,
    val replyToMessageId: String? = null,     // Threading support
    val isSystem: Boolean = false,
    val isSystemMessage: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    TEXT, GIFT, SYSTEM, ENTRY, ADMIN_WARNING
}

// 🎁 GIFT ITEM - Added SVGA/Lottie animation support and full-screen triggers
data class GiftItem(
    val id: String,
    val name: String,
    val emoji: String,
    val costCoins: Long,
    val category: String = "Classic",
    val description: String = "",
    val animationUrl: String = "",      // URL to .svga or .json (Lottie) for high-end rendering
    val soundUrl: String = "",          // Optional sound effect for the gift
    val isFullScreenEffect: Boolean = false // True for rockets/castles that take over the screen
)

val AVAILABLE_GIFTS = listOf(
    GiftItem("gift_rose", "Rose", "🌹", 10, "Romantic", "A gentle fragrant red rose"),
    GiftItem("gift_heart", "Heart", "💖", 50, "Romantic", "Full of romantic love"),
    GiftItem("gift_coffee", "Warm Coffee", "☕", 100, "Support", "Warm drink for the speaker"),
    GiftItem("gift_car", "Sports Car", "🏎️", 500, "Luxury", "Speed and style into the room", "urls/car.svga", isFullScreenEffect = true),
    GiftItem("gift_rocket", "Space Rocket", "🚀", 1000, "Luxury", "Launches into orbit with sparks", "urls/rocket.svga", isFullScreenEffect = true),
    GiftItem("gift_yacht", "Mega Yacht", "🛳️", 3000, "VIP", "Cruising on party waves", "urls/yacht.svga", isFullScreenEffect = true),
    GiftItem("gift_crown", "Royal Crown", "👑", 5000, "VIP", "Crown fit for royalty", "urls/crown.svga", isFullScreenEffect = true),
    GiftItem("gift_castle", "Crystal Castle", "🏰", 10000, "Super VIP", "Grand supreme gift of honor", "urls/castle.svga", isFullScreenEffect = true)
)

// 💸 GIFT TRANSACTION - Added combo multipliers and network broadcast flags
data class GiftTransaction(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val giftId: String = "",
    val giftName: String = "",
    val giftEmoji: String = "",
    val coins: Long = 0L,
    val comboCount: Int = 1,            // E.g., "Sent Rose x10!"
    val isBroadcastEvent: Boolean = false, // Triggers app-wide marquee banner for huge gifts
    val timestamp: Long = System.currentTimeMillis()
)

// 👤 USER PROFILE - Added agencies, entry animations, permanent public ID, frames, and privacy controls
data class FirebaseUserProfile(
    val userId: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String? = null,
    val avatar: String = "",
    val coverImage: String = "",
    val profileFrameId: String = "frame_default",
    val bio: String = "Voice Room VIP Member 🎙️✨",
    val country: String = "Global",
    val gender: String = "Not specified",
    val accountStatus: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    
    // Core Progression
    val level: Int = 1,
    val vipLevel: Int = 0,
    val svipLevel: Int = 0,
    val experiencePoints: Long = 0L,
    
    // Economy
    val coinBalance: Long = 2500L,       // Currency for spending
    val diamondBalance: Long = 0L,       // Currency earned from receiving gifts (cash-out currency)
    val receivedDiamonds: Long = 0L,
    val giftsReceivedCount: Int = 0,
    
    // Social & Agency
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val agencyId: String? = null,        // Link to host management agency
    val badges: List<String> = emptyList(), // e.g., ["TopGifter", "Singer"]
    
    // VIP Visuals
    val avatarFrameUrl: String = "",
    val roomEntryEffectUrl: String = "", // Animation played when user enters a room
    
    // Status & Moderation
    val role: String = "USER", // USER, MODERATOR, SENIOR_MODERATOR, ADMIN, SUPER_ADMIN
    val isPrivateAccount: Boolean = false,
    val moderationStatus: String = "NORMAL", // NORMAL, WARNED, RESTRICTED
    val restrictedUntil: Long? = null,
    val suspendedUntil: Long? = null,
    val isMuted: Boolean = false,
    val mutedUntil: Long? = null,
    val isOnline: Boolean = false,
    val invisibleMode: Boolean = false,  // VIP feature to hide online/entry status
    val callPrivacy: String = "Everyone", // "Everyone", "Friends", "Nobody"
    val lastActive: Long = System.currentTimeMillis()
) {
    val uid: String get() = userId
    val earnings: Long get() = if (diamondBalance > 0) diamondBalance else receivedDiamonds

    val isSuspended: Boolean get() {
        if (accountStatus == "SUSPENDED" || accountStatus == "BANNED") {
            val until = suspendedUntil
            return until == null || until > System.currentTimeMillis()
        }
        return false
    }

    val isRestricted: Boolean get() {
        if (moderationStatus == "RESTRICTED") {
            val until = restrictedUntil
            return until == null || until > System.currentTimeMillis()
        }
        return false
    }

    val isCurrentlyMuted: Boolean get() {
        if (isMuted) {
            val until = mutedUntil
            return until == null || until > System.currentTimeMillis()
        }
        return false
    }
}

// 👑 PROFILE FRAME - Stored in Firestore collection `frames`
data class ProfileFrame(
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val type: String = "VIP",         // "VIP", "EVENT", "LUXURY", "DEFAULT"
    val rarity: String = "Common",    // "Common", "Rare", "Epic", "Legendary"
    val glowColorHex: Long = 0xFFFFD700,
    val secondaryColorHex: Long = 0xFFFFA000,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// 🚩 USER REPORT - Stored in Firestore collection `reports`
data class UserReport(
    val id: String = "",
    val reporterUid: String = "",
    val reportedUid: String = "",
    val reportedPublicId: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// 💳 WALLET TRANSACTION - Added status, reference mapping, and balances
data class CoinTransactionItem(
    val id: String = "",
    val userId: String = "",
    val referenceId: String = "",        // Links back to GiftTransaction.id or PaymentIntent ID
    val type: String = "RECHARGE",
    val amount: Long = 0L,
    val balanceAfter: Long = 0L,         // Crucial for auditing missing funds
    val title: String = "",
    val description: String = "",
    val status: String = "COMPLETED",
    val timestamp: Long = System.currentTimeMillis()
)
