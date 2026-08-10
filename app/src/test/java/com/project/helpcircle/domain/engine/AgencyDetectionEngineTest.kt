package com.project.helpcircle.domain.engine

import com.project.helpcircle.domain.model.AgencyState
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class AgencyDetectionEngineTest {

    @Test
    fun `stays stable below the warning ratio`() {
        val engine = AgencyDetectionEngine(scrollThreshold = 10, warningRatio = 0.6)

        val state = engine.record(ScrollSignal(timestampMillis = 0))

        assertEquals(AgencyState.Stable, state)
    }

    @Test
    fun `enters warning once the ratio threshold is reached within the window`() {
        val engine = AgencyDetectionEngine(scrollThreshold = 10, warningRatio = 0.6)

        var state: AgencyState = AgencyState.Stable
        repeat(6) { i -> state = engine.record(ScrollSignal(timestampMillis = i * 100L)) }

        assertEquals(AgencyState.Warning, state)
    }

    @Test
    fun `enters crisis once the scroll threshold is reached within the window`() {
        val engine = AgencyDetectionEngine(scrollThreshold = 10, warningRatio = 0.6)

        var state: AgencyState = AgencyState.Stable
        repeat(10) { i -> state = engine.record(ScrollSignal(timestampMillis = i * 100L)) }

        assertEquals(AgencyState.Crisis, state)
    }

    @Test
    fun `drops back to stable once old signals fall outside the sliding window`() {
        val engine = AgencyDetectionEngine(
            windowSize = 1.seconds,
            scrollThreshold = 5,
            warningRatio = 0.6
        )
        repeat(5) { i -> engine.record(ScrollSignal(timestampMillis = i * 100L)) }

        val state = engine.record(ScrollSignal(timestampMillis = 5_000L))

        assertEquals(AgencyState.Stable, state)
    }
}
