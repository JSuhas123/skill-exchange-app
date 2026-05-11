# SkillExchange Material 3 Component Catalog

**Version**: 1.0  
**Date**: May 11, 2026  
**Status**: Production-Ready  

---

## Table of Contents

1. [Buttons](#buttons)
2. [Cards](#cards)
3. [Input Fields](#input-fields)
4. [State Components](#state-components)
5. [Navigation](#navigation)
6. [Dialogs & Sheets](#dialogs--sheets)
7. [Lists & Layouts](#lists--layouts)
8. [Best Practices](#best-practices)

---

## Buttons

### PremiumButton (Primary Action)

**Purpose**: Primary, most important actions  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
PremiumButton(
    text = "Save Changes",
    onClick = { viewModel.save() },
    modifier = Modifier.fillMaxWidth(),
    enabled = !isSaving,
    isLoading = isSaving
)
```

**Properties**:
- Height: 48dp (Material 3 standard)
- Shape: 12dp rounded corners
- Elevation: 2dp default, 6dp pressed
- Colors: Primary background, onPrimary text

**States**:
- Normal: Blue background, clickable
- Loading: Spinner inside, disabled
- Disabled: Gray background, not clickable
- Pressed: Elevated shadow

**Usage Examples**:
```kotlin
// Create action
PremiumButton("Create Post", onClick = { navigate() })

// Submit form
PremiumButton("Submit", onClick = { save() }, isLoading = isSaving)

// Confirm dialog
PremiumButton("Delete", onClick = { delete() }, 
    containerColor = MaterialTheme.colorScheme.error)
```

---

### OutlinedPremiumButton (Secondary Action)

**Purpose**: Secondary, less important actions  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
OutlinedPremiumButton(
    text = "Cancel",
    onClick = { onCancel() },
    modifier = Modifier.fillMaxWidth()
)
```

**Properties**:
- Height: 48dp
- Border: Primary color, 2dp
- Background: Transparent
- Text: Primary color

**When to Use**:
- Cancel/Dismiss actions
- Alternative options
- Non-destructive actions

```kotlin
// Cancel button
OutlinedPremiumButton("Cancel", onClick = { close() })

// Decline swap
OutlinedPremiumButton("Decline", onClick = { decline() })

// Try another
OutlinedPremiumButton("Try Again", onClick = { retry() })
```

---

### PremiumFloatingButton

**Purpose**: Primary screen action (FAB)  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
PremiumFloatingButton(
    onClick = { navigate() },
    icon = Icons.Default.Add,
    label = "Create Post"
)
```

**Properties**:
- Size: 56dp (standard FAB)
- Shape: Circular (16dp radius)
- Icon: 28dp inside
- Elevation: 6dp default, 12dp pressed

**Accessibility**:
- Large touch target
- Icon + Label in code
- High contrast

```kotlin
// Create post FAB
PremiumFloatingButton(
    onClick = onCreatePost,
    icon = Icons.Default.Add
)

// Message FAB
PremiumFloatingButton(
    onClick = onMessage,
    icon = Icons.AutoMirrored.Filled.Send
)
```

---

## Cards

### PostCard

**Purpose**: Display skill exchange posts  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
PostCard(
    post = post,
    onChatClick = { message(post.userId) },
    onSwapClick = { initiateSwap(post) },
    isOwnPost = post.userId == currentUserId,
    userName = "John Doe"
)
```

**Structure**:
```
┌─────────────────────────────┐
│ User Name         Days Ago   │
│                             │
│ Looking For │ Offering       │
│ [Skill A]   │ [Skill B]      │
│                             │
│ Description text here...    │
│ ... continued if needed     │
│                             │
│ [Message]  [Propose Swap]   │
└─────────────────────────────┘
```

**Properties**:
- Padding: 16dp
- Gap between items: 12dp
- Action buttons: Full width
- Elevation: 1dp default

**Usage**:
```kotlin
LazyColumn {
    items(posts) { post ->
        PostCard(
            post = post,
            onChatClick = { onChat(post.userId) },
            onSwapClick = { onSwap(post) },
            isOwnPost = post.userId == myId,
            userName = userMap[post.userId]?.name ?: "User"
        )
    }
}
```

---

### SkillCard

**Purpose**: Display individual skills  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
SkillCard(
    skill = skill,
    onActionClick = { requestSkill(skill) },
    actionText = "Request",
    isActionLoading = isRequesting
)
```

**Structure**:
```
┌────────────────────────┐
│ Skill Name  │          │
│ Description │ [Request]│
│ [Category]  │          │
└────────────────────────┘
```

**Properties**:
- Skill name: 16sp SemiBold
- Description: 12sp, 2 lines max
- Category badge: Secondary container
- Action button: Primary

**Usage**:
```kotlin
Column {
    skill.forEach { sk ->
        SkillCard(
            skill = sk,
            onActionClick = { requestSkill(sk) }
        )
    }
}
```

---

### ProfileCard

**Purpose**: Display user profile  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
ProfileCard(
    user = user,
    onEditClick = { navigateToEdit() }
)
```

**Structure**:
```
┌──────────────────┐
│      [Avatar]    │  48dp circle with initials
│    John Doe      │  User name
│                  │
│  [Edit Profile]  │  Button
└──────────────────┘
```

**Properties**:
- Background: Primary container
- Avatar: 72dp circle
- Button: Full width

---

### ScoreDisplayCard

**Purpose**: Show Skill Points and Trust Score  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
ScoreDisplayCard(
    skillPoints = 250,
    trustScore = 95
)
```

**Structure**:
```
┌─────────────────────────────┐
│  450         │       95      │
│  Skill       │  Trust        │
│  Points      │  Score        │
└─────────────────────────────┘
```

**Properties**:
- Side-by-side layout
- Large numbers (Headline)
- Labels below
- Divider between

---

## Input Fields

### PremiumTextField

**Purpose**: Text input with validation  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
PremiumTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email Address",
    placeholder = "Enter your email",
    isError = validationErrors.containsKey("email"),
    errorText = validationErrors["email"] ?: "",
    leadingIcon = { Icon(Icons.Default.Email) }
)
```

**Properties**:
- Height: 56dp
- Shape: 8dp rounded
- Border: Outline style
- Focus color: Primary

**States**:
- Normal: Outline border
- Focused: Primary border, cursor
- Error: Error red border, error text below
- Disabled: Gray background

**Validation Example**:
```kotlin
PremiumTextField(
    value = name,
    onValueChange = { name = it },
    label = "Full Name",
    isError = name.isEmpty() && touched,
    errorText = if (name.isEmpty() && touched) 
        "Name is required" else "",
    leadingIcon = { Icon(Icons.Default.Person) }
)
```

---

### SkillChip

**Purpose**: Removable skill tags  
**File**: `ui/components/PremiumComponents.kt`

```kotlin
SkillChip(
    text = "Photography",
    onRemove = { removeSkill("Photography") }
)
```

**Properties**:
- Background: Surface variant
- Selected background: Primary container
- Padding: 12dp horizontal, 6dp vertical
- Border: None (filled background)

**Usage in List**:
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(Alignment.Top),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.Top
) {
    skills.forEach { skill ->
        SkillChip(
            text = skill,
            onRemove = { removeSkill(skill) },
            isSelected = selectedSkills.contains(skill)
        )
    }
}
```

---

## State Components

### FullScreenLoading

**Purpose**: Full-screen loading indicator  
**File**: `ui/components/StateComponents.kt`

```kotlin
FullScreenLoading(message = "Loading your profile...")
```

**Structure**:
```
         [Spinner]
      
    Loading your profile...
```

**Properties**:
- Centered on screen
- Spinner: Primary color
- Message: Optional, below spinner
- Fill: Entire screen

**Usage**:
```kotlin
when (state) {
    is Resource.Loading -> {
        FullScreenLoading("Fetching posts...")
    }
    is Resource.Success -> { /* Content */ }
}
```

---

### ErrorView

**Purpose**: Error state with retry  
**File**: `ui/components/StateComponents.kt`

```kotlin
ErrorView(
    message = "Network connection lost. Please try again.",
    onRetry = { viewModel.retry() }
)
```

**Structure**:
```
       [Error Icon]
       
  Something Went Wrong
  
  Network connection lost.
  Please try again.
  
     [Try Again]
```

**Properties**:
- Icon: Error in error container
- Title: "Something Went Wrong"
- Message: Custom error text
- Button: Optional retry action

**Usage**:
```kotlin
when (state) {
    is Resource.Error -> {
        ErrorView(
            message = (state as Resource.Error).message 
                ?: "Unknown error",
            onRetry = { viewModel.loadData() }
        )
    }
}
```

---

### EmptyView

**Purpose**: Empty state with action  
**File**: `ui/components/StateComponents.kt`

```kotlin
EmptyView(
    title = "No Posts Yet",
    message = "Create your first skill post to get started!",
    action = "Create Post",
    onAction = { navigateToCreate() }
)
```

**Structure**:
```
       [Search Icon]
       
    No Posts Yet
    
Create your first skill post
    to get started!
    
      [Create Post]
```

**Properties**:
- Icon: Customizable
- Title: Descriptive
- Message: Helpful
- Action: Optional button

**Usage**:
```kotlin
if (posts.isEmpty()) {
    EmptyView(
        title = "No Results",
        message = "Try searching for a different skill",
        icon = Icons.Default.Search
    )
} else {
    LazyColumn { /* Show posts */ }
}
```

---

## Navigation

### PremiumTopAppBar

**Purpose**: Screen header with optional search  
**File**: `ui/components/NavigationComponents.kt`

```kotlin
// Simple header
PremiumTopAppBar(title = "Skill Board")

// With search
PremiumTopAppBar(
    title = "Search",
    onSearchChange = viewModel::onSearchQueryChanged,
    searchQuery = searchQuery
)
```

**Properties**:
- Height: 64dp
- Title: Centered
- Search: Inline text field
- Actions: Icons on right

**Usage**:
```kotlin
Scaffold(
    topBar = {
        PremiumTopAppBar(
            title = "My Profile",
            navigationIcon = {
                IconButton(onClick = { back() }) {
                    Icon(Icons.Default.ArrowBack)
                }
            }
        )
    }
)
```

---

### PremiumNavigationBar

**Purpose**: Bottom navigation  
**File**: `ui/components/NavigationComponents.kt`

```kotlin
PremiumNavigationBar(
    selectedItem = currentTab,
    onItemSelected = { setTab(it) },
    items = listOf(
        NavigationItem(Icons.Default.Home, "Home", "home"),
        NavigationItem(Icons.Default.Search, "Explore", "explore"),
        NavigationItem(Icons.Default.Message, "Chat", "chat"),
        NavigationItem(Icons.Default.AccountCircle, "Profile", "profile")
    )
)
```

**Properties**:
- Height: 80dp
- Items: 4-5 maximum
- Icons: 24dp
- Labels: Always visible
- Active: Primary container background

**Usage**:
```kotlin
Scaffold(
    bottomBar = {
        PremiumNavigationBar(
            selectedItem = selectedTab,
            onItemSelected = { navigateTo(tabs[it]) },
            items = navigationItems
        )
    }
) { padding ->
    /* Screen content */
}
```

---

## Dialogs & Sheets

### PremiumAlertDialog

**Purpose**: Confirmation dialogs  
**File**: `ui/components/NavigationComponents.kt`

```kotlin
PremiumAlertDialog(
    title = "Delete Post?",
    message = "This action cannot be undone.",
    dismissText = "Keep",
    confirmText = "Delete",
    onDismiss = { showDialog = false },
    onConfirm = { deletePost() },
    isError = true
)
```

**Properties**:
- Title: Bold, descriptive
- Message: Clear explanation
- Buttons: Dismiss (outline), Confirm (filled)
- Error variant: Red confirm button

**Destructive Action Pattern**:
```kotlin
if (showDeleteDialog) {
    PremiumAlertDialog(
        title = "Cancel Swap?",
        message = "Canceling reduces trust by 5 points",
        dismissText = "Keep Swap",
        confirmText = "Cancel",
        onDismiss = { showDeleteDialog = false },
        onConfirm = { cancelSwap() },
        isError = true
    )
}
```

---

### PremiumSnackbar

**Purpose**: Temporary notifications  
**File**: `ui/components/NavigationComponents.kt`

```kotlin
val snackbarHostState = remember { SnackbarHostState() }

// Show success
snackbarHostState.showSnackbar(
    "Profile updated successfully"
)

// Show error
snackbarHostState.showSnackbar(
    "Failed to update profile",
    duration = SnackbarDuration.Long
)
```

**Usage in Scaffold**:
```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) }
) { padding ->
    LaunchedEffect(result) {
        when (result) {
            is Success -> snackbarHostState.showSnackbar("Success!")
            is Error -> snackbarHostState.showSnackbar("Error")
        }
    }
}
```

---

## Lists & Layouts

### Basic List

```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    items(items, key = { it.id }) { item ->
        PostCard(item)
    }
}
```

**Key Points**:
- `contentPadding`: 16dp on all sides
- `verticalArrangement`: 12dp between items
- `key` parameter: Required for efficient recomposition

### Pull-to-Refresh List

```kotlin
var isRefreshing by remember { mutableStateOf(false) }

PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = {
        isRefreshing = true
        viewModel.refresh()
        isRefreshing = false
    }
) {
    LazyColumn { /* Items */ }
}
```

### Paginated List

```kotlin
LazyColumn {
    items(posts, key = { it.id }) { post ->
        PostCard(post)
    }
    
    if (hasMore) {
        item {
            Button(
                onClick = { loadMore() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Load More")
            }
        }
    }
}
```

---

## Best Practices

### 1. Loading States

Always show loading indicator when making requests:

```kotlin
val uiState by viewModel.uiState.collectAsState()

when (uiState) {
    is Resource.Loading -> {
        FullScreenLoading("Saving...")
    }
    is Resource.Success -> {
        // Content
    }
    is Resource.Error -> {
        ErrorView(onRetry)
    }
}
```

### 2. Error Handling

Provide clear error messages and retry options:

```kotlin
is Resource.Error -> {
    ErrorView(
        message = (uiState as Resource.Error).message 
            ?: "Something went wrong",
        onRetry = { viewModel.loadData() }
    )
}
```

### 3. Empty States

Show helpful empty state when no data:

```kotlin
if (items.isEmpty()) {
    EmptyView(
        title = "No Items",
        message = "Create your first item to get started",
        action = "Create",
        onAction = { navigate() }
    )
} else {
    LazyColumn { /* Content */ }
}
```

### 4. Form Validation

Display inline validation errors:

```kotlin
PremiumTextField(
    value = email,
    onValueChange = { email = it },
    label = "Email",
    isError = errors.containsKey("email"),
    errorText = errors["email"] ?: ""
)
```

### 5. Disabled States

Always disable buttons during async operations:

```kotlin
PremiumButton(
    text = "Save",
    onClick = { save() },
    enabled = !isSaving,
    isLoading = isSaving
)
```

### 6. Navigation

Use consistent navigation patterns:

```kotlin
Scaffold(
    topBar = { PremiumTopAppBar("Title") },
    bottomBar = { PremiumNavigationBar(...) },
    floatingActionButton = { PremiumFloatingButton(...) }
) { padding ->
    Column(modifier = Modifier.padding(padding)) {
        /* Content */
    }
}
```

### 7. Spacing

Use consistent spacing from the system:

```kotlin
// 16dp padding on sides
// 12dp between items
// 24dp between sections

LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    // Items here
}
```

### 8. Performance

Optimize list rendering:

```kotlin
// ✅ Good
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp)
) {
    items(items, key = { it.id }) { item ->
        ItemCard(item)
    }
}

// ❌ Avoid
Column {
    items.forEach { item ->  // Recomposes all
        ItemCard(item)
    }
}
```

---

## Quick Reference

| Component | Purpose | File |
|-----------|---------|------|
| `PremiumButton` | Primary action | PremiumComponents.kt |
| `OutlinedPremiumButton` | Secondary action | PremiumComponents.kt |
| `PremiumFloatingButton` | FAB | PremiumComponents.kt |
| `PostCard` | Post display | PremiumComponents.kt |
| `SkillCard` | Skill display | PremiumComponents.kt |
| `ProfileCard` | Profile display | PremiumComponents.kt |
| `PremiumTextField` | Text input | PremiumComponents.kt |
| `FullScreenLoading` | Loading state | StateComponents.kt |
| `ErrorView` | Error state | StateComponents.kt |
| `EmptyView` | Empty state | StateComponents.kt |
| `PremiumTopAppBar` | Header | NavigationComponents.kt |
| `PremiumNavigationBar` | Bottom nav | NavigationComponents.kt |
| `PremiumAlertDialog` | Dialog | NavigationComponents.kt |

---

## Support

**Questions?** Contact: Development Team  
**Slack**: #skillexchange-ui  
**Docs**: [UI_UX_GUIDELINES.md](UI_UX_GUIDELINES.md)  

---

**Document Version**: 1.0  
**Last Updated**: May 11, 2026
