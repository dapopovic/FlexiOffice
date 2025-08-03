package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking

@Composable
fun CancelBookingDialog(
    showDialog: Boolean,
    selectedBooking: Booking?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmCancel: () -> Unit,
) {
    if (showDialog && selectedBooking != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.cancel_booking_dialog_title)) },
            text = { Text(stringResource(R.string.cancel_booking_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = onConfirmCancel,
                    enabled = !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.calendar_cancel_button))
                    }
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
