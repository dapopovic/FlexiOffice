package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.BookingRepository
import com.example.flexioffice.data.TeamRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.BookingType
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val showBookingDialog: Boolean = false,
    val selectedDate: LocalDate? = null,
    val comment: String = "",
    val userBookings: List<Booking> = emptyList(),
)

@HiltViewModel
class BookingViewModel
    @Inject
    constructor(
        private val bookingRepository: BookingRepository,
        private val teamRepository: TeamRepository,
        private val userRepository: UserRepository,
        private val auth: FirebaseAuth,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BookingUiState())
        val uiState: StateFlow<BookingUiState> = _uiState

        init {
            loadUserBookings()
        }

        private fun loadUserBookings() {
            val userId = auth.currentUser?.uid ?: return

            viewModelScope.launch {
                try {
                    val bookings = bookingRepository.getUserBookings(userId)
                    _uiState.update { it.copy(userBookings = bookings) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }

        fun showBookingDialog() {
            _uiState.update { it.copy(showBookingDialog = true) }
        }

        fun hideBookingDialog() {
            _uiState.update { it.copy(showBookingDialog = false, selectedDate = null, comment = "") }
        }

        fun updateSelectedDate(date: LocalDate) {
            _uiState.update { it.copy(selectedDate = date) }
        }

        fun updateComment(comment: String) {
            _uiState.update { it.copy(comment = comment) }
        }

        fun createBooking() {
            val currentState = _uiState.value
            val userId =
                auth.currentUser?.uid ?: run {
                    _uiState.update { it.copy(error = "Benutzer nicht eingeloggt") }
                    return
                }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                try {
                    val user = userRepository.getCurrentUser()
                    val team = teamRepository.getCurrentUserTeam()

                    if (currentState.selectedDate == null) {
                        _uiState.update { it.copy(error = "Bitte wählen Sie ein Datum aus") }
                        return@launch
                    }

                    // Prüfe, ob bereits eine Buchung für dieses Datum existiert
                    val existingBooking =
                        currentState.userBookings.find {
                            it.date == currentState.selectedDate
                        }

                    if (existingBooking != null) {
                        _uiState.update { it.copy(error = "Sie haben bereits eine Buchung für diesen Tag erstellt") }
                        return@launch
                    }

                    if (user == null) {
                        _uiState.update { it.copy(error = "Benutzer konnte nicht geladen werden") }
                        return@launch
                    }

                    if (team == null) {
                        _uiState.update { it.copy(error = "Team konnte nicht geladen werden") }
                        return@launch
                    }

                    android.util.Log.d("BookingViewModel", "Erstelle Buchung für User: ${user.name}, Team: ${team.id}")

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

                    android.util.Log.d("BookingViewModel", "Speichere Buchung in Firestore...")
                    bookingRepository.createBooking(booking)
                    android.util.Log.d("BookingViewModel", "Buchung erfolgreich gespeichert")
                    hideBookingDialog()
                    loadUserBookings() // Aktualisiere die Liste der Buchungen
                } catch (e: Exception) {
                    android.util.Log.e("BookingViewModel", "Fehler beim Erstellen der Buchung", e)
                    _uiState.update { it.copy(error = "Fehler beim Erstellen der Buchung: ${e.message}") }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
