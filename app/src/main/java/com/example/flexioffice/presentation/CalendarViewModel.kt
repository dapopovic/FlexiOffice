package com.example.flexioffice.presentation

import android.hardware.SensorManager
import androidx.compose.ui.graphics.Color
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
import com.example.flexioffice.util.ShakeDetector
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
    val allBookings: List<Booking> = emptyList(), // Ungefilterte Buchungen
    val teamMembers: List<User> = emptyList(),
    val selectedTeamMember: String? = null, // Filter für Teammitglied
    val selectedStatus: BookingStatus? = null, // Filter für Status
    val showBookingDialog: Boolean = false,
    val bookingDialogDate: LocalDate? = null,
    val bookingComment: String = "",
    val isCreatingBooking: Boolean = false,
    val showCancelDialog: Boolean = false,
    val cancelBookingId: String? = null,
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

        companion object {
            private const val HOME_OFFICE_COLOR = 0xFF4CAF50L // Green
        }

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
                                if (user?.teamId.isNullOrEmpty() || user?.teamId == User.NO_TEAM) {
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
                                            val allBookings = bookingsResult.getOrNull() ?: emptyList()
                                            val bookings = allBookings.filter { it.status != BookingStatus.CANCELLED }
                                            CalendarUiState(
                                                isLoading = false,
                                                currentUser = user,
                                                currentMonth = currentMonth,
                                                bookings = bookings,
                                                allBookings = allBookings,
                                                teamMembers = emptyList(), // Wird später geladen
                                                events = mapBookingsToEvents(bookings),
                                                errorMessage = bookingsResult.exceptionOrNull()?.message,
                                            )
                                        }
                                }
                            }
                        }
                    }.catch { e ->
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = e.message,
                                isLoading = false,
                            )
                    }.collect { state ->
                        _uiState.value = state
                        // Team-Mitglieder laden wenn User ein Team hat
                        if (!state.currentUser?.teamId.isNullOrEmpty() && state.currentUser?.teamId != User.NO_TEAM) {
                            android.util.Log.d(
                                "CalendarViewModel",
                                "Loading team members for teamId: ${state.currentUser?.teamId}",
                            )
                            loadTeamMembers()
                        }
                    }
            }
        }

        private fun mapBookingsToEvents(bookings: List<Booking>): List<CalendarEvent> =
            bookings.map { booking ->
                CalendarEvent(
                    id = booking.id,
                    title = booking.userName,
                    date = booking.date,
                    type = EventType.HOME_OFFICE,
                    participantNames = listOf(booking.userName),
                    color = Color(HOME_OFFICE_COLOR),
                )
            }

        fun clearErrorMessage() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun toggleViewMode() {
            _uiState.value = _uiState.value.copy(isWeekView = !_uiState.value.isWeekView)
        }

        fun selectDate(date: LocalDate) {
            _uiState.value = _uiState.value.copy(selectedDate = date)
        }

        fun nextMonth() {
            val nextMonth = _uiState.value.currentMonth.plusMonths(1)
            _uiState.value = _uiState.value.copy(currentMonth = nextMonth)
            loadBookingsForMonth(nextMonth)
        }

        fun previousMonth() {
            val previousMonth = _uiState.value.currentMonth.minusMonths(1)
            _uiState.value = _uiState.value.copy(currentMonth = previousMonth)
            loadBookingsForMonth(previousMonth)
        }

        fun refresh() {
            loadBookingsForMonth(_uiState.value.currentMonth)
        }

        // Filter-Funktionen
        fun setTeamMemberFilter(userId: String?) {
            _uiState.value = _uiState.value.copy(selectedTeamMember = userId)
            applyFilters()
        }

        fun setStatusFilter(status: BookingStatus?) {
            _uiState.value = _uiState.value.copy(selectedStatus = status)
            applyFilters()
        }

        fun clearFilters() {
            _uiState.value =
                _uiState.value.copy(
                    selectedTeamMember = null,
                    selectedStatus = null,
                )
            applyFilters()
        }

        private fun applyFilters() {
            val state = _uiState.value
            var filteredBookings = state.allBookings

            // Filter nach Teammitglied
            state.selectedTeamMember?.let { userId ->
                filteredBookings =
                    filteredBookings.filter { booking ->
                        booking.userId == userId
                    }
            }

            // Filter nach Status
            state.selectedStatus?.let { status ->
                filteredBookings =
                    filteredBookings.filter { booking ->
                        booking.status == status
                    }
            }

            _uiState.value =
                state.copy(
                    bookings = filteredBookings,
                    events = mapBookingsToEvents(filteredBookings),
                )
        }

        fun showBookingDialog(
            date: LocalDate,
            defaultComment: String = "",
        ) {
            _uiState.value =
                _uiState.value.copy(
                    showBookingDialog = true,
                    bookingDialogDate = date,
                    bookingComment = defaultComment,
                    errorMessage = null,
                )
        }

        fun hideBookingDialog() {
            _uiState.value =
                _uiState.value.copy(
                    showBookingDialog = false,
                    bookingDialogDate = null,
                    bookingComment = "",
                )
        }

        fun updateBookingComment(comment: String) {
            _uiState.value = _uiState.value.copy(bookingComment = comment)
        }

        fun createDirectBooking(
            date: LocalDate,
            comment: String = "Home-Office Tag",
        ) {
            val currentUser = _uiState.value.currentUser ?: return

            viewModelScope.launch {
                try {
                    val result =
                        bookingRepository.createBooking(
                            date = date,
                            comment = comment,
                            userId = currentUser.id,
                            userName = currentUser.name,
                            teamId = currentUser.teamId,
                        )
                    if (!result.isSuccess) {
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = result.exceptionOrNull()?.message ?: "Fehler beim Erstellen der Buchung",
                            )
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = e.message ?: "Fehler beim Erstellen der Buchung",
                        )
                }
            }
        }

        fun handleBookingCreation() {
            val currentState = _uiState.value
            val date = currentState.bookingDialogDate ?: return
            val comment = currentState.bookingComment
            val currentUser = currentState.currentUser ?: return

            viewModelScope.launch {
                _uiState.value = currentState.copy(isCreatingBooking = true)
                try {
                    val result =
                        bookingRepository.createBooking(
                            date = date,
                            comment = comment,
                            userId = currentUser.id,
                            userName = currentUser.name,
                            teamId = currentUser.teamId,
                        )

                    if (result.isSuccess) {
                        _uiState.value =
                            currentState.copy(
                                showBookingDialog = false,
                                bookingDialogDate = null,
                                bookingComment = "",
                                isCreatingBooking = false,
                            )
                        loadBookingsForMonth(currentState.currentMonth)
                    } else {
                        _uiState.value =
                            currentState.copy(
                                errorMessage = result.exceptionOrNull()?.message ?: "Fehler beim Erstellen der Buchung",
                                isCreatingBooking = false,
                            )
                    }
                } catch (e: Exception) {
                    _uiState.value =
                        currentState.copy(
                            errorMessage = e.message ?: "Fehler beim Erstellen der Buchung",
                            isCreatingBooking = false,
                        )
                }
            }
        }

        fun loadBookingsForMonth(month: YearMonth) {
            val teamId = _uiState.value.currentUser?.teamId
            if (teamId.isNullOrEmpty() || teamId == User.NO_TEAM) return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingMonthData = true)

                try {
                    // Buchungen laden
                    bookingRepository
                        .getTeamBookingsStream(teamId, month.year, month.monthValue)
                        .collect { bookingsResult ->
                            val allBookings = bookingsResult.getOrNull() ?: emptyList()

                            _uiState.value =
                                _uiState.value.copy(
                                    isLoadingMonthData = false,
                                    allBookings = allBookings,
                                    errorMessage = bookingsResult.exceptionOrNull()?.message,
                                )

                            applyFilters()
                        }
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingMonthData = false,
                            errorMessage = e.message ?: "Fehler beim Laden der Buchungen",
                        )
                }
            }
        }

        fun loadTeamMembers() {
            val teamId = _uiState.value.currentUser?.teamId
            if (teamId.isNullOrEmpty() || teamId == User.NO_TEAM) return

            android.util.Log.d("CalendarViewModel", "loadTeamMembers called for teamId: $teamId")
            viewModelScope.launch {
                try {
                    userRepository.getTeamMembersStream(teamId).collect { teamMembersResult ->
                        val teamMembers = teamMembersResult.getOrNull() ?: emptyList()
                        android.util.Log.d("CalendarViewModel", "Loaded ${teamMembers.size} team members")
                        teamMembers.forEach { member ->
                            android.util.Log.d("CalendarViewModel", "Team member: ${member.name} (${member.id})")
                        }
                        _uiState.value = _uiState.value.copy(teamMembers = teamMembers)
                    }
                } catch (e: Exception) {
                    // Team-Mitglieder-Fehler sind nicht kritisch, aber loggen wir es
                    android.util.Log.w("CalendarViewModel", "Fehler beim Laden der Team-Mitglieder: ${e.message}")
                }
            }
        }

        fun loadDemoData() {
            val teamId = _uiState.value.currentUser?.teamId
            if (teamId.isNullOrEmpty() || teamId == User.NO_TEAM) return

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingDemoData = true)
                bookingRepository
                    .createDemoBookings(teamId)
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(isLoadingDemoData = false)
                        loadBookingsForMonth(_uiState.value.currentMonth)
                    }.onFailure { e: Throwable ->
                        _uiState.value =
                            _uiState.value.copy(
                                isLoadingDemoData = false,
                                errorMessage = e.message ?: "Fehler beim Laden der Demo-Daten",
                            )
                    }
            }
        }

        // Shake-Erkennung
        private var sensorManager: android.hardware.SensorManager? = null
        private var shakeDetector: ShakeDetector? = null

        fun registerShakeDetection(context: android.content.Context) {
            if (sensorManager != null) return
            sensorManager =
                context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
            val accelerometer = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
            shakeDetector =
                ShakeDetector {
                    onShakeDetected()
                }
            sensorManager?.registerListener(
                shakeDetector,
                accelerometer,
                android.hardware.SensorManager.SENSOR_DELAY_UI,
            )
        }

        fun unregisterShakeDetection() {
            sensorManager?.unregisterListener(shakeDetector)
            sensorManager = null
            shakeDetector = null
        }

        private fun onShakeDetected() {
            val state = _uiState.value
            if (state.showCancelDialog) return // Dialog ist offen, Shake ignorieren
            val selectedDate = state.selectedDate
            val currentUser = state.currentUser
            if (selectedDate != null && currentUser != null) {
                val booking =
                    state.bookings.find {
                        it.date == selectedDate &&
                            it.userId == currentUser.id &&
                            it.status != BookingStatus.CANCELLED
                    }
                if (booking != null) {
                    _uiState.value = state.copy(showCancelDialog = true, cancelBookingId = booking.id)
                }
            }
        }

        fun hideCancelDialog() {
            _uiState.value = _uiState.value.copy(showCancelDialog = false, cancelBookingId = null)
        }

        fun confirmCancelBooking() {
            val state = _uiState.value
            val bookingId = state.cancelBookingId ?: return
            val currentUser = state.currentUser ?: return
            viewModelScope.launch {
                try {
                    bookingRepository.updateBookingStatus(
                        bookingId = bookingId,
                        status = BookingStatus.CANCELLED,
                        reviewerId = currentUser.id,
                    )
                    _uiState.value = state.copy(showCancelDialog = false, cancelBookingId = null)
                    loadBookingsForMonth(state.currentMonth)
                } catch (e: Exception) {
                    _uiState.value =
                        state.copy(
                            errorMessage = e.message ?: "Fehler beim Stornieren der Buchung",
                            showCancelDialog = false,
                            cancelBookingId = null,
                        )
                }
            }
        }
    }
