package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.repository.NudgeRepository
import kotlinx.coroutines.flow.Flow

/** Streams nudges sent to this user by their community. */
class ObserveIncomingNudgesUseCase(
    private val nudgeRepository: NudgeRepository
) {
    operator fun invoke(): Flow<Nudge> = nudgeRepository.incomingNudges
}
