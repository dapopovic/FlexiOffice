package com.example.flexioffice.presentation.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

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
                Text(
                    text = "Home Office Anträge",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
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
                items(uiState.userBookings.sortedByDescending { it.date }) { booking ->
                    BookingItem(booking = booking)
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
                            val today = LocalDate.now()
                            DatePickerDialog(
                                context,
                                { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                    viewModel.updateSelectedDate(
                                        LocalDate.of(year, month + 1, dayOfMonth),
                                    )
                                },
                                today.year,
                                today.monthValue - 1,
                                today.dayOfMonth,
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            uiState.selectedDate?.toString() ?: "Datum auswählen",
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

    // Entfernt doppelte Layouts
}

@Composable
private fun BookingItem(booking: Booking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = booking.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        when (booking.status) {
                            BookingStatus.APPROVED -> "✓ Genehmigt"
                            BookingStatus.PENDING -> "⏳ Ausstehend"
                            BookingStatus.DECLINED -> "✗ Abgelehnt"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        when (booking.status) {
                            BookingStatus.APPROVED -> MaterialTheme.colorScheme.primary
                            BookingStatus.PENDING -> MaterialTheme.colorScheme.secondary
                            BookingStatus.DECLINED -> MaterialTheme.colorScheme.error
                        },
                )
            }
            if (booking.comment.isNotEmpty()) {
                Text(
                    text = booking.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
