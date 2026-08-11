package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.InviteCodeGenerator
import com.project.helpcircle.domain.repository.CommunityRepository

/** Creates a new community with a freshly generated invite code and joins the caller into it. */
class CreateCommunityUseCase(
    private val communityRepository: CommunityRepository
) {
    suspend operator fun invoke(): CommunityState =
        communityRepository.createCommunity(InviteCodeGenerator.generate())
}
