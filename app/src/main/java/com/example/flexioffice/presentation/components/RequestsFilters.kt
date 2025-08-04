package com.example.flexioffice.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.R
import com.example.flexioffice.data.model.User
import com.example.flexioffice.ui.components.base.FilterDropdown

@Composable
fun RequestsFilters(
    teamMembers: List<User>,
    selectedTeamMember: String?,
    onTeamMemberFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterDropdown(
        items = teamMembers,
        selectedItem = teamMembers.find { it.id == selectedTeamMember },
        onItemSelected = { user -> onTeamMemberFilterChange(user?.id) },
        itemLabel = { user -> user.name },
        allItemsLabel = stringResource(R.string.filters_all_members),
        onClearFilters = onClearFilters,
        modifier = modifier
    )
}
