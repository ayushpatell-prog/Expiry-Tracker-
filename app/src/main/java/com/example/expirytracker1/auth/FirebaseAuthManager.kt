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

    fun updateProfile(
        fullName: String? = null,
        photoUri: android.net.Uri? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onFailure("User not logged in")
            return
        }

        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder().apply {
            fullName?.let { setDisplayName(it) }
            photoUri?.let { setPhotoUri(it) }
        }.build()

        user.updateProfile(profileUpdates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.localizedMessage ?: "Update failed") }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            onFailure("User session invalid")
            return
        }

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it.localizedMessage ?: "Password change failed") }
            }
            .addOnFailureListener { onFailure("Incorrect current password") }
    }

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