package com.example.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirebaseRtdbUser(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val provider: String = "email" // "email", "phone", "google"
)
