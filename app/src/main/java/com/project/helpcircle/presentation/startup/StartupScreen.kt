package com.project.helpcircle.presentation.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.R
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing

/**
 * Blocking splash while [StartupViewModel] resolves whether this launch needs onboarding or can
 * resume straight into the dashboard.
 *
 * Shows the logo rather than a bare spinner: this is the first thing every launch renders, and a
 * lone spinner on an empty screen reads as the app having failed to start.
 */
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

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_help_circle_logo),
                contentDescription = null,
                modifier = Modifier.size(Sizes.logo)
            )
            CircularProgressIndicator()
        }
    }
}
