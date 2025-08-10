package com.example.flexioffice.presentation.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R

/**
 * Allgemeine Komponente für Bestätigungsdialoge
 * Unterstützt verschiedene Dialog-Typen mit einheitlichem Design
 */
@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    type: ConfirmationDialogType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean = false,
    title: String? = null,
    message: String? = null,
    itemName: String? = null,
    additionalInfo: String? = null,
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // Prevent dismiss while processing
                if (!isLoading) onDismiss()
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
                                containerColor = type.containerColor(),
                            ),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                            tint = type.onContainerColor(),
                        )
                    }
                    Column {
                        Text(
                            text = title ?: stringResource(type.defaultTitleRes),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(type.defaultSubtitleRes),
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
                    // Message text
                    if (message != null || type.defaultMessageRes != null) {
                        Text(
                            text = message ?: stringResource(type.defaultMessageRes!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Item info card (if item name provided)
                    if (itemName != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = type.itemIcon ?: type.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp),
                                )
                                Column {
                                    Text(
                                        text = itemName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (additionalInfo != null) {
                                        Text(
                                            text = additionalInfo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Warning card (for destructive actions)
                    if (type.isDestructive) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.warning),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text =
                                            stringResource(
                                                type.warningMessageRes ?: R.string.action_cannot_be_undone,
                                            ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onConfirm,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = type.buttonColor(),
                                contentColor = type.onButtonColor(),
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = type.onButtonColor(),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = type.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text =
                                    if (isLoading) {
                                        stringResource(R.string.processing)
                                    } else {
                                        stringResource(type.confirmButtonTextRes)
                                    },
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            dismissButton = null,
        )
    }
}

/**
 * Definiert die verschiedenen Typen von Bestätigungsdialogen
 */
sealed class ConfirmationDialogType(
    val icon: ImageVector,
    val itemIcon: ImageVector? = null,
    @StringRes val defaultTitleRes: Int,
    @StringRes val defaultSubtitleRes: Int,
    @StringRes val defaultMessageRes: Int? = null,
    @StringRes val confirmButtonTextRes: Int,
    @StringRes val warningMessageRes: Int? = null,
    val isDestructive: Boolean = false,
) {
    @Composable
    fun containerColor() =
        when {
            isDestructive -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.primaryContainer
        }

    @Composable
    fun onContainerColor() =
        when {
            isDestructive -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        }

    @Composable
    fun buttonColor() =
        when {
            isDestructive -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }

    @Composable
    fun onButtonColor() =
        when {
            isDestructive -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        }

    // Spezifische Dialog-Typen
    data object CancelBooking : ConfirmationDialogType(
        icon = Icons.Default.Delete,
        defaultTitleRes = R.string.cancel_booking_dialog_title,
        defaultSubtitleRes = R.string.cancel_booking_dialog_subtitle,
        defaultMessageRes = R.string.cancel_booking_dialog_message,
        confirmButtonTextRes = R.string.calendar_cancel_button,
        warningMessageRes = R.string.cancel_booking_warning,
        isDestructive = true,
    )

    data object DeleteTeamMember : ConfirmationDialogType(
        icon = Icons.Default.Delete,
        itemIcon = Icons.Default.Delete, // Person icon would be better
        defaultTitleRes = R.string.remove_team_member_title,
        defaultSubtitleRes = R.string.remove_team_member_warning,
        confirmButtonTextRes = R.string.team_member_remove_button,
        warningMessageRes = R.string.remove_team_member_lost_access,
        isDestructive = true,
    )

    data object GeneralDelete : ConfirmationDialogType(
        icon = Icons.Default.Delete,
        defaultTitleRes = R.string.delete_dialog_title,
        defaultSubtitleRes = R.string.delete_dialog_subtitle,
        defaultMessageRes = R.string.delete_dialog_message,
        confirmButtonTextRes = R.string.delete_button,
        warningMessageRes = R.string.action_cannot_be_undone,
        isDestructive = true,
    )

    data object CancelTeamInvitation : ConfirmationDialogType(
        icon = Icons.Default.Close,
        defaultTitleRes = R.string.cancel_invitation_dialog_title,
        defaultSubtitleRes = R.string.cancel_invitation_dialog_subtitle,
        defaultMessageRes = R.string.cancel_invitation_dialog_message,
        confirmButtonTextRes = R.string.cancel_invitation_confirm_button,
        warningMessageRes = R.string.cancel_invitation_warning,
        isDestructive = true,
    )

    data object GeneralConfirm : ConfirmationDialogType(
        icon = Icons.Default.Warning,
        defaultTitleRes = R.string.confirm_action_title,
        defaultSubtitleRes = R.string.confirm_action_subtitle,
        defaultMessageRes = R.string.confirm_action_message,
        confirmButtonTextRes = R.string.confirm_button,
        isDestructive = false,
    )
}
