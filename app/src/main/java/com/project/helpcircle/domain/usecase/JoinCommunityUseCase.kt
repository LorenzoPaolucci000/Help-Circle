package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository

/** Joins the user into a support community and returns its current [CommunityState]. */
class JoinCommunityUseCase(
    private val communityRepository: CommunityRepository
) {
    suspend operator fun invoke(communityId: String): CommunityState =
        communityRepository.joinCommunity(communityId)
}
