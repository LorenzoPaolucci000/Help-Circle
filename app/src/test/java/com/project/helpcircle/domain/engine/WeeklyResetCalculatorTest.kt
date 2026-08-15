package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val ZONE: ZoneId = ZoneId.of("UTC")

/** A guaranteed Sunday-23:59 boundary in [ZONE], derived rather than hardcoded to a specific calendar date. */
private val SUNDAY_NIGHT_MILLIS: Long = LocalDate.of(2026, 1, 1)
    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    .let { LocalDateTime.of(it, LocalTime.of(23, 59)) }
    .atZone(ZONE)
    .toInstant()
    .toEpochMilli()

private fun atUtc(date: LocalDate, hour: Int, minute: Int = 0): Long =
    LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(ZONE).toInstant().toEpochMilli()

class WeeklyResetCalculatorTest {

    @Test
    fun `boundary is the prior week's Sunday when still short of this week's`() {
        val now = SUNDAY_NIGHT_MILLIS - 60_000

        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(now, ZONE)

        assertEquals(SUNDAY_NIGHT_MILLIS - WeeklyResetCalculator.WEEK_DURATION_MILLIS, boundary)
    }

    @Test
    fun `boundary is exactly now when now is exactly Sunday 23-59`() {
        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(SUNDAY_NIGHT_MILLIS, ZONE)

        assertEquals(SUNDAY_NIGHT_MILLIS, boundary)
    }

    @Test
    fun `boundary stays this week's Sunday shortly after it passes`() {
        val now = SUNDAY_NIGHT_MILLIS + 60_000

        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(now, ZONE)

        assertEquals(SUNDAY_NIGHT_MILLIS, boundary)
    }

    @Test
    fun `boundary stays last Sunday through the following week`() {
        val mondayAfter = SUNDAY_NIGHT_MILLIS + 24 * 60 * 60 * 1000L

        val boundary = WeeklyResetCalculator.mostRecentResetBoundaryMillis(mondayAfter, ZONE)

        assertEquals(SUNDAY_NIGHT_MILLIS, boundary)
    }

    @Test
    fun `weekly summary spans exactly one week ending at the given boundary`() {
        val summary = WeeklyResetCalculator.buildWeeklySummary(
            weekEndEpochMillis = SUNDAY_NIGHT_MILLIS,
            agencyIndexDelta = 7,
            episodesThisWeek = emptyList(),
            zoneId = ZONE
        )

        assertEquals(SUNDAY_NIGHT_MILLIS - WeeklyResetCalculator.WEEK_DURATION_MILLIS, summary.weekStartEpochMillis)
        assertEquals(7, summary.agencyIndexDelta)
    }

    @Test
    fun `an empty week has no most-effective category or peak crisis hour`() {
        val summary = WeeklyResetCalculator.buildWeeklySummary(
            weekEndEpochMillis = SUNDAY_NIGHT_MILLIS,
            agencyIndexDelta = 0,
            episodesThisWeek = emptyList(),
            zoneId = ZONE
        )

        assertNull(summary.mostEffectiveInterventionCategory)
        assertNull(summary.peakCrisisHour)
    }

    @Test
    fun `most-effective category counts only effective interventions with a category`() {
        val day = LocalDate.of(2026, 1, 5)
        val episodes = listOf(
            CrisisEpisodeRecord(atUtc(day, 9), nudgeCategory = "Haptic", wasEffectiveIntervention = true),
            CrisisEpisodeRecord(atUtc(day, 10), nudgeCategory = "Haptic", wasEffectiveIntervention = true),
            CrisisEpisodeRecord(atUtc(day, 11), nudgeCategory = "Text", wasEffectiveIntervention = true),
            // Not effective: shouldn't count toward Text's tally.
            CrisisEpisodeRecord(atUtc(day, 12), nudgeCategory = "Text", wasEffectiveIntervention = false),
            // No nudge at all: shouldn't count toward anything.
            CrisisEpisodeRecord(atUtc(day, 13), nudgeCategory = null, wasEffectiveIntervention = false)
        )

        val summary = WeeklyResetCalculator.buildWeeklySummary(
            weekEndEpochMillis = SUNDAY_NIGHT_MILLIS,
            agencyIndexDelta = 0,
            episodesThisWeek = episodes,
            zoneId = ZONE
        )

        assertEquals("Haptic", summary.mostEffectiveInterventionCategory)
    }

    @Test
    fun `most recent week start is one week before the most recent boundary`() {
        val weekStart = WeeklyResetCalculator.mostRecentWeekStartEpochMillis(SUNDAY_NIGHT_MILLIS, ZONE)

        assertEquals(SUNDAY_NIGHT_MILLIS - WeeklyResetCalculator.WEEK_DURATION_MILLIS, weekStart)
    }

    @Test
    fun `a new snapshot should be recorded when none has ever been recorded`() {
        val shouldRecord = WeeklyResetCalculator.shouldRecordNewWeeklySnapshot(
            latestRecordedWeekStartEpochMillis = null,
            nowEpochMillis = SUNDAY_NIGHT_MILLIS,
            zoneId = ZONE
        )

        assertTrue(shouldRecord)
    }

    @Test
    fun `no new snapshot is needed when one was already recorded for the current week`() {
        val currentWeekStart = WeeklyResetCalculator.mostRecentWeekStartEpochMillis(SUNDAY_NIGHT_MILLIS, ZONE)

        val shouldRecord = WeeklyResetCalculator.shouldRecordNewWeeklySnapshot(
            latestRecordedWeekStartEpochMillis = currentWeekStart,
            nowEpochMillis = SUNDAY_NIGHT_MILLIS,
            zoneId = ZONE
        )

        assertEquals(false, shouldRecord)
    }

    @Test
    fun `a new snapshot is needed once a fresh boundary has passed since the last recorded one`() {
        val lastWeekStart = WeeklyResetCalculator.mostRecentWeekStartEpochMillis(SUNDAY_NIGHT_MILLIS, ZONE)
        val nextSundayNight = SUNDAY_NIGHT_MILLIS + WeeklyResetCalculator.WEEK_DURATION_MILLIS

        val shouldRecord = WeeklyResetCalculator.shouldRecordNewWeeklySnapshot(
            latestRecordedWeekStartEpochMillis = lastWeekStart,
            nowEpochMillis = nextSundayNight,
            zoneId = ZONE
        )

        assertTrue(shouldRecord)
    }

    @Test
    fun `peak crisis hour is the hour with the most episode starts, effective or not`() {
        val day = LocalDate.of(2026, 1, 5)
        val episodes = listOf(
            CrisisEpisodeRecord(atUtc(day, 22), nudgeCategory = null, wasEffectiveIntervention = false),
            CrisisEpisodeRecord(atUtc(day, 22, minute = 15), nudgeCategory = null, wasEffectiveIntervention = false),
            CrisisEpisodeRecord(atUtc(day, 9), nudgeCategory = null, wasEffectiveIntervention = false)
        )

        val summary = WeeklyResetCalculator.buildWeeklySummary(
            weekEndEpochMillis = SUNDAY_NIGHT_MILLIS,
            agencyIndexDelta = 0,
            episodesThisWeek = episodes,
            zoneId = ZONE
        )

        assertEquals(22, summary.peakCrisisHour)
    }
}
