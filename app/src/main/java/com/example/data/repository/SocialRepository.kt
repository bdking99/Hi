package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class SocialRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    companion object {
        private const val TAG = "SocialRepository"
        const val USERS_COLLECTION = "users"
        const val FOLLOWING_SUBCOLLECTION = "following"
        const val FOLLOWERS_SUBCOLLECTION = "followers"
        const val BLOCKED_SUBCOLLECTION = "blocked"
        const val FOLLOW_REQUESTS_COLLECTION = "followRequests"
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    // ==========================================
    // FOLLOW / UNFOLLOW SYSTEM
    // ==========================================

    /**
     * Follow or unfollow a target user idempotently.
     * If target user is private, sends a follow request instead.
     */
    suspend fun toggleFollow(
        currentUid: String,
        targetUser: FirebaseUserProfile
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (currentUid.isBlank() || targetUser.userId.isBlank() || currentUid == targetUser.userId) {
                return@withContext Result.failure(IllegalArgumentException("Invalid user IDs for follow"))
            }

            // Check if blocked in either direction
            if (isBlocked(currentUid, targetUser.userId)) {
                return@withContext Result.failure(IllegalStateException("Cannot follow a blocked user"))
            }

            val followingDocRef = firestore.collection(USERS_COLLECTION)
                .document(currentUid)
                .collection(FOLLOWING_SUBCOLLECTION)
                .document(targetUser.userId)

            val followingDoc = followingDocRef.get().await()
            val isCurrentlyFollowing = followingDoc.exists()

            if (isCurrentlyFollowing) {
                // UNFOLLOW
                val followerDocRef = firestore.collection(USERS_COLLECTION)
                    .document(targetUser.userId)
                    .collection(FOLLOWERS_SUBCOLLECTION)
                    .document(currentUid)

                val batch = firestore.batch()
                batch.delete(followingDocRef)
                batch.delete(followerDocRef)
                batch.update(firestore.collection(USERS_COLLECTION).document(currentUid), "followingCount", FieldValue.increment(-1))
                batch.update(firestore.collection(USERS_COLLECTION).document(targetUser.userId), "followersCount", FieldValue.increment(-1))
                batch.commit().await()

                return@withContext Result.success("UNFOLLOWED")
            } else {
                // Check if target account is private
                if (targetUser.isPrivateAccount) {
                    val requestResult = sendFollowRequest(currentUid, targetUser)
                    return@withContext requestResult.map { "REQUESTED" }
                }

                // Normal FOLLOW
                val currentUserDoc = firestore.collection(USERS_COLLECTION).document(currentUid).get().await()
                val currentProfile = currentUserDoc.toObject(FirebaseUserProfile::class.java)

                val followingRecord = FollowingRecord(
                    uid = currentUid,
                    targetUid = targetUser.userId,
                    targetPublicUserId = targetUser.publicUserId,
                    targetDisplayName = targetUser.displayName,
                    targetAvatar = targetUser.avatar,
                    createdAt = System.currentTimeMillis()
                )

                val followerRecord = FollowerRecord(
                    uid = targetUser.userId,
                    followerUid = currentUid,
                    followerPublicUserId = currentProfile?.publicUserId ?: "",
                    followerDisplayName = currentProfile?.displayName ?: "User",
                    followerAvatar = currentProfile?.avatar ?: "",
                    createdAt = System.currentTimeMillis()
                )

                val followerDocRef = firestore.collection(USERS_COLLECTION)
                    .document(targetUser.userId)
                    .collection(FOLLOWERS_SUBCOLLECTION)
                    .document(currentUid)

                val batch = firestore.batch()
                batch.set(followingDocRef, followingRecord)
                batch.set(followerDocRef, followerRecord)
                batch.update(firestore.collection(USERS_COLLECTION).document(currentUid), "followingCount", FieldValue.increment(1))
                batch.update(firestore.collection(USERS_COLLECTION).document(targetUser.userId), "followersCount", FieldValue.increment(1))
                batch.commit().await()

                // Trigger real notification
                notificationRepository.sendNotification(
                    AppNotification(
                        recipientUid = targetUser.userId,
                        senderUid = currentUid,
                        senderPublicUserId = currentProfile?.publicUserId ?: "",
                        senderDisplayName = currentProfile?.displayName ?: "Someone",
                        senderAvatarUrl = currentProfile?.avatar ?: "",
                        type = NotificationTypes.FOLLOW,
                        title = "New Follower",
                        body = "${currentProfile?.displayName ?: "A user"} started following you.",
                        referenceId = currentUid,
                        referenceType = "USER"
                    )
                )

                return@withContext Result.success("FOLLOWING")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in toggleFollow: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream to check if current user is following target user.
     */
    fun isFollowingStream(currentUid: String, targetUid: String): Flow<Boolean> = callbackFlow {
        if (currentUid.isBlank() || targetUid.isBlank() || auth.currentUser == null) {
            trySend(false)
            awaitClose {}
            return@callbackFlow
        }

        val docRef = firestore.collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(FOLLOWING_SUBCOLLECTION)
            .document(targetUid)

        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(false)
                return@addSnapshotListener
            }
            trySend(snapshot != null && snapshot.exists())
        }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of users the current user is following.
     */
    fun getFollowingStream(uid: String): Flow<List<FollowingRecord>> = callbackFlow {
        if (uid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(FOLLOWING_SUBCOLLECTION)
            .limit(100)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FollowingRecord::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    /**
     * Real-time stream of users following the specified user.
     */
    fun getFollowersStream(uid: String): Flow<List<FollowerRecord>> = callbackFlow {
        if (uid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .collection(FOLLOWERS_SUBCOLLECTION)
            .limit(100)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FollowerRecord::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    // ==========================================
    // FOLLOW REQUESTS (PRIVATE PROFILES)
    // ==========================================

    private suspend fun sendFollowRequest(currentUid: String, targetUser: FirebaseUserProfile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestId = "${currentUid}_${targetUser.userId}"
            val existing = firestore.collection(FOLLOW_REQUESTS_COLLECTION).document(requestId).get().await()
            if (existing.exists() && existing.getString("status") == "PENDING") {
                return@withContext Result.success(Unit) // Already pending
            }

            val currentUserDoc = firestore.collection(USERS_COLLECTION).document(currentUid).get().await()
            val currentProfile = currentUserDoc.toObject(FirebaseUserProfile::class.java)

            val request = FollowRequest(
                requestId = requestId,
                senderUid = currentUid,
                receiverUid = targetUser.userId,
                senderPublicUserId = currentProfile?.publicUserId ?: "",
                receiverPublicUserId = targetUser.publicUserId,
                senderDisplayName = currentProfile?.displayName ?: "User",
                senderAvatarUrl = currentProfile?.avatar ?: "",
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(FOLLOW_REQUESTS_COLLECTION).document(requestId).set(request).await()

            // Send notification to receiver
            notificationRepository.sendNotification(
                AppNotification(
                    recipientUid = targetUser.userId,
                    senderUid = currentUid,
                    senderPublicUserId = currentProfile?.publicUserId ?: "",
                    senderDisplayName = currentProfile?.displayName ?: "User",
                    senderAvatarUrl = currentProfile?.avatar ?: "",
                    type = NotificationTypes.FOLLOW_REQUEST,
                    title = "Follow Request",
                    body = "${currentProfile?.displayName ?: "A user"} requested to follow you.",
                    referenceId = requestId,
                    referenceType = "USER"
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPendingFollowRequestsStream(receiverUid: String): Flow<List<FollowRequest>> = callbackFlow {
        if (receiverUid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(FOLLOW_REQUESTS_COLLECTION)
            .whereEqualTo("receiverUid", receiverUid)
            .whereEqualTo("status", "PENDING")
            .limit(50)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(FollowRequest::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.createdAt })
        }

        awaitClose { registration.remove() }
    }

    suspend fun respondToFollowRequest(requestId: String, accept: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(FOLLOW_REQUESTS_COLLECTION).document(requestId)
            val doc = docRef.get().await()
            if (!doc.exists()) return@withContext Result.failure(Exception("Request not found"))

            val request = doc.toObject(FollowRequest::class.java) ?: return@withContext Result.failure(Exception("Invalid request"))

            if (accept) {
                docRef.update(mapOf("status" to "ACCEPTED", "updatedAt" to System.currentTimeMillis())).await()

                // Establish mutual follow
                val senderDoc = firestore.collection(USERS_COLLECTION).document(request.senderUid).get().await()
                val receiverDoc = firestore.collection(USERS_COLLECTION).document(request.receiverUid).get().await()
                val senderProfile = senderDoc.toObject(FirebaseUserProfile::class.java)
                val receiverProfile = receiverDoc.toObject(FirebaseUserProfile::class.java)

                val followingRecord = FollowingRecord(
                    uid = request.senderUid,
                    targetUid = request.receiverUid,
                    targetPublicUserId = request.receiverPublicUserId,
                    targetDisplayName = receiverProfile?.displayName ?: "",
                    targetAvatar = receiverProfile?.avatar ?: "",
                    createdAt = System.currentTimeMillis()
                )

                val followerRecord = FollowerRecord(
                    uid = request.receiverUid,
                    followerUid = request.senderUid,
                    followerPublicUserId = request.senderPublicUserId,
                    followerDisplayName = senderProfile?.displayName ?: "",
                    followerAvatar = senderProfile?.avatar ?: "",
                    createdAt = System.currentTimeMillis()
                )

                val batch = firestore.batch()
                batch.set(firestore.collection(USERS_COLLECTION).document(request.senderUid).collection(FOLLOWING_SUBCOLLECTION).document(request.receiverUid), followingRecord)
                batch.set(firestore.collection(USERS_COLLECTION).document(request.receiverUid).collection(FOLLOWERS_SUBCOLLECTION).document(request.senderUid), followerRecord)
                batch.update(firestore.collection(USERS_COLLECTION).document(request.senderUid), "followingCount", FieldValue.increment(1))
                batch.update(firestore.collection(USERS_COLLECTION).document(request.receiverUid), "followersCount", FieldValue.increment(1))
                batch.commit().await()

                // Send accepted notification
                notificationRepository.sendNotification(
                    AppNotification(
                        recipientUid = request.senderUid,
                        senderUid = request.receiverUid,
                        senderPublicUserId = receiverProfile?.publicUserId ?: "",
                        senderDisplayName = receiverProfile?.displayName ?: "User",
                        senderAvatarUrl = receiverProfile?.avatar ?: "",
                        type = NotificationTypes.FOLLOW_ACCEPTED,
                        title = "Follow Request Accepted",
                        body = "${receiverProfile?.displayName ?: "User"} accepted your follow request.",
                        referenceId = request.receiverUid,
                        referenceType = "USER"
                    )
                )
            } else {
                docRef.update(mapOf("status" to "REJECTED", "updatedAt" to System.currentTimeMillis())).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // BLOCK / UNBLOCK SYSTEM
    // ==========================================

    /**
     * Check if either user has blocked the other.
     */
    suspend fun isBlocked(uidA: String, uidB: String): Boolean = withContext(Dispatchers.IO) {
        if (uidA.isBlank() || uidB.isBlank() || uidA == uidB) return@withContext false
        try {
            val aBlockedB = firestore.collection(USERS_COLLECTION)
                .document(uidA)
                .collection(BLOCKED_SUBCOLLECTION)
                .document(uidB)
                .get()
                .await()
            if (aBlockedB.exists()) return@withContext true

            val bBlockedA = firestore.collection(USERS_COLLECTION)
                .document(uidB)
                .collection(BLOCKED_SUBCOLLECTION)
                .document(uidA)
                .get()
                .await()
            return@withContext bBlockedA.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Block another user. Automatically breaks follows in both directions and cancels pending follow requests.
     */
    suspend fun blockUser(
        currentUid: String,
        targetUser: FirebaseUserProfile,
        reason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (currentUid.isBlank() || targetUser.userId.isBlank() || currentUid == targetUser.userId) {
                return@withContext Result.failure(IllegalArgumentException("Invalid user IDs for blocking"))
            }

            val blockRecord = BlockedUserRecord(
                targetUid = targetUser.userId,
                targetPublicUserId = targetUser.publicUserId,
                targetDisplayName = targetUser.displayName,
                targetAvatar = targetUser.avatar,
                blockedAt = System.currentTimeMillis(),
                reason = reason
            )

            val batch = firestore.batch()

            // 1. Add to blocked collection
            val blockRef = firestore.collection(USERS_COLLECTION)
                .document(currentUid)
                .collection(BLOCKED_SUBCOLLECTION)
                .document(targetUser.userId)
            batch.set(blockRef, blockRecord)

            // 2. Remove follow A -> B
            val followingRefA = firestore.collection(USERS_COLLECTION).document(currentUid).collection(FOLLOWING_SUBCOLLECTION).document(targetUser.userId)
            val followerRefB = firestore.collection(USERS_COLLECTION).document(targetUser.userId).collection(FOLLOWERS_SUBCOLLECTION).document(currentUid)
            batch.delete(followingRefA)
            batch.delete(followerRefB)

            // 3. Remove follow B -> A
            val followingRefB = firestore.collection(USERS_COLLECTION).document(targetUser.userId).collection(FOLLOWING_SUBCOLLECTION).document(currentUid)
            val followerRefA = firestore.collection(USERS_COLLECTION).document(currentUid).collection(FOLLOWERS_SUBCOLLECTION).document(targetUser.userId)
            batch.delete(followingRefB)
            batch.delete(followerRefA)

            // 4. Cancel follow requests
            val req1 = firestore.collection(FOLLOW_REQUESTS_COLLECTION).document("${currentUid}_${targetUser.userId}")
            val req2 = firestore.collection(FOLLOW_REQUESTS_COLLECTION).document("${targetUser.userId}_${currentUid}")
            batch.delete(req1)
            batch.delete(req2)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unblock a previously blocked user.
     */
    suspend fun unblockUser(currentUid: String, targetUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (currentUid.isBlank() || targetUid.isBlank()) return@withContext Result.failure(IllegalArgumentException("Invalid user IDs"))

            firestore.collection(USERS_COLLECTION)
                .document(currentUid)
                .collection(BLOCKED_SUBCOLLECTION)
                .document(targetUid)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of blocked users for the current user.
     */
    fun getBlockedUsersStream(currentUid: String): Flow<List<BlockedUserRecord>> = callbackFlow {
        if (currentUid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(BLOCKED_SUBCOLLECTION)
            .limit(100)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(BlockedUserRecord::class.java) } ?: emptyList()
            trySend(list.sortedByDescending { it.blockedAt })
        }

        awaitClose { registration.remove() }
    }

    /**
     * Check if a specific target user is blocked by current user in real time.
     */
    fun isUserBlockedStream(currentUid: String, targetUid: String): Flow<Boolean> = callbackFlow {
        if (currentUid.isBlank() || targetUid.isBlank() || auth.currentUser == null) {
            trySend(false)
            awaitClose {}
            return@callbackFlow
        }

        val docRef = firestore.collection(USERS_COLLECTION)
            .document(currentUid)
            .collection(BLOCKED_SUBCOLLECTION)
            .document(targetUid)

        val registration = docRef.addSnapshotListener { snapshot, _ ->
            trySend(snapshot != null && snapshot.exists())
        }

        awaitClose { registration.remove() }
    }
}
