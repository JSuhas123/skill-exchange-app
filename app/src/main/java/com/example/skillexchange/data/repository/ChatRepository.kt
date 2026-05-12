package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Message
import com.example.skillexchange.utils.ErrorHandler
import com.example.skillexchange.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getThreadPath(threadId: String) = "messages/$threadId/messages"
    
    fun getMessages(threadId: String): Flow<Resource<List<Message>>> = callbackFlow {
        try {
            Timber.d("Setting up message listener for thread: $threadId")
            
            val subscription = firestore.collection(getThreadPath(threadId))
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .limit(100) // Limit for better performance
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to sync messages for thread $threadId")
                        trySend(Resource.Error(error.message ?: "Failed to sync messages"))
                        return@addSnapshotListener
                    }
                    
                    snapshot?.let {
                        val messages = it.toObjects(Message::class.java)
                            .sortedBy { msg -> msg.timestamp.seconds } // Ensure proper ordering
                        Timber.d("Fetched ${messages.size} messages for thread $threadId")
                        trySend(Resource.Success(messages))
                    }
                }
            
            awaitClose {
                subscription.remove()
                Timber.d("Message listener removed for thread $threadId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up message listener")
            trySend(Resource.Error(e.message ?: "Error setting up listener"))
            close(e)
        }
    }

    suspend fun sendMessage(threadId: String, message: Message): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Sending message to thread $threadId from user ${message.senderId}")
        
        firestore.collection(getThreadPath(threadId))
            .add(message)
            .await()
        
        Timber.d("Message sent successfully to thread $threadId")
    }.onFailure { e ->
        Timber.e(e, "Failed to send message to thread $threadId")
    }
    
    suspend fun deleteMessage(threadId: String, messageId: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Deleting message $messageId from thread $threadId")
        
        firestore.collection(getThreadPath(threadId))
            .document(messageId)
            .delete()
            .await()
        
        Timber.d("Message deleted successfully from thread $threadId")
    }.onFailure { e ->
        Timber.e(e, "Failed to delete message from thread $threadId")
    }
    
    suspend fun getMessageCount(threadId: String): Result<Int> = ErrorHandler.withRetry {
        Timber.d("Getting message count for thread $threadId")
        
        val count = firestore.collection(getThreadPath(threadId))
            .get()
            .await()
            .size()
        
        Timber.d("Thread $threadId has $count messages")
        count
    }.onFailure { e ->
        Timber.e(e, "Failed to get message count")
    }
    
    suspend fun clearOldMessages(threadId: String, daysOld: Int): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Clearing messages older than $daysOld days from thread $threadId")
        
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000)
        val oldMessages = firestore.collection(getThreadPath(threadId))
            .whereLessThan("timestamp", cutoffTime)
            .get()
            .await()
        
        val batch = firestore.batch()
        for (doc in oldMessages.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
        
        Timber.d("Cleared ${oldMessages.size()} old messages from thread $threadId")
    }.onFailure { e ->
        Timber.e(e, "Failed to clear old messages")
    }
}
