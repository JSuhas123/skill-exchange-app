package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.utils.ErrorHandler
import com.example.skillexchange.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwapRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val swapsCollection = firestore.collection("swaps")
    private val usersCollection = firestore.collection("users")

    fun getSwapsForUser(userId: String): Flow<Resource<List<Swap>>> = callbackFlow {
        try {
            val subscription = swapsCollection
                .whereArrayContains("users", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100) // Limit for better performance
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to sync swaps for user $userId")
                        trySend(Resource.Error(error.message ?: "Failed to sync swaps", error))
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val swaps = it.toObjects(Swap::class.java)
                        Timber.d("Fetched ${swaps.size} swaps for user $userId")
                        trySend(Resource.Success(swaps))
                    }
                }
            awaitClose {
                subscription.remove()
                Timber.d("Swap listener removed for user $userId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up swap listener")
            emit(Resource.Error(e.message ?: "Error setting up listener", e))
        }
    }

    suspend fun createSwap(swap: Swap): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Creating swap between ${swap.userA} and ${swap.userB}")
        val docRef = swapsCollection.document()
        docRef.set(swap.copy(id = docRef.id)).await()
        Timber.d("Swap created with ID: ${docRef.id}")
    }.onFailure { e ->
        Timber.e(e, "Failed to create swap")
    }

    suspend fun updateSwapStatus(swapId: String, status: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Updating swap $swapId status to $status")
        if (status == "cancelled") {
            cancelSwapWithPenalty(swapId)
        } else {
            swapsCollection.document(swapId).update("status", status).await()
        }
        Timber.d("Swap status updated successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to update swap status")
    }

    suspend fun confirmSwapAndProgress(swapId: String, isUserA: Boolean): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Confirming swap completion for swap $swapId, isUserA: $isUserA")
        
        firestore.runTransaction { transaction ->
            val swapRef = swapsCollection.document(swapId)
            val swapSnapshot = transaction.get(swapRef)
            val swap = swapSnapshot.toObject(Swap::class.java)
            
            if (swap == null) {
                Timber.e("Swap not found: $swapId")
                throw IllegalStateException("Swap not found")
            }
            
            // Prevent duplicate confirmations
            val field = if (isUserA) "confirmedA" else "confirmedB"
            val alreadyConfirmed = if (isUserA) swap.confirmedA else swap.confirmedB
            
            if (alreadyConfirmed) {
                Timber.w("User already confirmed this swap: $swapId")
                return@runTransaction // Idempotent - no-op if already confirmed
            }
            
            // Update confirmation for current user
            transaction.update(swapRef, field, true)
            
            val confirmedA = if (isUserA) true else swap.confirmedA
            val confirmedB = if (!isUserA) true else swap.confirmedB
            
            // Only progress to completion if both users confirm and swap is not already completed
            if (confirmedA && confirmedB && swap.status != "completed") {
                Timber.d("Both users confirmed, marking swap as completed")
                transaction.update(swapRef, "status", "completed")
                
                // Atomic update of trust scores and skill points
                val userARef = usersCollection.document(swap.userA)
                val userBRef = usersCollection.document(swap.userB)
                
                transaction.update(userARef, "trustScore", FieldValue.increment(10))
                transaction.update(userBRef, "trustScore", FieldValue.increment(10))
                transaction.update(userARef, "skillPoints", FieldValue.increment(-swap.hours.toLong()))
                transaction.update(userBRef, "skillPoints", FieldValue.increment(swap.hours.toLong()))
                
                Timber.d("Swap completed and user scores updated")
            }
        }.await()
        
        Timber.d("Swap confirmation completed successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to confirm swap")
    }

    suspend fun cancelSwapWithPenalty(swapId: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Cancelling swap $swapId with penalty")
        
        firestore.runTransaction { transaction ->
            val swapRef = swapsCollection.document(swapId)
            val swapSnapshot = transaction.get(swapRef)
            val swap = swapSnapshot.toObject(Swap::class.java)
            
            if (swap == null) {
                Timber.e("Swap not found for cancellation: $swapId")
                return@runTransaction
            }
            
            // Don't apply penalty if already completed or cancelled
            if (swap.status == "completed" || swap.status == "cancelled") {
                Timber.d("Swap already in terminal state: ${swap.status}")
                return@runTransaction
            }
            
            transaction.update(swapRef, "status", "cancelled")
            
            // Apply penalty to both users
            val userARef = usersCollection.document(swap.userA)
            val userBRef = usersCollection.document(swap.userB)
            
            transaction.update(userARef, "trustScore", FieldValue.increment(-5))
            transaction.update(userBRef, "trustScore", FieldValue.increment(-5))
            
            Timber.d("Swap cancelled and penalties applied")
        }.await()
        
        Timber.d("Swap cancellation completed successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to cancel swap")
    }
    
    suspend fun acceptSwap(swapId: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Accepting swap $swapId")
        swapsCollection.document(swapId).update("status", "accepted").await()
        Timber.d("Swap accepted successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to accept swap")
    }
}

