package com.project.helpcircle.presentation.community

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.CommunityMember
import com.project.helpcircle.domain.model.MemberStatus
import com.project.helpcircle.domain.model.Nudge
import com.project.helpcircle.domain.model.VisualLandscape
import kotlinx.coroutines.delay

/** Entry point: hoists [CommunityDashboardViewModel] state and hands it to the stateless content below. */
@Composable
fun CommunityDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: CommunityDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    CommunityDashboardContent(
        uiState = uiState,
        onMemberClicked = viewModel::onMemberClicked,
        onNudgePickerDismissed = viewModel::onNudgePickerDismissed,
        onNudgeSelected = viewModel::onNudgeSelected,
        onNudgeFeedbackShown = viewModel::onNudgeFeedbackShown,
        modifier = modifier
    )
}

@Composable
private fun CommunityDashboardContent(
    uiState: CommunityDashboardUiState,
    onMemberClicked: (CommunityMember) -> Unit,
    onNudgePickerDismissed: () -> Unit,
    onNudgeSelected: (Nudge) -> Unit,
    onNudgeFeedbackShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LivingLandscapeBackground(landscape = uiState.visualLandscape, modifier = Modifier.fillMaxSize())

        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            !uiState.hasActiveCommunity -> Text(
                text = "You haven't joined a community yet.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isSolo) {
                    SoloModeBanner(
                        inviteCode = uiState.inviteCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CollectiveIndexDisplay(collectiveIndex = if (uiState.isSolo) null else uiState.collectiveIndex)
                    Spacer(modifier = Modifier.height(40.dp))
                    if (uiState.isSolo) {
                        EmptyPeersState()
                    } else {
                        MemberStatusRow(members = uiState.members, onMemberClicked = onMemberClicked)
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
                NudgeOption("Text nudge", Nudge.Text("Thinking of you"), availableCharges, isSending, onNudgeSelected)
                NudgeOption("Haptic nudge", Nudge.Haptic, availableCharges, isSending, onNudgeSelected)
                NudgeOption("Grey-scale", Nudge.GreyscaleLevel(level = 1), availableCharges, isSending, onNudgeSelected)
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

/** Background gradient that reflects the community's shared [VisualLandscape] mood. */
@Composable
private fun LivingLandscapeBackground(landscape: VisualLandscape, modifier: Modifier = Modifier) {
    val (topColor, bottomColor) = landscapeColors(landscape)
    val animatedTop by animateColorAsState(targetValue = topColor, animationSpec = tween(800), label = "landscapeTop")
    val animatedBottom by animateColorAsState(targetValue = bottomColor, animationSpec = tween(800), label = "landscapeBottom")

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(colors = listOf(animatedTop, animatedBottom))
        )
    )
}

private fun landscapeColors(landscape: VisualLandscape): Pair<Color, Color> = when (landscape) {
    VisualLandscape.TEMPEST -> Color(0xFF263238) to Color(0xFF0B0F12)
    VisualLandscape.RAINY -> Color(0xFF607D8B) to Color(0xFF455A64)
    VisualLandscape.MISTY -> Color(0xFFCFD8DC) to Color(0xFFA6B4B8)
    VisualLandscape.SERENE -> Color(0xFF81C7D4) to Color(0xFF4F9A94)
    VisualLandscape.FLOURISHING -> Color(0xFFFFE082) to Color(0xFF8BC34A)
}

/** Large, central IA_comm score; shows "--" when solo, since one member has nothing to average. */
@Composable
private fun CollectiveIndexDisplay(collectiveIndex: Int?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = collectiveIndex?.toString() ?: "--",
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Community Agency Index",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/** Shown in place of the score/roster while this device is the community's only member. */
@Composable
private fun SoloModeBanner(inviteCode: String, modifier: Modifier = Modifier) {
    val clipboardManager = LocalClipboardManager.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Invite friends to unlock peer support",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = inviteCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { clipboardManager.setText(AnnotatedString(inviteCode)) }) {
                Text("Copy code")
            }
        }
    }
}

/** Peer-roster empty state: shown instead of [MemberStatusRow] while the circle has no other members. */
@Composable
private fun EmptyPeersState(modifier: Modifier = Modifier) {
    Text(
        text = "No peers yet. Share your invite code to grow your circle.",
        modifier = modifier.padding(horizontal = 24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center
    )
}

/** Peer roster: pseudonym and coarse status per member, this community's whole "safe place" identity. Tap a card to send that peer a nudge. */
@Composable
private fun MemberStatusRow(
    members: List<CommunityMember>,
    onMemberClicked: (CommunityMember) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(members, key = { it.anonymousId }) { member ->
            MemberStatusCard(member = member, onClick = { onMemberClicked(member) })
        }
    }
}

@Composable
private fun MemberStatusCard(member: CommunityMember, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(statusColor(member.status))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = member.nickname.ifBlank { "Anonymous" },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun statusColor(status: MemberStatus): Color = when (status) {
    MemberStatus.OK -> Color(0xFF66BB6A)
    MemberStatus.AT_RISK -> Color(0xFFFFB74D)
    MemberStatus.CRISIS -> Color(0xFFE57373)
}
