package com.example.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.domain.usecase.SeedDatabaseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val seedDatabaseUseCase: SeedDatabaseUseCase
) : ViewModel() {

    private val _hasSession = MutableStateFlow<Boolean?>(null)
    val hasSession: StateFlow<Boolean?> = _hasSession.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            seedDatabaseUseCase.execute()
            val token = authRepository.currentSessionToken.firstOrNull()
            if (token != null) {
                val session = authRepository.getCurrentSession(token)
                _hasSession.value = session != null
            } else {
                _hasSession.value = false
            }
        }
    }
}
