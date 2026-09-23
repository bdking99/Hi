package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository,
    private val socialRepository: SocialRepository? = null,
    private val notificationRepository: NotificationRepository? = null
) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    companion object {
        private const val TAG = "ChatRepository"
        private const val CONVERSATIONS_COLLECTION = "conversations"
        private const val MESSAGES_COLLECTION = "messages"
        private const val BLOCKS_COLLECTION = "blocks"
        private const val REPORTS_COLLECTION = "reports"

        /**
         * Deterministic conversation ID for two users.
         * The same two users will always yield the exact same ID regardless of who initiates.
         */
        fun getDeterministicConversationId(uidA: String, uidB: String): String {
            return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
        }
    }

    /**
     * Check if either user has blocked the other.
     */
    suspend fun isBlocked(uidA: String, uidB: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val blockAtoB = firestore.collection(BLOCKS_COLLECTION)
                .document("${uidA}_${uidB}")
                .get()
                .await()
            if (blockAtoB.exists()) return@withContext true

            val blockBtoA = firestore.collection(BLOCKS_COLLECTION)
                .document("${uidB}_${uidA}")
                .get()
                .await()
            return@withContext blockBtoA.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking block status: ${e.message}")
            false
        }
    }

    /**
     * Block a user.
     */
    suspend fun blockUser(blockerUid: String, blockedUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val record = UserBlockRecord(blockerUid = blockerUid, blockedUid = blockedUid)
            firestore.collection(BLOCKS_COLLECTION)
                .document("${blockerUid}_${blockedUid}")
                .set(record)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unblock a user.
     */
    suspend fun unblockUser(blockerUid: String, blockedUid: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(BLOCKS_COLLECTION)
                .document("${blockerUid}_${blockedUid}")
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unblocking user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create or fetch the conversation document.
     */
    suspend fun getOrCreateConversation(
        currentUid: String,
        otherUid: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conversationId = getDeterministicConversationId(currentUid, otherUid)
            val docRef = firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId)
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                val newConversation = hashMapOf(
                    "conversationId" to conversationId,
                    "participantIds" to listOf(currentUid, otherUid),
                    "createdAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "lastMessage" to "",
                    "lastMessageType" to "text",
                    "lastMessageSenderId" to "",
                    "unreadCounts" to mapOf(currentUid to 0L, otherUid to 0L)
                )
                docRef.set(newConversation).await()
            }
            Result.success(conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in getOrCreateConversation: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send a real-time message to Firestore.
     */
    suspend fun sendMessage(
        conversationId: String,
        senderUid: String,
        receiverUid: String,
        senderPublicUserId: String,
        text: String,
        messageType: String = "text",
        replyToMessageId: String? = null
    ): Result<DirectMessage> = withContext(Dispatchers.IO) {
        try {
            // Check block status (both subcollection and legacy)
            val blocked = socialRepository?.isBlocked(senderUid, receiverUid) ?: isBlocked(senderUid, receiverUid)
            if (blocked) {
                return@withContext Result.failure(IllegalStateException("You can't message this user."))
            }

            // Check if sender account is muted or suspended
            val senderProfile = userRepository.getUserProfile(senderUid)
            if (senderProfile?.isSuspended == true) {
                return@withContext Result.failure(IllegalStateException("Your account is currently suspended."))
            }
            if (senderProfile?.isCurrentlyMuted == true) {
                return@withContext Result.failure(IllegalStateException("Your messaging permission is temporarily muted."))
            }

            val messageId = "msg_${UUID.randomUUID()}"
            val now = System.currentTimeMillis()
            val message = DirectMessage(
                messageId = messageId,
                conversationId = conversationId,
                senderUid = senderUid,
                receiverUid = receiverUid,
                senderPublicUserId = senderPublicUserId,
                messageType = messageType,
                text = text,
                createdAt = now,
                isRead = false,
                readAt = null,
                replyToMessageId = replyToMessageId,
                deleted = false,
                status = "sent"
            )

            val convRef = firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId)
            val msgRef = convRef.collection(MESSAGES_COLLECTION).document(messageId)

            firestore.runBatch { batch ->
                // Write message
                batch.set(msgRef, message)

                // Update conversation metadata and increment receiver unread count
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageType" to messageType,
                        "lastMessageSenderId" to senderUid,
                        "updatedAt" to now,
                        "unreadCounts.$receiverUid" to FieldValue.increment(1)
                    )
                )
            }.await()

            // Trigger real notification for the recipient
            notificationRepository?.sendNotification(
                AppNotification(
                    recipientUid = receiverUid,
                    senderUid = senderUid,
                    senderPublicUserId = senderPublicUserId,
                    senderDisplayName = senderProfile?.displayName ?: "User",
                    senderAvatarUrl = senderProfile?.avatar ?: "",
                    type = NotificationTypes.NEW_MESSAGE,
                    title = "New Message from ${senderProfile?.displayName ?: "Someone"}",
                    body = text.take(100),
                    referenceId = conversationId,
                    referenceType = "CONVERSATION"
                )
            )

            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of messages for a conversation with pagination support.
     */
    fun getMessagesStream(
        conversationId: String,
        limit: Long = 50
    ): Flow<List<DirectMessage>> = callbackFlow {
        if (conversationId.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Notice listening to messages: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DirectMessage::class.java)
                }
                trySend(messages)
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Mark unread messages as read when the receiver opens the chat.
     */
    suspend fun markMessagesAsRead(
        conversationId: String,
        currentUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val convRef = firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId)
            val unreadSnapshot = convRef.collection(MESSAGES_COLLECTION)
                .whereEqualTo("receiverUid", currentUid)
                .whereEqualTo("isRead", false)
                .limit(50)
                .get()
                .await()

            if (!unreadSnapshot.isEmpty) {
                val now = System.currentTimeMillis()
                val batch = firestore.batch()
                for (doc in unreadSnapshot.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "isRead" to true,
                            "readAt" to now,
                            "status" to "seen"
                        )
                    )
                }
                // Reset user's unread count
                batch.update(convRef, "unreadCounts.$currentUid", 0L)
                batch.commit().await()
            } else {
                // Ensure conversation unread count is reset even if messages are already marked
                convRef.update("unreadCounts.$currentUid", 0L).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Soft delete a message.
     */
    suspend fun deleteMessage(
        conversationId: String,
        messageId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val msgRef = firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)

            msgRef.update(
                mapOf(
                    "deleted" to true,
                    "deletedAt" to System.currentTimeMillis(),
                    "text" to "This message was deleted"
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of all conversations for the current user.
     */
    fun getConversationsStream(currentUid: String): Flow<List<DirectConversationItem>> = callbackFlow {
        val effectiveUid = if (currentUid.isNotBlank()) currentUid else (auth.currentUser?.uid ?: "")
        if (effectiveUid.isBlank() || auth.currentUser == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val query = firestore.collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", effectiveUid)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Notice listening to conversations: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val conversations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DirectConversation::class.java)
                }.sortedByDescending { it.updatedAt }

                // In background coroutine, populate other participant profiles
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    val items = conversations.map { conv ->
                        val otherUid = conv.participantIds.find { it != effectiveUid } ?: effectiveUid
                        val profile = userRepository.getUserProfile(otherUid)
                        val unread = (conv.unreadCounts[effectiveUid] ?: 0L).toInt()
                        DirectConversationItem(
                            conversation = conv,
                            otherUser = profile,
                            unreadCount = unread
                        )
                    }
                    trySend(items)
                }
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Report a user or message.
     */
    suspend fun reportUserOrMessage(report: UserReportRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val reportId = if (report.reportId.isBlank()) "rep_${UUID.randomUUID()}" else report.reportId
            val finalRecord = report.copy(reportId = reportId, createdAt = System.currentTimeMillis())
            firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .set(finalRecord)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting: ${e.message}", e)
            Result.failure(e)
        }
    }
}
