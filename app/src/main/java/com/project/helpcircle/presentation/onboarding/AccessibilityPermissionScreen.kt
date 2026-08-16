package com.project.helpcircle.presentation.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.presentation.common.MetaChip
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.SecondaryButton
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.StepProgressHeader
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing
import com.project.helpcircle.ui.theme.StatusColors

/**
 * Mandatory onboarding step between [SelectMonitoredAppsScreen] and joining/creating a circle: the
 * user must grant the accessibility permission that scroll detection depends on before continuing,
 * so it's never possible to finish onboarding into a dashboard that silently detects nothing.
 * Reuses [MonitoringStatusViewModel][com.project.helpcircle.presentation.common.MonitoringStatusViewModel]
 * rather than a dedicated one, since permission liveness is already tracked there for
 * [MonitoringDisabledBanner][com.project.helpcircle.presentation.common.MonitoringDisabledBanner]
 * and updates live via its `ContentObserver` while this screen sits in the background.
 *
 * The explanation is three single lines rather than three paragraphs. Asking for a permission this
 * broad needs to be answerable at a glance — a wall of reassuring text is the thing users scroll
 * past, and it also pushed the buttons off the bottom of the screen.
 */
@Composable
fun AccessibilityPermissionScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: com.project.helpcircle.presentation.common.MonitoringStatusViewModel = hiltViewModel()
) {
    val isMonitoringActive by viewModel.isMonitoringActive.collectAsState()
    val context = LocalContext.current

    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        StepProgressHeader(step = 4, totalSteps = ONBOARDING_STEP_COUNT, onBack = onBack)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Turn on monitoring",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Android needs your permission before the app can notice you scrolling.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        SectionCard(title = "What it can and can't see") {
            PermissionFact(Icons.Filled.Visibility, "Sees that a scroll happened")
            Spacer(modifier = Modifier.height(Spacing.md))
            PermissionFact(Icons.Filled.VisibilityOff, "Never reads what's on your screen")
            Spacer(modifier = Modifier.height(Spacing.md))
            PermissionFact(Icons.Filled.Lock, "Stays encrypted on this phone only")
        }

        if (isMonitoringActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = StatusColors.ok,
                    modifier = Modifier.size(Spacing.xl)
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                MetaChip(text = "Permission granted")
            }
        }

        if (isMonitoringActive) {
            PrimaryButton(text = "Continue", onClick = onContinue)
            SecondaryButton(
                text = "Open Accessibility settings again",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
        } else {
            PrimaryButton(
                text = "Open Accessibility settings",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
            // Kept visible but disabled rather than hidden, so the user can see that granting the
            // permission is what unblocks the flow.
            PrimaryButton(text = "Continue", onClick = onContinue, enabled = false)
        }
    }
}

/** One single-line fact about what the permission does, with the icon that stands for it. */
@Composable
private fun PermissionFact(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.rowIcon)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Spacing.xl)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
