package com.example.flexioffice.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
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
import com.example.flexioffice.data.model.EventType
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
    onDateLongPress: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit,
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
                        onLongClick = { onDateLongPress(day.date) },
                        onDoubleClick = { onDateDoubleClick(day.date) },
                    )
                },
                monthHeader = { /* Empty, we use custom header */ },
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
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
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
                        isToday ->
                            MaterialTheme.colorScheme.primaryContainer
                        else -> Color.Transparent
                    },
                ).combinedClickable(
                    enabled = isCurrentMonth,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onDoubleClick = onDoubleClick,
                ),
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
                        isToday ->
                            MaterialTheme.colorScheme.onPrimaryContainer
                        !isCurrentMonth ->
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )

            // Event indicators
            if (events.isNotEmpty() && isCurrentMonth) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    events.take(3).forEach { event ->
                        Box(
                            modifier =
                                Modifier
                                    .size(4.dp)
                                    .background(
                                        event.color,
                                        CircleShape,
                                    ),
                        )
                    }
                    if (events.size > 3) {
                        Text(
                            text = "+",
                            fontSize = 8.sp,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
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
        selectedDate?.let { date ->
            val firstDayOfWeek = firstDayOfWeekFromLocale()
            val daysFromFirstDayOfWeek = (date.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            date.minusDays(daysFromFirstDayOfWeek.toLong())
        }
            ?: {
                val firstDayOfWeek = firstDayOfWeekFromLocale()
                val daysFromFirstDayOfWeek =
                    (today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
                today.minusDays(daysFromFirstDayOfWeek.toLong())
            }()
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

            // Day rows with team member bookings
            WeekTableHeader()
            WeekDayRows(
                weekDays = weekDays,
                events = events,
                selectedDate = selectedDate,
                today = today,
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
private fun WeekTableHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                ).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Day column header
        Text(
            text = "Tag",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(50.dp),
        )

        // Divider line
        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.3f,
                        ),
                    ),
        )

        // Bookings column header
        Text(
            text = "Home Office Anträge",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun WeekDayRows(
    weekDays: List<LocalDate>,
    events: List<CalendarEvent>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                ).padding(
                    bottom = 8.dp,
                ), // Add padding to ensure content isn't cut off
    ) {
        weekDays.forEachIndexed { index, date ->
            val dayEvents = events.filter { it.date == date }
            val isSelected = selectedDate == date
            val isToday = date == today
            WeekDayRow(
                date = date,
                events = dayEvents,
                isSelected = isSelected,
                isToday = isToday,
                onDateSelected = onDateSelected,
                isLastRow = index == weekDays.size - 1,
            )
        }
    }
}

private fun getRowBackgroundColor(
    isSelected: Boolean,
    isToday: Boolean,
    colorScheme: ColorScheme,
): Color =
    when {
        isSelected -> colorScheme.primary.copy(alpha = 0.7f)
        isToday -> colorScheme.secondary.copy(alpha = 0.5f)
        else -> colorScheme.surfaceVariant
    }

@Composable
private fun WeekDayRow(
    date: LocalDate,
    events: List<CalendarEvent>,
    isSelected: Boolean,
    isToday: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    isLastRow: Boolean = false,
) {
    val rememberScrollable = rememberScrollState()
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        getRowBackgroundColor(isSelected, isToday, MaterialTheme.colorScheme),
                    ).clickable { onDateSelected(date) }
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom =
                            if (isLastRow) {
                                16.dp
                            } else {
                                12.dp // Extra bottom padding for
                            },
                        // last row
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Day column (left side)
            Column(
                modifier = Modifier.width(50.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text =
                        date.dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.GERMAN,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color =
                        when {
                            isSelected ->
                                MaterialTheme.colorScheme.primary
                            isToday ->
                                MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = "${date.dayOfMonth}.${date.monthValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        when {
                            isSelected ->
                                MaterialTheme.colorScheme
                                    .onPrimaryContainer
                            isToday ->
                                MaterialTheme.colorScheme
                                    .onSecondaryContainer
                            else ->
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        },
                )
            }

            // Divider line
            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.3f,
                            ),
                        ),
            )

            // Events section (table cells)
            if (events.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                            .horizontalScroll(rememberScrollable)
                            .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    events.take(4).forEach { event ->
                        TableEventCell(
                            event = event,
                        )
                    }
                    if (events.size > 4) {
                        OverflowIndicator(
                            count = events.size - 4,
                            isSelected = isSelected,
                            isToday = isToday,
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = 0.5f),
                    )
                }
            }
        }

        // Row separator (except for last row)
        if (!isLastRow) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.2f,
                            ),
                        ),
            )
        }
    }
}

@Composable
private fun TableEventCell(event: CalendarEvent) {
    Card(
        modifier = Modifier.height(40.dp).wrapContentWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = event.color,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(6.dp),
    ) {
        Row(
            modifier =
                Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Event type icon
            Icon(
                imageVector =
                    when (event.type) {
                        EventType.HOME_OFFICE ->
                            ImageVector.vectorResource(
                                R.drawable.home_work_24px,
                            )
                        EventType.OFFICE_BOOKING ->
                            ImageVector.vectorResource(
                                R.drawable.work_24px,
                            )
                        EventType.TEAM_MEETING ->
                            ImageVector.vectorResource(
                                R.drawable.group_24px,
                            )
                        EventType.VACATION ->
                            ImageVector.vectorResource(
                                R.drawable.schedule_24px,
                            )
                    },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )

            // Event title
            Text(
                text = event.title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OverflowIndicator(
    count: Int,
    isSelected: Boolean,
    isToday: Boolean,
) {
    Card(
        modifier = Modifier.height(40.dp).width(48.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when {
                        isSelected ->
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.8f,
                            )
                        isToday ->
                            MaterialTheme.colorScheme.secondary.copy(
                                alpha = 0.8f,
                            )
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(6.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+$count",
                style = MaterialTheme.typography.labelSmall,
                color =
                    when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        isToday -> MaterialTheme.colorScheme.onSecondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
