package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.CommunityState
import kotlinx.coroutines.flow.Flow

/** Manages membership in and observation of a support community's [CommunityState]. */
interface CommunityRepository {
    fun observeCommunityState(communityId: String): Flow<CommunityState>
    suspend fun joinCommunity(communityId: String): CommunityState
    suspend fun reportCrisis(communityId: String)
}
