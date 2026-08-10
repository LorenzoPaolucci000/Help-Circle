package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.repository.AgencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeAgencyRepository : AgencyRepository {
    private val indexFlow = MutableStateFlow(AgencyIndex.baseline())
    private val stateFlow = MutableStateFlow<AgencyState>(AgencyState.Stable)

    override val currentAgencyIndex = indexFlow
    override val currentAgencyState = stateFlow

    override suspend fun recordFocusSession(session: FocusSession) = Unit

    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        indexFlow.value = index
    }

    override suspend fun reportAgencyState(state: AgencyState) {
        stateFlow.value = state
    }
}

class CalculateAgencyIndexUseCaseTest {

    @Test
    fun `clamps the result to the 0 to 100 range`() = runBlocking {
        val useCase = CalculateAgencyIndexUseCase(FakeAgencyRepository())

        val overflow = useCase(deltaAutonomy = 200, deltaSupport = 0)
        assertEquals(AgencyIndex.MAX, overflow.value)

        val underflow = useCase(deltaAutonomy = -200, deltaSupport = 0)
        assertEquals(AgencyIndex.MIN, underflow.value)
    }

    @Test
    fun `applies the baseline formula for in-range deltas`() = runBlocking {
        val useCase = CalculateAgencyIndexUseCase(FakeAgencyRepository())

        val result = useCase(deltaAutonomy = 10, deltaSupport = 5)

        assertEquals(65, result.value)
    }
}
