package com.project.helpcircle.presentation.community

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.presentation.common.MonitoredAppsRequiredBanner

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
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            MonitoredAppsRequiredBanner(
                actionLabel = "Go to Settings",
                onActionClicked = onGoToMonitoredApps
            )
        }
        return
    }
    Column(modifier = modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            Tab(
                selected = uiState.selectedTab == JoinCommunityTab.JOIN,
                onClick = { onTabSelected(JoinCommunityTab.JOIN) },
                text = { Text("Join") }
            )
            Tab(
                selected = uiState.selectedTab == JoinCommunityTab.CREATE,
                onClick = { onTabSelected(JoinCommunityTab.CREATE) },
                text = { Text("Create") }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            when (uiState.selectedTab) {
                JoinCommunityTab.JOIN -> JoinTabContent(uiState, onInviteCodeInputChanged, onJoinClicked)
                JoinCommunityTab.CREATE -> CreateTabContent(uiState, onCreateClicked, onContinueAfterCreateClicked)
            }
        }
    }
}

@Composable
private fun JoinTabContent(
    uiState: JoinCommunityUiState,
    onInviteCodeInputChanged: (String) -> Unit,
    onJoinClicked: () -> Unit
) {
    Text(text = "Join your circle", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Enter the circle code a member shared with you",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(24.dp))
    OutlinedTextField(
        value = uiState.inviteCodeInput,
        onValueChange = onInviteCodeInputChanged,
        singleLine = true,
        enabled = !uiState.isJoining,
        isError = uiState.joinError != null,
        supportingText = { uiState.joinError?.let { Text(it) } },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = onJoinClicked,
        enabled = uiState.inviteCodeInput.isNotBlank() && !uiState.isJoining,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (uiState.isJoining) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(if (uiState.joinTimedOut) "Retry" else "Join")
        }
    }
}

@Composable
private fun CreateTabContent(
    uiState: JoinCommunityUiState,
    onCreateClicked: () -> Unit,
    onContinueAfterCreateClicked: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val inviteCode = uiState.createdInviteCode

    if (inviteCode == null) {
        Text(text = "Create a circle", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start a new circle and invite friends to join",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        uiState.createError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(12.dp))
        }
        Button(
            onClick = onCreateClicked,
            enabled = !uiState.isCreating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (uiState.createTimedOut) "Retry" else "Create a circle")
            }
        }
    } else {
        Text(text = "Your circle is ready", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Share this code with peers you trust",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = inviteCode,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { clipboardManager.setText(AnnotatedString(inviteCode)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy code")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinueAfterCreateClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
