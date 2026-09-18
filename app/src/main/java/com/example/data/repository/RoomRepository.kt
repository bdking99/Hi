package com.example.data.repository

import com.example.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RoomRepository {
    private val collectionPath = "active_voice_rooms"
    private val usersCollectionPath = "users"
    private val transactionsCollectionPath = "gift_transactions"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Real-time stream of all active voice rooms in Firestore.
     * Automatically seeds sample active rooms if Firestore is empty.
     */
    fun getActiveVoiceRooms(): Flow<List<RoomData>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getFallbackRooms())
            close()
            return@callbackFlow
        }

        try {
            val listenerRegistration = firestore.collection(collectionPath)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(getFallbackRooms())
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val rooms = snapshot.toObjects(RoomData::class.java)
                        trySend(rooms)
                    } else {
                        // Seed initial real-time rooms so users can immediately join and interact
                        seedInitialRooms(firestore)
                        trySend(getFallbackRooms())
                    }
                }

            awaitClose {
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getFallbackRooms())
            close()
        }
    }

    /**
     * Real-time stream of a specific voice room with seat statuses.
     */
    fun getRoomStream(roomId: String): Flow<RoomData?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            val fallback = getFallbackRooms().find { it.id == roomId } ?: getFallbackRooms().first()
            trySend(fallback)
            close()
            return@callbackFlow
        }

        try {
            val listenerRegistration = firestore.collection(collectionPath).document(roomId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        val fallback = getFallbackRooms().find { it.id == roomId }
                        trySend(fallback)
                        return@addSnapshotListener
                    }
                    val room = snapshot.toObject(RoomData::class.java)
                    trySend(room)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = getFallbackRooms().find { it.id == roomId }
            trySend(fallback)
            close()
        }
    }

    /**
     * Create a brand new Voice/Party Room and publish to Firestore.
     */
    suspend fun createVoiceRoom(
        title: String,
        description: String,
        category: String,
        hostUser: FirebaseUserProfile,
        bgGradientStart: String = "#2D124D",
        bgGradientEnd: String = "#1B1035"
    ): Result<String> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database connection not ready"))
            val newRoomId = "room_" + UUID.randomUUID().toString().take(8)

            val initialSeats = mutableListOf<SeatData>()
            // Seat 0 is occupied by the host
            initialSeats.add(
                SeatData(
                    seatIndex = 0,
                    userId = hostUser.userId,
                    userName = hostUser.displayName.ifEmpty { "Host" },
                    userAvatar = hostUser.avatar,
                    isSpeaking = false,
                    isMuted = false,
                    vipLevel = hostUser.vipLevel
                )
            )
            // Remaining 7 seats are free
            for (i in 1..7) {
                initialSeats.add(SeatData(seatIndex = i))
            }

            val newRoom = RoomData(
                id = newRoomId,
                title = title.ifBlank { "VIP Voice Party" },
                description = description.ifBlank { "Welcome to our lively voice room!" },
                hostId = hostUser.userId,
                hostName = hostUser.displayName.ifEmpty { "Host" },
                hostAvatar = hostUser.avatar,
                agencyName = "Diamond Club",
                hostLevel = hostUser.level,
                viewerCount = "1",
                countryFlag = "🇺🇸",
                tag = category.ifBlank { "Party" },
                bgGradientStart = bgGradientStart,
                bgGradientEnd = bgGradientEnd,
                seats = initialSeats,
                activeUserIds = listOf(hostUser.userId),
                createdAt = System.currentTimeMillis()
            )

            firestore.collection(collectionPath).document(newRoomId).set(newRoom).await()

            // Also post welcome system message
            sendChatMessage(
                newRoomId,
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    roomId = newRoomId,
                    senderName = "System",
                    text = "🎉 Welcome to $title! Be respectful and enjoy the party.",
                    isSystem = true
                )
            )

            Result.success(newRoomId)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * User takes a seat on the stage in real-time.
     */
    suspend fun takeSeat(roomId: String, seatIndex: Int, user: FirebaseUserProfile): Result<Unit> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            val roomRef = firestore.collection(collectionPath).document(roomId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val room = snapshot.toObject(RoomData::class.java) ?: throw Exception("Room not found")

                val updatedSeats = room.seats.toMutableList()
                if (seatIndex in updatedSeats.indices) {
                    val currentSeat = updatedSeats[seatIndex]
                    if (currentSeat.userId != null && currentSeat.userId != user.userId) {
                        throw Exception("Seat is already taken!")
                    }
                    updatedSeats[seatIndex] = SeatData(
                        seatIndex = seatIndex,
                        userId = user.userId,
                        userName = user.displayName,
                        userAvatar = user.avatar,
                        isSpeaking = false,
                        isMuted = false,
                        vipLevel = user.vipLevel
                    )
                }

                transaction.update(roomRef, "seats", updatedSeats)
            }.await()

            sendChatMessage(
                roomId,
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    roomId = roomId,
                    senderName = "System",
                    text = "🎤 ${user.displayName} took Seat ${seatIndex + 1}",
                    isSystem = true
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * User leaves their seat on the stage.
     */
    suspend fun leaveSeat(roomId: String, seatIndex: Int, userId: String): Result<Unit> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            val roomRef = firestore.collection(collectionPath).document(roomId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val room = snapshot.toObject(RoomData::class.java) ?: throw Exception("Room not found")

                val updatedSeats = room.seats.toMutableList()
                if (seatIndex in updatedSeats.indices) {
                    val currentSeat = updatedSeats[seatIndex]
                    if (currentSeat.userId == userId || currentSeat.userId == null) {
                        updatedSeats[seatIndex] = SeatData(seatIndex = seatIndex)
                    }
                }

                transaction.update(roomRef, "seats", updatedSeats)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Speaker toggles their microphone Mute/Unmute state.
     */
    suspend fun toggleMicMute(roomId: String, seatIndex: Int, isMuted: Boolean): Result<Unit> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            val roomRef = firestore.collection(collectionPath).document(roomId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val room = snapshot.toObject(RoomData::class.java) ?: throw Exception("Room not found")

                val updatedSeats = room.seats.toMutableList()
                if (seatIndex in updatedSeats.indices) {
                    val currentSeat = updatedSeats[seatIndex]
                    updatedSeats[seatIndex] = currentSeat.copy(isMuted = isMuted, isSpeaking = if (isMuted) false else currentSeat.isSpeaking)
                }

                transaction.update(roomRef, "seats", updatedSeats)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Room Host/Admin controls: Mute another speaker or kick them off their seat.
     */
    suspend fun hostControlSeat(roomId: String, seatIndex: Int, kick: Boolean, mute: Boolean): Result<Unit> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            val roomRef = firestore.collection(collectionPath).document(roomId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(roomRef)
                val room = snapshot.toObject(RoomData::class.java) ?: throw Exception("Room not found")

                val updatedSeats = room.seats.toMutableList()
                if (seatIndex in updatedSeats.indices) {
                    val currentSeat = updatedSeats[seatIndex]
                    if (kick) {
                        updatedSeats[seatIndex] = SeatData(seatIndex = seatIndex)
                    } else if (mute) {
                        updatedSeats[seatIndex] = currentSeat.copy(isMuted = true, isSpeaking = false)
                    }
                }

                transaction.update(roomRef, "seats", updatedSeats)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of room chat comments.
     */
    fun getChatMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getFallbackChat(roomId))
            close()
            return@callbackFlow
        }

        try {
            val listenerRegistration = firestore.collection(collectionPath).document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(getFallbackChat(roomId))
                        return@addSnapshotListener
                    }
                    val msgs = snapshot.toObjects(ChatMessage::class.java)
                    trySend(if (msgs.isEmpty()) getFallbackChat(roomId) else msgs)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getFallbackChat(roomId))
            close()
        }
    }

    /**
     * Send a real-time message to room chat.
     */
    suspend fun sendChatMessage(roomId: String, message: ChatMessage): Result<Unit> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            val msgId = message.id.ifEmpty { UUID.randomUUID().toString() }
            val finalMsg = message.copy(id = msgId, roomId = roomId, timestamp = System.currentTimeMillis())
            firestore.collection(collectionPath).document(roomId)
                .collection("messages").document(msgId).set(finalMsg).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of latest gift sent in the room to trigger visual animations.
     */
    fun getLatestRoomGiftStream(roomId: String): Flow<GiftTransaction?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(collectionPath).document(roomId)
                .collection("gifts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || snapshot.isEmpty) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val latest = snapshot.toObjects(GiftTransaction::class.java).firstOrNull()
                    trySend(latest)
                }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
            close()
        }
    }

    /**
     * Real-Time Gift Transaction:
     * - Atomic Firestore transaction verifies sender balance >= gift.costCoins.
     * - Prevents duplicate deductions or self-gifting.
     * - Deducts coins from sender, increments receiver's receivedDiamonds & giftsReceivedCount.
     * - Logs gift transaction for both room animation and user showcase.
     */
    suspend fun sendGift(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatar: String,
        receiverId: String,
        receiverName: String,
        gift: GiftItem
    ): Result<GiftTransaction> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            
            if (senderId == receiverId) {
                return Result.failure(Exception("You cannot send gifts to yourself!"))
            }

            val senderRef = firestore.collection(usersCollectionPath).document(senderId)
            val receiverRef = firestore.collection(usersCollectionPath).document(receiverId)
            val txId = "tx_gift_" + UUID.randomUUID().toString()

            val giftTx = GiftTransaction(
                id = txId,
                roomId = roomId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                receiverId = receiverId,
                receiverName = receiverName,
                giftId = gift.id,
                giftName = gift.name,
                giftEmoji = gift.emoji,
                coins = gift.costCoins,
                timestamp = System.currentTimeMillis()
            )

            firestore.runTransaction { tx ->
                val senderSnap = tx.get(senderRef)
                val currentCoins = senderSnap.getLong("coinBalance") ?: 2500L

                if (currentCoins < gift.costCoins) {
                    throw Exception("Insufficient coin balance! Needs ${gift.costCoins} coins (You have $currentCoins).")
                }

                // Deduct from sender
                tx.update(senderRef, "coinBalance", currentCoins - gift.costCoins)

                // Add to receiver
                val receiverSnap = tx.get(receiverRef)
                val currentDiamonds = receiverSnap.getLong("receivedDiamonds") ?: 0L
                val currentGiftsCount = receiverSnap.getLong("giftsReceivedCount") ?: 0L
                tx.update(receiverRef, "receivedDiamonds", currentDiamonds + gift.costCoins)
                tx.update(receiverRef, "giftsReceivedCount", currentGiftsCount + 1)

                // Add transaction record
                val roomGiftsRef = firestore.collection(collectionPath).document(roomId).collection("gifts").document(txId)
                tx.set(roomGiftsRef, giftTx)

                val globalGiftsRef = firestore.collection(transactionsCollectionPath).document(txId)
                tx.set(globalGiftsRef, giftTx)

                // Log sender transaction
                val senderTxRef = firestore.collection(usersCollectionPath).document(senderId).collection("transactions").document(txId)
                val senderTx = CoinTransactionItem(
                    id = txId,
                    userId = senderId,
                    type = "GIFT_SENT",
                    amount = -gift.costCoins,
                    title = "Sent ${gift.emoji} ${gift.name}",
                    description = "To $receiverName in room",
                    timestamp = System.currentTimeMillis()
                )
                tx.set(senderTxRef, senderTx)

                // Log receiver transaction
                val receiverTxRef = firestore.collection(usersCollectionPath).document(receiverId).collection("transactions").document(txId)
                val receiverTx = CoinTransactionItem(
                    id = txId,
                    userId = receiverId,
                    type = "GIFT_RECEIVED",
                    amount = gift.costCoins,
                    title = "Received ${gift.emoji} ${gift.name}",
                    description = "From $senderName",
                    timestamp = System.currentTimeMillis()
                )
                tx.set(receiverTxRef, receiverTx)
            }.await()

            // Announce in room chat
            sendChatMessage(
                roomId,
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    roomId = roomId,
                    senderName = "System",
                    text = "🎁 $senderName gifted ${gift.emoji} ${gift.name} to $receiverName!",
                    isSystem = true
                )
            )

            Result.success(giftTx)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of user profile by User ID (for profile screen or profile visit).
     */
    fun getUserProfileStream(userId: String): Flow<FirebaseUserProfile?> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getFallbackUserProfile(userId))
            close()
            return@callbackFlow
        }

        try {
            val userRef = firestore.collection(usersCollectionPath).document(userId)
            val listener = userRef.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    val fallback = getFallbackUserProfile(userId)
                    trySend(fallback)
                    // If doesn't exist, initialize in Firestore
                    userRef.set(fallback, SetOptions.merge())
                    return@addSnapshotListener
                }
                val profile = snapshot.toObject(FirebaseUserProfile::class.java)
                trySend(profile)
            }

            awaitClose {
                listener.remove()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(getFallbackUserProfile(userId))
            close()
        }
    }

    /**
     * Fetch user profile once (e.g. for clicking a seat to view public profile).
     */
    suspend fun getUserProfileOnce(userId: String): FirebaseUserProfile {
        return try {
            val firestore = getFirestore() ?: return getFallbackUserProfile(userId)
            val doc = firestore.collection(usersCollectionPath).document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(FirebaseUserProfile::class.java) ?: getFallbackUserProfile(userId)
            } else {
                val fallback = getFallbackUserProfile(userId)
                firestore.collection(usersCollectionPath).document(userId).set(fallback).await()
                fallback
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackUserProfile(userId)
        }
    }

    /**
     * Update user profile details in Firestore.
     */
    suspend fun updateUserProfile(profile: FirebaseUserProfile): Result<Unit> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            firestore.collection(usersCollectionPath).document(profile.userId).set(profile, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Instant Coin Recharge with transaction record.
     */
    suspend fun rechargeCoins(userId: String, coins: Long, packTitle: String): Result<Long> {
        return try {
            val firestore = getFirestore() ?: return Result.failure(Exception("Database unavailable"))
            val userRef = firestore.collection(usersCollectionPath).document(userId)
            val txId = "tx_rec_" + UUID.randomUUID().toString()

            var newBalance = 0L
            firestore.runTransaction { tx ->
                val snap = tx.get(userRef)
                val current = snap.getLong("coinBalance") ?: 2500L
                newBalance = current + coins
                tx.update(userRef, "coinBalance", newBalance)

                val txItem = CoinTransactionItem(
                    id = txId,
                    userId = userId,
                    type = "RECHARGE",
                    amount = coins,
                    title = "Recharged $packTitle",
                    description = "+$coins Coins added to wallet",
                    timestamp = System.currentTimeMillis()
                )
                val txRef = userRef.collection("transactions").document(txId)
                tx.set(txRef, txItem)
            }.await()

            Result.success(newBalance)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Stream user coin transaction history.
     */
    fun getUserTransactions(userId: String): Flow<List<CoinTransactionItem>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(usersCollectionPath).document(userId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.toObjects(CoinTransactionItem::class.java)
                    trySend(items)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }

    /**
     * Stream gifts received by user for the Gift Showcase.
     */
    fun getUserReceivedGifts(userId: String): Flow<List<GiftTransaction>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        try {
            val listener = firestore.collection(transactionsCollectionPath)
                .whereEqualTo("receiverId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot.toObjects(GiftTransaction::class.java)
                    trySend(items)
                }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(emptyList())
            close()
        }
    }

    // ----------------------------------------------------
    // Fallback & Seed Data Helpers
    // ----------------------------------------------------
    private fun seedInitialRooms(firestore: FirebaseFirestore) {
        try {
            val batch = firestore.batch()
            for (room in getFallbackRooms()) {
                val doc = firestore.collection(collectionPath).document(room.id)
                batch.set(doc, room)
            }
            batch.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFallbackRooms(): List<RoomData> {
        val room1Seats = listOf(
            SeatData(0, "ID_HOST_1001", "Aria Star", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", isSpeaking = true, isMuted = false, vipLevel = 3),
            SeatData(1, "ID_SOPHIA_2002", "Sophia", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", isSpeaking = false, isMuted = false, vipLevel = 1),
            SeatData(2, "ID_LEO_3003", "Leo Cruz", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", isSpeaking = true, isMuted = false, vipLevel = 2),
            SeatData(3),
            SeatData(4, "ID_ELENA_4004", "Elena Joy", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150", isSpeaking = false, isMuted = true, vipLevel = 1),
            SeatData(5),
            SeatData(6),
            SeatData(7)
        )

        val room2Seats = listOf(
            SeatData(0, "ID_HOST_1002", "DJ Mike", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", isSpeaking = true, isMuted = false, vipLevel = 4),
            SeatData(1, "ID_CHLOE_2005", "Chloe", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", isSpeaking = false, isMuted = false, vipLevel = 2),
            SeatData(2),
            SeatData(3),
            SeatData(4),
            SeatData(5),
            SeatData(6),
            SeatData(7)
        )

        return listOf(
            RoomData(
                id = "room_acoustic_night",
                title = "🎸 Acoustic Vibes & Late Chat",
                description = "Singing acoustic covers and having chill late night conversations. Grab a seat!",
                hostId = "ID_HOST_1001",
                hostName = "Aria Star",
                hostAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                agencyName = "Diamond VIP Agency",
                hostLevel = 28,
                viewerCount = "1.8k",
                countryFlag = "🇺🇸",
                tag = "Music",
                bgGradientStart = "#3C1053",
                bgGradientEnd = "#AD5389",
                seats = room1Seats,
                createdAt = System.currentTimeMillis() - 3600000
            ),
            RoomData(
                id = "room_electronic_party",
                title = "⚡ EDM Beats & Club Chillout",
                description = "Live DJ mixes, gift wars, and party games!",
                hostId = "ID_HOST_1002",
                hostName = "DJ Mike",
                hostAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                agencyName = "Royal Stars",
                hostLevel = 35,
                viewerCount = "3.2k",
                countryFlag = "🇬🇧",
                tag = "Party",
                bgGradientStart = "#0F2027",
                bgGradientEnd = "#2C5364",
                seats = room2Seats,
                createdAt = System.currentTimeMillis() - 1800000
            ),
            RoomData(
                id = "room_global_friends",
                title = "🌍 Global Friends & Language Exchange",
                description = "Meet people from across the globe, practice languages, and relax.",
                hostId = "ID_HOST_1003",
                hostName = "Kenji Tokyo",
                hostAvatar = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150",
                agencyName = "Global Nexus",
                hostLevel = 19,
                viewerCount = "920",
                countryFlag = "🇯🇵",
                tag = "Chat",
                bgGradientStart = "#134E5E",
                bgGradientEnd = "#71B280",
                seats = List(8) { if (it == 0) SeatData(0, "ID_HOST_1003", "Kenji Tokyo", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150", isSpeaking = true) else SeatData(it) },
                createdAt = System.currentTimeMillis() - 7200000
            )
        )
    }

    private fun getFallbackChat(roomId: String): List<ChatMessage> {
        return listOf(
            ChatMessage(id = "msg_1", roomId = roomId, senderName = "System", text = "✨ Welcome to the Voice Party! Respect each other and have fun.", isSystem = true),
            ChatMessage(id = "msg_2", roomId = roomId, senderName = "Sophia", text = "Hey Aria, wonderful voice today! 💖"),
            ChatMessage(id = "msg_3", roomId = roomId, senderName = "Leo", text = "Sent 5 Roses 🌹"),
            ChatMessage(id = "msg_4", roomId = roomId, senderName = "Alex", text = "Great vibes in this club 🔥")
        )
    }

    private fun getFallbackUserProfile(userId: String): FirebaseUserProfile {
        return when (userId) {
            "ID_HOST_1001" -> FirebaseUserProfile(
                userId = "ID_HOST_1001",
                publicUserId = "88992211",
                displayName = "Aria Star",
                username = "aria_star",
                avatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                bio = "Singer, songwriter & party host! Acoustic nights every evening. 🎸🎶",
                level = 28,
                vipLevel = 3,
                svipLevel = 1,
                coinBalance = 45000L,
                receivedDiamonds = 89000L,
                giftsReceivedCount = 142,
                followersCount = 18400,
                followingCount = 120
            )
            "ID_SOPHIA_2002" -> FirebaseUserProfile(
                userId = "ID_SOPHIA_2002",
                publicUserId = "77334411",
                displayName = "Sophia",
                username = "sophia_sweet",
                avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                coverImage = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=800",
                bio = "Coffee lover ☕ Fashion & lifestyle blogger ✨",
                level = 15,
                vipLevel = 1,
                coinBalance = 12000L,
                receivedDiamonds = 24000L,
                giftsReceivedCount = 68,
                followersCount = 5600,
                followingCount = 210
            )
            "ID_LEO_3003" -> FirebaseUserProfile(
                userId = "ID_LEO_3003",
                publicUserId = "66221199",
                displayName = "Leo Cruz",
                username = "leo_cruz",
                avatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                bio = "Gamer, tech enthusiast & voice chatter 🎮",
                level = 20,
                vipLevel = 2,
                coinBalance = 8000L,
                receivedDiamonds = 35000L,
                giftsReceivedCount = 89,
                followersCount = 9200,
                followingCount = 450
            )
            else -> FirebaseUserProfile(
                userId = userId,
                publicUserId = "9988" + userId.takeLast(4).filter { it.isDigit() }.padStart(4, '7'),
                displayName = "Star Member",
                username = "user_" + userId.take(6),
                avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                coverImage = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                bio = "Voice room explorer and party enthusiast! 🎙️✨",
                level = 5,
                vipLevel = 1,
                coinBalance = 3500L,
                receivedDiamonds = 5000L,
                giftsReceivedCount = 12,
                followersCount = 320,
                followingCount = 45
            )
        }
    }
}
