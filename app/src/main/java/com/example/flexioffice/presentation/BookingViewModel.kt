package com.example.flexioffice.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.BookingRepository
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
    val approverName: String? = null,
)

@HiltViewModel
class BookingViewModel
    @Inject
    constructor(
        private val bookingRepository: BookingRepository,
        private val teamRepository: TeamRepository,
        private val userRepository: UserRepository,
        private val authRepository: AuthRepository,
        private val auth: FirebaseAuth,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                BookingUiState(),
            )
        val uiState: StateFlow<BookingUiState> = _uiState

        init {
            observeUserBookings()
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
                        _uiState.value =
                            _uiState.value.copy(error = e.message, isLoading = false)
                    }.collect { state -> _uiState.update { state } }
            }
        }

        fun showBookingDialog() {
            _uiState.update { it.copy(showBookingDialog = true) }
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
        }

        fun updateComment(comment: String) {
            _uiState.update { it.copy(comment = comment) }
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

            // Prüfe, ob das ausgewählte Datum in der Vergangenheit liegt
            if (currentState.selectedDate.isBefore(LocalDate.now())) {
                _uiState.update {
                    it.copy(error = "Buchungen für vergangene Tage sind nicht möglich")
                }
                return
            }
            // Prüfe, ob bereits eine aktive Buchung für dieses Datum existiert
            val existingBooking =
                currentState.userBookings.find {
                    it.date == currentState.selectedDate &&
                        it.status != BookingStatus.CANCELLED
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
                        "Erstelle Buchung für User: ${user.name}, Team: ${team.id}",
                    )

                    val booking =
                        Booking(
                            id = "", // Die ID wird jetzt im Repository gesetzt
                            userId = userId,
                            userName = user.name,
                            teamId = team.id,
                            dateString = currentState.selectedDate.toString(),
                            type = BookingType.HOME_OFFICE,
                            status = BookingStatus.PENDING,
                            comment = currentState.comment,
                            createdAt = LocalDate.now().toString(),
                            reviewerId = team.managerId,
                        )

                    bookingRepository.createBooking(booking)
                    hideBookingDialog()
                } catch (e: Exception) {
                    Log.e("BookingViewModel", "Fehler beim Erstellen der Buchung", e)
                    _uiState.update {
                        it.copy(
                            error = "Die Buchung konnte nicht erstellt werden. Bitte versuchen Sie es später erneut.",
                        )
                    }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        private suspend fun loadApproverName(userId: String?): String {
            return try {
                if (userId == null) return "Nicht zugewiesen"
                val user = userRepository.getUserById(userId).getOrNull()
                user?.name ?: "Unbekannt"
            } catch (e: Exception) {
                Log.e("BookingViewModel", "Error loading approver name for userId: $userId", e)
                "Fehler beim Laden"
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
                        auth.currentUser?.uid
                            ?: throw Exception("Benutzer nicht eingeloggt")
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
            _uiState.update { it.copy(showCancelledBookings = !it.showCancelledBookings) }
        }
    }
