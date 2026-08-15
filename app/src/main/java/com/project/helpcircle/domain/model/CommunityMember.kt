package com.project.helpcircle.domain.model

/**
 * A community peer as shown to other members: a pseudonym, coarse status, and [agencyScore] — the
 * derived 0-100 IA_ind value (never raw behavioral data), shared only within the same community
 * so [CommunityState.collectiveIndex] can be computed from real per-member scores.
 *
 * [satisfaction] is the peer's own self-declared rating of how their week is going, and
 * [satisfactionWeekStartEpochMillis] records which week it was submitted for — both null until
 * they rate a week. The week stamp is what lets [CommunitySatisfaction.from] ignore a rating left
 * over from a previous week instead of counting it toward the current one.
 */
data class CommunityMember(
    val anonymousId: String,
    val nickname: String,
    val status: MemberStatus,
    val agencyScore: Int,
    val satisfaction: WeeklySatisfaction? = null,
    val satisfactionWeekStartEpochMillis: Long? = null
)
