package com.example.expirytracker1.screens

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.util.Log
import com.example.expirytracker1.auth.FirebaseAuthManager

object GoogleAuthHandler {
    private const val WEB_CLIENT_ID = "661895060089-ov5judiu5krntteklq64n3ksmjde7ofe.apps.googleusercontent.com" // Placeholder - requires user to provide real ID

    suspend fun signIn(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )
            
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                FirebaseAuthManager.loginWithGoogle(
                    idToken = googleIdTokenCredential.idToken,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } else if (credential is GoogleIdTokenCredential) {
                FirebaseAuthManager.loginWithGoogle(
                    idToken = credential.idToken,
                    onSuccess = onSuccess,
                    onFailure = onFailure
                )
            } else {
                Log.e("GOOGLE_AUTH", "Unexpected credential type: ${credential.type}")
                onFailure("Unexpected credential type: ${credential.type}")
            }
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH", "Sign in failed", e)
            onFailure(e.localizedMessage ?: "Google Sign In failed")
        }
    }
}
