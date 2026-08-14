package com.project.helpcircle.presentation.fallback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.usecase.StartSystemFallbackBreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemFallbackUiState(
    val isBreakStarted: Boolean = false,
    val isDismissed: Boolean = false
)

/**
 * Backs the System Fallback prompt the accessibility service launches when the community is
 * offline or hasn't responded to a crisis in time. Taking the break no longer scores anything at
 * tap time — it only records that the break started (via [StartSystemFallbackBreakUseCase]);
 * [com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase] is what verifies real elapsed
 * time and actually closes the episode later, once genuinely due. Continuing just dismisses the
 * prompt without touching detection or scoring, since the user hasn't actually left the doomscroll
 * loop.
 */
@HiltViewModel
class SystemFallbackViewModel @Inject constructor(
    private val startSystemFallbackBreakUseCase: StartSystemFallbackBreakUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemFallbackUiState())
    val uiState: StateFlow<SystemFallbackUiState> = _uiState.asStateFlow()

    fun onTakeBreakClicked() {
        startSystemFallbackBreakUseCase(System.currentTimeMillis())
        _uiState.update { it.copy(isBreakStarted = true) }
        viewModelScope.launch {
            // Purely cosmetic: gives the "Break started" confirmation a moment to be seen before
            // the dialog closes. Doesn't gate scoring in any way — that's resolved independently,
            // for real, whenever the next scroll signal arrives.
            delay(BREAK_CONFIRMATION_DISPLAY_MS)
            _uiState.update { it.copy(isDismissed = true) }
        }
    }

    fun onContinueClicked() {
        _uiState.update { it.copy(isDismissed = true) }
    }

    private companion object {
        const val BREAK_CONFIRMATION_DISPLAY_MS = 1_500L
    }
}
