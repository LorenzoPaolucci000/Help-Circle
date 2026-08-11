package com.project.helpcircle.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.usecase.NicknameValidationResult

/** Entry point: hoists [NicknameSetupViewModel] state and hands it to the stateless content below. */
@Composable
fun NicknameSetupScreen(
    onNicknameSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NicknameSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNicknameSaved()
    }

    NicknameSetupContent(
        uiState = uiState,
        onNicknameChanged = viewModel::onNicknameChanged,
        onGenerateClicked = viewModel::onGenerateClicked,
        onContinueClicked = viewModel::onContinueClicked,
        modifier = modifier
    )
}

@Composable
private fun NicknameSetupContent(
    uiState: NicknameSetupUiState,
    onNicknameChanged: (String) -> Unit,
    onGenerateClicked: () -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Choose your circle name", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your name is only visible to your circle members — never to us",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = onNicknameChanged,
            singleLine = true,
            isError = uiState.validationResult != null && uiState.validationResult != NicknameValidationResult.Valid,
            supportingText = { validationMessage(uiState.validationResult)?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onGenerateClicked) {
            Text("Generate one for me")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinueClicked,
            enabled = uiState.validationResult == NicknameValidationResult.Valid && !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}

private fun validationMessage(result: NicknameValidationResult?): String? = when (result) {
    NicknameValidationResult.TooShort -> "At least 3 characters"
    NicknameValidationResult.TooLong -> "At most 20 characters"
    NicknameValidationResult.InvalidCharacters -> "Letters and numbers only"
    NicknameValidationResult.Valid, null -> null
}
