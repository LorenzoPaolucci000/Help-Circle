package com.project.helpcircle.domain.model

import java.time.Instant
import java.time.ZoneId

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

    /**
     * Applies passive replenishment for the time elapsed since [lastReplenishedAtEpochMillis], as
     * of [nowEpochMillis]: a full reset to [MAX_CHARGES] if a local midnight has passed since the
     * last touch, otherwise one charge per elapsed [FOCUS_MODE_INTERVAL_MILLIS] (while [isFocusMode]
     * is true) or [BASELINE_INTERVAL_MILLIS] (otherwise), capped at [MAX_CHARGES].
     */
    fun replenished(
        nowEpochMillis: Long,
        isFocusMode: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ChargeWallet {
        if (nowEpochMillis <= lastReplenishedAtEpochMillis) return this

        val lastDay = Instant.ofEpochMilli(lastReplenishedAtEpochMillis).atZone(zoneId).toLocalDate()
        val nowDay = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        if (nowDay.isAfter(lastDay)) {
            val todayMidnightMillis = nowDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
            return ChargeWallet(currentCharges = MAX_CHARGES, lastReplenishedAtEpochMillis = todayMidnightMillis)
        }

        val intervalMillis = if (isFocusMode) FOCUS_MODE_INTERVAL_MILLIS else BASELINE_INTERVAL_MILLIS
        val elapsedIntervals = (nowEpochMillis - lastReplenishedAtEpochMillis) / intervalMillis
        if (elapsedIntervals <= 0) return this
        return copy(
            currentCharges = (currentCharges + elapsedIntervals.toInt()).coerceAtMost(MAX_CHARGES),
            lastReplenishedAtEpochMillis = lastReplenishedAtEpochMillis + elapsedIntervals * intervalMillis
        )
    }

    companion object {
        const val MAX_CHARGES = 10
        const val BASELINE_INTERVAL_MILLIS = 60 * 60 * 1000L
        const val FOCUS_MODE_INTERVAL_MILLIS = 30 * 60 * 1000L
    }
}
