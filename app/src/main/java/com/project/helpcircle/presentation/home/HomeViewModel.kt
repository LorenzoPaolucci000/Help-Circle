package com.project.helpcircle.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.usecase.ObserveAgencyHomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/** UI state for [HomeScreen]: this device's own IA_ind and weekly history — never a peer's. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val currentAgencyIndex: Int = AgencyIndex.baseline().value,
    val weeklyDeltasOldestFirst: List<Int> = emptyList(),
    val latestWeeklySummary: WeeklySummary? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAgencyHome: ObserveAgencyHomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeAgencyHome()
            .onEach { summary ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentAgencyIndex = summary.currentIndex.value,
                        weeklyDeltasOldestFirst = summary.weeklySummariesOldestFirst.map { weekly -> weekly.agencyIndexDelta },
                        latestWeeklySummary = summary.latestWeeklySummary
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
