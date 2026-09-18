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
    val isHost: Boolean = false         // Differentiate the main host from co-hosts/guests
)

// 💬 CHAT MESSAGE - Added rich message types, replies, and VIP chat bubbles
data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val text: String = "",
    val type: MessageType = MessageType.TEXT, // TEXT, GIFT, SYSTEM, ENTRY
    val chatBubbleUrl: String? = null,        // Custom background for VIP chat bubbles
    val replyToMessageId: String? = null,     // Threading support
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

// 👤 USER PROFILE - Added agencies, entry animations, and privacy controls
data class FirebaseUserProfile(
    val userId: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatar: String = "",
    val coverImage: String = "",
    val bio: String = "",
    
    // Core Progression
    val level: Int = 1,
    val vipLevel: Int = 0,
    val svipLevel: Int = 0,
    val experiencePoints: Long = 0L,
    
    // Economy
    val coinBalance: Long = 2500L,       // Currency for spending
    val diamondBalance: Long = 0L,       // Currency earned from receiving gifts (cash-out currency)
    val giftsReceivedCount: Int = 0,
    
    // Social & Agency
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val agencyId: String? = null,        // Link to host management agency
    val badges: List<String> = emptyList(), // e.g., ["TopGifter", "Singer"]
    
    // VIP Visuals
    val avatarFrameUrl: String = "",
    val roomEntryEffectUrl: String = "", // Animation played when user enters a room
    
    // Status
    val isOnline: Boolean = true,
    val invisibleMode: Boolean = false,  // VIP feature to hide online/entry status
    val lastActive: Long = System.currentTimeMillis()
)

// 💳 WALLET TRANSACTION - Added status, reference mapping, and balances
data class CoinTransactionItem(
    val id: String = "",
    val userId: String = "",
    val referenceId: String = "",        // Links back to GiftTransaction.id or PaymentIntent ID
    val type: TransactionType = TransactionType.RECHARGE, 
    val amount: Long = 0L,
    val balanceAfter: Long = 0L,         // Crucial for auditing missing funds
    val title: String = "",
    val description: String = "",
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TransactionType {
    RECHARGE, GIFT_SENT, GIFT_RECEIVED, WITHDRAWAL, SYSTEM_REWARD
}

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED, REFUNDED
}
