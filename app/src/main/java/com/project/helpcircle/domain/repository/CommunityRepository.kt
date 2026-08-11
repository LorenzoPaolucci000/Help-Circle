package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CommunityState
import kotlinx.coroutines.flow.Flow

/** Manages membership in and observation of a support community's [CommunityState]. */
interface CommunityRepository {
    fun observeCommunityState(communityId: String): Flow<CommunityState>
    suspend fun joinCommunity(communityId: String): CommunityState

    /** Creates a new community bearing the given invite code and joins the caller into it. */
    suspend fun createCommunity(inviteCode: String): CommunityState

    /** Joins the community whose invite code matches, or null if none does. */
    suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState?

    suspend fun reportCrisis(communityId: String)
    suspend fun reportRecovery(communityId: String)

    /** The community this device last joined, or null if it has never joined one. */
    suspend fun getActiveCommunityId(): String?

    /** How many members are currently in the given community. */
    suspend fun getMemberCount(communityId: String): Int
}
