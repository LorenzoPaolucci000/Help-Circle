package com.project.helpcircle.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityObservation
import com.project.helpcircle.domain.model.CommunitySatisfaction
import com.project.helpcircle.domain.model.VisualLandscape
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityWeeklyTrendUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for [CommunityDashboardScreen]; each member carries only a pseudonym and coarse status,
 * never PII or a raw IA_ind score. Purely a read-only picture of how the circle is doing — sending
 * an intervention lives on the Help screen instead.
 */
data class CommunityDashboardUiState(
    val isLoading: Boolean = true,
    val hasActiveCommunity: Boolean = true,
    val isSolo: Boolean = false,
    val inviteCode: String = "",
    val collectiveIndex: Int = 50,
    val visualLandscape: VisualLandscape = VisualLandscape.MISTY,
    val members: List<CommunityMember> = emptyList(),
    val latestWeeklyCollectiveIndex: Int? = null,
    val previousWeeklyCollectiveIndex: Int? = null,
    /** How the circle collectively rates the week in progress; null while solo, since one member isn't a community mood. */
    val communitySatisfaction: CommunitySatisfaction? = null
)

@HiltViewModel
class CommunityDashboardViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val observeCommunityState: ObserveCommunityStateUseCase,
    private val observeCommunityWeeklyTrend: ObserveCommunityWeeklyTrendUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityDashboardUiState())
    val uiState: StateFlow<CommunityDashboardUiState> = _uiState.asStateFlow()

    // A Firestore listener's error can arrive right as this ViewModel starts being cleared (e.g.
    // this community's tab is kept alive in the background by the bottom nav's multi-back-stack
    // pattern while the user leaves via Settings, then the whole tab host gets torn down the
    // instant the leave completes) — a race the .catch{} guard below can't fully cover, since by
    // then there's no longer an active downstream collector for a late exception to be routed
    // through, so it would otherwise crash the app instead. This scope is still cancelled together
    // with viewModelScope (same parent Job), but its handler is the backstop that guarantees a
    // late/racy listener failure is dropped instead of ever reaching an uncaught state.
    private val listenerScope = CoroutineScope(
        SupervisorJob(viewModelScope.coroutineContext[Job]) + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, _ -> }
    )

    init {
        viewModelScope.launch {
            val communityId = communityRepository.getActiveCommunityId()
            if (communityId == null) {
                _uiState.update { it.copy(isLoading = false, hasActiveCommunity = false) }
                return@launch
            }
            observeCommunityState(communityId)
                // A Firestore listener failure (e.g. security rules rejecting the read) must not
                // crash the app; the dashboard just stops receiving live updates.
                .catch { _uiState.update { state -> state.copy(isLoading = false) } }
                .onEach { observation ->
                    when (observation) {
                        is CommunityObservation.Populated -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasActiveCommunity = true,
                                isSolo = false,
                                inviteCode = observation.state.inviteCode,
                                collectiveIndex = observation.state.collectiveIndex.value,
                                visualLandscape = observation.state.visualLandscape,
                                members = observation.state.members,
                                // Recomputed per emission rather than once at subscription, so the
                                // week it's matched against stays current for a long-lived listener.
                                communitySatisfaction = CommunitySatisfaction.from(
                                    members = observation.state.members,
                                    currentWeekStartEpochMillis = WeeklyResetCalculator
                                        .currentWeekStartEpochMillis(System.currentTimeMillis())
                                )
                            )
                        }
                        is CommunityObservation.SoloMode -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                hasActiveCommunity = true,
                                isSolo = true,
                                inviteCode = observation.inviteCode,
                                members = emptyList(),
                                communitySatisfaction = null
                            )
                        }
                    }
                }
                .launchIn(listenerScope)

            observeCommunityWeeklyTrend(communityId)
                .onEach { trend ->
                    _uiState.update {
                        it.copy(
                            latestWeeklyCollectiveIndex = trend.latest?.collectiveIndexValue,
                            previousWeeklyCollectiveIndex = trend.previous?.collectiveIndexValue
                        )
                    }
                }
                .launchIn(listenerScope)
        }
    }
}
