package com.project.helpcircle.presentation.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.usecase.NudgeResult
import com.project.helpcircle.domain.usecase.ObserveChargeWalletUseCase
import com.project.helpcircle.domain.usecase.ObserveHelpablePeersUseCase
import com.project.helpcircle.domain.usecase.SendNudgeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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

/** UI state for [HelpScreen]: the peers this device can currently intervene for, and what it can afford to send them. */
data class HelpUiState(
    val isLoading: Boolean = true,
    val hasActiveCommunity: Boolean = true,
    /** Peers who are at risk or in crisis, most urgent first; never this device's own entry. */
    val peersNeedingHelp: List<CommunityMember> = emptyList(),
    /** How many peers the circle has at all, which is what separates "no peers yet" from "nobody needs help". */
    val totalPeerCount: Int = 0,
    val availableCharges: Int = ChargeWallet.MAX_CHARGES,
    val maxCharges: Int = ChargeWallet.MAX_CHARGES,
    val nudgeTarget: CommunityMember? = null,
    val isSendingNudge: Boolean = false,
    val nudgeFeedback: String? = null
)

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val observeChargeWallet: ObserveChargeWalletUseCase,
    private val observeHelpablePeers: ObserveHelpablePeersUseCase,
    private val sendNudge: SendNudgeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HelpUiState())
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    // A Firestore listener's error can arrive right as this ViewModel starts being cleared (e.g.
    // this tab is kept alive in the background by the bottom nav's multi-back-stack pattern while
    // the user leaves the circle via Settings, then the whole tab host gets torn down the instant
    // the leave completes) — a race the .catch{} guard below can't fully cover, since by then
    // there's no longer an active downstream collector for a late exception to be routed through,
    // so it would otherwise crash the app instead. This scope is still cancelled together with
    // viewModelScope (same parent Job), but its handler is the backstop that guarantees a late or
    // racy listener failure is dropped instead of ever reaching an uncaught state.
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
            observeHelpablePeers(communityId)
                // A listener failure (e.g. security rules rejecting the read) must not crash the
                // app; the screen just stops receiving live updates.
                .catch { _uiState.update { state -> state.copy(isLoading = false) } }
                .onEach { helpable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasActiveCommunity = true,
                            peersNeedingHelp = helpable.peers,
                            totalPeerCount = helpable.totalPeerCount,
                            // A peer who recovers while their picker is open is no longer a valid
                            // target, so the dialog closes rather than sending into a stale state.
                            nudgeTarget = it.nudgeTarget?.takeIf { target ->
                                helpable.peers.any { peer -> peer.anonymousId == target.anonymousId }
                            }
                        )
                    }
                }
                .launchIn(listenerScope)
        }

        observeChargeWallet()
            .onEach { wallet -> _uiState.update { it.copy(availableCharges = wallet.currentCharges) } }
            .launchIn(viewModelScope)
    }

    /** Opens the nudge-type picker for the tapped peer. */
    fun onMemberClicked(member: CommunityMember) {
        _uiState.update { it.copy(nudgeTarget = member) }
    }

    fun onNudgePickerDismissed() {
        _uiState.update { it.copy(nudgeTarget = null) }
    }

    fun onNudgeFeedbackShown() {
        _uiState.update { it.copy(nudgeFeedback = null) }
    }

    fun onNudgeSelected(nudge: Nudge) {
        val target = _uiState.value.nudgeTarget ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingNudge = true) }
            val communityId = communityRepository.getActiveCommunityId()
            if (communityId == null) {
                _uiState.update {
                    it.copy(isSendingNudge = false, nudgeTarget = null, nudgeFeedback = "No active circle")
                }
                return@launch
            }
            val feedback = try {
                when (val result = sendNudge(communityId, target.anonymousId, nudge)) {
                    is NudgeResult.Sent -> "Nudge sent to ${target.nickname}"
                    is NudgeResult.Error -> result.message
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                e.message ?: "Not enough charges"
            }
            _uiState.update { it.copy(isSendingNudge = false, nudgeTarget = null, nudgeFeedback = feedback) }
        }
    }
}
