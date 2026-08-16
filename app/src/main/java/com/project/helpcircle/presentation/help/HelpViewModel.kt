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
    /**
     * Who the next intervention would go to. Nothing can be sent until this is set, which is what
     * the screen renders the intervention buttons as unavailable to convey.
     */
    val selectedPeer: CommunityMember? = null,
    /** The multi-option intervention whose follow-up dialog is open, if any. */
    val optionPickerFor: InterventionType? = null,
    /**
     * How far each progressive intervention has been escalated for [selectedPeer]. Reset whenever
     * the selection changes, so escalation is always per-peer rather than a running global tally.
     */
    val sentLevels: Map<InterventionType, Int> = emptyMap(),
    val isSendingNudge: Boolean = false,
    val nudgeFeedback: String? = null
) {
    /** True once a peer is chosen, i.e. once the intervention buttons become usable. */
    val canIntervene: Boolean get() = selectedPeer != null

    /** How many presses of [type] have already landed for the selected peer. */
    fun sentLevelOf(type: InterventionType): Int = sentLevels[type] ?: 0

    /** False once a type has been pressed as many times as it can be for this peer. */
    fun hasRemaining(type: InterventionType): Boolean = sentLevelOf(type) < type.maxLevel
}

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
                        val stillSelectable = it.selectedPeer?.takeIf { target ->
                            helpable.peers.any { peer -> peer.anonymousId == target.anonymousId }
                        }
                        it.copy(
                            isLoading = false,
                            hasActiveCommunity = true,
                            peersNeedingHelp = helpable.peers,
                            totalPeerCount = helpable.totalPeerCount,
                            // A peer who recovers while they're selected is no longer a valid
                            // target, so the selection (and any open option dialog) is dropped
                            // rather than left pointing at a state the screen has just decided is
                            // no longer helpable.
                            selectedPeer = stillSelectable,
                            optionPickerFor = it.optionPickerFor.takeIf { _ -> stillSelectable != null }
                        )
                    }
                }
                .launchIn(listenerScope)
        }

        observeChargeWallet()
            .onEach { wallet -> _uiState.update { it.copy(availableCharges = wallet.currentCharges) } }
            .launchIn(viewModelScope)
    }

    /**
     * Chooses who an intervention would go to, which is what unlocks the intervention buttons.
     * Tapping the already-selected peer clears the selection, so a mis-tap is undoable without
     * having to send something.
     */
    fun onMemberClicked(member: CommunityMember) {
        _uiState.update {
            val alreadySelected = it.selectedPeer?.anonymousId == member.anonymousId
            it.copy(
                selectedPeer = if (alreadySelected) null else member,
                // Deselecting must not leave a dialog open over a peer who is no longer chosen.
                optionPickerFor = null,
                // Escalation is per-peer: switching target starts the grey-scale ramp over rather
                // than carrying the previous peer's intensity across to someone else.
                sentLevels = emptyMap()
            )
        }
    }

    /**
     * Handles a tap on one of the four intervention buttons. [InterventionType.TEXT] asks which
     * variant first; the rest send immediately, with progressive types sending the next level up
     * each time and charging again for it.
     */
    fun onInterventionClicked(type: InterventionType) {
        val state = _uiState.value
        if (state.selectedPeer == null || state.isSendingNudge) return
        val nudge = type.nudgeAfter(state.sentLevelOf(type))
        if (nudge == null) {
            // Either a type that asks instead of sending, or one already at full intensity. The
            // button is disabled in the exhausted case, so in practice this opens the dialog.
            if (type.options.isNotEmpty()) {
                _uiState.update { it.copy(optionPickerFor = type) }
            }
        } else {
            sendNudgeTo(state.selectedPeer, nudge, escalating = type)
        }
    }

    fun onOptionPickerDismissed() {
        _uiState.update { it.copy(optionPickerFor = null) }
    }

    fun onNudgeFeedbackShown() {
        _uiState.update { it.copy(nudgeFeedback = null) }
    }

    /** Sends the variant picked from a multi-option intervention's follow-up dialog. */
    fun onNudgeSelected(nudge: Nudge) {
        val state = _uiState.value
        val target = state.selectedPeer ?: return
        sendNudgeTo(target, nudge, escalating = state.optionPickerFor)
    }

    /**
     * @param escalating the button this came from, whose level counter advances only if the send
     *   actually lands — a rejected or unaffordable attempt must not consume a step of the ramp.
     */
    private fun sendNudgeTo(target: CommunityMember, nudge: Nudge, escalating: InterventionType?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingNudge = true) }
            val communityId = communityRepository.getActiveCommunityId()
            if (communityId == null) {
                _uiState.update {
                    it.copy(
                        isSendingNudge = false,
                        selectedPeer = null,
                        optionPickerFor = null,
                        nudgeFeedback = "No active circle"
                    )
                }
                return@launch
            }
            var landed = false
            val feedback = try {
                when (val result = sendNudge(communityId, target.anonymousId, nudge)) {
                    is NudgeResult.Sent -> {
                        landed = true
                        "Nudge sent to ${target.nickname}"
                    }
                    is NudgeResult.Error -> result.message
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IllegalStateException) {
                e.message ?: "Not enough charges"
            }
            // The peer stays selected: a progressive intervention is escalated by pressing the same
            // button again, which would be impossible if sending cleared the target.
            _uiState.update {
                it.copy(
                    isSendingNudge = false,
                    optionPickerFor = null,
                    sentLevels = if (landed && escalating != null) {
                        it.sentLevels + (escalating to (it.sentLevelOf(escalating) + 1))
                    } else {
                        it.sentLevels
                    },
                    nudgeFeedback = feedback
                )
            }
        }
    }
}
