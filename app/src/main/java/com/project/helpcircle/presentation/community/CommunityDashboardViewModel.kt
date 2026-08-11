package com.project.helpcircle.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.VisualLandscape
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveIncomingNudgesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for [CommunityDashboardScreen]; carries no member identities, only anonymous agency values. */
data class CommunityDashboardUiState(
    val isLoading: Boolean = true,
    val hasActiveCommunity: Boolean = true,
    val collectiveIndex: Int = 50,
    val visualLandscape: VisualLandscape = VisualLandscape.OVERCAST,
    val memberIndices: List<Int> = emptyList(),
    val latestNudge: Nudge? = null
)

@HiltViewModel
class CommunityDashboardViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val observeCommunityState: ObserveCommunityStateUseCase,
    private val observeIncomingNudges: ObserveIncomingNudgesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityDashboardUiState())
    val uiState: StateFlow<CommunityDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val communityId = communityRepository.getActiveCommunityId()
            if (communityId == null) {
                _uiState.update { it.copy(isLoading = false, hasActiveCommunity = false) }
                return@launch
            }
            observeCommunityState(communityId)
                .onEach { state ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasActiveCommunity = true,
                            collectiveIndex = state.collectiveIndex.value,
                            visualLandscape = state.visualLandscape,
                            memberIndices = state.memberAgencyIndices.map { index -> index.value }
                        )
                    }
                }
                .launchIn(viewModelScope)
        }

        observeIncomingNudges()
            .onEach { nudge -> _uiState.update { it.copy(latestNudge = nudge) } }
            .launchIn(viewModelScope)
    }
}
