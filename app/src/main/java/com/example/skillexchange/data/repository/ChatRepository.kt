package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Message
import com.example.skillexchange.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getMessages(threadId: String): Flow<Resource<List<Message>>> = callbackFlow {
        val subscription = firestore.collection("messages")
            .document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to sync messages"))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val messages = it.toObjects(Message::class.java)
                    trySend(Resource.Success(messages))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(threadId: String, message: Message): Result<Unit> = runCatching {
        firestore.collection("messages")
            .document(threadId)
            .collection("messages")
            .add(message)
            .await()
    }
}
