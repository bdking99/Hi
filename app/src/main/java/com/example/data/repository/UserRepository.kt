package com.example.data.repository

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    fun getUserFlow(userId: String): Flow<UserEntity?> = userDao.getUserFlow(userId)
    fun getProfileFlow(userId: String): Flow<ProfileEntity?> = userDao.getProfileFlow(userId)
    
    suspend fun getUser(userId: String): UserEntity? = userDao.getUser(userId)
}
