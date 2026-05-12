package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Post
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
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(post: Post): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Creating post by user ${post.userId}")
        val docRef = postsCollection.document()
        val postWithId = post.copy(id = docRef.id)
        docRef.set(postWithId).await()
        Timber.d("Post created with ID: ${docRef.id}")
    }.onFailure { e ->
        Timber.e(e, "Failed to create post")
    }

    fun getPosts(): Flow<Resource<List<Post>>> = callbackFlow {
        try {
            Timber.d("Setting up posts listener")
            
            val subscription = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100) // Limit for better performance
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to sync posts")
                        trySend(Resource.Error(error.message ?: "Failed to sync posts"))
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val posts = it.toObjects(Post::class.java)
                        Timber.d("Fetched ${posts.size} posts")
                        trySend(Resource.Success(posts))
                    }
                }
            
            awaitClose {
                subscription.remove()
                Timber.d("Posts listener removed")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up posts listener")
            trySend(Resource.Error(e.message ?: "Error setting up listener"))
            close(e)
        }
    }
    
    fun getPostsByUser(userId: String): Flow<Resource<List<Post>>> = callbackFlow {
        try {
            Timber.d("Setting up posts listener for user: $userId")
            
            val subscription = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to sync posts for user $userId")
                        trySend(Resource.Error(error.message ?: "Failed to sync posts"))
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val posts = it.toObjects(Post::class.java)
                        Timber.d("Fetched ${posts.size} posts for user $userId")
                        trySend(Resource.Success(posts))
                    }
                }
            
            awaitClose {
                subscription.remove()
                Timber.d("Posts listener removed for user: $userId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up user posts listener")
            trySend(Resource.Error(e.message ?: "Error setting up listener"))
            close(e)
        }
    }
    
    suspend fun getPostsBySkill(skillRequired: String): Result<List<Post>> = ErrorHandler.withRetry {
        Timber.d("Getting posts for skill: $skillRequired")
        
        val snapshot = postsCollection
            .whereEqualTo("skillRequired", skillRequired)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        
        val posts = snapshot.toObjects(Post::class.java)
        Timber.d("Found ${posts.size} posts for skill: $skillRequired")
        posts
    }.onFailure { e ->
        Timber.e(e, "Failed to get posts by skill")
    }
    
    suspend fun deletePost(postId: String): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Deleting post: $postId")
        postsCollection.document(postId).delete().await()
        Timber.d("Post deleted successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to delete post")
    }
    
    suspend fun updatePost(post: Post): Result<Unit> = ErrorHandler.withRetry {
        Timber.d("Updating post: ${post.id}")
        postsCollection.document(post.id).set(post).await()
        Timber.d("Post updated successfully")
    }.onFailure { e ->
        Timber.e(e, "Failed to update post")
    }
}
