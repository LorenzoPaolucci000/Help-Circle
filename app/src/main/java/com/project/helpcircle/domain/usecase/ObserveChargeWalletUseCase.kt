package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The local user's [ChargeWallet], with passive replenishment (and the daily reset) applied
 * lazily against the current time on every read — there's no scheduled background job driving
 * this, so a wallet that hasn't been observed in a while simply catches up the moment it next is.
 * Persists the caught-up wallet back via [UserRepository.updateChargeWallet] whenever it changes.
 */
class ObserveChargeWalletUseCase(
    private val userRepository: UserRepository,
    private val isFocusModeActiveUseCase: IsFocusModeActiveUseCase
) {
    operator fun invoke(): Flow<ChargeWallet> = userRepository.chargeWallet.map { wallet ->
        val now = System.currentTimeMillis()
        val replenished = wallet.replenished(now, isFocusModeActiveUseCase(now))
        if (replenished != wallet) {
            userRepository.updateChargeWallet(replenished)
        }
        replenished
    }
}
