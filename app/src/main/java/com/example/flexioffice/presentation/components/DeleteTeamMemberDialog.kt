package com.example.flexioffice.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.R
import com.example.flexioffice.data.model.User

@Composable
fun DeleteTeamMemberDialog(
    showDialog: Boolean,
    userToDelete: User?,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    ConfirmationDialog(
        showDialog = showDialog,
        type = ConfirmationDialogType.DeleteTeamMember,
        onDismiss = onDismiss,
        onConfirm = onConfirmDelete,
        isLoading = isLoading,
        itemName = userToDelete?.name,
        additionalInfo = stringResource(R.string.remove_team_member_info)
    )
}
