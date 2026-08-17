package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityObservation
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SOLO_MEMBER_COUNT = 1

/** Streams live updates to a community's state, distinguishing solo membership from a populated community. */
class ObserveCommunityStateUseCase(
    private val communityRepository: CommunityRepository
) {
    operator fun invoke(communityId: String): Flow<CommunityObservation> =
        communityRepository.observeCommunityState(communityId).map { state ->
            if (state.members.size <= SOLO_MEMBER_COUNT) {
                CommunityObservation.SoloMode(state.communityId, state.inviteCode, state.name)
            } else {
                CommunityObservation.Populated(state)
            }
        }
}
