package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.NudgeRepository

private const val SOLO_MEMBER_COUNT = 1

/** The outcome of attempting to send a [Nudge]. */
sealed class NudgeResult {
    data object Sent : NudgeResult()
    data class Error(val message: String) : NudgeResult()
}

/** Consumes the required charges and dispatches a [Nudge] to a community member, if there are peers to receive it. */
class SendNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val communityRepository: CommunityRepository,
    private val consumeChargeUseCase: ConsumeChargeUseCase
) {
    suspend operator fun invoke(communityId: String, targetUserId: String, nudge: Nudge): NudgeResult {
        if (communityRepository.getMemberCount(communityId) <= SOLO_MEMBER_COUNT) {
            return NudgeResult.Error("No peers to notify")
        }
        consumeChargeUseCase(nudge)
        nudgeRepository.sendNudge(communityId, targetUserId, nudge)
        return NudgeResult.Sent
    }
}
