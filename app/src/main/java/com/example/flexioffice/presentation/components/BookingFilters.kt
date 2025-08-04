package com.example.flexioffice.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.R
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.labelRes
import com.example.flexioffice.ui.components.base.FilterDropdown

@Composable
fun BookingFilters(
    selectedStatus: BookingStatus?,
    showCancelledBookings: Boolean,
    onStatusFilterChange: (BookingStatus?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Available statuses based on showCancelledBookings
    val availableStatuses = if (showCancelledBookings) {
        BookingStatus.entries
    } else {
        BookingStatus.entries.filter { it != BookingStatus.CANCELLED }
    }

    FilterDropdown(
        items = availableStatuses,
        selectedItem = selectedStatus,
        onItemSelected = onStatusFilterChange,
        itemLabel = { status -> stringResource(status.labelRes()) },
        allItemsLabel = stringResource(R.string.filters_all_status),
        onClearFilters = onClearFilters,
        modifier = modifier
    )
}
