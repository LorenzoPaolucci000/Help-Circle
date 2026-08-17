package com.project.helpcircle.domain.usecase

import com.project.helpcircle.domain.model.CommunityNameGenerator
import com.project.helpcircle.domain.model.CommunityState
import com.project.helpcircle.domain.model.InviteCodeGenerator
import com.project.helpcircle.domain.repository.CommunityRepository
import com.project.helpcircle.domain.repository.MonitoredAppsRepository
import java.util.UUID
import kotlinx.coroutines.flow.first

/**
 * Creates a new community with a freshly generated invite code and joins the caller into it.
 * Requires at least one monitored app first, same rationale as [JoinCommunityByInviteCodeUseCase].
 *
 * A create attempt that times out client-side isn't actually cancelled server-side — the
 * underlying write can still land later. If the caller retries, this reuses the same community ID,
 * invite code and name as the attempt being retried (all cleared only once a call actually
 * succeeds) instead of generating fresh ones, so a late-landing original write and a retry both
 * target the same document rather than risking an orphaned duplicate community.
 */
class CreateCommunityUseCase(
    private val communityRepository: CommunityRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository
) {
    private var pendingCommunityId: String? = null
    private var pendingInviteCode: String? = null
    private var pendingName: String? = null

    /**
     * [name] is the circle's chosen name. A blank one falls back to a generated suggestion rather
     * than being written through: callers are expected to have validated it, and a circle with no
     * name at all would be indistinguishable from the ones that predate names existing.
     */
    suspend operator fun invoke(name: String = ""): CommunityState {
        check(monitoredAppsRepository.monitoredPackageNames.first().isNotEmpty()) {
            "Add at least one app to monitor first"
        }
        val communityId = pendingCommunityId ?: UUID.randomUUID().toString().also { pendingCommunityId = it }
        val inviteCode = pendingInviteCode ?: InviteCodeGenerator.generate().also { pendingInviteCode = it }
        val communityName = pendingName
            ?: name.trim().ifBlank { CommunityNameGenerator.generate() }.also { pendingName = it }
        val state = communityRepository.createCommunity(communityId, inviteCode, communityName)
        pendingCommunityId = null
        pendingInviteCode = null
        pendingName = null
        return state
    }
}
