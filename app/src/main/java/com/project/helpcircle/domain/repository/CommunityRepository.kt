package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CommunityState
import kotlinx.coroutines.flow.Flow

/** Manages membership in and observation of a support community's [CommunityState]. */
interface CommunityRepository {
    fun observeCommunityState(communityId: String): Flow<CommunityState>
    suspend fun joinCommunity(communityId: String): CommunityState

    /**
     * Creates a new community with the given ID bearing the given invite code, and joins the
     * caller into it. [communityId] is supplied by the caller (rather than generated here) so a
     * retried attempt can reuse the same ID as the one it's retrying — a timed-out create isn't
     * actually cancelled server-side, so without a stable ID a retry could leave an orphaned
     * duplicate community behind if the original attempt completes late in the background.
     */
    suspend fun createCommunity(communityId: String, inviteCode: String): CommunityState

    /** Joins the community whose invite code matches, or null if none does. */
    suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState?

    suspend fun reportCrisis(communityId: String)
    suspend fun reportRecovery(communityId: String)

    /** Removes this device's own roster entry from [communityId] and forgets it as the active community. */
    suspend fun leaveCommunity(communityId: String)

    /** The community this device last joined, or null if it has never joined one. */
    suspend fun getActiveCommunityId(): String?

    /** How many members are currently in the given community. */
    suspend fun getMemberCount(communityId: String): Int
}
