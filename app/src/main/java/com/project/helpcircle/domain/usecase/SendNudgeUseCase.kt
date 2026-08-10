package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.repository.NudgeRepository

/** Consumes the required charges and dispatches a [Nudge] to a community member. */
class SendNudgeUseCase(
    private val nudgeRepository: NudgeRepository,
    private val consumeChargeUseCase: ConsumeChargeUseCase
) {
    suspend operator fun invoke(communityId: String, targetUserId: String, nudge: Nudge) {
        consumeChargeUseCase(nudge)
        nudgeRepository.sendNudge(communityId, targetUserId, nudge)
    }
}
