package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.WeeklySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Pure week-boundary/summary math for the lazy weekly IA_ind reset (every Sunday 23:59 local
 * time). No repository access, so it's safely callable from any layer without a dependency-
 * direction concern — mirrors [ChargeWallet][com.project.helpcircle.domain.model.ChargeWallet]'s
 * own pure `replenished()` computation.
 */
object WeeklyResetCalculator {
    const val WEEK_DURATION_MILLIS = 7L * 24 * 60 * 60 * 1000

    /** The epoch millis of the most recent Sunday 23:59 local time at or before [nowEpochMillis]. */
    fun mostRecentResetBoundaryMillis(nowEpochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId)
        val thisWeekSundayNight = now
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            .withHour(23).withMinute(59).withSecond(0).withNano(0)
        val boundary = if (thisWeekSundayNight.isAfter(now)) thisWeekSundayNight.minusWeeks(1) else thisWeekSundayNight
        return boundary.toInstant().toEpochMilli()
    }

    /**
     * Builds the recap for the week ending at [weekEndEpochMillis]: [agencyIndexDelta] is this
     * device's own IA_ind trend across the week (see [WeeklySummary]'s own doc for why); the
     * most-effective-category and peak-crisis-hour stats are derived from [episodesThisWeek].
     */
    fun buildWeeklySummary(
        weekEndEpochMillis: Long,
        agencyIndexDelta: Int,
        episodesThisWeek: List<CrisisEpisodeRecord>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeeklySummary {
        val mostEffectiveCategory = episodesThisWeek
            .filter { it.wasEffectiveIntervention && it.nudgeCategory != null }
            .groupingBy { it.nudgeCategory }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        val peakCrisisHour = episodesThisWeek
            .groupingBy { Instant.ofEpochMilli(it.startedAtEpochMillis).atZone(zoneId).hour }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        return WeeklySummary(
            weekStartEpochMillis = weekEndEpochMillis - WEEK_DURATION_MILLIS,
            agencyIndexDelta = agencyIndexDelta,
            mostEffectiveInterventionCategory = mostEffectiveCategory,
            peakCrisisHour = peakCrisisHour
        )
    }
}
