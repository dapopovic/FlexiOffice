package com.example.flexioffice.presentation.screens

import android.graphics.drawable.Icon
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.presentation.BookingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Log.e("CalendarScreen", "Error: $message")
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showBookingDialog() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Neuer Home Office Antrag",
                    modifier = Modifier.padding(12.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Antrag erstellen",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable.schedule_24px,
                                ),
                            contentDescription = "Anträge Icon",
                            modifier = Modifier.padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Home Office Anträge",
                            style =
                                MaterialTheme.typography
                                    .headlineMedium,
                        )
                    }
                    Text(
                        text =
                            "Hier können Sie Ihre Home Office Anträge verwalten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Stornierte Anträge anzeigen",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
                        Switch(
                            checked = uiState.showCancelledBookings,
                            onCheckedChange = {
                                viewModel.toggleCancelledBookings()
                            },
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
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "Keine Anträge vorhanden",
                                style =
                                    MaterialTheme.typography
                                        .bodyLarge,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text =
                                    "Erstellen Sie einen neuen Home Office Antrag mit dem + Button",
                                style =
                                    MaterialTheme.typography
                                        .bodyMedium,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                items(
                    uiState.userBookings
                        .filter { booking ->
                            uiState.showCancelledBookings ||
                                booking.status !=
                                BookingStatus.CANCELLED
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .primaryContainer,
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable.home_work_24px,
                                ),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                            tint =
                                MaterialTheme.colorScheme
                                    .onPrimaryContainer,
                        )
                    }
                    Column {
                        Text(
                            "Home Office Antrag",
                            style =
                                MaterialTheme.typography
                                    .headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Erstellen Sie einen neuen Antrag",
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    // Date Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    if (uiState.selectedDate !=
                                        null
                                    ) {
                                        MaterialTheme
                                            .colorScheme
                                            .primaryContainer
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceContainerHigh
                                    },
                            ),
                        onClick = { viewModel.showDatePicker() },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .calendar_today_24px,
                                    ),
                                contentDescription =
                                    "Datum auswählen",
                                tint =
                                    if (uiState.selectedDate !=
                                        null
                                    ) {
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimaryContainer
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                    },
                                modifier = Modifier.size(24.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text =
                                        if (uiState.selectedDate !=
                                            null
                                        ) {
                                            "Ausgewähltes Datum"
                                        } else {
                                            "Datum auswählen"
                                        },
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,
                                    color =
                                        if (uiState.selectedDate !=
                                            null
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .onPrimaryContainer
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        },
                                )
                                Text(
                                    text =
                                        uiState.selectedDate
                                            ?.format(
                                                DateTimeFormatter
                                                    .ofLocalizedDate(
                                                        FormatStyle
                                                            .FULL,
                                                    ),
                                            )
                                            ?: "Tippen Sie hier, um ein Datum zu wählen",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge,
                                    color =
                                        if (uiState.selectedDate !=
                                            null
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .onPrimaryContainer
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        },
                                )
                            }
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .chevron_right_24px_filled,
                                    ),
                                contentDescription = null,
                                tint =
                                    if (uiState.selectedDate !=
                                        null
                                    ) {
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimaryContainer
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                    },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Comment Field with enhanced styling
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,
                            ),
                    ) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    painter =
                                        painterResource(
                                            R.drawable
                                                .edit_note_24px,
                                        ),
                                    contentDescription =
                                        "Kommentar",
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    modifier =
                                        Modifier.size(
                                            20.dp,
                                        ),
                                )
                                Text(
                                    "Kommentar (Optional)",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = uiState.comment,
                                onValueChange = {
                                    viewModel.updateComment(it)
                                },
                                placeholder = {
                                    Text(
                                        "Fügen Sie einen Kommentar zu Ihrem Antrag hinzu...",
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                shape = MaterialTheme.shapes.medium,
                                leadingIcon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                R.drawable
                                                    .comment_24px,
                                            ),
                                        contentDescription =
                                        null,
                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }

                    // Work Type Selection (new feature)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .tertiaryContainer,
                            ),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .workspace_premium_24px,
                                    ),
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme
                                        .onTertiaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(
                                    "Home Office",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onTertiaryContainer,
                                )
                                Text(
                                    "Vollzeit von zu Hause arbeiten",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onTertiaryContainer
                                            .copy(
                                                alpha =
                                                0.8f,
                                            ),
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                painter =
                                    painterResource(
                                        R.drawable
                                            .check_circle_24px,
                                    ),
                                contentDescription = "Ausgewählt",
                                tint =
                                    MaterialTheme.colorScheme
                                        .onTertiaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Error Display with enhanced styling
                    if (uiState.error != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    painter =
                                        painterResource(
                                            R.drawable
                                                .error_24px,
                                        ),
                                    contentDescription =
                                        "Fehler",
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onErrorContainer,
                                    modifier =
                                        Modifier.size(
                                            20.dp,
                                        ),
                                )
                                Text(
                                    text = uiState.error!!,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onErrorContainer,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.createBooking() },
                        enabled =
                            !uiState.isLoading &&
                                uiState.selectedDate != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .primary,
                                contentColor =
                                    MaterialTheme.colorScheme
                                        .onPrimary,
                            ),
                    ) {
                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier.padding(vertical = 8.dp),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(
                                            20.dp,
                                        ),
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    painter =
                                        painterResource(
                                            R.drawable
                                                .send_24px,
                                        ),
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(
                                            20.dp,
                                        ),
                                )
                            }
                            Text(
                                if (uiState.isLoading) {
                                    "Antrag wird erstellt..."
                                } else {
                                    "Antrag einreichen"
                                },
                                style =
                                    MaterialTheme.typography
                                        .titleMedium,
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.hideBookingDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    R.drawable.close_24px,
                                ),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Abbrechen")
                    }
                }
            },
            dismissButton = null,
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
                            text =
                                uiState.approverName
                                    ?: "Wird geladen...",
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

    // Material 3 DatePicker Dialog
    if (uiState.showDatePicker) {
        val today = LocalDate.now()
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    uiState.selectedDate?.toEpochDay()?.let {
                        it * 24 * 60 * 60 * 1000
                    }
                        ?: (today.toEpochDay() * 24 * 60 * 60 * 1000),
                yearRange = today.year..(today.year + 1),
                selectableDates =
                    object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            val date =
                                LocalDate.ofEpochDay(
                                    utcTimeMillis /
                                        (
                                            24 *
                                                60 *
                                                60 *
                                                1000
                                        ),
                                )
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
                            val selectedDate =
                                LocalDate.ofEpochDay(
                                    millis /
                                        (
                                            24 *
                                                60 *
                                                60 *
                                                1000
                                        ),
                                )
                            viewModel.updateSelectedDate(selectedDate)
                        }
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Abbrechen")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
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
    val dateTimeFormatter =
        remember {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
        }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = { onClick(booking) },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (booking.status == BookingStatus.CANCELLED) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceBright
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (isStorniert) 1.dp else 4.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status Icon
            Icon(
                painter =
                    painterResource(
                        when (booking.status) {
                            BookingStatus.APPROVED ->
                                R.drawable.assignment_24px_filled
                            BookingStatus.PENDING ->
                                R.drawable.schedule_24px
                            BookingStatus.DECLINED ->
                                R.drawable.cancel_24px
                            BookingStatus.CANCELLED ->
                                R.drawable.cancel_24px
                        },
                    ),
                contentDescription = "Status",
                tint =
                    when (booking.status) {
                        BookingStatus.APPROVED ->
                            MaterialTheme.colorScheme.primary
                        BookingStatus.PENDING ->
                            MaterialTheme.colorScheme.tertiary
                        BookingStatus.DECLINED ->
                            MaterialTheme.colorScheme.error
                        BookingStatus.CANCELLED ->
                            MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Date
                Text(
                    text = booking.date.format(dateTimeFormatter),
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (isStorniert) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    when (booking.status) {
                                        BookingStatus
                                            .APPROVED,
                                        ->
                                            MaterialTheme
                                                .colorScheme
                                                .primaryContainer
                                        BookingStatus
                                            .PENDING,
                                        ->
                                            MaterialTheme
                                                .colorScheme
                                                .tertiaryContainer
                                        BookingStatus
                                            .DECLINED,
                                        ->
                                            MaterialTheme
                                                .colorScheme
                                                .errorContainer
                                        BookingStatus
                                            .CANCELLED,
                                        ->
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                    },
                            ),
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Text(
                            text =
                                when (booking.status) {
                                    BookingStatus.APPROVED ->
                                        "Genehmigt"
                                    BookingStatus.PENDING ->
                                        "Ausstehend"
                                    BookingStatus.DECLINED ->
                                        "Abgelehnt"
                                    BookingStatus.CANCELLED ->
                                        "Storniert"
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                when (booking.status) {
                                    BookingStatus.APPROVED ->
                                        MaterialTheme
                                            .colorScheme
                                            .onPrimaryContainer
                                    BookingStatus.PENDING ->
                                        MaterialTheme
                                            .colorScheme
                                            .onTertiaryContainer
                                    BookingStatus.DECLINED ->
                                        MaterialTheme
                                            .colorScheme
                                            .onErrorContainer
                                    BookingStatus.CANCELLED ->
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                },
                            modifier =
                                Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp,
                                ),
                        )
                    }
                }

                // Comment if present
                if (booking.comment.isNotBlank()) {
                    Text(
                        text = booking.comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            // Action Button
            if (!isStorniert) {
                IconButton(
                    onClick = { onCancelClick(booking) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.cancel_24px),
                        contentDescription = "Buchung stornieren",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
