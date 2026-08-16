package com.project.helpcircle.presentation.help

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.presentation.common.EmptyState
import com.project.helpcircle.presentation.common.MetaChip
import com.project.helpcircle.presentation.common.ScreenColumn
import com.project.helpcircle.presentation.common.ScreenHeader
import com.project.helpcircle.presentation.common.SectionCard
import com.project.helpcircle.presentation.common.SegmentedBar
import com.project.helpcircle.presentation.common.StatusDot
import com.project.helpcircle.presentation.common.statusLabel
import com.project.helpcircle.ui.theme.Shapes
import com.project.helpcircle.ui.theme.Sizes
import com.project.helpcircle.ui.theme.Spacing
import kotlinx.coroutines.delay

/** Entry point: hoists [HelpViewModel] state and hands it to the stateless content below. */
@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HelpContent(
        uiState = uiState,
        onMemberClicked = viewModel::onMemberClicked,
        onInterventionClicked = viewModel::onInterventionClicked,
        onOptionPickerDismissed = viewModel::onOptionPickerDismissed,
        onNudgeSelected = viewModel::onNudgeSelected,
        onNudgeFeedbackShown = viewModel::onNudgeFeedbackShown,
        modifier = modifier
    )
}

/**
 * The one place a nudge can be sent from. Unlike the community dashboard's roster, this lists only
 * the peers an intervention is actually appropriate for right now — see
 * [HelpablePeers][com.project.helpcircle.domain.model.HelpablePeers].
 *
 * The flow is deliberately two-step: choose who, then choose what. The intervention buttons are
 * visible from the start so the user can see what the tab is for, but stay out of focus until a
 * peer is picked, which makes it impossible to fire one off without having chosen a recipient.
 */
@Composable
private fun HelpContent(
    uiState: HelpUiState,
    onMemberClicked: (CommunityMember) -> Unit,
    onInterventionClicked: (InterventionType) -> Unit,
    onOptionPickerDismissed: () -> Unit,
    onNudgeSelected: (Nudge) -> Unit,
    onNudgeFeedbackShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            !uiState.hasActiveCommunity -> EmptyState(
                title = "No circle yet",
                detail = "You haven't joined a community yet.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.xxl),
                centered = true
            )

            else -> ScreenColumn(verticalSpacing = Spacing.lg) {
                ScreenHeader(
                    overline = "Peer support",
                    title = "Support a peer"
                )

                ChargeWalletCard(
                    availableCharges = uiState.availableCharges,
                    maxCharges = uiState.maxCharges
                )

                PeersNeedingHelpCard(
                    peers = uiState.peersNeedingHelp,
                    totalPeerCount = uiState.totalPeerCount,
                    selectedPeer = uiState.selectedPeer,
                    onMemberClicked = onMemberClicked
                )

                InterventionsCard(
                    uiState = uiState,
                    onInterventionClicked = onInterventionClicked
                )
            }
        }

        uiState.optionPickerFor?.let { type ->
            InterventionOptionDialog(
                type = type,
                targetName = uiState.selectedPeer?.nickname.orEmpty().ifBlank { "your peer" },
                isSending = uiState.isSendingNudge,
                onNudgeSelected = onNudgeSelected,
                onDismiss = onOptionPickerDismissed
            )
        }

        uiState.nudgeFeedback?.let { feedback ->
            NudgeFeedbackBanner(
                message = feedback,
                onShown = onNudgeFeedbackShown,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.xxl)
            )
        }
    }
}

/**
 * The intervention budget, shown here because this is the only screen where it changes anything:
 * every nudge below costs charges, and they come back on their own rather than being bought.
 *
 * Drawn as discrete pips rather than a continuous bar, because a charge is a thing you spend one of
 * — "8 of 10" is a count, not a percentage.
 */
@Composable
private fun ChargeWalletCard(availableCharges: Int, maxCharges: Int, modifier: Modifier = Modifier) {
    SectionCard(
        modifier = modifier,
        title = "Intervention charges",
        subtitle = "$availableCharges of $maxCharges available",
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        trailing = {
            Text(
                text = "$availableCharges",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) {
        SegmentedBar(
            filled = availableCharges,
            total = maxCharges,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "They refill on their own — faster while you're off your monitored apps, and " +
                "back to full every night.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

/**
 * The peers a nudge is appropriate for, most urgent first.
 *
 * Each row shows a pseudonym and a coarse status and nothing else. The design this was adapted from
 * also showed *why* each peer was struggling and how long ago — neither of which exists here, and
 * neither of which could, since the only things that ever leave a device are a nickname, a status
 * tier, the derived score and a self-declared mood.
 */
@Composable
private fun PeersNeedingHelpCard(
    peers: List<CommunityMember>,
    totalPeerCount: Int,
    selectedPeer: CommunityMember?,
    onMemberClicked: (CommunityMember) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionCard(
        modifier = modifier,
        title = "Who needs support",
        subtitle = when {
            totalPeerCount == 0 -> "Nobody has joined your circle yet."
            peers.isEmpty() -> "Peers appear here while they're at risk or in a crisis."
            selectedPeer != null -> "Tap ${selectedPeer.nickname.ifBlank { "them" }} again to deselect."
            peers.size == 1 -> "Tap the peer you want to support."
            else -> "Tap the peer you want to support."
        }
    ) {
        when {
            totalPeerCount == 0 -> EmptyState(
                title = "No peers yet",
                detail = "Share your invite code from the Community tab to grow your circle."
            )

            peers.isEmpty() -> EmptyState(
                title = "Everyone is doing okay right now",
                detail = "Nothing to do — your circle will appear here the moment someone starts slipping."
            )

            // Ordering comes from the domain (crisis before at-risk), so nothing here re-sorts.
            else -> peers.forEachIndexed { index, peer ->
                if (index > 0) Spacer(modifier = Modifier.height(Spacing.sm))
                HelpablePeerRow(
                    member = peer,
                    isSelected = peer.anonymousId == selectedPeer?.anonymousId,
                    onClick = { onMemberClicked(peer) }
                )
            }
        }
    }
}

/** One peer who can be nudged: pseudonym and coarse status, the same red/amber the roster uses. */
@Composable
private fun HelpablePeerRow(
    member: CommunityMember,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Shapes.card)
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, Shapes.card)
                } else {
                    Modifier
                }
            )
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(status = member.status)
        Column(modifier = Modifier.padding(start = Spacing.lg)) {
            Text(
                text = member.nickname.ifBlank { "Someone" },
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = statusLabel(member.status),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * The four interventions, as a 2x2 grid of buttons.
 *
 * Until a peer is selected the grid is blurred and dimmed rather than hidden, so the user can see
 * what the tab offers before committing to a recipient. The dimming matters as much as the blur:
 * `Modifier.blur` is a no-op below API 31, so on older devices the alpha is the only thing left to
 * signal that these aren't ready yet.
 */
@Composable
private fun InterventionsCard(
    uiState: HelpUiState,
    onInterventionClicked: (InterventionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val unlocked = uiState.canIntervene && !uiState.isSendingNudge
    SectionCard(
        modifier = modifier,
        title = "Choose intervention",
        subtitle = uiState.selectedPeer
            ?.let { "Sending to ${it.nickname.ifBlank { "this peer" }}" }
            ?: "Pick a peer above to unlock these."
    ) {
        Column(
            modifier = Modifier
                .then(if (unlocked) Modifier else Modifier.blur(6.dp))
                .alpha(if (unlocked) 1f else 0.45f),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            InterventionType.entries.chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    rowTypes.forEach { type ->
                        InterventionButton(
                            type = type,
                            sentLevel = uiState.sentLevelOf(type),
                            enabled = unlocked &&
                                uiState.hasRemaining(type) &&
                                uiState.availableCharges >= type.chargeCost,
                            onClick = { onInterventionClicked(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * One intervention: its icon, its name, what it costs, and — for a progressive type — how far it
 * has already been escalated for the selected peer.
 */
@Composable
private fun InterventionButton(
    type: InterventionType,
    sentLevel: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = sentLevel > 0
    val contentColor = when {
        enabled && active -> MaterialTheme.colorScheme.onTertiaryContainer
        enabled -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .height(Sizes.interventionButton)
            .clip(Shapes.card)
            .background(
                when {
                    // Already-escalated buttons keep a distinct fill even once exhausted, so "I sent
                    // this three times" stays visible rather than looking merely unavailable.
                    active -> MaterialTheme.colorScheme.tertiaryContainer
                    enabled -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = type.label,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "${type.chargeCost}⚡",
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = 0.8f)
        )
        // The indicator's row is reserved on every button, not just the progressive one, so all four
        // stay the same height and their icons stay on the same line.
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (type.isProgressive) {
            IntensityPips(level = sentLevel, maxLevel = type.maxLevel, color = contentColor)
        } else {
            Spacer(modifier = Modifier.height(Sizes.intensityPip))
        }
    }
}

/**
 * The intensity indicator inside a progressive intervention's button: one pip per level, filled up
 * to the level already sent. This is the only thing telling the user that pressing grey-scale again
 * makes it stronger rather than repeating what they just sent.
 */
@Composable
private fun IntensityPips(level: Int, maxLevel: Int, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLevel) { index ->
            Box(
                modifier = Modifier
                    .size(Sizes.intensityPip)
                    .clip(CircleShape)
                    .then(
                        if (index < level) {
                            Modifier.background(color)
                        } else {
                            Modifier.border(1.dp, color.copy(alpha = 0.5f), CircleShape)
                        }
                    )
            )
        }
    }
}

/** The icon shown on an intervention's button; kept out of [InterventionType] so it stays UI-free. */
private val InterventionType.icon: ImageVector
    get() = when (this) {
        InterventionType.TEXT -> Icons.Filled.ChatBubble
        InterventionType.GREYSCALE -> Icons.Filled.FilterBAndW
        InterventionType.HAPTIC -> Icons.Filled.Vibration
        InterventionType.BLUR -> Icons.Filled.BlurOn
    }

/**
 * The follow-up for the two interventions that have variants. Haptic and content blur never reach
 * here — there is nothing further to ask, so they send on the first tap.
 */
@Composable
private fun InterventionOptionDialog(
    type: InterventionType,
    targetName: String,
    isSending: Boolean,
    onNudgeSelected: (Nudge) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.card,
        title = {
            Column {
                Text(
                    text = type.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Send to $targetName",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                type.options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.field)
                            .clickable(enabled = !isSending) { onNudgeSelected(option.nudge) }
                            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            // Weighted so a long label wraps within its own space instead of
                            // pushing the charge chip off the trailing edge.
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.padding(horizontal = Spacing.xs))
                        MetaChip(text = "${option.nudge.chargeCost}⚡")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Transient result banner for the last nudge attempt; clears itself after a short delay. */
@Composable
private fun NudgeFeedbackBanner(message: String, onShown: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        delay(2000)
        onShown()
    }
    Box(
        modifier = modifier
            .clip(Shapes.pill)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            textAlign = TextAlign.Center
        )
    }
}
