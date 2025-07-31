package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking

@Composable
fun BookingScreenHeader(
    showCancelledBookings: Boolean,
    onToggleCancelledBookings: () -> Unit,
    onToggleMultiSelectView: (booking: Booking?) -> Unit,
    isMultiselectMode: Boolean = false,
    isBookingListEmpty: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter =
                    painterResource(
                        R.drawable.schedule_24px,
                    ),
                contentDescription = "Anträge Icon",
                modifier = Modifier.padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Home Office Anträge",
                style = MaterialTheme.typography.headlineMedium,
            )
            EnterMultiSelectModeButton(
                isMultiselectMode = isMultiselectMode,
                isListEmpty = isBookingListEmpty,
                onEnterMultiSelectMode = onToggleMultiSelectView,
            )
        }
        Text(
            text = "Hier können Sie Ihre Home Office Anträge verwalten.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Stornierte Anträge anzeigen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = showCancelledBookings,
                onCheckedChange = { onToggleCancelledBookings() },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
