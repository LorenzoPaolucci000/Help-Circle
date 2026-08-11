package com.project.helpcircle.di

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase
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
}
