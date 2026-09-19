package com.example.ui.screens.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.RoomRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class RoomViewModel(
    private val roomRepository: RoomRepository,
    private val authRepository: AuthRepository? = null,
    private val userRepository: UserRepository? = null
) : ViewModel() {

    val activeRooms: StateFlow<List<RoomData>> = roomRepository.getActiveVoiceRooms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentRoomId = MutableStateFlow<String?>(null)

    val currentRoom: StateFlow<RoomData?> = _currentRoomId.flatMapLatest { roomId ->
        if (roomId == null) flowOf(null)
        else roomRepository.getRoomStream(roomId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val roomChat: StateFlow<List<ChatMessage>> = _currentRoomId.flatMapLatest { roomId ->
        if (roomId == null) flowOf(emptyList())
        else roomRepository.getChatMessages(roomId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestRoomGift: StateFlow<GiftTransaction?> = _currentRoomId.flatMapLatest { roomId ->
        if (roomId == null) flowOf(null)
        else roomRepository.getLatestRoomGiftStream(roomId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentUserProfile = MutableStateFlow(
        FirebaseUserProfile(
            userId = "USER_ME_CURRENT",
            publicUserId = "88991122",
            displayName = "My VIP Avatar",
            username = "vip_clubber",
            avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            coverImage = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
            bio = "Real-time Voice Club Star! Let's party & connect. 🎙️✨",
            level = 12,
            vipLevel = 2,
            svipLevel = 0,
            coinBalance = 5000L,
            receivedDiamonds = 12500L,
            giftsReceivedCount = 38
        )
    )
    val currentUserProfile: StateFlow<FirebaseUserProfile> = _currentUserProfile.asStateFlow()

    private val _visitedUserProfile = MutableStateFlow<FirebaseUserProfile?>(null)
    val visitedUserProfile: StateFlow<FirebaseUserProfile?> = _visitedUserProfile.asStateFlow()

    private val _visitedUserGifts = MutableStateFlow<List<GiftTransaction>>(emptyList())
    val visitedUserGifts: StateFlow<List<GiftTransaction>> = _visitedUserGifts.asStateFlow()

    val userTransactions: StateFlow<List<CoinTransactionItem>> = _currentUserProfile.flatMapLatest { user ->
        roomRepository.getUserTransactions(user.userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiNotice = MutableStateFlow<String?>(null)
    val uiNotice: StateFlow<String?> = _uiNotice.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                if (authRepository != null && userRepository != null) {
                    val token = authRepository.currentSessionToken.firstOrNull()
                    if (token != null) {
                        val session = authRepository.getCurrentSession(token)
                        if (session != null) {
                            val user = userRepository.getUser(session.userId)
                            if (user != null) {
                                val baseProfile = FirebaseUserProfile(
                                    userId = user.id,
                                    publicUserId = user.publicUserId,
                                    displayName = user.displayName.ifBlank { user.username },
                                    username = user.username,
                                    avatar = user.avatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                    coverImage = user.coverImage ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                                    bio = "Voice party enthusiast! 🎙️✨",
                                    level = 8,
                                    vipLevel = 2,
                                    coinBalance = 3500L
                                )
                                _currentUserProfile.value = baseProfile

                                // Connect real-time profile stream from Firestore
                                roomRepository.getUserProfileStream(user.id).collect { firestoreProfile ->
                                    if (firestoreProfile != null) {
                                        _currentUserProfile.value = firestoreProfile
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectRoom(roomId: String) {
        _currentRoomId.value = roomId
    }

    fun closeCurrentRoom() {
        _currentRoomId.value = null
    }

    fun deleteRoom(roomId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            roomRepository.deleteRoom(roomId)
            if (_currentRoomId.value == roomId) {
                _currentRoomId.value = null
            }
            _uiNotice.value = "Room deleted successfully."
            onDeleted()
        }
    }

    fun clearNotice() {
        _uiNotice.value = null
    }

    fun createRoom(
        title: String,
        description: String,
        category: String,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = roomRepository.createVoiceRoom(
                title = title,
                description = description,
                category = category,
                hostUser = _currentUserProfile.value
            )
            res.onSuccess { roomId ->
                _currentRoomId.value = roomId
                _uiNotice.value = "Party Room created successfully! 🎙️"
                onCreated(roomId)
            }.onFailure { err ->
                _uiNotice.value = "Failed to create room: ${err.message}"
            }
        }
    }

    fun takeSeat(roomId: String, seatIndex: Int) {
        viewModelScope.launch {
            val res = roomRepository.takeSeat(roomId, seatIndex, _currentUserProfile.value)
            res.onSuccess {
                _uiNotice.value = "You took Seat ${seatIndex + 1}! 🎙️ Speak away!"
            }.onFailure { err ->
                _uiNotice.value = err.message ?: "Could not take seat"
            }
        }
    }

    fun leaveSeat(roomId: String, seatIndex: Int) {
        viewModelScope.launch {
            val res = roomRepository.leaveSeat(roomId, seatIndex, _currentUserProfile.value.userId)
            res.onSuccess {
                _uiNotice.value = "Left the seat"
            }.onFailure { err ->
                _uiNotice.value = err.message
            }
        }
    }

    fun toggleMicMute(roomId: String, seatIndex: Int, isMuted: Boolean) {
        viewModelScope.launch {
            roomRepository.toggleMicMute(roomId, seatIndex, isMuted)
        }
    }

    fun hostControlSeat(roomId: String, seatIndex: Int, kick: Boolean, mute: Boolean) {
        viewModelScope.launch {
            val res = roomRepository.hostControlSeat(roomId, seatIndex, kick, mute)
            res.onSuccess {
                _uiNotice.value = if (kick) "Speaker removed from seat" else "Speaker microphone muted"
            }
        }
    }

    fun sendChat(roomId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val user = _currentUserProfile.value
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                senderId = user.userId,
                senderName = user.displayName,
                senderAvatar = user.avatar,
                text = text.trim(),
                isSystem = false
            )
            roomRepository.sendChatMessage(roomId, msg)
        }
    }

    fun sendGift(
        roomId: String,
        receiverId: String,
        receiverName: String,
        gift: GiftItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val sender = _currentUserProfile.value
            val res = roomRepository.sendGift(
                roomId = roomId,
                senderId = sender.userId,
                senderName = sender.displayName,
                senderAvatar = sender.avatar,
                receiverId = receiverId,
                receiverName = receiverName,
                gift = gift
            )
            res.onSuccess {
                // Update local balance immediately
                _currentUserProfile.value = _currentUserProfile.value.copy(
                    coinBalance = maxOf(0L, _currentUserProfile.value.coinBalance - gift.costCoins)
                )
                _uiNotice.value = "Sent ${gift.emoji} ${gift.name} to $receiverName! 🎉"
                onSuccess()
            }.onFailure { err ->
                val msg = err.message ?: "Failed to send gift"
                _uiNotice.value = msg
                onError(msg)
            }
        }
    }

    fun sendGift(
        roomId: String,
        recipientId: String,
        recipientName: String,
        giftId: String,
        giftName: String,
        giftEmoji: String,
        coins: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        sendGift(
            roomId = roomId,
            receiverId = recipientId,
            receiverName = recipientName,
            gift = GiftItem(id = giftId, name = giftName, emoji = giftEmoji, costCoins = coins),
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun visitUserProfile(userId: String) {
        viewModelScope.launch {
            val profile = roomRepository.getUserProfileOnce(userId)
            _visitedUserProfile.value = profile
            // Load received gifts
            roomRepository.getUserReceivedGifts(userId).collect { gifts ->
                _visitedUserGifts.value = gifts
            }
        }
    }

    fun closeVisitedProfile() {
        _visitedUserProfile.value = null
        _visitedUserGifts.value = emptyList()
    }

    fun clearVisitedUser() {
        closeVisitedProfile()
    }

    fun rechargeCoins(coins: Long, packTitle: String) {
        viewModelScope.launch {
            val res = roomRepository.rechargeCoins(_currentUserProfile.value.userId, coins, packTitle)
            res.onSuccess { newBal ->
                _currentUserProfile.value = _currentUserProfile.value.copy(coinBalance = newBal)
                _uiNotice.value = "Successfully recharged $coins Coins! 🪙"
            }.onFailure { err ->
                _uiNotice.value = "Recharge error: ${err.message}"
            }
        }
    }

    fun updateProfile(displayName: String, bio: String, avatar: String) {
        viewModelScope.launch {
            val updated = _currentUserProfile.value.copy(
                displayName = displayName.ifBlank { _currentUserProfile.value.displayName },
                bio = bio.ifBlank { _currentUserProfile.value.bio },
                avatar = avatar.ifBlank { _currentUserProfile.value.avatar }
            )
            _currentUserProfile.value = updated
            roomRepository.updateUserProfile(updated)
            _uiNotice.value = "Profile updated successfully! ✨"
        }
    }
}
