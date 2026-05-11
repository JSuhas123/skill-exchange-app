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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.ui.components.*
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.SwapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapDetailsScreenRefactored(
    swapId: String,
    onBack: () -> Unit,
    viewModel: SwapViewModel = hiltViewModel()
) {
    val swap by viewModel.getSwapDetails(swapId).collectAsState(initial = Resource.Loading())
    val actionStatus by viewModel.actionStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }

    // Handle action status
    LaunchedEffect(actionStatus) {
        when (actionStatus) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Swap updated successfully")
                viewModel.resetActionStatus()
                onBack()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    "Error: ${(actionStatus as Resource.Error).message}",
                    duration = SnackbarDuration.Long
                )
                viewModel.resetActionStatus()
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
                        "Swap Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            when (val state = swap) {
                is Resource.Loading -> {
                    FullScreenLoading("Loading swap details...")
                }
                is Resource.Error -> {
                    ErrorView(
                        message = (state as Resource.Error).message ?: "Failed to load swap",
                        onRetry = { /* Retry */ }
                    )
                }
                is Resource.Success -> {
                    val swapData = state.data ?: return@Scaffold
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SwapStatusCard(status = swapData.status)
                        }

                        item {
                            SkillExchangeCard(
                                skillFrom = swapData.skillA,
                                skillTo = swapData.skillB,
                                hoursProposed = swapData.hoursProposed
                            )
                        }

                        item {
                            ParticipantCard(
                                title = "You're Offering",
                                skill = swapData.skillA,
                                confirmed = swapData.confirmedA
                            )
                        }

                        item {
                            ParticipantCard(
                                title = "You're Receiving",
                                skill = swapData.skillB,
                                confirmed = swapData.confirmedB
                            )
                        }

                        item {
                            if (swapData.status == "pending") {
                                TimelineCard(
                                    createdAt = swapData.timestamp.seconds,
                                    status = swapData.status
                                )
                            }
                        }
                    }

                    // Action buttons
                    if (swapData.status == "pending" || swapData.status == "accepted") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (swapData.status == "pending") {
                                OutlinedPremiumButton(
                                    text = "Decline",
                                    onClick = { showCancelDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                                PremiumButton(
                                    text = "Accept",
                                    onClick = {
                                        viewModel.acceptSwap(swapId)
                                    },
                                    modifier = Modifier.weight(1f),
                                    isLoading = actionStatus is Resource.Loading
                                )
                            } else if (swapData.status == "accepted") {
                                PremiumButton(
                                    text = "Mark Complete",
                                    onClick = {
                                        viewModel.confirmCompletion(swapId)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isLoading = actionStatus is Resource.Loading
                                )
                            }
                        }
                    }
                }
                else -> FullScreenLoading()
            }
        }

        if (showCancelDialog) {
            PremiumAlertDialog(
                title = "Cancel Swap?",
                message = "This action cannot be undone. Canceling will reduce your trust score by 5 points.",
                dismissText = "Keep Swap",
                confirmText = "Cancel Swap",
                onDismiss = { showCancelDialog = false },
                onConfirm = {
                    viewModel.cancelSwap(swapId)
                    showCancelDialog = false
                },
                isError = true
            )
        }
    }
}

@Composable
private fun SwapStatusCard(status: String) {
    val (statusColor, statusIcon, statusText) = when (status) {
        "pending" -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.Schedule,
            "Pending Your Response"
        )
        "accepted" -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.CheckCircle,
            "Both Have Accepted"
        )
        "completed" -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Done,
            "Swap Completed"
        )
        "cancelled" -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Cancel,
            "Swap Cancelled"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            Icons.Default.Help,
            "Unknown Status"
        )
    }

    Surface(
        color = statusColor.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = "Status",
                tint = statusColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "Status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun SkillExchangeCard(
    skillFrom: String,
    skillTo: String,
    hoursProposed: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Skill Exchange Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Offering",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        skillFrom,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Icon(
                    Icons.Default.SwapHoriz,
                    "Swap",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Receiving",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        skillTo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    "Duration",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "Proposed Duration: $hoursProposed hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ParticipantCard(
    title: String,
    skill: String,
    confirmed: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        skill,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (confirmed) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Confirmed",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Confirmed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(createdAt: Long, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Timeline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Created",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Pending your response",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
