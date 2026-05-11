package com.example.skillexchange.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.data.model.User
import com.example.skillexchange.ui.components.FullScreenLoading
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var skillsOfferedText by remember { mutableStateOf("") }
    var skillsNeededText by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is Resource.Success) {
            val user = (uiState as Resource.Success).data
            name = user?.name ?: ""
            skillsOfferedText = user?.skillsOffered?.joinToString(", ") ?: ""
            skillsNeededText = user?.skillsNeeded?.joinToString(", ") ?: ""
        }
    }

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Profile updated successfully")
                viewModel.resetSaveStatus()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar("Update failed: ${(saveStatus as Resource.Error).message}")
                viewModel.resetSaveStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile Settings", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {
                        viewModel.signOut()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is Resource.Loading -> FullScreenLoading()
                is Resource.Error -> {
                    Text(
                        text = state.message ?: "Sync Error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is Resource.Success -> {
                    ProfileContent(
                        user = state.data,
                        name = name,
                        onNameChange = { name = it },
                        skillsOffered = skillsOfferedText,
                        onOfferedChange = { skillsOfferedText = it },
                        skillsNeeded = skillsNeededText,
                        onNeededChange = { skillsNeededText = it },
                        isSaving = saveStatus is Resource.Loading,
                        onSave = {
                            val offered = skillsOfferedText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val needed = skillsNeededText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            viewModel.saveProfile(name, offered, needed)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User?,
    name: String,
    onNameChange: (String) -> Unit,
    skillsOffered: String,
    onOfferedChange: (String) -> Unit,
    skillsNeeded: String,
    onNeededChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Stats Overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                label = "Trust Score",
                value = "${user?.trustScore ?: 0}",
                icon = Icons.Default.VerifiedUser,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Skill Points",
                value = "${user?.skillPoints ?: 0}",
                icon = Icons.Default.Token,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Skills & Expertise",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        ProfileField(
            value = skillsOffered,
            onValueChange = onOfferedChange,
            label = "Skills You Offer",
            placeholder = "e.g. Gardening, Basic IT, Tailoring",
            icon = Icons.Default.Handshake
        )

        Spacer(modifier = Modifier.height(16.dp))

        ProfileField(
            value = skillsNeeded,
            onValueChange = onNeededChange,
            label = "Skills You Need",
            placeholder = "e.g. English Speaking, Carpentry",
            icon = Icons.Default.Psychology
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), 
                    color = MaterialTheme.colorScheme.onPrimary, 
                    strokeWidth = 3.dp
                )
            } else {
                Text("Update Official Profile", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ProfileField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, icon: ImageVector) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        leadingIcon = { Icon(icon, contentDescription = null) },
        supportingText = { Text("Use commas to separate multiple skills") }
    )
}
