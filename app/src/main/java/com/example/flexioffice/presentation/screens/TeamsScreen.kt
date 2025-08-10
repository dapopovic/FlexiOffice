package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.R
import com.example.flexioffice.data.model.TeamInvitation
import com.example.flexioffice.data.model.User
import com.example.flexioffice.presentation.TeamEvent
import com.example.flexioffice.presentation.TeamViewModel
import com.example.flexioffice.presentation.components.CreateTeamDialog
import com.example.flexioffice.presentation.components.DeleteTeamMemberDialog
import com.example.flexioffice.presentation.components.Header
import com.example.flexioffice.presentation.components.InvitationAction
import com.example.flexioffice.presentation.components.InvitationConfirmationDialog
import com.example.flexioffice.presentation.components.InviteTeamMemberDialog
import com.example.flexioffice.presentation.components.TeamInvitationCard

private const val TAG = "TeamsScreen"

@Composable
fun TeamMemberItem(
    member: User,
    isManager: Boolean,
    canRemoveMember: Boolean = false,
    onRemoveClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isManager || member.role == User.ROLE_MANAGER) {
                    Icons.Default.Star
                } else {
                    Icons.Default.Person
                },
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint =
                    if (isManager || member.role == User.ROLE_MANAGER) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Column {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text =
                        stringResource(
                            if (isManager || member.role == User.ROLE_MANAGER) {
                                R.string.team_member_manager
                            } else {
                                R.string.team_member_member
                            },
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (canRemoveMember && member.role != User.ROLE_MANAGER) {
            IconButton(
                onClick = onRemoveClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.team_member_remove_content_desc),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun TeamsScreen(viewModel: TeamViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var inviteEmail by remember { mutableStateOf("") }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var showInvitationConfirmation by remember { mutableStateOf(false) }
    var pendingInvitationAction by remember { mutableStateOf<InvitationAction?>(null) }

    // Event-Handling
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TeamEvent.TeamCreationSuccess -> {
                    showCreateTeamDialog = false
                    teamName = ""
                    teamDescription = ""
                }
                is TeamEvent.InviteSuccess -> {
                    inviteEmail = ""
                }
                is TeamEvent.MemberRemoved -> {
                    Log.d(TAG, "Member successfully removed")
                    // The UI will automatically update due to state changes
                }
                is TeamEvent.InvitationAccepted -> {
                    Log.d(TAG, "Invitation accepted successfully")
                    showInvitationConfirmation = false
                    pendingInvitationAction = null
                }
                is TeamEvent.InvitationDeclined -> {
                    Log.d(TAG, "Invitation declined")
                    showInvitationConfirmation = false
                    pendingInvitationAction = null
                }
                is TeamEvent.InvitationCancelled -> {
                    Log.d(TAG, "Invitation cancelled successfully")
                    // The UI will automatically update due to state changes
                }
                is TeamEvent.Error -> {
                    Log.e(TAG, "Error: ${event.message}")
                    // Here you could show a Toast or Snackbar
                }
            }
        }
    }

    // Team creation dialog - Using new CreateTeamDialog component
    CreateTeamDialog(
        showDialog = showCreateTeamDialog,
        teamName = teamName,
        teamDescription = teamDescription,
        isLoading = uiState.isLoading,
        onDismiss = { showCreateTeamDialog = false },
        onTeamNameChange = { teamName = it },
        onTeamDescriptionChange = { teamDescription = it },
        onCreateTeam = { viewModel.createTeam(teamName, teamDescription) },
    )

    // Delete confirmation dialog - Using new ConfirmationDialog component
    DeleteTeamMemberDialog(
        showDialog = showDeleteConfirmation,
        userToDelete = userToDelete,
        onDismiss = {
            showDeleteConfirmation = false
            userToDelete = null
        },
        onConfirmDelete = {
            userToDelete?.let { viewModel.removeMember(it.id) }
            showDeleteConfirmation = false
            userToDelete = null
        },
    )

    // Team member invite dialog - Using new InviteTeamMemberDialog component
    InviteTeamMemberDialog(
        showDialog = uiState.isInviteDialogVisible,
        inviteEmail = inviteEmail,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onDismiss = { viewModel.hideInviteDialog() },
        onEmailChange = { inviteEmail = it },
        onInvite = { viewModel.inviteUserByEmail(inviteEmail) },
    )

    // Invitation confirmation dialog
    InvitationConfirmationDialog(
        showDialog = showInvitationConfirmation,
        action = pendingInvitationAction,
        isLoading = uiState.isLoading,
        onDismiss = {
            if (!uiState.isLoading) {
                showInvitationConfirmation = false
                pendingInvitationAction = null
            }
        },
        onConfirm = {
            // Keep dialog open while the request is processing; close on success events
            when (val action = pendingInvitationAction) {
                is InvitationAction.Accept -> viewModel.acceptInvitation(action.invitation.id)
                is InvitationAction.Decline -> viewModel.declineInvitation(action.invitation.id)
                null -> { /* Do nothing */ }
            }
        },
    )

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header
            Header(
                title = stringResource(R.string.teams_title),
                iconVector = Icons.Default.Person,
                hasBackButton = false,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Pending invitations section (show when user has no team)
            Log.d(TAG, "Pending invitations: ${uiState.pendingInvitations.size}, $pendingInvitationAction")
            if (uiState.pendingInvitations.isNotEmpty() && uiState.currentTeam == null) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.pending_invitations_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.pendingInvitations,
                            key = { it.id },
                        ) { invitation ->
                            TeamInvitationCard(
                                invitation = invitation,
                                onAccept = {
                                    pendingInvitationAction = InvitationAction.Accept(invitation)
                                    showInvitationConfirmation = true
                                },
                                onDecline = {
                                    pendingInvitationAction = InvitationAction.Decline(invitation)
                                    showInvitationConfirmation = true
                                },
                            )
                        }
                    }
                }
            }

            // Keep content visible even while loading to avoid blank flicker
            if (uiState.currentTeam == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.teams_icon),
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Text(
                        text =
                            stringResource(
                                if (!uiState.canCreateTeam) R.string.no_team_assigned else R.string.ready_for_new_team,
                            ),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text =
                            stringResource(
                                if (!uiState.canCreateTeam) {
                                    R.string.no_team_allowed_notification
                                } else {
                                    R.string.no_team_notification
                                },
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp),
                    )
                }
            } else {
                // show Team details
                Text(
                    text = stringResource(R.string.manage_team_and_invite),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.currentTeam?.name ?: stringResource(R.string.my_team),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        val currentTeamDescription = uiState.currentTeam?.description
                        if (currentTeamDescription?.isNotBlank() == true) {
                            Text(
                                text = currentTeamDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )
                        }

                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            // Header für Mitglieder
                            item {
                                Text(
                                    text = stringResource(R.string.team_members_count, uiState.teamMembers.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp, bottom = 8.dp),
                                )
                            }
                            // Mitgliederliste
                            items(uiState.teamMembers, key = { it.id }) { member ->
                                TeamMemberItem(
                                    member = member,
                                    isManager = member.id == uiState.currentTeam?.managerId,
                                    canRemoveMember = uiState.isTeamManager,
                                    onRemoveClick = {
                                        userToDelete = member
                                        showDeleteConfirmation = true
                                    },
                                )
                            }
                            // Ausgehende Einladungen
                            if (uiState.isTeamManager && uiState.teamPendingInvitations.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.outgoing_invitations_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp, bottom = 8.dp),
                                    )
                                }
                                items(uiState.teamPendingInvitations, key = { it.id }) { invitation ->
                                    OutgoingInvitationItem(
                                        invitation = invitation,
                                        onCancel = { viewModel.cancelTeamInvitation(invitation.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // Lightweight progress overlay (keeps content visible, avoids screen blanking)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        // Floating Action Button positioned at bottom right
        // show FAB only if the user has no team and is allowed to create one
        if (uiState.canCreateTeam && uiState.currentTeam == null) {
            ExtendedFloatingActionButton(
                onClick = { showCreateTeamDialog = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.create_team_button))
            }
        } else if (uiState.currentTeam?.managerId == uiState.currentUser?.id) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showInviteDialog() },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.team_member_invite_button)) },
                expanded = true,
            )
        }
    }
}

@Composable
private fun OutgoingInvitationItem(
    invitation: TeamInvitation,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = invitation.invitedUserDisplayName.ifBlank { invitation.invitedUserEmail },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.team_invitation_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel_invitation),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
