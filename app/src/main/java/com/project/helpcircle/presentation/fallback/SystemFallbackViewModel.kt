package com.project.helpcircle.presentation.fallback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.usecase.AcknowledgeRecoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SystemFallbackUiState(
    val isDismissed: Boolean = false
)

/**
 * Backs the System Fallback prompt the accessibility service launches when the community is
 * offline or hasn't responded to a crisis in time. Its two choices map onto the app's existing
 * recovery mechanics: taking the break closes out the crisis episode exactly like the "I'm back"
 * notification action does, while continuing just dismisses the prompt without touching detection
 * or scoring, since the user hasn't actually left the doomscroll loop.
 */
@HiltViewModel
class SystemFallbackViewModel @Inject constructor(
    private val acknowledgeRecoveryUseCase: AcknowledgeRecoveryUseCase,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemFallbackUiState())
    val uiState: StateFlow<SystemFallbackUiState> = _uiState.asStateFlow()

    fun onTakeBreakClicked() {
        viewModelScope.launch {
            acknowledgeRecoveryUseCase(System.currentTimeMillis())
            communityRepository.getActiveCommunityId()?.let { communityRepository.reportRecovery(it) }
            _uiState.update { it.copy(isDismissed = true) }
        }
    }

    fun onContinueClicked() {
        _uiState.update { it.copy(isDismissed = true) }
    }
}
