package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_transactions")
data class CoinTransactionEntity(
    @PrimaryKey val id: String, // Transaction ID
    val userId: String,
    val transactionType: String, // RECHARGE, GIFT_SENT, GIFT_RECEIVED, SYSTEM_REWARD
    val amount: Long,
    val balanceBefore: Long,
    val balanceAfter: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // PENDING, SUCCESS, FAILED
    val referenceId: String? // e.g., gift id, or payment provider reference
)
