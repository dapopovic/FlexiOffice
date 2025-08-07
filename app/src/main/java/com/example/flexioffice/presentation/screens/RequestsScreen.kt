package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.presentation.RequestsViewModel
import com.example.flexioffice.presentation.components.Filters
import com.example.flexioffice.presentation.components.Header
import com.example.flexioffice.presentation.components.swipeableCard
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

    Column(modifier = Modifier.fillMaxSize()) {
        // TopBar when in multi-select mode
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

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
        ) {
            Header(
                modifier = Modifier.padding(bottom = 16.dp),
                title = stringResource(R.string.requests_title),
                iconVector = ImageVector.vectorResource(R.drawable.assignment_24px),
                iconDescription = stringResource(R.string.requests_icon_desc),
                isMultiSelectMode = uiState.isMultiSelectMode,
                doNotShowMultiSelectButton = uiState.pendingRequests.isEmpty(),
                onEnterMultiSelectMode = viewModel::startMultiSelectMode,
            )

            Text(
                text = stringResource(R.string.requests_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Filter nur für Manager anzeigen
            if (uiState.currentUser?.role == com.example.flexioffice.data.model.User.ROLE_MANAGER &&
                uiState.teamMembers.isNotEmpty()
            ) {
                Filters(
                    items = uiState.teamMembers.map { it.name },
                    selectedItem =
                        uiState.selectedTeamMember?.let { userId ->
                            uiState.teamMembers.find { it.id == userId }?.name
                        },
                    onItemSelected = { name ->
                        viewModel.setTeamMemberFilter(
                            uiState.teamMembers
                                .find {
                                    it.name == name
                                }?.id,
                        )
                    },
                    onClearFilters = { viewModel.clearFilters() },
                    defaultItem = stringResource(R.string.filters_all_members),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            if (uiState.isLoading) {
                // Loading state
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = stringResource(R.string.requests_loading),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (uiState.pendingRequests.isEmpty()) {
                // Empty state
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                            text = stringResource(R.string.requests_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Text(
                            text = stringResource(R.string.requests_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else {
                // List of pending requests
                LazyColumn(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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

    // Swipe-Feedback-Logik
    val approveColor = MaterialTheme.colorScheme.primary
    val declineColor = MaterialTheme.colorScheme.error
    val neutralColor = MaterialTheme.colorScheme.surfaceVariant
    var swipeBackgroundColor by remember { mutableStateOf<Color?>(neutralColor) }
    val swipeAlpha: (Float, Float) -> Float =
        remember {
            { offset, threshold -> (kotlin.math.abs(offset) / threshold).coerceAtMost(0.3f) }
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .swipeableCard(
                    onSwipeLeft = { if (!isProcessing) onDecline() },
                    onSwipeRight = { if (!isProcessing) onApprove() },
                    swipeThresholdFraction = 0.5f,
                    isEnabled = !isProcessing,
                    onOffsetChange = { offsetX, threshold ->
                        swipeBackgroundColor =
                            when {
                                offsetX > 0f -> approveColor.copy(alpha = swipeAlpha(offsetX, threshold))
                                offsetX < 0f -> declineColor.copy(alpha = swipeAlpha(offsetX, threshold))
                                else ->
                                    neutralColor.copy(
                                        alpha = swipeAlpha(offsetX, threshold),
                                    )
                            }
                    },
                ).combinedClickable(
                    onClick = {
                        if (isMultiSelectMode) {
                            onSelectionChanged(!isSelected)
                        }
                    },
                    onLongClick = { onLongClick() },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> neutralColor
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (swipeBackgroundColor != null) {
                            Modifier.background(swipeBackgroundColor!!)
                        } else {
                            Modifier
                        },
                    ).padding(20.dp),
        ) {
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
                        text =
                            stringResource(
                                R.string.request_item_date,
                                booking.date.format(dateFormatter),
                            ),
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
                    text = stringResource(R.string.request_item_comment, booking.comment),
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
                            text =
                                if (isProcessing) {
                                    stringResource(
                                        R.string.requests_action_processing,
                                    )
                                } else {
                                    stringResource(R.string.requests_action_approve)
                                },
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
                        Text(text = stringResource(R.string.requests_action_decline))
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
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.multi_select_exit))
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
                        contentDescription = stringResource(R.string.batch_approve_selected),
                    )
                }
                IconButton(
                    onClick = onBatchDecline,
                    enabled = !isBatchProcessing,
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.cancel_24px),
                        contentDescription = stringResource(R.string.batch_decline_selected),
                    )
                }
                IconButton(onClick = onClearSelection) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.check_box_outline_blank_24px),
                        contentDescription = stringResource(R.string.batch_clear_selection),
                    )
                }
            }
            IconButton(onClick = onSelectAll) {
                Icon(
                    ImageVector.vectorResource(R.drawable.check_box_24px),
                    contentDescription = stringResource(R.string.select_all),
                )
            }
        },
    )
}
