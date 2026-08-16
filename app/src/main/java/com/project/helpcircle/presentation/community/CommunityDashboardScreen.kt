package com.project.helpcircle.presentation.community

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.CommunitySatisfaction
import com.project.helpcircle.domain.model.VisualLandscape
import com.project.helpcircle.presentation.common.EmptyState
import com.project.helpcircle.presentation.common.FillBar
import com.project.helpcircle.presentation.common.HeroCard
import com.project.helpcircle.presentation.common.IndexRing
import com.project.helpcircle.presentation.common.MetaChip
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.ScreenHeader
import com.project.helpcircle.presentation.common.SecondaryButton
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.StatusDot
import com.project.helpcircle.presentation.common.statusLabel
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing
import com.project.helpcircle.ui.theme.landscapeGradient
import com.project.helpcircle.ui.theme.landscapeOnColor
import com.project.helpcircle.ui.theme.scoreBandColor
import com.project.helpcircle.ui.theme.trendColor

/** Entry point: hoists [CommunityDashboardViewModel] state and hands it to the stateless content below. */
@Composable
fun CommunityDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: CommunityDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    CommunityDashboardContent(uiState = uiState, modifier = modifier)
}

@Composable
private fun CommunityDashboardContent(
    uiState: CommunityDashboardUiState,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        !uiState.hasActiveCommunity -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                title = "No circle yet",
                detail = "You haven't joined a community yet.",
                modifier = Modifier.padding(Spacing.xxl),
                centered = true
            )
        }

        else -> ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
            ScreenHeader(
                overline = "Your circle",
                title = uiState.inviteCode.ifBlank { "Community" },
                trailing = {
                    MetaChip(text = if (uiState.isSolo) "Just you" else "${uiState.members.size} members")
                }
            )

            LandscapeHero(
                landscape = uiState.visualLandscape,
                collectiveIndex = if (uiState.isSolo) null else uiState.collectiveIndex
            )

            if (uiState.isSolo) {
                InviteCodeCard(inviteCode = uiState.inviteCode)
            } else {
                CollectiveIndexCard(
                    collectiveIndex = uiState.collectiveIndex,
                    latestWeekly = uiState.latestWeeklyCollectiveIndex,
                    previousWeekly = uiState.previousWeeklyCollectiveIndex
                )
                if (uiState.communitySatisfaction != null) {
                    CommunitySatisfactionCard(satisfaction = uiState.communitySatisfaction)
                }
            }

            MemberRosterCard(members = uiState.members, isSolo = uiState.isSolo)
        }
    }
}

/**
 * The community's shared mood, as a gradient landscape band keyed to [VisualLandscape].
 *
 * This used to be painted across the whole screen, which forced every other element on the
 * dashboard to draw itself in hardcoded white so it would stay legible on top. Confining it to its
 * own card lets the rest of the screen use ordinary theme colors, which is what makes the dashboard
 * work in dark mode at all.
 */
@Composable
private fun LandscapeHero(
    landscape: VisualLandscape,
    collectiveIndex: Int?,
    modifier: Modifier = Modifier
) {
    val (topColor, bottomColor) = landscapeGradient(landscape)
    val animatedTop by animateColorAsState(targetValue = topColor, animationSpec = tween(800), label = "landscapeTop")
    val animatedBottom by animateColorAsState(targetValue = bottomColor, animationSpec = tween(800), label = "landscapeBottom")
    val onLandscape = landscapeOnColor(landscape)

    HeroCard(
        modifier = modifier,
        brush = Brush.verticalGradient(colors = listOf(animatedTop, animatedBottom)),
        contentColor = onLandscape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                MetaChip(
                    text = landscapeLabel(landscape),
                    containerColor = onLandscape.copy(alpha = 0.18f),
                    contentColor = onLandscape
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "COLLECTIVE WELLBEING",
                    style = MaterialTheme.typography.labelMedium,
                    color = onLandscape.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = landscapePhrase(landscape, isSolo = collectiveIndex == null),
                    style = MaterialTheme.typography.titleLarge,
                    color = onLandscape
                )
            }
            if (collectiveIndex != null) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Column(
                    modifier = Modifier
                        .background(
                            color = onLandscape.copy(alpha = 0.18f),
                            shape = Shapes.pill
                        )
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = collectiveIndex.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = onLandscape
                    )
                    Text(
                        text = "/ 100",
                        style = MaterialTheme.typography.labelSmall,
                        color = onLandscape.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

/**
 * IA_comm as a ring, with the weekly comparison beneath it.
 *
 * The trend is built from snapshots this device recorded locally — see
 * [ObserveCommunityWeeklyTrendUseCase][com.project.helpcircle.domain.usecase.ObserveCommunityWeeklyTrendUseCase]
 * for why a peer's device can hold a different number for the same week.
 */
@Composable
private fun CollectiveIndexCard(
    collectiveIndex: Int,
    latestWeekly: Int?,
    previousWeekly: Int?,
    modifier: Modifier = Modifier
) {
    val weeklyDelta = if (latestWeekly != null && previousWeekly != null) latestWeekly - previousWeekly else null
    SectionCard(
        modifier = modifier,
        title = "Collective agency score",
        subtitle = "This week · IA_comm",
        trailing = weeklyDelta?.let { delta ->
            {
                MetaChip(
                    text = if (delta >= 0) "+$delta pts this week" else "$delta pts this week",
                    containerColor = trendColor(delta).copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            IndexRing(value = collectiveIndex)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "The average of everyone's agency index, plus a bonus when the whole " +
                        "circle is holding steady.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (latestWeekly != null) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "Last week: $latestWeekly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (previousWeekly != null) {
                        Text(
                            text = "The week before: $previousWeekly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * The circle's collective self-evaluation for the week in progress: a bar whose fill and color both
 * track the average of the members who rated it.
 *
 * Deliberately distinct from the IA_comm number above — that one is derived from detected behavior,
 * this one from what people say about themselves — so the two are never merged into a single figure
 * even though both land on a 0-100 scale.
 */
@Composable
private fun CommunitySatisfactionCard(
    satisfaction: CommunitySatisfaction,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        title = "Circle mood",
        subtitle = "How the week in progress feels, in everyone's own words"
    ) {
        if (!satisfaction.hasRatings) {
            EmptyState(
                title = "Nobody has rated this week yet",
                detail = "Ratings appear here as people fill them in on their own Me tab."
            )
            return@SectionCard
        }

        val animatedColor by animateColorAsState(
            targetValue = scoreBandColor(satisfaction.averageScore),
            animationSpec = tween(600),
            label = "satisfactionColor"
        )
        FillBar(
            fraction = satisfaction.averageScore / 100f,
            color = animatedColor,
            height = Sizes.barHeight
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            // The denominator is the whole circle, not just the raters, so silence stays visible
            // rather than one happy member rendering the same full bar as five.
            text = "${satisfactionFace(satisfaction.averageScore)}  " +
                "${satisfaction.ratedMemberCount} of ${satisfaction.memberCount} rated",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shown in place of the score while this device is the community's only member. */
@Composable
private fun InviteCodeCard(inviteCode: String, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    SectionCard(
        modifier = modifier,
        title = "Invite your circle",
        subtitle = "Peer support switches on as soon as somebody joins you.",
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = inviteCode,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        SecondaryButton(
            text = "Copy code",
            onClick = { clipboardManager.setText(AnnotatedString(inviteCode)) }
        )
    }
}

/**
 * Peer roster: pseudonym and coarse status per member, this community's whole "safe place" identity.
 * Display-only — intervening on a peer happens from the Help screen, which lists just the peers a
 * nudge is actually appropriate for.
 */
@Composable
private fun MemberRosterCard(
    members: List<CommunityMember>,
    isSolo: Boolean,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        title = "Who's here",
        subtitle = if (isSolo) null else "${members.size} in this circle"
    ) {
        if (isSolo || members.isEmpty()) {
            EmptyState(
                title = "No peers yet",
                detail = "Share your invite code to grow your circle."
            )
            return@SectionCard
        }
        members.forEachIndexed { index, member ->
            if (index > 0) Spacer(modifier = Modifier.height(Spacing.md))
            MemberRow(member = member)
        }
    }
}

@Composable
private fun MemberRow(member: CommunityMember, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(status = member.status)
        Column(modifier = Modifier.padding(start = Spacing.lg)) {
            Text(
                text = member.nickname.ifBlank { "Anonymous" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = statusLabel(member.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun landscapeLabel(landscape: VisualLandscape): String = when (landscape) {
    VisualLandscape.TEMPEST -> "Tempest"
    VisualLandscape.RAINY -> "Rainy"
    VisualLandscape.MISTY -> "Misty"
    VisualLandscape.SERENE -> "Serene"
    VisualLandscape.FLOURISHING -> "Flourishing"
}

/** A plain-language reading of the landscape, so the band means something without a legend. */
private fun landscapePhrase(landscape: VisualLandscape, isSolo: Boolean): String = when {
    isSolo -> "Your circle is waiting for you"
    landscape == VisualLandscape.TEMPEST -> "Your circle is struggling"
    landscape == VisualLandscape.RAINY -> "Your circle is having a hard week"
    landscape == VisualLandscape.MISTY -> "Your circle is finding its footing"
    landscape == VisualLandscape.SERENE -> "Your circle is doing well"
    else -> "Your circle is thriving"
}

/** The face the circle's average lands closest to, matching the three the home screen offers. */
private fun satisfactionFace(averageScore: Int): String = when {
    averageScore <= 33 -> "😞"
    averageScore <= 66 -> "😐"
    else -> "😊"
}
