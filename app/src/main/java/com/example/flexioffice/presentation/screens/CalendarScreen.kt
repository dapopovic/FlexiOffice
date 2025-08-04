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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.flexioffice.R
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.labelRes
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import com.example.flexioffice.presentation.CalendarUiState
import com.example.flexioffice.presentation.CalendarViewModel
import com.example.flexioffice.presentation.components.BookingDialog
import com.example.flexioffice.presentation.components.BookingLegend
import com.example.flexioffice.presentation.components.CalendarFilters
import com.example.flexioffice.presentation.components.EventsList
import com.example.flexioffice.presentation.components.Filters
import com.example.flexioffice.presentation.components.MonthCalendar
import com.example.flexioffice.presentation.components.TeamHomeOfficeSummary
import com.example.flexioffice.presentation.components.WeekCalendar
import java.time.YearMonth

private const val TAG = "CalendarScreen"

@Composable
private fun CalendarViewWithLoading(
    uiState: CalendarUiState,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDateLongPress: (java.time.LocalDate) -> Unit,
    onDateDoubleClick: (java.time.LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
) {
    Box {
        // Calendar View
        if (uiState.isWeekView) {
            WeekCalendar(
                selectedDate = uiState.selectedDate,
                events = uiState.events,
                onDateSelected = onDateSelected,
            )
        } else {
            MonthCalendar(
                currentMonth = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                events = uiState.events,
                onDateSelected = onDateSelected,
                onDateLongPress = onDateLongPress,
                onDateDoubleClick = onDateDoubleClick,
                onMonthChanged = onMonthChanged,
            )
        }

        // Loading overlay for month data
        if (uiState.isLoadingMonthData) {
            Box(
                modifier = Modifier.fillMaxSize().height(300.dp),
                // Approximate calendar height
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = stringResource(R.string.calendar_loading_data),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    navigationController: NavHostController,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Shake-Erkennung registrieren
    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.registerShakeDetection(context)
        onDispose {
            viewModel.unregisterShakeDetection()
        }
    }

    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Log.e(TAG, "Error: $message")
            snackbarHostState.showSnackbar(message)
            viewModel.clearErrorMessage()
        }
    }

    // Booking Dialog
    if (uiState.showBookingDialog) {
        BookingDialog(
            showDialog = true,
            selectedDate = uiState.bookingDialogDate,
            comment = uiState.bookingComment,
            error = uiState.errorMessage,
            isLoading = uiState.isCreatingBooking,
            onDismiss = { viewModel.hideBookingDialog() },
            onDateClick = { /* Date is already selected */ },
            onCommentChange = { viewModel.updateBookingComment(it) },
            onCreateBooking = { viewModel.handleBookingCreation() },
        )
    }

    // Cancel-Dialog for Shake
    if (uiState.showCancelDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCancelDialog() },
            title = { Text(stringResource(R.string.calendar_cancel_booking_title)) },
            text = { Text(stringResource(R.string.calendar_cancel_booking_message)) },
            confirmButton = {
                Button(onClick = { viewModel.confirmCancelBooking() }) {
                    Text(stringResource(R.string.calendar_cancel_button))
                }
            },
            dismissButton = {
                Button(onClick = { viewModel.hideCancelDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            // Show full screen loading only for initial app loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header with title and view toggle
                CalendarHeader(
                    isWeekView = uiState.isWeekView,
                    onToggleView = viewModel::toggleViewMode,
                    onRefresh = { viewModel.loadBookingsForMonth(uiState.currentMonth) },
                    isLoadingDemoData = uiState.isLoadingDemoData,
                )

                // Filters for team members and status
                if (!uiState.currentUser?.teamId.isNullOrEmpty() &&
                    uiState.currentUser?.teamId != com.example.flexioffice.data.model.User.NO_TEAM
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Filters(
                            modifier = Modifier.weight(1f),
                            // items where the label is not "Cancelled"
                            items =
                                BookingStatus.entries
                                    .map {
                                        stringResource(it.labelRes())
                                    }.filter {
                                        it != stringResource(R.string.booking_item_status_cancelled)
                                    },
                            selectedItem = uiState.selectedStatus?.let { stringResource(it.labelRes()) },
                            onItemSelected = { item ->
                                val status = BookingStatus.entries.find { getString(context, it.labelRes()) == item }
                                viewModel.setStatusFilter(status)
                            },
                            onClearFilters = { viewModel.clearFilters() },
                            defaultItem = stringResource(R.string.filters_all_status),
                        )
                        Filters(
                            modifier = Modifier.weight(2f),
                            items = uiState.teamMembers.map { it.name },
                            selectedItem =
                                uiState.selectedTeamMember?.let { userId ->
                                    uiState.teamMembers.find { it.id == userId }?.name
                                },
                            onItemSelected = { name ->
                                viewModel.setTeamMemberFilter(
                                    uiState.teamMembers
                                        .find {
                                            it.name == name
                                        }?.id,
                                )
                            },
                            onClearFilters = { viewModel.clearFilters() },
                            defaultItem = stringResource(R.string.filters_all_members),
                        )
                    }
                }

                // Calendar View with loading indicator
                CalendarViewWithLoading(
                    uiState = uiState,
                    onDateSelected = viewModel::selectDate,
                    onDateLongPress = { date ->
                        // Show booking dialog on long press
                        viewModel.showBookingDialog(date)
                    },
                    onDateDoubleClick = { date ->
                        // Book a direct booking on double click
                        viewModel.createDirectBooking(date)
                    },
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

                // Team Summary
                if (uiState.events.isNotEmpty()) {
                    TeamHomeOfficeSummary(events = uiState.events)
                }

                // Events List
                EventsList(
                    selectedDate = uiState.selectedDate,
                    events = uiState.events,
                )

                // Legend (only show if no team available)
                if (!uiState.currentUser?.teamId.isNullOrEmpty() &&
                    uiState.currentUser?.teamId != com.example.flexioffice.data.model.User.NO_TEAM
                ) {
                    BookingLegend()
                }

                if (uiState.events.isEmpty()) {
                    // Show empty state if no events but has team
                    EmptyStateOrDemoButton(
                        hasTeam = !uiState.currentUser?.teamId.isNullOrEmpty(),
                        navigationController,
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
    isLoadingDemoData: Boolean = false,
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
                        R.drawable.calendar_month_24px_filled,
                    ), // Using existing icon as placeholder
                contentDescription = stringResource(R.string.calendar_icon_content_desc),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.headlineMedium,
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
                label = { Text(stringResource(R.string.calendar_month_view)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(
                            R.drawable.calendar_view_month_24px_filled,
                        ),
                        contentDescription = stringResource(R.string.calendar_month_view_content_desc),
                    )
                },
            )

            FilterChip(
                selected = isWeekView,
                onClick = { if (!isWeekView) onToggleView() },
                label = { Text(stringResource(R.string.calendar_week_view)) },
                leadingIcon = {
                    Icon(
                        ImageVector.vectorResource(
                            R.drawable.calendar_view_week_24px_filled,
                        ),
                        contentDescription = stringResource(R.string.calendar_week_view_content_desc),
                    )
                },
            )

            // Refresh Button with loading indicator
            IconButton(onClick = onRefresh, enabled = !isLoadingDemoData) {
                if (isLoadingDemoData) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.calendar_refresh_content_desc),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateOrDemoButton(
    hasTeam: Boolean,
    navigationController: NavHostController,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.calendar_empty_state_icon),
                style = MaterialTheme.typography.displayMedium,
            )

            if (!hasTeam) {
                Text(
                    text = stringResource(R.string.calendar_no_team_assigned),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.calendar_no_team_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.calendar_no_events_found),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.calendar_no_events_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        // Navigate to booking without affecting the navigation stack
                        navigationController.navigate(FlexiOfficeRoutes.Booking.route) {
                            popUpTo(navigationController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                ) { Text(stringResource(R.string.calendar_book_home_office)) }
            }
        }
    }
}
