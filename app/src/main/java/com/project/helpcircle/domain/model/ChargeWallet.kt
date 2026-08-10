package com.project.helpcircle.domain.model

/** A user's balance of intervention charges (max 10), spent on [Nudge]s and replenished over time. */
data class ChargeWallet(
    val currentCharges: Int,
    val lastReplenishedAtEpochMillis: Long
) {
    fun canAfford(nudge: Nudge): Boolean = currentCharges >= nudge.chargeCost

    fun spend(nudge: Nudge): ChargeWallet {
        require(canAfford(nudge)) { "Not enough charges for ${nudge::class.simpleName}" }
        return copy(currentCharges = currentCharges - nudge.chargeCost)
    }

    fun replenish(amount: Int, atEpochMillis: Long): ChargeWallet = copy(
        currentCharges = (currentCharges + amount).coerceAtMost(MAX_CHARGES),
        lastReplenishedAtEpochMillis = atEpochMillis
    )

    companion object {
        const val MAX_CHARGES = 10
    }
}
