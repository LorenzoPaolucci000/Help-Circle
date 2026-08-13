package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.SystemFallbackEvaluator
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.repository.CommunityRepository

private const val SOLO_MEMBER_COUNT = 1

/**
 * Checks whether the active community is offline (no peers to notify) at most once per crisis
 * episode — never on every scroll signal, so this never adds a network round-trip to the
 * accessibility service's per-event hot path — then lets [SystemFallbackEvaluator] decide whether
 * the system's autonomous fallback prompt is due.
 */
class EvaluateSystemFallbackUseCase(
    private val crisisEpisodeTracker: CrisisEpisodeTracker,
    private val systemFallbackEvaluator: SystemFallbackEvaluator,
    private val communityRepository: CommunityRepository
) {
    private var cachedForEpisodeStartedAt: Long? = null
    private var cachedCommunityOffline = false

    suspend operator fun invoke(state: AgencyState, atEpochMillis: Long): Boolean {
        if (state != AgencyState.Crisis) return false
        val startedAt = crisisEpisodeTracker.currentCrisisStartedAtMillis() ?: return false

        if (cachedForEpisodeStartedAt != startedAt) {
            cachedForEpisodeStartedAt = startedAt
            val communityId = communityRepository.getActiveCommunityId()
            cachedCommunityOffline = communityId == null ||
                communityRepository.getMemberCount(communityId) <= SOLO_MEMBER_COUNT
        }

        return systemFallbackEvaluator.evaluate(state, atEpochMillis, cachedCommunityOffline)
    }
}
