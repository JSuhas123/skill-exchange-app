package com.example.skillexchange.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.data.model.User
import com.example.skillexchange.ui.components.*
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenRefactored(
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditMode by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedSkillsOffered by remember { mutableStateOf("") }
    var editedSkillsNeeded by remember { mutableStateOf("") }

    // Handle save status
    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    "Profile updated successfully",
                    duration = SnackbarDuration.Short
                )
                viewModel.resetSaveStatus()
                isEditMode = false
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    "Error: ${(saveStatus as Resource.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetSaveStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Handle back */ }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle settings */ }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is Resource.Loading -> {
                    FullScreenLoading("Loading profile...")
                }
                is Resource.Error -> {
                    ErrorView(
                        message = (state as Resource.Error).message ?: "Failed to load profile",
                        onRetry = { /* Retry logic */ }
                    )
                }
                is Resource.Success -> {
                    val user = state.data ?: return@Scaffold

                    if (isEditMode) {
                        EditProfileContent(
                            user = user,
                            editedName = editedName,
                            editedSkillsOffered = editedSkillsOffered,
                            editedSkillsNeeded = editedSkillsNeeded,
                            onNameChange = { editedName = it },
                            onSkillsOfferedChange = { editedSkillsOffered = it },
                            onSkillsNeededChange = { editedSkillsNeeded = it },
                            validationErrors = validationErrors,
                            onSave = {
                                viewModel.saveProfile(
                                    editedName,
                                    editedSkillsOffered,
                                    editedSkillsNeeded
                                )
                            },
                            onCancel = {
                                isEditMode = false
                                editedName = user.name
                                editedSkillsOffered = user.skillsOffered.joinToString(", ")
                                editedSkillsNeeded = user.skillsNeeded.joinToString(", ")
                            },
                            isSaving = saveStatus is Resource.Loading
                        )
                    } else {
                        ViewProfileContent(
                            user = user,
                            onEditClick = {
                                editedName = user.name
                                editedSkillsOffered = user.skillsOffered.joinToString(", ")
                                editedSkillsNeeded = user.skillsNeeded.joinToString(", ")
                                isEditMode = true
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
                else -> FullScreenLoading()
            }
        }
    }
}

@Composable
private fun ViewProfileContent(
    user: User,
    onEditClick: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileCard(user = user, onEditClick = onEditClick)
        }

        item {
            ScoreDisplayCard(
                skillPoints = user.skillPoints,
                trustScore = user.trustScore
            )
        }

        item {
            ExpandableSection(
                title = "Skills Offered",
                isExpanded = true,
                onToggle = {}
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    user.skillsOffered.forEach { skill ->
                        SkillChip(text = skill)
                    }
                    if (user.skillsOffered.isEmpty()) {
                        Text(
                            "No skills offered yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            ExpandableSection(
                title = "Skills Needed",
                isExpanded = true,
                onToggle = {}
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    user.skillsNeeded.forEach { skill ->
                        SkillChip(text = skill)
                    }
                    if (user.skillsNeeded.isEmpty()) {
                        Text(
                            "No skills needed yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            PremiumButton(
                text = "Edit Profile",
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary
            )
        }

        item {
            OutlinedPremiumButton(
                text = "Sign Out",
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EditProfileContent(
    user: User,
    editedName: String,
    editedSkillsOffered: String,
    editedSkillsNeeded: String,
    onNameChange: (String) -> Unit,
    onSkillsOfferedChange: (String) -> Unit,
    onSkillsNeededChange: (String) -> Unit,
    validationErrors: Map<String, String>,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            PremiumTextField(
                value = editedName,
                onValueChange = onNameChange,
                label = "Full Name",
                isError = validationErrors.containsKey("name"),
                errorText = validationErrors["name"] ?: ""
            )
        }

        item {
            PremiumTextField(
                value = editedSkillsOffered,
                onValueChange = onSkillsOfferedChange,
                label = "Skills Offered",
                placeholder = "Separate with commas",
                maxLines = 3,
                isError = validationErrors.containsKey("skillsOffered"),
                errorText = validationErrors["skillsOffered"] ?: ""
            )
        }

        item {
            PremiumTextField(
                value = editedSkillsNeeded,
                onValueChange = onSkillsNeededChange,
                label = "Skills Needed",
                placeholder = "Separate with commas",
                maxLines = 3,
                isError = validationErrors.containsKey("skillsNeeded"),
                errorText = validationErrors["skillsNeeded"] ?: ""
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedPremiumButton(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
                PremiumButton(
                    text = "Save Changes",
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    isLoading = isSaving
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
