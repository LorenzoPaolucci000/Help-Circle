package com.project.helpcircle.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.helpcircle.ui.theme.Spacing

/**
 * The chrome shared by every onboarding step: an optional back arrow, a "Step N of M" label, and a
 * progress bar showing how far through the flow the user is.
 *
 * Onboarding is four screens long and each one previously gave no indication of where it sat in the
 * sequence, so a first-time user had no idea how much was left before they could use the app.
 */
@Composable
fun StepProgressHeader(
    step: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // The spacer keeps the label optically centred whether or not a back arrow is present,
            // so the header doesn't shift sideways between steps.
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Text(
                text = "Step $step of $totalSteps",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        StepProgressBar(step = step, totalSteps = totalSteps)
    }
}

@Composable
private fun StepProgressBar(step: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        FillBar(
            fraction = step.toFloat() / totalSteps.coerceAtLeast(1),
            height = 4.dp,
            minVisibleFraction = 0f
        )
    }
}
