package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.User
import com.example.skillexchange.utils.ErrorHandler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getUser(userId: String): Result<User?> = ErrorHandler.withRetry {
        Timber.d("Fetching user: $userId")
        val user = usersCollection.document(userId).get().await().toObject(User::class.java)
        Timber.d("User fetched successfully: $userId")
        user
    }.onFailure { e ->
        Timber.e(e, "Failed to fetch user: $userId")
    }
    
    fun getUserFlow(userId: String): Flow<Result<User?>> = flow {
        try {
            val listener = usersCollection.document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to listen to user: $userId")
                        emit(Result.failure(error))
                        return@addSnapshotListener
                    }
                    
                    snapshot?.let {
                        val user = it.toObject(User::class.java)
                        Timber.d("User snapshot updated: $userId")
                        emit(Result.success(user))
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up user listener")
            emit(Result.failure(e))
        }
    }

    suspend fun saveUser(user: User): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Saving user: ${user.id}")
        usersCollection.document(user.id).set(user).await()
        Timber.d("User saved successfully: ${user.id}")
    }.onFailure { e ->
        Timber.e(e, "Failed to save user: ${user.id}")
    }
    
    suspend fun createUser(user: User): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Creating new user: ${user.id}")
        
        val snapshot = usersCollection.document(user.id).get().await()
        if (snapshot.exists()) {
            throw IllegalStateException("User already exists: ${user.id}")
        }
        
        usersCollection.document(user.id).set(user).await()
        Timber.d("User created successfully: ${user.id}")
    }.onFailure { e ->
        Timber.e(e, "Failed to create user: ${user.id}")
    }
    
    suspend fun updateUserField(userId: String, field: String, value: Any): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Updating user field: $userId.$field")
        usersCollection.document(userId).update(field, value).await()
        Timber.d("User field updated successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to update user field")
    }
    
    suspend fun deleteUser(userId: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Deleting user: $userId")
        usersCollection.document(userId).delete().await()
        Timber.d("User deleted successfully: $userId")
    }.onFailure { e ->
        Timber.e(e, "Failed to delete user: $userId")
    }
    
    suspend fun userExists(userId: String): Result<Boolean> = ErrorHandler.withRetry {
        Timber.d("Checking if user exists: $userId")
        val snapshot = usersCollection.document(userId).get().await()
        val exists = snapshot.exists()
        Timber.d("User exists: $userId = $exists")
        exists
    }.onFailure { e ->
        Timber.e(e, "Failed to check user existence")
    }
}
