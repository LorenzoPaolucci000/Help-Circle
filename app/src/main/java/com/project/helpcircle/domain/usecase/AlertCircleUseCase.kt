package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.PeerAlertRepository

/**
 * Pushes a crisis alert to the rest of the user's circle.
 *
 * Called only when [PublishAgencyStatusUseCase] reports the user has just *entered* crisis, not on
 * every reading while they stay there — otherwise a single doomscroll session would ring every
 * peer's phone once per scroll event.
 */
class AlertCircleUseCase(
    private val communityRepository: CommunityRepository,
    private val peerAlertRepository: PeerAlertRepository
) {
    suspend operator fun invoke() {
        val communityId = communityRepository.getActiveCommunityId() ?: return
        peerAlertRepository.alertCircle(communityId)
    }
}
