package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actorUserId: String,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val metadata: String?,
    val createdAt: Long = System.currentTimeMillis()
)
