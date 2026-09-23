package com.example.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AuthRepository
import com.example.data.repository.GameRepository
import com.example.data.repository.UserRepository
import com.example.data.repository.WalletRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class GameRoundPhase {
    BETTING_OPEN,
    BETTING_LOCKED,
    DEALING_ANIMATION,
    RESULT_SHOWCASE,
    ROUND_ENDED
}

class GameViewModel(
    private val gameRepository: GameRepository = GameRepository(),
    private val walletRepository: WalletRepository = WalletRepository(),
    private val authRepository: AuthRepository? = null,
    private val userRepository: UserRepository? = null
) : ViewModel() {

    // Current User Profile & Wallet
    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _publicUserId = MutableStateFlow<String>("1000001")
    val publicUserId: StateFlow<String> = _publicUserId.asStateFlow()

    private val _displayName = MutableStateFlow<String>("Player")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String>("")
    val avatarUrl: StateFlow<String> = _avatarUrl.asStateFlow()

    val wallet: StateFlow<UserWallet?> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(UserWallet(coinBalance = 2500L))
        else walletRepository.getWalletStream(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserWallet())

    // 1. Game Catalog & Filter
    val gamesCatalog: StateFlow<List<GameItem>> = gameRepository.getGamesCatalogStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), gameRepository.getDefaultGamesCatalog())

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // 2. Active Game Arena
    private val _activeGame = MutableStateFlow<GameItem?>(null)
    val activeGame: StateFlow<GameItem?> = _activeGame.asStateFlow()

    private val _activeSession = MutableStateFlow<GameSession?>(null)
    val activeSession: StateFlow<GameSession?> = _activeSession.asStateFlow()

    private val _currentRound = MutableStateFlow<GameRound?>(null)
    val currentRound: StateFlow<GameRound?> = _currentRound.asStateFlow()

    private val _sessionMembers = MutableStateFlow<List<GameMember>>(emptyList())
    val sessionMembers: StateFlow<List<GameMember>> = _sessionMembers.asStateFlow()

    private val _playerRoundBets = MutableStateFlow<List<GameBet>>(emptyList())
    val playerRoundBets: StateFlow<List<GameBet>> = _playerRoundBets.asStateFlow()

    // 3. Synchronized Server Countdown & Phase
    private val _roundPhase = MutableStateFlow<GameRoundPhase>(GameRoundPhase.BETTING_OPEN)
    val roundPhase: StateFlow<GameRoundPhase> = _roundPhase.asStateFlow()

    private val _secondsRemaining = MutableStateFlow<Int>(12)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    private val _phaseStatusMessage = MutableStateFlow<String>("🔥 PLACE YOUR BETS!")
    val phaseStatusMessage: StateFlow<String> = _phaseStatusMessage.asStateFlow()

    private val _lastRoundOutcome = MutableStateFlow<ServerRoundOutcome?>(null)
    val lastRoundOutcome: StateFlow<ServerRoundOutcome?> = _lastRoundOutcome.asStateFlow()

    // 4. History, Leaderboard, Events, Invitations
    val gameHistory: StateFlow<List<GameHistoryItem>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList())
        else gameRepository.getGameHistoryStream(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaderboard: StateFlow<List<GameLeaderboardEntry>> = gameRepository.getLeaderboardStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeEvents: StateFlow<List<GameEvent>> = gameRepository.getActiveGameEventsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingInvitations: StateFlow<List<GameInvitation>> = _currentUserId.flatMapLatest { uid ->
        if (uid.isBlank()) flowOf(emptyList())
        else gameRepository.getIncomingGameInvitationsStream(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Coroutine Jobs for Live State
    private var sessionListenerJob: Job? = null
    private var membersListenerJob: Job? = null
    private var roundListenerJob: Job? = null
    private var playerBetsListenerJob: Job? = null
    private var countdownLoopJob: Job? = null
    private var serverSyncJob: Job? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (fbUser != null) {
                    _currentUserId.value = fbUser.uid
                    _displayName.value = fbUser.displayName ?: "VIP Player"
                    _avatarUrl.value = fbUser.photoUrl?.toString() ?: ""
                }

                authRepository?.currentUserIdFlow?.collect { uid ->
                    if (!uid.isNullOrBlank()) {
                        _currentUserId.value = uid
                        userRepository?.getPublicProfile(uid)?.let { profile ->
                            _publicUserId.value = profile.publicUserId
                            _displayName.value = profile.displayName
                            _avatarUrl.value = profile.avatar
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    // ==========================================
    // ARENA ENTRY & SESSION ORCHESTRATION
    // ==========================================

    fun enterGame(game: GameItem, linkedVoiceRoomId: String? = null) {
        if (!game.enabled || game.status == "DISABLED" || game.status == "MAINTENANCE") {
            return
        }

        _activeGame.value = game

        viewModelScope.launch {
            val session = gameRepository.getOrCreateGlobalSession(
                gameId = game.gameId,
                gameName = game.name,
                userUid = _currentUserId.value.ifBlank { "guest_${UUID.randomUUID()}" },
                publicUserId = _publicUserId.value,
                displayName = _displayName.value,
                photoUrl = _avatarUrl.value,
                voiceRoomId = linkedVoiceRoomId
            )
            _activeSession.value = session

            // Bind real-time listeners
            bindSessionListeners(session.sessionId, game.gameId)
            startAuthoritativeCountdownLoop(session.sessionId, game.gameId)
        }
    }

    fun leaveCurrentGame() {
        val session = _activeSession.value
        val uid = _currentUserId.value
        if (session != null && uid.isNotBlank()) {
            viewModelScope.launch {
                gameRepository.leaveSession(session.sessionId, uid)
            }
        }

        sessionListenerJob?.cancel()
        membersListenerJob?.cancel()
        roundListenerJob?.cancel()
        playerBetsListenerJob?.cancel()
        countdownLoopJob?.cancel()
        serverSyncJob?.cancel()

        _activeGame.value = null
        _activeSession.value = null
        _currentRound.value = null
        _playerRoundBets.value = emptyList()
        _lastRoundOutcome.value = null
    }

    private fun bindSessionListeners(sessionId: String, gameId: String) {
        sessionListenerJob?.cancel()
        sessionListenerJob = viewModelScope.launch {
            gameRepository.getActiveSessionStream(sessionId).collect { session ->
                _activeSession.value = session
            }
        }

        membersListenerJob?.cancel()
        membersListenerJob = viewModelScope.launch {
            gameRepository.getSessionMembersStream(sessionId).collect { members ->
                _sessionMembers.value = members
            }
        }

        roundListenerJob?.cancel()
        roundListenerJob = viewModelScope.launch {
            gameRepository.getLatestRoundStream(sessionId).collect { round ->
                _currentRound.value = round
                if (round != null) {
                    bindPlayerBetsListener(sessionId, round.roundId)
                }
            }
        }
    }

    private fun bindPlayerBetsListener(sessionId: String, roundId: String) {
        playerBetsListenerJob?.cancel()
        playerBetsListenerJob = viewModelScope.launch {
            val uid = _currentUserId.value
            if (uid.isNotBlank()) {
                gameRepository.getPlayerRoundBetsStream(sessionId, roundId, uid).collect { bets ->
                    _playerRoundBets.value = bets
                }
            }
        }
    }

    // ==========================================
    // AUTHORITATIVE SYNCHRONIZED COUNTDOWN LOOP
    // ==========================================

    private fun startAuthoritativeCountdownLoop(sessionId: String, gameId: String) {
        countdownLoopJob?.cancel()
        countdownLoopJob = viewModelScope.launch {
            while (true) {
                val round = _currentRound.value
                val now = System.currentTimeMillis()

                if (round != null) {
                    val bettingClose = round.bettingClosedAt
                    val resultTime = round.resultAt
                    val roundEndTime = round.roundEndAt

                    if (now < bettingClose) {
                        // Phase 1: Betting Open
                        _roundPhase.value = GameRoundPhase.BETTING_OPEN
                        val secsLeft = maxOf(0, ((bettingClose - now) / 1000L).toInt())
                        _secondsRemaining.value = secsLeft
                        _phaseStatusMessage.value = "🔥 PLACE YOUR BETS! ($secsLeft s)"
                    } else if (now < resultTime) {
                        // Phase 2: Betting Locked & Dealing Animation
                        _roundPhase.value = GameRoundPhase.DEALING_ANIMATION
                        val secsLeft = maxOf(0, ((resultTime - now) / 1000L).toInt())
                        _secondsRemaining.value = secsLeft
                        _phaseStatusMessage.value = "🃏 DEALING CARDS..."

                        // Settle round server-side if not already settled
                        if (round.status == "OPEN") {
                            launch {
                                val outcome = gameRepository.settleRoundOutcome(sessionId, round.roundId, gameId)
                                if (outcome != null) {
                                    _lastRoundOutcome.value = outcome
                                }
                            }
                        }
                    } else if (now < roundEndTime) {
                        // Phase 3: Result Showcase & Win Distribution
                        _roundPhase.value = GameRoundPhase.RESULT_SHOWCASE
                        val secsLeft = maxOf(0, ((roundEndTime - now) / 1000L).toInt())
                        _secondsRemaining.value = secsLeft
                        val outcomeName = round.resultOutcome.ifBlank { _lastRoundOutcome.value?.outcomeName ?: "RESULT" }
                        _phaseStatusMessage.value = "🎉 WINNER: $outcomeName ($secsLeft s)"
                    } else {
                        // Phase 4: Round Complete -> Advance to next round
                        _roundPhase.value = GameRoundPhase.ROUND_ENDED
                        _phaseStatusMessage.value = "Next round starting..."
                        _lastRoundOutcome.value = null

                        // Start next round
                        val nextRoundNum = round.roundNumber + 1
                        gameRepository.startNewRound(sessionId, gameId, nextRoundNum)
                        delay(1000)
                    }
                } else {
                    _phaseStatusMessage.value = "Connecting to Game Server..."
                }

                delay(500)
            }
        }
    }

    // ==========================================
    // BET PLACEMENT & ACTIONS
    // ==========================================

    fun placeBet(
        betTarget: String,
        amount: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val session = _activeSession.value
        val round = _currentRound.value
        val game = _activeGame.value
        val uid = _currentUserId.value

        if (session == null || round == null || game == null) {
            onError("Game session not active")
            return
        }

        if (uid.isBlank()) {
            onError("Please log in to play")
            return
        }

        if (_roundPhase.value != GameRoundPhase.BETTING_OPEN) {
            onError("Betting is currently closed for this round")
            return
        }

        val curBal = wallet.value?.coinBalance ?: 0L
        if (curBal < amount) {
            onError("Insufficient Coins. Please recharge your wallet.")
            return
        }

        viewModelScope.launch {
            val (success, message) = gameRepository.placeGameBet(
                sessionId = session.sessionId,
                roundId = round.roundId,
                gameId = game.gameId,
                userUid = uid,
                publicUserId = _publicUserId.value,
                displayName = _displayName.value,
                avatarUrl = _avatarUrl.value,
                betTarget = betTarget,
                amount = amount
            )

            if (success) {
                onSuccess()
            } else {
                onError(message)
            }
        }
    }

    fun sendGameInvite(recipientUid: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val session = _activeSession.value
        val game = _activeGame.value
        val uid = _currentUserId.value

        if (session == null || game == null || uid.isBlank()) {
            onError("Session not available")
            return
        }

        viewModelScope.launch {
            val sent = gameRepository.sendGameInvitation(
                senderUid = uid,
                senderPublicId = _publicUserId.value,
                senderDisplayName = _displayName.value,
                senderAvatarUrl = _avatarUrl.value,
                recipientUid = recipientUid,
                gameId = game.gameId,
                gameName = game.name,
                sessionId = session.sessionId
            )
            if (sent) onSuccess() else onError("Failed to send invite")
        }
    }

    // ==========================================
    // ADMIN ACTIONS
    // ==========================================

    fun toggleGameStatus(gameId: String, newStatus: String, isEnabled: Boolean) {
        viewModelScope.launch {
            gameRepository.updateGameStatus(gameId, newStatus, isEnabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        leaveCurrentGame()
    }
}
