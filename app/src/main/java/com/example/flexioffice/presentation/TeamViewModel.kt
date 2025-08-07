package com.example.flexioffice.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexioffice.data.AuthRepository
import com.example.flexioffice.data.TeamRepository
import com.example.flexioffice.data.UserRepository
import com.example.flexioffice.data.model.Team
import com.example.flexioffice.data.model.TeamInvitation
import com.example.flexioffice.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val currentUser: User? = null,
    val currentTeam: Team? = null,
    val teamMembers: List<User> = emptyList(),
    val isInviteDialogVisible: Boolean = false,
    val pendingInvitations: List<TeamInvitation> = emptyList(),
) {
    val canCreateTeam: Boolean =
        currentUser?.teamId == User.NO_TEAM &&
            currentUser.role == User.ROLE_MANAGER

    val isTeamManager: Boolean =
        currentTeam?.managerId == currentUser?.id
}

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

        private val _events = Channel<TeamEvent>()
        val events = _events.receiveAsFlow()

        /** Removes a member from the team */
        fun removeMember(userId: String) {
            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isLoading = true) }

                    val currentTeam = _uiState.value.currentTeam
                    if (currentTeam == null) {
                        _events.send(TeamEvent.Error("Kein aktives Team gefunden"))
                        return@launch
                    }

                    if (_uiState.value.currentUser?.id != currentTeam.managerId) {
                        _events.send(TeamEvent.Error("Keine Berechtigung zum Entfernen von Mitgliedern"))
                        return@launch
                    }

                    // First remove user from team members
                    val updatedMembers = currentTeam.members.filter { it != userId }
                    val updatedTeam = currentTeam.copy(members = updatedMembers)

                    teamRepository
                        .updateTeam(updatedTeam)
                        .onSuccess {
                            // Then reset team ID and restore manager role for user
                            val userUpdateSuccess = userRepository.removeUserFromTeam(userId)
                            if (userUpdateSuccess) {
                                _events.send(TeamEvent.MemberRemoved)
                            } else {
                                _events.send(TeamEvent.Error("Fehler beim Aktualisieren des Users"))
                            }
                        }.onFailure { e ->
                            _events.send(TeamEvent.Error("Fehler beim Aktualisieren des Teams: ${e.message}"))
                        }
                } catch (e: Exception) {
                    _events.send(TeamEvent.Error("Fehler beim Entfernen des Mitglieds: ${e.message}"))
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }

        init {
            observeUserAndTeamData()
            observePendingInvitations()
        }

        /** Loads team details if the user is in a team */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeUserAndTeamData() {
            viewModelScope.launch {
                // Reactively listen for the authenticated user's ID
                authRepository.currentUser
                    .map { it?.uid }
                    .distinctUntilChanged()
                    .flatMapLatest { userId ->
                        if (userId == null) {
                            // If user logs out, reset state
                            flowOf(
                                TeamUiState(
                                    currentUser = null,
                                    currentTeam = null,
                                    teamMembers = emptyList(),
                                ),
                            )
                        } else {
                            // When we have a user ID, get their data stream
                            userRepository.getUserStream(userId).flatMapLatest { userResult ->
                                val user = userResult.getOrNull()
                                val teamId = user?.teamId

                                if (teamId.isNullOrEmpty() || teamId == User.NO_TEAM) {
                                    // User is not in a team
                                    flowOf(TeamUiState(currentUser = user, isLoading = false))
                                } else {
                                    // User is in a team, combine team and member streams
                                    combine(
                                        teamRepository.getTeamStream(teamId),
                                        userRepository.getTeamMembersStream(teamId),
                                    ) { teamResult, membersResult ->
                                        TeamUiState(
                                            currentUser = user,
                                            currentTeam = teamResult.getOrNull(),
                                            teamMembers = membersResult.getOrNull() ?: emptyList(),
                                            errorMessage =
                                                teamResult.exceptionOrNull()?.message
                                                    ?: membersResult.exceptionOrNull()?.message,
                                            isLoading = false,
                                        )
                                    }
                                }
                            }
                        }
                    }.catch { e ->
                        // Handle errors from the upstream flows
                        _uiState.update { it.copy(errorMessage = e.message) }
                    }.collect { state ->
                        // Update the main UI state
                        _uiState.value = state
                    }
            }
        }

        /** Loads the current user's team */
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

        /** Loads the members of the specified team */
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

        /** Creates a new team */
        fun createTeam(
            name: String,
            description: String = "",
        ) {
            if (name.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Der Teamname darf nicht leer sein.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val currentUser =
                    _uiState.value.currentUser
                        ?: run {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "Kein aktueller Benutzer gefunden. Bitte anmelden.",
                                )
                            }
                            return@launch
                        }

                val newTeam =
                    Team(
                        name = name,
                        description = description,
                        members = listOf(currentUser.id),
                        managerId = currentUser.id,
                    )

                // Use the atomic repository function
                teamRepository
                    .createTeamAtomically(newTeam)
                    .onSuccess {
                        _events.send(TeamEvent.TeamCreationSuccess)
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(errorMessage = "Team-Erstellung fehlgeschlagen: ${e.message}")
                        }
                    }

                _uiState.update { it.copy(isLoading = false) }
            }
        }

        /** Clears any error message */
        fun clearError() {
            _uiState.value = _uiState.value.copy(errorMessage = null)
        }

        /** Shows the invite dialog */
        fun showInviteDialog() {
            _uiState.value = _uiState.value.copy(isInviteDialogVisible = true)
        }

        /** Hides the invite dialog */
        fun hideInviteDialog() {
            _uiState.value = _uiState.value.copy(isInviteDialogVisible = false)
        }

        /** Invites a user by email */
        fun inviteUserByEmail(email: String) {
            if (email.isBlank()) {
                _uiState.update { it.copy(errorMessage = "E-Mail darf nicht leer sein.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val teamId = _uiState.value.currentTeam?.id
                val managerId = _uiState.value.currentUser?.id

                if (teamId == null || managerId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Team oder Manager nicht gefunden. Bitte anmelden.",
                        )
                    }
                    return@launch
                }

                // Use the new invitation system
                teamRepository
                    .createTeamInvitation(teamId, managerId, email)
                    .onSuccess {
                        _events.send(TeamEvent.InviteSuccess)
                        hideInviteDialog()
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(errorMessage = "Einladung fehlgeschlagen: ${e.message}")
                        }
                    }

                _uiState.update { it.copy(isLoading = false) }
            }
        }

        /** Loads pending invitations for the current user */

        /** Observes pending invitations in real-time */
        private fun observePendingInvitations() {
            viewModelScope.launch {
                teamRepository
                    .getPendingInvitationsFlow()
                    .collect { result ->
                        result
                            .onSuccess { invitations ->
                                _uiState.update { it.copy(pendingInvitations = invitations) }
                            }.onFailure { e ->
                                _uiState.update {
                                    it.copy(
                                        errorMessage = "Fehler beim Laden der Einladungen: ${e.message}",
                                    )
                                }
                            }
                    }
            }
        }

        fun loadPendingInvitations() {
            viewModelScope.launch {
                teamRepository
                    .getPendingInvitations()
                    .onSuccess { invitations ->
                        _uiState.update { it.copy(pendingInvitations = invitations) }
                    }.onFailure { e ->
                        _uiState.update { it.copy(errorMessage = "Fehler beim Laden der Einladungen: ${e.message}") }
                    }
            }
        }

        /** Accepts a team invitation */
        fun acceptInvitation(invitationId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                teamRepository
                    .acceptTeamInvitation(invitationId)
                    .onSuccess {
                        _events.send(TeamEvent.InvitationAccepted)
                        loadPendingInvitations() // Refresh invitations
                        // The team data will be updated automatically via the existing flows
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                errorMessage = "Fehler beim Akzeptieren der Einladung: ${e.message}",
                            )
                        }
                    }
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        /** Declines a team invitation */
        fun declineInvitation(invitationId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                teamRepository
                    .declineTeamInvitation(invitationId)
                    .onSuccess {
                        _events.send(TeamEvent.InvitationDeclined)
                        loadPendingInvitations() // Refresh invitations
                    }.onFailure { e ->
                        _uiState.update { it.copy(errorMessage = "Fehler beim Ablehnen der Einladung: ${e.message}") }
                    }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

sealed class TeamEvent {
    object TeamCreationSuccess : TeamEvent()

    object InviteSuccess : TeamEvent()

    object MemberRemoved : TeamEvent()

    object InvitationAccepted : TeamEvent()

    object InvitationDeclined : TeamEvent()

    data class Error(
        val message: String,
    ) : TeamEvent()
}
