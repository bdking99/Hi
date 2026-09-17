package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email OR phone = :email LIMIT 1")
    suspend fun getUserByEmailOrPhone(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE userId = :userId")
    fun getProfileFlow(userId: String): Flow<ProfileEntity?>

    @Transaction
    suspend fun registerUser(user: UserEntity, profile: ProfileEntity) {
        insertUser(user)
        insertProfile(profile)
    }
}
