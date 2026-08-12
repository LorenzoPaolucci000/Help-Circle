package com.project.helpcircle.domain.model

/** IA_comm: the shared state of a support community, derived from its members' [AgencyIndex] values. */
data class CommunityState(
    val communityId: String,
    val memberAgencyIndices: List<AgencyIndex>,
    val cohesionBonusApplied: Boolean,
    val members: List<CommunityMember> = emptyList(),
    val inviteCode: String = ""
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
    /** 0-20: thunderstorm, lightning, dark terrain. */
    TEMPEST,

    /** 21-40: overcast, heavy rain, wind-swept. */
    RAINY,

    /** 41-60: desaturated, thin fog, still vegetation. */
    MISTY,

    /** 61-80: calm green, drifting clouds, daylight. */
    SERENE,

    /** 81-100: blooming meadow, clear sky, warm colors. */
    FLOURISHING;

    companion object {
        fun forIndex(index: AgencyIndex): VisualLandscape = when {
            index.value <= 20 -> TEMPEST
            index.value <= 40 -> RAINY
            index.value <= 60 -> MISTY
            index.value <= 80 -> SERENE
            else -> FLOURISHING
        }
    }
}
