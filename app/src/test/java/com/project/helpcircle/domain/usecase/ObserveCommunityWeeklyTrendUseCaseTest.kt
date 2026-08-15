package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityWeeklySummary
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class TrendFakeCommunityWeeklyHistoryRepository(
    initialSummaries: List<CommunityWeeklySummary>
) : CommunityWeeklyHistoryRepository {
    private val summariesFlow = MutableStateFlow(initialSummaries)
    override fun weeklySummaries(communityId: String): Flow<List<CommunityWeeklySummary>> = summariesFlow
    override suspend fun ensureWeeklySnapshotApplied(communityId: String, currentCollectiveIndexValue: Int) = Unit
}

class ObserveCommunityWeeklyTrendUseCaseTest {

    @Test
    fun `with no history both latest and previous are null`() = runBlocking {
        val useCase = ObserveCommunityWeeklyTrendUseCase(TrendFakeCommunityWeeklyHistoryRepository(emptyList()))

        val trend = useCase("comm-1").first()

        assertNull(trend.latest)
        assertNull(trend.previous)
    }

    @Test
    fun `with one recorded week only latest is set`() = runBlocking {
        val onlyWeek = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 1_000, collectiveIndexValue = 60)
        val useCase = ObserveCommunityWeeklyTrendUseCase(TrendFakeCommunityWeeklyHistoryRepository(listOf(onlyWeek)))

        val trend = useCase("comm-1").first()

        assertEquals(onlyWeek, trend.latest)
        assertNull(trend.previous)
    }

    @Test
    fun `latest and previous are picked correctly regardless of input order`() {
        val oldest = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 1_000, collectiveIndexValue = 55)
        val middle = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 2_000, collectiveIndexValue = 62)
        val newest = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 3_000, collectiveIndexValue = 70)
        val useCase = ObserveCommunityWeeklyTrendUseCase(
            TrendFakeCommunityWeeklyHistoryRepository(listOf(newest, oldest, middle))
        )

        val trend = runBlocking { useCase("comm-1").first() }

        assertEquals(newest, trend.latest)
        assertEquals(middle, trend.previous)
    }
}
