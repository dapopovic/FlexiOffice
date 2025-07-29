package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsSheet(
    showSheet: Boolean,
    booking: Booking?,
    approverName: String?,
    onDismiss: () -> Unit,
) {
    if (showSheet && booking != null) {
        val bottomSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Home Office Details",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // Datum
                Column {
                    Text(
                        text = "Datum",
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                    )
                    Text(
                        text =
                            booking.date.format(
                                DateTimeFormatter
                                    .ofLocalizedDate(
                                        FormatStyle
                                            .LONG,
                                    ),
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                // Status
                Column {
                    Text(
                        text = "Status",
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                    )
                    Text(
                        text =
                            when (booking.status) {
                                BookingStatus.APPROVED ->
                                    "✓ Genehmigt"
                                BookingStatus.PENDING ->
                                    "⏳ Ausstehend"
                                BookingStatus.DECLINED ->
                                    "✗ Abgelehnt"
                                BookingStatus.CANCELLED ->
                                    "⊘ Storniert"
                            },
                        style = MaterialTheme.typography.bodyLarge,
                        color =
                            when (booking.status) {
                                BookingStatus.APPROVED ->
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                                BookingStatus.PENDING ->
                                    MaterialTheme
                                        .colorScheme
                                        .secondary
                                BookingStatus.DECLINED ->
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                BookingStatus.CANCELLED ->
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            },
                    )
                }

                // Antragsteller
                Column {
                    Text(
                        text = "Antragsteller",
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                    )
                    Text(
                        text = booking.userName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                // Approver
                Column {
                    Text(
                        text = "Genehmigt durch",
                        style =
                            MaterialTheme.typography
                                .labelMedium,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                    )
                    Text(
                        text = approverName ?: "Wird geladen...",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                // Kommentar (wenn vorhanden)
                if (booking.comment.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Kommentar",
                            style =
                                MaterialTheme.typography
                                    .labelMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
                        Text(
                            text = booking.comment,
                            style =
                                MaterialTheme.typography
                                    .bodyLarge,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
