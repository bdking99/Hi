package com.example.utils

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleSignInUserData(
    val idToken: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

object GoogleSignInHelper {
    const val WEB_CLIENT_ID = "625968572161-u1km263o9v6tfm9kvcjuqt9970igfupr.apps.googleusercontent.com"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithCredentialManager(activity: ComponentActivity): Result<GoogleSignInUserData> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential

            when {
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    Result.success(
                        GoogleSignInUserData(
                            idToken = googleIdTokenCredential.idToken,
                            email = googleIdTokenCredential.id,
                            displayName = googleIdTokenCredential.displayName,
                            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                        )
                    )
                }
                else -> {
                    Result.failure(Exception("Unsupported credential type returned."))
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In cancelled."))
        } catch (e: GetCredentialException) {
            e.printStackTrace()
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(FirebaseAuthUtils.getFriendlyErrorMessage(e)))
        }
    }
}
