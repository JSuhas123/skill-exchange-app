# App Stability & Optimization Guidelines

**Document Version:** 1.0  
**Last Updated:** 2025-01-15  
**Status:** Implementation In Progress

---

## 1. Null Safety & Type Safety

### Current Implementation
All Kotlin code uses strict null safety with explicit nullable types (?) and non-null assertions (!!) only when guaranteed safe.

### Best Practices in Use

✅ **GOOD: Explicit null handling**
```kotlin
// Use let/apply/also for safe navigation
currentUser?.let { user ->
    // Only executes if currentUser is not null
    updateUI(user)
}

// Use Elvis operator
val userName = user?.name ?: "Anonymous"

// Use safe calls
user?.id?.length ?: 0
```

✅ **GOOD: Type-safe repository returns**
```kotlin
// Result<T> ensures type safety
suspend fun createSwap(): Result<Unit> = try {
    Result.success(Unit)
} catch (e: Exception) {
    Result.failure(e)
}

// Flow<Resource<T>> ensures state safety
fun getMessages(): Flow<Resource<List<Message>>> = callbackFlow {
    // Guaranteed to emit one of: Loading, Success, Error, Idle
}
```

### Avoid Patterns

❌ **AVOID: Non-null assertions**
```kotlin
// Don't use !! unless guaranteed non-null
val userId = user!!.id  // Will crash if user is null

// Better:
val userId = user?.id ?: return  // Early exit if null
```

❌ **AVOID: Unchecked type casts**
```kotlin
// Don't use unchecked casts
val swap = data as Swap  // May throw ClassCastException

// Better:
val swap = data as? Swap ?: return error("Invalid data")
```

---

## 2. Lifecycle Management & Memory Leaks

### Coroutine Lifecycle Safety

✅ **GOOD: Scoped coroutines**
```kotlin
class MyViewModel : ViewModel() {
    // viewModelScope automatically cancels when ViewModel destroyed
    fun loadData() {
        viewModelScope.launch {
            val data = repository.getData()
            state.value = data
        }
    }
}

// In Compose
LaunchedEffect(Unit) {
    // Automatically cancels when composable leaves composition
    val data = viewModel.getData()
}
```

✅ **GOOD: Proper Firestore listener cleanup**
```kotlin
fun getMessages(): Flow<Resource<>> = callbackFlow {
    val subscription = firestore.collection("messages")
        .addSnapshotListener { snapshot, error ->
            if (error != null) sendBlocking(Resource.Error(...))
            else sendBlocking(Resource.Success(...))
        }
    
    // CRITICAL: Cleanup when Flow collection ends
    awaitClose { subscription.remove() }
}
```

### Common Memory Leaks to Avoid

❌ **LEAK: Holding Activity references**
```kotlin
// DON'T do this
class MyRepository(activity: Activity) {
    private val activityRef = activity  // Leaks activity!
}

// DO this instead
class MyRepository : ViewModel() {
    // ViewModel lifecycle matches Fragment/Activity
}
```

❌ **LEAK: Non-removed Firestore listeners**
```kotlin
// DON'T do this
fun getMessages() {
    firestore.collection("messages")
        .addSnapshotListener { snapshot, error ->  // Never removed!
            // Listener stays alive forever
        }
}

// DO this instead with awaitClose
fun getMessages(): Flow<Resource<>> = callbackFlow {
    val subscription = firestore.collection("messages")
        .addSnapshotListener { snapshot, error -> /* ... */ }
    awaitClose { subscription.remove() }  // Guaranteed cleanup
}
```

---

## 3. Crash Prevention Patterns

### Network Error Handling

✅ **GOOD: Retry with exponential backoff**
```kotlin
// All suspend functions use ErrorHandler
suspend fun createPost(post: Post): Result<Unit> = 
    ErrorHandler.withRetry {
        firestore.collection("posts").document(post.id).set(post).await()
    }

// Automatic retry: 3 attempts, 1-10 second delays
```

✅ **GOOD: Network detection**
```kotlin
// Check network before operations
if (!networkManager.isConnected()) {
    return Resource.Error("No internet connection")
}

// Or observe network status
networkManager.isOnline.collect { online ->
    if (!online) showOfflineUI()
}
```

### Null Reference Prevention

✅ **GOOD: Safe resource access**
```kotlin
// Always check Resource state
when (val resource = data) {
    is Resource.Loading -> showLoading()
    is Resource.Success -> updateUI(resource.data)  // Guaranteed non-null
    is Resource.Error -> showError(resource.message)
    is Resource.Idle -> {}
}
```

---

## 4. ANR (Application Not Responding) Prevention

### Move Heavy Operations Off Main Thread

✅ **GOOD: Use Dispatchers.IO**
```kotlin
// In ViewModel
fun loadPosts() {
    viewModelScope.launch {
        val posts = withContext(Dispatchers.IO) {
            repository.getPosts()  // Network call on IO thread
        }
        // Update UI on main thread
        uiState.value = posts
    }
}
```

✅ **GOOD: Use proper thread for Firestore**
```kotlin
// Firestore already handles threading internally
viewModelScope.launch {
    // Safe to call from main thread
    val result = firestore.collection("posts").get().await()
}
```

### Avoid Blocking Calls

❌ **BAD: Blocking calls**
```kotlin
// DON'T block main thread
val data = firestore.collection("posts").get().runBlocking()  // FREEZE!

// DO use suspend functions
val data = firestore.collection("posts").get().await()  // Non-blocking
```

---

## 5. Compose Recomposition Optimization

### Prevent Excessive Recompositions

✅ **GOOD: Stable state objects**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()  // Stable collection
    
    when (uiState) {
        is Resource.Success -> MyContent(uiState.data)
    }
}

// Only recomposes when uiState actually changes
```

✅ **GOOD: Keys for list items**
```kotlin
@Composable
fun PostList(posts: List<Post>) {
    LazyColumn {
        items(posts, key = { it.id }) { post ->
            PostCard(post)  // Reuses composition for same post ID
        }
    }
}
```

### Avoid Performance Issues

❌ **BAD: Unnecessary state changes**
```kotlin
// DON'T create new objects in composition
val colors = listOf(Color.Red, Color.Blue)  // New list every recomposition!

// DO use constants or remember
val colors = remember { listOf(Color.Red, Color.Blue) }
```

---

## 6. Firestore Transaction Safety

### Atomic Operations for Consistency

✅ **GOOD: Use transactions for multi-document updates**
```kotlin
// SwapRepository.confirmSwapAndProgress()
suspend fun confirmSwapAndProgress(swapId: String): Result<Unit> = 
    ErrorHandler.withRetry {
        firestore.runTransaction { transaction ->
            val swapRef = firestore.collection("swaps").document(swapId)
            val swap = transaction.get(swapRef).toObject(Swap::class.java) ?: return@runTransaction
            
            // Idempotent check
            val alreadyConfirmed = if (isUserA) swap.confirmedA else swap.confirmedB
            if (alreadyConfirmed) return@runTransaction  // Already done, skip
            
            // Atomic updates
            transaction.update(swapRef, "confirmedA", true)  // or confirmedB
            transaction.update(userARef, "trustScore", FieldValue.increment(10))
            transaction.update(userBRef, "trustScore", FieldValue.increment(10))
        }
    }

// All updates succeed or all fail - no partial updates
```

---

## 7. Error Message Safety

### No Sensitive Data in Error Messages

✅ **GOOD: User-friendly error messages**
```kotlin
// ErrorHandler provides safe messages
when (error) {
    is IOException -> "Connection error. Check your internet."
    is FirebaseException -> "Unable to process request. Try again."
    else -> "Something went wrong. Please try again."
}

// No stack traces or internal details exposed
```

❌ **BAD: Exposing sensitive information**
```kotlin
// DON'T log sensitive data
Timber.e("User password: $password")  // Never!
Timber.e("Auth token: $token")  // Never!
Timber.e(e, "Full exception: ${e.message}")  // Expose to Crashlytics
```

---

## 8. Data Validation at Every Layer

### Input Validation

✅ **GOOD: Validate in ViewModel before repository call**
```kotlin
fun createPost(skillRequired: String, skillOffered: String, description: String) {
    val errors = mutableMapOf<String, String>()
    
    // Validate each field
    if (!InputValidator.isValidSkill(skillRequired)) 
        errors["skillRequired"] = "Invalid skill"
    if (!InputValidator.isValidDescription(description)) 
        errors["description"] = "Description too short"
    
    if (errors.isNotEmpty()) {
        _validationErrors.value = errors
        return
    }
    
    // If validation passes, call repository
    viewModelScope.launch {
        val result = postRepository.createPost(...)
    }
}
```

### Firestore Rules Validation

✅ **GOOD: Database enforces second layer**
```
rules_version = '2';
service cloud.firestore {
    match /databases/{database}/documents {
        match /posts/{document=**} {
            allow create: if request.auth != null
                && request.resource.data.skillRequired is string
                && request.resource.data.skillRequired.size() >= 2
                && request.resource.data.skillRequired.size() <= 100;
        }
    }
}
```

---

## 9. Image Loading & Caching

### Coil Image Library Configuration

✅ **GOOD: Proper image loading with caching**
```kotlin
@Composable
fun UserAvatar(imageUrl: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Profile",
        modifier = Modifier.size(48.dp),
        contentScale = ContentScale.Crop
    )
}

// Coil automatically:
// - Caches in memory and disk
// - Limits concurrent network requests
// - Handles OOM gracefully
```

### Image Upload Optimization

✅ **GOOD: Compress before upload**
```kotlin
suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> = 
    withContext(Dispatchers.Default) {
        try {
            // Compress image to <500KB
            val compressed = compressImage(imageUri, 85)  // 85% quality
            
            // Upload with size check
            val file = File(context.cacheDir, "temp_image.jpg")
            file.outputStream().write(compressed)
            
            if (file.length() > 5_000_000) {  // 5MB limit
                return@withContext Result.failure(Exception("File too large"))
            }
            
            // Upload to Firebase
            val downloadUrl = storageRepository.uploadImage(userId, file)
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
```

---

## 10. Production Logging Configuration

### Timber + Crashlytics Integration

✅ **GOOD: Production logging**
```kotlin
class SkillExchangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize production logger
        ProductionLogger.init(BuildConfig.DEBUG)
        
        // Firebase Crashlytics auto-capture
        Firebase.crashlytics.apply {
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        }
    }
}

// In repositories: Log errors automatically
Timber.e(exception, "Failed to create post")  // Routes to Crashlytics in prod
```

### Log Levels

- 🔴 **ERROR**: Actual errors that affect functionality (log to Crashlytics)
- 🟡 **WARN**: Unexpected but recoverable (log to Crashlytics)
- 🔵 **INFO**: Important events like login, logout (silent)
- ⚪ **DEBUG**: Detailed flow info (debug builds only)

---

## 11. Stability Checklist

- ✅ All suspend functions use ErrorHandler.withRetry()
- ✅ All Flow listeners use callbackFlow with awaitClose()
- ✅ All ViewModels use viewModelScope (auto-cancel)
- ✅ All Network calls use withContext(Dispatchers.IO) or trust Firestore
- ✅ All Resource states handled with when() expressions
- ✅ No hardcoded !! assertions unless guaranteed safe
- ✅ No untyped list casting (use as? Swap)
- ✅ All database transactions use runTransaction()
- ✅ All images compressed before upload
- ✅ Error messages don't expose sensitive data
- ✅ Firestore listeners are always cleaned up
- ✅ Navigation lifecycle properly managed
- ✅ Input validation in ViewModel before repository
- ✅ Firestore rules provide second validation layer
- ✅ ANR-prone operations run on Dispatchers.IO
- ✅ Compose Lists use key() parameter

---

## 12. Testing Stability

### Unit Tests for Stability
- Test null handling with elvis operator
- Test error retry logic with mock failures
- Test transaction idempotency
- Test Network detection before/after network change
- Test Firestore listener cleanup with Flow completion

### Integration Tests
- Test end-to-end swap flow with network interruption
- Test message ordering across network delays
- Test profile image upload with various sizes
- Test session recovery after app restart

---

**Last Review:** 2025-01-15  
**Next Review:** 2025-02-15  
**Responsible Team:** Quality Assurance & Architecture
