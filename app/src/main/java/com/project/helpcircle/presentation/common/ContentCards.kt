package com.project.helpcircle.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.project.helpcircle.ui.theme.Elevation
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing

/**
 * The standard content card: a soft tinted surface with a generous inset, optionally introduced by
 * a [SectionHeader].
 *
 * Container and content colors are always supplied as a matched pair from the color scheme. That
 * matters more than it looks: cards used to set a hardcoded container color while letting their
 * text inherit the scheme's `onSurface`, which rendered as white-on-white the moment dark mode
 * became reachable. Passing a [containerColor] without its matching content color is the one thing
 * this component is designed to prevent.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: PaddingValues = PaddingValues(Spacing.xl),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            if (title != null) {
                SectionHeader(title = title, subtitle = subtitle, trailing = trailing)
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            content()
        }
    }
}

/**
 * A tinted hero card for the one element that dominates a screen. Larger radius than [SectionCard]
 * and no default section header, since a hero usually composes its own layout.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    /** Painted over [containerColor] when set, for a hero whose background is a gradient. */
    brush: Brush? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.heroCard,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)
    ) {
        Column(
            modifier = if (brush != null) Modifier.background(brush) else Modifier,
            content = content
        )
    }
}

/**
 * One cell of the small three-across statistics row: a leading glyph, the figure itself, and a
 * caption naming it. Intended to be given `Modifier.weight(1f)` by the calling Row.
 */
@Composable
fun StatMiniCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    leading: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        shape = Shapes.card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (leading != null) {
                Text(text = leading, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * The shared shape for "there is nothing here", which in this app is usually good news rather than
 * an error — hence a headline plus an explanatory line rather than a bare message.
 */
@Composable
fun EmptyState(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
    }
}

/**
 * The scheme's matching "on" color for a container color.
 *
 * Material's own `contentColorFor` only knows the roles it defines and silently returns
 * `Color.Unspecified` for anything else, which is how a card ends up drawing text in an inherited
 * color that doesn't suit its background. This falls back to `onSurface`, which is always legible
 * on the surface family the cards here use.
 */
@Composable
private fun contentColorFor(containerColor: Color): Color {
    val scheme = MaterialTheme.colorScheme
    return when (containerColor) {
        scheme.primaryContainer -> scheme.onPrimaryContainer
        scheme.secondaryContainer -> scheme.onSecondaryContainer
        scheme.tertiaryContainer -> scheme.onTertiaryContainer
        scheme.errorContainer -> scheme.onErrorContainer
        scheme.surfaceVariant -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }
}
