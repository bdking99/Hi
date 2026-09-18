package com.example.data.model

data class SeatData(
    val seatIndex: Int = 0,
    val userId: String? = null,
    val userName: String? = null,
    val userAvatar: String? = null,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val vipLevel: Int = 0
)

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val text: String = "",
    val isSystem: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class GiftItem(
    val id: String,
    val name: String,
    val emoji: String,
    val costCoins: Long,
    val category: String = "Classic",
    val description: String = ""
)

val AVAILABLE_GIFTS = listOf(
    GiftItem("gift_rose", "Rose", "🌹", 10, "Romantic", "A gentle fragrant red rose"),
    GiftItem("gift_heart", "Heart", "💖", 50, "Romantic", "Full of romantic love"),
    GiftItem("gift_coffee", "Warm Coffee", "☕", 100, "Support", "Warm drink for the speaker"),
    GiftItem("gift_car", "Sports Car", "🏎️", 500, "Luxury", "Speed and style into the room"),
    GiftItem("gift_rocket", "Space Rocket", "🚀", 1000, "Luxury", "Launches into orbit with sparks"),
    GiftItem("gift_yacht", "Mega Yacht", "🛳️", 3000, "VIP", "Cruising on party waves"),
    GiftItem("gift_crown", "Royal Crown", "👑", 5000, "VIP", "Crown fit for royalty"),
    GiftItem("gift_castle", "Crystal Castle", "🏰", 10000, "Super VIP", "Grand supreme gift of honor")
)

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
    val timestamp: Long = System.currentTimeMillis()
)

data class FirebaseUserProfile(
    val userId: String = "",
    val publicUserId: String = "",
    val displayName: String = "",
    val username: String = "",
    val avatar: String = "",
    val coverImage: String = "",
    val bio: String = "",
    val level: Int = 1,
    val vipLevel: Int = 0,
    val svipLevel: Int = 0,
    val coinBalance: Long = 2500L,
    val receivedDiamonds: Long = 0L,
    val giftsReceivedCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isOnline: Boolean = true,
    val lastActive: Long = System.currentTimeMillis()
)

data class CoinTransactionItem(
    val id: String = "",
    val userId: String = "",
    val type: String = "RECHARGE", // "RECHARGE", "GIFT_SENT", "GIFT_RECEIVED"
    val amount: Long = 0L,
    val title: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
