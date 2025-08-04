package com.example.flexioffice.presentation.components

import androidx.compose.runtime.Composable
import com.example.flexioffice.data.model.Booking
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CancelBookingDialog(
    showDialog: Boolean,
    selectedBooking: Booking?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmCancel: () -> Unit,
) {
    ConfirmationDialog(
        showDialog = showDialog,
        type = ConfirmationDialogType.CancelBooking,
        onDismiss = onDismiss,
        onConfirm = onConfirmCancel,
        isLoading = isLoading,
        itemName = selectedBooking?.let { booking ->
            "Home Office am ${booking.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))}"
        },
        additionalInfo = selectedBooking?.comment?.takeIf { it.isNotBlank() }?.let { "Kommentar: $it" }
    )
}
