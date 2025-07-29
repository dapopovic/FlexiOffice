package com.example.flexioffice.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextOverflow
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
import java.time.LocalTime
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

    Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
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
                    monthHeader = { /* Empty, we use custom header */},
            )
        }
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
                    Modifier.aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    },
                            )
                            .clickable(enabled = isCurrentMonth) { onClick() },
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
                                        Modifier.size(4.dp)
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
    val startOfWeek =
            selectedDate?.let { date -> date.minusDays(date.dayOfWeek.value.toLong() - 1) }
                    ?: today.minusDays(today.dayOfWeek.value.toLong() - 1)
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Week navigation header
            WeekHeader(
                    startOfWeek = startOfWeek,
                    onPreviousWeek = { onDateSelected(startOfWeek.minusWeeks(1)) },
                    onNextWeek = { onDateSelected(startOfWeek.plusWeeks(1)) },
            )

            // Days header row
            WeekDaysHeader(
                    weekDays = weekDays,
                    selectedDate = selectedDate,
                    today = today,
                    onDateSelected = onDateSelected,
            )

            // Time grid with events
            WeekTimeGrid(
                    weekDays = weekDays,
                    events = events,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
            )
        }
    }
}

@Composable
private fun WeekHeader(
        startOfWeek: LocalDate,
        onPreviousWeek: () -> Unit,
        onNextWeek: () -> Unit,
) {
    val endOfWeek = startOfWeek.plusDays(6)
    val monthYear =
            if (startOfWeek.month == endOfWeek.month) {
                "${startOfWeek.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)} ${startOfWeek.year}"
            } else {
                "${startOfWeek.month.getDisplayName(
                TextStyle.SHORT,
                Locale.GERMAN,
            )} - ${endOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.GERMAN)} ${endOfWeek.year}"
            }

    Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousWeek) {
            Icon(
                    ImageVector.vectorResource(R.drawable.chevron_left_24px_filled),
                    contentDescription = "Vorherige Woche",
            )
        }

        Text(
                text = monthYear,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
        )

        IconButton(onClick = onNextWeek) {
            Icon(
                    ImageVector.vectorResource(R.drawable.chevron_right_24px_filled),
                    contentDescription = "Nächste Woche",
            )
        }
    }
}

@Composable
private fun WeekDaysHeader(
        weekDays: List<LocalDate>,
        selectedDate: LocalDate?,
        today: LocalDate,
        onDateSelected: (LocalDate) -> Unit,
) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        // Time column placeholder
        Box(
                modifier = Modifier.width(50.dp),
        )

        // Day columns
        weekDays.forEach { date ->
            val isSelected = selectedDate == date
            val isToday = date == today

            Column(
                    modifier = Modifier.weight(1f).clickable { onDateSelected(date) }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                )

                Box(
                        modifier =
                                Modifier.size(32.dp)
                                        .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
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
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekTimeGrid(
        weekDays: List<LocalDate>,
        events: List<CalendarEvent>,
        selectedDate: LocalDate?,
        onDateSelected: (LocalDate) -> Unit,
) {
    val timeSlots = (8..18).map { hour -> String.format(Locale.GERMAN, "%02d:00", hour) }
    val currentTime = LocalTime.now()
    val today = LocalDate.now()

    LazyColumn(
            modifier = Modifier.heightIn(max = 400.dp),
    ) {
        items(timeSlots) { timeSlot: String ->
            val hour = timeSlot.substring(0, 2).toInt()
            val isCurrentHour = today in weekDays && currentTime.hour == hour

            Row(
                    modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.Top,
            ) {
                // Time label
                Box(
                        modifier = Modifier.width(50.dp).padding(top = 4.dp),
                ) {
                    Text(
                            text = timeSlot,
                            fontSize = 12.sp,
                            color =
                                    if (isCurrentHour) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            fontWeight = if (isCurrentHour) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                // Day columns
                weekDays.forEachIndexed { _, date ->
                    val dayEvents = events.filter { it.date == date }
                    val isToday = date == today
                    val showCurrentTimeIndicator = isToday && isCurrentHour

                    Box(
                            modifier =
                                    Modifier.weight(1f)
                                            .fillMaxHeight()
                                            .background(
                                                    when {
                                                        date == selectedDate ->
                                                                MaterialTheme.colorScheme
                                                                        .primaryContainer.copy(
                                                                        alpha = 0.1f,
                                                                )
                                                        isToday -> MaterialTheme.colorScheme.surface
                                                        else -> Color.Transparent
                                                    },
                                            )
                                            .clickable { onDateSelected(date) },
                    ) {
                        // Vertical divider
                        Box(
                                modifier =
                                        Modifier.width(1.dp)
                                                .fillMaxHeight()
                                                .background(
                                                        MaterialTheme.colorScheme.outline.copy(
                                                                alpha = 0.3f
                                                        )
                                                )
                                                .align(Alignment.CenterEnd),
                        )

                        // Horizontal divider
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(1.dp)
                                                .background(
                                                        if (isCurrentHour && isToday) {
                                                            MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.5f
                                                            )
                                                        } else {
                                                            MaterialTheme.colorScheme.outline.copy(
                                                                    alpha = 0.2f
                                                            )
                                                        },
                                                )
                                                .align(Alignment.BottomCenter),
                        )

                        // Current time indicator
                        if (showCurrentTimeIndicator) {
                            val minuteProgress = currentTime.minute / 60f
                            Box(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(top = (60.dp * minuteProgress)),
                            )
                        }

                        // Events for this day (show in appropriate time slot)
                        if (timeSlot == "09:00" && dayEvents.isNotEmpty()) {
                            LazyColumn(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(end = 3.dp, start = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(dayEvents.take(2)) { event: CalendarEvent ->
                                    WeekEventItem(event = event)
                                }
                                if (dayEvents.size > 2) {
                                    item {
                                        Text(
                                                text = "+${dayEvents.size - 2} mehr",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekEventItem(event: CalendarEvent) {
    Card(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            colors =
                    CardDefaults.cardColors(
                            containerColor = Color(event.color).copy(alpha = 0.9f),
                    ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(4.dp),
    ) {
        Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            // Event type indicator dot
            Box(
                    modifier =
                            Modifier.size(8.dp)
                                    .background(
                                            Color.White.copy(alpha = 0.8f),
                                            CircleShape,
                                    ),
            )

            Text(
                    text = event.title,
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
        }
    }
}
