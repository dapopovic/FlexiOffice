package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.TeamRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Team
import com.example.flexioffice.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isTeamCreated: Boolean = false,
    val canCreateTeam: Boolean = false,
    val currentUser: User? = null,
    val currentTeam: Team? = null,
    val teamMembers: List<User> = emptyList(),
    val isInviteDialogVisible: Boolean = false,
    val shouldRefreshUserData: Boolean = false,
)

@HiltViewModel
class TeamViewModel
    @Inject
    constructor(
        private val teamRepository: TeamRepository,
        private val userRepository: UserRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TeamUiState())
        val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

        init {
            checkUserStatus()
        }

        fun userDataRefreshed() {
            _uiState.value = _uiState.value.copy(shouldRefreshUserData = false)
        }

        private fun checkUserStatus() {
            viewModelScope.launch {
                authRepository.currentUser.collect { firebaseUser ->
                    if (firebaseUser != null) {
                        userRepository
                            .getUser(firebaseUser.uid)
                            .onSuccess { user ->
                                _uiState.value =
                                    _uiState.value.copy(
                                        currentUser = user,
                                        canCreateTeam =
                                            user?.teamId == User.NO_TEAM &&
                                                user?.role == User.ROLE_MANAGER,
                                    )

                                // Wenn der User in einem Team ist, lade die Team-Details
                                if (user?.teamId?.isNotEmpty() == true) {
                                    loadTeamDetails(user.teamId)
                                }
                            }.onFailure { exception ->
                                _uiState.value =
                                    _uiState.value.copy(
                                        errorMessage = exception.message,
                                    )
                            }
                    }
                }
            }
        }

        private fun loadTeamDetails(teamId: String) {
            viewModelScope.launch {
                teamRepository
                    .getTeam(teamId)
                    .onSuccess { team ->
                        _uiState.value = _uiState.value.copy(currentTeam = team)
                        loadTeamMembers(teamId)
                    }.onFailure { exception ->
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = "Fehler beim Laden der Team-Details: ${exception.message}",
                            )
                    }
            }
        }

        private fun loadTeamMembers(teamId: String) {
            viewModelScope.launch {
                val members = mutableListOf<User>()

                userRepository
                    .getUsersByTeamId(teamId)
                    .onSuccess { users ->
                        members.addAll(users)
                    }.onFailure { exception ->
                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage = "Fehler beim Laden der Team-Mitglieder: ${exception.message}",
                            )
                    }

                _uiState.value = _uiState.value.copy(teamMembers = members)
            }
        }

        fun createTeam(
            name: String,
            description: String = "",
        ) {
            if (name.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "Der Teamname darf nicht leer sein",
                    )
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                try {
                    val currentUserId =
                        authRepository.currentUser.first()?.uid
                            ?: throw Exception("Benutzer nicht angemeldet")

                    val team =
                        Team(
                            name = name,
                            description = description,
                            members = listOf(currentUserId),
                            managerId = currentUserId,
                        )
                    val teamId = teamRepository.createTeam(team).getOrThrow()

                    userRepository.updateUserTeamId(currentUserId, teamId).getOrThrow()

                    val updatedUser = _uiState.value.currentUser?.copy(teamId = teamId)

                    _uiState.value =
                        _uiState.value.copy(
                            currentUser = updatedUser,
                            canCreateTeam = false,
                            isLoading = false,
                            isTeamCreated = true,
                            errorMessage = null,
                        )
                    loadTeamDetails(teamId)
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Fehler beim Erstellen des Teams: ${e.message}",
                        )
                }
            }
        }

        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        fun resetCreatedState() {
            _uiState.value = _uiState.value.copy(isTeamCreated = false)
        }

        fun showInviteDialog() {
            _uiState.value = _uiState.value.copy(isInviteDialogVisible = true)
        }

        fun hideInviteDialog() {
            _uiState.value = _uiState.value.copy(isInviteDialogVisible = false)
        }

        fun inviteUserByEmail(email: String) {
            if (email.isBlank()) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage = "E-Mail-Adresse darf nicht leer sein",
                    )
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                // Prüfen ob der aktuelle User der Team-Manager ist
                val currentTeam = _uiState.value.currentTeam
                val currentUser = _uiState.value.currentUser

                if (currentTeam == null || currentUser == null) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Team oder Benutzer nicht gefunden",
                        )
                    return@launch
                }

                if (currentTeam.managerId != authRepository.currentUser.first()?.uid) {
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Nur der Team-Manager kann Mitglieder einladen",
                        )
                    return@launch
                }

                // Benutzer anhand der E-Mail suchen
                // Da wir keine direkte E-Mail-Suche haben, müssen wir das später implementieren
                // Hier könnte man einen Cloud Function aufrufen, die die Suche durchführt

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        isInviteDialogVisible = false,
                    )
            }
        }
    }
