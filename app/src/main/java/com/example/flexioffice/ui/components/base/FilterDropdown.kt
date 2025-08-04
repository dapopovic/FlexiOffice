package com.example.flexioffice.ui.components.base

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
import com.example.flexioffice.ui.theme.FlexiOfficeSpacing

/**
 * Generic filter dropdown component for consistent filtering UI
 */
@Composable
fun <T> FilterDropdown(
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T?) -> Unit,
    itemLabel: (T) -> String,
    allItemsLabel: String,
    modifier: Modifier = Modifier,
    showClearButton: Boolean = true,
    onClearFilters: (() -> Unit)? = null
) {
    var showDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FlexiOfficeSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter Chip
        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            FilterChip(
                onClick = { showDropdown = true },
                label = {
                    Text(
                        text = selectedItem?.let(itemLabel) ?: allItemsLabel
                    )
                },
                selected = selectedItem != null,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                // "All items" option
                DropdownMenuItem(
                    text = { Text(allItemsLabel) },
                    onClick = {
                        onItemSelected(null)
                        showDropdown = false
                    }
                )

                // Individual items
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemLabel(item)) },
                        onClick = {
                            onItemSelected(item)
                            showDropdown = false
                        }
                    )
                }
            }
        }

        // Clear Filters Button
        if (showClearButton && selectedItem != null && onClearFilters != null) {
            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.filters_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Multi-filter row component for multiple filter dropdowns
 */
@Composable
fun FilterRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FlexiOfficeSpacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}