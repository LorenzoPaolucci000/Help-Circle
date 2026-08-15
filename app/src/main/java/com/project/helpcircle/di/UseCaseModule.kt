package com.project.helpcircle.di

import com.project.helpcircle.domain.engine.AgencyDetectionEngine
import com.project.helpcircle.domain.engine.CrisisEpisodeTracker
import com.project.helpcircle.domain.engine.ForegroundAppTracker
import com.project.helpcircle.domain.engine.SystemFallbackEvaluator
import com.project.helpcircle.domain.repository.AgencyRepository
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.CommunityWeeklyHistoryRepository
import com.project.helpcircle.domain.repository.InstalledAppsRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import com.project.helpcircle.domain.repository.NudgeRepository
import com.project.helpcircle.domain.repository.UserRepository
import com.project.helpcircle.domain.repository.WeeklyHistoryRepository
import com.project.helpcircle.domain.repository.WeeklySatisfactionRepository
import com.project.helpcircle.domain.usecase.AcknowledgeRecoveryUseCase
import com.project.helpcircle.domain.usecase.CalculateAgencyIndexUseCase
import com.project.helpcircle.domain.usecase.ConsumeChargeUseCase
import com.project.helpcircle.domain.usecase.CreateCommunityUseCase
import com.project.helpcircle.domain.usecase.DetectLossOfAgencyUseCase
import com.project.helpcircle.domain.usecase.EvaluateSystemFallbackUseCase
import com.project.helpcircle.domain.usecase.GetInstalledAppsUseCase
import com.project.helpcircle.domain.usecase.IsFocusModeActiveUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityByInviteCodeUseCase
import com.project.helpcircle.domain.usecase.JoinCommunityUseCase
import com.project.helpcircle.domain.usecase.LeaveCommunityUseCase
import com.project.helpcircle.domain.usecase.ObserveAgencyHomeUseCase
import com.project.helpcircle.domain.usecase.ObserveChargeWalletUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase
import com.project.helpcircle.domain.usecase.ObserveCommunityWeeklyTrendUseCase
import com.project.helpcircle.domain.usecase.ObserveIncomingNudgesUseCase
import com.project.helpcircle.domain.usecase.ObserveWeeklySatisfactionUseCase
import com.project.helpcircle.domain.usecase.SendNudgeUseCase
import com.project.helpcircle.domain.usecase.StartSystemFallbackBreakUseCase
import com.project.helpcircle.domain.usecase.SubmitWeeklySatisfactionUseCase
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
    @Singleton
    fun provideForegroundAppTracker(): ForegroundAppTracker = ForegroundAppTracker()

    @Provides
    @Singleton
    fun provideSystemFallbackEvaluator(
        crisisEpisodeTracker: CrisisEpisodeTracker
    ): SystemFallbackEvaluator = SystemFallbackEvaluator(crisisEpisodeTracker)

    @Provides
    fun provideCalculateAgencyIndexUseCase(
        agencyRepository: AgencyRepository
    ): CalculateAgencyIndexUseCase = CalculateAgencyIndexUseCase(agencyRepository)

    @Provides
    fun provideDetectLossOfAgencyUseCase(
        agencyDetectionEngine: AgencyDetectionEngine,
        agencyRepository: AgencyRepository,
        crisisEpisodeTracker: CrisisEpisodeTracker,
        calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase,
        weeklyHistoryRepository: WeeklyHistoryRepository,
        evaluateSystemFallbackUseCase: EvaluateSystemFallbackUseCase,
        acknowledgeRecoveryUseCase: AcknowledgeRecoveryUseCase,
        communityRepository: CommunityRepository
    ): DetectLossOfAgencyUseCase = DetectLossOfAgencyUseCase(
        agencyDetectionEngine,
        agencyRepository,
        crisisEpisodeTracker,
        calculateAgencyIndexUseCase,
        weeklyHistoryRepository,
        evaluateSystemFallbackUseCase,
        acknowledgeRecoveryUseCase,
        communityRepository
    )

    @Provides
    fun provideStartSystemFallbackBreakUseCase(
        crisisEpisodeTracker: CrisisEpisodeTracker
    ): StartSystemFallbackBreakUseCase = StartSystemFallbackBreakUseCase(crisisEpisodeTracker)

    @Provides
    @Singleton
    fun provideEvaluateSystemFallbackUseCase(
        crisisEpisodeTracker: CrisisEpisodeTracker,
        systemFallbackEvaluator: SystemFallbackEvaluator,
        communityRepository: CommunityRepository
    ): EvaluateSystemFallbackUseCase = EvaluateSystemFallbackUseCase(
        crisisEpisodeTracker,
        systemFallbackEvaluator,
        communityRepository
    )

    @Provides
    fun provideAcknowledgeRecoveryUseCase(
        agencyRepository: AgencyRepository,
        crisisEpisodeTracker: CrisisEpisodeTracker,
        calculateAgencyIndexUseCase: CalculateAgencyIndexUseCase,
        weeklyHistoryRepository: WeeklyHistoryRepository
    ): AcknowledgeRecoveryUseCase = AcknowledgeRecoveryUseCase(
        agencyRepository,
        crisisEpisodeTracker,
        calculateAgencyIndexUseCase,
        weeklyHistoryRepository
    )

    @Provides
    fun provideObserveAgencyHomeUseCase(
        agencyRepository: AgencyRepository,
        weeklyHistoryRepository: WeeklyHistoryRepository
    ): ObserveAgencyHomeUseCase = ObserveAgencyHomeUseCase(agencyRepository, weeklyHistoryRepository)

    @Provides
    fun provideObserveCommunityStateUseCase(
        communityRepository: CommunityRepository
    ): ObserveCommunityStateUseCase = ObserveCommunityStateUseCase(communityRepository)

    @Provides
    fun provideObserveCommunityWeeklyTrendUseCase(
        communityWeeklyHistoryRepository: CommunityWeeklyHistoryRepository
    ): ObserveCommunityWeeklyTrendUseCase = ObserveCommunityWeeklyTrendUseCase(communityWeeklyHistoryRepository)

    @Provides
    fun provideObserveIncomingNudgesUseCase(
        nudgeRepository: NudgeRepository
    ): ObserveIncomingNudgesUseCase = ObserveIncomingNudgesUseCase(nudgeRepository)

    @Provides
    fun provideValidateNicknameUseCase(): ValidateNicknameUseCase = ValidateNicknameUseCase()

    @Provides
    fun provideJoinCommunityUseCase(
        communityRepository: CommunityRepository,
        monitoredAppsRepository: MonitoredAppsRepository
    ): JoinCommunityUseCase = JoinCommunityUseCase(communityRepository, monitoredAppsRepository)

    @Provides
    fun provideCreateCommunityUseCase(
        communityRepository: CommunityRepository,
        monitoredAppsRepository: MonitoredAppsRepository
    ): CreateCommunityUseCase = CreateCommunityUseCase(communityRepository, monitoredAppsRepository)

    @Provides
    fun provideJoinCommunityByInviteCodeUseCase(
        communityRepository: CommunityRepository,
        monitoredAppsRepository: MonitoredAppsRepository
    ): JoinCommunityByInviteCodeUseCase =
        JoinCommunityByInviteCodeUseCase(communityRepository, monitoredAppsRepository)

    @Provides
    fun provideLeaveCommunityUseCase(
        communityRepository: CommunityRepository
    ): LeaveCommunityUseCase = LeaveCommunityUseCase(communityRepository)

    @Provides
    fun provideObserveChargeWalletUseCase(
        userRepository: UserRepository,
        isFocusModeActiveUseCase: IsFocusModeActiveUseCase
    ): ObserveChargeWalletUseCase = ObserveChargeWalletUseCase(userRepository, isFocusModeActiveUseCase)

    @Provides
    fun provideConsumeChargeUseCase(
        userRepository: UserRepository,
        observeChargeWalletUseCase: ObserveChargeWalletUseCase
    ): ConsumeChargeUseCase = ConsumeChargeUseCase(userRepository, observeChargeWalletUseCase)

    @Provides
    fun provideSendNudgeUseCase(
        nudgeRepository: NudgeRepository,
        communityRepository: CommunityRepository,
        consumeChargeUseCase: ConsumeChargeUseCase
    ): SendNudgeUseCase = SendNudgeUseCase(nudgeRepository, communityRepository, consumeChargeUseCase)

    @Provides
    fun provideGetInstalledAppsUseCase(
        installedAppsRepository: InstalledAppsRepository
    ): GetInstalledAppsUseCase = GetInstalledAppsUseCase(installedAppsRepository)

    @Provides
    fun provideIsFocusModeActiveUseCase(
        foregroundAppTracker: ForegroundAppTracker
    ): IsFocusModeActiveUseCase = IsFocusModeActiveUseCase(foregroundAppTracker)

    @Provides
    fun provideObserveWeeklySatisfactionUseCase(
        weeklySatisfactionRepository: WeeklySatisfactionRepository
    ): ObserveWeeklySatisfactionUseCase = ObserveWeeklySatisfactionUseCase(weeklySatisfactionRepository)

    @Provides
    fun provideSubmitWeeklySatisfactionUseCase(
        weeklySatisfactionRepository: WeeklySatisfactionRepository,
        communityRepository: CommunityRepository
    ): SubmitWeeklySatisfactionUseCase =
        SubmitWeeklySatisfactionUseCase(weeklySatisfactionRepository, communityRepository)
}
