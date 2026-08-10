package com.project.helpcircle.domain.model

/** IA_comm: the shared state of a support community, derived from its members' [AgencyIndex] values. */
data class CommunityState(
    val communityId: String,
    val memberAgencyIndices: List<AgencyIndex>,
    val cohesionBonusApplied: Boolean
) {
    val collectiveIndex: AgencyIndex
        get() {
            if (memberAgencyIndices.isEmpty()) return AgencyIndex.baseline()
            val average = memberAgencyIndices.sumOf { it.value } / memberAgencyIndices.size
            val bonus = if (cohesionBonusApplied) COHESION_BONUS else 0
            return AgencyIndex.of(average + bonus)
        }

    val visualLandscape: VisualLandscape
        get() = VisualLandscape.forIndex(collectiveIndex)

    companion object {
        const val COHESION_BONUS = 5
    }
}

/** The shared UI's visual theme, driven by [CommunityState.collectiveIndex]. */
enum class VisualLandscape {
    STORM,
    OVERCAST,
    CALM,
    BLOOMING_MEADOW;

    companion object {
        fun forIndex(index: AgencyIndex): VisualLandscape = when {
            index.value < 25 -> STORM
            index.value < 50 -> OVERCAST
            index.value < 75 -> CALM
            else -> BLOOMING_MEADOW
        }
    }
}
