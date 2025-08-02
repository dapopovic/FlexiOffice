package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.User

@Composable
fun CalendarFilters(
    teamMembers: List<User>,
    selectedTeamMember: String?,
    selectedStatus: BookingStatus?,
    onTeamMemberFilterChange: (String?) -> Unit,
    onStatusFilterChange: (BookingStatus?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTeamMemberDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }

    // Debug log
    android.util.Log.d("CalendarFilters", "teamMembers count: ${teamMembers.size}")
    teamMembers.forEach { member ->
        android.util.Log.d("CalendarFilters", "Team member: ${member.name} (${member.id})")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Team Member Filter
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            FilterChip(
                onClick = { showTeamMemberDropdown = true },
                label = {
                    Text(
                        text = selectedTeamMember?.let { userId ->
                            teamMembers.find { it.id == userId }?.name ?: "Teammitglied"
                        } ?: "Alle Mitglieder"
                    )
                },
                selected = selectedTeamMember != null,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            DropdownMenu(
                expanded = showTeamMemberDropdown,
                onDismissRequest = { showTeamMemberDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Alle Mitglieder") },
                    onClick = {
                        onTeamMemberFilterChange(null)
                        showTeamMemberDropdown = false
                    }
                )
                teamMembers.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.name) },
                        onClick = {
                            onTeamMemberFilterChange(member.id)
                            showTeamMemberDropdown = false
                        }
                    )
                }
            }
        }

        // Status Filter
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            FilterChip(
                onClick = { showStatusDropdown = true },
                label = {
                    Text(
                        text = selectedStatus?.let { getStatusDisplayName(it) } ?: "Alle Status"
                    )
                },
                selected = selectedStatus != null,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            DropdownMenu(
                expanded = showStatusDropdown,
                onDismissRequest = { showStatusDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Alle Status") },
                    onClick = {
                        onStatusFilterChange(null)
                        showStatusDropdown = false
                    }
                )
                BookingStatus.values().forEach { status ->
                    DropdownMenuItem(
                        text = { Text(getStatusDisplayName(status)) },
                        onClick = {
                            onStatusFilterChange(status)
                            showStatusDropdown = false
                        }
                    )
                }
            }
        }

        // Clear Filters Button (nur anzeigen wenn Filter aktiv sind)
        if (selectedTeamMember != null || selectedStatus != null) {
            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Filter zurücksetzen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getStatusDisplayName(status: BookingStatus): String {
    return when (status) {
        BookingStatus.PENDING -> "Ausstehend"
        BookingStatus.APPROVED -> "Genehmigt"
        BookingStatus.DECLINED -> "Abgelehnt"
        BookingStatus.CANCELLED -> "Storniert"
    }
}
