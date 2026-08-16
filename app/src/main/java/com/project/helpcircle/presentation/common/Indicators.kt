package com.project.helpcircle.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing
import com.project.helpcircle.ui.theme.statusColor

/**
 * A small tinted pill carrying one piece of metadata — "Private · not shared", a member count, a
 * weekly delta. Deliberately low-emphasis: a chip annotates the thing next to it rather than
 * competing with it.
 */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Text(
        text = text,
        modifier = modifier
            .clip(Shapes.pill)
            .background(containerColor)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        style = MaterialTheme.typography.labelMedium,
        color = contentColor
    )
}

/** The coarse status dot shown beside a peer's pseudonym. Never a score — only the tier. */
@Composable
fun StatusDot(status: MemberStatus, modifier: Modifier = Modifier, size: Dp = Sizes.statusDot) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(statusColor(status))
    )
}

/**
 * The user-facing wording for a coarse status. Kept here rather than on the domain enum so the
 * copy can change without touching a model the whole app depends on.
 */
fun statusLabel(status: MemberStatus): String = when (status) {
    MemberStatus.OK -> "Doing okay"
    MemberStatus.AT_RISK -> "At risk"
    MemberStatus.CRISIS -> "In a crisis"
}

/**
 * A continuous progress bar for a fraction of a whole.
 *
 * [minVisibleFraction] keeps a nonzero-but-tiny value visible as a nub rather than vanishing
 * entirely — an all-negative circle mood should read as "pinned to the bottom", not as a missing
 * element, which is a meaningfully different message.
 */
@Composable
fun FillBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = Sizes.barHeight,
    minVisibleFraction: Float = 0.03f,
    animate: Boolean = true
) {
    val target = fraction.coerceIn(0f, 1f).coerceAtLeast(minVisibleFraction)
    val animatedFraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (animate) 600 else 0),
        label = "fillBar"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(Shapes.pill)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .clip(Shapes.pill)
                .background(color)
        )
    }
}

/**
 * A circular gauge for a 0-[max] index, with the figure itself in the middle.
 *
 * [placeholder] covers the case where there is no meaningful value to draw — a circle of one member
 * has nothing to average — which shows an empty track rather than a misleading zero.
 */
@Composable
fun IndexRing(
    value: Int?,
    modifier: Modifier = Modifier,
    max: Int = 100,
    diameter: Dp = Sizes.ringSize,
    stroke: Dp = Sizes.ringStroke,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    placeholder: String = "--"
) {
    val animatedFraction by animateFloatAsState(
        targetValue = ((value ?: 0).toFloat() / max.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "indexRing"
    )
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedFraction > 0f) {
                drawArc(
                    color = color,
                    // Starts at twelve o'clock so the gauge fills clockwise from the top.
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value?.toString() ?: placeholder,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "/ $max",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A bar divided into discrete segments, for quantities that are counted rather than measured — the
 * charge wallet in particular, where "3 of 10" is a number of individual things the user spends,
 * not a percentage.
 */
@Composable
fun SegmentedBar(
    filled: Int,
    total: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = Sizes.segmentHeight
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        repeat(total.coerceAtLeast(1)) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(Shapes.pill)
                    .background(if (index < filled) color else trackColor)
            )
        }
    }
}
