package com.project.helpcircle.presentation.fallback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
        onTakeBreakClicked = viewModel::onTakeBreakClicked,
        onContinueClicked = viewModel::onContinueClicked,
        modifier = modifier
    )
}

@Composable
private fun SystemFallbackContent(
    onTakeBreakClicked: () -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Your circle isn't around right now",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Would you like to take a short break?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onTakeBreakClicked, modifier = Modifier.fillMaxWidth()) {
                Text("Take a 2-minute break")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onContinueClicked, modifier = Modifier.fillMaxWidth()) {
                Text("I'm in control — continue")
            }
        }
    }
}
