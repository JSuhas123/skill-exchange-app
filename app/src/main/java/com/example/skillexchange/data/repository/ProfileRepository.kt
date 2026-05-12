package com.example.skillexchange.data.repository

import android.net.Uri
import com.example.skillexchange.data.model.User
import com.example.skillexchange.utils.ErrorHandler
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val usersCollection = firestore.collection("users")
    private val profileImagesRef = storage.reference.child("profile_images")
    
    suspend fun getProfile(userId: String): Result<User?> = runCatching {
        usersCollection.document(userId).get().await().toObject(User::class.java)
    }
    
    suspend fun updateProfile(user: User): Result<Unit> = runCatching {
        usersCollection.document(user.id).set(user).await()
        Timber.d("Profile updated for ${user.id}")
    }.onFailure { e ->
        Timber.e(e, "Failed to update profile")
    }
    
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = runCatching {
        val imagePath = "users/$userId/profile_${System.currentTimeMillis()}.jpg"
        val imageRef = profileImagesRef.child(imagePath)
        
        imageRef.putFile(imageUri).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()
        
        Timber.d("Profile image uploaded: $downloadUrl")
        downloadUrl
    }.onFailure { e ->
        Timber.e(e, "Failed to upload profile image")
    }
    
    suspend fun deleteProfileImage(imagePath: String): Result<Unit> = runCatching {
        storage.reference.child(imagePath).delete().await()
        Timber.d("Profile image deleted: $imagePath")
    }.onFailure { e ->
        Timber.e(e, "Failed to delete profile image")
    }
    
    suspend fun updateProfilePicture(userId: String, imageUri: Uri): Result<Unit> = runCatching {
        val downloadUrl = uploadProfileImage(userId, imageUri).getOrThrow()
        val userRef = usersCollection.document(userId)
        userRef.update("profilePictureUrl", downloadUrl).await()
        Timber.d("Profile picture URL updated")
    }.onFailure { e ->
        Timber.e(e, "Failed to update profile picture")
    }
}
