package com.project.helpcircle.presentation.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.presentation.common.MonitoringStatusViewModel

/**
 * Mandatory onboarding step between [SelectMonitoredAppsScreen] and joining/creating a circle: the
 * user must grant the accessibility permission that scroll detection depends on before continuing,
 * so it's never possible to finish onboarding into a dashboard that silently detects nothing.
 * Reuses [MonitoringStatusViewModel] rather than a dedicated one, since permission liveness is
 * already tracked there for [com.project.helpcircle.presentation.common.MonitoringDisabledBanner]
 * and updates live via its `ContentObserver` while this screen is sitting in the background.
 */
@Composable
fun AccessibilityPermissionScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MonitoringStatusViewModel = hiltViewModel()
) {
    val isMonitoringActive by viewModel.isMonitoringActive.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = "Turn on monitoring", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "HelpCircle needs Android's Accessibility permission to notice scroll " +
                "activity in the apps you just selected — that's how it detects a doomscroll " +
                "session starting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "It never reads what's on your screen — only that a scroll happened, " +
                "never the content, contacts, or messages behind it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "That scroll activity is encrypted and stored only on this phone. It's " +
                "never uploaded anywhere, so nobody — not your circle, not us — can ever see it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        if (isMonitoringActive) {
            Text(
                text = "Permission granted",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isMonitoringActive) "Open Accessibility settings again" else "Open Accessibility settings")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onContinue,
            enabled = isMonitoringActive,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
