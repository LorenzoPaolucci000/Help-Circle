package com.project.helpcircle.presentation.help

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.TextNudgeStyle
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
        onNudgePickerDismissed = viewModel::onNudgePickerDismissed,
        onNudgeSelected = viewModel::onNudgeSelected,
        onNudgeFeedbackShown = viewModel::onNudgeFeedbackShown,
        modifier = modifier
    )
}

/**
 * The one place a nudge can be sent from. Unlike the community dashboard's roster, this lists only
 * the peers an intervention is actually appropriate for right now — see
 * [HelpablePeers][com.project.helpcircle.domain.model.HelpablePeers].
 */
@Composable
private fun HelpContent(
    uiState: HelpUiState,
    onMemberClicked: (CommunityMember) -> Unit,
    onNudgePickerDismissed: () -> Unit,
    onNudgeSelected: (Nudge) -> Unit,
    onNudgeFeedbackShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            !uiState.hasActiveCommunity -> Text(
                text = "You haven't joined a community yet.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                ChargeWalletCard(
                    availableCharges = uiState.availableCharges,
                    maxCharges = uiState.maxCharges
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Who needs support?", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Peers show up here while they're at risk or in a crisis. " +
                        "Tap one to send them a nudge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    uiState.totalPeerCount == 0 -> HelpEmptyState(
                        title = "No peers yet",
                        detail = "Share your invite code from the Community tab to grow your circle."
                    )

                    uiState.peersNeedingHelp.isEmpty() -> HelpEmptyState(
                        title = "Everyone is doing okay right now",
                        detail = "Nothing to do — your circle will appear here the moment someone starts slipping."
                    )

                    else -> uiState.peersNeedingHelp.forEach { peer ->
                        // Ordering comes from the domain (crisis before at-risk), so nothing here
                        // re-sorts the list.
                        HelpablePeerCard(
                            member = peer,
                            onClick = { onMemberClicked(peer) },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }

        uiState.nudgeTarget?.let { target ->
            NudgePickerDialog(
                target = target,
                availableCharges = uiState.availableCharges,
                isSending = uiState.isSendingNudge,
                onNudgeSelected = onNudgeSelected,
                onDismiss = onNudgePickerDismissed
            )
        }

        uiState.nudgeFeedback?.let { feedback ->
            NudgeFeedbackBanner(
                message = feedback,
                onShown = onNudgeFeedbackShown,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }
}

/**
 * The intervention budget, shown here because this is the only screen where it changes anything:
 * every nudge below costs charges, and they come back on their own rather than being bought.
 */
@Composable
private fun ChargeWalletCard(availableCharges: Int, maxCharges: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Your charges", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "$availableCharges of $maxCharges",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            val animatedFraction by animateFloatAsState(
                targetValue = (availableCharges.toFloat() / maxCharges).coerceIn(0f, 1f),
                animationSpec = tween(600),
                label = "chargeFill"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "They refill on their own — faster while you're off your monitored apps, " +
                    "and back to full every night.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One peer who can be nudged: pseudonym and coarse status, the same red/amber the roster uses. */
@Composable
private fun HelpablePeerCard(member: CommunityMember, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(statusColor(member.status))
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = member.nickname.ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = statusLabel(member.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Shared shape for both "nobody to help" cases, which mean very different things to the user. */
@Composable
private fun HelpEmptyState(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Matches the community roster's palette, so amber and red mean the same thing on both tabs. */
private fun statusColor(status: MemberStatus): Color = when (status) {
    MemberStatus.OK -> Color(0xFF66BB6A)
    MemberStatus.AT_RISK -> Color(0xFFFFB74D)
    MemberStatus.CRISIS -> Color(0xFFE57373)
}

private fun statusLabel(status: MemberStatus): String = when (status) {
    MemberStatus.OK -> "Doing okay"
    MemberStatus.AT_RISK -> "At risk"
    MemberStatus.CRISIS -> "In a crisis"
}

/** Lets the sender pick which [Nudge] to send [target], disabling options they can't afford. */
@Composable
private fun NudgePickerDialog(
    target: CommunityMember,
    availableCharges: Int,
    isSending: Boolean,
    onNudgeSelected: (Nudge) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send a nudge to ${target.nickname.ifBlank { "Anonymous" }}") },
        text = {
            Column {
                NudgeOption("Text: Comic", Nudge.Text(TextNudgeStyle.COMIC), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Text: Poetic", Nudge.Text(TextNudgeStyle.POETIC), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Text: Severe", Nudge.Text(TextNudgeStyle.SEVERE), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Haptic nudge", Nudge.Haptic, availableCharges, isSending, onNudgeSelected)
                NudgeOption("Grey-scale 33%", Nudge.GreyscaleLevel(level = 1), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Grey-scale 66%", Nudge.GreyscaleLevel(level = 2), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Grey-scale 100%", Nudge.GreyscaleLevel(level = 3), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Content blur", Nudge.ContentBlur, availableCharges, isSending, onNudgeSelected)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun NudgeOption(
    label: String,
    nudge: Nudge,
    availableCharges: Int,
    isSending: Boolean,
    onNudgeSelected: (Nudge) -> Unit
) {
    TextButton(
        onClick = { onNudgeSelected(nudge) },
        enabled = !isSending && availableCharges >= nudge.chargeCost,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("$label (${nudge.chargeCost} charge${if (nudge.chargeCost == 1) "" else "s"})")
    }
}

/** Transient result banner for the last nudge attempt; clears itself after a short delay. */
@Composable
private fun NudgeFeedbackBanner(message: String, onShown: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(message) {
        delay(2000)
        onShown()
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Text(text = message, modifier = Modifier.padding(16.dp))
    }
}
