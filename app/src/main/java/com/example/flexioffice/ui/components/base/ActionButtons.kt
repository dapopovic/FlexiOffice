package com.example.flexioffice.ui.components.base

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.flexioffice.ui.theme.FlexiOfficeSpacing

/**
 * Standardized action button components for consistent styling
 */

@Composable
fun FlexiOfficePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    iconPainter: Painter? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors()
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(FlexiOfficeSpacing.smallIconSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FlexiOfficeSpacing.small))
                    Text(text)
                }
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FlexiOfficeSpacing.smallIconSize)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FlexiOfficeSpacing.extraSmall))
                    Text(text)
                }
            }
            iconPainter != null -> {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(FlexiOfficeSpacing.smallIconSize)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FlexiOfficeSpacing.extraSmall))
                    Text(text)
                }
            }
            else -> {
                Text(text)
            }
        }
    }
}

@Composable
fun FlexiOfficeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    iconPainter: Painter? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(FlexiOfficeSpacing.smallIconSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FlexiOfficeSpacing.small))
                    Text(text)
                }
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FlexiOfficeSpacing.smallIconSize)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FlexiOfficeSpacing.extraSmall))
                    Text(text)
                }
            }
            iconPainter != null -> {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(FlexiOfficeSpacing.smallIconSize)
                )
                if (text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(FlexiOfficeSpacing.extraSmall))
                    Text(text)
                }
            }
            else -> {
                Text(text)
            }
        }
    }
}

@Composable
fun FlexiOfficeTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Text(text)
    }
}