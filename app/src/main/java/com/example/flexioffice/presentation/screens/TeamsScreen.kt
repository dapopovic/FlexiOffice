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
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flexioffice.data.model.User
import com.example.flexioffice.presentation.TeamEvent
import com.example.flexioffice.presentation.TeamViewModel

@Composable
fun TeamMemberItem(
    member: User,
    isManager: Boolean,
    canRemoveMember: Boolean = false,
    onRemoveClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
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
                        when {
                            isManager || member.role == User.ROLE_MANAGER -> "Manager"
                            else -> "Mitglied"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        
        if (canRemoveMember && member.role != User.ROLE_MANAGER) {
            IconButton(
                onClick = onRemoveClick
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Mitglied entfernen",
                    tint = MaterialTheme.colorScheme.error
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
                    Log.d("TeamsScreen", "Mitglied erfolgreich entfernt")
                    // Die UI wird automatisch durch den Flow aktualisiert
                }
                is TeamEvent.Error -> {
                    Log.e("TeamsScreen", "Fehler: ${event.message}")
                    // Hier könnte man einen Toast oder Snackbar anzeigen
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TeamEvent.TeamCreationSuccess -> {
                    showCreateTeamDialog = false
                    teamName = ""
                    teamDescription = ""
                    // Feedback anzeigen
                    Log.d("TeamsScreen", "Team erfolgreich erstellt")
                }
                is TeamEvent.InviteSuccess -> {
                    inviteEmail = ""
                    Log.d("TeamsScreen", "Benutzer erfolgreich eingeladen")
                }
                is TeamEvent.MemberRemoved -> {
                    Log.d("TeamsScreen", "Mitglied erfolgreich entfernt")
                }
                is TeamEvent.Error -> {
                    // Hier würde man normalerweise einen Snackbar oder Toast anzeigen
                    Log.e("TeamsScreen", event.message)
                }
            }
        }
    }

    // Team-Erstellungsdialog
    if (showCreateTeamDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTeamDialog = false },
            title = { Text("Neues Team erstellen") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Teamname") },
                        placeholder = { Text("z.B. Marketing Team") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = teamDescription,
                        onValueChange = { teamDescription = it },
                        label = { Text("Beschreibung") },
                        placeholder = { Text("Kurze Beschreibung des Teams") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createTeam(teamName, teamDescription)
                    },
                ) { Text("Erstellen") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateTeamDialog = false },
                ) { Text("Abbrechen") }
            },
        )
    }

    // Einladungsdialog
    if (uiState.isInviteDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hideInviteDialog() },
            title = { Text("Teammitglied einladen") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("E-Mail-Adresse") },
                        placeholder = { Text("beispiel@email.com") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                    )
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.inviteUserByEmail(inviteEmail) },
                    enabled = inviteEmail.isNotBlank() && !uiState.isLoading,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Einladen")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideInviteDialog() },
                ) { Text("Abbrechen") }
            },
        )
    }

    if (uiState.isLoading) {
        // Ladeanzeige anzeigen, wenn Daten geladen werden
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }
    Scaffold(
        floatingActionButton = {
            // FAB nur anzeigen, wenn der Benutzer kein Team hat und eines erstellen darf
            if (uiState.canCreateTeam && uiState.currentTeam == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateTeamDialog = true },
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Team erstellen")
                }
            } else if (uiState.currentTeam?.managerId == uiState.currentUser?.id) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showInviteDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Mitglied einladen") },
                    expanded = true,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    text = "Teams",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.currentTeam == null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "👥",
                        style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Text(
                        text =
                            if (!uiState.canCreateTeam) {
                                "Kein Team zugeordnet"
                            } else {
                                "Bereit für ein neues Team?"
                            },
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text =
                            if (!uiState.canCreateTeam) {
                                "Sie sind derzeit keinem Team zugeordnet und haben nicht die Berechtigung, ein Team zu erstellen."
                            } else {
                                "Sie sind derzeit keinem Team zugeordnet. Erstellen Sie ein neues Team und laden Sie Mitglieder ein."
                            },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp),
                    )
                }
            } else {
                // Teamdetails anzeigen
                Text(
                    text = "Verwalten Sie Ihr Team und laden Sie Mitglieder ein",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.currentTeam?.name ?: "Mein Team",
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
                            text = "Teammitglieder (${uiState.teamMembers.size})",
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
                                        viewModel.removeMember(member.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamMemberItem(
    member: User,
    isManager: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isManager || member.role == "manager") {
                Icons.Default.Star
            } else {
                Icons.Default.Person
            },
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
            tint =
                if (isManager || member.role == "manager") {
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
                    when {
                        isManager || member.role == "manager" -> "Manager"
                        else -> "Mitglied"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
