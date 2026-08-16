package com.project.helpcircle.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.presentation.common.MonitoredAppsRequiredBanner
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.SecondaryButton
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.StepProgressHeader
import com.project.helpcircle.presentation.onboarding.ONBOARDING_STEP_COUNT
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/** Entry point: hoists [JoinCommunityViewModel] state and hands it to the stateless content below. */
@Composable
fun JoinCommunityScreen(
    onJoined: () -> Unit,
    onGoToMonitoredApps: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinCommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.hasJoined) {
        if (uiState.hasJoined) onJoined()
    }

    JoinCommunityContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onInviteCodeInputChanged = viewModel::onInviteCodeInputChanged,
        onJoinClicked = viewModel::onJoinClicked,
        onCreateClicked = viewModel::onCreateClicked,
        onContinueAfterCreateClicked = viewModel::onContinueAfterCreateClicked,
        onGoToMonitoredApps = onGoToMonitoredApps,
        modifier = modifier
    )
}

/**
 * The last onboarding step. The two ways in are radio cards rather than a tab row: a tab row reads
 * as "two views of the same thing", whereas joining an existing circle and starting a new one are
 * genuinely different choices, only one of which the user will make.
 */
@Composable
private fun JoinCommunityContent(
    uiState: JoinCommunityUiState,
    onTabSelected: (JoinCommunityTab) -> Unit,
    onInviteCodeInputChanged: (String) -> Unit,
    onJoinClicked: () -> Unit,
    onCreateClicked: () -> Unit,
    onContinueAfterCreateClicked: () -> Unit,
    onGoToMonitoredApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!uiState.hasMonitoredApps) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(Spacing.xxl),
            contentAlignment = Alignment.Center
        ) {
            MonitoredAppsRequiredBanner(
                actionLabel = "Choose apps to monitor",
                onActionClicked = onGoToMonitoredApps
            )
        }
        return
    }

    // Once a circle exists there is nothing left to choose, so the options give way to the code.
    val createdInviteCode = uiState.createdInviteCode
    if (createdInviteCode != null) {
        CreatedCircleContent(
            inviteCode = createdInviteCode,
            onContinue = onContinueAfterCreateClicked,
            modifier = modifier
        )
        return
    }

    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        StepProgressHeader(step = 5, totalSteps = ONBOARDING_STEP_COUNT)

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Your circle",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Join a circle you were invited to, or start your own.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        CircleOptionCard(
            title = "Join with a code",
            detail = "Enter the 6-character code a member shared with you.",
            selected = uiState.selectedTab == JoinCommunityTab.JOIN,
            onClick = { onTabSelected(JoinCommunityTab.JOIN) }
        )
        CircleOptionCard(
            title = "Create a circle",
            detail = "Start a new one and invite people yourself.",
            selected = uiState.selectedTab == JoinCommunityTab.CREATE,
            onClick = { onTabSelected(JoinCommunityTab.CREATE) }
        )

        when (uiState.selectedTab) {
            JoinCommunityTab.JOIN -> {
                OutlinedTextField(
                    value = uiState.inviteCodeInput,
                    onValueChange = onInviteCodeInputChanged,
                    singleLine = true,
                    shape = Shapes.field,
                    enabled = !uiState.isJoining,
                    label = { Text("Invite code") },
                    isError = uiState.joinError != null,
                    supportingText = { uiState.joinError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.isJoining) {
                    LoadingRow()
                } else {
                    PrimaryButton(
                        text = if (uiState.joinTimedOut) "Retry" else "Join",
                        onClick = onJoinClicked,
                        enabled = uiState.inviteCodeInput.isNotBlank()
                    )
                }
            }

            JoinCommunityTab.CREATE -> {
                uiState.createError?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (uiState.isCreating) {
                    LoadingRow()
                } else {
                    PrimaryButton(
                        text = if (uiState.createTimedOut) "Retry" else "Create a circle",
                        onClick = onCreateClicked
                    )
                }
            }
        }
    }
}

/** One of the two ways into a circle, selectable as a whole card rather than by a small radio dot. */
@Composable
private fun CircleOptionCard(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, Shapes.card)
                } else {
                    Modifier
                }
            )
            .selectable(selected = selected, onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

/** Shown after a circle is created: the invite code is the only thing that matters here. */
@Composable
private fun CreatedCircleContent(
    inviteCode: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        StepProgressHeader(step = ONBOARDING_STEP_COUNT, totalSteps = ONBOARDING_STEP_COUNT)

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your circle is ready",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Share this code with people you trust.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        SectionCard(
            title = "Your invite code",
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = inviteCode,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            SecondaryButton(
                text = "Copy code",
                onClick = { clipboardManager.setText(AnnotatedString(inviteCode)) }
            )
        }

        PrimaryButton(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun LoadingRow(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(Spacing.xxl), strokeWidth = 2.dp)
    }
}
