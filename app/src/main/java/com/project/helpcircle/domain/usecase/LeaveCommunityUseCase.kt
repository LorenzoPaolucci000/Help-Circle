package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.repository.CommunityRepository

/** Removes the user from a support community, freeing them to join or create another. */
class LeaveCommunityUseCase(
    private val communityRepository: CommunityRepository
) {
    suspend operator fun invoke(communityId: String) = communityRepository.leaveCommunity(communityId)
}
