package com.project.helpcircle.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.presentation.common.EmptyState
import com.project.helpcircle.presentation.common.MonitoredAppsRequiredBanner
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.StepProgressHeader
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/**
 * Mandatory onboarding step between nickname setup and joining/creating a circle: the user must
 * pick at least one app to monitor, since a circle with nothing being watched for doomscroll
 * detection would make the crisis-support loop pointless.
 */
@Composable
fun SelectMonitoredAppsScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelectMonitoredAppsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) {
            onContinue()
            viewModel.onDoneHandled()
        }
    }

    SelectMonitoredAppsContent(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onAppToggled = viewModel::onAppToggled,
        onContinueClicked = viewModel::onContinueClicked,
        modifier = modifier
    )
}

@Composable
private fun SelectMonitoredAppsContent(
    uiState: SelectMonitoredAppsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onAppToggled: (String) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        StepProgressHeader(step = 3, totalSteps = ONBOARDING_STEP_COUNT)

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Which apps to watch",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Pick at least one. Only these count toward detection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.showEmptySelectionError) {
            MonitoredAppsRequiredBanner(modifier = Modifier.fillMaxWidth())
        }

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            singleLine = true,
            shape = Shapes.field,
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth()
        )

        SectionCard(title = "Available apps") {
            if (uiState.appsByCategory.isEmpty()) {
                EmptyState(title = "No apps match", detail = "Try a different search term.")
                return@SectionCard
            }
            AppCategory.entries.forEach { category ->
                val appsInCategory = uiState.appsByCategory[category].orEmpty()
                if (appsInCategory.isEmpty()) return@forEach
                CategoryHeader(category)
                appsInCategory.forEach { app ->
                    AppRow(
                        app = app,
                        isMonitored = app.packageName in uiState.pendingMonitoredPackageNames,
                        onToggled = { onAppToggled(app.packageName) }
                    )
                }
            }
        }

        if (uiState.isSaving) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(Spacing.xxl), strokeWidth = Spacing.xs / 2)
            }
        } else {
            PrimaryButton(text = "Continue", onClick = onContinueClicked)
        }
    }
}

@Composable
private fun CategoryHeader(category: AppCategory, modifier: Modifier = Modifier) {
    Text(
        text = category.displayName().uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = Spacing.md, bottom = Spacing.xs)
    )
}

@Composable
private fun AppRow(app: AppInfo, isMonitored: Boolean, onToggled: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = app.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = isMonitored, onCheckedChange = { onToggled() })
    }
}

private fun AppCategory.displayName(): String = when (this) {
    AppCategory.SOCIAL -> "Social"
    AppCategory.VIDEO -> "Video"
    AppCategory.GAME -> "Games"
    AppCategory.NEWS -> "News"
    AppCategory.PRODUCTIVITY -> "Productivity"
    AppCategory.OTHER -> "Other"
}
