package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow

/** Streams live updates to a community's [CommunityState]. */
class ObserveCommunityStateUseCase(
    private val communityRepository: CommunityRepository
) {
    operator fun invoke(communityId: String): Flow<CommunityState> =
        communityRepository.observeCommunityState(communityId)
}
