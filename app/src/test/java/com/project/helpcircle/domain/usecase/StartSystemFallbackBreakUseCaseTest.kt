package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.model.AgencyState
import org.junit.Assert.assertEquals
import org.junit.Test

class StartSystemFallbackBreakUseCaseTest {

    @Test
    fun `starting a break records its start time on the tracker without scoring anything`() {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val useCase = StartSystemFallbackBreakUseCase(tracker)

        useCase(atEpochMillis = 500)

        assertEquals(500L, tracker.pendingBreakStartedAtMillis())
    }
}
