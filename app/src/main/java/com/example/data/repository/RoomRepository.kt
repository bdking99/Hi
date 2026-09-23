package com.example.data.repository

import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RoomRepository {
    private val collectionPath = "active_voice_rooms"
    private val roomsCollectionPath = "rooms"
    private val usersCollectionPath = "users"
    private val transactionsCollectionPath = "gift_transactions"

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // In-memory reactive state cache for offline resilience and immediate UI updates
    private val _roomsState = MutableStateFlow<List<RoomData>>(emptyList())
    private val _roomDetailMap = ConcurrentHashMap<String, MutableStateFlow<RoomData?>>()
    private val _roomChatMap = ConcurrentHashMap<String, MutableStateFlow<List<ChatMessage>>>()
    private val _roomGiftMap = ConcurrentHashMap<String, MutableStateFlow<GiftTransaction?>>()
    private val _roomMembersMap = ConcurrentHashMap<String, MutableStateFlow<List<RoomMember>>>()
    private val _speakerRequestsMap = ConcurrentHashMap<String, MutableStateFlow<List<SpeakerRequest>>>()
    private val _roomSignalsMap = ConcurrentHashMap<String, MutableStateFlow<RoomRtcSignal?>>()
    private val _userProfilesMap = ConcurrentHashMap<String, MutableStateFlow<FirebaseUserProfile?>>()
    private val _userTransactionsMap = ConcurrentHashMap<String, MutableStateFlow<List<CoinTransactionItem>>>()
    private val _userReceivedGiftsMap = ConcurrentHashMap<String, MutableStateFlow<List<GiftTransaction>>>()

    init {
        // Initialize default seed rooms in-memory
        val initialRooms = getFallbackRooms()
        _roomsState.value = initialRooms
        for (room in initialRooms) {
            _roomDetailMap[room.id] = MutableStateFlow(room)
            _roomChatMap[room.id] = MutableStateFlow(getFallbackChat(room.id))
            _roomGiftMap[room.id] = MutableStateFlow(null)
            _roomMembersMap[room.id] = MutableStateFlow(
                listOf(
                    RoomMember(
                        uid = room.hostId,
                        roomId = room.id,
                        displayName = room.hostName,
                        avatarUrl = room.hostAvatar,
                        role = "HOST",
                        seatIndex = 0
                    )
                )
            )
            _speakerRequestsMap[room.id] = MutableStateFlow(emptyList())
            _roomSignalsMap[room.id] = MutableStateFlow(null)
        }

        // Initialize sample user profiles
        val initialProfiles = listOf(
            getFallbackUserProfile("ID_HOST_1001"),
            getFallbackUserProfile("ID_SOPHIA_2002"),
            getFallbackUserProfile("ID_LEO_3003"),
            getFallbackUserProfile("ID_ELENA_4004"),
            getFallbackUserProfile("USER_ME_CURRENT")
        )
        for (p in initialProfiles) {
            _userProfilesMap[p.userId] = MutableStateFlow(p)
        }

        // Try anonymous sign-in to satisfy Firestore security rules
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen to Firestore if available
        listenToFirestoreRooms()
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun listenToFirestoreRooms() {
        val firestore = getFirestore() ?: return
        try {
            // Listen to rooms collection
            firestore.collection(roomsCollectionPath)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val firestoreRooms = snapshot.toObjects(RoomData::class.java)
                        if (firestoreRooms.isNotEmpty()) {
                            _roomsState.value = firestoreRooms
                            for (r in firestoreRooms) {
                                val flow = _roomDetailMap.getOrPut(r.id) { MutableStateFlow(r) }
                                flow.value = r
                            }
                        }
                    } else {
                        // Fallback to active_voice_rooms
                        listenToLegacyRooms(firestore)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listenToLegacyRooms(firestore: FirebaseFirestore) {
        try {
            firestore.collection(collectionPath)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        val firestoreRooms = snapshot.toObjects(RoomData::class.java)
                        if (firestoreRooms.isNotEmpty()) {
                            _roomsState.value = firestoreRooms
                            for (r in firestoreRooms) {
                                val flow = _roomDetailMap.getOrPut(r.id) { MutableStateFlow(r) }
                                flow.value = r
                            }
                        }
                    } else if (snapshot == null || snapshot.isEmpty) {
                        seedInitialRooms(firestore)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generate a unique 6-digit numeric Room ID (e.g. 739281).
     */
    private suspend fun generateUniqueNumericRoomId(firestore: FirebaseFirestore?): String {
        val random = java.util.Random()
        if (firestore == null) {
            return (100000 + random.nextInt(900000)).toString()
        }
        for (attempt in 0..5) {
            val candidate = (100000 + random.nextInt(900000)).toString()
            try {
                val doc = firestore.collection("room_ids").document(candidate).get().await()
                if (!doc.exists()) {
                    firestore.collection("room_ids").document(candidate).set(mapOf("createdAt" to System.currentTimeMillis())).await()
                    return candidate
                }
            } catch (e: Exception) {
                return candidate
            }
        }
        return (100000 + random.nextInt(900000)).toString()
    }

    /**
     * Real-time stream of all active voice rooms.
     */
    fun getActiveVoiceRooms(): Flow<List<RoomData>> = _roomsState.asStateFlow()

    /**
     * Real-time stream of a specific voice room with seat statuses.
     */
    fun getRoomStream(roomId: String): Flow<RoomData?> {
        val flow = _roomDetailMap.getOrPut(roomId) {
            val fallback = _roomsState.value.find { it.id == roomId } ?: getFallbackRooms().first()
            MutableStateFlow(fallback)
        }
        // Listen to Firestore document updates in background
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestore.collection(roomsCollectionPath).document(roomId)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val remoteRoom = snapshot.toObject(RoomData::class.java)
                            if (remoteRoom != null) {
                                flow.value = remoteRoom
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return flow.asStateFlow()
    }

    /**
     * Create a brand new Voice/Party Room with unique numeric ID, cover, and subcollections.
     */
    suspend fun createVoiceRoom(
        title: String,
        description: String,
        category: String,
        hostUser: FirebaseUserProfile,
        coverUrl: String = "",
        password: String = "",
        roomType: RoomType = RoomType.PUBLIC,
        bgGradientStart: String = "#2D124D",
        bgGradientEnd: String = "#1B1035"
    ): Result<String> {
        val firestore = getFirestore()
        val numericId = generateUniqueNumericRoomId(firestore)
        val newRoomId = "room_$numericId"

        val initialSeats = mutableListOf<SeatData>()
        initialSeats.add(
            SeatData(
                seatIndex = 0,
                userId = hostUser.userId,
                userName = hostUser.displayName.ifEmpty { "Host" },
                userAvatar = hostUser.avatar,
                isSpeaking = false,
                isMuted = false,
                vipLevel = hostUser.vipLevel,
                isHost = true
            )
        )
        for (i in 1..9) {
            initialSeats.add(SeatData(seatIndex = i))
        }

        val newRoom = RoomData(
            id = newRoomId,
            numericId = numericId,
            title = title.ifBlank { "Voice Room #$numericId" },
            description = description.ifBlank { "Welcome to our lively voice room!" },
            coverUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800" },
            hostId = hostUser.userId,
            hostName = hostUser.displayName.ifEmpty { "Host" },
            hostAvatar = hostUser.avatar,
            agencyName = "Diamond Club",
            hostLevel = hostUser.level,
            roomType = roomType,
            password = password,
            viewerCount = "1",
            countryFlag = "🌐",
            tag = category.ifBlank { "Party" },
            bgGradientStart = bgGradientStart,
            bgGradientEnd = bgGradientEnd,
            seats = initialSeats,
            activeUserIds = listOf(hostUser.userId),
            createdAt = System.currentTimeMillis()
        )

        // Update local in-memory state
        val updatedList = listOf(newRoom) + _roomsState.value
        _roomsState.value = updatedList
        _roomDetailMap[newRoomId] = MutableStateFlow(newRoom)
        _roomChatMap[newRoomId] = MutableStateFlow(
            listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    roomId = newRoomId,
                    senderName = "System",
                    text = "🎉 Welcome to ${newRoom.title}! Room ID: $numericId. Enjoy chatting!",
                    isSystem = true
                )
            )
        )
        _roomGiftMap[newRoomId] = MutableStateFlow(null)

        // Set initial host member
        val hostMember = RoomMember(
            uid = hostUser.userId,
            roomId = newRoomId,
            displayName = hostUser.displayName,
            avatarUrl = hostUser.avatar,
            role = "HOST",
            seatIndex = 0
        )
        _roomMembersMap[newRoomId] = MutableStateFlow(listOf(hostMember))
        _speakerRequestsMap[newRoomId] = MutableStateFlow(emptyList())

        // Try syncing to Firestore in background across rooms & active_voice_rooms & members
        repositoryScope.launch {
            try {
                if (firestore != null) {
                    firestore.collection(roomsCollectionPath).document(newRoomId).set(newRoom).await()
                    firestore.collection(collectionPath).document(newRoomId).set(newRoom).await()
                    firestore.collection(roomsCollectionPath).document(newRoomId)
                        .collection("members").document(hostUser.userId).set(hostMember).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(newRoomId)
    }

    /**
     * Delete a voice room by ID.
     */
    suspend fun deleteRoom(roomId: String): Result<Unit> {
        val currentList = _roomsState.value.filter { it.id != roomId }
        _roomsState.value = currentList
        _roomDetailMap.remove(roomId)
        _roomChatMap.remove(roomId)
        _roomGiftMap.remove(roomId)
        _roomMembersMap.remove(roomId)
        _speakerRequestsMap.remove(roomId)

        repositoryScope.launch {
            try {
                val firestore = getFirestore()
                firestore?.collection(roomsCollectionPath)?.document(roomId)?.delete()?.await()
                firestore?.collection(collectionPath)?.document(roomId)?.delete()?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Clear / reset rooms.
     */
    suspend fun clearAllRooms(): Result<Unit> {
        _roomsState.value = emptyList()
        _roomDetailMap.clear()
        _roomChatMap.clear()
        _roomGiftMap.clear()
        _roomMembersMap.clear()
        _speakerRequestsMap.clear()
        return Result.success(Unit)
    }

    /**
     * Join room as a member (adds to rooms/{roomId}/members/{uid}).
     */
    suspend fun joinRoomMember(roomId: String, user: FirebaseUserProfile, role: String = "AUDIENCE"): Result<Unit> {
        val member = RoomMember(
            uid = user.userId,
            roomId = roomId,
            displayName = user.displayName,
            avatarUrl = user.avatar,
            role = role,
            joinedAt = System.currentTimeMillis()
        )
        val currentList = _roomMembersMap.getOrPut(roomId) { MutableStateFlow(emptyList()) }.value
        val updated = currentList.filter { it.uid != user.userId } + member
        _roomMembersMap[roomId]?.value = updated

        // Update activeUserIds on room
        val room = _roomDetailMap[roomId]?.value
        if (room != null && !room.activeUserIds.contains(user.userId)) {
            val updatedUsers = room.activeUserIds + user.userId
            val updatedRoom = room.copy(
                activeUserIds = updatedUsers,
                viewerCount = "${updatedUsers.size}"
            )
            _roomDetailMap[roomId]?.value = updatedRoom
        }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("members").document(user.userId).set(member).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Leave room member.
     */
    suspend fun leaveRoomMember(roomId: String, userId: String): Result<Unit> {
        val currentList = _roomMembersMap[roomId]?.value ?: emptyList()
        val leavingMember = currentList.find { it.uid == userId }
        _roomMembersMap[roomId]?.value = currentList.filter { it.uid != userId }

        // If leaving user was seated, free the seat automatically
        val room = _roomDetailMap[roomId]?.value
        val seatedIndex = room?.seats?.indexOfFirst { it.userId == userId } ?: -1
        if (seatedIndex != -1 || leavingMember?.seatIndex != null) {
            leaveSeat(roomId, if (seatedIndex != -1) seatedIndex else leavingMember?.seatIndex ?: 0, userId)
        }

        // Update activeUserIds
        if (room != null) {
            val updatedUsers = room.activeUserIds.filter { it != userId }
            val updatedRoom = room.copy(
                activeUserIds = updatedUsers,
                viewerCount = "${maxOf(1, updatedUsers.size)}"
            )
            _roomDetailMap[roomId]?.value = updatedRoom
        }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("members").document(userId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Real-time stream of room members.
     */
    fun getRoomMembers(roomId: String): Flow<List<RoomMember>> {
        val flow = _roomMembersMap.getOrPut(roomId) { MutableStateFlow(emptyList()) }
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("members")
                    .addSnapshotListener { snap, err ->
                        if (err == null && snap != null) {
                            val members = snap.toObjects(RoomMember::class.java)
                            flow.value = members
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return flow.asStateFlow()
    }

    /**
     * User takes a seat on the stage in real-time.
     * Enforces: One user -> One seat, One seat -> One user.
     * Moving to a new seat automatically frees any previous seat.
     */
    suspend fun takeSeat(roomId: String, seatIndex: Int, user: FirebaseUserProfile): Result<Unit> {
        val currentRoom = _roomDetailMap[roomId]?.value ?: _roomsState.value.find { it.id == roomId }
        if (currentRoom != null) {
            val updatedSeats = currentRoom.seats.toMutableList()
            if (seatIndex in updatedSeats.indices) {
                // 1. If seat is locked, reject
                if (updatedSeats[seatIndex].isLocked) {
                    return Result.failure(Exception("Seat is locked by the host."))
                }
                // 2. If seat is occupied by another user, reject
                val targetOccupant = updatedSeats[seatIndex].userId
                if (targetOccupant != null && targetOccupant != user.userId) {
                    return Result.failure(Exception("Seat ${seatIndex + 1} is already occupied."))
                }

                // 3. Auto-release any previous seat occupied by this user
                for (i in updatedSeats.indices) {
                    if (i != seatIndex && updatedSeats[i].userId == user.userId) {
                        updatedSeats[i] = SeatData(seatIndex = i)
                    }
                }

                val isHost = currentRoom.hostId == user.userId
                val isCoHost = currentRoom.coHostIds.contains(user.userId)
                updatedSeats[seatIndex] = SeatData(
                    seatIndex = seatIndex,
                    userId = user.userId,
                    userName = user.displayName,
                    userAvatar = user.avatar,
                    isSpeaking = false,
                    isMuted = false,
                    vipLevel = user.vipLevel,
                    isHost = isHost,
                    isCoHost = isCoHost
                )
            }
            val updatedRoom = currentRoom.copy(seats = updatedSeats)
            _roomDetailMap.getOrPut(roomId) { MutableStateFlow(updatedRoom) }.value = updatedRoom
            _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }

            // Update member role in room members map
            val members = _roomMembersMap[roomId]?.value ?: emptyList()
            val updatedMembers = members.map { m ->
                if (m.uid == user.userId) m.copy(role = if (m.role == "HOST") "HOST" else if (m.role == "CO_HOST") "CO_HOST" else "SPEAKER", seatIndex = seatIndex) else m
            }
            _roomMembersMap[roomId]?.value = updatedMembers

            // Add system chat
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
        }

        // Sync to Firestore across both collections atomically
        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                val paths = listOf(roomsCollectionPath, collectionPath)
                for (path in paths) {
                    val roomRef = firestore.collection(path).document(roomId)
                    firestore.runTransaction { tx ->
                        val snap = tx.get(roomRef)
                        val remoteRoom = snap.toObject(RoomData::class.java)
                        if (remoteRoom != null) {
                            val remoteSeats = remoteRoom.seats.toMutableList()
                            if (seatIndex in remoteSeats.indices) {
                                val target = remoteSeats[seatIndex]
                                if (target.isLocked) {
                                    return@runTransaction
                                }
                                if (target.userId != null && target.userId != user.userId) {
                                    return@runTransaction
                                }

                                // Auto-release any previous seat occupied by this user in remote state
                                for (i in remoteSeats.indices) {
                                    if (i != seatIndex && remoteSeats[i].userId == user.userId) {
                                        remoteSeats[i] = SeatData(seatIndex = i)
                                    }
                                }

                                val isHost = remoteRoom.hostId == user.userId
                                val isCoHost = remoteRoom.coHostIds.contains(user.userId)
                                remoteSeats[seatIndex] = SeatData(
                                    seatIndex = seatIndex,
                                    userId = user.userId,
                                    userName = user.displayName,
                                    userAvatar = user.avatar,
                                    isSpeaking = false,
                                    isMuted = false,
                                    vipLevel = user.vipLevel,
                                    isHost = isHost,
                                    isCoHost = isCoHost
                                )
                                tx.update(roomRef, "seats", remoteSeats)
                            }
                        }
                    }.await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(Unit)
    }

    /**
     * User leaves their seat on the stage.
     * Automatically frees any seat occupied by userId in this room.
     */
    suspend fun leaveSeat(roomId: String, seatIndex: Int, userId: String): Result<Unit> {
        val currentRoom = _roomDetailMap[roomId]?.value ?: _roomsState.value.find { it.id == roomId }
        if (currentRoom != null) {
            val updatedSeats = currentRoom.seats.toMutableList()
            for (i in updatedSeats.indices) {
                if (i == seatIndex || updatedSeats[i].userId == userId) {
                    updatedSeats[i] = SeatData(seatIndex = i)
                }
            }
            val updatedRoom = currentRoom.copy(seats = updatedSeats)
            _roomDetailMap.getOrPut(roomId) { MutableStateFlow(updatedRoom) }.value = updatedRoom
            _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }

            // Update member role
            val members = _roomMembersMap[roomId]?.value ?: emptyList()
            val updatedMembers = members.map { m ->
                if (m.uid == userId) m.copy(role = if (m.role == "HOST") "HOST" else if (m.role == "CO_HOST") "CO_HOST" else "AUDIENCE", seatIndex = null) else m
            }
            _roomMembersMap[roomId]?.value = updatedMembers
        }

        // Sync to Firestore in background
        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                val paths = listOf(roomsCollectionPath, collectionPath)
                for (path in paths) {
                    val roomRef = firestore.collection(path).document(roomId)
                    firestore.runTransaction { tx ->
                        val snap = tx.get(roomRef)
                        val remoteRoom = snap.toObject(RoomData::class.java)
                        if (remoteRoom != null) {
                            val remoteSeats = remoteRoom.seats.toMutableList()
                            var changed = false
                            for (i in remoteSeats.indices) {
                                if (i == seatIndex || remoteSeats[i].userId == userId) {
                                    remoteSeats[i] = SeatData(seatIndex = i)
                                    changed = true
                                }
                            }
                            if (changed) {
                                tx.update(roomRef, "seats", remoteSeats)
                            }
                        }
                    }.await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(Unit)
    }

    /**
     * Speaker toggles their microphone Mute/Unmute state.
     */
    suspend fun toggleMicMute(roomId: String, seatIndex: Int, isMuted: Boolean): Result<Unit> {
        val currentRoom = _roomDetailMap[roomId]?.value ?: _roomsState.value.find { it.id == roomId }
        if (currentRoom != null) {
            val updatedSeats = currentRoom.seats.toMutableList()
            if (seatIndex in updatedSeats.indices) {
                val currentSeat = updatedSeats[seatIndex]
                updatedSeats[seatIndex] = currentSeat.copy(
                    isMuted = isMuted,
                    isSpeaking = if (isMuted) false else currentSeat.isSpeaking
                )
            }
            val updatedRoom = currentRoom.copy(seats = updatedSeats)
            _roomDetailMap.getOrPut(roomId) { MutableStateFlow(updatedRoom) }.value = updatedRoom
            _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }
        }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                val paths = listOf(roomsCollectionPath, collectionPath)
                for (path in paths) {
                    val roomRef = firestore.collection(path).document(roomId)
                    firestore.runTransaction { tx ->
                        val snap = tx.get(roomRef)
                        val remoteRoom = snap.toObject(RoomData::class.java)
                        if (remoteRoom != null) {
                            val remoteSeats = remoteRoom.seats.toMutableList()
                            if (seatIndex in remoteSeats.indices) {
                                remoteSeats[seatIndex] = remoteSeats[seatIndex].copy(
                                    isMuted = isMuted,
                                    isSpeaking = if (isMuted) false else remoteSeats[seatIndex].isSpeaking
                                )
                                tx.update(roomRef, "seats", remoteSeats)
                            }
                        }
                    }.await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(Unit)
    }

    /**
     * Host controls: Mute or kick another speaker.
     */
    suspend fun hostControlSeat(roomId: String, seatIndex: Int, kick: Boolean, mute: Boolean): Result<Unit> {
        val currentRoom = _roomDetailMap[roomId]?.value ?: _roomsState.value.find { it.id == roomId }
        if (currentRoom != null) {
            val updatedSeats = currentRoom.seats.toMutableList()
            if (seatIndex in updatedSeats.indices) {
                if (kick) {
                    val kickedUserId = updatedSeats[seatIndex].userId
                    updatedSeats[seatIndex] = SeatData(seatIndex = seatIndex)
                    // Update member role
                    if (kickedUserId != null) {
                        val members = _roomMembersMap[roomId]?.value ?: emptyList()
                        _roomMembersMap[roomId]?.value = members.map {
                            if (it.uid == kickedUserId) it.copy(role = "AUDIENCE", seatIndex = null) else it
                        }
                    }
                } else if (mute) {
                    updatedSeats[seatIndex] = updatedSeats[seatIndex].copy(isMuted = true, isSpeaking = false)
                }
            }
            val updatedRoom = currentRoom.copy(seats = updatedSeats)
            _roomDetailMap.getOrPut(roomId) { MutableStateFlow(updatedRoom) }.value = updatedRoom
            _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }
        }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                val paths = listOf(roomsCollectionPath, collectionPath)
                for (path in paths) {
                    val roomRef = firestore.collection(path).document(roomId)
                    firestore.runTransaction { tx ->
                        val snap = tx.get(roomRef)
                        val remoteRoom = snap.toObject(RoomData::class.java)
                        if (remoteRoom != null) {
                            val remoteSeats = remoteRoom.seats.toMutableList()
                            if (seatIndex in remoteSeats.indices) {
                                if (kick) {
                                    remoteSeats[seatIndex] = SeatData(seatIndex = seatIndex)
                                } else if (mute) {
                                    remoteSeats[seatIndex] = remoteSeats[seatIndex].copy(isMuted = true, isSpeaking = false)
                                }
                                tx.update(roomRef, "seats", remoteSeats)
                            }
                        }
                    }.await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(Unit)
    }

    /**
     * Request to speak (audience member requests a seat).
     */
    suspend fun requestToSpeak(roomId: String, user: FirebaseUserProfile, targetSeatIndex: Int = -1): Result<Unit> {
        val request = SpeakerRequest(
            id = user.userId,
            roomId = roomId,
            userId = user.userId,
            userName = user.displayName,
            userAvatar = user.avatar,
            userLevel = user.level,
            status = "PENDING",
            targetSeatIndex = targetSeatIndex,
            requestedAt = System.currentTimeMillis()
        )
        val requests = _speakerRequestsMap.getOrPut(roomId) { MutableStateFlow(emptyList()) }.value
        _speakerRequestsMap[roomId]?.value = requests.filter { it.userId != user.userId } + request

        sendChatMessage(
            roomId,
            ChatMessage(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                senderName = "System",
                text = "✋ ${user.displayName} requested to speak.",
                isSystem = true
            )
        )

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("speakerRequests").document(user.userId).set(request).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Cancel speaking request.
     */
    suspend fun cancelSpeakerRequest(roomId: String, userId: String): Result<Unit> {
        val requests = _speakerRequestsMap[roomId]?.value ?: emptyList()
        _speakerRequestsMap[roomId]?.value = requests.filter { it.userId != userId }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("speakerRequests").document(userId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Approve speaking request: assign user to first available empty seat.
     */
    suspend fun approveSpeakerRequest(roomId: String, request: SpeakerRequest): Result<Unit> {
        // Remove from pending requests
        val requests = _speakerRequestsMap[roomId]?.value ?: emptyList()
        _speakerRequestsMap[roomId]?.value = requests.filter { it.userId != request.userId }

        // Find empty seat
        val currentRoom = _roomDetailMap[roomId]?.value ?: _roomsState.value.find { it.id == roomId }
        if (currentRoom != null) {
            val emptyIndex = if (request.targetSeatIndex in 1..7 && currentRoom.seats[request.targetSeatIndex].userId == null) {
                request.targetSeatIndex
            } else {
                currentRoom.seats.indices.firstOrNull { it > 0 && currentRoom.seats[it].userId == null && !currentRoom.seats[it].isLocked } ?: -1
            }

            if (emptyIndex != -1) {
                val fakeProfile = FirebaseUserProfile(
                    userId = request.userId,
                    displayName = request.userName,
                    avatar = request.userAvatar,
                    level = request.userLevel
                )
                takeSeat(roomId, emptyIndex, fakeProfile)
            }
        }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("speakerRequests").document(request.userId).update("status", "APPROVED").await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Reject speaking request.
     */
    suspend fun rejectSpeakerRequest(roomId: String, userId: String): Result<Unit> {
        val requests = _speakerRequestsMap[roomId]?.value ?: emptyList()
        _speakerRequestsMap[roomId]?.value = requests.filter { it.userId != userId }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("speakerRequests").document(userId).delete().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Real-time stream of speaker requests.
     */
    fun getSpeakerRequests(roomId: String): Flow<List<SpeakerRequest>> {
        val flow = _speakerRequestsMap.getOrPut(roomId) { MutableStateFlow(emptyList()) }
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("speakerRequests")
                    .whereEqualTo("status", "PENDING")
                    .addSnapshotListener { snap, err ->
                        if (err == null && snap != null) {
                            flow.value = snap.toObjects(SpeakerRequest::class.java)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return flow.asStateFlow()
    }

    /**
     * Assign or remove a co-host.
     */
    suspend fun assignCoHost(roomId: String, targetUserId: String, isCoHost: Boolean): Result<Unit> {
        val room = _roomDetailMap[roomId]?.value ?: return Result.failure(Exception("Room not found"))
        val updatedCoHosts = if (isCoHost) {
            (room.coHostIds + targetUserId).distinct()
        } else {
            room.coHostIds.filter { it != targetUserId }
        }
        val updatedSeats = room.seats.map { seat ->
            if (seat.userId == targetUserId) seat.copy(isCoHost = isCoHost) else seat
        }
        val updatedRoom = room.copy(coHostIds = updatedCoHosts, seats = updatedSeats)
        _roomDetailMap[roomId]?.value = updatedRoom
        _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }

        // Update member role
        val members = _roomMembersMap[roomId]?.value ?: emptyList()
        _roomMembersMap[roomId]?.value = members.map {
            if (it.uid == targetUserId) it.copy(role = if (isCoHost) "CO_HOST" else if (it.seatIndex != null) "SPEAKER" else "AUDIENCE") else it
        }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId).update("coHostIds", updatedCoHosts).await()
                firestore.collection(collectionPath).document(roomId).update("coHostIds", updatedCoHosts).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Lock/unlock a specific seat so audience cannot sit there.
     */
    suspend fun lockSeat(roomId: String, seatIndex: Int, isLocked: Boolean): Result<Unit> {
        val room = _roomDetailMap[roomId]?.value ?: return Result.failure(Exception("Room not found"))
        val updatedSeats = room.seats.toMutableList()
        if (seatIndex in updatedSeats.indices) {
            updatedSeats[seatIndex] = updatedSeats[seatIndex].copy(isLocked = isLocked)
        }
        val updatedRoom = room.copy(seats = updatedSeats)
        _roomDetailMap[roomId]?.value = updatedRoom
        _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId).update("seats", updatedSeats).await()
                firestore.collection(collectionPath).document(roomId).update("seats", updatedSeats).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Mute or unmute all non-host seats at once.
     */
    suspend fun muteAllSeats(roomId: String, isAllMuted: Boolean): Result<Unit> {
        val room = _roomDetailMap[roomId]?.value ?: return Result.failure(Exception("Room not found"))
        val updatedSeats = room.seats.map { seat ->
            if (!seat.isHost && seat.userId != null) {
                seat.copy(isMuted = isAllMuted, isSpeaking = if (isAllMuted) false else seat.isSpeaking)
            } else {
                seat
            }
        }
        val updatedRoom = room.copy(seats = updatedSeats, isAllMuted = isAllMuted)
        _roomDetailMap[roomId]?.value = updatedRoom
        _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId).update(
                    mapOf(
                        "seats" to updatedSeats,
                        "isAllMuted" to isAllMuted
                    )
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Lock or unlock the entire room with an optional password.
     */
    suspend fun setRoomLock(roomId: String, isLocked: Boolean, password: String = ""): Result<Unit> {
        val room = _roomDetailMap[roomId]?.value ?: return Result.failure(Exception("Room not found"))
        val updatedRoom = room.copy(
            isLocked = isLocked,
            password = password,
            roomType = if (isLocked || password.isNotBlank()) RoomType.PRIVATE else RoomType.PUBLIC
        )
        _roomDetailMap[roomId]?.value = updatedRoom
        _roomsState.value = _roomsState.value.map { if (it.id == roomId) updatedRoom else it }

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId).update(
                    mapOf(
                        "isLocked" to isLocked,
                        "password" to password,
                        "roomType" to updatedRoom.roomType.name
                    )
                ).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Send WebRTC signal for group room audio.
     */
    fun sendRoomRtcSignal(roomId: String, signal: RoomRtcSignal) {
        _roomSignalsMap[roomId]?.value = signal
        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("signals").document(signal.id).set(signal).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Listen to incoming WebRTC signals in this room.
     */
    fun getRoomRtcSignals(roomId: String, myUserId: String): Flow<RoomRtcSignal?> {
        val flow = _roomSignalsMap.getOrPut(roomId) { MutableStateFlow(null) }
        val firestore = getFirestore()
        if (firestore != null) {
            try {
                firestore.collection(roomsCollectionPath).document(roomId)
                    .collection("signals")
                    .whereEqualTo("receiverUid", myUserId)
                    .addSnapshotListener { snap, err ->
                        if (err == null && snap != null && !snap.isEmpty) {
                            val signals = snap.toObjects(RoomRtcSignal::class.java)
                            if (signals.isNotEmpty()) {
                                flow.value = signals.last()
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return flow.asStateFlow()
    }

    /**
     * Search active rooms by numeric ID or title.
     */
    fun searchRooms(query: String): List<RoomData> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return _roomsState.value
        return _roomsState.value.filter { room ->
            room.numericId.contains(cleanQuery) ||
            room.title.lowercase().contains(cleanQuery) ||
            room.tag.lowercase().contains(cleanQuery) ||
            room.hostName.lowercase().contains(cleanQuery)
        }
    }

    /**
     * Real-time stream of room chat comments.
     */
    fun getChatMessages(roomId: String): Flow<List<ChatMessage>> {
        val flow = _roomChatMap.getOrPut(roomId) {
            MutableStateFlow(getFallbackChat(roomId))
        }
        return flow.asStateFlow()
    }

    /**
     * Send a real-time message to room chat.
     */
    suspend fun sendChatMessage(roomId: String, message: ChatMessage): Result<Unit> {
        val msgId = message.id.ifEmpty { UUID.randomUUID().toString() }
        val finalMsg = message.copy(id = msgId, roomId = roomId, timestamp = System.currentTimeMillis())

        val flow = _roomChatMap.getOrPut(roomId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + finalMsg

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(collectionPath).document(roomId)
                    .collection("messages").document(msgId).set(finalMsg).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(Unit)
    }

    /**
     * Real-time stream of latest gift sent in the room to trigger visual animations.
     */
    fun getLatestRoomGiftStream(roomId: String): Flow<GiftTransaction?> {
        val flow = _roomGiftMap.getOrPut(roomId) { MutableStateFlow(null) }
        return flow.asStateFlow()
    }

    /**
     * Real-Time Gift Transaction:
     * - Deducts coins from sender, increments receiver's receivedDiamonds & giftsReceivedCount.
     * - Emits latest gift to room for floating luxury animation.
     * - Adds announcement to room chat.
     * - Updates local user profile and transactions.
     * - Syncs to Firestore in background.
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
        val txId = "tx_gift_" + UUID.randomUUID().toString().take(8)
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

        // 1. Emit latest gift to room state to trigger animation
        val giftFlow = _roomGiftMap.getOrPut(roomId) { MutableStateFlow(null) }
        giftFlow.value = giftTx

        // 2. Announce in room chat
        sendChatMessage(
            roomId,
            ChatMessage(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                senderName = "System",
                text = "🎁 $senderName sent ${gift.emoji} ${gift.name} to $receiverName!",
                isSystem = true
            )
        )

        // 3. Update Receiver Profile in-memory
        val receiverFlow = _userProfilesMap.getOrPut(receiverId) {
            MutableStateFlow(getFallbackUserProfile(receiverId))
        }
        val currentReceiver = receiverFlow.value ?: getFallbackUserProfile(receiverId)
        val updatedReceiver = currentReceiver.copy(
            receivedDiamonds = currentReceiver.receivedDiamonds + gift.costCoins,
            giftsReceivedCount = currentReceiver.giftsReceivedCount + 1
        )
        receiverFlow.value = updatedReceiver

        // 4. Update Receiver's received gifts showcase
        val receivedGiftsFlow = _userReceivedGiftsMap.getOrPut(receiverId) { MutableStateFlow(emptyList()) }
        receivedGiftsFlow.value = listOf(giftTx) + receivedGiftsFlow.value

        // 5. Update Sender's profile & transactions
        val senderFlow = _userProfilesMap.getOrPut(senderId) {
            MutableStateFlow(getFallbackUserProfile(senderId))
        }
        val currentSender = senderFlow.value ?: getFallbackUserProfile(senderId)
        val newSenderBalance = maxOf(0L, currentSender.coinBalance - gift.costCoins)
        senderFlow.value = currentSender.copy(coinBalance = newSenderBalance)

        val senderTxFlow = _userTransactionsMap.getOrPut(senderId) { MutableStateFlow(emptyList()) }
        val senderTx = CoinTransactionItem(
            id = txId,
            userId = senderId,
            type = "GIFT_SENT",
            amount = -gift.costCoins,
            title = "Sent ${gift.emoji} ${gift.name}",
            description = "To $receiverName in room",
            timestamp = System.currentTimeMillis()
        )
        senderTxFlow.value = listOf(senderTx) + senderTxFlow.value

        // Asynchronously persist to Firestore
        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                val senderRef = firestore.collection(usersCollectionPath).document(senderId)
                val receiverRef = firestore.collection(usersCollectionPath).document(receiverId)
                val senderWalletRef = firestore.collection("wallets").document(senderId)
                val receiverWalletRef = firestore.collection("wallets").document(receiverId)
                val roomGiftsRef = firestore.collection(collectionPath).document(roomId).collection("gifts").document(txId)
                val globalGiftTxRef = firestore.collection("giftTransactions").document(txId)

                firestore.runTransaction { tx ->
                    val senderSnap = tx.get(senderRef)
                    val senderWalletSnap = tx.get(senderWalletRef)
                    val cur = if (senderWalletSnap.exists()) {
                        senderWalletSnap.getLong("coinBalance") ?: (senderSnap.getLong("coinBalance") ?: 5000L)
                    } else {
                        senderSnap.getLong("coinBalance") ?: 5000L
                    }
                    val newSenderBal = maxOf(0L, cur - gift.costCoins)
                    val senderSpent = (senderWalletSnap.getLong("lifetimeSpentCoins") ?: 0L) + gift.costCoins

                    tx.update(senderRef, "coinBalance", newSenderBal)
                    tx.set(
                        senderWalletRef,
                        mapOf(
                            "uid" to senderId,
                            "coinBalance" to newSenderBal,
                            "lifetimeSpentCoins" to senderSpent,
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )

                    val receiverSnap = tx.get(receiverRef)
                    val receiverWalletSnap = tx.get(receiverWalletRef)
                    val curDiamonds = receiverSnap.getLong("receivedDiamonds") ?: 0L
                    val curGifts = receiverSnap.getLong("giftsReceivedCount") ?: 0L
                    val receiverGiftVal = (receiverWalletSnap.getLong("lifetimeGiftValue") ?: 0L) + gift.costCoins

                    tx.update(receiverRef, "receivedDiamonds", curDiamonds + gift.costCoins)
                    tx.update(receiverRef, "giftsReceivedCount", curGifts + 1)
                    tx.set(
                        receiverWalletRef,
                        mapOf(
                            "uid" to receiverId,
                            "lifetimeGiftValue" to receiverGiftVal,
                            "lifetimeReceivedCoins" to ((receiverWalletSnap.getLong("lifetimeReceivedCoins") ?: 0L) + gift.costCoins),
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )

                    tx.set(roomGiftsRef, giftTx)

                    // Write to global giftTransactions
                    val fullGiftRecord = mapOf(
                        "giftTransactionId" to txId,
                        "operationId" to txId,
                        "senderUid" to senderId,
                        "senderDisplayName" to senderName,
                        "senderAvatarUrl" to senderAvatar,
                        "recipientUid" to receiverId,
                        "recipientDisplayName" to receiverName,
                        "giftId" to gift.id,
                        "giftName" to gift.name,
                        "giftEmoji" to gift.emoji,
                        "coinPrice" to gift.costCoins,
                        "quantity" to 1,
                        "totalCoins" to gift.costCoins,
                        "roomId" to roomId,
                        "createdAt" to System.currentTimeMillis(),
                        "status" to "SUCCESS"
                    )
                    tx.set(globalGiftTxRef, fullGiftRecord)

                    // Write walletTransactions for sender and recipient
                    val senderLedgerId = firestore.collection("walletTransactions").document().id
                    tx.set(
                        firestore.collection("walletTransactions").document(senderLedgerId),
                        mapOf(
                            "transactionId" to senderLedgerId,
                            "uid" to senderId,
                            "type" to "GIFT_SENT",
                            "amount" to -gift.costCoins,
                            "balanceBefore" to cur,
                            "balanceAfter" to newSenderBal,
                            "referenceId" to txId,
                            "status" to "SUCCESS",
                            "description" to "Sent ${gift.emoji} ${gift.name} to $receiverName in room",
                            "createdAt" to System.currentTimeMillis(),
                            "serverTimestamp" to System.currentTimeMillis()
                        )
                    )
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(giftTx)
    }

    /**
     * Real-time stream of user profile by User ID.
     */
    fun getUserProfileStream(userId: String): Flow<FirebaseUserProfile?> {
        val flow = _userProfilesMap.getOrPut(userId) {
            MutableStateFlow(getFallbackUserProfile(userId))
        }
        return flow.asStateFlow()
    }

    /**
     * Fetch user profile once.
     */
    suspend fun getUserProfileOnce(userId: String): FirebaseUserProfile {
        return _userProfilesMap[userId]?.value ?: getFallbackUserProfile(userId)
    }

    /**
     * Update user profile details.
     */
    suspend fun updateUserProfile(profile: FirebaseUserProfile): Result<Unit> {
        val flow = _userProfilesMap.getOrPut(profile.userId) { MutableStateFlow(profile) }
        flow.value = profile

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                firestore.collection(usersCollectionPath).document(profile.userId).set(profile, SetOptions.merge()).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Result.success(Unit)
    }

    /**
     * Instant Coin Recharge with transaction record.
     */
    suspend fun rechargeCoins(userId: String, coins: Long, packTitle: String): Result<Long> {
        val flow = _userProfilesMap.getOrPut(userId) { MutableStateFlow(getFallbackUserProfile(userId)) }
        val curProfile = flow.value ?: getFallbackUserProfile(userId)
        val newBalance = curProfile.coinBalance + coins
        flow.value = curProfile.copy(coinBalance = newBalance)

        val txId = "tx_rec_" + UUID.randomUUID().toString().take(8)
        val txFlow = _userTransactionsMap.getOrPut(userId) { MutableStateFlow(emptyList()) }
        val txItem = CoinTransactionItem(
            id = txId,
            userId = userId,
            type = "RECHARGE",
            amount = coins,
            title = "Recharged $packTitle",
            description = "+$coins Coins added to wallet",
            timestamp = System.currentTimeMillis()
        )
        txFlow.value = listOf(txItem) + txFlow.value

        repositoryScope.launch {
            try {
                val firestore = getFirestore() ?: return@launch
                val userRef = firestore.collection(usersCollectionPath).document(userId)
                firestore.runTransaction { tx ->
                    val snap = tx.get(userRef)
                    val cur = snap.getLong("coinBalance") ?: 5000L
                    tx.update(userRef, "coinBalance", cur + coins)
                }.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Result.success(newBalance)
    }

    /**
     * Stream user coin transaction history.
     */
    fun getUserTransactions(userId: String): Flow<List<CoinTransactionItem>> {
        val flow = _userTransactionsMap.getOrPut(userId) { MutableStateFlow(emptyList()) }
        return flow.asStateFlow()
    }

    /**
     * Stream gifts received by user for the Gift Showcase.
     */
    fun getUserReceivedGifts(userId: String): Flow<List<GiftTransaction>> {
        val flow = _userReceivedGiftsMap.getOrPut(userId) {
            MutableStateFlow(
                listOf(
                    GiftTransaction(giftEmoji = "🌹", giftName = "Rose", coins = 10),
                    GiftTransaction(giftEmoji = "👑", giftName = "Crown", coins = 5000),
                    GiftTransaction(giftEmoji = "🚀", giftName = "Rocket", coins = 1000),
                    GiftTransaction(giftEmoji = "🏎️", giftName = "Sports Car", coins = 500)
                )
            )
        }
        return flow.asStateFlow()
    }

    // ----------------------------------------------------
    // Fallback & Seed Data Helpers
    // ----------------------------------------------------
    private fun seedInitialRooms(firestore: FirebaseFirestore) {
        repositoryScope.launch {
            try {
                val batch = firestore.batch()
                for (room in getFallbackRooms()) {
                    val doc = firestore.collection(collectionPath).document(room.id)
                    batch.set(doc, room)
                }
                batch.commit().await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFallbackRooms(): List<RoomData> {
        val room1Seats = listOf(
            SeatData(0, "ID_HOST_1001", "Aria Star", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", isSpeaking = true, isMuted = false, vipLevel = 3, isHost = true),
            SeatData(1, "ID_SOPHIA_2002", "Sophia", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", isSpeaking = false, isMuted = false, vipLevel = 1),
            SeatData(2, "ID_LEO_3003", "Leo Cruz", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", isSpeaking = true, isMuted = false, vipLevel = 2),
            SeatData(3),
            SeatData(4, "ID_ELENA_4004", "Elena Joy", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150", isSpeaking = false, isMuted = true, vipLevel = 1),
            SeatData(5),
            SeatData(6),
            SeatData(7),
            SeatData(8),
            SeatData(9)
        )

        val room2Seats = listOf(
            SeatData(0, "ID_HOST_1002", "DJ Mike", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", isSpeaking = true, isMuted = false, vipLevel = 4, isHost = true),
            SeatData(1, "ID_CHLOE_2005", "Chloe", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", isSpeaking = false, isMuted = false, vipLevel = 2),
            SeatData(2),
            SeatData(3),
            SeatData(4),
            SeatData(5),
            SeatData(6),
            SeatData(7),
            SeatData(8),
            SeatData(9)
        )

        val banglaSeats = listOf(
            SeatData(0, "ID_HOST_BANGLA", "Tanvir Ahmed", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150", isSpeaking = true, isMuted = false, vipLevel = 5, isHost = true),
            SeatData(1, "ID_SADIA_BANGLA", "Sadia Noor", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", isSpeaking = false, isMuted = false, vipLevel = 3),
            SeatData(2),
            SeatData(3),
            SeatData(4),
            SeatData(5),
            SeatData(6),
            SeatData(7),
            SeatData(8),
            SeatData(9)
        )

        return listOf(
            RoomData(
                id = "room_acoustic_night",
                numericId = "738192",
                title = "🎸 Acoustic Vibes & Late Chat",
                description = "Singing acoustic covers and having chill late night conversations. Grab a seat!",
                coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
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
                id = "room_bangla_adda",
                numericId = "519024",
                title = "🇧🇩 বাংলা আড্ডা ও গান নাইট",
                description = "সবাই আসুন স্টেজে উঠে কথা বলুন ও গান শুনুন! Enjoy live chat.",
                coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800",
                hostId = "ID_HOST_BANGLA",
                hostName = "Tanvir Ahmed",
                hostAvatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                agencyName = "Bengal Kings 👑",
                hostLevel = 42,
                viewerCount = "4.5k",
                countryFlag = "🇧🇩",
                tag = "Adda",
                bgGradientStart = "#004D40",
                bgGradientEnd = "#00796B",
                seats = banglaSeats,
                createdAt = System.currentTimeMillis() - 1200000
            ),
            RoomData(
                id = "room_electronic_party",
                numericId = "842105",
                title = "⚡ EDM Beats & Club Chillout",
                description = "Live DJ mixes, gift wars, and party games!",
                coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800",
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
                numericId = "629341",
                title = "🌍 Global Friends & Language Exchange",
                description = "Meet people from across the globe, practice languages, and relax.",
                coverUrl = "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=800",
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
                seats = List(10) { if (it == 0) SeatData(0, "ID_HOST_1003", "Kenji Tokyo", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150", isSpeaking = true, isHost = true) else SeatData(it) },
                createdAt = System.currentTimeMillis() - 7200000
            )
        )
    }

    private fun getFallbackChat(roomId: String): List<ChatMessage> {
        return listOf(
            ChatMessage(id = "msg_1", roomId = roomId, senderName = "System", text = "✨ Welcome to the Voice Party! Respect each other and have fun.", isSystem = true),
            ChatMessage(id = "msg_2", roomId = roomId, senderName = "Sophia", text = "Hey everyone, wonderful voice today! 💖"),
            ChatMessage(id = "msg_3", roomId = roomId, senderName = "Leo", text = "Sent 5 Roses 🌹"),
            ChatMessage(id = "msg_4", roomId = roomId, senderName = "Tanvir", text = "স্টেজে এসে কথা বলুন সবাই 🔥")
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
            "ID_HOST_BANGLA" -> FirebaseUserProfile(
                userId = "ID_HOST_BANGLA",
                publicUserId = "77112233",
                displayName = "Tanvir Ahmed",
                username = "tanvir_live",
                avatar = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150",
                coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                bio = "Welcome to Bangla Adda Club! 🇧🇩🎙️ প্রতিদিন লাইভ গান ও আড্ডা।",
                level = 42,
                vipLevel = 5,
                coinBalance = 85000L,
                receivedDiamonds = 150000L,
                giftsReceivedCount = 420,
                followersCount = 34500,
                followingCount = 89
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
                publicUserId = "8899" + userId.takeLast(4).filter { it.isDigit() }.padStart(4, '7'),
                displayName = "VIP Clubber",
                username = "user_" + userId.take(6),
                avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                coverImage = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                bio = "Voice room star! Let's party & connect. 🎙️✨",
                level = 10,
                vipLevel = 2,
                coinBalance = 50000L,
                receivedDiamonds = 12500L,
                giftsReceivedCount = 38,
                followersCount = 1450,
                followingCount = 112
            )
        }
    }
}
