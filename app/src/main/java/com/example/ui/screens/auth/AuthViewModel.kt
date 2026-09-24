package com.example.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FirebaseRtdbUser
import com.example.data.repository.AuthRepository
import com.example.utils.FirebaseAuthUtils
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _otpVerificationId = MutableStateFlow<String?>(null)
    val otpVerificationId: StateFlow<String?> = _otpVerificationId.asStateFlow()

    private val _resendToken = MutableStateFlow<PhoneAuthProvider.ForceResendingToken?>(null)

    /**
     * Unified Login: Dispatches to Email/Password or Phone OTP based on input
     */
    fun login(emailOrPhone: String, passwordRaw: String, activity: Activity? = null) {
        val input = emailOrPhone.trim()
        if (input.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your Gmail or Phone Number.")
            return
        }

        if (isEmailAddress(input)) {
            // Email/Password login
            if (passwordRaw.isBlank()) {
                _uiState.value = AuthUiState.Error("Please enter your password.")
                return
            }
            _uiState.value = AuthUiState.Loading
            viewModelScope.launch {
                val result = authRepository.loginWithEmail(input, passwordRaw)
                if (result.isSuccess) {
                    _uiState.value = AuthUiState.Success(result.getOrThrow())
                } else {
                    _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
                }
            }
        } else if (input.startsWith("+") || input.any { it.isDigit() }) {
            // Phone Number login -> Send OTP
            if (activity == null) {
                _uiState.value = AuthUiState.Error("Unable to initiate phone authentication on this device.")
                return
            }
            if (!isValidPhoneNumber(input)) {
                _uiState.value = AuthUiState.Error("Invalid phone number format. Please enter a valid number with country code (e.g. +1 555-123-4567 or +52 1 234-567-8900).")
                return
            }
            val formattedPhone = formatPhoneNumber(input)
            sendPhoneOtp(formattedPhone, activity)
        } else {
            _uiState.value = AuthUiState.Error("Please enter a valid Gmail address (e.g. name@gmail.com) or Phone Number (e.g. +1234567890).")
        }
    }

    /**
     * Sends OTP for Firebase Phone Authentication
     */
    fun sendPhoneOtp(phoneNumber: String, activity: Activity) {
        if (!isValidPhoneNumber(phoneNumber)) {
            _uiState.value = AuthUiState.Error("Invalid phone number. Please include '+' and country code (e.g. +1 555-123-4567 or +52 1 234-567-8900).")
            return
        }
        val cleanPhone = formatPhoneNumber(phoneNumber)

        _uiState.value = AuthUiState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                val code = credential.smsCode
                val id = _otpVerificationId.value
                if (id != null && !code.isNullOrBlank()) {
                    verifyPhoneOtp(id, code)
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.value = AuthUiState.Error(FirebaseAuthUtils.getFriendlyErrorMessage(e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                _otpVerificationId.value = verificationId
                _resendToken.value = token
                _uiState.value = AuthUiState.OtpCodeSent(verificationId, cleanPhone)
            }
        }

        try {
            authRepository.sendPhoneVerificationCode(cleanPhone, activity, callbacks)
        } catch (e: Exception) {
            _uiState.value = AuthUiState.Error(FirebaseAuthUtils.getFriendlyErrorMessage(e))
        }
    }

    /**
     * Verifies OTP Code and Signs In / Registers Phone User
     */
    fun verifyPhoneOtp(
        verificationId: String,
        smsCode: String,
        name: String? = null,
        username: String? = null,
        email: String? = null,
        photoUrl: String? = null
    ) {
        val code = smsCode.trim()
        if (code.isBlank() || code.length < 6) {
            _uiState.value = AuthUiState.Error("Please enter the complete 6-digit OTP code.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.verifyPhoneOtpAndSignIn(
                verificationId = verificationId,
                smsCode = code,
                name = name,
                username = username,
                email = email,
                photoUrl = photoUrl
            )
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "OTP verification failed")
            }
        }
    }

    /**
     * Full Registration with Validation
     */
    fun register(
        name: String,
        username: String,
        email: String,
        phone: String,
        passwordRaw: String,
        confirmPassword: String,
        photoUrl: String? = null
    ) {
        val cleanName = name.trim()
        val cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        val cleanEmail = email.trim()
        val cleanPhone = phone.trim()

        // Validation Rules
        if (cleanName.isBlank()) {
            _uiState.value = AuthUiState.Error("Name is required.")
            return
        }
        if (cleanUsername.isBlank()) {
            _uiState.value = AuthUiState.Error("Username is required.")
            return
        }
        if (cleanUsername.length < 3) {
            _uiState.value = AuthUiState.Error("Username must be at least 3 characters.")
            return
        }
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid Gmail / email address.")
            return
        }
        if (cleanPhone.isBlank() || !isValidPhoneNumber(cleanPhone)) {
            _uiState.value = AuthUiState.Error("Please enter a valid phone number with country code (e.g. +1 555-123-4567 or +52 1 234-567-8900).")
            return
        }
        if (passwordRaw.isBlank()) {
            _uiState.value = AuthUiState.Error("Password is required.")
            return
        }
        if (passwordRaw.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }
        if (passwordRaw != confirmPassword) {
            _uiState.value = AuthUiState.Error("Confirm Password must exactly match Password.")
            return
        }

        val formattedPhone = formatPhoneNumber(cleanPhone)
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.registerWithEmail(
                name = cleanName,
                username = cleanUsername,
                email = cleanEmail,
                phone = formattedPhone,
                passwordRaw = passwordRaw,
                photoUrl = photoUrl
            )
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    /**
     * Firebase Google Sign-In with ID Token
     */
    fun signInWithGoogle(
        idToken: String,
        email: String? = null,
        displayName: String? = null,
        photoUrl: String? = null
    ) {
        if (idToken.isBlank()) {
            _uiState.value = AuthUiState.Error("Google Sign-In failed. No ID token received.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken, email, displayName, photoUrl)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success(result.getOrThrow())
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Google Sign-In failed")
            }
        }
    }

    /**
     * Sends password reset email
     */
    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address to reset password.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(cleanEmail)
            if (result.isSuccess) {
                _uiState.value = AuthUiState.ResetEmailSent("Password reset link sent to $cleanEmail. Please check your inbox.")
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.message ?: "Failed to send reset email.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun isEmailAddress(input: String): Boolean {
        return input.contains("@")
    }

    fun formatPhoneNumber(input: String): String {
        val clean = input.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        return when {
            clean.startsWith("+") -> clean
            clean.startsWith("00") -> "+" + clean.substring(2)
            clean.length == 10 && clean[0] in '2'..'9' -> "+1$clean"
            clean.length == 11 && clean.startsWith("1") -> "+$clean"
            else -> "+$clean"
        }
    }

    fun isValidPhoneNumber(input: String): Boolean {
        val formatted = formatPhoneNumber(input)
        val regex = Regex("^\\+[1-9]\\d{6,14}$")
        return regex.matches(formatted)
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class OtpCodeSent(val verificationId: String, val phoneNumber: String) : AuthUiState()
    data class ResetEmailSent(val message: String) : AuthUiState()
    data class Success(val user: FirebaseRtdbUser) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
