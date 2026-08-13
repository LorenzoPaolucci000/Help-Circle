package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import kotlinx.coroutines.flow.first

/**
 * Joins the user into a support community and returns its current [CommunityState]. Requires at
 * least one monitored app first, same rationale as [JoinCommunityByInviteCodeUseCase].
 */
class JoinCommunityUseCase(
    private val communityRepository: CommunityRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository
) {
    suspend operator fun invoke(communityId: String): CommunityState {
        check(monitoredAppsRepository.monitoredPackageNames.first().isNotEmpty()) {
            "Add at least one app to monitor first"
        }
        return communityRepository.joinCommunity(communityId)
    }
}
