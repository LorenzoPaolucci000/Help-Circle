package com.project.helpcircle.domain.model

/** Fixed text-nudge presets — not free text, so senders can't be quoted saying something unintended. */
enum class TextNudgeStyle(val message: String) {
    COMIC("Your thumb is begging for mercy!"),
    POETIC("The world outside the screen is in full bloom"),
    SEVERE("Stop scrolling and close the app immediately")
}

/** A peer intervention a user can send, each costing a fixed number of charges from [ChargeWallet]. */
sealed class Nudge(val chargeCost: Int) {
    data class Text(val style: TextNudgeStyle) : Nudge(chargeCost = 1)

    /** Progressive desaturation, in 3 fixed steps (33%/66%/100%), each costing 1 charge regardless of level. */
    data class GreyscaleLevel(val level: Int) : Nudge(chargeCost = 1) {
        init {
            require(level in 1..MAX_LEVEL) { "Grey-scale level must be 1-$MAX_LEVEL (33%/66%/100%)" }
        }

        companion object {
            const val MAX_LEVEL = 3
        }
    }

    data object Haptic : Nudge(chargeCost = 2)

    data object ContentBlur : Nudge(chargeCost = 4)
}

/** Coarse category label for local-only history (e.g. the weekly summary's intervention-category stats); never synced. */
val Nudge.categoryLabel: String
    get() = when (this) {
        is Nudge.Text -> "Text"
        is Nudge.GreyscaleLevel -> "Greyscale"
        Nudge.Haptic -> "Haptic"
        Nudge.ContentBlur -> "ContentBlur"
    }
