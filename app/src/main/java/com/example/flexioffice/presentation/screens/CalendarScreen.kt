package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.data.model.User
import com.example.flexioffice.presentation.CalendarViewModel
import com.example.flexioffice.presentation.components.EventsList
import com.example.flexioffice.presentation.components.MonthCalendar
import com.example.flexioffice.presentation.components.TeamHomeOfficeSummary
import com.example.flexioffice.presentation.components.WeekCalendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Log.e("CalendarScreen", "Error: $message")
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header with title and view toggle
                CalendarHeader(
                        isWeekView = uiState.isWeekView,
                        onToggleView = viewModel::toggleViewMode,
                        onRefresh = viewModel::loadDemoData,
                )

                // Calendar View
                if (uiState.isWeekView) {
                    WeekCalendar(
                            selectedDate = uiState.selectedDate,
                            events = uiState.events,
                            onDateSelected = viewModel::selectDate,
                    )
                } else {
                    MonthCalendar(
                            currentMonth = uiState.currentMonth,
                            selectedDate = uiState.selectedDate,
                            events = uiState.events,
                            onDateSelected = viewModel::selectDate,
                            onMonthChanged = { month ->
                                if (month != uiState.currentMonth) {
                                    if (month.isAfter(uiState.currentMonth)) {
                                        viewModel.nextMonth()
                                    } else {
                                        viewModel.previousMonth()
                                    }
                                }
                            },
                    )
                }

                // Team Summary
                if (uiState.events.isNotEmpty()) {
                    TeamHomeOfficeSummary(events = uiState.events)
                }

                // Events List
                EventsList(
                        selectedDate = uiState.selectedDate,
                        events = uiState.events,
                )

                // Empty State or Demo Data Button
                if (uiState.events.isEmpty() && !uiState.isLoading) {
                    EmptyStateOrDemoButton(
                            hasTeam =
                                    !uiState.currentUser?.teamId.isNullOrEmpty() &&
                                            uiState.currentUser?.teamId != User.NO_TEAM,
                            onLoadDemo = viewModel::loadDemoData,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
        isWeekView: Boolean,
        onToggleView: () -> Unit,
        onRefresh: () -> Unit,
) {
    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                    imageVector =
                            ImageVector.vectorResource(
                                    R.drawable.calendar_month_24px_filled
                            ), // Using existing icon as placeholder
                    contentDescription = "Kalender Icon",
                    tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                    text = "Kalender",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
            )
        }

        Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            // View Mode Toggle
            FilterChip(
                    selected = !isWeekView,
                    onClick = { if (isWeekView) onToggleView() },
                    label = { Text("Monat") },
                    leadingIcon = {
                        Icon(
                                ImageVector.vectorResource(
                                        R.drawable.calendar_view_month_24px_filled
                                ),
                                contentDescription = "Monatsansicht",
                        )
                    },
            )

            FilterChip(
                    selected = isWeekView,
                    onClick = { if (!isWeekView) onToggleView() },
                    label = { Text("Woche") },
                    leadingIcon = {
                        Icon(
                                ImageVector.vectorResource(
                                        R.drawable.calendar_view_week_24px_filled
                                ),
                                contentDescription = "Wochenansicht",
                        )
                    },
            )

            // Refresh Button
            IconButton(onClick = onRefresh) {
                Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Aktualisieren",
                )
            }
        }
    }
}

@Composable
private fun EmptyStateOrDemoButton(
        hasTeam: Boolean,
        onLoadDemo: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                    text = "📅",
                    style = MaterialTheme.typography.displayMedium,
            )

            if (!hasTeam) {
                Text(
                        text = "Kein Team zugewiesen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                )
                Text(
                        text =
                                "Sie müssen einem Team beitreten, um Team-Home-Office-Tage zu sehen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                )
            } else {
                Text(
                        text = "Keine Events gefunden",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                )
                Text(
                        text = "Es sind noch keine Home-Office-Tage für Ihr Team geplant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onLoadDemo) { Text("Demo-Daten laden") }

                OutlinedButton(onClick = { /* TODO: Navigate to booking */}) {
                    Text("Home-Office buchen")
                }
            }
        }
    }
}
