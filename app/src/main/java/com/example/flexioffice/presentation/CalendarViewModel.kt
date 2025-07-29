package com.example.flexioffice.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.BookingRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.CalendarEvent
import com.example.flexioffice.data.model.EventType
import com.example.flexioffice.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val isLoadingMonthData: Boolean = false,
    val isLoadingDemoData: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val isWeekView: Boolean = false,
    val events: List<CalendarEvent> = emptyList(),
    val bookings: List<Booking> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val bookingRepository: BookingRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(CalendarUiState())
        val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                authRepository
                    .currentUser
                    .map { it?.uid }
                    .distinctUntilChanged()
                    .flatMapLatest { userId ->
                        if (userId == null) {
                            flowOf(CalendarUiState(isLoading = false, currentUser = null))
                        } else {
                            userRepository.getUserStream(userId).flatMapLatest { userResult ->
                                val user = userResult.getOrNull()
                                if (user?.teamId.isNullOrEmpty() || user.teamId == User.NO_TEAM) {
                                    // User has no team, show empty calendar
                                    flowOf(
                                        CalendarUiState(
                                            isLoading = false,
                                            currentUser = user,
                                            currentMonth = YearMonth.now(),
                                        ),
                                    )
                                } else {
                                    // User has a team, load team bookings
                                    val currentMonth = YearMonth.now()
                                    bookingRepository
                                        .getTeamBookingsStream(
                                            user.teamId,
                                            currentMonth.year,
                                            currentMonth.monthValue,
                                        ).map { bookingsResult ->
                                            val bookings =
                                                (bookingsResult.getOrNull() ?: emptyList()).filter {
                                                    it.status != BookingStatus.CANCELLED
                                                }
                                            Log.d("CalendarViewModel", "Loaded bookings: $bookings")
                                            CalendarUiState(
                                                isLoading = false,
                                                currentUser = user,
                                                currentMonth = currentMonth,
                                                bookings = bookings,
                                                events = mapBookingsToEvents(bookings),
                                                errorMessage =
                                                    bookingsResult
                                                        .exceptionOrNull()
                                                        ?.message,
                                            )
                                        }
                                }
                            }
                        }
                    }.catch { e ->
                        _uiState.value =
                            _uiState.value.copy(errorMessage = e.message, isLoading = false)
                    }.collect { state -> _uiState.value = state }
            }
        }

        /** Wechselt zwischen Monats- und Wochenansicht */
        fun toggleViewMode() {
            _uiState.value = _uiState.value.copy(isWeekView = !_uiState.value.isWeekView)
        }

        /** Wählt ein Datum aus */
        fun selectDate(date: LocalDate) {
            _uiState.value = _uiState.value.copy(selectedDate = date)
        }

        /** Navigiert zum vorherigen Monat */
        fun previousMonth() {
            val currentMonth = _uiState.value.currentMonth
            val newMonth = currentMonth.minusMonths(1)
            _uiState.value = _uiState.value.copy(currentMonth = newMonth)
            loadBookingsForMonth(newMonth)
        }

        /** Navigiert zum nächsten Monat */
        fun nextMonth() {
            val currentMonth = _uiState.value.currentMonth
            val newMonth = currentMonth.plusMonths(1)
            _uiState.value = _uiState.value.copy(currentMonth = newMonth)
            loadBookingsForMonth(newMonth)
        }

        /** Lädt Buchungen für einen bestimmten Monat */
        fun loadBookingsForMonth(month: YearMonth) {
            val teamId = _uiState.value.currentUser?.teamId
            if (teamId.isNullOrEmpty() || teamId == User.NO_TEAM) return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingMonthData = true)

                bookingRepository.getTeamBookingsStream(teamId, month.year, month.monthValue).collect { result ->
                    val bookings =
                        (result.getOrNull() ?: emptyList()).filter {
                            it.status != BookingStatus.CANCELLED
                        }
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingMonthData = false,
                            bookings = bookings,
                            events = mapBookingsToEvents(bookings),
                            errorMessage = result.exceptionOrNull()?.message,
                        )
                }
            }
        }

        /** Konvertiert Buchungen zu Kalender-Events */
        private fun mapBookingsToEvents(bookings: List<Booking>): List<CalendarEvent> =
            bookings
                .map { booking ->
                    val date = booking.date
                    CalendarEvent(
                        id = booking.id,
                        title = booking.userName,
                        date = date,
                        type = EventType.HOME_OFFICE,
                        participantNames = listOf(booking.userName),
                        color = 0xFF4CAF50, // Green for home office
                    )
                }

        /** Lädt Demo-Daten für Testing */
        fun loadDemoData() {
            val teamId = _uiState.value.currentUser?.teamId
            if (teamId.isNullOrEmpty() || teamId == User.NO_TEAM) return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingDemoData = true)
                bookingRepository
                    .createDemoBookings(teamId)
                    .onSuccess {
                        // Demo data created, clear demo loading and reload current month
                        _uiState.value = _uiState.value.copy(isLoadingDemoData = false)
                        loadBookingsForMonth(_uiState.value.currentMonth)
                    }.onFailure { e: Throwable ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoadingDemoData = false,
                                errorMessage =
                                    "Fehler beim Laden der Demo-Daten: ${e.message}",
                            )
                    }
            }
        }

        /** Löscht Fehlermeldungen */
        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }
    }
