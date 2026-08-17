package com.project.helpcircle.domain.model

import kotlin.random.Random

/**
 * Suggests a name for a newly created community, for a creator who'd rather not think of one.
 *
 * Kept as a pure object here rather than following the user-nickname generator's shape (which reads
 * its word lists from string-array resources and therefore needs a Context): a community name is
 * chosen entirely inside the domain layer, so hardcoding the lists keeps this JVM-testable.
 *
 * The word lists are deliberately separate from the ones people are named from, so a circle and one
 * of its members can never end up sharing a name. Every word is letters-only and the suffix is two
 * digits, so the result always satisfies the same 3-20 letters-and-digits rule a nickname does.
 */
object CommunityNameGenerator {
    private val ADJECTIVES = listOf(
        "Open", "Shared", "Woven", "Rooted", "Golden",
        "Amber", "Hidden", "Northern", "Summer", "Autumn",
        "Evening", "Morning", "Lively", "Humble", "Sunlit"
    )

    private val NOUNS = listOf(
        "Harbor", "Grove", "Circle", "Hearth", "Haven",
        "Commons", "Refuge", "Lantern", "Anchor", "Beacon",
        "Cabin", "Garden", "Orbit", "Table", "Bridge"
    )

    private const val MIN_SUFFIX = 10
    private const val MAX_SUFFIX = 100

    fun generate(): String = "${ADJECTIVES.random()}${NOUNS.random()}${Random.nextInt(MIN_SUFFIX, MAX_SUFFIX)}"
}
