package com.project.helpcircle.di

import com.project.helpcircle.data.repository.AgencyRepositoryImpl
import com.project.helpcircle.data.repository.CommunityRepositoryImpl
import com.project.helpcircle.data.repository.MonitoredAppsRepositoryImpl
import com.project.helpcircle.data.repository.NudgeRepositoryImpl
import com.project.helpcircle.data.repository.UserRepositoryImpl
import com.project.helpcircle.data.repository.WeeklyHistoryRepositoryImpl
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.InstalledAppsRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.repository.MonitoringStatusRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import com.project.helpcircle.os.InstalledAppsRepositoryImpl
import com.project.helpcircle.os.MonitoringStatusRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Hilt module binding domain repository interfaces to their data-layer implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAgencyRepository(impl: AgencyRepositoryImpl): AgencyRepository

    @Binds
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindCommunityRepository(impl: CommunityRepositoryImpl): CommunityRepository

    @Binds
    abstract fun bindNudgeRepository(impl: NudgeRepositoryImpl): NudgeRepository

    @Binds
    abstract fun bindMonitoredAppsRepository(impl: MonitoredAppsRepositoryImpl): MonitoredAppsRepository

    @Binds
    abstract fun bindInstalledAppsRepository(impl: InstalledAppsRepositoryImpl): InstalledAppsRepository

    @Binds
    abstract fun bindWeeklyHistoryRepository(impl: WeeklyHistoryRepositoryImpl): WeeklyHistoryRepository

    @Binds
    abstract fun bindMonitoringStatusRepository(impl: MonitoringStatusRepositoryImpl): MonitoringStatusRepository
}
