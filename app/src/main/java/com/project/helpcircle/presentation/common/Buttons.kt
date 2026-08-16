package com.project.helpcircle.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/**
 * The full-width pill button that commits a screen's main action.
 *
 * Its disabled state is a muted tint of the same color rather than grey, so a button the user
 * can't press yet still reads as the thing they are working toward — which matters on the
 * onboarding screens, where "Continue" is disabled until a step is genuinely complete.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = Shapes.pill,
        contentPadding = PaddingValues(vertical = Spacing.lg),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
        )
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

/** The lower-emphasis pill, for an alternative action shown next to a [PrimaryButton]. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = Shapes.pill,
        contentPadding = PaddingValues(vertical = Spacing.lg)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}

/**
 * A destructive action, rendered in the scheme's error color. Used for the irreversible ones —
 * leaving a circle — which should never look like an ordinary button.
 */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = Shapes.pill,
        contentPadding = PaddingValues(vertical = Spacing.md),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleSmall)
    }
}
