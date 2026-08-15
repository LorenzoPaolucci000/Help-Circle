package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.WeeklySatisfaction
import kotlinx.coroutines.flow.Flow

/**
 * This device's own self-reported [WeeklySatisfaction] ratings, one per week, stored locally.
 *
 * Both methods take the week explicitly rather than deriving "now" internally, so a caller that
 * submits a rating and then publishes it to the community stamps both with the exact same week —
 * a boundary crossing mid-operation can't leave the local copy and the shared copy disagreeing
 * about which week the rating belongs to.
 */
interface WeeklySatisfactionRepository {
    /** The rating stored for the week starting at [weekStartEpochMillis], or null if that week hasn't been rated. */
    fun satisfactionForWeek(weekStartEpochMillis: Long): Flow<WeeklySatisfaction?>

    /** Records (or replaces) this device's rating for the week starting at [weekStartEpochMillis]. */
    suspend fun submit(weekStartEpochMillis: Long, satisfaction: WeeklySatisfaction)
}
