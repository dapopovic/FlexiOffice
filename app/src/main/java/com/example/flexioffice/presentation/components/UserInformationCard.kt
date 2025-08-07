package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.User
import com.google.firebase.auth.FirebaseUser

@Composable
fun UserInformationCard(
    modifier: Modifier = Modifier,
    user: User? = null,
    firebaseUser: FirebaseUser? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.user_information),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            user?.let { userData ->
                // Team information
                UserInfoRow(
                    icon = ImageVector.vectorResource(R.drawable.group_24px),
                    label = stringResource(R.string.team_status),
                    value = when {
                        userData.teamId.isEmpty() -> stringResource(R.string.no_team_assigned_status)
                        userData.role == "manager" -> stringResource(R.string.team_manager)
                        else -> stringResource(R.string.team_member)
                    }
                )

                // Name
                UserInfoRow(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.name_label),
                    value = userData.name.ifEmpty { stringResource(R.string.not_specified) }
                )

                // Email
                UserInfoRow(
                    icon = Icons.Default.Email,
                    label = stringResource(R.string.email_address_label),
                    value = userData.email.ifEmpty { stringResource(R.string.not_specified) }
                )

                // Role
                UserInfoRow(
                    icon = ImageVector.vectorResource(id = R.drawable.work_24px),
                    label = stringResource(R.string.role_label),
                    value = when (userData.role) {
                        "admin" -> stringResource(R.string.role_admin)
                        "manager" -> stringResource(R.string.role_manager)
                        "user" -> stringResource(R.string.role_user)
                        else -> userData.role
                    }
                )
            } ?: run {
                // Fallback if user data is not available, show Firebase user
                firebaseUser?.let { fbUser ->
                    UserInfoRow(
                        icon = Icons.Default.Email,
                        label = stringResource(R.string.email_address_label),
                        value = fbUser.email ?: stringResource(R.string.not_available)
                    )
                }
            }
        }
    }
}
