package com.example.skillexchange.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.skillexchange.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        dataStore: DataStore<Preferences>
    ): AuthRepository = AuthRepository(auth, firestore, dataStore)

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore): UserRepository = UserRepository(firestore)

    @Provides
    @Singleton
    fun provideSkillRepository(firestore: FirebaseFirestore): SkillRepository = SkillRepository(firestore)

    @Provides
    @Singleton
    fun provideChatRepository(firestore: FirebaseFirestore): ChatRepository = ChatRepository(firestore)

    @Provides
    @Singleton
    fun provideExchangeRepository(firestore: FirebaseFirestore): ExchangeRepository = ExchangeRepository(firestore)

    @Provides
    @Singleton
    fun provideStorageRepository(storage: FirebaseStorage): StorageRepository = StorageRepository(storage)

    @Provides
    @Singleton
    fun providePostRepository(firestore: FirebaseFirestore): PostRepository = PostRepository(firestore)

    @Provides
    @Singleton
    fun provideSwapRepository(firestore: FirebaseFirestore): SwapRepository = SwapRepository(firestore)

    @Provides
    @Singleton
    fun provideProfileRepository(
        firestore: FirebaseFirestore,
        storage: FirebaseStorage
    ): ProfileRepository = ProfileRepository(firestore, storage)
}
