package com.project.helpcircle.presentation.startup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/** Blocking splash while [StartupViewModel] resolves whether this launch needs onboarding or can resume straight into the dashboard. */
@Composable
fun StartupScreen(
    onDestinationResolved: (StartupDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StartupViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        destination?.let(onDestinationResolved)
    }

    Box(modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}
