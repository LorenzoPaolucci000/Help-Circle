package com.project.helpcircle.presentation.fallback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.SecondaryButton
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing

/** Entry point: hoists [SystemFallbackViewModel] state and hands it to the stateless content below. */
@Composable
fun SystemFallbackScreen(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SystemFallbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDismissed) {
        if (uiState.isDismissed) onDismissed()
    }

    SystemFallbackContent(
        isBreakStarted = uiState.isBreakStarted,
        onTakeBreakClicked = viewModel::onTakeBreakClicked,
        onContinueClicked = viewModel::onContinueClicked,
        modifier = modifier
    )
}

/**
 * The autonomous intervention shown when nobody in the circle is around to help.
 *
 * Deliberately small and calm: it renders in a floating dialog window rather than taking over the
 * screen, so it interrupts without seizing control — the opposite of a hard blocker. Once a break
 * is started it says so briefly and gets out of the way, since a live countdown would mean staring
 * at the phone, which is exactly what the break is for.
 */
@Composable
private fun SystemFallbackContent(
    isBreakStarted: Boolean,
    onTakeBreakClicked: () -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.heroCard,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.rowIcon + Spacing.lg)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SelfImprovement,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Sizes.rowIcon - Spacing.md)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.lg))

            if (isBreakStarted) {
                Text(
                    text = "Break started",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "See you in a couple of minutes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Your circle isn't around right now",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Would you like to take a short break?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xl))
                PrimaryButton(text = "Take a 2-minute break", onClick = onTakeBreakClicked)
                Spacer(modifier = Modifier.height(Spacing.sm))
                SecondaryButton(text = "I'm in control, continue", onClick = onContinueClicked)
            }
        }
    }
}
