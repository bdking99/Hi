package com.example.data.repository

import com.example.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class WalletRepository {

    companion object {
        const val WALLETS_COLLECTION = "wallets"
        const val TRANSACTIONS_COLLECTION = "walletTransactions"
        const val GIFTS_COLLECTION = "gifts"
        const val GIFT_TRANSACTIONS_COLLECTION = "giftTransactions"
        const val FRAMES_COLLECTION = "profileFrames"
        const val USER_FRAMES_COLLECTION = "userFrames"
        const val USERS_COLLECTION = "users"
        const val AUDIT_LOGS_COLLECTION = "auditLogs"
        const val COIN_PACKAGES_COLLECTION = "coinPackages"
        const val COIN_PURCHASE_REQUESTS_COLLECTION = "coinPurchaseRequests"
        const val WITHDRAWAL_REQUESTS_COLLECTION = "withdrawalRequests"
        const val COIN_EXCHANGE_REQUESTS_COLLECTION = "coinExchangeRequests"
        const val SYSTEM_SETTINGS_COLLECTION = "system_settings"
        const val PAYMENT_CONFIG_DOC = "payment_config"
    }

    private val coroutineScope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 1. WALLET REAL-TIME STREAM & SNAPSHOT
    // ==========================================

    fun getWalletStream(uid: String): Flow<UserWallet?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(UserWallet(uid = uid, coinBalance = 2500L))
            close()
            return@callbackFlow
        }

        try {
            val docRef = firestore.collection(WALLETS_COLLECTION).document(uid)
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val wallet = snapshot.toObject(UserWallet::class.java)
                    trySend(wallet)
                } else {
                    // Auto-initialize wallet for this user if not present
                    initWalletForUser(firestore, uid)
                    trySend(UserWallet(uid = uid, coinBalance = 2500L))
                }
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun initWalletForUser(firestore: FirebaseFirestore, uid: String) {
        try {
            firestore.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener { userDoc ->
                    val publicId = userDoc.getString("publicUserId") ?: "1000001"
                    val existingCoins = userDoc.getLong("coinBalance") ?: 2500L
                    val wallet = UserWallet(
                        uid = uid,
                        publicUserId = publicId,
                        coinBalance = existingCoins,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    firestore.collection(WALLETS_COLLECTION).document(uid).set(wallet, SetOptions.merge())
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getWallet(uid: String): UserWallet? = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext null
        try {
            val snapshot = firestore.collection(WALLETS_COLLECTION).document(uid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserWallet::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 2. ATOMIC COIN RECHARGE / TOP UP
    // ==========================================

    suspend fun rechargeWallet(
        uid: String,
        amount: Long,
        packageTitle: String,
        operationId: String = UUID.randomUUID().toString()
    ): Result<Long> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firebase unavailable"))

        try {
            // 1. Idempotency Check: Check if transaction already exists
            val existingTx = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("referenceId", operationId)
                .limit(1)
                .get()
                .await()

            if (!existingTx.isEmpty) {
                val existingDoc = existingTx.documents.first()
                val balAfter = existingDoc.getLong("balanceAfter") ?: 0L
                return@withContext Result.success(balAfter)
            }

            // 2. Run Atomic Transaction
            val updatedBalance = firestore.runTransaction { transaction ->
                val walletRef = firestore.collection(WALLETS_COLLECTION).document(uid)
                val userRef = firestore.collection(USERS_COLLECTION).document(uid)
                
                val walletSnap = transaction.get(walletRef)
                val userSnap = transaction.get(userRef)

                val publicId = if (walletSnap.exists()) {
                    walletSnap.getString("publicUserId") ?: userSnap.getString("publicUserId") ?: ""
                } else {
                    userSnap.getString("publicUserId") ?: ""
                }

                val currentBalance = if (walletSnap.exists()) {
                    walletSnap.getLong("coinBalance") ?: 0L
                } else {
                    userSnap.getLong("coinBalance") ?: 2500L
                }

                val newBalance = currentBalance + amount
                val lifetimePurchased = (walletSnap.getLong("lifetimePurchasedCoins") ?: 0L) + amount

                // Update Wallet
                val walletUpdates = mapOf(
                    "uid" to uid,
                    "publicUserId" to publicId,
                    "coinBalance" to newBalance,
                    "lifetimePurchasedCoins" to lifetimePurchased,
                    "updatedAt" to System.currentTimeMillis()
                )
                transaction.set(walletRef, walletUpdates, SetOptions.merge())

                // Sync Users document
                transaction.update(userRef, "coinBalance", newBalance)

                // Create Ledger Transaction
                val txId = firestore.collection(TRANSACTIONS_COLLECTION).document().id
                val tx = WalletTransaction(
                    transactionId = txId,
                    uid = uid,
                    publicUserId = publicId,
                    type = "COIN_RECHARGE",
                    amount = amount,
                    balanceBefore = currentBalance,
                    balanceAfter = newBalance,
                    referenceId = operationId,
                    status = "SUCCESS",
                    description = "Top up: $packageTitle (+%,d Coins)".format(amount),
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                val txRef = firestore.collection(TRANSACTIONS_COLLECTION).document(txId)
                transaction.set(txRef, tx)

                // Create Audit Log
                val auditId = firestore.collection(AUDIT_LOGS_COLLECTION).document().id
                val audit = AuditLogRecord(
                    logId = auditId,
                    actorUid = uid,
                    actorRole = "USER",
                    action = "COIN_RECHARGE",
                    targetUid = uid,
                    referenceId = txId,
                    metadata = mapOf("amount" to amount, "package" to packageTitle, "operationId" to operationId)
                )
                transaction.set(firestore.collection(AUDIT_LOGS_COLLECTION).document(auditId), audit)

                newBalance
            }.await()

            Result.success(updatedBalance)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // ==========================================
    // 3. ATOMIC SEND GIFT WITH IDEMPOTENCY
    // ==========================================

    suspend fun sendGiftAtomic(
        senderUid: String,
        recipientUid: String,
        giftId: String,
        quantity: Int = 1,
        roomId: String? = null,
        roomTitle: String? = null,
        operationId: String = UUID.randomUUID().toString(),
        allowSelfGift: Boolean = false
    ): GiftSendResult = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext GiftSendResult.Failure("Database connection unavailable.")

        if (senderUid == recipientUid && !allowSelfGift) {
            return@withContext GiftSendResult.Failure("Self-gifting is disabled by policy. Please select another user.")
        }

        if (quantity <= 0) {
            return@withContext GiftSendResult.Failure("Invalid gift quantity ($quantity).")
        }

        try {
            // Check block status between users
            val blocked1 = firestore.collection(USERS_COLLECTION).document(senderUid).collection("blocked").document(recipientUid).get().await()
            val blocked2 = firestore.collection(USERS_COLLECTION).document(recipientUid).collection("blocked").document(senderUid).get().await()
            if (blocked1.exists() || blocked2.exists()) {
                return@withContext GiftSendResult.Failure("Cannot send gift: Interaction between users is blocked.")
            }

            // 1. Idempotency Check: Verify if this operationId was already executed
            val existingGiftQuery = firestore.collection(GIFT_TRANSACTIONS_COLLECTION)
                .whereEqualTo("operationId", operationId)
                .limit(1)
                .get()
                .await()

            if (!existingGiftQuery.isEmpty) {
                val existingRecord = existingGiftQuery.documents.first().toObject(GiftTransactionRecord::class.java)
                if (existingRecord != null) {
                    val senderWalletSnap = firestore.collection(WALLETS_COLLECTION).document(senderUid).get().await()
                    val curBal = senderWalletSnap.getLong("coinBalance") ?: 0L
                    return@withContext GiftSendResult.Success(existingRecord, curBal)
                }
            }

            // 2. Fetch Gift Metadata
            val giftDoc = firestore.collection(GIFTS_COLLECTION).document(giftId).get().await()
            val giftItem = if (giftDoc.exists()) {
                giftDoc.toObject(CatalogGift::class.java)
            } else {
                // Fallback from predefined catalog
                getDefaultGifts().find { it.giftId == giftId }
            }

            if (giftItem == null || !giftItem.isActive) {
                return@withContext GiftSendResult.Failure("This gift is currently unavailable or inactive.")
            }

            val totalCost = giftItem.coinPrice * quantity

            // 3. Execute Firestore Transaction
            val resultRecord = firestore.runTransaction { transaction ->
                val senderWalletRef = firestore.collection(WALLETS_COLLECTION).document(senderUid)
                val recipientWalletRef = firestore.collection(WALLETS_COLLECTION).document(recipientUid)
                val senderUserRef = firestore.collection(USERS_COLLECTION).document(senderUid)
                val recipientUserRef = firestore.collection(USERS_COLLECTION).document(recipientUid)

                val senderWalletSnap = transaction.get(senderWalletRef)
                val recipientWalletSnap = transaction.get(recipientWalletRef)
                val senderUserSnap = transaction.get(senderUserRef)
                val recipientUserSnap = transaction.get(recipientUserRef)

                val senderBalance = if (senderWalletSnap.exists()) {
                    senderWalletSnap.getLong("coinBalance") ?: 0L
                } else {
                    senderUserSnap.getLong("coinBalance") ?: 2500L
                }

                if (senderBalance < totalCost) {
                    throw IllegalStateException("Insufficient coin balance. Required: %,d Coins, Available: %,d Coins.".format(totalCost, senderBalance))
                }

                val senderPublicId = senderUserSnap.getString("publicUserId") ?: senderWalletSnap.getString("publicUserId") ?: "1000001"
                val senderName = senderUserSnap.getString("displayName") ?: "Sender"
                val senderAvatar = senderUserSnap.getString("avatar") ?: ""

                val recipientPublicId = recipientUserSnap.getString("publicUserId") ?: recipientWalletSnap.getString("publicUserId") ?: "1000002"
                val recipientName = recipientUserSnap.getString("displayName") ?: "Recipient"
                val recipientAvatar = recipientUserSnap.getString("avatar") ?: ""

                val newSenderBalance = senderBalance - totalCost
                val senderLifetimeSpent = (senderWalletSnap.getLong("lifetimeSpentCoins") ?: 0L) + totalCost

                // Update Sender Wallet
                transaction.set(
                    senderWalletRef,
                    mapOf(
                        "uid" to senderUid,
                        "publicUserId" to senderPublicId,
                        "coinBalance" to newSenderBalance,
                        "lifetimeSpentCoins" to senderLifetimeSpent,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                transaction.update(senderUserRef, "coinBalance", newSenderBalance)

                // Update Recipient Wallet (credit received gifts value)
                val recipientCurrentBalance = if (recipientWalletSnap.exists()) {
                    recipientWalletSnap.getLong("coinBalance") ?: 0L
                } else {
                    recipientUserSnap.getLong("coinBalance") ?: 0L
                }
                val recipientLifetimeReceived = (recipientWalletSnap.getLong("lifetimeReceivedCoins") ?: 0L) + totalCost
                val recipientLifetimeGiftVal = (recipientWalletSnap.getLong("lifetimeGiftValue") ?: 0L) + totalCost

                transaction.set(
                    recipientWalletRef,
                    mapOf(
                        "uid" to recipientUid,
                        "publicUserId" to recipientPublicId,
                        "lifetimeReceivedCoins" to recipientLifetimeReceived,
                        "lifetimeGiftValue" to recipientLifetimeGiftVal,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                transaction.update(
                    recipientUserRef,
                    mapOf(
                        "giftsReceivedCount" to FieldValue.increment(quantity.toLong()),
                        "diamondBalance" to FieldValue.increment(totalCost)
                    )
                )

                // Create Gift Transaction Record
                val giftTxId = firestore.collection(GIFT_TRANSACTIONS_COLLECTION).document().id
                val giftRecord = GiftTransactionRecord(
                    giftTransactionId = giftTxId,
                    operationId = operationId,
                    senderUid = senderUid,
                    senderPublicUserId = senderPublicId,
                    senderDisplayName = senderName,
                    senderAvatarUrl = senderAvatar,
                    recipientUid = recipientUid,
                    recipientPublicUserId = recipientPublicId,
                    recipientDisplayName = recipientName,
                    recipientAvatarUrl = recipientAvatar,
                    giftId = giftItem.giftId,
                    giftName = giftItem.name,
                    giftEmoji = giftItem.emoji,
                    giftImageUrl = giftItem.imageUrl,
                    coinPrice = giftItem.coinPrice,
                    quantity = quantity,
                    totalCoins = totalCost,
                    roomId = roomId,
                    roomTitle = roomTitle,
                    createdAt = System.currentTimeMillis(),
                    status = "SUCCESS"
                )
                transaction.set(firestore.collection(GIFT_TRANSACTIONS_COLLECTION).document(giftTxId), giftRecord)

                // Create Sender Ledger Transaction
                val senderTxId = firestore.collection(TRANSACTIONS_COLLECTION).document().id
                val senderTx = WalletTransaction(
                    transactionId = senderTxId,
                    uid = senderUid,
                    publicUserId = senderPublicId,
                    type = "GIFT_SENT",
                    amount = -totalCost,
                    balanceBefore = senderBalance,
                    balanceAfter = newSenderBalance,
                    referenceId = giftTxId,
                    status = "SUCCESS",
                    description = "Sent ${giftItem.emoji} ${giftItem.name} x$quantity to $recipientName (ID: $recipientPublicId)",
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                transaction.set(firestore.collection(TRANSACTIONS_COLLECTION).document(senderTxId), senderTx)

                // Create Recipient Ledger Transaction
                val recipientTxId = firestore.collection(TRANSACTIONS_COLLECTION).document().id
                val recipientTx = WalletTransaction(
                    transactionId = recipientTxId,
                    uid = recipientUid,
                    publicUserId = recipientPublicId,
                    type = "GIFT_RECEIVED",
                    amount = totalCost,
                    balanceBefore = recipientCurrentBalance,
                    balanceAfter = recipientCurrentBalance + totalCost,
                    referenceId = giftTxId,
                    status = "SUCCESS",
                    description = "Received ${giftItem.emoji} ${giftItem.name} x$quantity from $senderName (ID: $senderPublicId)",
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                transaction.set(firestore.collection(TRANSACTIONS_COLLECTION).document(recipientTxId), recipientTx)

                // Write In-Room Chat/Event if inside a voice room
                if (!roomId.isNullOrBlank()) {
                    val chatMsgId = firestore.collection("rooms").document(roomId).collection("chat").document().id
                    val giftChatMessage = ChatMessage(
                        id = chatMsgId,
                        roomId = roomId,
                        senderId = senderUid,
                        senderName = senderName,
                        senderAvatar = senderAvatar,
                        text = "🎁 Sent ${giftItem.emoji} ${giftItem.name} x$quantity to $recipientName!",
                        type = MessageType.GIFT,
                        timestamp = System.currentTimeMillis()
                    )
                    transaction.set(
                        firestore.collection("rooms").document(roomId).collection("chat").document(chatMsgId),
                        giftChatMessage
                    )
                }

                // Create Audit Log
                val auditId = firestore.collection(AUDIT_LOGS_COLLECTION).document().id
                val audit = AuditLogRecord(
                    logId = auditId,
                    actorUid = senderUid,
                    actorRole = "USER",
                    action = "GIFT_SENT",
                    targetUid = recipientUid,
                    referenceId = giftTxId,
                    metadata = mapOf(
                        "giftId" to giftItem.giftId,
                        "quantity" to quantity,
                        "totalCoins" to totalCost,
                        "roomId" to roomId
                    )
                )
                transaction.set(firestore.collection(AUDIT_LOGS_COLLECTION).document(auditId), audit)

                Pair(giftRecord, newSenderBalance)
            }.await()

            // Trigger real notification for the recipient
            try {
                val notifId = "notif_${UUID.randomUUID()}"
                val notif = AppNotification(
                    notificationId = notifId,
                    recipientUid = recipientUid,
                    senderUid = senderUid,
                    senderPublicUserId = resultRecord.first.senderPublicUserId,
                    senderDisplayName = resultRecord.first.senderDisplayName,
                    senderAvatarUrl = resultRecord.first.senderAvatarUrl,
                    type = NotificationTypes.GIFT_RECEIVED,
                    title = "Gift Received! ${resultRecord.first.giftEmoji}",
                    body = "${resultRecord.first.senderDisplayName} sent you ${resultRecord.first.giftEmoji} ${resultRecord.first.giftName} x$quantity!",
                    referenceId = resultRecord.first.giftTransactionId,
                    referenceType = "GIFT",
                    createdAt = System.currentTimeMillis()
                )
                firestore.collection("notifications").document(notifId).set(notif)
            } catch (ignored: Exception) {}

            GiftSendResult.Success(resultRecord.first, resultRecord.second)
        } catch (e: Exception) {
            e.printStackTrace()
            GiftSendResult.Failure(e.localizedMessage ?: "Failed to complete gift transaction.")
        }
    }

    // ==========================================
    // 4. ATOMIC PROFILE FRAME PURCHASE & OWNERSHIP
    // ==========================================

    suspend fun purchaseFrameAtomic(
        uid: String,
        frameId: String,
        operationId: String = UUID.randomUUID().toString()
    ): FramePurchaseResult = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext FramePurchaseResult.Failure("Database connection unavailable.")

        try {
            // 1. Check if user already owns this frame
            val ownedDoc = firestore.collection(USER_FRAMES_COLLECTION)
                .document(uid)
                .collection("owned")
                .document(frameId)
                .get()
                .await()

            if (ownedDoc.exists()) {
                // Already owned, auto-equip
                equipFrame(uid, frameId)
                val curBal = getWallet(uid)?.coinBalance ?: 0L
                val frameItem = getProfileFramesCatalog().find { it.frameId == frameId } ?: getDefaultFrames().first()
                return@withContext FramePurchaseResult.Success(frameItem, curBal)
            }

            // 2. Fetch Frame Metadata
            val frameDoc = firestore.collection(FRAMES_COLLECTION).document(frameId).get().await()
            val frameItem = if (frameDoc.exists()) {
                frameDoc.toObject(ProfileFrameItem::class.java)
            } else {
                getDefaultFrames().find { it.frameId == frameId }
            }

            if (frameItem == null || !frameItem.isActive) {
                return@withContext FramePurchaseResult.Failure("This profile frame is currently unavailable.")
            }

            // 3. Execute Transaction
            val newBalance = firestore.runTransaction { transaction ->
                val walletRef = firestore.collection(WALLETS_COLLECTION).document(uid)
                val userRef = firestore.collection(USERS_COLLECTION).document(uid)
                val ownedFrameRef = firestore.collection(USER_FRAMES_COLLECTION).document(uid).collection("owned").document(frameId)

                val walletSnap = transaction.get(walletRef)
                val userSnap = transaction.get(userRef)

                val currentBalance = if (walletSnap.exists()) {
                    walletSnap.getLong("coinBalance") ?: 0L
                } else {
                    userSnap.getLong("coinBalance") ?: 2500L
                }

                if (currentBalance < frameItem.requiredCoins) {
                    throw IllegalStateException("Insufficient coins to purchase frame. Required: %,d Coins, Available: %,d Coins.".format(frameItem.requiredCoins, currentBalance))
                }

                val publicId = userSnap.getString("publicUserId") ?: walletSnap.getString("publicUserId") ?: "1000001"
                val updatedBal = currentBalance - frameItem.requiredCoins
                val lifetimeSpent = (walletSnap.getLong("lifetimeSpentCoins") ?: 0L) + frameItem.requiredCoins

                // Deduct from wallet
                transaction.set(
                    walletRef,
                    mapOf(
                        "uid" to uid,
                        "publicUserId" to publicId,
                        "coinBalance" to updatedBal,
                        "lifetimeSpentCoins" to lifetimeSpent,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                transaction.update(userRef, "coinBalance", updatedBal)

                // Equip frame on user profile
                transaction.update(userRef, "profileFrameId", frameId)

                // Add to owned frames collection
                val ownedRecord = OwnedFrame(
                    frameId = frameId,
                    uid = uid,
                    acquiredAt = System.currentTimeMillis(),
                    source = "PURCHASE",
                    isEquipped = true
                )
                transaction.set(ownedFrameRef, ownedRecord)

                // Add ledger transaction
                val txId = firestore.collection(TRANSACTIONS_COLLECTION).document().id
                val tx = WalletTransaction(
                    transactionId = txId,
                    uid = uid,
                    publicUserId = publicId,
                    type = "FRAME_PURCHASE",
                    amount = -frameItem.requiredCoins,
                    balanceBefore = currentBalance,
                    balanceAfter = updatedBal,
                    referenceId = frameId,
                    status = "SUCCESS",
                    description = "Purchased Frame: ${frameItem.name} (-%,d Coins)".format(frameItem.requiredCoins),
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                transaction.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), tx)

                // Audit log
                val auditId = firestore.collection(AUDIT_LOGS_COLLECTION).document().id
                val audit = AuditLogRecord(
                    logId = auditId,
                    actorUid = uid,
                    actorRole = "USER",
                    action = "FRAME_PURCHASED",
                    targetUid = uid,
                    referenceId = frameId,
                    metadata = mapOf("price" to frameItem.requiredCoins, "frameName" to frameItem.name)
                )
                transaction.set(firestore.collection(AUDIT_LOGS_COLLECTION).document(auditId), audit)

                updatedBal
            }.await()

            FramePurchaseResult.Success(frameItem, newBalance)
        } catch (e: Exception) {
            e.printStackTrace()
            FramePurchaseResult.Failure(e.localizedMessage ?: "Failed to purchase profile frame.")
        }
    }

    suspend fun equipFrame(uid: String, frameId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firestore unavailable"))
        try {
            // Update user profile
            firestore.collection(USERS_COLLECTION).document(uid).update("profileFrameId", frameId).await()
            // Mark isEquipped in owned frames
            val ownedQuery = firestore.collection(USER_FRAMES_COLLECTION).document(uid).collection("owned").get().await()
            for (doc in ownedQuery.documents) {
                doc.reference.update("isEquipped", doc.id == frameId).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun unequipFrame(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Firestore unavailable"))
        try {
            firestore.collection(USERS_COLLECTION).document(uid).update("profileFrameId", "frame_default").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 5. GIFT & FRAME CATALOG REAL-TIME STREAMS
    // ==========================================

    fun getGiftsCatalogStream(): Flow<List<CatalogGift>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultGifts())
            close()
            return@callbackFlow
        }

        try {
            val collectionRef = firestore.collection(GIFTS_COLLECTION).orderBy("sortOrder", Query.Direction.ASCENDING)
            val listener = collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    seedDefaultGifts(firestore)
                    trySend(getDefaultGifts())
                    return@addSnapshotListener
                }

                val gifts = snapshot.toObjects(CatalogGift::class.java).filter { it.isActive }
                trySend(if (gifts.isNotEmpty()) gifts else getDefaultGifts())
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getDefaultGifts())
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun seedDefaultGifts(firestore: FirebaseFirestore) {
        val defaultGifts = getDefaultGifts()
        for (gift in defaultGifts) {
            firestore.collection(GIFTS_COLLECTION).document(gift.giftId).set(gift, SetOptions.merge())
        }
    }

    fun getDefaultGifts(): List<CatalogGift> {
        return listOf(
            CatalogGift("gift_rose", "Rose", "A fragrant romantic red rose", "", "", "", 10, "Romantic", "🌹", true, 1),
            CatalogGift("gift_heart", "Heart", "Pure love and heartfelt energy", "", "", "", 50, "Romantic", "💖", true, 2),
            CatalogGift("gift_coffee", "Warm Coffee", "Fresh brew for the speaker", "", "", "", 100, "Support", "☕", true, 3),
            CatalogGift("gift_diamond", "Sparkling Diamond", "Brilliant cut gem of honor", "", "", "", 500, "Luxury", "💎", true, 4),
            CatalogGift("gift_crown", "Imperial Crown", "Fit for voice royalty", "", "", "", 1000, "VIP", "👑", true, 5),
            CatalogGift("gift_sports_car", "Sports Car", "Speed and roar through the room", "", "", "", 2500, "Luxury", "🏎️", true, 6, isFullScreenEffect = true),
            CatalogGift("gift_space_rocket", "Space Rocket", "Launches into orbit with sparks", "", "", "", 5000, "VIP", "🚀", true, 7, isFullScreenEffect = true),
            CatalogGift("gift_super_yacht", "Mega Yacht", "Cruising on celebration waves", "", "", "", 10000, "Super VIP", "🛳️", true, 8, isFullScreenEffect = true),
            CatalogGift("gift_crystal_castle", "Crystal Castle", "Grand supreme palace of triumph", "", "", "", 20000, "Super VIP", "🏰", true, 9, isFullScreenEffect = true)
        )
    }

    fun getProfileFramesCatalogStream(): Flow<List<ProfileFrameItem>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultFrames())
            close()
            return@callbackFlow
        }

        try {
            val collectionRef = firestore.collection(FRAMES_COLLECTION).orderBy("sortOrder", Query.Direction.ASCENDING)
            val listener = collectionRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) {
                    seedDefaultFrames(firestore)
                    trySend(getDefaultFrames())
                    return@addSnapshotListener
                }

                val frames = snapshot.toObjects(ProfileFrameItem::class.java).filter { it.isActive }
                trySend(if (frames.isNotEmpty()) frames else getDefaultFrames())
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getDefaultFrames())
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun seedDefaultFrames(firestore: FirebaseFirestore) {
        val defaultFrames = getDefaultFrames()
        for (frame in defaultFrames) {
            firestore.collection(FRAMES_COLLECTION).document(frame.frameId).set(frame, SetOptions.merge())
        }
    }

    suspend fun getProfileFramesCatalog(): List<ProfileFrameItem> = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext getDefaultFrames()
        try {
            val snapshot = firestore.collection(FRAMES_COLLECTION).orderBy("sortOrder", Query.Direction.ASCENDING).get().await()
            if (!snapshot.isEmpty) {
                snapshot.toObjects(ProfileFrameItem::class.java)
            } else {
                getDefaultFrames()
            }
        } catch (e: Exception) {
            getDefaultFrames()
        }
    }

    fun getDefaultFrames(): List<ProfileFrameItem> {
        return listOf(
            ProfileFrameItem(
                frameId = "frame_gold_crown",
                name = "Imperial Gold Crown",
                description = "Emperor's crown ring with sparkling gold aura",
                type = "VIP",
                rarity = "Legendary",
                requiredCoins = 1000L,
                requiredVipLevel = 1,
                glowColorHex = 0xFFFFD700,
                secondaryColorHex = 0xFFFFA000,
                sortOrder = 1
            ),
            ProfileFrameItem(
                frameId = "frame_neon_cyber",
                name = "Cyber Neon Aura",
                description = "High-voltage neon particle ring",
                type = "EVENT",
                rarity = "Epic",
                requiredCoins = 500L,
                requiredVipLevel = 0,
                glowColorHex = 0xFF00E5FF,
                secondaryColorHex = 0xFF9D00FF,
                sortOrder = 2
            ),
            ProfileFrameItem(
                frameId = "frame_royal_ruby",
                name = "Royal Ruby Sovereign",
                description = "Crimson ruby ring with blazing radiance",
                type = "LUXURY",
                rarity = "Legendary",
                requiredCoins = 2500L,
                requiredVipLevel = 2,
                glowColorHex = 0xFFFF1744,
                secondaryColorHex = 0xFFFF80AB,
                sortOrder = 3
            ),
            ProfileFrameItem(
                frameId = "frame_dragon_fire",
                name = "Blazing Dragon Flame",
                description = "Mythical dragon flame aura",
                type = "EVENT",
                rarity = "Epic",
                requiredCoins = 1500L,
                requiredVipLevel = 0,
                glowColorHex = 0xFFFF6D00,
                secondaryColorHex = 0xFFFFD600,
                sortOrder = 4
            ),
            ProfileFrameItem(
                frameId = "frame_emerald_king",
                name = "Emerald Sovereign Halo",
                description = "Gleaming jade and emerald crystal ring",
                type = "VIP",
                rarity = "Rare",
                requiredCoins = 800L,
                requiredVipLevel = 1,
                glowColorHex = 0xFF00E676,
                secondaryColorHex = 0xFF1DE9B6,
                sortOrder = 5
            ),
            ProfileFrameItem(
                frameId = "frame_celestial_star",
                name = "Celestial Crystal Star",
                description = "Cosmic nebula glow with crystal star flares",
                type = "LUXURY",
                rarity = "Legendary",
                requiredCoins = 5000L,
                requiredVipLevel = 3,
                glowColorHex = 0xFFE040FB,
                secondaryColorHex = 0xFF7C4DFF,
                sortOrder = 6
            )
        )
    }

    // ==========================================
    // 6. OWNED FRAMES & TRANSACTION LISTS
    // ==========================================

    fun getUserOwnedFramesStream(uid: String): Flow<List<OwnedFrame>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(USER_FRAMES_COLLECTION)
                .document(uid)
                .collection("owned")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val owned = snapshot.toObjects(OwnedFrame::class.java)
                    trySend(owned)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getWalletTransactionsStream(uid: String, filterType: String = "ALL"): Flow<List<WalletTransaction>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            var query = firestore.collection(TRANSACTIONS_COLLECTION)
                .whereEqualTo("uid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)

            if (filterType != "ALL") {
                query = query.whereEqualTo("type", filterType)
            }

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot.toObjects(WalletTransaction::class.java)
                trySend(items)
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getGiftsSentStream(uid: String): Flow<List<GiftTransactionRecord>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(GIFT_TRANSACTIONS_COLLECTION)
                .whereEqualTo("senderUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.toObjects(GiftTransactionRecord::class.java)
                    trySend(items)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    fun getGiftsReceivedStream(uid: String): Flow<List<GiftTransactionRecord>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(GIFT_TRANSACTIONS_COLLECTION)
                .whereEqualTo("recipientUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.toObjects(GiftTransactionRecord::class.java)
                    trySend(items)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            trySend(emptyList())
            close()
        }
    }.flowOn(Dispatchers.IO)

    // ==========================================
    // 7. DYNAMIC COIN PURCHASE PACKAGES (Firestore)
    // ==========================================

    val DEFAULT_COIN_PACKAGES = listOf(
        CoinPackage("pack_100k", 100000L, 0L, 550.0, "BDT", true, 1, null),
        CoinPackage("pack_200k", 200000L, 0L, 1100.0, "BDT", true, 2, null),
        CoinPackage("pack_300k", 300000L, 0L, 1650.0, "BDT", true, 3, null),
        CoinPackage("pack_400k", 400000L, 0L, 2200.0, "BDT", true, 4, null),
        CoinPackage("pack_500k", 500000L, 0L, 2750.0, "BDT", true, 5, "POPULAR"),
        CoinPackage("pack_1m", 1000000L, 25000L, 5500.0, "BDT", true, 6, "BEST VALUE"),
        CoinPackage("pack_2m", 2000000L, 60000L, 11000.0, "BDT", true, 7, "SPECIAL"),
        CoinPackage("pack_3m", 3000000L, 100000L, 16500.0, "BDT", true, 8, "VIP BONUS"),
        CoinPackage("pack_4m", 4000000L, 150000L, 22000.0, "BDT", true, 9, "KING PACK"),
        CoinPackage("pack_5m", 5000000L, 250000L, 27500.0, "BDT", true, 10, "SUPREME")
    )

    fun getCoinPackagesStream(): Flow<List<CoinPackage>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(DEFAULT_COIN_PACKAGES)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COIN_PACKAGES_COLLECTION)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DEFAULT_COIN_PACKAGES)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val packages = snapshot.documents.mapNotNull { it.toObject(CoinPackage::class.java) }
                        .sortedBy { it.sortOrder }
                    trySend(packages)
                } else {
                    // Seed defaults if empty
                    seedDefaultCoinPackages()
                    trySend(DEFAULT_COIN_PACKAGES)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    private fun seedDefaultCoinPackages() {
        val firestore = getFirestore() ?: return
        coroutineScope.launch {
            try {
                for (pack in DEFAULT_COIN_PACKAGES) {
                    firestore.collection(COIN_PACKAGES_COLLECTION).document(pack.packageId)
                        .set(pack, SetOptions.merge())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun saveCoinPackage(
        packageItem: CoinPackage,
        adminUid: String,
        adminRole: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin role required."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))
            val packId = packageItem.packageId.ifBlank { "pack_${UUID.randomUUID()}" }
            val itemToSave = packageItem.copy(packageId = packId, updatedAt = System.currentTimeMillis())
            firestore.collection(COIN_PACKAGES_COLLECTION).document(packId).set(itemToSave, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 8. PAYMENT SYSTEM CONFIG (bKash / Nagad)
    // ==========================================

    fun getPaymentSystemConfigStream(): Flow<PaymentSystemConfig> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(PaymentSystemConfig())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(SYSTEM_SETTINGS_COLLECTION).document(PAYMENT_CONFIG_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(PaymentSystemConfig())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val config = snapshot.toObject(PaymentSystemConfig::class.java) ?: PaymentSystemConfig()
                    trySend(config)
                } else {
                    val defaultConfig = PaymentSystemConfig()
                    firestore.collection(SYSTEM_SETTINGS_COLLECTION).document(PAYMENT_CONFIG_DOC).set(defaultConfig)
                    trySend(defaultConfig)
                }
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun savePaymentSystemConfig(
        config: PaymentSystemConfig,
        adminUid: String,
        adminRole: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin role required."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))
            firestore.collection(SYSTEM_SETTINGS_COLLECTION).document(PAYMENT_CONFIG_DOC)
                .set(config.copy(updatedAt = System.currentTimeMillis()), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 9. COIN PURCHASE REQUEST (Bangla Payment UI)
    // ==========================================

    suspend fun submitCoinPurchaseRequest(
        uid: String,
        publicUserId: String,
        packageId: String,
        paymentMethod: String,
        paymentReference: String,
        senderAccountPhone: String
    ): Result<CoinPurchaseRequest> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))
            val cleanRef = paymentReference.trim().uppercase()
            if (cleanRef.length < 5) {
                return@withContext Result.failure(IllegalArgumentException("সঠিক Transaction ID (TrxID) প্রদান করুন (কমপক্ষে ৫ অক্ষর)।"))
            }

            // 1. Duplicate TrxID protection: check if reference was ever submitted
            val existing = firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION)
                .whereEqualTo("paymentReference", cleanRef)
                .limit(1)
                .get().await()

            if (!existing.isEmpty) {
                return@withContext Result.failure(IllegalStateException("এই Transaction ID ($cleanRef) টি ইতিমধ্যে ব্যবহার করা হয়েছে! অনুগ্রহ করে আপনার নিজস্ব পেমেন্টের সঠিক ট্রানজেকশন আইডি দিন।"))
            }

            // 2. Fetch authoritative package details from Firestore
            val packSnap = firestore.collection(COIN_PACKAGES_COLLECTION).document(packageId).get().await()
            val pack = if (packSnap.exists()) {
                packSnap.toObject(CoinPackage::class.java)
            } else {
                DEFAULT_COIN_PACKAGES.find { it.packageId == packageId }
            } ?: return@withContext Result.failure(IllegalArgumentException("প্যাকেজটি খুঁজে পাওয়া যায়নি।"))

            val requestId = "cpr_${UUID.randomUUID().toString().take(12)}"
            val request = CoinPurchaseRequest(
                requestId = requestId,
                uid = uid,
                publicUserId = publicUserId,
                packageId = pack.packageId,
                coinAmount = pack.coinAmount,
                bonusCoins = pack.bonusCoins,
                totalCoins = pack.coinAmount + pack.bonusCoins,
                priceBDT = pack.priceBDT,
                paymentMethod = paymentMethod,
                paymentReference = cleanRef,
                senderAccountPhone = senderAccountPhone.trim(),
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION).document(requestId).set(request).await()

            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserPurchaseRequestsStream(uid: String): Flow<List<CoinPurchaseRequest>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION)
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.toObjects(CoinPurchaseRequest::class.java)
                trySend(list)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun getAllPurchaseRequestsStream(statusFilter: String? = null): Flow<List<CoinPurchaseRequest>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var query = firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

        if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
            query = firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION)
                .whereEqualTo("status", statusFilter)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.toObjects(CoinPurchaseRequest::class.java)
            trySend(list)
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    // Server-Authoritative Approval of Coin Purchase by Admin
    suspend fun approveCoinPurchaseRequest(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        requestId: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))

            val newBalance = firestore.runTransaction { tx ->
                val reqRef = firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION).document(requestId)
                val reqSnap = tx.get(reqRef)
                if (!reqSnap.exists()) {
                    throw IllegalArgumentException("Purchase request not found.")
                }
                val currentStatus = reqSnap.getString("status") ?: "PENDING"
                if (currentStatus == "APPROVED") {
                    throw IllegalStateException("This purchase request has already been approved.")
                }

                val uid = reqSnap.getString("uid") ?: throw IllegalStateException("Missing user ID.")
                val publicUserId = reqSnap.getString("publicUserId") ?: ""
                val coinAmount = reqSnap.getLong("coinAmount") ?: 0L
                val bonusCoins = reqSnap.getLong("bonusCoins") ?: 0L
                val totalCoins = coinAmount + bonusCoins
                val paymentRef = reqSnap.getString("paymentReference") ?: ""
                val packageId = reqSnap.getString("packageId") ?: ""

                val walletRef = firestore.collection(WALLETS_COLLECTION).document(uid)
                val walletSnap = tx.get(walletRef)
                val currentBal = walletSnap.getLong("coinBalance") ?: 0L
                val currentLifetime = walletSnap.getLong("lifetimePurchasedCoins") ?: 0L
                val newBal = currentBal + totalCoins
                val newLifetime = currentLifetime + totalCoins

                // Update wallet
                tx.update(
                    walletRef,
                    mapOf(
                        "coinBalance" to newBal,
                        "lifetimePurchasedCoins" to newLifetime,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                // Update user doc
                val userRef = firestore.collection(USERS_COLLECTION).document(uid)
                tx.update(userRef, "coinBalance", newBal)

                // Update request status
                tx.update(
                    reqRef,
                    mapOf(
                        "status" to "APPROVED",
                        "reviewedBy" to adminDisplayName,
                        "reviewedAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                // Create immutable ledger record
                val txId = "tx_coin_purchase_${UUID.randomUUID().toString().take(12)}"
                val ledger = WalletTransaction(
                    transactionId = txId,
                    uid = uid,
                    publicUserId = publicUserId,
                    type = "COIN_PURCHASE",
                    amount = totalCoins,
                    balanceBefore = currentBal,
                    balanceAfter = newBal,
                    referenceId = paymentRef,
                    status = "SUCCESS",
                    description = "Purchased Package $packageId: %,d Coins (+%,d Bonus)".format(coinAmount, bonusCoins),
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                tx.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), ledger)

                newBal
            }.await()

            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectCoinPurchaseRequest(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        requestId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin privilege required."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))
            firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION).document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "rejectReason" to reason,
                    "reviewedBy" to adminDisplayName,
                    "reviewedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 10. WITHDRAWAL SYSTEM & BALANCE PROTECTION
    // ==========================================

    suspend fun submitWithdrawalRequest(
        uid: String,
        publicUserId: String,
        requestedCoins: Long,
        paymentMethod: String,
        accountNumber: String
    ): Result<WithdrawalRequest> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))
            if (requestedCoins < 50000L) {
                return@withContext Result.failure(IllegalArgumentException("সর্বনিম্ন উত্তোলনের পরিমাণ ৫০,০০০ কয়েন।"))
            }
            val cleanAccount = accountNumber.trim()
            if (cleanAccount.length < 11) {
                return@withContext Result.failure(IllegalArgumentException("সঠিক ১১ ডিজিটের মোবাইল নম্বর দিন (যেমনঃ 017xxxxxxxx)।"))
            }

            // 1. Fetch current wallet balance
            val walletDoc = firestore.collection(WALLETS_COLLECTION).document(uid).get().await()
            val currentCoins = walletDoc.getLong("coinBalance") ?: 0L

            // 2. Fetch pending withdrawals to prevent exceeding total balance
            val pendingDocs = firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION)
                .whereEqualTo("uid", uid)
                .whereIn("status", listOf("PENDING", "PROCESSING", "APPROVED"))
                .get().await()

            var pendingCoinsTotal = 0L
            for (doc in pendingDocs.documents) {
                pendingCoinsTotal += (doc.getLong("requestedCoins") ?: 0L)
            }

            if (currentCoins < (pendingCoinsTotal + requestedCoins)) {
                val available = (currentCoins - pendingCoinsTotal).coerceAtLeast(0L)
                return@withContext Result.failure(IllegalStateException("পর্যাপ্ত কয়েন ব্যালেন্স নেই! আপনার পেন্ডিং রিকোয়েস্ট বাদে উপলব্ধ ব্যালেন্স: %,d কয়েন।".format(available)))
            }

            // 3. Fetch server-authoritative exchange rate
            val configDoc = firestore.collection(SYSTEM_SETTINGS_COLLECTION).document(PAYMENT_CONFIG_DOC).get().await()
            val rate = configDoc.getDouble("coinExchangeRateBDT") ?: 0.0055
            val bdtAmount = requestedCoins * rate

            val requestId = "wdr_${UUID.randomUUID().toString().take(12)}"
            val request = WithdrawalRequest(
                requestId = requestId,
                uid = uid,
                publicUserId = publicUserId,
                requestedCoins = requestedCoins,
                exchangeRate = rate,
                withdrawalAmountBDT = bdtAmount,
                paymentMethod = paymentMethod,
                accountNumber = cleanAccount,
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION).document(requestId).set(request).await()
            Result.success(request)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserWithdrawalRequestsStream(uid: String): Flow<List<WithdrawalRequest>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION)
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot.toObjects(WithdrawalRequest::class.java)
                trySend(list)
            }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    fun getAllWithdrawalRequestsStream(statusFilter: String? = null): Flow<List<WithdrawalRequest>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        var query = firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)

        if (!statusFilter.isNullOrBlank() && statusFilter != "ALL") {
            query = firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION)
                .whereEqualTo("status", statusFilter)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot.toObjects(WithdrawalRequest::class.java)
            trySend(list)
        }

        awaitClose { listener.remove() }
    }.flowOn(Dispatchers.IO)

    suspend fun reviewWithdrawalRequest(
        adminUid: String,
        adminRole: String,
        adminDisplayName: String,
        requestId: String,
        approve: Boolean,
        transactionRef: String? = null,
        rejectReason: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!ModerationRoles.isAdminOrAbove(adminRole)) {
                return@withContext Result.failure(SecurityException("Unauthorized: Admin role required."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))

            if (!approve) {
                // Reject withdrawal request
                firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION).document(requestId).update(
                    mapOf(
                        "status" to "REJECTED",
                        "rejectReason" to (rejectReason ?: "Denied by administrator"),
                        "reviewedBy" to adminDisplayName,
                        "reviewedAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
                return@withContext Result.success(Unit)
            }

            // If approved, debit coins atomically from wallet
            firestore.runTransaction { tx ->
                val reqRef = firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION).document(requestId)
                val reqSnap = tx.get(reqRef)
                if (!reqSnap.exists()) throw IllegalArgumentException("Withdrawal request not found.")
                val status = reqSnap.getString("status") ?: "PENDING"
                if (status == "PAID") throw IllegalStateException("Withdrawal already paid.")

                val uid = reqSnap.getString("uid") ?: throw IllegalStateException("Missing user ID.")
                val publicUserId = reqSnap.getString("publicUserId") ?: ""
                val requestedCoins = reqSnap.getLong("requestedCoins") ?: 0L
                val bdtAmount = reqSnap.getDouble("withdrawalAmountBDT") ?: 0.0

                val walletRef = firestore.collection(WALLETS_COLLECTION).document(uid)
                val walletSnap = tx.get(walletRef)
                val currentCoins = walletSnap.getLong("coinBalance") ?: 0L
                if (currentCoins < requestedCoins) {
                    throw IllegalStateException("User does not have sufficient coins.")
                }

                val newBalance = currentCoins - requestedCoins
                val lifetimeSpent = (walletSnap.getLong("lifetimeSpentCoins") ?: 0L) + requestedCoins

                // Update wallet
                tx.update(
                    walletRef,
                    mapOf(
                        "coinBalance" to newBalance,
                        "lifetimeSpentCoins" to lifetimeSpent,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                // Update user doc
                val userRef = firestore.collection(USERS_COLLECTION).document(uid)
                tx.update(userRef, "coinBalance", newBalance)

                // Update request
                tx.update(
                    reqRef,
                    mapOf(
                        "status" to "PAID",
                        "transactionRef" to (transactionRef ?: "PAID_BY_ADMIN"),
                        "reviewedBy" to adminDisplayName,
                        "reviewedAt" to System.currentTimeMillis(),
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                // Create ledger transaction
                val txId = "tx_withdr_${UUID.randomUUID().toString().take(12)}"
                val ledger = WalletTransaction(
                    transactionId = txId,
                    uid = uid,
                    publicUserId = publicUserId,
                    type = "WITHDRAWAL",
                    amount = requestedCoins,
                    balanceBefore = currentCoins,
                    balanceAfter = newBalance,
                    referenceId = transactionRef ?: requestId,
                    status = "SUCCESS",
                    description = "Withdrawal Paid: ৳%.2f (%,d Coins)".format(bdtAmount, requestedCoins),
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                tx.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), ledger)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==========================================
    // 11. COIN EXCHANGE
    // ==========================================

    suspend fun executeCoinExchangeAtomic(
        uid: String,
        publicUserId: String,
        coinsToExchange: Long
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (coinsToExchange <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("Exchanged coin amount must be greater than zero."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))

            val newBalance = firestore.runTransaction { tx ->
                val walletRef = firestore.collection(WALLETS_COLLECTION).document(uid)
                val walletSnap = tx.get(walletRef)
                val currentCoins = walletSnap.getLong("coinBalance") ?: 0L
                if (currentCoins < coinsToExchange) {
                    throw IllegalStateException("Insufficient coin balance.")
                }

                val newCoins = currentCoins - coinsToExchange
                val diamonds = coinsToExchange // 1:1 or configurable exchange
                tx.update(walletRef, "coinBalance", newCoins, "updatedAt", System.currentTimeMillis())

                val userRef = firestore.collection(USERS_COLLECTION).document(uid)
                tx.update(userRef, "coinBalance", newCoins)

                val txId = "tx_exchange_${UUID.randomUUID().toString().take(12)}"
                val ledger = WalletTransaction(
                    transactionId = txId,
                    uid = uid,
                    publicUserId = publicUserId,
                    type = "COIN_EXCHANGE",
                    amount = coinsToExchange,
                    balanceBefore = currentCoins,
                    balanceAfter = newCoins,
                    referenceId = txId,
                    status = "SUCCESS",
                    description = "Exchanged %,d Coins for %,d Diamonds".format(coinsToExchange, diamonds),
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                tx.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), ledger)

                newCoins
            }.await()

            Result.success(newBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exchangeDiamondsToCoins(
        uid: String,
        publicUserId: String,
        diamondsToExchange: Long
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (diamondsToExchange <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("Diamond amount must be greater than zero."))
            }
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database connection unavailable."))

            val newCoins = firestore.runTransaction { tx ->
                val walletRef = firestore.collection(WALLETS_COLLECTION).document(uid)
                val walletSnap = tx.get(walletRef)
                val currentGiftValue = walletSnap.getLong("lifetimeGiftValue") ?: 0L
                if (currentGiftValue < diamondsToExchange) {
                    throw IllegalStateException("Insufficient diamond balance.")
                }

                val currentCoins = walletSnap.getLong("coinBalance") ?: 0L
                val updatedCoins = currentCoins + diamondsToExchange
                val updatedGifts = currentGiftValue - diamondsToExchange

                tx.update(
                    walletRef,
                    mapOf(
                        "coinBalance" to updatedCoins,
                        "lifetimeGiftValue" to updatedGifts,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )

                val userRef = firestore.collection(USERS_COLLECTION).document(uid)
                tx.update(userRef, "coinBalance", updatedCoins)

                val txId = "tx_ex_d2c_${UUID.randomUUID().toString().take(12)}"
                val ledger = WalletTransaction(
                    transactionId = txId,
                    uid = uid,
                    publicUserId = publicUserId,
                    type = "DIAMOND_EXCHANGE",
                    amount = diamondsToExchange,
                    balanceBefore = currentCoins,
                    balanceAfter = updatedCoins,
                    referenceId = txId,
                    status = "SUCCESS",
                    description = "Exchanged %,d Diamonds to %,d Coins".format(diamondsToExchange, diamondsToExchange),
                    createdAt = System.currentTimeMillis(),
                    serverTimestamp = System.currentTimeMillis()
                )
                tx.set(firestore.collection(TRANSACTIONS_COLLECTION).document(txId), ledger)

                updatedCoins
            }.await()

            Result.success(newCoins)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin stream of all pending coin purchase requests for manual verification.
     */
    fun getPendingCoinPurchaseRequestsStream(): Flow<List<CoinPurchaseRequest>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(CoinPurchaseRequest::class.java) ?: emptyList()
                trySend(list.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Rejects a coin purchase request with mandatory reason and audit record.
     */
    suspend fun rejectCoinPurchaseRequest(
        requestId: String,
        rejectionReason: String,
        adminUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database unavailable"))
            firestore.collection(COIN_PURCHASE_REQUESTS_COLLECTION).document(requestId).update(
                mapOf(
                    "status" to "REJECTED",
                    "rejectionReason" to rejectionReason,
                    "approvedByAdminUid" to adminUid,
                    "approvedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Admin stream of all pending withdrawal requests.
     */
    fun getPendingWithdrawalRequestsStream(): Flow<List<WithdrawalRequest>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = firestore.collection(WITHDRAWAL_REQUESTS_COLLECTION)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(WithdrawalRequest::class.java) ?: emptyList()
                trySend(list.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    /**
     * Admin updates payment system settings (e.g. bKash / Nagad active numbers).
     */
    suspend fun updatePaymentSystemConfig(config: PaymentSystemConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val firestore = getFirestore() ?: return@withContext Result.failure(Exception("Database unavailable"))
            firestore.collection(SYSTEM_SETTINGS_COLLECTION).document(PAYMENT_CONFIG_DOC).set(
                config.copy(updatedAt = System.currentTimeMillis())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
