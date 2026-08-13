package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import kotlinx.coroutines.flow.first

/**
 * Looks up a community by its shared invite code and joins the caller into it, or returns null if
 * no community has that code. Requires at least one monitored app first — joining a circle with
 * nothing being watched for doomscroll detection would make the crisis-support loop pointless.
 */
class JoinCommunityByInviteCodeUseCase(
    private val communityRepository: CommunityRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository
) {
    suspend operator fun invoke(inviteCode: String): CommunityState? {
        check(monitoredAppsRepository.monitoredPackageNames.first().isNotEmpty()) {
            "Add at least one app to monitor first"
        }
        return communityRepository.joinCommunityByInviteCode(inviteCode.trim().uppercase())
    }
}
