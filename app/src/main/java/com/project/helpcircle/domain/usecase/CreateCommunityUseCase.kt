package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.InviteCodeGenerator
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import kotlinx.coroutines.flow.first

/**
 * Creates a new community with a freshly generated invite code and joins the caller into it.
 * Requires at least one monitored app first, same rationale as [JoinCommunityByInviteCodeUseCase].
 */
class CreateCommunityUseCase(
    private val communityRepository: CommunityRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository
) {
    suspend operator fun invoke(): CommunityState {
        check(monitoredAppsRepository.monitoredPackageNames.first().isNotEmpty()) {
            "Add at least one app to monitor first"
        }
        return communityRepository.createCommunity(InviteCodeGenerator.generate())
    }
}
