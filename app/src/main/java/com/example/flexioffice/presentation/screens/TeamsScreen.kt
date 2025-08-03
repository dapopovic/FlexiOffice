package com.example.flexioffice.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.flexioffice.data.model.User
import com.example.flexioffice.presentation.TeamEvent
import com.example.flexioffice.presentation.TeamViewModel

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
                    // Die UI wird automatisch durch den Flow aktualisiert
                }
                is TeamEvent.Error -> {
                    Log.e(TAG, "Error: ${event.message}")
                    // Hier könnte man einen Toast oder Snackbar anzeigen
                }
            }
        }
    }

    // Team-Erstellungsdialog
    if (showCreateTeamDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTeamDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.primaryContainer,
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.create_team_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.create_team_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    // Team Name Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,
                            ),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = stringResource(R.string.team_name_content_desc),
                                )
                                Text(
                                    stringResource(R.string.team_name_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = teamName,
                                onValueChange = { teamName = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.team_name_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }

                    // Team Description Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,
                            ),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.team_description_content_desc),
                                )
                                Text(
                                    stringResource(R.string.team_description_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = teamDescription,
                                onValueChange = { teamDescription = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.team_description_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                shape = MaterialTheme.shapes.medium,
                            )
                        }
                    }

                    // Team Benefits Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(
                                    stringResource(R.string.team_benefits_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    stringResource(R.string.team_benefits_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                            .copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.createTeam(teamName, teamDescription) },
                        enabled = teamName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(R.string.create_team_button),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    TextButton(
                        onClick = { showCreateTeamDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.cancel)) }
                }
            },
            dismissButton = null,
        )
    }

    // Löschbestätigungsdialog
    if (showDeleteConfirmation && userToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                userToDelete = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer,
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.remove_team_member_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.remove_team_member_warning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    // User Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(userToDelete!!.name)
                                Text(
                                    stringResource(R.string.remove_team_member_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Warning Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp),
                            )
                            Column {
                                Text(
                                    stringResource(R.string.warning),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    stringResource(R.string.remove_team_member_lost_access),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            userToDelete?.let { viewModel.removeMember(it.id) }
                            showDeleteConfirmation = false
                            userToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(R.string.team_member_remove_button),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            userToDelete = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.cancel)) }
                }
            },
            dismissButton = null,
        )
    }

    // Team-Mitglied einladen dialog
    if (uiState.isInviteDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hideInviteDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.invite_team_member_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.invite_team_member_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    // Email Input Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme
                                        .surfaceContainerHigh,
                            ),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(R.string.email_content_desc),
                                )
                                Text(
                                    stringResource(R.string.email_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            OutlinedTextField(
                                value = inviteEmail,
                                onValueChange = { inviteEmail = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.email_placeholder),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint =
                                            MaterialTheme.colorScheme
                                                .onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }

                    // Team Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(
                                    stringResource(R.string.team_invite_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    stringResource(R.string.team_invite_info),
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                            .copy(alpha = 0.8f),
                                )
                            }
                        }
                    }

                    // Error Display
                    if (uiState.errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.errorContainer,
                                ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.error_content_desc),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = uiState.errorMessage!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.inviteUserByEmail(inviteEmail) },
                        enabled = inviteEmail.isNotBlank() && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Text(
                                if (uiState.isLoading) {
                                    stringResource(R.string.invite_sending)
                                } else {
                                    stringResource(R.string.invite_send_button)
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    TextButton(
                        onClick = { viewModel.hideInviteDialog() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.cancel)) }
                }
            },
            dismissButton = null,
        )
    }

    Scaffold(
        floatingActionButton = {
            // show FAB only if the user has no team and is allowed to create one
            if (uiState.canCreateTeam && uiState.currentTeam == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateTeamDialog = true },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_team_button))
                }
            } else if (uiState.currentTeam?.managerId == uiState.currentUser?.id) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showInviteDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.team_member_invite_button)) },
                    expanded = true,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.teams_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.currentTeam == null && !uiState.isLoading) {
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

                        Text(
                            text = stringResource(R.string.team_members_count, uiState.teamMembers.size),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        LazyColumn {
                            items(uiState.teamMembers) { member ->
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
                        }
                    }
                }
            }
        }
    }
}
