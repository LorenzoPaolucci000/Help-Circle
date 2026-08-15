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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.WeeklySatisfaction
import com.project.helpcircle.domain.model.WeeklySummary
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
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            PersonalIndexDisplay(currentAgencyIndex = uiState.currentAgencyIndex)
            Spacer(modifier = Modifier.height(40.dp))
            WeeklyTrendChart(weeklyDeltasOldestFirst = uiState.weeklyDeltasOldestFirst)
            Spacer(modifier = Modifier.height(32.dp))
            // Placed before the retrospective card deliberately: this asks about the week in
            // progress, whereas "Last week" recaps a finished one.
            WeeklySatisfactionCard(
                selected = uiState.currentWeekSatisfaction,
                isSubmitting = uiState.isSubmittingSatisfaction,
                error = uiState.satisfactionError,
                onSelect = onSatisfactionSelected
            )
            Spacer(modifier = Modifier.height(16.dp))
            LatestWeeklySummaryCard(
                summary = uiState.latestWeeklySummary,
                previousSummary = uiState.previousWeeklySummary
            )
        }
    }
}

/** Large, central IA_ind score — this device's own, never a peer's. */
@Composable
private fun PersonalIndexDisplay(currentAgencyIndex: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = currentAgencyIndex.toString(),
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Your Agency Index",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A simple bar-per-week trend: bar height reflects that week's IA_ind delta magnitude, color reflects its sign. */
@Composable
private fun WeeklyTrendChart(weeklyDeltasOldestFirst: List<Int>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Weekly trend", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(12.dp))
        if (weeklyDeltasOldestFirst.isEmpty()) {
            Text(
                text = "No weekly history yet — check back after your first weekly reset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom
            ) {
                val maxMagnitude = weeklyDeltasOldestFirst.maxOf { abs(it) }.coerceAtLeast(1)
                weeklyDeltasOldestFirst.forEach { delta ->
                    val barHeight = (abs(delta).toFloat() / maxMagnitude * 52).coerceAtLeast(4f)
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (delta >= 0) Color(0xFF66BB6A) else Color(0xFFE57373))
                    )
                }
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "How is this week going?", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (selected == null) {
                    "Your circle sees the shared mood — never what caused it."
                } else {
                    "Shared with your circle. Tap another face to change it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(shape)
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
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = option.emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(4.dp))
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

/** Recap of the most recently completed week: when crises peaked and which intervention helped most, each set against the week before it. */
@Composable
private fun LatestWeeklySummaryCard(
    summary: WeeklySummary?,
    previousSummary: WeeklySummary?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Last week", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (summary == null) {
                Text(
                    text = "No weekly summary yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                WeeklyStatRow(
                    label = "Peak crisis hour",
                    value = summary.peakCrisisHour?.let { "$it:00" } ?: "No crises",
                    previousValue = previousSummary?.let { it.peakCrisisHour?.let { hour -> "$hour:00" } ?: "No crises" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                WeeklyStatRow(
                    label = "Most effective intervention",
                    value = summary.mostEffectiveInterventionCategory ?: "None",
                    previousValue = previousSummary?.let { it.mostEffectiveInterventionCategory ?: "None" }
                )
                Spacer(modifier = Modifier.height(8.dp))
                WeeklyStatRow(
                    label = "Agency trend",
                    value = summary.agencyIndexDelta.toSignedString(),
                    previousValue = previousSummary?.agencyIndexDelta?.toSignedString()
                )
            }
        }
    }
}

/** One "Last week" stat, with an optional caption comparing it to the week before that. */
@Composable
private fun WeeklyStatRow(label: String, value: String, previousValue: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = "$label: $value")
        if (previousValue != null) {
            Text(
                text = "The week before: $previousValue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Int.toSignedString(): String = if (this >= 0) "+$this" else toString()
