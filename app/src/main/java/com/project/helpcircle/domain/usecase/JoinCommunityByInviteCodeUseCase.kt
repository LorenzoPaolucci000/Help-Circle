package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository

/** Looks up a community by its shared invite code and joins the caller into it, or returns null if no community has that code. */
class JoinCommunityByInviteCodeUseCase(
    private val communityRepository: CommunityRepository
) {
    suspend operator fun invoke(inviteCode: String): CommunityState? =
        communityRepository.joinCommunityByInviteCode(inviteCode.trim().uppercase())
}
