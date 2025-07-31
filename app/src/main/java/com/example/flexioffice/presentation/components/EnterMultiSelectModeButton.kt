package com.example.flexioffice.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.flexioffice.R
import com.example.flexioffice.data.model.Booking

@Composable
fun EnterMultiSelectModeButton(
    isMultiselectMode: Boolean,
    isBookingListEmpty: Boolean,
    onToggleMultiSelectView: (booking: Booking?) -> Unit,
) {
    if (!isMultiselectMode && !isBookingListEmpty) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onToggleMultiSelectView(null) }) {
                Icon(
                    ImageVector.vectorResource(R.drawable.done_all_24px),
                    contentDescription = "Multi-Select View",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
