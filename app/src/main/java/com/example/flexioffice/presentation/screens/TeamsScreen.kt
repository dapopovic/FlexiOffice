package com.example.flexioffice.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.flexioffice.data.model.User
import com.example.flexioffice.navigation.FlexiOfficeRoutes
import com.example.flexioffice.presentation.MainViewModel
import com.example.flexioffice.presentation.TeamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inviteEmail by remember { mutableStateOf("") }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    val currentUserId by viewModel.currentUserId.collectAsState()

    // Beobachte shouldRefreshUserData und aktualisiere MainViewModel entsprechend
    LaunchedEffect(uiState.shouldRefreshUserData) {
        if (uiState.shouldRefreshUserData) {
            mainViewModel.refreshUserData()
            viewModel.userDataRefreshed()
        }
    }

    // Team-Erstellungsdialog
    if (showCreateTeamDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTeamDialog = false },
            title = { Text("Neues Team erstellen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Teamname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = teamDescription,
                        onValueChange = { teamDescription = it },
                        label = { Text("Beschreibung") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createTeam(teamName, teamDescription)
                        showCreateTeamDialog = false
                        teamName = ""
                        teamDescription = ""
                    }
                ) {
                    Text("Erstellen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTeamDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Einladungsdialog
    if (uiState.isInviteDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hideInviteDialog() },
            title = { Text("Teammitglied einladen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("E-Mail-Adresse des Benutzers") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.inviteUserByEmail(inviteEmail)
                    },
                    enabled = inviteEmail.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Einladen")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideInviteDialog() }) {
                    Text("Abbrechen")
                }
            }
        )
    }


    Scaffold(
        floatingActionButton = {
            // FAB nur anzeigen, wenn der Benutzer kein Team hat und eines erstellen darf
            if (uiState.canCreateTeam && uiState.currentTeam == null) {
                FloatingActionButton(
                    onClick = { showCreateTeamDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Team erstellen")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Teams",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (uiState.currentTeam?.managerId == currentUserId) {
                    IconButton(onClick = { viewModel.showInviteDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Mitglied hinzufügen")
                    }
                }
            }

            if (uiState.currentTeam == null) {
                if (!uiState.canCreateTeam) {
                    // Benutzer hat kein Team und kann keins erstellen
                    Text(
                        text = "Sie sind derzeit keinem Team zugeordnet und haben nicht die Berechtigung, ein Team zu erstellen.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    // Benutzer kann ein Team erstellen, aber hat noch keins
                    Text(
                        text = "Sie sind derzeit keinem Team zugeordnet. Nutzen Sie den + Button, um ein neues Team zu erstellen.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Teamdetails anzeigen
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = uiState.currentTeam?.name ?: "Mein Team",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val teamDescription = uiState.currentTeam?.description
                        if (teamDescription?.isNotBlank() == true) {
                            Text(
                                text = teamDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        Text(
                            text = "Teammitglieder",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn {
                            items(uiState.teamMembers) { member ->
                                TeamMemberItem(
                                    member = member,
                                    isManager = member.name == uiState.currentTeam?.managerId
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
fun TeamMemberItem(member: User, isManager: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = when {
                    isManager || member.role == "manager" -> "Manager"
                    else -> "Mitglied"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
