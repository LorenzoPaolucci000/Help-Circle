package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.DetectionConfig
import com.project.helpcircle.domain.engine.ScrollSignal
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository

/** The doomscroll-risk reading a signal produced, plus whether the system's autonomous fallback prompt is now due. */
data class LossOfAgencyResult(
    val state: AgencyState,
    val offerSystemFallback: Boolean
)

/**
 * Feeds a scroll signal into [AgencyDetectionEngine], reports the resulting [AgencyState], and
 * lets [CrisisEpisodeTracker] award/penalize the IA_ind deltas that transition earns (spontaneous
 * recovery, an ignored crisis, or a crisis-episode's nudge outcome) via [CalculateAgencyIndexUseCase].
 * Whenever the tracker reports a closed episode, it's archived to [WeeklyHistoryRepository] for the
 * weekly summary's peak-crisis-hours/intervention-category stats. Also asks
 * [EvaluateSystemFallbackUseCase] whether the community is offline or unresponsive enough that the
 * system should autonomously offer its own break prompt, per the crisis-fallback spec. Finally,
 * hands the reading to [PublishAgencyStatusUseCase] so the circle sees the user's coarse status
 * change — the only step here that leaves the device, which is why it runs last.
 *
 * Before any of that, resolves any System Fallback break the user committed to via
 * [StartSystemFallbackBreakUseCase]: this is the single choke point every scroll signal already
 * passes through, so it's where real elapsed time against
 * [CrisisEpisodeTracker.pendingBreakStartedAtMillis] gets verified — tapping "take a break" alone
 * awards nothing; only a signal arriving here at least [DetectionConfig.SYSTEM_FALLBACK_BREAK_DURATION_MS]
 * later does. A signal arriving *before* that resets the pending break wholesale (no partial
 * credit), since renewed scrolling in a monitored app before the break is up means it wasn't taken.
 */
class DetectLossOfAgencyUseCase(
    private val agencyDetectionEngine: AgencyDetectionEngine,
    private val agencyRepository: AgencyRepository,
    private val crisisEpisodeTracker: CrisisEpisodeTracker,
    private val calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase,
    private val weeklyHistoryRepository: WeeklyHistoryRepository,
    private val evaluateSystemFallbackUseCase: EvaluateSystemFallbackUseCase,
    private val acknowledgeRecoveryUseCase: AcknowledgeRecoveryUseCase,
    private val communityRepository: CommunityRepository,
    private val publishAgencyStatusUseCase: PublishAgencyStatusUseCase
) {
    suspend operator fun invoke(signal: ScrollSignal): LossOfAgencyResult {
        resolvePendingBreak(signal.timestampMillis)
        val state = agencyDetectionEngine.record(signal)
        agencyRepository.reportAgencyState(state)
        val delta = crisisEpisodeTracker.onAgencyStateUpdated(state, signal.timestampMillis)
        if (!delta.isEmpty) {
            calculateAgencyIndexUseCase(delta.deltaAutonomy, delta.deltaSupport)
        }
        delta.closedEpisode?.let { episode ->
            weeklyHistoryRepository.recordCrisisEpisode(
                CrisisEpisodeRecord(episode.startedAtEpochMillis, episode.nudgeCategory, episode.wasEffectiveIntervention)
            )
        }
        val offerSystemFallback = evaluateSystemFallbackUseCase(state, signal.timestampMillis)
        // Last, and deliberately so: this is the only step here that touches the network, and
        // everything above it is local scoring that must still happen when the circle is
        // unreachable. Publishing earlier would let a Firestore failure skip the episode
        // bookkeeping and the fallback decision entirely.
        publishAgencyStatusUseCase(state)
        return LossOfAgencyResult(state, offerSystemFallback)
    }

    private suspend fun resolvePendingBreak(nowMillis: Long) {
        val startedAt = crisisEpisodeTracker.pendingBreakStartedAtMillis() ?: return
        if (nowMillis - startedAt >= DetectionConfig.SYSTEM_FALLBACK_BREAK_DURATION_MS) {
            acknowledgeRecoveryUseCase(nowMillis)
            calculateAgencyIndexUseCase(deltaAutonomy = DetectionConfig.ASSISTED_BREAK_COMPLETION_DELTA)
            agencyDetectionEngine.reset()
            communityRepository.getActiveCommunityId()?.let { communityRepository.reportRecovery(it) }
        } else {
            crisisEpisodeTracker.cancelPendingBreak()
        }
    }
}
