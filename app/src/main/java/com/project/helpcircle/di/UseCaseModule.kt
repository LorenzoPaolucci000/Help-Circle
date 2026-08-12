package com.project.helpcircle.di

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import com.project.helpcircle.domain.usecase.AcknowledgeRecoveryUseCase
import com.project.helpcircle.domain.usecase.CalculateAgencyIndexUseCase
import com.project.helpcircle.domain.usecase.ConsumeChargeUseCase
import com.project.helpcircle.domain.usecase.CreateCommunityUseCase
import com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityByInviteCodeUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveIncomingNudgesUseCase
import com.project.helpcircle.domain.usecase.SendNudgeUseCase
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
    @Singleton
    fun provideCrisisEpisodeTracker(): CrisisEpisodeTracker = CrisisEpisodeTracker()

    @Provides
    fun provideCalculateAgencyIndexUseCase(
        agencyRepository: AgencyRepository
    ): CalculateAgencyIndexUseCase = CalculateAgencyIndexUseCase(agencyRepository)

    @Provides
    fun provideDetectLossOfAgencyUseCase(
        agencyDetectionEngine: AgencyDetectionEngine,
        agencyRepository: AgencyRepository,
        crisisEpisodeTracker: CrisisEpisodeTracker,
        calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase
    ): DetectLossOfAgencyUseCase = DetectLossOfAgencyUseCase(
        agencyDetectionEngine,
        agencyRepository,
        crisisEpisodeTracker,
        calculateAgencyIndexUseCase
    )

    @Provides
    fun provideAcknowledgeRecoveryUseCase(
        agencyRepository: AgencyRepository,
        crisisEpisodeTracker: CrisisEpisodeTracker,
        calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase
    ): AcknowledgeRecoveryUseCase = AcknowledgeRecoveryUseCase(
        agencyRepository,
        crisisEpisodeTracker,
        calculateAgencyIndexUseCase
    )

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

    @Provides
    fun provideJoinCommunityUseCase(
        communityRepository: CommunityRepository
    ): JoinCommunityUseCase = JoinCommunityUseCase(communityRepository)

    @Provides
    fun provideCreateCommunityUseCase(
        communityRepository: CommunityRepository
    ): CreateCommunityUseCase = CreateCommunityUseCase(communityRepository)

    @Provides
    fun provideJoinCommunityByInviteCodeUseCase(
        communityRepository: CommunityRepository
    ): JoinCommunityByInviteCodeUseCase = JoinCommunityByInviteCodeUseCase(communityRepository)

    @Provides
    fun provideConsumeChargeUseCase(
        userRepository: UserRepository
    ): ConsumeChargeUseCase = ConsumeChargeUseCase(userRepository)

    @Provides
    fun provideSendNudgeUseCase(
        nudgeRepository: NudgeRepository,
        communityRepository: CommunityRepository,
        consumeChargeUseCase: ConsumeChargeUseCase
    ): SendNudgeUseCase = SendNudgeUseCase(nudgeRepository, communityRepository, consumeChargeUseCase)
}
