package com.example.data.repository

import android.util.Log
import com.example.data.model.CallSession
import com.example.data.model.IceCandidatePayload
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CallRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) {
    companion object {
        private const val TAG = "CallRepository"
        private const val CALLS_COLLECTION = "calls"
        private const val CALLER_CANDIDATES = "caller_candidates"
        private const val RECEIVER_CANDIDATES = "receiver_candidates"
    }

    /**
     * Check if a caller is allowed to call target receiver based on blocks and privacy settings.
     */
    suspend fun canInitiateCall(callerUid: String, receiverUid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (chatRepository.isBlocked(callerUid, receiverUid)) {
                return@withContext Result.failure(IllegalStateException("Call blocked: You or the recipient have blocked communication."))
            }

            val receiverProfile = userRepository.getUserProfile(receiverUid)
            if (receiverProfile == null) {
                return@withContext Result.failure(IllegalStateException("Recipient account does not exist."))
            }

            // Check privacy setting (Everyone, Friends, Nobody)
            when (receiverProfile.callPrivacy) {
                "Nobody" -> return@withContext Result.failure(IllegalStateException("${receiverProfile.displayName} is not accepting voice calls."))
                "Friends" -> {
                    val isFollowing = userRepository.isFollowing(receiverUid, callerUid)
                    if (!isFollowing) {
                        return@withContext Result.failure(IllegalStateException("Only friends can call ${receiverProfile.displayName}."))
                    }
                }
                else -> { /* "Everyone" is allowed */ }
            }

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking call eligibility: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new call session document in Firestore with ringing status.
     */
    suspend fun createCallSession(session: CallSession): Result<String> = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection(CALLS_COLLECTION).document(session.callId)
            docRef.set(session).await()
            Result.success(session.callId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating call session: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update call status (ringing, accepted, rejected, busy, missed, ended, failed, cancelled).
     */
    suspend fun updateCallStatus(
        callId: String,
        status: String,
        endedBy: String? = null,
        duration: Long = 0L
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to status
            )
            val now = System.currentTimeMillis()
            if (status == "accepted") {
                updates["acceptedAt"] = now
            } else if (status in listOf("ended", "rejected", "missed", "failed", "cancelled")) {
                updates["endedAt"] = now
                if (endedBy != null) updates["endedBy"] = endedBy
                if (duration > 0) updates["duration"] = duration
            }

            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating call status: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload SDP offer (Caller).
     */
    suspend fun setSdpOffer(callId: String, sdpOffer: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .update("sdpOffer", sdpOffer)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sdp offer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload SDP answer (Receiver).
     */
    suspend fun setSdpAnswer(callId: String, sdpAnswer: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .update(
                    mapOf(
                        "sdpAnswer" to sdpAnswer,
                        "status" to "accepted",
                        "acceptedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting sdp answer: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Add ICE candidate from caller.
     */
    suspend fun addCallerCandidate(callId: String, candidate: IceCandidatePayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .collection(CALLER_CANDIDATES)
                .add(candidate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding caller candidate: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Add ICE candidate from receiver.
     */
    suspend fun addReceiverCandidate(callId: String, candidate: IceCandidatePayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection(CALLS_COLLECTION)
                .document(callId)
                .collection(RECEIVER_CANDIDATES)
                .add(candidate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding receiver candidate: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener on call session document.
     */
    fun listenToCall(callId: String): Flow<CallSession?> = callbackFlow {
        val registration = firestore.collection(CALLS_COLLECTION)
            .document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to call $callId: ${error.message}", error)
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val session = snapshot.toObject(CallSession::class.java)
                    trySend(session)
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Real-time stream of caller candidates (listened by receiver).
     */
    fun listenToCallerCandidates(callId: String): Flow<List<IceCandidatePayload>> = callbackFlow {
        val registration = firestore.collection(CALLS_COLLECTION)
            .document(callId)
            .collection(CALLER_CANDIDATES)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val candidates = snapshot.documents.mapNotNull { it.toObject(IceCandidatePayload::class.java) }
                trySend(candidates)
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Real-time stream of receiver candidates (listened by caller).
     */
    fun listenToReceiverCandidates(callId: String): Flow<List<IceCandidatePayload>> = callbackFlow {
        val registration = firestore.collection(CALLS_COLLECTION)
            .document(callId)
            .collection(RECEIVER_CANDIDATES)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val candidates = snapshot.documents.mapNotNull { it.toObject(IceCandidatePayload::class.java) }
                trySend(candidates)
            }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Real-time stream of incoming ringing calls for the current user.
     */
    fun listenToIncomingCalls(currentUid: String): Flow<CallSession?> = callbackFlow {
        val query = firestore.collection(CALLS_COLLECTION)
            .whereEqualTo("receiverUid", currentUid)
            .whereEqualTo("status", "ringing")
            .limit(1)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(null)
                return@addSnapshotListener
            }
            val incoming = snapshot.documents.firstOrNull()?.toObject(CallSession::class.java)
            trySend(incoming)
        }

        awaitClose {
            registration.remove()
        }
    }

    /**
     * Real-time call history stream for current user.
     */
    fun getCallHistoryStream(currentUid: String): Flow<List<CallSession>> = callbackFlow {
        // Query calls where caller is current user
        val callerQuery = firestore.collection(CALLS_COLLECTION)
            .whereEqualTo("callerUid", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)

        // Query calls where receiver is current user
        val receiverQuery = firestore.collection(CALLS_COLLECTION)
            .whereEqualTo("receiverUid", currentUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)

        var callerCalls = emptyList<CallSession>()
        var receiverCalls = emptyList<CallSession>()

        fun emitCombined() {
            val combined = (callerCalls + receiverCalls)
                .distinctBy { it.callId }
                .sortedByDescending { it.createdAt }
            trySend(combined)
        }

        val reg1 = callerQuery.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                callerCalls = snapshot.documents.mapNotNull { it.toObject(CallSession::class.java) }
                emitCombined()
            }
        }

        val reg2 = receiverQuery.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                receiverCalls = snapshot.documents.mapNotNull { it.toObject(CallSession::class.java) }
                emitCombined()
            }
        }

        awaitClose {
            reg1.remove()
            reg2.remove()
        }
    }
}
