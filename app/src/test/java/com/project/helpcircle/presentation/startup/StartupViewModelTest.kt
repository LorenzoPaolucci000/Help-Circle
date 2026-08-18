package com.project.helpcircle.presentation.startup

import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class StartupFakeUserRepository(private val nickname: String) : UserRepository {
    override suspend fun getOrCreateIdentity(): UserIdentity = UserIdentity(anonymousHash = "uid", nickname = nickname)
    override suspend fun saveNickname(nickname: String) = Unit
    override val chargeWallet get() = throw UnsupportedOperationException()
    override suspend fun updateChargeWallet(wallet: ChargeWallet) = Unit
}

private class StartupFakeCommunityRepository(private val activeCommunityId: String?) : CommunityRepository {
    override fun observeCommunityState(communityId: String): Flow<CommunityState> = emptyFlow()
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
    override suspend fun getMemberCount(communityId: String): Int = 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class StartupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `routes to nickname setup when no nickname is saved yet`() {
        val viewModel = StartupViewModel(
            StartupFakeUserRepository(nickname = ""),
            StartupFakeCommunityRepository(activeCommunityId = "comm-1")
        )

        assertEquals(StartupDestination.NICKNAME_SETUP, viewModel.destination.value)
    }

    @Test
    fun `routes to join community when a nickname exists but no active circle`() {
        val viewModel = StartupViewModel(
            StartupFakeUserRepository(nickname = "Wanderer42"),
            StartupFakeCommunityRepository(activeCommunityId = null)
        )

        assertEquals(StartupDestination.JOIN_COMMUNITY, viewModel.destination.value)
    }

    @Test
    fun `routes straight to the dashboard when a nickname and active circle both exist`() {
        val viewModel = StartupViewModel(
            StartupFakeUserRepository(nickname = "Wanderer42"),
            StartupFakeCommunityRepository(activeCommunityId = "comm-1")
        )

        assertEquals(StartupDestination.COMMUNITY_DASHBOARD, viewModel.destination.value)
    }
}
