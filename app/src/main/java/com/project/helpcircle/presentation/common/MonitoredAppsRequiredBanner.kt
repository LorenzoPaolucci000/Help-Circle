package com.project.helpcircle.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.project.helpcircle.ui.theme.Elevation
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/**
 * Shared warning shown wherever the user needs at least one monitored app before proceeding —
 * joining/creating a circle, or the mandatory onboarding step — but currently has none.
 * [actionLabel]/[onActionClicked] are only supplied where there's somewhere else to navigate to fix
 * it; the onboarding step itself is already that place, so it just shows the message inline.
 */
@Composable
fun MonitoredAppsRequiredBanner(
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClicked: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pick at least one app first",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "A circle can't help you if nothing is being watched.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClicked != null) {
                Spacer(modifier = Modifier.height(Spacing.lg))
                Button(
                    onClick = onActionClicked,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}
