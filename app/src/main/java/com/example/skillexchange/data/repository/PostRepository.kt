package com.example.skillexchange.data.repository

import com.example.skillexchange.data.model.Post
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
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val postsCollection = firestore.collection("posts")

    suspend fun createPost(post: Post): Result<Unit> = runCatching {
        val docRef = postsCollection.document()
        val postWithId = post.copy(id = docRef.id)
        docRef.set(postWithId).await()
    }

    fun getPosts(): Flow<Resource<List<Post>>> = callbackFlow {
        val subscription = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to sync posts"))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val posts = it.toObjects(Post::class.java)
                    trySend(Resource.Success(posts))
                }
            }
        awaitClose { subscription.remove() }
    }
}
