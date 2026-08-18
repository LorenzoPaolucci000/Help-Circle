package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.WeeklySatisfaction
import kotlinx.coroutines.flow.Flow

/** Manages membership in and observation of a support community's [CommunityState]. */
interface CommunityRepository {
    fun observeCommunityState(communityId: String): Flow<CommunityState>
    suspend fun joinCommunity(communityId: String): CommunityState

    /**
     * Creates a new community with the given ID bearing the given invite code and [name], and joins
     * the caller into it. [communityId] is supplied by the caller (rather than generated here) so a
     * retried attempt can reuse the same ID as the one it's retrying — a timed-out create isn't
     * actually cancelled server-side, so without a stable ID a retry could leave an orphaned
     * duplicate community behind if the original attempt completes late in the background. [name]
     * must be stable across retries of the same logical create for the same reason, otherwise a
     * late-landing original and its retry would disagree on what the circle is called.
     *
     * The name is only ever written here: it can't be changed once the circle exists, since nothing
     * in the app decides who among a circle's members would be allowed to rename it.
     */
    suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState

    /** Joins the community whose invite code matches, or null if none does. */
    suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState?

    suspend fun reportCrisis(communityId: String)
    suspend fun reportRecovery(communityId: String)

    /**
     * Shares this device's coarse [MemberStatus] with its circle. Distinct from [reportCrisis] and
     * [reportRecovery], which mark episode milestones and also append an event: this is the
     * continuous tier signal the roster and the Help tab's peer list are built from, written on
     * every change of tier including the intermediate at-risk one that neither of those covers.
     */
    suspend fun publishStatus(communityId: String, status: MemberStatus)

    /**
     * Publishes this device's own self-reported satisfaction rating for the week starting at
     * [weekStartEpochMillis] to its roster entry, so peers can see it aggregated on the community
     * dashboard. The week stamp travels with the rating so a stale one is recognisable as stale
     * rather than silently counted toward a later week.
     */
    suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    )

    /** Removes this device's own roster entry from [communityId] and forgets it as the active community. */
    suspend fun leaveCommunity(communityId: String)

    /** The community this device last joined, or null if it has never joined one. */
    suspend fun getActiveCommunityId(): String?

    /** How many members are currently in the given community. */
    suspend fun getMemberCount(communityId: String): Int
}
