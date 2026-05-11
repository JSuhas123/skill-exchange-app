package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Skill
import com.example.skillexchange.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val skillsCollection = firestore.collection("skills")

    fun getSkills(): Flow<Resource<List<Skill>>> = callbackFlow {
        val subscription = skillsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to sync skills"))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val skills = it.toObjects(Skill::class.java)
                    trySend(Resource.Success(skills))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addSkill(skill: Skill): Result<Unit> = runCatching {
        skillsCollection.add(skill).await()
    }
}
