package com.project.helpcircle.ui.theme

import androidx.compose.ui.graphics.Color
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.VisualLandscape

/**
 * Colors that carry meaning rather than brand.
 *
 * These deliberately sit outside the Material color scheme: "at risk" has to read as amber and
 * "in a crisis" as red no matter what the primary/secondary palette is, so they can't be expressed
 * as scheme roles. They were previously duplicated as inline hex literals across the community,
 * help and home screens, which meant the same status could drift to a different shade depending on
 * which screen you were looking at. This is the single source of truth for them.
 *
 * The values are unchanged from what the screens used before. They were picked against a light
 * background, so each screen re-checks them in dark mode as it adopts them.
 */
object StatusColors {
    val ok = Color(0xFF66BB6A)
    val atRisk = Color(0xFFFFB74D)
    val crisis = Color(0xFFE57373)

    /** A positive weekly movement in an index; [negativeTrend] is its counterpart. */
    val positiveTrend = ok
    val negativeTrend = crisis

    /** Upper bound of the "poor" band for any 0-100 score rendered as red/amber/green. */
    const val POOR_SCORE_MAX = 33
    /** Upper bound of the "middling" band. Anything above it reads as good. */
    const val FAIR_SCORE_MAX = 66
}

/** The dot/label color for a peer's coarse status. */
fun statusColor(status: MemberStatus): Color = when (status) {
    MemberStatus.OK -> StatusColors.ok
    MemberStatus.AT_RISK -> StatusColors.atRisk
    MemberStatus.CRISIS -> StatusColors.crisis
}

/**
 * The red/amber/green band a 0-100 score falls into. Used by the circle-mood bar, and by anything
 * else that has to summarise a score as a single traffic-light color.
 */
fun scoreBandColor(score: Int): Color = when {
    score <= StatusColors.POOR_SCORE_MAX -> StatusColors.crisis
    score <= StatusColors.FAIR_SCORE_MAX -> StatusColors.atRisk
    else -> StatusColors.ok
}

/** The color of a weekly delta: green when the index improved, red when it fell. */
fun trendColor(delta: Int): Color =
    if (delta >= 0) StatusColors.positiveTrend else StatusColors.negativeTrend

/**
 * The two-stop gradient behind the community dashboard's landscape, keyed to IA_comm's band.
 * `first` is the sky (top), `second` the ground (bottom).
 */
fun landscapeGradient(landscape: VisualLandscape): Pair<Color, Color> = when (landscape) {
    VisualLandscape.TEMPEST -> Color(0xFF263238) to Color(0xFF0B0F12)
    VisualLandscape.RAINY -> Color(0xFF607D8B) to Color(0xFF455A64)
    VisualLandscape.MISTY -> Color(0xFFCFD8DC) to Color(0xFFA6B4B8)
    VisualLandscape.SERENE -> Color(0xFF81C7D4) to Color(0xFF4F9A94)
    VisualLandscape.FLOURISHING -> Color(0xFFFFE082) to Color(0xFF8BC34A)
}

/**
 * Text color legible on top of [landscapeGradient].
 *
 * The five gradients span near-black to pale yellow, so a single fixed color can't work on all of
 * them: white is unreadable on the misty and flourishing bands, and near-black is unreadable on the
 * tempest one. This is why the landscape's own content color has to be chosen per band rather than
 * inherited from the theme, which knows nothing about the gradient painted behind it.
 */
fun landscapeOnColor(landscape: VisualLandscape): Color = when (landscape) {
    VisualLandscape.TEMPEST, VisualLandscape.RAINY, VisualLandscape.SERENE -> Color.White
    VisualLandscape.MISTY, VisualLandscape.FLOURISHING -> Color(0xFF1B2A24)
}
