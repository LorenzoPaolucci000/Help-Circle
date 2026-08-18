package com.project.helpcircle.presentation.home

import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.AgencyState
import com.project.helpcircle.domain.model.CrisisEpisodeRecord
import com.project.helpcircle.domain.model.FocusSession
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import com.project.helpcircle.domain.repository.WeeklySatisfactionRepository
import com.project.helpcircle.domain.usecase.ObserveAgencyHomeUseCase
import com.project.helpcircle.domain.usecase.ObserveWeeklySatisfactionUseCase
import com.project.helpcircle.domain.usecase.SubmitWeeklySatisfactionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private class HomeViewModelFakeAgencyRepository(initialIndex: AgencyIndex) : AgencyRepository {
    val indexFlow = MutableStateFlow(initialIndex)
    override val currentAgencyIndex: Flow<AgencyIndex> = indexFlow
    override val currentAgencyState: Flow<AgencyState> = MutableStateFlow(AgencyState.Stable)

    override suspend fun recordFocusSession(session: FocusSession) = Unit
    override suspend fun updateAgencyIndex(index: AgencyIndex) {
        indexFlow.value = index
    }
    override suspend fun reportAgencyState(state: AgencyState) = Unit
    override suspend fun adjustAgencyDeltas(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex = indexFlow.value
    override suspend fun getLastArchivedAgencyIndex(): Int? = null
    override suspend fun archiveAgencyIndex(agencyIndexValue: Int) = Unit
    override suspend fun getLastWeeklyResetAtEpochMillis(): Long? = null
    override suspend fun resetAgencyIndexForNewWeek(atEpochMillis: Long) = Unit
}

private class HomeViewModelFakeWeeklyHistoryRepository(initialSummaries: List<WeeklySummary>) : WeeklyHistoryRepository {
    override suspend fun recordCrisisEpisode(record: CrisisEpisodeRecord) = Unit
    override suspend fun getCrisisEpisodesBetween(fromEpochMillis: Long, untilEpochMillis: Long): List<CrisisEpisodeRecord> = emptyList()
    override suspend fun deleteCrisisEpisodesBefore(beforeEpochMillis: Long) = Unit
    override suspend fun saveWeeklySummary(summary: WeeklySummary) = Unit
    override val weeklySummaries: Flow<List<WeeklySummary>> = MutableStateFlow(initialSummaries)
}

private class HomeViewModelFakeWeeklySatisfactionRepository : WeeklySatisfactionRepository {
    val submitted = mutableListOf<Pair<Long, WeeklySatisfaction>>()
    private val stored = MutableStateFlow<WeeklySatisfaction?>(null)

    override fun satisfactionForWeek(weekStartEpochMillis: Long): Flow<WeeklySatisfaction?> = stored

    override suspend fun submit(weekStartEpochMillis: Long, satisfaction: WeeklySatisfaction) {
        submitted += weekStartEpochMillis to satisfaction
        stored.value = satisfaction
    }
}

private class HomeViewModelFakeCommunityRepository(
    private val activeCommunityId: String? = null,
    private val throwOnPublish: Boolean = false
) : CommunityRepository {
    val publishedSatisfactions = mutableListOf<WeeklySatisfaction>()

    override fun observeCommunityState(communityId: String): Flow<CommunityState> =
        MutableStateFlow(CommunityState(communityId, emptyList(), cohesionBonusApplied = false))

    override suspend fun joinCommunity(communityId: String): CommunityState =
        CommunityState(communityId, emptyList(), cohesionBonusApplied = false)

    override suspend fun createCommunity(communityId: String, inviteCode: String, name: String): CommunityState =
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
        publishedSatisfactions += satisfaction
    }

    override suspend fun leaveCommunity(communityId: String) = Unit
    override suspend fun getActiveCommunityId(): String? = activeCommunityId
    override suspend fun getMemberCount(communityId: String): Int = 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the ViewModel with satisfaction wiring defaulted out, so tests that only care about the agency summary stay readable. */
    private fun homeViewModel(
        observeAgencyHome: ObserveAgencyHomeUseCase,
        satisfactionRepository: WeeklySatisfactionRepository = HomeViewModelFakeWeeklySatisfactionRepository(),
        communityRepository: CommunityRepository = HomeViewModelFakeCommunityRepository()
    ): HomeViewModel = HomeViewModel(
        observeAgencyHome,
        ObserveWeeklySatisfactionUseCase(satisfactionRepository),
        SubmitWeeklySatisfactionUseCase(satisfactionRepository, communityRepository)
    )

    /** The agency-summary half of the screen, stubbed out for tests that only exercise the satisfaction picker. */
    private fun emptyAgencyHome(): ObserveAgencyHomeUseCase = ObserveAgencyHomeUseCase(
        HomeViewModelFakeAgencyRepository(AgencyIndex.baseline()),
        HomeViewModelFakeWeeklyHistoryRepository(emptyList())
    )

    @Test
    fun `with no weekly history the ui state shows the live index and no summary`() {
        val useCase = ObserveAgencyHomeUseCase(
            HomeViewModelFakeAgencyRepository(AgencyIndex.of(65)),
            HomeViewModelFakeWeeklyHistoryRepository(emptyList())
        )

        val viewModel = homeViewModel(useCase)

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals(65, viewModel.uiState.value.currentAgencyIndex)
        assertEquals(emptyList<Int>(), viewModel.uiState.value.weeklyDeltasOldestFirst)
        assertNull(viewModel.uiState.value.latestWeeklySummary)
        assertNull(viewModel.uiState.value.previousWeeklySummary)
    }

    @Test
    fun `weekly deltas and the latest summary are surfaced from the use case`() {
        val oldest = WeeklySummary(weekStartEpochMillis = 1_000, agencyIndexDelta = 5, mostEffectiveInterventionCategory = "Text", peakCrisisHour = 22)
        val newest = WeeklySummary(weekStartEpochMillis = 2_000, agencyIndexDelta = -3, mostEffectiveInterventionCategory = "Haptic", peakCrisisHour = 23)
        val useCase = ObserveAgencyHomeUseCase(
            HomeViewModelFakeAgencyRepository(AgencyIndex.baseline()),
            HomeViewModelFakeWeeklyHistoryRepository(listOf(newest, oldest))
        )

        val viewModel = homeViewModel(useCase)

        assertEquals(listOf(5, -3), viewModel.uiState.value.weeklyDeltasOldestFirst)
        assertEquals(newest, viewModel.uiState.value.latestWeeklySummary)
        assertEquals(oldest, viewModel.uiState.value.previousWeeklySummary)
    }

    @Test
    fun `before anything is rated the week shows as unrated`() {
        val viewModel = homeViewModel(emptyAgencyHome())

        assertNull(viewModel.uiState.value.currentWeekSatisfaction)
        assertNull(viewModel.uiState.value.satisfactionError)
        assertEquals(false, viewModel.uiState.value.isSubmittingSatisfaction)
    }

    @Test
    fun `picking a face records it locally and shares it with the circle`() {
        val satisfactionRepository = HomeViewModelFakeWeeklySatisfactionRepository()
        val communityRepository = HomeViewModelFakeCommunityRepository(activeCommunityId = "comm-1")
        val viewModel = homeViewModel(emptyAgencyHome(), satisfactionRepository, communityRepository)

        viewModel.onSatisfactionSelected(WeeklySatisfaction.HAPPY)

        assertEquals(WeeklySatisfaction.HAPPY, viewModel.uiState.value.currentWeekSatisfaction)
        assertEquals(listOf(WeeklySatisfaction.HAPPY), communityRepository.publishedSatisfactions)
        assertNull(viewModel.uiState.value.satisfactionError)
        assertEquals(false, viewModel.uiState.value.isSubmittingSatisfaction)
    }

    @Test
    fun `changing your mind replaces the earlier answer`() {
        val satisfactionRepository = HomeViewModelFakeWeeklySatisfactionRepository()
        val viewModel = homeViewModel(emptyAgencyHome(), satisfactionRepository)

        viewModel.onSatisfactionSelected(WeeklySatisfaction.BAD)
        viewModel.onSatisfactionSelected(WeeklySatisfaction.NEUTRAL)

        assertEquals(WeeklySatisfaction.NEUTRAL, viewModel.uiState.value.currentWeekSatisfaction)
        // One row per submission, but both stamped with the same week — the repository upserts.
        assertEquals(2, satisfactionRepository.submitted.size)
        assertEquals(satisfactionRepository.submitted[0].first, satisfactionRepository.submitted[1].first)
    }

    @Test
    fun `a failed share surfaces an error but keeps the choice selected`() {
        val satisfactionRepository = HomeViewModelFakeWeeklySatisfactionRepository()
        val communityRepository = HomeViewModelFakeCommunityRepository(
            activeCommunityId = "comm-1",
            throwOnPublish = true
        )
        val viewModel = homeViewModel(emptyAgencyHome(), satisfactionRepository, communityRepository)

        viewModel.onSatisfactionSelected(WeeklySatisfaction.BAD)

        // The local write lands before the publish is attempted, so the face stays chosen and only
        // the sharing is reported as failed.
        assertEquals(WeeklySatisfaction.BAD, viewModel.uiState.value.currentWeekSatisfaction)
        assertNotNull(viewModel.uiState.value.satisfactionError)
        assertEquals(false, viewModel.uiState.value.isSubmittingSatisfaction)
    }

    @Test
    fun `a retry after a failure clears the previous error`() {
        val satisfactionRepository = HomeViewModelFakeWeeklySatisfactionRepository()
        val failing = HomeViewModelFakeCommunityRepository(activeCommunityId = "comm-1", throwOnPublish = true)
        val viewModel = homeViewModel(emptyAgencyHome(), satisfactionRepository, failing)
        viewModel.onSatisfactionSelected(WeeklySatisfaction.BAD)
        assertNotNull(viewModel.uiState.value.satisfactionError)

        val healthy = HomeViewModelFakeCommunityRepository(activeCommunityId = "comm-1")
        val retried = homeViewModel(emptyAgencyHome(), satisfactionRepository, healthy)
        retried.onSatisfactionSelected(WeeklySatisfaction.BAD)

        assertNull(retried.uiState.value.satisfactionError)
    }

    @Test
    fun `previous summary is the second-most-recent week, not merely the oldest`() {
        val oldest = WeeklySummary(weekStartEpochMillis = 1_000, agencyIndexDelta = 5, mostEffectiveInterventionCategory = "Text", peakCrisisHour = 22)
        val middle = WeeklySummary(weekStartEpochMillis = 2_000, agencyIndexDelta = 2, mostEffectiveInterventionCategory = "Haptic", peakCrisisHour = 21)
        val newest = WeeklySummary(weekStartEpochMillis = 3_000, agencyIndexDelta = -3, mostEffectiveInterventionCategory = "Blur", peakCrisisHour = 23)
        val useCase = ObserveAgencyHomeUseCase(
            HomeViewModelFakeAgencyRepository(AgencyIndex.baseline()),
            HomeViewModelFakeWeeklyHistoryRepository(listOf(newest, oldest, middle))
        )

        val viewModel = homeViewModel(useCase)

        assertEquals(newest, viewModel.uiState.value.latestWeeklySummary)
        assertEquals(middle, viewModel.uiState.value.previousWeeklySummary)
    }
}
