package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemFallbackEvaluatorTest {

    @Test
    fun `no crisis open never offers the fallback`() {
        val tracker = CrisisEpisodeTracker()
        val evaluator = SystemFallbackEvaluator(tracker)

        val offered = evaluator.evaluate(AgencyState.Stable, atEpochMillis = 0, communityOffline = true)

        assertFalse(offered)
    }

    @Test
    fun `an offline community offers the fallback immediately on crisis start`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val evaluator = SystemFallbackEvaluator(tracker)

        val offered = evaluator.evaluate(AgencyState.Crisis, atEpochMillis = 0, communityOffline = true)

        assertTrue(offered)
    }

    @Test
    fun `a populated community does not offer the fallback before the timeout`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val evaluator = SystemFallbackEvaluator(tracker)

        val offered = evaluator.evaluate(
            AgencyState.Crisis,
            atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS,
            communityOffline = false
        )

        assertFalse(offered)
    }

    @Test
    fun `a populated community with no peer response offers the fallback past the timeout`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val evaluator = SystemFallbackEvaluator(tracker)

        val offered = evaluator.evaluate(
            AgencyState.Crisis,
            atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS + 1,
            communityOffline = false
        )

        assertTrue(offered)
    }

    @Test
    fun `a nudge received before the timeout suppresses the fallback offer`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        tracker.onNudgeReceived(atEpochMillis = 1_000, category = "Text")
        val evaluator = SystemFallbackEvaluator(tracker)

        val offered = evaluator.evaluate(
            AgencyState.Crisis,
            atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS + 1,
            communityOffline = false
        )

        assertFalse(offered)
    }

    @Test
    fun `the fallback is only offered once per episode`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val evaluator = SystemFallbackEvaluator(tracker)

        val firstOffer = evaluator.evaluate(AgencyState.Crisis, atEpochMillis = 0, communityOffline = true)
        val secondOffer = evaluator.evaluate(AgencyState.Crisis, atEpochMillis = 100, communityOffline = true)

        assertTrue(firstOffer)
        assertFalse(secondOffer)
    }

    @Test
    fun `a new episode after one that offered a fallback can offer again`() {
        val tracker = CrisisEpisodeTracker()
        val evaluator = SystemFallbackEvaluator(tracker)
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        evaluator.evaluate(AgencyState.Crisis, atEpochMillis = 0, communityOffline = true)
        tracker.onAgencyStateUpdated(AgencyState.Stable, atEpochMillis = 1_000)

        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 2_000)
        val offeredAgain = evaluator.evaluate(AgencyState.Crisis, atEpochMillis = 2_000, communityOffline = true)

        assertTrue(offeredAgain)
    }
}
