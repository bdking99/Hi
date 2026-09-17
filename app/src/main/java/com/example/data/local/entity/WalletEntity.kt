package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val userId: String,
    val coinBalance: Long = 0,
    val diamondBalance: Long = 0, // premium currency or earnings
    val updatedAt: Long = System.currentTimeMillis()
)
