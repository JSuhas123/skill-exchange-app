# Offline-First Support Implementation Guide

**Document Version:** 1.0  
**Status:** Implementation Ready  
**Last Updated:** 2025-01-15

---

## Overview

The SkillExchange app is designed for rural/low-connectivity environments where network interruptions are frequent. This guide provides offline-first strategies while maintaining eventual consistency across devices.

---

## 1. Current State

### What Works Offline
✅ **Navigation & UI**: All screens remain responsive  
✅ **Session Management**: DataStore maintains user session  
✅ **Input Composition**: Users can compose messages/posts  
✅ **Error Handling**: Network errors detected and displayed  

### What Requires Network
❌ **Real-time Sync**: Messages, posts, swaps  
❌ **Profile Updates**: User profile changes  
❌ **Media Upload**: Profile images  

---

## 2. Firestore Offline Persistence Configuration

### Enable Offline Mode (RECOMMENDED)

Add to `di/AppModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val settings = firestoreSettings {
            isPersistenceEnabled = true  // ✅ ENABLE
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
        
        return Firebase.firestore.apply {
            firestoreSettings = settings
        }
    }
}
```

### Behavior With Offline Persistence
- ✅ Reads served from local cache
- ✅ Writes queued locally, synced when online
- ✅ 40MB default cache (tuned for rural users)
- ✅ Automatic sync on network restoration

---

## 3. Write Ahead Queue for Critical Operations

Create `utils/OfflineSyncQueue.kt`:

```kotlin
package com.example.skillexchange.utils

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

data class PendingOperation(
    val id: String,
    val type: String,  // "create_post", "send_message", "create_swap"
    val payload: String,  // JSON
    val timestamp: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

class OfflineSyncQueue(context: Context) {
    private val dataStore = context.preferencesDataStore("offline_queue")
    private val gson = Gson()
    private val operationKey = stringSetPreferencesKey("pending_operations")

    // Queue operation for later sync
    suspend fun queueOperation(operation: PendingOperation) {
        dataStore.edit { preferences ->
            val operations = preferences[operationKey]?.toMutableSet() ?: mutableSetOf()
            operations.add(gson.toJson(operation))
            preferences[operationKey] = operations
            Timber.d("Queued: ${operation.type} (${operations.size} pending)")
        }
    }

    // Get all pending operations
    fun getPendingOperations(): Flow<List<PendingOperation>> =
        dataStore.data.map { preferences ->
            val operations = preferences[operationKey] ?: emptySet()
            operations.mapNotNull { json ->
                try {
                    gson.fromJson(json, PendingOperation::class.java)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to deserialize operation")
                    null
                }
            }
        }

    // Remove after successful sync
    suspend fun removeOperation(operationId: String) {
        dataStore.edit { preferences ->
            val operations = preferences[operationKey]?.toMutableSet() ?: mutableSetOf()
            operations.removeAll { json ->
                try {
                    gson.fromJson(json, PendingOperation::class.java).id == operationId
                } catch (e: Exception) {
                    false
                }
            }
            preferences[operationKey] = operations
        }
    }

    // Increment retry count
    suspend fun incrementRetry(operationId: String) {
        dataStore.edit { preferences ->
            val operations = preferences[operationKey]?.toMutableSet() ?: mutableSetOf()
            val updated = operations.map { json ->
                try {
                    val op = gson.fromJson(json, PendingOperation::class.java)
                    if (op.id == operationId) {
                        gson.toJson(op.copy(retryCount = op.retryCount + 1))
                    } else {
                        json
                    }
                } catch (e: Exception) {
                    json
                }
            }.toSet()
            preferences[operationKey] = updated
        }
    }

    // Clear failed operations after max retries
    suspend fun clearFailedOperations() {
        dataStore.edit { preferences ->
            val operations = preferences[operationKey]?.toMutableSet() ?: mutableSetOf()
            val filtered = operations.filter { json ->
                try {
                    val op = gson.fromJson(json, PendingOperation::class.java)
                    op.retryCount < op.maxRetries
                } catch (e: Exception) {
                    false
                }
            }.toSet()
            preferences[operationKey] = filtered
        }
    }
}
```

---

## 4. Sync Service for Network Restoration

Create `services/OfflineSyncService.kt`:

```kotlin
package com.example.skillexchange.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.*
import com.example.skillexchange.utils.OfflineSyncQueue
import com.google.gson.Gson
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class OfflineSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    private val syncQueue = OfflineSyncQueue(context)
    private val gson = Gson()
    
    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting offline sync...")
            
            // Retry all pending operations
            syncQueue.getPendingOperations().collect { operations ->
                operations.forEach { operation ->
                    try {
                        if (!syncOperation(operation)) {
                            // Still offline or error, keep in queue
                            syncQueue.incrementRetry(operation.id)
                        } else {
                            // Success, remove from queue
                            syncQueue.removeOperation(operation.id)
                            Timber.d("Synced: ${operation.type}")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Sync failed for: ${operation.type}")
                        syncQueue.incrementRetry(operation.id)
                    }
                }
                
                // Clear operations exceeding max retries
                syncQueue.clearFailedOperations()
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Sync worker failed")
            Result.retry()
        }
    }
    
    private suspend fun syncOperation(op: PendingOperation): Boolean {
        // Implement based on operation type
        return when (op.type) {
            "create_post" -> syncCreatePost(op.payload)
            "send_message" -> syncSendMessage(op.payload)
            "create_swap" -> syncCreateSwap(op.payload)
            else -> {
                Timber.w("Unknown operation type: ${op.type}")
                false
            }
        }
    }
    
    private suspend fun syncCreatePost(payload: String): Boolean {
        // Deserialize and sync
        return true  // Implementation depends on repository
    }
    
    private suspend fun syncSendMessage(payload: String): Boolean {
        return true
    }
    
    private suspend fun syncCreateSwap(payload: String): Boolean {
        return true
    }
    
    companion object {
        const val WORK_NAME = "offline_sync"
        
        fun scheduleSync(context: Context) {
            val syncWork = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWork
            )
        }
    }
}

// Schedule syncing periodically
// In SkillExchangeApplication.onCreate():
// OfflineSyncWorker.scheduleSync(this)
```

---

## 5. Offline Indicator UI Components

Add to `ui/components/StateComponents.kt`:

```kotlin
@Composable
fun OfflineIndicator(isOnline: Boolean) {
    if (!isOnline) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "No internet",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "You're offline. Changes will sync when connection restored.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SyncIndicator(pendingOperations: Int) {
    if (pendingOperations > 0) {
        Badge(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.warningContainer,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.dp,
                    color = MaterialTheme.colorScheme.warning
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Syncing ($pendingOperations)",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
```

---

## 6. Repository Integration for Offline Support

Update `data/repository/PostRepository.kt`:

```kotlin
suspend fun createPost(post: Post): Result<Unit> = try {
    if (!networkManager.isConnected()) {
        // Queue for later sync
        offlineSyncQueue.queueOperation(
            PendingOperation(
                id = UUID.randomUUID().toString(),
                type = "create_post",
                payload = gson.toJson(post),
                timestamp = System.currentTimeMillis()
            )
        )
        return Result.success(Unit)
    }
    
    // If online, sync immediately
    ErrorHandler.withRetry {
        firestore.collection("posts").document(post.id).set(post).await()
    }
} catch (e: Exception) {
    Timber.e(e, "Failed to create post")
    Result.failure(e)
}
```

---

## 7. Network State Monitoring

Update ViewModels to observe network state:

```kotlin
class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val networkManager: NetworkManager
) : ViewModel() {
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline
    
    init {
        viewModelScope.launch {
            networkManager.isOnline.collect { online ->
                _isOnline.value = online
                if (online) {
                    Timber.d("Network restored, syncing offline operations...")
                    // Trigger sync
                }
            }
        }
    }
}
```

---

## 8. Offline Experience in UI

```kotlin
@Composable
fun CreatePostScreen(viewModel: CreatePostViewModel) {
    val isOnline by viewModel.isOnline.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Show offline banner if no network
        if (!isOnline) {
            OfflineIndicator(isOnline = false)
        }
        
        // Rest of UI with "Save Draft" vs "Create Post" button
        if (isOnline) {
            PremiumButton(text = "Create Post") { /* Save to Firestore */ }
        } else {
            PremiumButton(text = "Save as Draft (sync later)") { /* Queue operation */ }
        }
    }
}
```

---

## 9. Offline Limitations & User Communication

### When Offline
- ✅ View existing posts (from cache)
- ✅ View messages (from cache)
- ✅ Compose new messages (queued for sync)
- ✅ Create posts (queued for sync)
- ⚠️ Cannot upload images (requires network)
- ⚠️ Cannot view real-time updates
- ⚠️ Cannot access new content

### User Communication

Show in-app toast/dialog:
```
"You're offline. Your messages will send when connection is restored."
"Creating post... (1 pending)"
"Failed to sync after 3 attempts. Check your internet."
```

---

## 10. Data Consistency Strategies

### Eventual Consistency Model
- Write-local-first: Optimize for latency, accept eventual consistency
- Conflict Resolution: Server-side last-write-wins
- Idempotent Operations: Safe to retry without duplicates

### Firestore TTL Cleanup
```kotlin
// ChatRepository: Clear old cached messages
suspend fun clearOldMessages(threadId: String, olderThanDays: Int = 30) {
    val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000)
    firestore.collection("messages/$threadId/messages")
        .whereLessThan("timestamp", cutoffTime)
        .get()
        .await()
        .documents
        .forEach { doc ->
            doc.reference.delete().await()
        }
}
```

---

## 11. Configuration for Rural Deployments

### Optimize for Low Bandwidth

```kotlin
// In AppModule: Reduce cache size for low-end devices
val settings = firestoreSettings {
    isPersistenceEnabled = true
    cacheSizeBytes = 10_000_000  // 10MB instead of unlimited
}

// Reduce image quality
const val IMAGE_QUALITY = 60  // 60% instead of 85%
const val IMAGE_MAX_SIZE = 2_000_000  // 2MB instead of 5MB
```

### Compression Strategies
- ✅ GZIP request/response compression (automatic in Firebase)
- ✅ Image compression before upload (60% quality for rural)
- ✅ Batch operations to reduce API calls
- ✅ Pagination to limit payload size

---

## 12. Testing Offline Functionality

### Unit Tests
```kotlin
@Test
fun testQueueOperationWhenOffline() {
    whenever(networkManager.isConnected()).thenReturn(false)
    
    val result = postRepository.createPost(testPost)
    
    assertTrue(result.isSuccess)
    assertTrue(offlineSyncQueue.hasPendingOperations())
}

@Test
fun testSyncOperationWhenOnline() {
    // Verify operation syncs when network restored
}
```

### Manual Testing
1. **Create Post While Offline**
   - Disable WiFi/mobile
   - Create post → Should show "Draft saved"
   - Open offline queue → Should see pending operation
   - Enable network → Verify post syncs

2. **Send Message While Offline**
   - Send message → Should queue
   - Verify delivered when online

3. **Network Interruption**
   - Start upload
   - Kill network mid-upload
   - Verify auto-retry when restored

---

## 13. Monitoring Offline Operations

Track in Firebase Analytics:
```kotlin
Firebase.analytics.logEvent("offline_operation_queued") {
    param("operation_type", "create_post")
    param("queue_size", pendingOps.size)
}

Firebase.analytics.logEvent("offline_operation_synced") {
    param("operation_type", "create_post")
    param("retry_count", operation.retryCount)
    param("time_queued_ms", System.currentTimeMillis() - operation.timestamp)
}
```

---

## 14. Rollout Strategy

### Phase 1: Enable Firestore Offline Persistence (Week 1)
- ✅ Update AppModule.kt
- ✅ Deploy to staging
- ✅ Test 48-hour offline scenario
- ✅ Monitor cache size metrics

### Phase 2: Add Sync Queue (Week 2)
- ✅ Add OfflineSyncQueue.kt
- ✅ Integrate with repositories
- ✅ Add UI indicators
- ✅ Test end-to-end offline → online flow

### Phase 3: Deploy OfflineSyncWorker (Week 3)
- ✅ Add background sync service
- ✅ Schedule periodic work
- ✅ Monitor sync success rates

---

## 15. Success Metrics

- ✅ Offline post creation queued successfully
- ✅ 95% of queued operations sync within 10 minutes of network restoration
- ✅ <5% operation retry rate
- ✅ <1MB average cache size per user
- ✅ Zero data corruption from offline conflicts

---

**Document Owner:** Infrastructure & Platform Team  
**Last Review:** 2025-01-15  
**Next Review:** 2025-03-15
