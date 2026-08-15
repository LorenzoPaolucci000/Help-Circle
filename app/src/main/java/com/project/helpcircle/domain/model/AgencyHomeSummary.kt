package com.project.helpcircle.domain.model

/**
 * What [com.project.helpcircle.domain.usecase.ObserveAgencyHomeUseCase] emits for the personal
 * "Me" home screen: the live IA_ind alongside this device's locally-stored weekly history, oldest
 * first so it can be plotted as a trend line.
 */
data class AgencyHomeSummary(
    val currentIndex: AgencyIndex,
    val weeklySummariesOldestFirst: List<WeeklySummary>
) {
    /** The most recently generated weekly recap, or null before the first weekly reset has ever happened. */
    val latestWeeklySummary: WeeklySummary? get() = weeklySummariesOldestFirst.lastOrNull()

    /** The week before [latestWeeklySummary], so the Home screen can show a "vs. last week" comparison; null if fewer than two weeks of history exist yet. */
    val previousWeeklySummary: WeeklySummary? get() = weeklySummariesOldestFirst.dropLast(1).lastOrNull()
}
