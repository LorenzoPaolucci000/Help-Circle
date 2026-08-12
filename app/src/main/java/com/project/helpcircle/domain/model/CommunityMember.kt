package com.project.helpcircle.domain.model

/**
 * A community peer as shown to other members: a pseudonym, coarse status, and [agencyScore] — the
 * derived 0-100 IA_ind value (never raw behavioral data), shared only within the same community
 * so [CommunityState.collectiveIndex] can be computed from real per-member scores.
 */
data class CommunityMember(
    val anonymousId: String,
    val nickname: String,
    val status: MemberStatus,
    val agencyScore: Int
)
