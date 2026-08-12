package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** Spends the charges required for a [Nudge], failing if the (freshly replenished) wallet can't afford it. */
class ConsumeChargeUseCase(
    private val userRepository: UserRepository,
    private val observeChargeWalletUseCase: ObserveChargeWalletUseCase
) {
    suspend operator fun invoke(nudge: Nudge) {
        val wallet = observeChargeWalletUseCase().first()
        check(wallet.canAfford(nudge)) { "Not enough charges to send ${nudge::class.simpleName}" }
        userRepository.updateChargeWallet(wallet.spend(nudge))
    }
}
