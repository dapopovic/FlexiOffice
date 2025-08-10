package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.CalendarEvent
import com.example.flexioffice.data.model.EventType
import com.example.flexioffice.data.model.statusColor
import com.example.flexioffice.data.model.statusIcon
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
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
        ) {
            val headerText =
                if (selectedDate != null) {
                    val dayName =
                        selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
                    val formattedDate =
                        selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    "$dayName, $formattedDate"
                } else {
                    stringResource(R.string.EventComponents_ereignisse)
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
                            stringResource(R.string.EventComponents_keine_ereignisse_tag)
                        } else {
                            stringResource(R.string.EventComponents_Datum_wählen)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                selectedEvents.forEach {
                    EventItem(event = it)
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent) {
    val context = LocalContext.current
    val statusColorRes = event.status.statusColor()
    val statusTint = Color(ContextCompat.getColor(context, statusColorRes))
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Event status icon (consistent with legend and calendar)
        Icon(
            imageVector = event.status.statusIcon(),
            contentDescription = null,
            tint = statusTint,
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
                text = stringResource(R.string.EventComponents_team_home_office),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Today's home office
            HomeofficeSummaryItem(
                title = stringResource(R.string.EventComponents_heute),
                events = todayEvents,
            )

            // Tomorrow's home office
            HomeofficeSummaryItem(
                title = stringResource(R.string.EventComponents_morgen),
                events = tomorrowEvents,
            )

            if (todayEvents.isEmpty() && tomorrowEvents.isEmpty()) {
                Text(
                    text = stringResource(R.string.EventComponents_keine_home_office_zwei_tagen_geplant),
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
