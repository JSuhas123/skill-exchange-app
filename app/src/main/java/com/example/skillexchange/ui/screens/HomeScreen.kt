package com.example.skillexchange.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.ui.components.EmptyView
import com.example.skillexchange.ui.components.ErrorView
import com.example.skillexchange.ui.components.FullScreenLoading
import com.example.skillexchange.ui.components.PremiumSkillCard
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.SkillViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: SkillViewModel = hiltViewModel()) {
    val skillsState by viewModel.skills.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Available Resources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* Help or Info */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Information")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val resource = skillsState) {
                is Resource.Loading -> FullScreenLoading()
                is Resource.Error -> ErrorView(
                    message = resource.message ?: "Sync Error",
                    onRetry = { viewModel.fetchSkills() }
                )
                is Resource.Success -> {
                    val skills = resource.data ?: emptyList()
                    if (skills.isEmpty()) {
                        EmptyView(message = "The skill repository is currently being updated. Check back shortly.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Discover expertise in your neighborhood and start an exchange.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(skills) { skill ->
                                PremiumSkillCard(
                                    skill = skill,
                                    onActionClick = { /* Navigate to swap proposal */ },
                                    actionText = "Request"
                                )
                            }
                        }
                    }
                }
                is Resource.Idle -> {}
            }
        }
    }
}
