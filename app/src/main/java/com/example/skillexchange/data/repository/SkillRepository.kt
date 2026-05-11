package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Skill
import com.example.skillexchange.utils.ErrorHandler
import com.example.skillexchange.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkillRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val skillsCollection = firestore.collection("skills")

    fun getSkills(): Flow<Resource<List<Skill>>> = callbackFlow {
        try {
            Timber.d("Setting up skills listener")
            
            val subscription = skillsCollection
                .orderBy("name")
                .limit(200) // Limit for better performance
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to sync skills")
                        trySend(Resource.Error(error.message ?: "Failed to sync skills", error))
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val skills = it.toObjects(Skill::class.java)
                        Timber.d("Fetched ${skills.size} skills")
                        trySend(Resource.Success(skills))
                    }
                }
            
            awaitClose {
                subscription.remove()
                Timber.d("Skills listener removed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up skills listener")
            emit(Resource.Error(e.message ?: "Error setting up listener", e))
        }
    }
    
    fun getSkillsByCategory(category: String): Flow<Resource<List<Skill>>> = callbackFlow {
        try {
            Timber.d("Setting up skills listener for category: $category")
            
            val subscription = skillsCollection
                .whereEqualTo("category", category)
                .orderBy("name")
                .limit(200)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to sync skills for category $category")
                        trySend(Resource.Error(error.message ?: "Failed to sync skills", error))
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val skills = it.toObjects(Skill::class.java)
                        Timber.d("Fetched ${skills.size} skills for category $category")
                        trySend(Resource.Success(skills))
                    }
                }
            
            awaitClose {
                subscription.remove()
                Timber.d("Skills listener removed for category: $category")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up category skills listener")
            emit(Resource.Error(e.message ?: "Error setting up listener", e))
        }
    }

    suspend fun addSkill(skill: Skill): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Adding skill: ${skill.name}")
        skillsCollection.add(skill).await()
        Timber.d("Skill added successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to add skill")
    }
    
    suspend fun updateSkill(skill: Skill): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Updating skill: ${skill.id}")
        skillsCollection.document(skill.id).set(skill).await()
        Timber.d("Skill updated successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to update skill")
    }
    
    suspend fun deleteSkill(skillId: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Deleting skill: $skillId")
        skillsCollection.document(skillId).delete().await()
        Timber.d("Skill deleted successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to delete skill")
    }
    
    suspend fun searchSkills(query: String): Result<List<Skill>> = ErrorHandler.withRetry {
        Timber.d("Searching skills with query: $query")
        val snapshot = skillsCollection
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThan("name", query + "\uf8ff")
            .limit(50)
            .get()
            .await()
        val skills = snapshot.toObjects(Skill::class.java)
        Timber.d("Found ${skills.size} skills for query: $query")
        skills
    }.onFailure { e ->
        Timber.e(e, "Failed to search skills")
    }
}
