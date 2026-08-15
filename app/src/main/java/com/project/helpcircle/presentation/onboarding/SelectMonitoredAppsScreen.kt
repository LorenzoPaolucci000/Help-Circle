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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.AppCategory
import com.project.helpcircle.domain.model.AppInfo
import com.project.helpcircle.presentation.common.MonitoredAppsRequiredBanner

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
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Choose apps to monitor", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pick at least one app you'd like HelpCircle to watch for doomscroll patterns.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            singleLine = true,
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.showEmptySelectionError) {
            MonitoredAppsRequiredBanner(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                AppCategory.entries.forEach { category ->
                    val appsInCategory = uiState.appsByCategory[category].orEmpty()
                    if (appsInCategory.isNotEmpty()) {
                        item(key = "header_${category.name}") { CategoryHeader(category) }
                        items(appsInCategory, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                isMonitored = app.packageName in uiState.pendingMonitoredPackageNames,
                                onToggled = { onAppToggled(app.packageName) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onContinueClicked,
            enabled = !uiState.isSaving && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun CategoryHeader(category: AppCategory, modifier: Modifier = Modifier) {
    Text(
        text = category.displayName(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun AppRow(app: AppInfo, isMonitored: Boolean, onToggled: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = app.displayName, style = MaterialTheme.typography.bodyMedium)
        Checkbox(checked = isMonitored, onCheckedChange = { onToggled() })
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
