package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // UUID
    val publicUserId: String,
    val username: String,
    val displayName: String,
    val email: String?,
    val phone: String?,
    val passwordHash: String?,
    val avatar: String?,
    val coverImage: String?,
    val countryId: Int?,
    val status: String = "ACTIVE",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis(),
    val roleId: Int = 1 // Default USER
)
