package com.project.helpcircle.data.repository

import com.project.helpcircle.data.local.dao.ChargeWalletDao
import com.project.helpcircle.data.local.dao.UserIdentityDao
import com.project.helpcircle.data.local.entity.ChargeWalletEntity
import com.project.helpcircle.data.local.entity.UserIdentityEntity
import com.project.helpcircle.domain.model.ChargeWallet
import com.project.helpcircle.domain.model.UserIdentity
import com.project.helpcircle.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** Room-backed [UserRepository]: manages the anonymous identity and charge wallet locally. */
class UserRepositoryImpl @Inject constructor(
    private val userIdentityDao: UserIdentityDao,
    private val chargeWalletDao: ChargeWalletDao
) : UserRepository {

    override suspend fun getOrCreateIdentity(): UserIdentity {
        userIdentityDao.get()?.let { return UserIdentity(it.anonymousHash) }
        val identity = UserIdentity(UUID.randomUUID().toString().replace("-", ""))
        userIdentityDao.insert(UserIdentityEntity(anonymousHash = identity.anonymousHash))
        return identity
    }

    override val chargeWallet: Flow<ChargeWallet> = chargeWalletDao.observe()
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
