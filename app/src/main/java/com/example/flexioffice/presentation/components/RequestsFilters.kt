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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.User

@Composable
fun RequestsFilters(
    teamMembers: List<User>,
    selectedTeamMember: String?,
    onTeamMemberFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTeamMemberDropdown by remember { mutableStateOf(false) }

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
                        text =
                            selectedTeamMember?.let { userId ->
                                teamMembers.find { it.id == userId }?.name
                                    ?: stringResource(R.string.filters_team_member_fallback)
                            } ?: stringResource(R.string.filters_all_members),
                    )
                },
                selected = selectedTeamMember != null,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )

            DropdownMenu(
                expanded = showTeamMemberDropdown,
                onDismissRequest = { showTeamMemberDropdown = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.filters_all_members)) },
                    onClick = {
                        onTeamMemberFilterChange(null)
                        showTeamMemberDropdown = false
                    },
                )
                teamMembers.forEach { member ->
                    DropdownMenuItem(
                        text = { Text(member.name) },
                        onClick = {
                            onTeamMemberFilterChange(member.id)
                            showTeamMemberDropdown = false
                        },
                    )
                }
            }
        }

        // Clear Filters Button (nur anzeigen wenn Filter aktiv sind)
        if (selectedTeamMember != null) {
            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.filters_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
