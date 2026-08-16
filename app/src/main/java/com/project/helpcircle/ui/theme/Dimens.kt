package com.project.helpcircle.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Layout tokens shared by every screen.
 *
 * Screens were each choosing their own paddings and corner radii inline, so the same structure —
 * a card, a section gap, a screen gutter — was a slightly different size on every tab. Referencing
 * these instead keeps the spacing rhythm consistent and makes a global adjustment a one-file change.
 */
object Spacing {
    /** Hairline gaps, e.g. between a label and the value directly beneath it. */
    val xs = 4.dp
    /** Related elements inside a single row or block. */
    val sm = 8.dp
    /** The default gap between siblings in a list or column. */
    val md = 12.dp
    /** Padding inside a card, and the gap between stacked cards. */
    val lg = 16.dp
    /** Roomier card padding, used by the cards that carry a screen's primary content. */
    val xl = 20.dp
    /** Separation between distinct sections of a screen. */
    val xxl = 24.dp
    /** Leading/trailing gutter of a screen's content column. */
    val screenHorizontal = 20.dp
    /** Top gap between the system bars and a screen's header. */
    val screenTop = 20.dp
    /**
     * Trailing gap at the bottom of a scrolling column. Deliberately generous: content sitting
     * under the bottom navigation bar has been clipped repeatedly in this project, and the last
     * card needs to clear it even when the column is scrolled fully down.
     */
    val screenBottom = 32.dp
}

/** Corner radii. The mockups lean on large, soft radii, with pill shapes for buttons and chips. */
object Radius {
    val sm = 8.dp
    val md = 12.dp
    /** Standard content card. */
    val lg = 20.dp
    /** Hero/feature cards that dominate a screen. */
    val xl = 28.dp
}

object Shapes {
    val card = RoundedCornerShape(Radius.lg)
    val heroCard = RoundedCornerShape(Radius.xl)
    val field = RoundedCornerShape(Radius.md)
    /** Fully rounded: primary buttons, chips, and segmented/fill bars. */
    val pill = RoundedCornerShape(percent = 50)
}

object Elevation {
    /** Cards sit flat on a tinted background and are separated by color, not shadow. */
    val card = 0.dp
    val raisedCard = 2.dp
    val dialog = 6.dp
}

/** Fixed sizes for the recurring small visual elements. */
object Sizes {
    /** The coarse status dot next to a peer's nickname. */
    val statusDot = 12.dp
    /** Height of a continuous fill bar (charge wallet, circle mood). */
    val barHeight = 10.dp
    /** Height of one segment in a segmented bar. */
    val segmentHeight = 8.dp
    /** Stroke width of the circular index ring. */
    val ringStroke = 12.dp
    /** Diameter of the circular index ring. */
    val ringSize = 140.dp
    /** Leading icon inside a list row or feature row. */
    val rowIcon = 40.dp
    /** Centred hero icon on onboarding screens. */
    val heroIcon = 56.dp
    /** The app logo, shown at the top of the welcome screen. */
    val logo = 88.dp
    /**
     * Fixed height for the Help tab's intervention buttons. Fixed on purpose: only the progressive
     * intervention carries an intensity indicator, so leaving the buttons to size themselves makes
     * one of the four visibly taller than the rest.
     */
    val interventionButton = 132.dp
    /** One pip of an intensity indicator. */
    val intensityPip = 8.dp
}
