package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.first

/** Adds charges back to the user's [ChargeWallet], capped at the maximum. */
class ReplenishChargesUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(amount: Int, atEpochMillis: Long) {
        val wallet = userRepository.chargeWallet.first()
        userRepository.updateChargeWallet(wallet.replenish(amount, atEpochMillis))
    }
}
