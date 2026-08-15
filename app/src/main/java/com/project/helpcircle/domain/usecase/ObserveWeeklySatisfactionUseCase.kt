package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.WeeklySatisfactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * This device's own rating for the week currently in progress, or null while it's unrated — what
 * the home screen uses to decide between showing the emoji picker and showing the chosen rating.
 *
 * Deliberately a separate stream from [ObserveAgencyHomeUseCase] rather than another field on
 * `AgencyHomeSummary`: a satisfaction rating is self-reported and has nothing to do with the
 * detected IA_ind or its weekly history, so folding it in would force any future consumer of one
 * to depend on the other.
 *
 * Which week counts as current is resolved once per collection, not on a timer — consistent with
 * the lazy, no-background-job approach the weekly reset and charge replenishment already take. A
 * screen left open across a Sunday 23:59 boundary keeps showing the finished week's rating until
 * it is next collected.
 */
class ObserveWeeklySatisfactionUseCase(
    private val weeklySatisfactionRepository: WeeklySatisfactionRepository
) {
    operator fun invoke(): Flow<WeeklySatisfaction?> = flow {
        val currentWeekStart = WeeklyResetCalculator.currentWeekStartEpochMillis(System.currentTimeMillis())
        emitAll(weeklySatisfactionRepository.satisfactionForWeek(currentWeekStart))
    }
}
