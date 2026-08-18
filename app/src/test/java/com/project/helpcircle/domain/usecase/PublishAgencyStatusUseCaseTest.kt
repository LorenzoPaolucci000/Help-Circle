package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.PublishedStatusTracker
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class PublishStatusFakeCommunityRepository(
    private val activeCommunityId: String? = "comm-1",
    private val throwOnPublish: Boolean = false
) : CommunityRepository {
    val published = mutableListOf<Pair<String, MemberStatus>>()

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        MutableStateFlow(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))

    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState =
        throw UnsupportedOperationException()

    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit

    override suspend fun publishStatus(communityId: String, status: MemberStatus) {
        if (throwOnPublish) throw RuntimeException("offline")
        published += communityId to status
    }

    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit

    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun ensureAlertSubscription() = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = 2
}

class PublishAgencyStatusUseCaseTest {

    private val tracker = PublishedStatusTracker()

    private fun useCase(repository: CommunityRepository) = PublishAgencyStatusUseCase(repository, tracker)

    @Test
    fun `each agency state maps to its own coarse status`() = runBlocking {
        val repository = PublishStatusFakeCommunityRepository()
        val publish = useCase(repository)

        publish(AgencyState.Stable)
        publish(AgencyState.Warning)
        publish(AgencyState.Crisis)

        assertEquals(
            listOf(MemberStatus.OK, MemberStatus.AT_RISK, MemberStatus.CRISIS),
            repository.published.map { it.second }
        )
    }

    /**
     * The property the whole design rests on: this runs for every scroll event, so anything other
     * than one write per transition would mean a network write per scroll.
     */
    @Test
    fun `an unchanged status is never written twice`() = runBlocking {
        val repository = PublishStatusFakeCommunityRepository()
        val publish = useCase(repository)

        repeat(50) { publish(AgencyState.Crisis) }

        assertEquals(1, repository.published.size)
    }

    @Test
    fun `returning to an earlier status writes again`() = runBlocking {
        val repository = PublishStatusFakeCommunityRepository()
        val publish = useCase(repository)

        publish(AgencyState.Crisis)
        publish(AgencyState.Stable)
        publish(AgencyState.Crisis)

        assertEquals(
            listOf(MemberStatus.CRISIS, MemberStatus.OK, MemberStatus.CRISIS),
            repository.published.map { it.second }
        )
    }

    @Test
    fun `only entering a crisis reports that peers should be alerted`() = runBlocking {
        val publish = useCase(PublishStatusFakeCommunityRepository())

        assertFalse(publish(AgencyState.Stable))
        assertFalse(publish(AgencyState.Warning))
        assertTrue(publish(AgencyState.Crisis))
        // Still in crisis, so there is nothing new to tell anyone.
        assertFalse(publish(AgencyState.Crisis))
    }

    /**
     * Belonging to no circle must still settle the status, otherwise every scroll event would
     * repeat the active-community lookup on the detection path. Safe because joining re-marks or
     * clears the tracker.
     */
    @Test
    fun `with no circle the status settles without writing anything`() = runBlocking {
        val repository = PublishStatusFakeCommunityRepository(activeCommunityId = null)
        val publish = useCase(repository)

        assertFalse(publish(AgencyState.Crisis))

        assertTrue(repository.published.isEmpty())
        assertTrue(tracker.isUnchanged(MemberStatus.CRISIS))
    }

    /**
     * A failed write must leave the tracker alone, so the next reading retries rather than the
     * status being silently suppressed until it happens to change again.
     */
    @Test
    fun `a failed write is retried on the next reading`() = runBlocking {
        val failing = PublishStatusFakeCommunityRepository(throwOnPublish = true)
        runCatching { useCase(failing)(AgencyState.Crisis) }

        assertFalse(tracker.isUnchanged(MemberStatus.CRISIS))

        val working = PublishStatusFakeCommunityRepository()
        useCase(working)(AgencyState.Crisis)

        assertEquals(listOf("comm-1" to MemberStatus.CRISIS), working.published)
    }
}
