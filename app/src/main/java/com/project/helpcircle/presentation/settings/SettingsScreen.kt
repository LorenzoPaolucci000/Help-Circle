package com.project.helpcircle.presentation.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.project.helpcircle.presentation.common.DestructiveButton
import com.project.helpcircle.presentation.common.EmptyState
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.ScreenHeader
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/** Entry point: hoists [SettingsViewModel] state and hands it to the stateless content below. */
@Composable
fun SettingsScreen(
    onLeftCommunity: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.hasLeftCommunity) {
        if (uiState.hasLeftCommunity) {
            viewModel.onLeftCommunityHandled()
            onLeftCommunity()
        }
    }
    SettingsContent(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onAppToggled = viewModel::onAppToggled,
        onSaveClicked = viewModel::onSaveClicked,
        onLeaveCommunityClicked = viewModel::onLeaveCommunityClicked,
        onLeaveCommunityDismissed = viewModel::onLeaveCommunityDismissed,
        onLeaveCommunityConfirmed = viewModel::onLeaveCommunityConfirmed,
        modifier = modifier
    )
}

/**
 * One scrolling column, like the other tabs, with the app list rendered as plain rows rather than a
 * `LazyColumn`.
 *
 * An earlier version pinned the header and actions and let the list scroll in a weighted region
 * between them. That collapsed on a real device: with the monitoring banner shown above the tab
 * content there was almost no room left, so the list rendered a few pixels tall and the leave button
 * was pushed off the bottom entirely. Since only social and video apps are monitorable at all, the
 * list is short by design and doesn't need its own scroll region.
 */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onAppToggled: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onLeaveCommunityClicked: () -> Unit,
    onLeaveCommunityDismissed: () -> Unit,
    onLeaveCommunityConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.showLeaveConfirmation) {
        LeaveCommunityDialog(
            uiState = uiState,
            onDismiss = onLeaveCommunityDismissed,
            onConfirm = onLeaveCommunityConfirmed
        )
    }
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        ScreenHeader(overline = "Your setup", title = "Monitored apps")

        MonitoringScopeCard(
            trackedCount = uiState.trackedCount,
            excludedCount = uiState.excludedCount
        )

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            singleLine = true,
            shape = Shapes.field,
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth()
        )

        SectionCard(
            title = "Apps",
            subtitle = "Only social and video apps can be monitored, so this list is short by design."
        ) {
            if (uiState.appsByCategory.isEmpty()) {
                EmptyState(
                    title = "No apps match",
                    detail = "Try a different search term."
                )
                return@SectionCard
            }
            AppCategory.entries.forEach { category ->
                val appsInCategory = uiState.appsByCategory[category].orEmpty()
                if (appsInCategory.isEmpty()) return@forEach
                CategoryHeader(
                    category = category,
                    trackedCount = appsInCategory.count { it.packageName in uiState.pendingMonitoredPackageNames },
                    totalCount = appsInCategory.size
                )
                appsInCategory.forEach { app ->
                    AppRow(
                        app = app,
                        isMonitored = app.packageName in uiState.pendingMonitoredPackageNames,
                        onToggled = { onAppToggled(app.packageName) }
                    )
                }
            }
        }

        uiState.saveMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (uiState.isSaving) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            PrimaryButton(text = "Save configuration", onClick = onSaveClicked)
        }
        DestructiveButton(text = "Leave this circle", onClick = onLeaveCommunityClicked)
    }
}

/** How much of the phone this app is watching, which is the one number this screen really decides. */
@Composable
private fun MonitoringScopeCard(trackedCount: Int, excludedCount: Int, modifier: Modifier = Modifier) {
    SectionCard(
        modifier = modifier,
        title = "Monitoring scope",
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = if (trackedCount == 1) "1 app" else "$trackedCount apps",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "actively tracked · $excludedCount excluded",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun CategoryHeader(
    category: AppCategory,
    trackedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = Spacing.md, bottom = Spacing.sm)) {
        Text(
            text = category.displayName().uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$trackedCount of $totalCount tracked",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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

/** Confirmation for the one irreversible action in the app. */
@Composable
private fun LeaveCommunityDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.card,
        title = { Text("Leave this circle?") },
        text = {
            Column {
                Text("You'll stop seeing this circle's members and it'll stop seeing you. This can't be undone.")
                uiState.leaveError?.let {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !uiState.isLeavingCommunity,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(if (uiState.leaveTimedOut) "Retry" else "Leave")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !uiState.isLeavingCommunity) {
                Text("Cancel")
            }
        }
    )
}

private fun AppCategory.displayName(): String = when (this) {
    AppCategory.SOCIAL -> "Social"
    AppCategory.VIDEO -> "Video"
    AppCategory.GAME -> "Games"
    AppCategory.NEWS -> "News"
    AppCategory.PRODUCTIVITY -> "Productivity"
    AppCategory.OTHER -> "Other"
}
