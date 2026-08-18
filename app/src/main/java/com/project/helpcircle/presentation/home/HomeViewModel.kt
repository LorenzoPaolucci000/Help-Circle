package com.project.helpcircle.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.usecase.ObserveAgencyHomeUseCase
import com.project.helpcircle.domain.usecase.ObserveWeeklySatisfactionUseCase
import com.project.helpcircle.domain.usecase.SubmitWeeklySatisfactionUseCase
import com.project.helpcircle.presentation.common.NETWORK_TIMEOUT_MILLIS
import com.project.helpcircle.presentation.common.SLOW_CONNECTION_MESSAGE
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** UI state for [HomeScreen]: this device's own IA_ind and weekly history — never a peer's. */
data class HomeUiState(
    val isLoading: Boolean = true,
    val currentAgencyIndex: Int = AgencyIndex.baseline().value,
    val weeklyDeltasOldestFirst: List<Int> = emptyList(),
    val latestWeeklySummary: WeeklySummary? = null,
    val previousWeeklySummary: WeeklySummary? = null,
    /** This device's own rating for the week in progress, or null while it hasn't been rated yet. */
    val currentWeekSatisfaction: WeeklySatisfaction? = null,
    val isSubmittingSatisfaction: Boolean = false,
    val satisfactionError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeAgencyHome: ObserveAgencyHomeUseCase,
    observeWeeklySatisfaction: ObserveWeeklySatisfactionUseCase,
    private val submitWeeklySatisfaction: SubmitWeeklySatisfactionUseCase
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
                        latestWeeklySummary = summary.latestWeeklySummary,
                        previousWeeklySummary = summary.previousWeeklySummary
                    )
                }
            }
            .launchIn(viewModelScope)

        // Kept a separate stream rather than merged into the agency summary: the rating is
        // self-reported and independent of the detected index, and it also updates on its own
        // whenever the user picks an emoji.
        observeWeeklySatisfaction()
            .onEach { satisfaction -> _uiState.update { it.copy(currentWeekSatisfaction = satisfaction) } }
            .launchIn(viewModelScope)
    }

    /**
     * Records the tapped rating. The local write inside the use case is what drives
     * [HomeUiState.currentWeekSatisfaction] back through the observed stream, so the choice shows
     * as selected even if sharing it with the circle then fails — the error only reports that the
     * peers' copy didn't land, and tapping again retries it.
     */
    fun onSatisfactionSelected(satisfaction: WeeklySatisfaction) {
        if (_uiState.value.isSubmittingSatisfaction) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingSatisfaction = true, satisfactionError = null) }
            val error = try {
                withTimeout(NETWORK_TIMEOUT_MILLIS) { submitWeeklySatisfaction(satisfaction) }
                null
            } catch (e: TimeoutCancellationException) {
                SLOW_CONNECTION_MESSAGE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Couldn't share this with your circle, tap to try again"
            }
            _uiState.update { it.copy(isSubmittingSatisfaction = false, satisfactionError = error) }
        }
    }
}
