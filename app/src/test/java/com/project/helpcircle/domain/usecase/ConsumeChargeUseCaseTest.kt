package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.engine.ForegroundAppTracker
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.TextNudgeStyle
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A wallet timestamped far enough in the future that [ChargeWallet.replenished] never touches it mid-test. */
private const val FROZEN_LAST_REPLENISHED_AT = Long.MAX_VALUE / 2

private class ConsumeChargeFakeUserRepository(currentCharges: Int) : UserRepository {
    val walletFlow = MutableStateFlow(
        ChargeWallet(currentCharges = currentCharges, lastReplenishedAtEpochMillis = FROZEN_LAST_REPLENISHED_AT)
    )

    override suspend fun getOrCreateIdentity(): UserIdentity = UserIdentity(anonymousHash = "uid", nickname = "nick")
    override suspend fun saveNickname(nickname: String) = Unit
    override val chargeWallet: Flow<ChargeWallet> = walletFlow
    override suspend fun updateChargeWallet(wallet: ChargeWallet) {
        walletFlow.value = wallet
    }
}

private fun consumeChargeUseCase(userRepository: UserRepository) = ConsumeChargeUseCase(
    userRepository,
    ObserveChargeWalletUseCase(userRepository, IsFocusModeActiveUseCase(ForegroundAppTracker()))
)

class ConsumeChargeUseCaseTest {

    @Test
    fun `spends and persists the nudge's charge cost when affordable`() = runBlocking {
        val userRepository = ConsumeChargeFakeUserRepository(currentCharges = 5)

        consumeChargeUseCase(userRepository)(Nudge.Haptic)

        assertEquals(3, userRepository.walletFlow.value.currentCharges)
    }

    @Test
    fun `throws and leaves the wallet untouched when charges are insufficient`() = runBlocking {
        val userRepository = ConsumeChargeFakeUserRepository(currentCharges = 1)

        var threw = false
        try {
            consumeChargeUseCase(userRepository)(Nudge.Haptic)
        } catch (e: IllegalStateException) {
            threw = true
        }

        assertTrue(threw)
        assertEquals(1, userRepository.walletFlow.value.currentCharges)
    }

    @Test
    fun `spends exactly the wallet's remaining charges when the nudge costs all of them`() = runBlocking {
        val userRepository = ConsumeChargeFakeUserRepository(currentCharges = 4)

        consumeChargeUseCase(userRepository)(Nudge.ContentBlur)

        assertEquals(0, userRepository.walletFlow.value.currentCharges)
        assertFalse(userRepository.walletFlow.value.canAfford(Nudge.Text(TextNudgeStyle.COMIC)))
    }
}
