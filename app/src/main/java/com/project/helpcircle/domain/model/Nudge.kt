package com.project.helpcircle.domain.model

/** A peer intervention a user can send, each costing a fixed number of charges from [ChargeWallet]. */
sealed class Nudge(val chargeCost: Int) {
    data class Text(val message: String) : Nudge(chargeCost = 1)

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
