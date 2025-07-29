package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    selectedDate: String? = null
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
        floatingActionButton = {
            BookingFloatingActionButton(
                onCreateBookingClick = { viewModel.showBookingDialog() },
            )
        },
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                BookingScreenHeader(
                    showCancelledBookings = uiState.showCancelledBookings,
                    onToggleCancelledBookings = { viewModel.toggleCancelledBookings() },
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
        onDateSelected = { selectedDate -> viewModel.updateSelectedDate(selectedDate) },
    )
}
