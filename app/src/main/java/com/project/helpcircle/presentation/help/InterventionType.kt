package com.project.helpcircle.presentation.help

import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.TextNudgeStyle

/**
 * The four kinds of intervention, as the Help screen presents them.
 *
 * This is a presentation-level grouping rather than a domain concept: [Nudge] models each sendable
 * thing individually (three text styles, three grey-scale levels, haptic, blur), which is the right
 * shape for sending but the wrong shape for choosing. A user picks "a text nudge" first and only
 * then picks which one, so the screen needs the coarser grouping.
 *
 * The three types differ in how a choice is finished:
 * - [TEXT] has variants that are alternatives, so it asks which one via a dialog.
 * - [GREYSCALE] has variants that are *intensities*, so it escalates: each press sends the next
 *   level up and costs another charge, rather than asking up front.
 * - [HAPTIC] and [BLUR] have nothing further to ask and send on the first press.
 */
enum class InterventionType(
    val label: String,
    /** What one press costs. Grey-scale charges the same at every level, so each escalation costs this again. */
    val chargeCost: Int
) {
    TEXT("Text", Nudge.Text(TextNudgeStyle.COMIC).chargeCost),
    GREYSCALE("Grey-scale", Nudge.GreyscaleLevel(level = 1).chargeCost),
    HAPTIC("Haptic", Nudge.Haptic.chargeCost),
    BLUR("Content blur", Nudge.ContentBlur.chargeCost);

    /** How many times this can be pressed for one peer before it is exhausted. */
    val maxLevel: Int
        get() = when (this) {
            GREYSCALE -> Nudge.GreyscaleLevel.MAX_LEVEL
            TEXT, HAPTIC, BLUR -> 1
        }

    /** True when pressing repeatedly escalates rather than repeating the same thing. */
    val isProgressive: Boolean get() = maxLevel > 1

    /** Alternatives to choose between in a follow-up dialog. Empty for everything that sends directly. */
    val options: List<InterventionOption>
        get() = when (this) {
            TEXT -> listOf(
                InterventionOption("Comic", Nudge.Text(TextNudgeStyle.COMIC)),
                InterventionOption("Poetic", Nudge.Text(TextNudgeStyle.POETIC)),
                InterventionOption("Severe", Nudge.Text(TextNudgeStyle.SEVERE))
            )
            GREYSCALE, HAPTIC, BLUR -> emptyList()
        }

    /**
     * What pressing this sends when [alreadySentLevel] presses have already landed for the current
     * peer, or null when there is nothing left to send ([TEXT], which asks instead; or a
     * progressive type already at full intensity).
     */
    fun nudgeAfter(alreadySentLevel: Int): Nudge? {
        val nextLevel = alreadySentLevel + 1
        if (nextLevel > maxLevel) return null
        return when (this) {
            GREYSCALE -> Nudge.GreyscaleLevel(level = nextLevel)
            HAPTIC -> Nudge.Haptic
            BLUR -> Nudge.ContentBlur
            TEXT -> null
        }
    }
}

/** One selectable variant of an [InterventionType] that offers alternatives. */
data class InterventionOption(val label: String, val nudge: Nudge)
