package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrisisEpisodeTrackerTest {

    @Test
    fun `starting a crisis earns no delta yet`() {
        val tracker = CrisisEpisodeTracker()

        val delta = tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)

        assertEquals(AgencyDelta.NONE, delta)
    }

    @Test
    fun `spontaneous recovery within the window earns Delta_Autonomy plus 5`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)

        val delta = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = DetectionConfig.SPONTANEOUS_RECOVERY_WINDOW_MS
        )

        assertEquals(DetectionConfig.SPONTANEOUS_RECOVERY_DELTA, delta.deltaAutonomy)
        assertEquals(0, delta.deltaSupport)
    }

    @Test
    fun `recovering outside the spontaneous window and with no nudge earns nothing, not a penalty`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)

        val delta = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = DetectionConfig.SPONTANEOUS_RECOVERY_WINDOW_MS + 1
        )

        assertTrue(delta.isEmpty)
    }

    @Test
    fun `an ignored crisis past the threshold earns Delta_Autonomy minus 5, applied only once`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)

        val firstTick = tracker.onAgencyStateUpdated(
            AgencyState.Crisis,
            atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS + 1
        )
        val secondTick = tracker.onAgencyStateUpdated(
            AgencyState.Crisis,
            atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS + 1_000
        )

        assertEquals(DetectionConfig.IGNORED_CRISIS_DELTA, firstTick.deltaAutonomy)
        assertTrue(secondTick.isEmpty)
    }

    @Test
    fun `a nudge followed by recovery within the intervention window earns Delta_Support plus 10`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        tracker.onNudgeReceived(atEpochMillis = 5_000, category = "Haptic")

        val delta = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = 5_000 + DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS
        )

        assertEquals(DetectionConfig.EFFECTIVE_INTERVENTION_DELTA, delta.deltaSupport)
        assertEquals(true, delta.closedEpisode?.wasEffectiveIntervention)
    }

    @Test
    fun `a nudge with the crisis still ongoing past the intervention window earns Delta_Support plus 2, applied only once`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        tracker.onNudgeReceived(atEpochMillis = 5_000, category = "Haptic")

        val firstTick = tracker.onAgencyStateUpdated(
            AgencyState.Crisis,
            atEpochMillis = 5_000 + DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS + 1
        )
        val secondTick = tracker.onAgencyStateUpdated(
            AgencyState.Crisis,
            atEpochMillis = 5_000 + DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS + 2_000
        )

        assertEquals(DetectionConfig.ACTIVE_PRESENCE_DELTA, firstTick.deltaSupport)
        assertTrue(secondTick.isEmpty)
    }

    @Test
    fun `no peer responding is never penalized`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)

        val delta = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS - 1
        )

        assertEquals(0, delta.deltaAutonomy)
        assertEquals(0, delta.deltaSupport)
    }

    @Test
    fun `closing an episode records its start time and nudge category regardless of whether it earned a delta`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 1_000)
        tracker.onNudgeReceived(atEpochMillis = 2_000, category = "Text")

        val delta = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = 2_000 + DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS + 1
        )

        val closed = delta.closedEpisode
        assertNotNull(closed)
        assertEquals(1_000L, closed?.startedAtEpochMillis)
        assertEquals("Text", closed?.nudgeCategory)
        assertEquals(false, closed?.wasEffectiveIntervention)
    }

    @Test
    fun `only the first nudge of an episode is recorded`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        tracker.onNudgeReceived(atEpochMillis = 1_000, category = "Haptic")
        tracker.onNudgeReceived(atEpochMillis = 2_000, category = "ContentBlur")

        val delta = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = 1_000 + DetectionConfig.EFFECTIVE_INTERVENTION_WINDOW_MS
        )

        assertEquals("Haptic", delta.closedEpisode?.nudgeCategory)
    }

    @Test
    fun `starts a fresh episode after the previous one closes`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        tracker.onAgencyStateUpdated(AgencyState.Stable, atEpochMillis = 1_000)

        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 2_000)
        val secondEpisodeClose = tracker.onAgencyStateUpdated(
            AgencyState.Stable,
            atEpochMillis = 2_000 + DetectionConfig.SPONTANEOUS_RECOVERY_WINDOW_MS
        )

        assertEquals(DetectionConfig.SPONTANEOUS_RECOVERY_DELTA, secondEpisodeClose.deltaAutonomy)
        assertNull(secondEpisodeClose.closedEpisode?.nudgeCategory)
    }

    @Test
    fun `a warning reading outside a crisis earns nothing`() {
        val tracker = CrisisEpisodeTracker()

        val delta = tracker.onAgencyStateUpdated(AgencyState.Warning, atEpochMillis = 0)

        assertEquals(AgencyDelta.NONE, delta)
    }
}
