package com.project.helpcircle.presentation.community

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/** Entry point: hoists [JoinCommunityViewModel] state and hands it to the stateless content below. */
@Composable
fun JoinCommunityScreen(
    onJoined: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinCommunityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.hasJoined) {
        if (uiState.hasJoined) onJoined()
    }

    JoinCommunityContent(
        uiState = uiState,
        onCommunityIdChanged = viewModel::onCommunityIdChanged,
        onJoinClicked = viewModel::onJoinClicked,
        modifier = modifier
    )
}

@Composable
private fun JoinCommunityContent(
    uiState: JoinCommunityUiState,
    onCommunityIdChanged: (String) -> Unit,
    onJoinClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            value = uiState.communityId,
            onValueChange = onCommunityIdChanged,
            singleLine = true,
            enabled = !uiState.isJoining,
            isError = uiState.errorMessage != null,
            supportingText = { uiState.errorMessage?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onJoinClicked,
            enabled = uiState.communityId.isNotBlank() && !uiState.isJoining,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isJoining) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Join")
            }
        }
    }
}
