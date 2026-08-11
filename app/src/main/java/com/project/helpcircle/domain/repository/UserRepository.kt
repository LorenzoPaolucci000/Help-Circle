package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.UserIdentity
import kotlinx.coroutines.flow.Flow

/** Manages the local user's anonymous [UserIdentity] and [ChargeWallet]. */
interface UserRepository {
    suspend fun getOrCreateIdentity(): UserIdentity

    /** Persists the user's self-chosen pseudonym for [getOrCreateIdentity] to return afterwards. */
    suspend fun saveNickname(nickname: String)

    val chargeWallet: Flow<ChargeWallet>
    suspend fun updateChargeWallet(wallet: ChargeWallet)
}
