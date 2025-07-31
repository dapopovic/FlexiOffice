package com.example.flexioffice.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalWearMaterialApi::class)
@Composable
fun BookingItem(
    booking: Booking,
    onClick: (Booking) -> Unit = {},
    onCancelClick: (Booking) -> Unit = {},
    onLongClick: () -> Unit = {},
    isMultiSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChanged: (Boolean) -> Unit = {},
) {
    val isStorniert = booking.status == BookingStatus.CANCELLED
    val dateTimeFormatter =
        remember {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMAN)
        }

    // Swipe-Logik: 0 = normal, 1 = links geswiped (cancel), 2 = rechts geswiped (details)
    val swipeableState = rememberSwipeableState(0)
    val swipeThreshold = 300f
    val anchors =
        mapOf(
            0f to 0, // Normal position
            -swipeThreshold to 1, // Left swipe - Cancel
            swipeThreshold to 2, // Right swipe - Details
        )

    // Reagiere auf Swipe-Aktionen
    LaunchedEffect(swipeableState.currentValue) {
        when (swipeableState.currentValue) {
            1 -> { // Links geswiped - Cancel Dialog
                if (!isStorniert) {
                    onCancelClick(booking)
                }
                swipeableState.animateTo(0) // Zurück zur normalen Position
            }
            2 -> { // Rechts geswiped - Details
                onClick(booking)
                swipeableState.animateTo(0) // Zurück zur normalen Position
            }
        }
    }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
                    orientation = Orientation.Horizontal,
                ).offset { IntOffset(swipeableState.offset.value.toInt(), 0) }
                .combinedClickable(
                    onClick = {
                        if (isMultiSelectMode && !isStorniert) {
                            onSelectionChanged(!isSelected)
                        } else if (!isMultiSelectMode) {
                            onClick(booking)
                        }
                    },
                    onLongClick = {
                        if (!isStorniert) {
                            onLongClick()
                        }
                    },
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (booking.status == BookingStatus.CANCELLED) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
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
            // Multi-select Checkbox
            if (isMultiSelectMode && !isStorniert) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChanged,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

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
