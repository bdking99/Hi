package com.example.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object FirebaseAuthUtils {

    fun getFriendlyErrorMessage(throwable: Throwable?): String {
        if (throwable == null) return "An unexpected error occurred. Please try again."

        val message = throwable.message ?: ""
        val errorString = throwable.toString()
        val errorCode = (throwable as? FirebaseAuthException)?.errorCode ?: ""

        when (throwable) {
            is FirebaseNetworkException -> {
                return "Network error. Please check your internet connection and try again."
            }
            is FirebaseTooManyRequestsException -> {
                return "Too many requests. Please wait a few moments before trying again."
            }
            is FirebaseAuthWeakPasswordException -> {
                return "Weak password. Password should be at least 6 characters."
            }
            is FirebaseAuthInvalidUserException -> {
                return when (throwable.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email. Please check your credentials or register."
                    "ERROR_USER_DISABLED" -> "This account has been disabled. Please contact support."
                    else -> "No user found with the provided credentials. Please register an account."
                }
            }
            is FirebaseAuthInvalidCredentialsException -> {
                return when {
                    errorCode == "ERROR_INVALID_PHONE_NUMBER" ||
                    message.contains("17042", ignoreCase = true) ||
                    message.contains("Invalid format", ignoreCase = true) ||
                    message.contains("phone number", ignoreCase = true) ->
                        "Invalid phone number format. Please ensure your number includes '+' and country code (e.g. +1 555-123-4567 or +52 1 234-567-8900)."

                    errorCode == "ERROR_WRONG_PASSWORD" || message.contains("password", ignoreCase = true) ->
                        "Incorrect password. Please check your password or use 'Forgot Password?' to reset it."

                    errorCode == "ERROR_INVALID_EMAIL" || message.contains("email", ignoreCase = true) ->
                        "Invalid email address format. Please enter a valid Gmail / email address."

                    errorCode == "ERROR_INVALID_VERIFICATION_CODE" ||
                    message.contains("sms code", ignoreCase = true) ||
                    message.contains("verification code", ignoreCase = true) ||
                    message.contains("17044", ignoreCase = true) ->
                        "Invalid OTP verification code. Please check the code and try again."

                    errorCode == "ERROR_SESSION_EXPIRED" || message.contains("expired", ignoreCase = true) ->
                        "The OTP verification code has expired. Please request a new code."

                    else -> "Invalid credentials provided. Please check your inputs and try again."
                }
            }
            is FirebaseAuthUserCollisionException -> {
                return "An account already exists with this email address or phone number. Please sign in instead."
            }
        }

        // Check specific error codes in message or toString
        return when {
            message.contains("17042", ignoreCase = true) ||
            message.contains("Invalid format", ignoreCase = true) ||
            message.contains("INVALID_PHONE_NUMBER", ignoreCase = true) ->
                "Invalid phone number format. Please ensure your number includes '+' and country code (e.g. +1 555-123-4567 or +52 1 234-567-8900)."

            message.contains("17010", ignoreCase = true) ||
            message.contains("TOO_MANY_REQUESTS", ignoreCase = true) ||
            message.contains("too-many-requests", ignoreCase = true) ->
                "Too many attempts. Please wait a few moments before trying again."

            message.contains("17044", ignoreCase = true) ||
            message.contains("QUOTA_EXCEEDED", ignoreCase = true) ||
            message.contains("quota-exceeded", ignoreCase = true) ->
                "SMS verification limit reached. Please sign in with Gmail & Password or use Continue with Google."

            message.contains("INVALID_CERT_HASH", ignoreCase = true) ||
            message.contains("Recaptcha-14", ignoreCase = true) ||
            message.contains("Play Integrity", ignoreCase = true) ||
            message.contains("reCAPTCHA", ignoreCase = true) ||
            message.contains("app-not-authorized", ignoreCase = true) ->
                "Phone SMS verification could not be verified on this device/emulator. Please sign in using Gmail & Password or Continue with Google."

            message.contains("The email address is badly formatted", ignoreCase = true) ||
            message.contains("badly formatted", ignoreCase = true) ||
            message.contains("invalid-email", ignoreCase = true) ->
                "Invalid email format. Please enter a valid Gmail / email address."

            message.contains("The email address is already in use", ignoreCase = true) ||
            message.contains("email-already-in-use", ignoreCase = true) ->
                "An account with this email already exists. Please log in or use another email."

            message.contains("phone-number-already-exists", ignoreCase = true) ||
            message.contains("phone number is already in use", ignoreCase = true) ->
                "This phone number is already registered. Please sign in with your phone number."

            message.contains("user-not-found", ignoreCase = true) ||
            message.contains("USER_NOT_FOUND", ignoreCase = true) ->
                "No account found with this email. Please register an account."

            message.contains("wrong-password", ignoreCase = true) ||
            message.contains("WRONG_PASSWORD", ignoreCase = true) ->
                "Incorrect password. Please try again or tap 'Forgot Password?'."

            message.contains("weak-password", ignoreCase = true) ||
            message.contains("WEAK_PASSWORD", ignoreCase = true) ->
                "Password is too weak. Please use at least 6 characters."

            message.contains("invalid-verification-code", ignoreCase = true) ||
            message.contains("invalid verification code", ignoreCase = true) ->
                "Invalid OTP code. Please check the 6-digit code and try again."

            message.contains("session-expired", ignoreCase = true) ||
            message.contains("sms-code-expired", ignoreCase = true) ->
                "The OTP code has expired. Please tap 'Resend' to receive a new code."

            message.contains("network", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("UNAVAILABLE", ignoreCase = true) ->
                "Network connection issue. Please check your internet connection."

            message.contains("Username is already taken", ignoreCase = true) ->
                "Username already taken. Please choose another username."

            message.contains("cancelled", ignoreCase = true) ||
            message.contains("canceled", ignoreCase = true) ->
                "Google Sign-In was cancelled."

            message.isNotBlank() && !message.startsWith("unknown status code") ->
                message

            else ->
                "Authentication failed. Please verify your credentials and try again."
        }
    }
}
