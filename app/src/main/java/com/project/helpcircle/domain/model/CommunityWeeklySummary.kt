package com.project.helpcircle.domain.model

/**
 * A locally-stored snapshot of this community's IA_comm at one weekly boundary, taken lazily by
 * this device the first time it observes the community after that boundary passes (same no-
 * background-job pattern as the individual [WeeklySummary]). Purely local: IA_comm itself is
 * never archived server-side, so each member's own "vs. last week" comparison reflects only what
 * this device happened to observe, not a value the community agrees on.
 */
data class CommunityWeeklySummary(
    val communityId: String,
    val weekStartEpochMillis: Long,
    val collectiveIndexValue: Int
)

/** The latest [CommunityWeeklySummary] this device has recorded for a community, and the one before it — enough to render a "vs. last week" comparison. */
data class CommunityWeeklyTrend(
    val latest: CommunityWeeklySummary?,
    val previous: CommunityWeeklySummary?
)
