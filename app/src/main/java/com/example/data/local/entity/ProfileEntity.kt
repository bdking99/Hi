package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val bio: String?,
    val level: Int = 1,
    val vipLevel: Int = 0,
    val svipLevel: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val agencyId: String?,
    val coinBalance: Long = 0,
    val earnings: Long = 0
)
