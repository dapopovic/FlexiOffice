package com.example.flexioffice.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexioffice.data.model.CalendarEvent
import com.example.flexioffice.data.model.EventType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EventsList(
    selectedDate: LocalDate?,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier,
) {
    val selectedEvents =
        selectedDate?.let { date -> events.filter { it.date == date } } ?: emptyList()

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val headerText =
                if (selectedDate != null) {
                    val dayName =
                        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                    val formattedDate =
                        selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    "$dayName, $formattedDate"
                } else {
                    "Ereignisse"
                }

            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (selectedEvents.isEmpty()) {
                Text(
                    text =
                        if (selectedDate != null) {
                            "Keine Ereignisse an diesem Tag"
                        } else {
                            "Wählen Sie ein Datum aus, um Ereignisse anzuzeigen"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { items(selectedEvents) { event -> EventItem(event = event) } }
            }
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Event color indicator
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .background(
                        Color(event.color),
                        CircleShape,
                    ),
        )

        // Event icon
        Icon(
            imageVector =
                when (event.type) {
                    EventType.HOME_OFFICE -> Icons.Default.Home
                    EventType.TEAM_MEETING -> Icons.Default.Home
                    else -> Icons.Default.Home
                },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )

        // Event details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            if (event.participantNames.isNotEmpty()) {
                Text(
                    text = event.participantNames.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun TeamHomeOfficeSummary(
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val todayEvents = events.filter { it.date == today && it.type == EventType.HOME_OFFICE }
    val tomorrowEvents =
        events.filter { it.date == today.plusDays(1) && it.type == EventType.HOME_OFFICE }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Team Home Office Übersicht",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Today's home office
            HomeofficeSummaryItem(
                title = "Heute",
                events = todayEvents,
            )

            // Tomorrow's home office
            HomeofficeSummaryItem(
                title = "Morgen",
                events = tomorrowEvents,
            )

            if (todayEvents.isEmpty() && tomorrowEvents.isEmpty()) {
                Text(
                    text = "Keine Home Office Tage in den nächsten zwei Tagen geplant.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HomeofficeSummaryItem(
    title: String,
    events: List<CalendarEvent>,
) {
    if (events.isNotEmpty()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )

            events.forEach { event ->
                Text(
                    text = "• ${event.participantNames.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
