package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.presentation.BookingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(viewModel: BookingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Stornierungsdialog
    if (uiState.showCancelDialog && uiState.selectedBooking != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCancelDialog() },
            title = { Text("Buchung stornieren") },
            text = { Text("Möchten Sie diese Buchung wirklich stornieren?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelBooking() },
                    enabled = !uiState.isLoading,
                ) {
                    if (uiState.isLoading) {
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
                TextButton(onClick = { viewModel.hideCancelDialog() }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showBookingDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Neuer Home Office Antrag",
                    modifier = Modifier.padding(12.dp),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Home Office Anträge",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Stornierte Anträge anzeigen",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Switch(
                            checked = uiState.showCancelledBookings,
                            onCheckedChange = { viewModel.toggleCancelledBookings() },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (uiState.userBookings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Keine Anträge vorhanden",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Erstellen Sie einen neuen Home Office Antrag mit dem + Button",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(
                    uiState.userBookings
                        .filter { booking ->
                            uiState.showCancelledBookings || booking.status != BookingStatus.CANCELLED
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

    if (uiState.showBookingDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideBookingDialog() },
            title = {
                Text(
                    "Neue Home Office Buchung",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.showDatePicker()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            uiState.selectedDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                ?: "Datum auswählen",
                        )
                    }

                    OutlinedTextField(
                        value = uiState.comment,
                        onValueChange = { viewModel.updateComment(it) },
                        label = { Text("Kommentar") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createBooking() },
                    enabled = !uiState.isLoading && uiState.selectedDate != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(
                        "Home Office buchen",
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideBookingDialog() }) {
                    Text("Abbrechen")
                }
            },
        )
    }

    if (uiState.showDetailsSheet) {
        uiState.selectedBooking?.let { booking ->
            val bottomSheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideDetailsSheet() },
                sheetState = bottomSheetState,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text =
                                booking.date.format(
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG),
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    // Status
                    Column {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text =
                                when (booking.status) {
                                    BookingStatus.APPROVED -> "✓ Genehmigt"
                                    BookingStatus.PENDING -> "⏳ Ausstehend"
                                    BookingStatus.DECLINED -> "✗ Abgelehnt"
                                    BookingStatus.CANCELLED -> "⊘ Storniert"
                                },
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                when (booking.status) {
                                    BookingStatus.APPROVED -> MaterialTheme.colorScheme.primary
                                    BookingStatus.PENDING -> MaterialTheme.colorScheme.secondary
                                    BookingStatus.DECLINED -> MaterialTheme.colorScheme.error
                                    BookingStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }

                    // Antragsteller
                    Column {
                        Text(
                            text = "Antragsteller",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = uiState.approverName ?: "Wird geladen...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    // Kommentar (wenn vorhanden)
                    if (booking.comment.isNotEmpty()) {
                        Column {
                            Text(
                                text = "Kommentar",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = booking.comment,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Material 3 DatePicker Dialog
    if (uiState.showDatePicker) {
        val today = LocalDate.now()
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    uiState.selectedDate?.toEpochDay()?.let { it * 24 * 60 * 60 * 1000 }
                        ?: (today.toEpochDay() * 24 * 60 * 60 * 1000),
                yearRange = today.year..(today.year + 1),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val date = LocalDate.ofEpochDay(utcTimeMillis / (24 * 60 * 60 * 1000))
                            return !date.isBefore(today)
                        }
                    },
            )

        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            viewModel.updateSelectedDate(selectedDate)
                        }
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Abbrechen")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingItem(
    booking: Booking,
    onClick: (Booking) -> Unit = {},
    onCancelClick: (Booking) -> Unit = {},
) {
    val isStorniert = booking.status == BookingStatus.CANCELLED
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(booking) },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (booking.status == BookingStatus.CANCELLED) {
                        MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text =
                        booking.date.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(java.util.Locale.GERMAN),
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isStorniert) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                if (booking.comment.isNotBlank()) {
                    Text(
                        text = booking.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (isStorniert) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text =
                        when (booking.status) {
                            BookingStatus.APPROVED -> "✓ Genehmigt"
                            BookingStatus.PENDING -> "⏳ Ausstehend"
                            BookingStatus.DECLINED -> "✗ Abgelehnt"
                            BookingStatus.CANCELLED -> "⊘ Storniert"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        when (booking.status) {
                            BookingStatus.APPROVED -> MaterialTheme.colorScheme.primary
                            BookingStatus.PENDING -> MaterialTheme.colorScheme.secondary
                            BookingStatus.DECLINED -> MaterialTheme.colorScheme.error
                            BookingStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )

                if (!isStorniert) {
                    IconButton(
                        onClick = { onCancelClick(booking) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Buchung stornieren",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
