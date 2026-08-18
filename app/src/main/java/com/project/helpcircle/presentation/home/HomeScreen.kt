package com.project.helpcircle.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.model.WeeklySummary
import com.project.helpcircle.presentation.common.EmptyState
import com.project.helpcircle.presentation.common.IndexRing
import com.project.helpcircle.presentation.common.MetaChip
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.ScreenHeader
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.StatMiniCard
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Spacing
import com.project.helpcircle.ui.theme.trendColor
import kotlin.math.abs

/** Entry point: hoists [HomeViewModel] state and hands it to the stateless content below. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(
        uiState = uiState,
        onSatisfactionSelected = viewModel::onSatisfactionSelected,
        modifier = modifier
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onSatisfactionSelected: (WeeklySatisfaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    ScreenColumn(modifier = modifier, verticalSpacing = Spacing.lg) {
        ScreenHeader(
            overline = "Just for you",
            title = "Your agency",
            // Literally true of everything on this screen: IA_ind and the weekly history are
            // computed and stored on the device and never published to the circle.
            trailing = { MetaChip(text = "Private · not shared") }
        )

        PersonalIndexCard(
            currentAgencyIndex = uiState.currentAgencyIndex,
            weeklyDelta = uiState.latestWeeklySummary?.agencyIndexDelta
        )

        WeeklyTrendCard(weeklyDeltasOldestFirst = uiState.weeklyDeltasOldestFirst)

        // Placed before the retrospective card deliberately: this asks about the week in progress,
        // whereas "Last week" recaps a finished one.
        WeeklySatisfactionCard(
            selected = uiState.currentWeekSatisfaction,
            isSubmitting = uiState.isSubmittingSatisfaction,
            error = uiState.satisfactionError,
            onSelect = onSatisfactionSelected
        )

        LatestWeeklySummaryCard(
            summary = uiState.latestWeeklySummary,
            previousSummary = uiState.previousWeeklySummary
        )
    }
}

/** This device's own IA_ind — never a peer's — with a plain-language note on what it measures. */
@Composable
private fun PersonalIndexCard(
    currentAgencyIndex: Int,
    weeklyDelta: Int?,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        title = "Agency index",
        subtitle = "Your digital autonomy · IA_ind",
        trailing = weeklyDelta?.let { delta ->
            {
                MetaChip(
                    text = if (delta >= 0) "+$delta pts" else "$delta pts",
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
            IndexRing(value = currentAgencyIndex)
            Text(
                text = "IA_ind measures how intentionally you use your phone, your choices, not " +
                    "just your habits. It starts at 50 and resets every week.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A bar-per-week trend: bar height reflects that week's IA_ind delta magnitude, color its sign.
 *
 * Deliberately per-week rather than per-day — the underlying history is one summary per completed
 * week, so a daily chart would have nothing real to plot.
 */
@Composable
private fun WeeklyTrendCard(weeklyDeltasOldestFirst: List<Int>, modifier: Modifier = Modifier) {
    SectionCard(
        modifier = modifier,
        title = "Weekly trend",
        subtitle = "How each finished week moved your index"
    ) {
        if (weeklyDeltasOldestFirst.isEmpty()) {
            EmptyState(
                title = "No weekly history yet",
                detail = "Your first bar appears after the weekly reset on Sunday night."
            )
            return@SectionCard
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            val maxMagnitude = weeklyDeltasOldestFirst.maxOf { abs(it) }.coerceAtLeast(1)
            weeklyDeltasOldestFirst.forEach { delta ->
                val barHeight = (abs(delta).toFloat() / maxMagnitude * 64).coerceAtLeast(6f)
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(barHeight.dp)
                        .clip(Shapes.field)
                        .background(trendColor(delta))
                )
            }
        }
    }
}

/**
 * The weekly self-evaluation: one tap on an emoji records how the user feels the week in progress
 * is going. Re-tapping a different face simply replaces the answer, so there's no separate submit
 * step and no way to get stuck with an answer you regret.
 */
@Composable
private fun WeeklySatisfactionCard(
    selected: WeeklySatisfaction?,
    isSubmitting: Boolean,
    error: String?,
    onSelect: (WeeklySatisfaction) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        title = "How is this week going?",
        subtitle = if (selected == null) {
            "Your circle sees the shared mood, never what caused it."
        } else {
            "Shared with your circle. Tap another face to change it."
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            WeeklySatisfaction.entries.forEach { option ->
                SatisfactionOption(
                    option = option,
                    isSelected = option == selected,
                    enabled = !isSubmitting,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (error != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/** One tappable face in the weekly self-evaluation, visibly filled in once it's the chosen one. */
@Composable
private fun SatisfactionOption(
    option: WeeklySatisfaction,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(Shapes.card)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = Shapes.card
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = option.emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

private val WeeklySatisfaction.emoji: String
    get() = when (this) {
        WeeklySatisfaction.BAD -> "😞"
        WeeklySatisfaction.NEUTRAL -> "😐"
        WeeklySatisfaction.HAPPY -> "😊"
    }

private val WeeklySatisfaction.label: String
    get() = when (this) {
        WeeklySatisfaction.BAD -> "Rough"
        WeeklySatisfaction.NEUTRAL -> "So-so"
        WeeklySatisfaction.HAPPY -> "Good"
    }

/**
 * Recap of the most recently completed week: when crises peaked and which intervention helped most,
 * each set against the week before it.
 */
@Composable
private fun LatestWeeklySummaryCard(
    summary: WeeklySummary?,
    previousSummary: WeeklySummary?,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        title = "Last week",
        subtitle = "The most recent week to have finished"
    ) {
        if (summary == null) {
            EmptyState(
                title = "No weekly summary yet",
                detail = "One is written for you after each weekly reset."
            )
            return@SectionCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Labels are kept to two short words: a third of a phone's width breaks anything
            // longer mid-word, which is how "Best intervention" first rendered as "interventi/on".
            StatMiniCard(
                value = summary.peakCrisisHourLabel(),
                label = "Peak hour",
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                value = summary.interventionLabel(),
                label = "Best nudge",
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                value = summary.agencyIndexDelta.toSignedString(),
                label = "Agency trend",
                valueColor = trendColor(summary.agencyIndexDelta),
                modifier = Modifier.weight(1f)
            )
        }
        if (previousSummary != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                // One line in the same order as the three cards above, rather than a caption under
                // each, so the comparison doesn't double the height of the card.
                text = "The week before: ${previousSummary.peakCrisisHourLabel()} · " +
                    "${previousSummary.interventionLabel()} · " +
                    previousSummary.agencyIndexDelta.toSignedString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun WeeklySummary.peakCrisisHourLabel(): String =
    peakCrisisHour?.let { "$it:00" } ?: "None"

private fun WeeklySummary.interventionLabel(): String =
    mostEffectiveInterventionCategory ?: "None"

private fun Int.toSignedString(): String = if (this >= 0) "+$this" else toString()
