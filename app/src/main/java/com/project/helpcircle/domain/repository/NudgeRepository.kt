package com.project.helpcircle.domain.repository

import com.project.helpcircle.domain.model.Nudge
import kotlinx.coroutines.flow.Flow

/** Sends and receives [Nudge]s between community members. */
interface NudgeRepository {
    val incomingNudges: Flow<Nudge>
    suspend fun sendNudge(communityId: String, targetUserId: String, nudge: Nudge)
}
