package com.example.skillexchange.data.repository

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        const val PHONE_VERIFICATION_TIMEOUT_SECONDS = 60L
    }

    private val sessionIdKey = stringPreferencesKey("session_id")
    private val userIdKey = stringPreferencesKey("user_id")
    
    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser
    
    val sessionId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[sessionIdKey]
    }
    
    suspend fun isSessionValid(): Boolean {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Refresh ID token to ensure session is still valid
                user.getIdToken(false).await()
                Timber.d("Session validation successful for user ${user.uid}")
                true
            } else {
                Timber.d("No active user session")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Session validation failed")
            false
        }
    }
    
    suspend fun signInAnonymously(): Result<FirebaseUser?> {
        return try {
            Timber.d("Attempting anonymous sign in")
            val result = firebaseAuth.signInAnonymously().await()
            val user = result.user
            
            if (user != null) {
                // Save session info
                dataStore.edit { preferences ->
                    preferences[sessionIdKey] = user.uid
                    preferences[userIdKey] = user.uid
                }
                // Create user profile if it doesn't exist
                ensureUserExists(user.uid).getOrNull()
                Timber.d("Anonymous sign in successful for user ${user.uid}")
                Result.success(user)
            } else {
                Timber.e("Anonymous sign in returned null user")
                Result.failure(Exception("Sign in returned null user"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Anonymous sign in failed")
            Result.failure(e)
        }
    }

    fun startPhoneNumberVerification(
        activity: Activity,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(PHONE_VERIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        userName: String,
        phoneNumber: String
    ): Result<FirebaseUser?> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                dataStore.edit { preferences ->
                    preferences[sessionIdKey] = user.uid
                    preferences[userIdKey] = user.uid
                }
                ensureUserExists(user.uid, userName, phoneNumber).getOrThrow()
                Result.success(user)
            } else {
                Result.failure(Exception("Phone sign in returned null user"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Phone sign in failed")
            Result.failure(e)
        }
    }
    
    suspend fun ensureUserExists(
        userId: String,
        userName: String = "Anonymous",
        phoneNumber: String = ""
    ): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            val snapshot = userRef.get().await()
            
            if (!snapshot.exists()) {
                val userData = mapOf(
                    "id" to userId,
                    "name" to userName,
                    "email" to "",
                    "phoneNumber" to phoneNumber,
                    "skillsOffered" to emptyList<String>(),
                    "skillsNeeded" to emptyList<String>(),
                    "trustScore" to 0,
                    "skillPoints" to 10,
                    "createdAt" to System.currentTimeMillis()
                )
                userRef.set(userData).await()
                Timber.d("Created user profile for $userId")
            } else {
                val updates = mutableMapOf<String, Any>()
                val existingName = snapshot.getString("name").orEmpty()
                val existingPhone = snapshot.getString("phoneNumber").orEmpty()
                if (userName.isNotBlank() && existingName != userName) {
                    updates["name"] = userName
                }
                if (phoneNumber.isNotBlank() && existingPhone != phoneNumber) {
                    updates["phoneNumber"] = phoneNumber
                }
                if (updates.isNotEmpty()) {
                    userRef.set(updates, SetOptions.merge()).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to ensure user exists")
            Result.failure(e)
        }
    }
    
    suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            dataStore.edit { preferences ->
                preferences.remove(sessionIdKey)
                preferences.remove(userIdKey)
            }
            Timber.d("Sign out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sign out failed")
            Result.failure(e)
        }
    }
    
    suspend fun recoverSession(): Result<FirebaseUser?> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                Timber.d("Session recovered for user ${user.uid}")
                Result.success(user)
            } else {
                Timber.d("No session to recover")
                Result.success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Session recovery failed")
            Result.failure(e)
        }
    }
}
