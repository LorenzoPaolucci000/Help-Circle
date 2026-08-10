package com.project.helpcircle.domain.model

/** A peer intervention a user can send, each costing a fixed number of charges from [ChargeWallet]. */
sealed class Nudge(val chargeCost: Int) {
    data class Text(val message: String) : Nudge(chargeCost = 1)

    data class GreyscaleLevel(val level: Int) : Nudge(chargeCost = level) {
        init {
            require(level >= 1) { "Grey-scale level must be at least 1" }
        }
    }

    data object Haptic : Nudge(chargeCost = 2)

    data object ContentBlur : Nudge(chargeCost = 4)
}
