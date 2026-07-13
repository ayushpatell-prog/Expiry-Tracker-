package com.example.expirytracker1.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.FirebaseApp

object FirebaseAuthManager {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun initializeAppCheck(context: android.content.Context) {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            android.util.Log.e("AUTH_MANAGER", "AppCheck init failed", e)
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()

                result.user?.updateProfile(profileUpdates)
                    ?.addOnCompleteListener {
                        onSuccess()
                    }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("AUTH_ERROR", "Signup failed", exception)
                onFailure(exception.localizedMessage ?: "Signup failed")
            }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Login failed")
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUser() = auth.currentUser

    fun resetPassword(
        email: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onFailure(it.localizedMessage ?: "Failed to send reset email")
            }
    }
}