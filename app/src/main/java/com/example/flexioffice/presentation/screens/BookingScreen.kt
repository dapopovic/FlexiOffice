package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.presentation.BookingViewModel
import com.example.flexioffice.presentation.components.BookingDatePickerDialog
import com.example.flexioffice.presentation.components.BookingDetailsSheet
import com.example.flexioffice.presentation.components.BookingDialog
import com.example.flexioffice.presentation.components.BookingFloatingActionButton
import com.example.flexioffice.presentation.components.BookingItem
import com.example.flexioffice.presentation.components.BookingScreenHeader
import com.example.flexioffice.presentation.components.CancelBookingDialog
import com.example.flexioffice.presentation.components.EmptyBookingsCard

@Composable
fun BookingScreen(
    viewModel: BookingViewModel = hiltViewModel(),
    selectedDate: String? = null,
) {
    // Wenn ein Datum übergeben wurde, zeige den Dialog
    LaunchedEffect(selectedDate) {
        selectedDate?.let { dateStr ->
            viewModel.showBookingDialog(java.time.LocalDate.parse(dateStr))
        }
    }
    val uiState by viewModel.uiState.collectAsState()

    // Cancel booking dialog
    CancelBookingDialog(
        showDialog = uiState.showCancelDialog,
        selectedBooking = uiState.selectedBooking,
        isLoading = uiState.isLoading,
        onDismiss = { viewModel.hideCancelDialog() },
        onConfirmCancel = { viewModel.cancelBooking() },
    )

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Log.e("BookingScreen", "Error: $message")
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState.isMultiSelectMode) {
                MultiSelectTopBar(
                    selectedCount = uiState.selectedBookings.size,
                    onExitMultiSelect = { viewModel.exitMultiSelectMode() },
                    onSelectAll = { viewModel.selectAllBookings() },
                    onClearSelection = { viewModel.clearSelection() },
                    onBatchCancel = { viewModel.batchCancelBookings() },
                    isBatchProcessing = uiState.isBatchProcessing,
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isMultiSelectMode) {
                BookingFloatingActionButton(
                    onCreateBookingClick = { viewModel.showBookingDialog() },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                BookingScreenHeader(
                    showCancelledBookings = uiState.showCancelledBookings,
                    onToggleCancelledBookings = { viewModel.toggleCancelledBookings() },
                    onToggleMultiSelectView = { viewModel.startMultiSelectMode() },
                    isMultiselectMode = uiState.isMultiSelectMode,
                    isBookingListEmpty =
                        uiState.userBookings.none { booking ->
                            booking.status != BookingStatus.CANCELLED
                        },
                )
            }

            if (uiState.userBookings.isEmpty()) {
                item { EmptyBookingsCard() }
            } else {
                items(
                    uiState.userBookings
                        .filter { booking ->
                            uiState.showCancelledBookings ||
                                booking.status != BookingStatus.CANCELLED
                        }.sortedByDescending { it.date },
                ) { booking ->
                    BookingItem(
                        booking = booking,
                        onClick = { viewModel.showDetailsSheet(it) },
                        onCancelClick = { viewModel.showCancelDialog(it) },
                        onLongClick = { viewModel.startMultiSelectMode(booking) },
                        isMultiSelectMode = uiState.isMultiSelectMode,
                        isSelected = uiState.selectedBookings.contains(booking.id),
                        onSelectionChanged = { viewModel.toggleBookingSelection(booking.id) },
                    )
                }
            }
        }
    }

    // Booking creation dialog
    BookingDialog(
        showDialog = uiState.showBookingDialog,
        selectedDate = uiState.selectedDate,
        comment = uiState.comment,
        error = uiState.error,
        isLoading = uiState.isLoading,
        onDismiss = { viewModel.hideBookingDialog() },
        onDateClick = { viewModel.showDatePicker() },
        onCommentChange = { viewModel.updateComment(it) },
        onCreateBooking = { viewModel.createBooking() },
    )

    // Booking details bottom sheet
    BookingDetailsSheet(
        showSheet = uiState.showDetailsSheet,
        booking = uiState.selectedBooking,
        approverName = uiState.approverName,
        onDismiss = { viewModel.hideDetailsSheet() },
    )

    // Date picker dialog
    BookingDatePickerDialog(
        showDatePicker = uiState.showDatePicker,
        selectedDate = uiState.selectedDate,
        onDismiss = { viewModel.hideDatePicker() },
        onDateSelected = { date -> viewModel.updateSelectedDate(date) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onExitMultiSelect: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onBatchCancel: () -> Unit,
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
                    onClick = onBatchCancel,
                    enabled = !isBatchProcessing,
                ) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.cancel_24px),
                        contentDescription = "Ausgewählte stornieren",
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
