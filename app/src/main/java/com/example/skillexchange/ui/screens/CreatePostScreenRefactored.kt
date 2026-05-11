package com.example.skillexchange.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.ui.components.*
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.CreatePostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreenRefactored(
    onSuccess: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val validationErrors by viewModel.validationErrors.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var skillRequired by remember { mutableStateOf("") }
    var skillOffered by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Handle creation result
    LaunchedEffect(uiState) {
        when (uiState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    "Post created successfully!",
                    duration = SnackbarDuration.Short
                )
                onSuccess()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    "Error: ${(uiState as Resource.Error).message}",
                    duration = SnackbarDuration.Long
                )
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
                        "Create Skill Post",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onSuccess) {
                        Icon(Icons.Default.Close, "Close")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Describe your skill exchange",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    PremiumTextField(
                        value = skillRequired,
                        onValueChange = { skillRequired = it },
                        label = "Skill You're Looking For",
                        placeholder = "e.g., English Tutoring",
                        isError = validationErrors.containsKey("skillRequired"),
                        errorText = validationErrors["skillRequired"] ?: "",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                "Skill",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }

                item {
                    PremiumTextField(
                        value = skillOffered,
                        onValueChange = { skillOffered = it },
                        label = "Skill You're Offering",
                        placeholder = "e.g., Computer Skills",
                        isError = validationErrors.containsKey("skillOffered"),
                        errorText = validationErrors["skillOffered"] ?: "",
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Offering",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }

                item {
                    PremiumTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Details",
                        placeholder = "Tell more about your request and what you can offer",
                        maxLines = 5,
                        isError = validationErrors.containsKey("description"),
                        errorText = validationErrors["description"] ?: "",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Description,
                                "Details",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Tips for a Great Post:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            listOf(
                                "Be clear about what you need",
                                "Describe your skills in detail",
                                "Mention your availability",
                                "Be respectful and honest"
                            ).forEach { tip ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        "Tip",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedPremiumButton(
                    text = "Cancel",
                    onClick = onSuccess,
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is Resource.Loading
                )
                PremiumButton(
                    text = "Create Post",
                    onClick = {
                        viewModel.createPost(skillRequired, skillOffered, description)
                    },
                    modifier = Modifier.weight(1f),
                    isLoading = uiState is Resource.Loading
                )
            }
        }
    }
}
