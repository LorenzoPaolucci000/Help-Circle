package com.project.helpcircle.presentation.community

import com.project.helpcircle.domain.engine.WeeklyResetCalculator
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.CommunityWeeklySummary
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityWeeklyTrendUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class DashboardFakeCommunityRepository(
    private val activeCommunityId: String?,
    private val communityStateFlow: MutableStateFlow<CommunityState> =
        MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false)),
    private val memberCount: Int = 0
) : CommunityRepository {
    override fun observeCommunityState(communityId: String): Flow<CommunityState> = communityStateFlow
    override suspend fun joinCommunity(communityId: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState = throw UnsupportedOperationException()
    override suspend fun joinCommunityByInviteCode(inviteCode: String): CommunityState? = null
    override suspend fun reportCrisis(communityId: String) = Unit
    override suspend fun publishStatus(communityId: String, status: MemberStatus) = Unit
    override suspend fun reportRecovery(communityId: String) = Unit
    override suspend fun publishSatisfaction(
        communityId: String,
        weekStartEpochMillis: Long,
        satisfaction: WeeklySatisfaction
    ) = Unit
    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun ensureAlertSubscription() = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = memberCount
}

private class DashboardFakeCommunityWeeklyHistoryRepository(
    initialSummaries: List<CommunityWeeklySummary> = emptyList()
) : CommunityWeeklyHistoryRepository {
    private val summariesFlow = MutableStateFlow(initialSummaries)
    override fun weeklySummaries(communityId: String): Flow<List<CommunityWeeklySummary>> = summariesFlow
    override suspend fun ensureWeeklySnapshotApplied(communityId: String, currentCollectiveIndexValue: Int) = Unit
}

private fun member(
    id: String,
    status: MemberStatus = MemberStatus.OK,
    score: Int = 50,
    satisfaction: WeeklySatisfaction? = null,
    satisfactionWeekStart: Long? = null
) = CommunityMember(id, "nick-$id", status, score, satisfaction, satisfactionWeekStart)

/** The week a rating must be stamped with to count right now — the same value the ViewModel derives internally. */
private fun currentWeekStart(): Long =
    WeeklyResetCalculator.currentWeekStartEpochMillis(System.currentTimeMillis())

@OptIn(ExperimentalCoroutinesApi::class)
class CommunityDashboardViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        communityRepository: CommunityRepository,
        communityWeeklyHistoryRepository: CommunityWeeklyHistoryRepository = DashboardFakeCommunityWeeklyHistoryRepository()
    ): CommunityDashboardViewModel = CommunityDashboardViewModel(
        communityRepository,
        ObserveCommunityStateUseCase(communityRepository),
        ObserveCommunityWeeklyTrendUseCase(communityWeeklyHistoryRepository)
    )

    @Test
    fun `no active community shows the no-circle state`() {
        val viewModel = viewModel(DashboardFakeCommunityRepository(activeCommunityId = null))

        val state = viewModel.uiState.value
        assertEquals(false, state.hasActiveCommunity)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `a community with one member loads as solo mode`() {
        val stateFlow = MutableStateFlow(
            CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val state = viewModel.uiState.value
        assertTrue(state.isSolo)
        assertEquals("AB12CD", state.inviteCode)
        assertTrue(state.members.isEmpty())
    }

    @Test
    fun `a community with peers loads its member roster`() {
        val members = listOf(member("self"), member("peer"))
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val state = viewModel.uiState.value
        assertEquals(false, state.isSolo)
        assertEquals(members, state.members)
    }

    @Test
    fun `a populated community surfaces its name`() {
        val members = listOf(member("self"), member("peer"))
        val stateFlow = MutableStateFlow(
            CommunityState(
                "comm-1",
                emptyList(),
                cohesionBonusApplied = false,
                members = members,
                name = "OpenHarbor42"
            )
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        assertEquals("OpenHarbor42", viewModel.uiState.value.communityName)
    }

    @Test
    fun `a solo community surfaces its name too`() {
        val stateFlow = MutableStateFlow(
            CommunityState(
                "comm-1",
                emptyList(),
                cohesionBonusApplied = false,
                inviteCode = "AB12CD",
                name = "OpenHarbor42"
            )
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val state = viewModel.uiState.value
        assertTrue(state.isSolo)
        assertEquals("OpenHarbor42", state.communityName)
    }

    @Test
    fun `a community created before names existed leaves the name blank`() {
        val stateFlow = MutableStateFlow(
            CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        assertEquals("", viewModel.uiState.value.communityName)
    }

    @Test
    fun `with no recorded weekly history the weekly trend fields are null`() {
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1"))

        val state = viewModel.uiState.value
        assertNull(state.latestWeeklyCollectiveIndex)
        assertNull(state.previousWeeklyCollectiveIndex)
    }

    @Test
    fun `the latest and previous weekly snapshots are reflected in the ui state`() {
        val oldest = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 1_000, collectiveIndexValue = 55)
        val newest = CommunityWeeklySummary(communityId = "comm-1", weekStartEpochMillis = 2_000, collectiveIndexValue = 68)
        val weeklyHistoryRepository = DashboardFakeCommunityWeeklyHistoryRepository(listOf(newest, oldest))
        val viewModel = viewModel(
            DashboardFakeCommunityRepository("comm-1"),
            communityWeeklyHistoryRepository = weeklyHistoryRepository
        )

        val state = viewModel.uiState.value
        assertEquals(68, state.latestWeeklyCollectiveIndex)
        assertEquals(55, state.previousWeeklyCollectiveIndex)
    }

    @Test
    fun `the circle's current-week ratings are aggregated into the ui state`() {
        val week = currentWeekStart()
        val members = listOf(
            member("self", satisfaction = WeeklySatisfaction.HAPPY, satisfactionWeekStart = week),
            member("peer", satisfaction = WeeklySatisfaction.NEUTRAL, satisfactionWeekStart = week)
        )
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val satisfaction = viewModel.uiState.value.communitySatisfaction
        assertEquals(2, satisfaction?.ratedMemberCount)
        assertEquals(2, satisfaction?.memberCount)
        assertEquals(75, satisfaction?.averageScore)
    }

    @Test
    fun `a circle where nobody has rated this week reports no ratings but still counts its members`() {
        val members = listOf(member("self"), member("peer"))
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val satisfaction = viewModel.uiState.value.communitySatisfaction
        assertEquals(false, satisfaction?.hasRatings)
        assertEquals(2, satisfaction?.memberCount)
    }

    @Test
    fun `ratings left over from a previous week are not counted toward this week`() {
        val staleWeek = currentWeekStart() - WeeklyResetCalculator.WEEK_DURATION_MILLIS
        val members = listOf(
            member("self", satisfaction = WeeklySatisfaction.HAPPY, satisfactionWeekStart = staleWeek),
            member("peer", satisfaction = WeeklySatisfaction.BAD, satisfactionWeekStart = currentWeekStart())
        )
        val stateFlow = MutableStateFlow(CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, members = members))
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        val satisfaction = viewModel.uiState.value.communitySatisfaction
        assertEquals(1, satisfaction?.ratedMemberCount)
        assertEquals(0, satisfaction?.averageScore)
    }

    @Test
    fun `solo mode reports no community satisfaction at all`() {
        val stateFlow = MutableStateFlow(
            CommunityState("comm-1", emptyList(), cohesionBonusApplied = false, inviteCode = "AB12CD")
        )
        val viewModel = viewModel(DashboardFakeCommunityRepository("comm-1", stateFlow))

        assertTrue(viewModel.uiState.value.isSolo)
        assertNull(viewModel.uiState.value.communitySatisfaction)
    }

}
