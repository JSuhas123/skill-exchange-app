# SkillExchange Architecture Diagrams

**Document Version:** 1.0  
**Date:** 2025-01-15  
**Target:** Visual understanding of app architecture, data flow, and system interactions

---

## 1. Overall Application Architecture

```mermaid
graph TB
    subgraph "UI Layer"
        MainActivity["MainActivity<br/>(Jetpack Compose)"]
        NavGraph["Navigation Graph<br/>(Type-Safe Routing)"]
        Screens["6 Refactored Screens<br/>SkillBoard, Profile, Chat, etc."]
    end

    subgraph "ViewModel Layer"
        AuthVM["AuthViewModel<br/>(Session Management)"]
        CreatePostVM["CreatePostViewModel<br/>(Validation)"]
        ProfileVM["ProfileViewModel<br/>(Profile & Upload)"]
        ChatVM["ChatViewModel<br/>(Messages)"]
        SwapVM["SwapViewModel<br/>(Swap Management)"]
        OtherVMs["+ 6 More ViewModels"]
    end

    subgraph "Repository Layer"
        AuthRepo["AuthRepository<br/>(Auth & Recovery)"]
        PostRepo["PostRepository<br/>(Posts CRUD)"]
        SkillRepo["SkillRepository<br/>(Skills)"]
        ChatRepo["ChatRepository<br/>(Messages)"]
        SwapRepo["SwapRepository<br/>(Swaps)"]
        UserRepo["UserRepository<br/>(User Profiles)"]
        ProfileRepo["ProfileRepository<br/>(Image Upload)"]
    end

    subgraph "Utility Layer"
        NetworkMgr["NetworkManager<br/>(Connectivity)"]
        ErrorHandler["ErrorHandler<br/>(Retry Logic)"]
        InputValidator["InputValidator<br/>(Data Validation)"]
        AsyncResource["AsyncResource<br/>(State Wrapper)"]
        Logger["ProductionLogger<br/>(Timber + Crashlytics)"]
    end

    subgraph "Data/Service Layer"
        Firestore["Firestore<br/>(NoSQL Database)"]
        Storage["Firebase Storage<br/>(Images)"]
        Auth["Firebase Auth<br/>(Anonymous)"]
        Crashlytics["Crashlytics<br/>(Error Tracking)"]
        Analytics["Analytics<br/>(Events)"]
    end

    subgraph "Dependency Injection"
        AppModule["AppModule<br/>(DataStore, Firestore)"]
        RepositoryModule["RepositoryModule<br/>(Repositories)"]
        Hilt["Hilt DI Container"]
    end

    MainActivity --> NavGraph
    NavGraph --> Screens
    Screens --> AuthVM
    Screens --> CreatePostVM
    Screens --> ProfileVM
    Screens --> ChatVM
    Screens --> SwapVM
    Screens --> OtherVMs

    AuthVM --> AuthRepo
    CreatePostVM --> PostRepo
    ProfileVM --> ProfileRepo
    ChatVM --> ChatRepo
    SwapVM --> SwapRepo
    OtherVMs --> UserRepo
    OtherVMs --> SkillRepo

    AuthRepo --> NetworkMgr
    PostRepo --> NetworkMgr
    SkillRepo --> NetworkMgr
    ChatRepo --> NetworkMgr
    SwapRepo --> NetworkMgr
    UserRepo --> NetworkMgr
    ProfileRepo --> NetworkMgr

    AuthRepo --> ErrorHandler
    PostRepo --> ErrorHandler
    ChatRepo --> ErrorHandler
    SwapRepo --> ErrorHandler

    CreatePostVM --> InputValidator
    ProfileVM --> InputValidator

    AuthRepo --> AsyncResource
    PostRepo --> AsyncResource

    Firestore --> Auth
    Firestore --> Analytics
    Storage --> Auth
    Firestore --> Crashlytics

    AuthRepo --> Logger
    PostRepo --> Logger
    ChatRepo --> Logger

    Hilt -.-> AppModule
    Hilt -.-> RepositoryModule
    RepositoryModule -.-> AuthRepo
    RepositoryModule -.-> PostRepo

    style MainActivity fill:#FF6B6B
    style NavGraph fill:#FF6B6B
    style Screens fill:#FF6B6B
    style AuthVM fill:#4ECDC4
    style CreatePostVM fill:#4ECDC4
    style ProfileVM fill:#4ECDC4
    style ChatVM fill:#4ECDC4
    style SwapVM fill:#4ECDC4
    style AuthRepo fill:#45B7D1
    style PostRepo fill:#45B7D1
    style Firestore fill:#95E1D3
    style Storage fill:#95E1D3
    style Auth fill:#95E1D3
```

---

## 2. User Authentication & Session Flow

```mermaid
sequenceDiagram
    participant User as User/App
    participant Auth as AuthViewModel
    participant AuthRepo as AuthRepository
    participant Firebase as Firebase Auth
    participant DataStore as DataStore<br/>Session
    participant Firestore as Firestore

    User->>Auth: App Start
    Auth->>AuthRepo: initializeAuth()
    
    alt Session Valid
        AuthRepo->>DataStore: Read sessionId
        DataStore-->>AuthRepo: sessionId found
        AuthRepo->>Firebase: refreshIdToken()
        Firebase-->>AuthRepo: Valid token
        AuthRepo-->>Auth: Session Valid ✅
    else Session Invalid/Expired
        AuthRepo->>Firebase: signInAnonymously()
        Firebase-->>AuthRepo: New auth token
        AuthRepo->>DataStore: Save sessionId
        DataStore-->>AuthRepo: Saved ✅
        AuthRepo->>Firestore: createUser()
        Firestore-->>AuthRepo: User created
        AuthRepo-->>Auth: New Session ✅
    end

    Auth->>User: showMainScreen()

    Note over DataStore: Session persists across<br/>app restarts
    Note over AuthRepo: Auto-recovery on network<br/>failure or token expiry
```

---

## 3. Skill Post Creation & Validation Flow

```mermaid
graph LR
    subgraph UI["UI Layer"]
        CreateScreen["CreatePostScreen<br/>(Input Fields)"]
    end

    subgraph ViewModel["ViewModel - Validation"]
        CreateVM["CreatePostViewModel"]
        ValidateSkill["validateSkill()"]
        ValidateDesc["validateDescription()"]
        ValidateLength["validateLength()"]
    end

    subgraph Utils["Utilities"]
        InputValidator["InputValidator<br/>(2-100 chars)"]
        ErrorMap["Error Mapper<br/>(User-friendly)"]
    end

    subgraph Repository["Repository"]
        PostRepo["PostRepository"]
        ErrorHandler["ErrorHandler<br/>(Retry 3x)"]
    end

    subgraph Firebase["Firebase"]
        FirestoreRules["Firestore Rules<br/>(Server validation)"]
        Firestore["Firestore<br/>(Store post)"]
    end

    CreateScreen -->|skillRequired| CreateVM
    CreateVM --> ValidateSkill
    ValidateSkill --> InputValidator
    InputValidator -->|Valid| CreateVM
    InputValidator -->|Invalid| ErrorMap
    ErrorMap -->|Show errors| CreateScreen

    CreateScreen -->|description| CreateVM
    CreateVM --> ValidateDesc
    ValidateDesc --> InputValidator

    CreateVM -->|All valid| PostRepo
    PostRepo --> ErrorHandler
    ErrorHandler -->|Try 1-3| FirestoreRules
    FirestoreRules -->|Valid| Firestore
    Firestore -->|Success| CreateVM
    Firestore -->|Error| ErrorHandler
    ErrorHandler -->|Retry| FirestoreRules

    style CreateScreen fill:#FFB6B9
    style CreateVM fill:#8FD3F4
    style InputValidator fill:#87CEEB
    style Firestore fill:#90EE90
```

---

## 4. Skill Swap Transaction Flow (Atomic)

```mermaid
graph TD
    subgraph "Initiate Swap"
        SwapVM["SwapViewModel<br/>initiateSwap()"]
        CheckPending["Check if action<br/>already pending"]
        ValidateHours["Validate hours<br/>1-168"]
    end

    subgraph "Create Swap"
        SwapRepo["SwapRepository<br/>createSwap()"]
        CreatePost["POST /swaps"]
    end

    subgraph "Accept Swap"
        AcceptVM["SwapViewModel<br/>acceptSwap()"]
        UpdateStatus["Update status<br/>to accepted"]
    end

    subgraph "Confirm & Progress"
        ConfirmVM["SwapViewModel<br/>confirmCompletion()"]
        Transaction["Firestore<br/>Transaction"]
        IdempotentCheck["Check if<br/>already confirmed"]
        UpdateUsers["Update both users<br/>(+10 trust)"]
        UpdateSkills["Deduct skill points"]
        UpdateSwapStatus["Update swap<br/>to completed"]
    end

    SwapVM --> CheckPending
    CheckPending -->|Pending| SwapVM
    CheckPending -->|Not pending| ValidateHours
    ValidateHours -->|Valid| SwapRepo
    SwapRepo --> CreatePost

    CreatePost -->|Success| AcceptVM
    AcceptVM --> UpdateStatus

    UpdateStatus -->|Success| ConfirmVM
    ConfirmVM --> Transaction
    Transaction --> IdempotentCheck

    IdempotentCheck -->|Already confirmed| Transaction
    IdempotentCheck -->|Not confirmed| UpdateUsers
    UpdateUsers --> UpdateSkills
    UpdateSkills --> UpdateSwapStatus
    UpdateSwapStatus -->|Commit| Transaction

    Transaction -->|Atomic: All or None| ConfirmVM

    style Transaction fill:#FFD700
    style IdempotentCheck fill:#87CEEB
    style UpdateUsers fill:#90EE90
    Note over Transaction: All updates succeed<br/>or all fail - zero partial updates
```

---

## 5. Real-Time Message Ordering

```mermaid
sequenceDiagram
    participant User1 as User A<br/>App
    participant ViewModel as ChatViewModel
    participant ChatRepo as ChatRepository
    participant Flow as Firestore<br/>Flow/Listener
    participant Firestore as Firestore<br/>DB

    User1->>ViewModel: onScreenVisible()
    ViewModel->>ChatRepo: getMessages(threadId)
    
    ChatRepo->>Flow: callbackFlow { listener }
    Flow->>Firestore: Query: orderBy timestamp ASC
    
    loop Snapshot Updates
        Firestore-->>Flow: New snapshot
        Flow->>Flow: Sort messages by timestamp
        Flow->>ViewModel: emit(Resource.Success(messages))
    end

    ViewModel->>User1: Display ordered messages

    User1->>ViewModel: onScreenLeave()
    ViewModel->>Flow: Close collection
    Flow->>Flow: subscription.remove()
    Flow-->>Firestore: Listener cleaned up ✅

    Note over Flow: Guaranteed message ordering<br/>even with network delays
    Note over Flow: Automatic cleanup prevents<br/>memory leaks
```

---

## 6. Offline-First Support Architecture

```mermaid
graph TB
    subgraph "Online State"
        Online["Network Active"]
        Immediate["Sync Immediately"]
    end

    subgraph "Offline Queue"
        Offline["Network Inactive"]
        QueueOp["Queue Operation<br/>OfflineSyncQueue"]
        DataStore["DataStore<br/>(Persisted)"]
    end

    subgraph "Background Sync"
        NetworkMgr["NetworkManager<br/>Detects Online"]
        SyncWorker["OfflineSyncWorker<br/>Background Job"]
        RetryLogic["Retry with<br/>Exponential Backoff"]
    end

    subgraph "UI Feedback"
        OfflineIndicator["Offline Banner<br/>User Aware"]
        SyncStatus["Sync Status<br/>Pending Count"]
    end

    Online -->|Create Post| Immediate
    Immediate -->|Success| Online

    Offline -->|Create Post| QueueOp
    QueueOp --> DataStore
    DataStore -->|Persists| DataStore
    
    OfflineIndicator -.->|Shows| Offline
    SyncStatus -.->|Tracks| DataStore

    NetworkMgr -->|Network Restored| SyncWorker
    SyncWorker --> RetryLogic
    RetryLogic -->|Success| Online
    RetryLogic -->|Failure| RetryLogic
    
    style Online fill:#90EE90
    style Offline fill:#FFB6C1
    style QueueOp fill:#87CEEB
    style DataStore fill:#FFD700
```

---

## 7. Error Handling & Retry Strategy

```mermaid
graph TB
    subgraph "Operation"
        Op["Repository Function<br/>Suspend"]
    end

    subgraph "ErrorHandler.withRetry"
        Try["Attempt 1"]
        isNetworkError["Is Network<br/>Error?"]
        isRetryable["Is Retryable?"]
    end

    subgraph "Retry Loop"
        Wait1["Wait 1s"]
        Try2["Attempt 2"]
        Wait2["Wait 2s"]
        Try3["Attempt 3"]
        Wait3["Wait 4s"]
        Try4["Attempt 4"]
    end

    subgraph "Fallback"
        LogError["Log to<br/>Crashlytics"]
        ReturnError["Return<br/>Result.failure()"]
    end

    Op --> Try
    Try -->|Exception| isNetworkError
    isNetworkError -->|No| LogError
    isNetworkError -->|Yes| isRetryable
    isRetryable -->|No| LogError
    isRetryable -->|Yes| Wait1
    
    Wait1 --> Try2
    Try2 -->|Success| Op
    Try2 -->|Failure| Wait2
    Wait2 --> Try3
    Try3 -->|Success| Op
    Try3 -->|Failure| Wait3
    Wait3 --> Try4
    Try4 -->|Success| Op
    Try4 -->|Failure| LogError

    LogError --> ReturnError
    ReturnError -->|To ViewModel| Op

    style Try fill:#87CEEB
    style Try2 fill:#87CEEB
    style Try3 fill:#87CEEB
    style Try4 fill:#87CEEB
    style Wait1 fill:#FFD700
    style Wait2 fill:#FFD700
    style LogError fill:#FFB6C1

    Note over Try,Try4: Max 3 retries<br/>Exponential backoff 1-10s
```

---

## 8. Data State Management (Resource Wrapper)

```mermaid
graph LR
    subgraph "States"
        Idle["Idle<br/>Initial State"]
        Loading["Loading<br/>Fetching Data"]
        Success["Success(data)<br/>Data Available"]
        Error["Error(message)<br/>Operation Failed"]
    end

    subgraph "Transitions"
        T1["User Action"]
        T2["Data Received"]
        T3["Exception"]
        T4["Retry Action"]
    end

    subgraph "UI Response"
        UI1["Show Nothing"]
        UI2["Show Spinner"]
        UI3["Show Content"]
        UI4["Show Error + Retry"]
    end

    Idle -->|T1| Loading
    Loading -->|T2| Success
    Loading -->|T3| Error
    Error -->|T4| Loading

    Idle --> UI1
    Loading --> UI2
    Success --> UI3
    Error --> UI4

    style Idle fill:#E0E0E0
    style Loading fill:#FFD700
    style Success fill:#90EE90
    style Error fill:#FFB6C1

    Note over Success,Error: Extension functions:<br/>isSuccess(), isError(), map(), etc.
```

---

## 9. Material 3 Component Hierarchy

```mermaid
graph TB
    subgraph "Button Components"
        PremiumButton["PremiumButton<br/>Primary 48dp"]
        OutlinedButton["OutlinedButton<br/>Secondary"]
        FloatingButton["FloatingButton<br/>FAB 56dp"]
    end

    subgraph "Card Components"
        SkillCard["SkillCard"]
        PostCard["PostCard<br/>2 Actions"]
        ProfileCard["ProfileCard"]
        ScoreCard["ScoreCard<br/>Points + Trust"]
    end

    subgraph "Input Components"
        TextField["PremiumTextField<br/>+ Validation"]
        SkillChip["SkillChip<br/>Removable"]
    end

    subgraph "Navigation"
        TopAppBar["TopAppBar<br/>Centered Title"]
        BottomNav["BottomNav<br/>5 Items"]
        SideNav["SideNav<br/>Tablets"]
    end

    subgraph "State Components"
        Loading["FullScreenLoading"]
        Error["ErrorView + Retry"]
        Empty["EmptyView"]
        Offline["OfflineView"]
    end

    subgraph "Dialog & Feedback"
        Dialog["AlertDialog<br/>Confirmation"]
        Snackbar["Snackbar<br/>Toast"]
        Badge["Badge<br/>Unread Count"]
    end

    style PremiumButton fill:#0056D2
    style OutlinedButton fill:#006B3F
    style FloatingButton fill:#5D5E7D
    style Loading fill:#FFD700
    style Error fill:#FFB6C1
    style Empty fill:#87CEEB
    style Offline fill:#FF6B6B
```

---

## 10. Firebase Integration Layers

```mermaid
graph TB
    subgraph "App Layer"
        Repositories["Repositories<br/>(8 Total)"]
    end

    subgraph "Firebase SDK"
        Firebase["Firebase Core"]
    end

    subgraph "Authentication"
        Auth["Firebase Auth<br/>Anonymous SignIn"]
        AuthRules["Authentication Rules<br/>isSignedIn()"]
    end

    subgraph "Database"
        Firestore["Firestore NoSQL"]
        Collections["6 Collections<br/>with Security Rules"]
        Indexes["7 Composite<br/>Performance Indexes"]
    end

    subgraph "Storage"
        Storage["Cloud Storage"]
        StorageRules["Storage Rules<br/>5MB Limit, image/*"]
    end

    subgraph "Analytics & Monitoring"
        Crashlytics["Crashlytics<br/>Crash Reporting"]
        Analytics["Analytics<br/>Event Tracking"]
        Performance["Performance<br/>Monitoring"]
    end

    Repositories --> Firebase
    Firebase --> Auth
    Firebase --> Firestore
    Firebase --> Storage
    Firebase --> Crashlytics
    Firebase --> Analytics

    Auth --> AuthRules
    Firestore --> Collections
    Firestore --> Indexes
    Storage --> StorageRules

    style Auth fill:#4ECDC4
    style Firestore fill:#45B7D1
    style Storage fill:#95E1D3
    style Crashlytics fill:#FF6B6B
    style Analytics fill:#FFD700
```

---

## 11. Testing Architecture

```mermaid
graph TB
    subgraph "Unit Tests"
        UtilTests["UtilityTests<br/>40+ cases"]
        InputValidator["✓ Input validation"]
        ErrorHandler["✓ Retry logic"]
        ResourceExt["✓ Resource extensions"]
    end

    subgraph "ViewModel Tests"
        VMTests["ViewModelTests<br/>30+ cases"]
        AuthVM["✓ AuthViewModel"]
        CreatePostVM["✓ CreatePostViewModel"]
        ProfileVM["✓ ProfileViewModel"]
        SwapVM["✓ SwapViewModel"]
    end

    subgraph "Repository Tests"
        RepoTests["RepositoryTests<br/>40+ cases"]
        MockFirestore["Mock Firestore<br/>for unit testing"]
        AuthRepo["✓ AuthRepository"]
        PostRepo["✓ PostRepository"]
    end

    subgraph "UI Tests"
        UITests["Compose UI Tests<br/>40+ cases"]
        ButtonTests["✓ Button components"]
        CardTests["✓ Card components"]
        StateTests["✓ State components"]
    end

    subgraph "Integration"
        Integration["Integration Tests"]
        EndToEnd["✓ Full user flows"]
        Network["✓ Network scenarios"]
    end

    UtilTests --> InputValidator
    UtilTests --> ErrorHandler
    UtilTests --> ResourceExt

    VMTests --> AuthVM
    VMTests --> CreatePostVM
    VMTests --> ProfileVM
    VMTests --> SwapVM

    RepoTests --> MockFirestore
    RepoTests --> AuthRepo
    RepoTests --> PostRepo

    UITests --> ButtonTests
    UITests --> CardTests
    UITests --> StateTests

    style UtilTests fill:#87CEEB
    style VMTests fill:#90EE90
    style RepoTests fill:#FFD700
    style UITests fill:#FFB6C1
    style Integration fill:#4ECDC4
```

---

## 12. Deployment Pipeline

```mermaid
graph LR
    subgraph "Local"
        Dev["Development"]
        Test["Run Tests<br/>150+ cases"]
    end

    subgraph "Version Control"
        Commit["Commit to Git"]
        Push["Push to GitHub"]
    end

    subgraph "Staging"
        Staging["Staging Environment<br/>Firebase Staging"]
        StagingTest["48hr Testing<br/>10+ Devices"]
    end

    subgraph "Play Store"
        Internal["Internal Testing<br/>7 days"]
        Beta["Beta Testing<br/>100 users"]
        Release["Production Release<br/>Gradual rollout"]
    end

    subgraph "Monitoring"
        Crashlytics["Crashlytics<br/>Error tracking"]
        Analytics["Analytics<br/>User metrics"]
        Alerts["Performance Alerts"]
    end

    Dev --> Test
    Test -->|Pass| Commit
    Commit --> Push
    Push --> Staging
    Staging --> StagingTest
    StagingTest -->|Pass| Internal
    Internal -->|7 days| Beta
    Beta -->|Pass| Release
    Release --> Crashlytics
    Release --> Analytics
    Release --> Alerts

    style Staging fill:#FFD700
    style Release fill:#90EE90
    style Crashlytics fill:#FF6B6B
```

---

## Document Legend

| Symbol | Meaning |
|--------|---------|
| 🔵 | Component/Module |
| 🔄 | Data Flow |
| ⚡ | Critical Path |
| ⏱️ | Timing/Delay |
| ✅ | Success State |
| ❌ | Error/Failure |
| 📊 | Data/Storage |

---

**All diagrams are rendered in Mermaid.js and compatible with:**
- GitHub markdown rendering
- GitLab markdown
- Notion
- Confluence
- VS Code with Mermaid extension

For interactive viewing, use [Mermaid Live Editor](https://mermaid.live)
