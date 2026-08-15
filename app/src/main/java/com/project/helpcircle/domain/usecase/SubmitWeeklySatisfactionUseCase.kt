package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.WeeklySatisfactionRepository

/**
 * Records the user's rating for the week in progress and shares it with their circle.
 *
 * The local write happens first and the shared copy second, deliberately: a failed publish (no
 * connectivity, rules rejection) then propagates to the caller for it to surface, while the user's
 * own record of how their week went is already safely stored. Re-submitting the same rating simply
 * overwrites the local row and re-attempts the publish, so a retry costs nothing and duplicates
 * nothing.
 *
 * Both writes are stamped with one single week value resolved here, so they can never disagree
 * about which week the rating belongs to even if a boundary passes mid-call.
 */
class SubmitWeeklySatisfactionUseCase(
    private val weeklySatisfactionRepository: WeeklySatisfactionRepository,
    private val communityRepository: CommunityRepository
) {
    suspend operator fun invoke(satisfaction: WeeklySatisfaction) {
        val currentWeekStart = WeeklyResetCalculator.currentWeekStartEpochMillis(System.currentTimeMillis())
        weeklySatisfactionRepository.submit(currentWeekStart, satisfaction)

        // No circle yet (still onboarding, or the user has left one): the rating stays purely local
        // until they join, rather than being an error.
        val communityId = communityRepository.getActiveCommunityId() ?: return
        communityRepository.publishSatisfaction(communityId, currentWeekStart, satisfaction)
    }
}
