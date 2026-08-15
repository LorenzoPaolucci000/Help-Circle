package com.project.helpcircle.domain.model

/**
 * The community-wide aggregate of its members' [WeeklySatisfaction] ratings for one specific week,
 * shown on the community dashboard as a shared indicator of how the circle is collectively doing.
 *
 * Deliberately separate from [CommunityState.collectiveIndex]: IA_comm is derived from detected
 * behavior, whereas this is what members say about themselves, so the two are never blended.
 */
data class CommunitySatisfaction(
    /** How many members have rated the week in question. */
    val ratedMemberCount: Int,
    /** How many members the circle has in total, rated or not — the denominator for "3 of 5 rated". */
    val memberCount: Int,
    /** Mean [WeeklySatisfaction.score] across the rated members, 0-100; meaningless unless [hasRatings]. */
    val averageScore: Int
) {
    /** Whether anyone has rated this week yet; the dashboard shows a prompt instead of a value when false. */
    val hasRatings: Boolean get() = ratedMemberCount > 0

    companion object {
        /**
         * Aggregates the ratings [members] have published for the week starting at
         * [currentWeekStartEpochMillis]. A member's rating only counts when it was submitted for
         * that exact week: a stale rating left over from a previous week is ignored rather than
         * carried forward, so the indicator always reflects the week actually in progress.
         */
        fun from(members: List<CommunityMember>, currentWeekStartEpochMillis: Long): CommunitySatisfaction {
            val ratings = members.mapNotNull { member ->
                member.satisfaction.takeIf { member.satisfactionWeekStartEpochMillis == currentWeekStartEpochMillis }
            }
            return CommunitySatisfaction(
                ratedMemberCount = ratings.size,
                memberCount = members.size,
                averageScore = if (ratings.isEmpty()) 0 else ratings.sumOf { it.score } / ratings.size
            )
        }
    }
}
