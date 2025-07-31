package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.presentation.RequestsViewModel
import com.example.flexioffice.presentation.components.EnterMultiSelectModeButton
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsScreen(viewModel: RequestsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Log.e("RequestsScreen", "Error: $message")
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState.isMultiSelectMode) {
                RequestsMultiSelectTopBar(
                    selectedCount = uiState.selectedRequests.size,
                    onExitMultiSelect = { viewModel.exitMultiSelectMode() },
                    onSelectAll = { viewModel.selectAllRequests() },
                    onClearSelection = { viewModel.clearSelection() },
                    onBatchApprove = { viewModel.batchApproveRequests() },
                    onBatchDecline = { viewModel.batchDeclineRequests() },
                    isBatchProcessing = uiState.isBatchProcessing,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.assignment_24px),
                    contentDescription = "Buchungsanfragen Icon",
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(text = "Buchungsanfragen", style = MaterialTheme.typography.headlineMedium)
                EnterMultiSelectModeButton(
                    isMultiselectMode = uiState.isMultiSelectMode,
                    onEnterMultiSelectMode = viewModel::startMultiSelectMode,
                    isListEmpty = uiState.pendingRequests.isEmpty(),
                )
            }

            Text(
                text = "Genehmigen oder lehnen Sie Buchungsanfragen ab",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            if (uiState.isLoading) {
                // Loading state
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Lade Anfragen...",
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (uiState.pendingRequests.isEmpty()) {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.assignment_24px),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Keine ausstehenden Anfragen",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Text(
                            text = "Alle Buchungsanfragen wurden bereits bearbeitet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else {
                // List of pending requests
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.pendingRequests) { booking ->
                        RequestItem(
                            booking = booking,
                            isProcessing = viewModel.isProcessingRequest(booking.id),
                            onApprove = { viewModel.approveRequest(booking) },
                            onDecline = { viewModel.declineRequest(booking) },
                            onLongClick = { viewModel.startMultiSelectMode(booking) },
                            isMultiSelectMode = uiState.isMultiSelectMode,
                            isSelected = uiState.selectedRequests.contains(booking.id),
                            onSelectionChanged = {
                                viewModel.toggleRequestSelection(booking.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RequestItem(
    booking: Booking,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onLongClick: () -> Unit = {},
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit = {},
) {
    val dateFormatter =
        remember {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isMultiSelectMode) {
                            onSelectionChanged(!isSelected)
                        }
                    },
                    onLongClick = { onLongClick() },
                ),
        colors =
            if (isSelected) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            } else {
                CardDefaults.cardColors()
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Multi-select Checkbox
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onSelectionChanged,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Request info
                    Text(
                        text = booking.userName,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Text(
                        text = "Home Office am ${booking.date.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                bottom =
                                    if (booking.comment.isNotBlank()) {
                                        8.dp
                                    } else {
                                        12.dp
                                    },
                            ),
                    )
                }
            }

            // Comment if present
            if (booking.comment.isNotBlank()) {
                Text(
                    text = "Kommentar: ${booking.comment}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            // Action buttons - only show if not in multi-select mode
            if (!isMultiSelectMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp).size(16.dp),
                            )
                        }
                        Text(
                            text = if (isProcessing) "Verarbeite..." else "Genehmigen",
                            modifier =
                                Modifier.padding(start = if (isProcessing) 8.dp else 0.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                    ) {
                        if (!isProcessing) {
                            Icon(
                                painter = painterResource(R.drawable.cancel_24px),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp).size(16.dp),
                            )
                        }
                        Text(text = "Ablehnen")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsMultiSelectTopBar(
    selectedCount: Int,
    onExitMultiSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onBatchApprove: () -> Unit,
    onBatchDecline: () -> Unit,
    isBatchProcessing: Boolean,
) {
    TopAppBar(
        title = { Text("$selectedCount ausgewählt") },
        navigationIcon = {
            IconButton(onClick = onExitMultiSelect) {
                Icon(Icons.Default.Close, contentDescription = "Multi-Select beenden")
            }
        },
        actions = {
            if (selectedCount > 0) {
                IconButton(
                    onClick = onBatchApprove,
                    enabled = !isBatchProcessing,
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Ausgewählte genehmigen",
                    )
                }
                IconButton(
                    onClick = onBatchDecline,
                    enabled = !isBatchProcessing,
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.cancel_24px),
                        contentDescription = "Ausgewählte ablehnen",
                    )
                }
                IconButton(onClick = onClearSelection) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.check_box_outline_blank_24px),
                        contentDescription = "Auswahl aufheben",
                    )
                }
            }
            IconButton(onClick = onSelectAll) {
                Icon(
                    ImageVector.vectorResource(R.drawable.check_box_24px),
                    contentDescription = "Alle auswählen",
                )
            }
        },
    )
}
