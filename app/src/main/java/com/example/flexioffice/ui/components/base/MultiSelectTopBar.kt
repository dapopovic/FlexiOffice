package com.example.flexioffice.ui.components.base

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.R

/**
 * Reusable multi-select TopBar component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectTopBar(
    selectedCount: Int,
    onExitMultiSelect: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = "$selectedCount ${stringResource(R.string.multi_select_selected_suffix)}"
            )
        },
        navigationIcon = {
            IconButton(onClick = onExitMultiSelect) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.multi_select_exit)
                )
            }
        },
        actions = { actions() },
        modifier = modifier
    )
}

/**
 * Action button for multi-select TopBar
 */
@Composable
fun MultiSelectAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}