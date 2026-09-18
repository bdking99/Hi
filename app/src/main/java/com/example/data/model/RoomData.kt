package com.example.data.model

data class RoomData(
    // 🆔 Core Identification
    val id: String = "",
    val title: String = "",
    val description: String = "Welcome to our voice room!",
    
    // 👤 Host Details
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatar: String = "",
    val agencyName: String = "Voice Club",
    val hostLevel: Int = 1,
    
    // 👑 Premium & Monetization Features
    val isPremiumRoom: Boolean = false,
    val entryFee: Int = 0,          // Coins/Diamonds required to enter
    val minLevelToJoin: Int = 0,    // Level restriction for exclusive rooms
    val totalEarnings: Long = 0L,   // Track gifts/currency dropped in the room
    
    // 🔒 State & Access Control
    val roomType: RoomType = RoomType.PUBLIC,
    val password: String = "",      // If roomType is PRIVATE
    val isLocked: Boolean = false,  // Temporarily lock the room
    val isAllMuted: Boolean = false,// Admin control to mute all seats
    
    // 🎨 Premium Customization & UI
    val countryFlag: String = "🇺🇸",
    val tag: String = "Chat",
    val bgGradientStart: String = "#2D124D",
    val bgGradientEnd: String = "#1B1035",
    val customBackgroundUrl: String = "", // VIP users can set image backgrounds
    val roomFrameUrl: String = "",        // VIP animated border around the room
    
    // 📊 Engagement & Tracking
    val viewerCount: Int = 1,       // Changed to Int for math/sorting
    val seats: List<SeatData> = emptyList(),
    val activeUserIds: List<String> = emptyList(),
    
    // 🕒 Metadata
    val createdAt: Long = System.currentTimeMillis()
)

// Define room access levels
enum class RoomType {
    PUBLIC,     // Anyone can join
    PRIVATE,    // Requires password or invite
    VIP         // Requires VIP subscription or entry fee
}
