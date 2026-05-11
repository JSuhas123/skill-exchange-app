package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getUser(userId: String): Result<User?> = runCatching {
        usersCollection.document(userId).get().await().toObject(User::class.java)
    }

    suspend fun saveUser(user: User): Result<Unit> = runCatching {
        usersCollection.document(user.id).set(user).await()
    }
}
