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
    private var deltaAutonomy = 0
    private var deltaSupport = 0
    private var lastArchivedIndex: Int? = null
    private var lastWeeklyResetAtEpochMillis: Long? = null

    override val currentAgencyIndex = indexFlow
    override val currentAgencyState = stateFlow

    override suspend fun recordFocusSession(session: FocusSession) = Unit

    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        indexFlow.value = index
    }

    override suspend fun reportAgencyState(state: AgencyState) {
        stateFlow.value = state
    }

    override suspend fun adjustAgencyDeltas(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex {
        this.deltaAutonomy += deltaAutonomy
        this.deltaSupport += deltaSupport
        val index = AgencyIndex.calculate(this.deltaAutonomy, this.deltaSupport)
        indexFlow.value = index
        return index
    }

    override suspend fun getLastArchivedAgencyIndex(): Int? = lastArchivedIndex

    override suspend fun archiveAgencyIndex(agencyIndexValue: Int) {
        lastArchivedIndex = agencyIndexValue
    }

    override suspend fun getLastWeeklyResetAtEpochMillis(): Long? = lastWeeklyResetAtEpochMillis

    override suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long) {
        deltaAutonomy = 0
        deltaSupport = 0
        indexFlow.value = AgencyIndex.baseline()
        lastWeeklyResetAtEpochMillis = atEpochMillis
    }
}

class CalculateAgencyIndexUseCaseTest {

    @Test
    fun `clamps the result to the 0 to 100 range`() = runBlocking {
        val overflow = CalculateAgencyIndexUseCase(FakeAgencyRepository())(deltaAutonomy = 200, deltaSupport = 0)
        assertEquals(AgencyIndex.MAX, overflow.value)

        val underflow = CalculateAgencyIndexUseCase(FakeAgencyRepository())(deltaAutonomy = -200, deltaSupport = 0)
        assertEquals(AgencyIndex.MIN, underflow.value)
    }

    @Test
    fun `applies the baseline formula for in-range deltas`() = runBlocking {
        val result = CalculateAgencyIndexUseCase(FakeAgencyRepository())(deltaAutonomy = 10, deltaSupport = 5)

        assertEquals(65, result.value)
    }

    @Test
    fun `accumulates deltas across successive calls`() = runBlocking {
        val useCase = CalculateAgencyIndexUseCase(FakeAgencyRepository())
        useCase(deltaAutonomy = 10, deltaSupport = 0)

        val result = useCase(deltaAutonomy = 5, deltaSupport = 0)

        assertEquals(65, result.value)
    }
}
