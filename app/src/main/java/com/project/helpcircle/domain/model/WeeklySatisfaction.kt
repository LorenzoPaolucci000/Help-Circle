package com.project.helpcircle.domain.model

/**
 * How satisfied a user reports feeling over the currently-running week — a self-declared,
 * three-way rating collected once per week alongside the weekly summary report.
 *
 * [score] maps each choice onto the same 0-100 range every other index in this project uses, so a
 * community's ratings can be averaged into a single comparable value (see [CommunitySatisfaction])
 * without the presentation layer having to invent its own weighting.
 *
 * This is a self-reported feeling, not behavioral data: sharing it with peers carries no usage
 * information whatsoever, so it stays within the Zero-PII rule the member roster already follows.
 */
enum class WeeklySatisfaction(val score: Int) {
    /** The week went badly. */
    BAD(0),

    /** Neither good nor bad. */
    NEUTRAL(50),

    /** The week went well. */
    HAPPY(100)
}
