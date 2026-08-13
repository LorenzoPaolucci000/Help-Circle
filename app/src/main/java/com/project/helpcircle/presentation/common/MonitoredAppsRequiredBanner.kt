package com.project.helpcircle.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Shared warning shown wherever the user needs at least one monitored app before proceeding —
 * joining/creating a circle, or the mandatory onboarding step — but currently has none. [actionLabel]/
 * [onActionClicked] are only supplied where there's somewhere else to navigate to fix it; the
 * onboarding step itself is already that place, so it just shows the message inline.
 */
@Composable
fun MonitoredAppsRequiredBanner(
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClicked: (() -> Unit)? = null
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "You need to monitor at least one app to join a circle",
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClicked != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onActionClicked, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}
