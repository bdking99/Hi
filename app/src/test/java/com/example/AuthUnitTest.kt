package com.example

import com.example.utils.FirebaseAuthUtils
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUnitTest {

    @Test
    fun `test friendly error message for weak password`() {
        val exception = FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Password should be at least 6 characters", null)
        val message = FirebaseAuthUtils.getFriendlyErrorMessage(exception)
        assertEquals("Weak password. Password should be at least 6 characters.", message)
    }

    @Test
    fun `test friendly error message for user collision`() {
        val exception = FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "The email address is already in use by another account.")
        val message = FirebaseAuthUtils.getFriendlyErrorMessage(exception)
        assertEquals("An account already exists with this email address or phone number.", message)
    }

    @Test
    fun `test friendly error message for invalid user`() {
        val exception = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "There is no user record corresponding to this identifier.")
        val message = FirebaseAuthUtils.getFriendlyErrorMessage(exception)
        assertEquals("No account found with this email or phone number.", message)
    }

    @Test
    fun `test friendly error message for wrong password`() {
        val exception = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "The password is invalid")
        val message = FirebaseAuthUtils.getFriendlyErrorMessage(exception)
        assertEquals("Wrong password. Please check your password and try again.", message)
    }

    @Test
    fun `test friendly error message for username taken`() {
        val exception = Exception("Username is already taken. Please choose another.")
        val message = FirebaseAuthUtils.getFriendlyErrorMessage(exception)
        assertTrue(message.contains("Username already taken"))
    }
}
