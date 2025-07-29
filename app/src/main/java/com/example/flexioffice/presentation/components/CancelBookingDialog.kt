package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            title = { Text("Buchung stornieren") },
            text = { Text("Möchten Sie diese Buchung wirklich stornieren?") },
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
                        Text("Stornieren")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Abbrechen")
                }
            },
        )
    }
}
