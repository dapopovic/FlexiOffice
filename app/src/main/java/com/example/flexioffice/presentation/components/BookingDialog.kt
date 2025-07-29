package com.example.flexioffice.presentation.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun BookingDialog(
    showDialog: Boolean,
    selectedDate: LocalDate?,
    comment: String,
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onDateClick: () -> Unit,
    onCommentChange: (String) -> Unit,
    onCreateBooking: () -> Unit,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
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
                                    if (selectedDate != null) {
                                        MaterialTheme
                                            .colorScheme
                                            .primaryContainer
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceContainerHigh
                                    },
                            ),
                        onClick = onDateClick,
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
                                    if (selectedDate != null) {
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
                                        if (selectedDate != null) {
                                            "Ausgewähltes Datum"
                                        } else {
                                            "Datum auswählen"
                                        },
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,
                                    color =
                                        if (selectedDate != null) {
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
                                        selectedDate
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
                                        if (selectedDate != null) {
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
                                    if (selectedDate != null) {
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
                                value = comment,
                                onValueChange = onCommentChange,
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
                                Icons.Default.CheckCircle,
                                contentDescription = "Ausgewählt",
                                tint =
                                    MaterialTheme.colorScheme
                                        .onTertiaryContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Error Display with enhanced styling
                    if (error != null) {
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
                                    text = error,
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
                        onClick = onCreateBooking,
                        enabled = !isLoading && selectedDate != null,
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
                            if (isLoading) {
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
                                if (isLoading) {
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
                        onClick = onDismiss,
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
}
