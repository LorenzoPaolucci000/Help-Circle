package com.project.helpcircle.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.project.helpcircle.R
import com.project.helpcircle.presentation.common.PrimaryButton
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.StepProgressHeader
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing

/** How many steps a first-time user walks through before the app is usable. */
const val ONBOARDING_STEP_COUNT = 5

/**
 * The first screen a new user sees: what the app is for, and what it does with their data.
 *
 * Both halves matter. Before this existed the flow opened straight onto "choose a nickname", which
 * asked for something before explaining anything. The privacy half is folded in here rather than
 * given its own step because the two answer the same question — "should I trust this?" — and the
 * design this was adapted from also gated its privacy screen behind a consent checkbox, which would
 * have been a legal-looking ceremony this app doesn't need.
 *
 * Sized to fit a phone screen without scrolling: the promises are single lines rather than
 * paragraphs, so "Get started" is visible the moment the screen opens rather than hidden below the
 * fold. The column can still scroll, which is what keeps it usable at large system font sizes.
 *
 * Wording deliberately avoids jargon: no "Zero-PII", no "SQLCipher", no "accessibility service".
 * Everything claimed here is literally true of the implementation.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        StepProgressHeader(step = 1, totalSteps = ONBOARDING_STEP_COUNT)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_help_circle_logo),
                contentDescription = null,
                modifier = Modifier.size(Sizes.logo)
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Mindful together",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "A circle of people who help each other put the phone down.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        SectionCard(title = "How this works") {
            WelcomePromise(Icons.Filled.Favorite, "Peer support, not surveillance")
            Spacer(modifier = Modifier.height(Spacing.md))
            WelcomePromise(Icons.Filled.Lock, "Your activity is encrypted on this phone")
            Spacer(modifier = Modifier.height(Spacing.md))
            WelcomePromise(Icons.Filled.Tune, "You choose which apps count")
            Spacer(modifier = Modifier.height(Spacing.md))
            WelcomePromise(Icons.Filled.VisibilityOff, "No account, no ads, no real name")
        }

        PrimaryButton(text = "Get started", onClick = onGetStarted)
    }
}

/**
 * One promise this app makes. Deliberately a single line: four short claims a user can take in at a
 * glance beat four paragraphs they scroll past.
 */
@Composable
private fun WelcomePromise(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.rowIcon)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(Spacing.xl)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
