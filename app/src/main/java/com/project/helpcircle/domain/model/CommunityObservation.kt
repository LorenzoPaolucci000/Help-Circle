package com.project.helpcircle.domain.model

/** What [com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase] emits: a community with peers, or this device alone in it. */
sealed class CommunityObservation {
    data class Populated(val state: CommunityState) : CommunityObservation()
    data class SoloMode(val communityId: String, val inviteCode: String) : CommunityObservation()
}
