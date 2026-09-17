package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey val id: Int,
    val name: String, // USER, HOST, AGENCY, RESELLER, ADMIN
    val permissions: String // JSON array of permissions
)
