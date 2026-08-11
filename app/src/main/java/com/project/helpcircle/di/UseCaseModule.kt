package com.project.helpcircle.di

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveIncomingNudgesUseCase
import com.project.helpcircle.domain.usecase.ValidateNicknameUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module providing domain engines/use cases the domain layer keeps free of DI annotations. */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideAgencyDetectionEngine(): AgencyDetectionEngine = AgencyDetectionEngine()

    @Provides
    fun provideDetectLossOfAgencyUseCase(
        agencyDetectionEngine: AgencyDetectionEngine,
        agencyRepository: AgencyRepository
    ): DetectLossOfAgencyUseCase = DetectLossOfAgencyUseCase(agencyDetectionEngine, agencyRepository)

    @Provides
    fun provideObserveCommunityStateUseCase(
        communityRepository: CommunityRepository
    ): ObserveCommunityStateUseCase = ObserveCommunityStateUseCase(communityRepository)

    @Provides
    fun provideObserveIncomingNudgesUseCase(
        nudgeRepository: NudgeRepository
    ): ObserveIncomingNudgesUseCase = ObserveIncomingNudgesUseCase(nudgeRepository)

    @Provides
    fun provideValidateNicknameUseCase(): ValidateNicknameUseCase = ValidateNicknameUseCase()
}
