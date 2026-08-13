package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.DetectionConfig
import com.project.helpcircle.domain.engine.SystemFallbackEvaluator
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class EvaluateFallbackFakeCommunityRepository(
    private var activeCommunityId: String?,
    private var memberCount: Int
) : CommunityRepository {
    var getMemberCountCallCount = 0
        private set

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        MutableStateFlow(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))
    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun createCommunity(inviteCode: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int {
        getMemberCountCallCount++
        return memberCount
    }
}

private fun useCase(
    communityRepository: CommunityRepository,
    tracker: CrisisEpisodeTracker = CrisisEpisodeTracker()
) = EvaluateSystemFallbackUseCase(tracker, SystemFallbackEvaluator(tracker), communityRepository)

class EvaluateSystemFallbackUseCaseTest {

    @Test
    fun `a stable state never offers the fallback`() = runBlocking {
        val useCase = useCase(EvaluateFallbackFakeCommunityRepository(activeCommunityId = null, memberCount = 0))

        val offered = useCase(AgencyState.Stable, atEpochMillis = 0)

        assertFalse(offered)
    }

    @Test
    fun `no active community offers the fallback immediately`() = runBlocking {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val useCase = useCase(EvaluateFallbackFakeCommunityRepository(activeCommunityId = null, memberCount = 0), tracker)

        val offered = useCase(AgencyState.Crisis, atEpochMillis = 0)

        assertTrue(offered)
    }

    @Test
    fun `a solo community offers the fallback immediately`() = runBlocking {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val useCase = useCase(EvaluateFallbackFakeCommunityRepository(activeCommunityId = "comm-1", memberCount = 1), tracker)

        val offered = useCase(AgencyState.Crisis, atEpochMillis = 0)

        assertTrue(offered)
    }

    @Test
    fun `a populated community does not offer the fallback before the timeout`() = runBlocking {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val useCase = useCase(EvaluateFallbackFakeCommunityRepository(activeCommunityId = "comm-1", memberCount = 2), tracker)

        val offered = useCase(AgencyState.Crisis, atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS)

        assertFalse(offered)
    }

    @Test
    fun `a populated community offers the fallback once the timeout passes`() = runBlocking {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val useCase = useCase(EvaluateFallbackFakeCommunityRepository(activeCommunityId = "comm-1", memberCount = 2), tracker)

        val offered = useCase(AgencyState.Crisis, atEpochMillis = DetectionConfig.IGNORED_CRISIS_THRESHOLD_MS + 1)

        assertTrue(offered)
    }

    @Test
    fun `the member count is fetched at most once per crisis episode`() = runBlocking {
        val tracker = CrisisEpisodeTracker()
        tracker.onAgencyStateUpdated(AgencyState.Crisis, atEpochMillis = 0)
        val communityRepository = EvaluateFallbackFakeCommunityRepository(activeCommunityId = "comm-1", memberCount = 2)
        val useCase = useCase(communityRepository, tracker)

        useCase(AgencyState.Crisis, atEpochMillis = 0)
        useCase(AgencyState.Crisis, atEpochMillis = 1_000)
        useCase(AgencyState.Crisis, atEpochMillis = 2_000)

        assertEquals(1, communityRepository.getMemberCountCallCount)
    }
}
