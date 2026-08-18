package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.PublishedStatusTracker
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.repository.CommunityRepository

/**
 * Shares this device's coarse status with its circle whenever the locally detected [AgencyState]
 * changes tier, so peers can see that somebody needs support.
 *
 * Until this existed nothing ever wrote anything but `OK`: the repository's crisis-reporting method
 * had no callers at all and `AT_RISK` was never written by anything, which left every roster entry
 * permanently green and meant `HelpablePeers` — which keeps only at-risk and crisis members — could
 * never return anyone, so no nudge could be sent.
 *
 * Only the tier crosses the wire, never the behavioural signal behind it, per the Zero-PII rule.
 * [PublishedStatusTracker] suppresses the write unless the tier actually changed; see its own doc
 * for why that matters on a path that runs per scroll event.
 */
class PublishAgencyStatusUseCase(
    private val communityRepository: CommunityRepository,
    private val publishedStatusTracker: PublishedStatusTracker
) {
    /**
     * Publishes the status matching [state] if it differs from the last one shared, and reports
     * whether this call is the moment the user *entered* crisis — the one transition that warrants
     * alerting peers, as opposed to merely updating their roster.
     */
    suspend operator fun invoke(state: AgencyState): Boolean {
        val status = state.toMemberStatus()
        if (publishedStatusTracker.isUnchanged(status)) return false
        val communityId = communityRepository.getActiveCommunityId()
        if (communityId == null) {
            // Nothing to tell, so record it as told. Without this the status would never settle
            // while the user belongs to no circle, and every scroll event would repeat the lookup
            // above — a database read on the hot detection path. Joining a circle re-marks or
            // clears this, so a real status can never end up suppressed.
            publishedStatusTracker.markPublished(status)
            return false
        }
        communityRepository.publishStatus(communityId, status)
        publishedStatusTracker.markPublished(status)
        return status == MemberStatus.CRISIS
    }
}

private fun AgencyState.toMemberStatus(): MemberStatus = when (this) {
    AgencyState.Crisis -> MemberStatus.CRISIS
    AgencyState.Warning -> MemberStatus.AT_RISK
    AgencyState.Stable -> MemberStatus.OK
}
