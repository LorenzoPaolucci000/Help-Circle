package com.project.helpcircle.domain.model

/** IA_ind: the user's individual agency score (0-100), always kept in range via [of]. */
@JvmInline
value class AgencyIndex private constructor(val value: Int) {

    fun apply(delta: Int): AgencyIndex = of(value + delta)

    companion object {
        const val MIN = 0
        const val MAX = 100
        const val BASELINE = 50

        fun of(value: Int): AgencyIndex = AgencyIndex(value.coerceIn(MIN, MAX))

        fun baseline(): AgencyIndex = of(BASELINE)

        /** IA_ind base formula: Clamp(50 + Delta_Autonomy + Delta_Support, 0, 100). */
        fun calculate(deltaAutonomy: Int, deltaSupport: Int): AgencyIndex =
            of(BASELINE + deltaAutonomy + deltaSupport)
    }
}
