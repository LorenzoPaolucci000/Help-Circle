package com.project.helpcircle.domain.model

/** What [com.project.helpcircle.domain.usecase.ObserveCommunityStateUseCase] emits: a community with peers, or this device alone in it. */
sealed class CommunityObservation {
    data class Populated(val state: CommunityState) : CommunityObservation()
    // SoloMode carries the name as well as the code because creating a circle lands the creator
    // here immediately — this is the first screen the name they just chose has to appear on.
    data class SoloMode(
        val communityId: String,
        val inviteCode: String,
        val name: String = ""
    ) : CommunityObservation()
}
