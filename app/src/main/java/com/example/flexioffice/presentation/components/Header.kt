package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking

@Composable
fun Header(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    iconVector: ImageVector? = null,
    iconDescription: String? = null,
    onBackPressed: () -> Unit = {},
    hasBackButton: Boolean = false,
    isMultiSelectMode: Boolean = false,
    doNotShowMultiSelectButton: Boolean = true,
    onEnterMultiSelectMode: (Booking?) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hasBackButton) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.geofencing_back_desc),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Icon(
            imageVector = iconVector ?: Icons.Default.Info,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            if (subtitle.isNullOrEmpty()) return@Column
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EnterMultiSelectModeButton(
            isMultiselectMode = isMultiSelectMode,
            onEnterMultiSelectMode = onEnterMultiSelectMode,
            isListEmpty = doNotShowMultiSelectButton,
        )
    }
}
