package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    val dataPayload: String?,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
