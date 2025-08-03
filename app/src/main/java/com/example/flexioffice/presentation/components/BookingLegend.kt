package com.example.flexioffice.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.flexioffice.data.model.BookingStatus

@Composable
fun BookingLegend(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Legende",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Status-Legende
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LegendItem(
                    icon = Icons.Default.Info,
                    color = Color(0xFFFF9800), // Orange
                    text = "Ausstehend",
                )
                LegendItem(
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50), // Green
                    text = "Genehmigt",
                )
                LegendItem(
                    icon = Icons.Default.Close,
                    color = Color(0xFFF44336), // Red
                    text = "Abgelehnt",
                )
                LegendItem(
                    icon = Icons.Default.Close,
                    color = Color(0xFF9E9E9E), // Grey
                    text = "Storniert",
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    icon: ImageVector,
    color: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Hilfsfunktion für Status-Farben
fun getStatusColor(status: BookingStatus): Color =
    when (status) {
        BookingStatus.PENDING -> Color(0xFFFF9800) // Orange
        BookingStatus.APPROVED -> Color(0xFF4CAF50) // Green
        BookingStatus.DECLINED -> Color(0xFFF44336) // Red
        BookingStatus.CANCELLED -> Color(0xFF9E9E9E) // Grey
    }

// Hilfsfunktion für Status-Icons
fun getStatusIcon(status: BookingStatus): ImageVector =
    when (status) {
        BookingStatus.PENDING -> Icons.Default.Info
        BookingStatus.APPROVED -> Icons.Default.CheckCircle
        BookingStatus.DECLINED -> Icons.Default.Close
        BookingStatus.CANCELLED -> Icons.Default.Close
    }
