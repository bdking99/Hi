package com.example.data.repository

import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ProfileEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.ProfileFrame
import com.example.data.model.UserReport
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {
    companion object {
        const val USERS_COLLECTION = "users"
        const val FRAMES_COLLECTION = "frames"
        const val REPORTS_COLLECTION = "reports"
        const val FOLLOWS_COLLECTION = "follows"
        const val PUBLIC_IDS_COLLECTION = "public_ids"
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Room local persistence flows
    fun getUserFlow(userId: String): Flow<UserEntity?> = userDao.getUserFlow(userId)
    fun getProfileFlow(userId: String): Flow<ProfileEntity?> = userDao.getProfileFlow(userId)
    
    suspend fun getUser(userId: String): UserEntity? = userDao.getUser(userId)

    suspend fun addCoins(userId: String, deltaCoins: Long) = withContext(Dispatchers.IO) {
        userDao.addCoins(userId, deltaCoins)
        try {
            val firestore = getFirestore() ?: return@withContext
            firestore.collection(USERS_COLLECTION).document(userId).update(
                "coinBalance", FieldValue.increment(deltaCoins)
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateBalances(userId: String, coins: Long, earnings: Long) = withContext(Dispatchers.IO) {
        userDao.updateBalances(userId, coins, earnings)
        try {
            val firestore = getFirestore() ?: return@withContext
            firestore.collection(USERS_COLLECTION).document(userId).update(
                mapOf(
                    "coinBalance" to coins,
                    "diamondBalance" to earnings
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        bio: String,
        avatar: String?,
        coverImage: String?,
        country: String? = null,
        gender: String? = null
    ) = withContext(Dispatchers.IO) {
        userDao.updateUserInfo(userId, displayName, avatar, coverImage)
        userDao.updateBio(userId, bio)

        try {
            val firestore = getFirestore() ?: return@withContext
            val updates = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "bio" to bio,
                "lastActive" to System.currentTimeMillis()
            )
            if (avatar != null) updates["avatar"] = avatar
            if (coverImage != null) updates["coverImage"] = coverImage
            if (country != null) updates["country"] = country
            if (gender != null) updates["gender"] = gender

            firestore.collection(USERS_COLLECTION).document(userId).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Real-time stream to fetch genuine public profile data for any user from Firestore.
     * Yields null if the user does not exist in Firestore.
     */
    fun getPublicProfileStream(userId: String): Flow<FirebaseUserProfile?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            val docRef = firestore.collection(USERS_COLLECTION).document(userId)
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(FirebaseUserProfile::class.java)
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
            close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Firestore query: One-shot fetch of genuine public profile data by user ID.
     */
    suspend fun getPublicProfile(userId: String): FirebaseUserProfile? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        try {
            val snapshot = firestore.collection(USERS_COLLECTION).document(userId).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(FirebaseUserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Updates real-time presence (Online / Offline + timestamp) in Firestore.
     */
    suspend fun updatePresence(userId: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext
            firestore.collection(USERS_COLLECTION).document(userId).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastActive" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Equips a profile frame in Firestore.
     */
    suspend fun updateProfileFrame(userId: String, frameId: String) = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext
            firestore.collection(USERS_COLLECTION).document(userId).update(
                "profileFrameId", frameId
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Real User Search in Firestore.
     * Searches by numeric Public User ID, username, or display name prefix.
     */
    suspend fun searchUsers(query: String): List<FirebaseUserProfile> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext emptyList()
        val cleanedQuery = query.trim()
        if (cleanedQuery.isBlank()) return@withContext emptyList()

        val results = mutableMapOf<String, FirebaseUserProfile>()

        try {
            // 1. Search by exact or prefix Public User ID
            val publicIdQuery = firestore.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("publicUserId", cleanedQuery)
                .whereLessThanOrEqualTo("publicUserId", cleanedQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()

            for (doc in publicIdQuery.documents) {
                val profile = doc.toObject(FirebaseUserProfile::class.java)
                if (profile != null && profile.userId.isNotBlank()) {
                    results[profile.userId] = profile
                }
            }

            // 2. Search by username prefix
            val usernameQuery = firestore.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("username", cleanedQuery.lowercase())
                .whereLessThanOrEqualTo("username", cleanedQuery.lowercase() + "\uf8ff")
                .limit(20)
                .get()
                .await()

            for (doc in usernameQuery.documents) {
                val profile = doc.toObject(FirebaseUserProfile::class.java)
                if (profile != null && profile.userId.isNotBlank()) {
                    results[profile.userId] = profile
                }
            }

            // 3. Search by displayName prefix
            val nameQuery = firestore.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("displayName", cleanedQuery)
                .whereLessThanOrEqualTo("displayName", cleanedQuery + "\uf8ff")
                .limit(20)
                .get()
                .await()

            for (doc in nameQuery.documents) {
                val profile = doc.toObject(FirebaseUserProfile::class.java)
                if (profile != null && profile.userId.isNotBlank()) {
                    results[profile.userId] = profile
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results.values.toList()
    }

    /**
     * Real-time stream of available Profile Frames from Firestore collection `frames`.
     * Seeds initial VIP frames if empty.
     */
    fun getAvailableFramesStream(): Flow<List<ProfileFrame>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultFrames())
            close()
            return@callbackFlow
        }

        try {
            val collectionRef = firestore.collection(FRAMES_COLLECTION)
                .orderBy("sortOrder", Query.Direction.ASCENDING)

            val listener = collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    // Seed initial frames into Firestore if empty
                    seedDefaultFramesIntoFirestore(firestore)
                    trySend(getDefaultFrames())
                    return@addSnapshotListener
                }

                val frames = snapshot.toObjects(ProfileFrame::class.java)
                trySend(frames)
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getDefaultFrames())
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun seedDefaultFramesIntoFirestore(firestore: FirebaseFirestore) {
        val defaultFrames = getDefaultFrames()
        for (frame in defaultFrames) {
            firestore.collection(FRAMES_COLLECTION).document(frame.id).set(frame, SetOptions.merge())
        }
    }

    fun getDefaultFrames(): List<ProfileFrame> {
        return listOf(
            ProfileFrame(
                id = "frame_gold_crown",
                name = "Imperial Gold Crown",
                imageUrl = "",
                type = "VIP",
                rarity = "Legendary",
                glowColorHex = 0xFFFFD700,
                secondaryColorHex = 0xFFFFA000,
                sortOrder = 1
            ),
            ProfileFrame(
                id = "frame_neon_cyber",
                name = "Cyber Neon Aura",
                imageUrl = "",
                type = "EVENT",
                rarity = "Epic",
                glowColorHex = 0xFF00E5FF,
                secondaryColorHex = 0xFF9D00FF,
                sortOrder = 2
            ),
            ProfileFrame(
                id = "frame_royal_ruby",
                name = "Royal Ruby Sovereign",
                imageUrl = "",
                type = "LUXURY",
                rarity = "Legendary",
                glowColorHex = 0xFFFF1744,
                secondaryColorHex = 0xFFFF80AB,
                sortOrder = 3
            ),
            ProfileFrame(
                id = "frame_dragon_fire",
                name = "Blazing Dragon Flame",
                imageUrl = "",
                type = "EVENT",
                rarity = "Epic",
                glowColorHex = 0xFFFF6D00,
                secondaryColorHex = 0xFFFFD600,
                sortOrder = 4
            ),
            ProfileFrame(
                id = "frame_emerald_king",
                name = "Emerald Sovereign Halo",
                imageUrl = "",
                type = "VIP",
                rarity = "Rare",
                glowColorHex = 0xFF00E676,
                secondaryColorHex = 0xFF1DE9B6,
                sortOrder = 5
            ),
            ProfileFrame(
                id = "frame_celestial_star",
                name = "Celestial Crystal Star",
                imageUrl = "",
                type = "LUXURY",
                rarity = "Legendary",
                glowColorHex = 0xFFE040FB,
                secondaryColorHex = 0xFF7C4DFF,
                sortOrder = 6
            )
        )
    }

    /**
     * Submit user report to Firestore `reports` collection
     */
    suspend fun reportUser(report: UserReport): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firestore unavailable"))
            val docId = if (report.id.isNotBlank()) report.id else firestore.collection(REPORTS_COLLECTION).document().id
            firestore.collection(REPORTS_COLLECTION).document(docId).set(report.copy(id = docId)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Follow/Unfollow user relationship in Firestore
     */
    suspend fun toggleFollow(currentUserId: String, targetUserId: String, follow: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firestore unavailable"))
            val followDocId = "${currentUserId}_${targetUserId}"
            val followRef = firestore.collection(FOLLOWS_COLLECTION).document(followDocId)

            if (follow) {
                followRef.set(
                    mapOf(
                        "followerUid" to currentUserId,
                        "followedUid" to targetUserId,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()
                // Update stats
                firestore.collection(USERS_COLLECTION).document(currentUserId)
                    .update("followingCount", FieldValue.increment(1)).await()
                firestore.collection(USERS_COLLECTION).document(targetUserId)
                    .update("followersCount", FieldValue.increment(1)).await()
            } else {
                followRef.delete().await()
                firestore.collection(USERS_COLLECTION).document(currentUserId)
                    .update("followingCount", FieldValue.increment(-1)).await()
                firestore.collection(USERS_COLLECTION).document(targetUserId)
                    .update("followersCount", FieldValue.increment(-1)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * One-shot fetch for user profile.
     */
    suspend fun getUserProfile(userId: String): FirebaseUserProfile? = getPublicProfile(userId)

    /**
     * Check if current user is following target user (one-shot).
     */
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext false
            val followDocId = "${currentUserId}_${targetUserId}"
            val doc = firestore.collection(FOLLOWS_COLLECTION).document(followDocId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if current user is following target user
     */
    fun isFollowingStream(currentUserId: String, targetUserId: String): Flow<Boolean> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || currentUserId.isBlank() || targetUserId.isBlank()) {
            trySend(false)
            close()
            return@callbackFlow
        }

        try {
            val followDocId = "${currentUserId}_${targetUserId}"
            val listener = firestore.collection(FOLLOWS_COLLECTION).document(followDocId)
                .addSnapshotListener { snapshot, _ ->
                    trySend(snapshot != null && snapshot.exists())
                }
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(false)
            close()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Block a user. Cleans up bidirectional follows in both collections.
     */
    suspend fun blockUser(currentUserId: String, targetUserId: String, reason: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firestore unavailable"))
            if (currentUserId.isBlank() || targetUserId.isBlank() || currentUserId == targetUserId) {
                return@withContext Result.failure(IllegalArgumentException("Invalid user IDs for blocking"))
            }

            val targetDoc = firestore.collection(USERS_COLLECTION).document(targetUserId).get().await()
            val targetProfile = targetDoc.toObject(FirebaseUserProfile::class.java)

            val blockRecord = mapOf(
                "targetUid" to targetUserId,
                "targetPublicUserId" to (targetProfile?.publicUserId ?: ""),
                "targetDisplayName" to (targetProfile?.displayName ?: ""),
                "targetAvatar" to (targetProfile?.avatar ?: ""),
                "blockedAt" to System.currentTimeMillis(),
                "reason" to reason
            )

            val batch = firestore.batch()
            val blockRef = firestore.collection(USERS_COLLECTION).document(currentUserId).collection("blocked").document(targetUserId)
            batch.set(blockRef, blockRecord)

            val followDoc1 = firestore.collection(FOLLOWS_COLLECTION).document("${currentUserId}_${targetUserId}")
            val followDoc2 = firestore.collection(FOLLOWS_COLLECTION).document("${targetUserId}_${currentUserId}")
            val followingRef1 = firestore.collection(USERS_COLLECTION).document(currentUserId).collection("following").document(targetUserId)
            val followerRef1 = firestore.collection(USERS_COLLECTION).document(targetUserId).collection("followers").document(currentUserId)
            val followingRef2 = firestore.collection(USERS_COLLECTION).document(targetUserId).collection("following").document(currentUserId)
            val followerRef2 = firestore.collection(USERS_COLLECTION).document(currentUserId).collection("followers").document(targetUserId)

            batch.delete(followDoc1)
            batch.delete(followDoc2)
            batch.delete(followingRef1)
            batch.delete(followerRef1)
            batch.delete(followingRef2)
            batch.delete(followerRef2)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Unblock a user.
     */
    suspend fun unblockUser(currentUserId: String, targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firestore unavailable"))
            firestore.collection(USERS_COLLECTION).document(currentUserId).collection("blocked").document(targetUserId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if either user has blocked the other.
     */
    suspend fun isBlocked(currentUserId: String, targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext false
            val aBlockedB = firestore.collection(USERS_COLLECTION).document(currentUserId).collection("blocked").document(targetUserId).get().await()
            if (aBlockedB.exists()) return@withContext true
            val bBlockedA = firestore.collection(USERS_COLLECTION).document(targetUserId).collection("blocked").document(currentUserId).get().await()
            bBlockedA.exists()
        } catch (e: Exception) {
            false
        }
    }
}

