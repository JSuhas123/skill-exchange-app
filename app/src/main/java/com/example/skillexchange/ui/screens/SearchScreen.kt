package com.example.skillexchange.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.data.model.Skill
import com.example.skillexchange.ui.components.EmptyView
import com.example.skillexchange.ui.components.ErrorView
import com.example.skillexchange.ui.components.FullScreenLoading
import com.example.skillexchange.ui.components.PremiumSkillCard
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Skill Repository", style = MaterialTheme.typography.titleLarge) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchSkills(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                placeholder = { Text("Search expertise (e.g. plumbing, excel...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val state = searchResults) {
                    is Resource.Loading -> FullScreenLoading()
                    is Resource.Error -> ErrorView(
                        message = state.message ?: "Search Engine Unavailable",
                        onRetry = { viewModel.searchSkills(searchQuery) }
                    )
                    is Resource.Success -> {
                        val skills = state.data ?: emptyList()
                        if (skills.isEmpty()) {
                            if (searchQuery.isBlank()) {
                                EmptyView(message = "Type above to explore our community's vast library of skills.")
                            } else {
                                EmptyView(message = "No specific skills found for '$searchQuery'. Try a broader term.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(skills) { skill ->
                                    PremiumSkillCard(
                                        skill = skill,
                                        onActionClick = { /* TODO */ },
                                        actionText = "Exchange"
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
}
