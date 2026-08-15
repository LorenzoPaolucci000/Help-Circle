package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.WeeklySatisfactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Records every submitted rating against the week it was stamped with, so tests can assert on both. */
private class FakeWeeklySatisfactionRepository : WeeklySatisfactionRepository {
    private val stored = MutableStateFlow<Map<Long, WeeklySatisfaction>>(emptyMap())
    val submittedWeeks = mutableListOf<Long>()

    override fun satisfactionForWeek(weekStartEpochMillis: Long): Flow<WeeklySatisfaction?> =
        stored.map { it[weekStartEpochMillis] }

    override suspend fun submit(weekStartEpochMillis: Long, satisfaction: WeeklySatisfaction) {
        submittedWeeks += weekStartEpochMillis
        stored.value = stored.value + (weekStartEpochMillis to satisfaction)
    }

    fun seed(weekStartEpochMillis: Long, satisfaction: WeeklySatisfaction) {
        stored.value = stored.value + (weekStartEpochMillis to satisfaction)
    }
}

private class SatisfactionFakeCommunityRepository(
    private val activeCommunityId: String? = "comm-1",
    private val throwOnPublish: Boolean = false
) : CommunityRepository {
    val publishedWeeks = mutableListOf<Long>()
    val publishedRatings = mutableListOf<WeeklySatisfaction>()

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        MutableStateFlow(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))

    override suspend fun joinCommunity(communityId: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false)

    override suspend fun createCommunity(communityId: String, inviteCode: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false)

    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit

    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) {
        if (throwOnPublish) throw RuntimeException("offline")
        publishedWeeks += weekStartEpochMillis
        publishedRatings += satisfaction
    }

    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = 0
}

private fun currentWeekStart(): Long =
    WeeklyResetCalculator.currentWeekStartEpochMillis(System.currentTimeMillis())

class ObserveWeeklySatisfactionUseCaseTest {

    @Test
    fun `an unrated week emits null`() = runBlocking {
        val useCase = ObserveWeeklySatisfactionUseCase(FakeWeeklySatisfactionRepository())

        assertNull(useCase().first())
    }

    @Test
    fun `the rating stored for the current week is emitted`() = runBlocking {
        val repository = FakeWeeklySatisfactionRepository()
        repository.seed(currentWeekStart(), WeeklySatisfaction.HAPPY)
        val useCase = ObserveWeeklySatisfactionUseCase(repository)

        assertEquals(WeeklySatisfaction.HAPPY, useCase().first())
    }

    @Test
    fun `a rating stored against a previous week is not surfaced as the current one`() = runBlocking {
        val repository = FakeWeeklySatisfactionRepository()
        repository.seed(currentWeekStart() - WeeklyResetCalculator.WEEK_DURATION_MILLIS, WeeklySatisfaction.HAPPY)
        val useCase = ObserveWeeklySatisfactionUseCase(repository)

        assertNull(useCase().first())
    }
}

class SubmitWeeklySatisfactionUseCaseTest {

    @Test
    fun `submitting stores the rating locally and publishes it to the circle`() = runBlocking {
        val satisfactionRepository = FakeWeeklySatisfactionRepository()
        val communityRepository = SatisfactionFakeCommunityRepository()
        val useCase = SubmitWeeklySatisfactionUseCase(satisfactionRepository, communityRepository)

        useCase(WeeklySatisfaction.NEUTRAL)

        assertEquals(WeeklySatisfaction.NEUTRAL, satisfactionRepository.satisfactionForWeek(currentWeekStart()).first())
        assertEquals(listOf(WeeklySatisfaction.NEUTRAL), communityRepository.publishedRatings)
    }

    @Test
    fun `the local copy and the shared copy are stamped with the same week`() = runBlocking {
        val satisfactionRepository = FakeWeeklySatisfactionRepository()
        val communityRepository = SatisfactionFakeCommunityRepository()
        val useCase = SubmitWeeklySatisfactionUseCase(satisfactionRepository, communityRepository)

        useCase(WeeklySatisfaction.BAD)

        assertEquals(1, satisfactionRepository.submittedWeeks.size)
        assertEquals(satisfactionRepository.submittedWeeks, communityRepository.publishedWeeks)
    }

    @Test
    fun `with no active circle the rating is stored locally and nothing is published`() = runBlocking {
        val satisfactionRepository = FakeWeeklySatisfactionRepository()
        val communityRepository = SatisfactionFakeCommunityRepository(activeCommunityId = null)
        val useCase = SubmitWeeklySatisfactionUseCase(satisfactionRepository, communityRepository)

        useCase(WeeklySatisfaction.HAPPY)

        assertEquals(WeeklySatisfaction.HAPPY, satisfactionRepository.satisfactionForWeek(currentWeekStart()).first())
        assertTrue(communityRepository.publishedRatings.isEmpty())
    }

    @Test
    fun `a failed publish still leaves the rating stored locally`() = runBlocking {
        val satisfactionRepository = FakeWeeklySatisfactionRepository()
        val communityRepository = SatisfactionFakeCommunityRepository(throwOnPublish = true)
        val useCase = SubmitWeeklySatisfactionUseCase(satisfactionRepository, communityRepository)

        try {
            useCase(WeeklySatisfaction.BAD)
            fail("expected the publish failure to propagate")
        } catch (e: RuntimeException) {
            assertEquals("offline", e.message)
        }

        // The local write happens first precisely so the user's own record survives this.
        assertEquals(WeeklySatisfaction.BAD, satisfactionRepository.satisfactionForWeek(currentWeekStart()).first())
    }

    @Test
    fun `re-submitting replaces the earlier answer rather than accumulating`() = runBlocking {
        val satisfactionRepository = FakeWeeklySatisfactionRepository()
        val communityRepository = SatisfactionFakeCommunityRepository()
        val useCase = SubmitWeeklySatisfactionUseCase(satisfactionRepository, communityRepository)

        useCase(WeeklySatisfaction.BAD)
        useCase(WeeklySatisfaction.HAPPY)

        assertEquals(WeeklySatisfaction.HAPPY, satisfactionRepository.satisfactionForWeek(currentWeekStart()).first())
        assertEquals(
            listOf(WeeklySatisfaction.BAD, WeeklySatisfaction.HAPPY),
            communityRepository.publishedRatings
        )
    }
}
