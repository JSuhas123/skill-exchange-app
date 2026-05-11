package com.example.skillexchange.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.data.model.Post
import com.example.skillexchange.ui.components.*
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.SkillBoardViewModel
import com.example.skillexchange.viewmodel.SwapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillBoardScreenRefactored(
    onChatClick: (String) -> Unit,
    viewModel: SkillBoardViewModel = hiltViewModel(),
    swapViewModel: SwapViewModel = hiltViewModel()
) {
    val uiState by viewModel.filteredPosts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val actionStatus by swapViewModel.actionStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    // Handle action status
    LaunchedEffect(actionStatus) {
        when (actionStatus) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar(
                    "Swap proposal sent successfully",
                    duration = SnackbarDuration.Short
                )
                swapViewModel.resetActionStatus()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    "Error: ${(actionStatus as Resource.Error).message ?: "Failed to send proposal"}",
                    duration = SnackbarDuration.Long
                )
                swapViewModel.resetActionStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PremiumTopAppBar(
                title = "Skill Board",
                onSearchChange = viewModel::onSearchQueryChanged,
                searchQuery = searchQuery
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
                    FullScreenLoading("Fetching skill posts...")
                }
                is Resource.Error -> {
                    ErrorView(
                        message = state.message ?: "Failed to load posts. Please check your connection.",
                        onRetry = { viewModel.onSearchQueryChanged(searchQuery) }
                    )
                }
                is Resource.Success -> {
                    val posts = state.data ?: emptyList()
                    if (posts.isEmpty()) {
                        EmptyView(
                            title = "No Skill Posts Found",
                            message = "No posts match your search. Try adjusting your filters or create your own post!",
                            action = "Create Post",
                            onAction = { /* Navigate to create post */ }
                        )
                    } else {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                viewModel.onSearchQueryChanged(searchQuery)
                                isRefreshing = false
                            }
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = posts,
                                    key = { post -> post.id }
                                ) { post ->
                                    PostCard(
                                        post = post,
                                        onChatClick = { onChatClick(post.userId) },
                                        onSwapClick = {
                                            swapViewModel.initiateSwap(
                                                userB = post.userId,
                                                skillA = post.skillRequired,
                                                skillB = post.skillOffered
                                            )
                                        },
                                        isOwnPost = post.userId == swapViewModel.currentUserId,
                                        isSwapLoading = swapViewModel.pendingActions.contains(post.id),
                                        userName = "Skill Poster" // In real app, fetch from user repository
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
                else -> FullScreenLoading()
            }
        }
    }
}
