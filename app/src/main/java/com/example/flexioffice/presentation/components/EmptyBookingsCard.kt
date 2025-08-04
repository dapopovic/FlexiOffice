package com.example.flexioffice.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.R
import com.example.flexioffice.ui.components.base.EmptyState

@Composable
fun EmptyBookingsCard(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.empty_bookings_title),
        subtitle = stringResource(R.string.empty_bookings_description),
        modifier = modifier
    )
}
