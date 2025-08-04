package com.example.flexioffice.ui.components.base

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.flexioffice.data.model.BookingStatus
import com.example.flexioffice.data.model.labelRes
import com.example.flexioffice.ui.theme.FlexiOfficeColors
import com.example.flexioffice.ui.theme.FlexiOfficeSpacing

/**
 * Reusable status badge component for consistent status display
 */
@Composable
fun StatusBadge(
    status: BookingStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        BookingStatus.APPROVED -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        BookingStatus.PENDING -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        BookingStatus.DECLINED -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        BookingStatus.CANCELLED -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
    ) {
        Text(
            text = stringResource(status.labelRes()),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(
                horizontal = FlexiOfficeSpacing.small,
                vertical = FlexiOfficeSpacing.extraSmall
            )
        )
    }
}