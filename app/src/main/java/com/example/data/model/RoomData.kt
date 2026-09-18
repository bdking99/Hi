package com.example.data.model

data class RoomData(
    val id: String = "",
    val title: String = "",
    val description: String = "Welcome to our voice room!",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatar: String = "",
    val agencyName: String = "Voice Club",
    val hostLevel: Int = 1,
    val viewerCount: String = "1",
    val countryFlag: String = "🇺🇸",
    val tag: String = "Chat",
    val bgGradientStart: String = "#2D124D",
    val bgGradientEnd: String = "#1B1035",
    val seats: List<SeatData> = emptyList(),
    val activeUserIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
