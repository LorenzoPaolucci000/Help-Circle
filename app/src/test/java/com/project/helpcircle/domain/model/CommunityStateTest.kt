package com.project.helpcircle.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

private fun state(values: List<Int>, cohesionBonusApplied: Boolean) = CommunityState(
    communityId = "comm-1",
    memberAgencyIndices = values.map { AgencyIndex.of(it) },
    cohesionBonusApplied = cohesionBonusApplied
)

class CommunityStateTest {

    @Test
    fun `collective index falls back to baseline when there are no members`() {
        assertEquals(AgencyIndex.baseline(), state(emptyList(), cohesionBonusApplied = false).collectiveIndex)
    }

    @Test
    fun `collective index averages member scores with no cohesion bonus`() {
        val collectiveIndex = state(listOf(40, 60), cohesionBonusApplied = false).collectiveIndex

        assertEquals(50, collectiveIndex.value)
    }

    @Test
    fun `collective index adds the cohesion bonus when it's applied`() {
        val collectiveIndex = state(listOf(40, 60), cohesionBonusApplied = true).collectiveIndex

        assertEquals(50 + CommunityState.COHESION_BONUS, collectiveIndex.value)
    }

    @Test
    fun `collective index averaging truncates rather than rounds`() {
        val collectiveIndex = state(listOf(50, 51), cohesionBonusApplied = false).collectiveIndex

        assertEquals(50, collectiveIndex.value)
    }

    @Test
    fun `collective index clamps at 100 even with the cohesion bonus pushing it over`() {
        val collectiveIndex = state(listOf(100, 100), cohesionBonusApplied = true).collectiveIndex

        assertEquals(AgencyIndex.MAX, collectiveIndex.value)
    }

    @Test
    fun `visual landscape buckets follow the collective index thresholds`() {
        assertEquals(VisualLandscape.TEMPEST, VisualLandscape.forIndex(AgencyIndex.of(0)))
        assertEquals(VisualLandscape.TEMPEST, VisualLandscape.forIndex(AgencyIndex.of(20)))
        assertEquals(VisualLandscape.RAINY, VisualLandscape.forIndex(AgencyIndex.of(21)))
        assertEquals(VisualLandscape.RAINY, VisualLandscape.forIndex(AgencyIndex.of(40)))
        assertEquals(VisualLandscape.MISTY, VisualLandscape.forIndex(AgencyIndex.of(41)))
        assertEquals(VisualLandscape.MISTY, VisualLandscape.forIndex(AgencyIndex.of(60)))
        assertEquals(VisualLandscape.SERENE, VisualLandscape.forIndex(AgencyIndex.of(61)))
        assertEquals(VisualLandscape.SERENE, VisualLandscape.forIndex(AgencyIndex.of(80)))
        assertEquals(VisualLandscape.FLOURISHING, VisualLandscape.forIndex(AgencyIndex.of(81)))
        assertEquals(VisualLandscape.FLOURISHING, VisualLandscape.forIndex(AgencyIndex.of(100)))
    }

    @Test
    fun `a community's visual landscape is derived from its own collective index`() {
        val communityState = state(listOf(90, 90), cohesionBonusApplied = true)

        assertEquals(VisualLandscape.FLOURISHING, communityState.visualLandscape)
    }
}
