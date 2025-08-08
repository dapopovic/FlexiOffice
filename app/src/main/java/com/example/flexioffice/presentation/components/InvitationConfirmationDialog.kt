package com.example.flexioffice.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.R
import com.example.flexioffice.data.model.TeamInvitation

sealed class InvitationAction {
    data class Accept(
        val invitation: TeamInvitation,
    ) : InvitationAction()

    data class Decline(
        val invitation: TeamInvitation,
    ) : InvitationAction()
}

@Composable
fun InvitationConfirmationDialog(
    showDialog: Boolean,
    action: InvitationAction?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (action == null) return

    when (action) {
        is InvitationAction.Accept -> {
            ConfirmationDialog(
                showDialog = showDialog,
                type = ConfirmationDialogType.GeneralConfirm,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                isLoading = isLoading,
                title = stringResource(R.string.team_invitation_accept_title),
                message = stringResource(R.string.team_invitation_accept_message, action.invitation.teamName),
                itemName = null,
                additionalInfo = null,
            )
        }
        is InvitationAction.Decline -> {
            ConfirmationDialog(
                showDialog = showDialog,
                type = ConfirmationDialogType.GeneralConfirm,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                isLoading = isLoading,
                title = stringResource(R.string.team_invitation_decline_title),
                message = stringResource(R.string.team_invitation_decline_message, action.invitation.teamName),
                itemName = null,
                additionalInfo = null,
            )
        }
    }
}
