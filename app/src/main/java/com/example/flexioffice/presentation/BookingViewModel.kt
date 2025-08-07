package com.example.flexioffice.presentation

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.BookingRepository
import com.example.flexioffice.data.NotificationRepository
import com.example.flexioffice.data.TeamRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showBookingDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val showDetailsSheet: Boolean = false,
    val showCancelDialog: Boolean = false,
    val selectedBooking: Booking? = null,
    val selectedDate: LocalDate? = null,
    val comment: String = "",
    val showCancelledBookings: Boolean = false,
    val userBookings: List<Booking> = emptyList(),
    val allUserBookings: List<Booking> = emptyList(), // unfiltered bookings
    val selectedStatus: BookingStatus? = null, // Filter for status
    val approverName: String? = null,
    val isWeekView: Boolean = false, // new Property for calendar view
    // Multi-select properties
    val isMultiSelectMode: Boolean = false,
    val selectedBookings: Set<String> = emptySet(), // Booking IDs
    val isBatchProcessing: Boolean = false,
)

@HiltViewModel
class BookingViewModel
    @Inject
    constructor(
        private val bookingRepository: BookingRepository,
        private val teamRepository: TeamRepository,
        private val userRepository: UserRepository,
        private val authRepository: AuthRepository,
        private val notificationRepository: NotificationRepository,
        private val auth: FirebaseAuth,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        companion object {
            private const val SELECTED_DATE_KEY = "booking_selected_date"
            private const val SELECTED_STATUS_KEY = "booking_selected_status"
            private const val IS_WEEK_VIEW_KEY = "booking_is_week_view"
            private const val SHOW_CANCELLED_KEY = "booking_show_cancelled"
            private const val COMMENT_KEY = "booking_comment"
            private const val TAG = "BookingViewModel"
        }

        private val _uiState =
            MutableStateFlow(
                BookingUiState(
                    selectedDate =
                        savedStateHandle.get<String>(SELECTED_DATE_KEY)?.let {
                            try {
                                LocalDate.parse(it)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing date", e)
                                null
                            }
                        },
                    selectedStatus =
                        savedStateHandle.get<String>(SELECTED_STATUS_KEY)?.let {
                            try {
                                BookingStatus.valueOf(it)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing booking status", e)
                                null
                            }
                        },
                    isWeekView = savedStateHandle.get<Boolean>(IS_WEEK_VIEW_KEY) ?: false,
                    showCancelledBookings = savedStateHandle.get<Boolean>(SHOW_CANCELLED_KEY) ?: false,
                    comment = savedStateHandle.get<String>(COMMENT_KEY) ?: "",
                ),
            )
        val uiState: StateFlow<BookingUiState> = _uiState

        init {
            observeUserBookings()
        }

        fun toggleCalendarView() {
            val newIsWeekView = !_uiState.value.isWeekView
            _uiState.update { it.copy(isWeekView = newIsWeekView) }
            savedStateHandle[IS_WEEK_VIEW_KEY] = newIsWeekView
        }

        fun onDateLongPressed(date: LocalDate) {
            showBookingDialogForDate(date)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeUserBookings() {
            viewModelScope.launch {
                authRepository
                    .currentUser
                    .map { it?.uid }
                    .distinctUntilChanged()
                    .flatMapLatest { userId ->
                        if (userId == null) {
                            flowOf(BookingUiState(isLoading = false, error = "User not logged in"))
                        } else {
                            userRepository.getUserStream(userId).flatMapLatest { userResult ->
                                val user = userResult.getOrNull()
                                if (user?.teamId.isNullOrEmpty() || user.teamId == User.NO_TEAM) {
                                    // User has no team, show empty calendar
                                    flowOf(
                                        BookingUiState(
                                            isLoading = false,
                                            userBookings = emptyList(),
                                            error = null,
                                        ),
                                    )
                                } else {
                                    bookingRepository
                                        .getUserBookingsStream(
                                            userId,
                                        ).map { bookingsResult ->
                                            val bookings =
                                                bookingsResult.getOrNull() ?: emptyList()
                                            BookingUiState(
                                                isLoading = false,
                                                userBookings = bookings,
                                                allUserBookings = bookings,
                                                error =
                                                    bookingsResult
                                                        .exceptionOrNull()
                                                        ?.message,
                                            )
                                        }
                                }
                            }
                        }
                    }.catch { e ->
                        _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                    }.collect { state -> _uiState.update { state } }
            }
        }

        // Filter functions
        fun setStatusFilter(status: BookingStatus?) {
            _uiState.update { it.copy(selectedStatus = status) }
            applyFilters()
        }

        fun clearFilters() {
            _uiState.update { it.copy(selectedStatus = null) }
            applyFilters()
        }

        private fun applyFilters() {
            val state = _uiState.value
            var filteredBookings = state.allUserBookings

            // Filter after status
            state.selectedStatus?.let { status ->
                filteredBookings =
                    filteredBookings.filter { booking ->
                        booking.status == status
                    }
            }

            _uiState.update { it.copy(userBookings = filteredBookings) }
        }

        fun showBookingDialogForDate(date: LocalDate) {
            viewModelScope.launch {
                _uiState.update { currentState ->
                    currentState.copy(
                        showBookingDialog = true,
                        selectedDate = date,
                        comment = "",
                    )
                }
            }
        }

        fun showBookingDialog(preselectedDate: LocalDate? = null) {
            viewModelScope.launch {
                _uiState.update { currentState ->
                    currentState.copy(
                        showBookingDialog = true,
                        selectedDate = preselectedDate,
                        comment = "",
                    )
                }
            }
        }

        fun hideBookingDialog() {
            _uiState.update {
                it.copy(
                    showBookingDialog = false,
                    showDatePicker = false,
                    selectedDate = null,
                    comment = "",
                )
            }
        }

        fun showDatePicker() {
            _uiState.update { it.copy(showDatePicker = true) }
        }

        fun hideDatePicker() {
            _uiState.update { it.copy(showDatePicker = false) }
        }

        fun updateSelectedDate(date: LocalDate) {
            _uiState.update { it.copy(selectedDate = date, showDatePicker = false) }
            savedStateHandle[SELECTED_DATE_KEY] = date.toString()
        }

        fun updateComment(comment: String) {
            _uiState.update { it.copy(comment = comment) }
            savedStateHandle[COMMENT_KEY] = comment
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun createBooking() {
            val userId =
                auth.currentUser?.uid
                    ?: run {
                        _uiState.update { it.copy(error = "Benutzer nicht eingeloggt") }
                        return
                    }
            val currentState = _uiState.value
            val selectedDate = currentState.selectedDate

            if (selectedDate == null) {
                _uiState.update { it.copy(error = "Bitte wählen Sie ein Datum aus") }
                return
            }

            // Check if the selected date is in the past
            if (selectedDate.isBefore(LocalDate.now())) {
                _uiState.update { it.copy(error = "Buchungen für vergangene Tage sind nicht möglich") }
                return
            }
            // Check if there is already an active booking for this date
            val existingBooking =
                currentState.userBookings.find {
                    it.date == currentState.selectedDate && it.status != BookingStatus.CANCELLED
                }
            if (existingBooking != null) {
                _uiState.update {
                    it.copy(error = "Sie haben bereits eine aktive Buchung für diesen Tag")
                }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                try {
                    val (user, team) =
                        coroutineScope {
                            val userDeferred = async { userRepository.getCurrentUser() }
                            val teamDeferred = async { teamRepository.getCurrentUserTeam() }
                            // await() will suspend until the results are ready
                            userDeferred.await() to teamDeferred.await()
                        }

                    if (user == null) {
                        _uiState.update { it.copy(error = "Benutzer konnte nicht geladen werden") }
                        return@launch
                    }

                    if (team == null) {
                        _uiState.update { it.copy(error = "Team konnte nicht geladen werden") }
                        return@launch
                    }

                    Log.d(
                        "BookingViewModel",
                        "Creating booking for User: ${user.name}, Team: ${team.id}",
                    )

                    val booking =
                        Booking(
                            id = "", // The ID will now be set in the repository
                            userId = userId,
                            userName = user.name,
                            teamId = team.id,
                            dateString = currentState.selectedDate.toString(),
                            type = BookingType.HOME_OFFICE,
                            status =
                                if (user.role == User.ROLE_MANAGER ||
                                    team.managerId == userId
                                ) {
                                    BookingStatus.APPROVED
                                } else {
                                    BookingStatus.PENDING
                                },
                            comment = currentState.comment,
                            createdAt = LocalDate.now().toString(),
                            reviewerId = team.managerId,
                        )

                    bookingRepository.createBooking(booking)
                    hideBookingDialog()
                    viewModelScope.launch {
                        try {
                            notificationRepository.sendNewBookingRequestNotification(
                                booking,
                                team.managerId,
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "BookingViewModel",
                                "Error sending notification",
                                e,
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BookingViewModel", "Error creating booking", e)
                    _uiState.update {
                        it.copy(
                            error =
                                "The booking could not be created. Please try again later.",
                        )
                    }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        private suspend fun loadApproverName(userId: String?): String {
            return try {
                if (userId == null) return "Not assigned"
                val user = userRepository.getUserById(userId).getOrNull()
                user?.name ?: "Unknown"
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error loading approver name for userId: $userId", e)
                "Error loading"
            }
        }

        fun showDetailsSheet(booking: Booking) {
            viewModelScope.launch {
                val approverName = loadApproverName(booking.reviewerId)
                _uiState.update { currentState ->
                    currentState.copy(
                        showDetailsSheet = true,
                        selectedBooking = booking,
                        approverName = approverName,
                    )
                }
            }
        }

        fun hideDetailsSheet() {
            viewModelScope.launch {
                _uiState.update { currentState ->
                    currentState.copy(
                        showDetailsSheet = false,
                        selectedBooking = null,
                        approverName = null,
                    )
                }
            }
        }

        fun showCancelDialog(booking: Booking) {
            _uiState.update {
                it.copy(
                    showCancelDialog = true,
                    selectedBooking = booking,
                )
            }
        }

        fun hideCancelDialog() {
            _uiState.update {
                it.copy(
                    showCancelDialog = false,
                    selectedBooking = null,
                )
            }
        }

        fun cancelBooking() {
            val booking = _uiState.value.selectedBooking ?: return

            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isLoading = true) }
                    val currentUserId =
                        auth.currentUser?.uid ?: throw Exception("Benutzer nicht eingeloggt")
                    bookingRepository.updateBookingStatus(
                        booking.id,
                        BookingStatus.CANCELLED,
                        currentUserId,
                    )
                    hideCancelDialog()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        fun toggleCancelledBookings() {
            val newShowCancelled = !_uiState.value.showCancelledBookings
            _uiState.update { it.copy(showCancelledBookings = newShowCancelled) }
            savedStateHandle[SHOW_CANCELLED_KEY] = newShowCancelled
        }

        // Multi-select functions
        fun startMultiSelectMode(booking: Booking? = null) {
            if (booking != null) {
                _uiState.update {
                    it.copy(
                        isMultiSelectMode = true,
                        selectedBookings = setOf(booking.id),
                    )
                }
                return
            }
            _uiState.update {
                it.copy(
                    isMultiSelectMode = true,
                    selectedBookings = emptySet(),
                )
            }
        }

        fun exitMultiSelectMode() {
            _uiState.update {
                it.copy(
                    isMultiSelectMode = false,
                    selectedBookings = emptySet(),
                )
            }
        }

        fun toggleBookingSelection(bookingId: String) {
            _uiState.update { currentState ->
                val selectedBookings = currentState.selectedBookings.toMutableSet()
                if (selectedBookings.contains(bookingId)) {
                    selectedBookings.remove(bookingId)
                } else {
                    selectedBookings.add(bookingId)
                }
                currentState.copy(selectedBookings = selectedBookings)
            }
        }

        fun selectAllBookings() {
            _uiState.update { currentState ->
                val allBookingIds =
                    currentState
                        .userBookings
                        .filter {
                            it.status != BookingStatus.CANCELLED
                        }.map { it.id }
                        .toSet()
                currentState.copy(selectedBookings = allBookingIds)
            }
        }

        fun clearSelection() {
            _uiState.update { it.copy(selectedBookings = emptySet()) }
        }

        fun batchCancelBookings() {
            val selectedIds = _uiState.value.selectedBookings
            if (selectedIds.isEmpty()) return

            val currentUserId = auth.currentUser?.uid ?: return

            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isBatchProcessing = true) }

                    bookingRepository
                        .updateBookingStatusBatch(
                            bookingIds = selectedIds.toList(),
                            status = BookingStatus.CANCELLED,
                            reviewerId = currentUserId,
                        ).fold(
                            onSuccess = {
                                _uiState.update {
                                    it.copy(
                                        selectedBookings = emptySet(),
                                        isMultiSelectMode = false,
                                        isBatchProcessing = false,
                                    )
                                }
                            },
                            onFailure = { e: Throwable ->
                                _uiState.update {
                                    it.copy(
                                        error = "Fehler beim Batch-Update: ${e.message}",
                                        isBatchProcessing = false,
                                    )
                                }
                            },
                        )
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            error = "Unerwarteter Fehler: ${e.message}",
                            isBatchProcessing = false,
                        )
                    }
                }
            }
        }
    }
