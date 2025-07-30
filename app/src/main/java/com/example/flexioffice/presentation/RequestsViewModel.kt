package com.example.flexioffice.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.BookingRepository
import com.example.flexioffice.data.NotificationRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Booking
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RequestsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingRequests: List<Booking> = emptyList(),
    val currentUser: User? = null,
    val selectedBooking: Booking? = null,
    val isApprovingRequest: Boolean = false,
    val isDecliningRequest: Boolean = false,
)

@HiltViewModel
class RequestsViewModel
    @Inject
    constructor(
        private val bookingRepository: BookingRepository,
        private val userRepository: UserRepository,
        private val authRepository: AuthRepository,
        private val notificationRepository: NotificationRepository,
        private val auth: FirebaseAuth,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RequestsUiState())
        val uiState: StateFlow<RequestsUiState> = _uiState

        init {
            observePendingRequests()
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observePendingRequests() {
            viewModelScope.launch {
                authRepository
                    .currentUser
                    .map { it?.uid }
                    .distinctUntilChanged()
                    .flatMapLatest { userId ->
                        if (userId == null) {
                            flowOf(RequestsUiState(isLoading = false, error = "User not logged in"))
                        } else {
                            userRepository.getUserStream(userId).flatMapLatest { userResult ->
                                val user = userResult.getOrNull()
                                if (user?.teamId.isNullOrEmpty() || user?.teamId == User.NO_TEAM) {
                                    // User has no team, show empty state
                                    flowOf(
                                        RequestsUiState(
                                            isLoading = false,
                                            currentUser = user,
                                            pendingRequests = emptyList(),
                                            error = null,
                                        ),
                                    )
                                } else if (user?.role != User.ROLE_MANAGER) {
                                    // User is not a manager, no access to requests
                                    flowOf(
                                        RequestsUiState(
                                            isLoading = false,
                                            currentUser = user,
                                            pendingRequests = emptyList(),
                                            error =
                                                "Keine Berechtigung zum Anzeigen von Anfragen",
                                        ),
                                    )
                                } else {
                                    // User is a manager, load pending team requests
                                    bookingRepository
                                        .getTeamPendingRequestsStream(user.teamId)
                                        .map { requestsResult ->
                                            val requests =
                                                requestsResult.getOrNull() ?: emptyList()
                                            RequestsUiState(
                                                isLoading = false,
                                                currentUser = user,
                                                pendingRequests = requests,
                                                error =
                                                    requestsResult
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

        fun approveRequest(booking: Booking) {
            processBookingRequest(
                booking = booking,
                newStatus = BookingStatus.APPROVED,
                isApproving = true,
            )
        }

        fun declineRequest(booking: Booking) {
            processBookingRequest(
                booking = booking,
                newStatus = BookingStatus.DECLINED,
                isApproving = false,
            )
        }

        private fun processBookingRequest(
            booking: Booking,
            newStatus: BookingStatus,
            isApproving: Boolean,
        ) {
            val currentUserId = auth.currentUser?.uid ?: return
            val action = if (isApproving) "Genehmige" else "Lehne"
            val actionPast = if (isApproving) "genehmigt" else "abgelehnt"
            val actionError = if (isApproving) "Genehmigen" else "Ablehnen"
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(
                            isApprovingRequest = isApproving,
                            isDecliningRequest = !isApproving,
                            selectedBooking = booking,
                            error = null,
                        )
                    }

                    Log.d(
                        "RequestsViewModel",
                        "$action Antrag ab: ${booking.id} für User: ${booking.userName}",
                    )

                    bookingRepository
                        .updateBookingStatus(
                            booking.id,
                            newStatus,
                            currentUserId,
                        ).fold(
                            onSuccess = {
                                Log.d("RequestsViewModel", "Antrag erfolgreich $actionPast")

                                // Send FCM notification to requester
                                sendStatusNotification(booking, newStatus)

                                _uiState.update {
                                    it.copy(
                                        isApprovingRequest = false,
                                        isDecliningRequest = false,
                                        selectedBooking = null,
                                    )
                                }
                            },
                            onFailure = { exception ->
                                Log.e("RequestsViewModel", "Fehler beim $actionError", exception)
                                _uiState.update {
                                    it.copy(
                                        error =
                                            "Fehler beim $actionError: ${exception.message}",
                                        isApprovingRequest = false,
                                        isDecliningRequest = false,
                                        selectedBooking = null,
                                    )
                                }
                            },
                        )
                } catch (e: Exception) {
                    Log.e("RequestsViewModel", "Unerwarteter Fehler beim $actionError", e)
                    _uiState.update {
                        it.copy(
                            error = "Unerwarteter Fehler: ${e.message}",
                            isApprovingRequest = false,
                            isDecliningRequest = false,
                            selectedBooking = null,
                        )
                    }
                }
            }
        }

        private fun sendStatusNotification(
            booking: Booking,
            newStatus: BookingStatus,
        ) {
            viewModelScope.launch {
                try {
                    notificationRepository.sendBookingStatusNotification(
                        booking = booking,
                        newStatus = newStatus,
                        reviewerName = uiState.value.currentUser?.name ?: "Manager",
                    )
                    Log.d("RequestsViewModel", "FCM-Notification erfolgreich gesendet")
                } catch (e: Exception) {
                    Log.e("RequestsViewModel", "Fehler beim Senden der FCM-Notification", e)
                    // Don't fail the overall operation if notification fails
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun isProcessingRequest(bookingId: String): Boolean {
            val state = _uiState.value
            return (state.isApprovingRequest || state.isDecliningRequest) &&
                state.selectedBooking?.id == bookingId
        }
    }
