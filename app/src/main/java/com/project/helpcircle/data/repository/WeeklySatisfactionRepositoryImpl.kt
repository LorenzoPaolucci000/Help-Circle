package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.WeeklySatisfactionDao
import com.project.helpcircle.data.local.entity.WeeklySatisfactionEntity
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.WeeklySatisfactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed [WeeklySatisfactionRepository]: this device's own ratings, stored encrypted and never synced from here. */
class WeeklySatisfactionRepositoryImpl @Inject constructor(
    private val weeklySatisfactionDao: WeeklySatisfactionDao
) : WeeklySatisfactionRepository {

    override fun satisfactionForWeek(weekStartEpochMillis: Long): Flow<WeeklySatisfaction?> =
        weeklySatisfactionDao.observeForWeek(weekStartEpochMillis).map { entity ->
            // A stored value that no longer maps to a known rating is treated as unrated rather
            // than crashing, the same way an unrecognised peer rating is dropped when read back
            // from the roster.
            entity?.satisfaction?.let { stored ->
                WeeklySatisfaction.entries.firstOrNull { it.name == stored }
            }
        }

    override suspend fun submit(weekStartEpochMillis: Long, satisfaction: WeeklySatisfaction) {
        weeklySatisfactionDao.upsert(WeeklySatisfactionEntity(weekStartEpochMillis, satisfaction.name))
    }
}
