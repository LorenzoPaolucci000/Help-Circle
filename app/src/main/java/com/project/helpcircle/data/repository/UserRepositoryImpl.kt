package com.project.helpcircle.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.project.helpcircle.data.local.dao.ChargeWalletDao
import com.project.helpcircle.data.local.dao.UserIdentityDao
import com.project.helpcircle.data.local.entity.ChargeWalletEntity
import com.project.helpcircle.data.local.entity.UserIdentityEntity
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

/**
 * Room-backed [UserRepository]: manages the anonymous identity and charge wallet locally.
 *
 * The identity itself is a Firebase Anonymous Auth UID (no email/password, never linked to
 * personal information), signed in once on first launch and cached in Room so later calls
 * don't need to hit Firebase again.
 */
class UserRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userIdentityDao: UserIdentityDao,
    private val chargeWalletDao: ChargeWalletDao
) : UserRepository {

    override suspend fun getOrCreateIdentity(): UserIdentity {
        userIdentityDao.get()?.let { return UserIdentity(it.anonymousHash, it.nickname) }
        val uid = firebaseAuth.currentUser?.uid
            ?: firebaseAuth.signInAnonymously().await().user!!.uid
        val identity = UserIdentity(uid)
        userIdentityDao.insert(UserIdentityEntity(anonymousHash = identity.anonymousHash))
        return identity
    }

    override suspend fun saveNickname(nickname: String) {
        val identity = getOrCreateIdentity()
        userIdentityDao.insert(UserIdentityEntity(anonymousHash = identity.anonymousHash, nickname = nickname))
    }

    // No row exists until the very first read; self-heal by seeding one at full charges rather
    // than requiring every caller to know about a separate get-or-create step.
    override val chargeWallet: Flow<ChargeWallet> = chargeWalletDao.observe()
        .onEach { entity ->
            if (entity == null) {
                chargeWalletDao.upsert(
                    ChargeWalletEntity(
                        currentCharges = ChargeWallet.MAX_CHARGES,
                        lastReplenishedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
        .filterNotNull()
        .map { ChargeWallet(it.currentCharges, it.lastReplenishedAtEpochMillis) }

    override suspend fun updateChargeWallet(wallet: ChargeWallet) {
        chargeWalletDao.upsert(
            ChargeWalletEntity(
                currentCharges = wallet.currentCharges,
                lastReplenishedAtEpochMillis = wallet.lastReplenishedAtEpochMillis
            )
        )
    }
}
