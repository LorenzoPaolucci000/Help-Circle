package com.project.helpcircle.presentation.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.usecase.NicknameValidationResult
import com.project.helpcircle.presentation.common.MetaChip
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.StepProgressHeader
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

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
    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        StepProgressHeader(step = 2, totalSteps = ONBOARDING_STEP_COUNT)

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose a nickname",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "This is the only thing your circle ever sees about you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = onNicknameChanged,
            singleLine = true,
            shape = Shapes.field,
            label = { Text("Nickname") },
            placeholder = { Text("e.g. quiet-otter") },
            isError = uiState.validationResult != null && uiState.validationResult != NicknameValidationResult.Valid,
            supportingText = { validationMessage(uiState.validationResult)?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MetaChip(text = "Not linked to your real name")
            TextButton(onClick = onGenerateClicked) {
                Text("Generate one for me")
            }
        }

        PrimaryButton(
            text = "Continue",
            onClick = onContinueClicked,
            enabled = uiState.validationResult == NicknameValidationResult.Valid && !uiState.isSaving
        )
    }
}

private fun validationMessage(result: NicknameValidationResult?): String? = when (result) {
    NicknameValidationResult.TooShort -> "At least 3 characters"
    NicknameValidationResult.TooLong -> "At most 20 characters"
    NicknameValidationResult.InvalidCharacters -> "Letters and numbers only"
    NicknameValidationResult.Valid, null -> null
}
