package com.example.skillexchange.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATASTORE_NAME = "skillexchange_preferences"

// Create dataStore with error handling
private fun createDataStore(context: Context): DataStore<Preferences> {
    return try {
        context.dataStore
    } catch (e: Exception) {
        Log.e("AppModule", "Failed to initialize DataStore, clearing cache", e)
        try {
            // Clear corrupted cache
            val dataStoreFile = context.getDir("datastore", Context.MODE_PRIVATE)
            val preferencesFile = java.io.File(dataStoreFile, "skillexchange_preferences.preferences_pb")
            if (preferencesFile.exists()) {
                preferencesFile.delete()
                Log.i("AppModule", "Cleared corrupted DataStore file")
            }
            context.dataStore
        } catch (retryException: Exception) {
            Log.e("AppModule", "Failed to recover DataStore after cache clear", retryException)
            throw retryException
        }
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        Log.e("AppModule", "Failed to initialize FirebaseAuth", e)
        throw e
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            firestore.firestoreSettings = settings
            firestore
        } catch (e: Exception) {
            Log.e("AppModule", "Failed to initialize Firestore", e)
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = try {
        FirebaseStorage.getInstance()
    } catch (e: Exception) {
        Log.e("AppModule", "Failed to initialize FirebaseStorage", e)
        throw e
    }
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return createDataStore(context)
    }
}
