package com.example.ui.screens.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DirectConversationItem
import com.example.data.model.DirectMessage
import com.example.data.model.FirebaseUserProfile
import com.example.data.model.UserReportRecord
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val currentUserId: String? get() = authRepository.getCurrentUserId()

    // Real-time conversation list
    private val _conversations = MutableStateFlow<List<DirectConversationItem>>(emptyList())
    val conversations: StateFlow<List<DirectConversationItem>> = _conversations.asStateFlow()

    private val _isLoadingConversations = MutableStateFlow(true)
    val isLoadingConversations: StateFlow<Boolean> = _isLoadingConversations.asStateFlow()

    // Currently open chat
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _activeOtherUser = MutableStateFlow<FirebaseUserProfile?>(null)
    val activeOtherUser: StateFlow<FirebaseUserProfile?> = _activeOtherUser.asStateFlow()

    private val _messages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val messages: StateFlow<List<DirectMessage>> = _messages.asStateFlow()

    private val _typedMessage = MutableStateFlow("")
    val typedMessage: StateFlow<String> = _typedMessage.asStateFlow()

    private val _replyTo = MutableStateFlow<DirectMessage?>(null)
    val replyTo: StateFlow<DirectMessage?> = _replyTo.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    private var messagesJob: Job? = null
    private var conversationsJob: Job? = null

    init {
        observeConversations()
    }

    private fun observeConversations() {
        conversationsJob?.cancel()
        conversationsJob = viewModelScope.launch {
            authRepository.currentUserIdFlow.collectLatest { uid ->
                val effectiveUid = uid ?: currentUserId
                if (!effectiveUid.isNullOrBlank()) {
                    _isLoadingConversations.value = true
                    chatRepository.getConversationsStream(effectiveUid).collect { list ->
                        _conversations.value = list
                        _isLoadingConversations.value = false
                    }
                } else {
                    _conversations.value = emptyList()
                    _isLoadingConversations.value = false
                }
            }
        }
    }

    fun openConversationWith(targetUser: FirebaseUserProfile) {
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = chatRepository.getOrCreateConversation(uid, targetUser.userId)
            result.onSuccess { convId ->
                openConversation(convId, targetUser)
            }.onFailure { err ->
                _statusNotice.value = "Failed to start chat: ${err.message}"
            }
        }
    }

    fun openConversation(convId: String, otherUser: FirebaseUserProfile?) {
        _activeConversationId.value = convId
        _activeOtherUser.value = otherUser
        val uid = currentUserId ?: return

        // Mark messages as read
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(convId, uid)
        }

        // Listen to messages
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.getMessagesStream(convId).collect { msgList ->
                _messages.value = msgList
                // Mark any newly incoming messages as read while actively open
                chatRepository.markMessagesAsRead(convId, uid)
            }
        }
    }

    fun closeActiveConversation() {
        _activeConversationId.value = null
        _activeOtherUser.value = null
        _messages.value = emptyList()
        _replyTo.value = null
        _typedMessage.value = ""
        messagesJob?.cancel()
    }

    fun updateTypedMessage(text: String) {
        _typedMessage.value = text
    }

    fun setReplyTo(message: DirectMessage?) {
        _replyTo.value = message
    }

    fun sendMessage() {
        val text = _typedMessage.value.trim()
        val convId = _activeConversationId.value ?: return
        val other = _activeOtherUser.value ?: return
        val senderUid = currentUserId ?: return
        if (text.isBlank() || _isSending.value) return

        _isSending.value = true
        val replyId = _replyTo.value?.messageId

        viewModelScope.launch {
            val senderProfile = userRepository.getUserProfile(senderUid)
            val publicId = senderProfile?.publicUserId ?: "0000000"

            val result = chatRepository.sendMessage(
                conversationId = convId,
                senderUid = senderUid,
                receiverUid = other.userId,
                senderPublicUserId = publicId,
                text = text,
                replyToMessageId = replyId
            )

            _isSending.value = false
            result.onSuccess {
                _typedMessage.value = ""
                _replyTo.value = null
            }.onFailure { err ->
                _statusNotice.value = err.message ?: "Failed to send message"
            }
        }
    }

    fun deleteMessage(messageId: String) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(convId, messageId)
        }
    }

    fun blockCurrentChatUser() {
        val other = _activeOtherUser.value ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val result = chatRepository.blockUser(uid, other.userId)
            result.onSuccess {
                _statusNotice.value = "User ${other.displayName} has been blocked."
                closeActiveConversation()
            }.onFailure { err ->
                _statusNotice.value = "Failed to block: ${err.message}"
            }
        }
    }

    fun reportCurrentChat(reason: String, details: String) {
        val other = _activeOtherUser.value ?: return
        val uid = currentUserId ?: return
        viewModelScope.launch {
            val report = UserReportRecord(
                reporterUid = uid,
                reportedUid = other.userId,
                conversationId = _activeConversationId.value,
                reason = reason,
                description = details
            )
            val result = chatRepository.reportUserOrMessage(report)
            result.onSuccess {
                _statusNotice.value = "Report submitted. Thank you for keeping the community safe."
            }.onFailure { err ->
                _statusNotice.value = "Report failed: ${err.message}"
            }
        }
    }

    fun clearNotice() {
        _statusNotice.value = null
    }
}
