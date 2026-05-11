package com.example.skillexchange.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.example.skillexchange.data.model.Swap
import com.example.skillexchange.ui.components.EmptyView
import com.example.skillexchange.ui.components.ErrorView
import com.example.skillexchange.ui.components.FullScreenLoading
import com.example.skillexchange.utils.Resource
import com.example.skillexchange.viewmodel.SwapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapScreen(
    viewModel: SwapViewModel = hiltViewModel()
) {
    val swapsState by viewModel.swaps.collectAsState()
    val actionStatus by viewModel.actionStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionStatus) {
        if (actionStatus is Resource.Success) {
            snackbarHostState.showSnackbar("Agreement status updated")
            viewModel.resetActionStatus()
        } else if (actionStatus is Resource.Error) {
            snackbarHostState.showSnackbar("Action failed: ${(actionStatus as Resource.Error).message}")
            viewModel.resetActionStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Exchange Agreements", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (val state = swapsState) {
                is Resource.Loading -> FullScreenLoading()
                is Resource.Error -> ErrorView(
                    message = state.message ?: "Database Sync Issue",
                    onRetry = { viewModel.fetchSwaps() }
                )
                is Resource.Success -> {
                    val swaps = state.data ?: emptyList()
                    if (swaps.isEmpty()) {
                        EmptyView(message = "You have no active skill exchange agreements.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(swaps) { swap ->
                                val isCurrentUserA = swap.userA == viewModel.currentUserId
                                val alreadyConfirmed = if (isCurrentUserA) swap.confirmedA else swap.confirmedB
                                
                                PremiumSwapCard(
                                    swap = swap,
                                    alreadyConfirmed = alreadyConfirmed,
                                    isUserB = swap.userB == viewModel.currentUserId,
                                    onAccept = { viewModel.acceptSwap(swap.id) },
                                    onComplete = { viewModel.confirmCompletion(swap.id) },
                                    onCancel = { viewModel.cancelSwap(swap.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumSwapCard(
    swap: Swap,
    alreadyConfirmed: Boolean,
    isUserB: Boolean,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusTag(status = swap.status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${swap.hours}h Session",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Swap Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SwapPart(label = "Provision", skill = swap.skillA, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                SwapPart(label = "Acquisition", skill = swap.skillB, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f), alignEnd = true)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (swap.status != "completed" && swap.status != "cancelled") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (swap.status == "pending" && isUserB) {
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept Terms")
                        }
                    } else if (swap.status == "accepted") {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            enabled = !alreadyConfirmed,
                            shape = MaterialTheme.shapes.large,
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (alreadyConfirmed) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (alreadyConfirmed) "Wait for other" else "Confirm Completion")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(0.7f),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Void")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusTag(status: String) {
    val (color, label) = when (status) {
        "pending" -> Color(0xFFFFA000) to "PENDING APPROVAL"
        "accepted" -> Color(0xFF388E3C) to "IN PROGRESS"
        "completed" -> Color(0xFF1976D2) to "SUCCESSFUL"
        "cancelled" -> Color(0xFFD32F2F) to "VOIDED"
        else -> MaterialTheme.colorScheme.outline to status.uppercase()
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SwapPart(label: String, skill: String, color: Color, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier = modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = skill,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
