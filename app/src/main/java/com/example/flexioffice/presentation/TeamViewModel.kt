package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.TeamRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Team
import com.example.flexioffice.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

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

    val currentUserId: StateFlow<String?> = authRepository.currentUser
        .map { it?.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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
            _uiState.value = _uiState.value.copy(errorMessage = "E-Mail darf nicht leer sein")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val currentUserId = authRepository.currentUser.first()?.uid
                    ?: throw Exception("Nicht angemeldet")
                val currentTeam = _uiState.value.currentTeam
                    ?: throw Exception("Kein Team ausgewählt")

                if (currentTeam.managerId != currentUserId) {
                    throw Exception("Nur Manager können einladen")
                }

                val querySnapshot = teamRepository.findUserByEmail(email)
                val invitedUserDoc = querySnapshot.documents.firstOrNull()
                    ?: throw Exception("Benutzer nicht gefunden")
                val invitedUserId = invitedUserDoc.id

                if (invitedUserDoc.getString("teamId")?.isNotEmpty() == true) {
                    throw Exception("Benutzer ist bereits in einem Team")
                }

                teamRepository.addTeamMember(currentTeam.id, invitedUserId)

                userRepository.updateUserTeamAndRole(
                    userId = invitedUserId,
                    teamId = currentTeam.id,
                    role = User.ROLE_USER
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isInviteDialogVisible = false,
                    shouldRefreshUserData = true
                )
                loadTeamDetails(currentTeam.id)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Einladung fehlgeschlagen"
                )
            }
        }
    }
    }
