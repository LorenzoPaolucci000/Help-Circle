package com.project.helpcircle.di

import android.content.Context
import com.project.helpcircle.data.local.AppDatabase
import com.project.helpcircle.data.local.SqlCipherPassphraseProvider
import com.project.helpcircle.data.local.dao.AgencyStateDao
import com.project.helpcircle.data.local.dao.ChargeWalletDao
import com.project.helpcircle.data.local.dao.FocusSessionDao
import com.project.helpcircle.data.local.dao.UserIdentityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module providing the SQLCipher-encrypted Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val passphrase = SqlCipherPassphraseProvider(context).getOrCreatePassphrase()
        return AppDatabase.build(context, passphrase)
    }

    @Provides
    fun provideFocusSessionDao(database: AppDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideChargeWalletDao(database: AppDatabase): ChargeWalletDao = database.chargeWalletDao()

    @Provides
    fun provideUserIdentityDao(database: AppDatabase): UserIdentityDao = database.userIdentityDao()

    @Provides
    fun provideAgencyStateDao(database: AppDatabase): AgencyStateDao = database.agencyStateDao()
}
