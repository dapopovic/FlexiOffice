package com.example.flexioffice.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.CalendarEvent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        rememberCalendarState(
            startMonth = currentMonth.minusMonths(12),
            endMonth = currentMonth.plusMonths(12),
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = firstDayOfWeekFromLocale(),
        )

    Column(modifier = modifier) {
        // Month Header
        MonthHeader(
            currentMonth = currentMonth,
            onPreviousMonth = { onMonthChanged(currentMonth.minusMonths(1)) },
            onNextMonth = { onMonthChanged(currentMonth.plusMonths(1)) },
        )

        // Days of Week Header
        DaysOfWeekHeader()

        // Calendar Grid
        HorizontalCalendar(
            state = state,
            dayContent = { day ->
                val dayEvents = events.filter { it.date == day.date }
                CalendarDay(
                    day = day,
                    isSelected = selectedDate == day.date,
                    events = dayEvents,
                    onClick = { onDateSelected(day.date) },
                )
            },
            monthHeader = { /* Empty, we use custom header */ },
        )
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                ImageVector.vectorResource(R.drawable.chevron_left_24px_filled),
                contentDescription = "Vorheriger Monat",
            )
        }

        Text(
            text =
                currentMonth.month.getDisplayName(TextStyle.FULL, Locale.GERMAN) +
                    " ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                ImageVector.vectorResource(R.drawable.chevron_right_24px_filled),
                contentDescription = "Nächster Monat",
            )
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        daysOfWeek().forEach { dayOfWeek ->
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalendarDay(
    day: CalendarDay,
    isSelected: Boolean,
    events: List<CalendarEvent>,
    onClick: () -> Unit,
) {
    val isToday = day.date == LocalDate.now()
    val isCurrentMonth = day.position == DayPosition.MonthDate

    Box(
        modifier =
            Modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    },
                ).clickable(enabled = isCurrentMonth) { onClick() },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                fontSize = 14.sp,
                color =
                    when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                        !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )

            // Event indicators
            if (events.isNotEmpty() && isCurrentMonth) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(events.take(3)) { event ->
                        Box(
                            modifier =
                                Modifier
                                    .size(4.dp)
                                    .background(
                                        Color(event.color),
                                        CircleShape,
                                    ),
                        )
                    }
                    if (events.size > 3) {
                        item {
                            Text(
                                text = "+",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekCalendar(
    selectedDate: LocalDate?,
    events: List<CalendarEvent>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Diese Woche",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                weekDays.forEach { date ->
                    val dayEvents = events.filter { it.date == date }
                    val isSelected = selectedDate == date
                    val isToday = date == today

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable { onDateSelected(date) }
                                .padding(4.dp),
                    ) {
                        Text(
                            text =
                                date.dayOfWeek.getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.GERMAN,
                                ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(
                                        when {
                                            isSelected ->
                                                MaterialTheme.colorScheme
                                                    .primary
                                            isToday ->
                                                MaterialTheme.colorScheme
                                                    .primaryContainer
                                            else -> Color.Transparent
                                        },
                                        CircleShape,
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 14.sp,
                                color =
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isToday ->
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                fontWeight =
                                    if (isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                        }

                        // Event indicator
                        if (dayEvents.isNotEmpty()) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .background(
                                            Color(dayEvents.first().color),
                                            CircleShape,
                                        ),
                            )
                        }
                    }
                }
            }
        }
    }
}
