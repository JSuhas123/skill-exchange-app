package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwapRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val swapsCollection = firestore.collection("swaps")
    private val usersCollection = firestore.collection("users")

    fun getSwapsForUser(userId: String): Flow<Resource<List<Swap>>> = callbackFlow {
        val subscription = swapsCollection
            .whereArrayContains("users", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to sync swaps"))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(Resource.Success(it.toObjects(Swap::class.java)))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createSwap(swap: Swap): Result<Unit> = runCatching {
        val docRef = swapsCollection.document()
        docRef.set(swap.copy(id = docRef.id)).await()
    }

    suspend fun updateSwapStatus(swapId: String, status: String): Result<Unit> = runCatching {
        if (status == "cancelled") {
            cancelSwapWithPenalty(swapId)
        } else {
            swapsCollection.document(swapId).update("status", status).await()
        }
    }

    suspend fun confirmSwapAndProgress(swapId: String, isUserA: Boolean): Result<Unit> = runCatching {
        firestore.runTransaction { transaction ->
            val swapRef = swapsCollection.document(swapId)
            val swap = transaction.get(swapRef).toObject(Swap::class.java) ?: return@runTransaction

            val field = if (isUserA) "confirmedA" else "confirmedB"
            transaction.update(swapRef, field, true)

            val confirmedA = if (isUserA) true else swap.confirmedA
            val confirmedB = if (!isUserA) true else swap.confirmedB

            if (confirmedA && confirmedB && swap.status != "completed") {
                transaction.update(swapRef, "status", "completed")
                
                val userARef = usersCollection.document(swap.userA)
                val userBRef = usersCollection.document(swap.userB)
                
                transaction.update(userARef, "trustScore", FieldValue.increment(10))
                transaction.update(userBRef, "trustScore", FieldValue.increment(10))
                transaction.update(userARef, "skillPoints", FieldValue.increment(-swap.hours.toLong()))
                transaction.update(userBRef, "skillPoints", FieldValue.increment(swap.hours.toLong()))
            }
        }.await()
    }

    private suspend fun cancelSwapWithPenalty(swapId: String) {
        firestore.runTransaction { transaction ->
            val swapRef = swapsCollection.document(swapId)
            val swap = transaction.get(swapRef).toObject(Swap::class.java) ?: return@runTransaction
            
            if (swap.status == "completed" || swap.status == "cancelled") return@runTransaction

            transaction.update(swapRef, "status", "cancelled")
            
            val userARef = usersCollection.document(swap.userA)
            val userBRef = usersCollection.document(swap.userB)
            
            transaction.update(userARef, "trustScore", FieldValue.increment(-5))
            transaction.update(userBRef, "trustScore", FieldValue.increment(-5))
        }.await()
    }
}
