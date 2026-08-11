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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.helpcircle.domain.model.AgencyIndex
import com.project.helpcircle.domain.model.VisualLandscape

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

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CollectiveIndexDisplay(collectiveIndex = uiState.collectiveIndex)
                Spacer(modifier = Modifier.height(40.dp))
                MemberStatusRow(memberIndices = uiState.memberIndices)
            }
        }
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
    VisualLandscape.STORM -> Color(0xFF37474F) to Color(0xFF102027)
    VisualLandscape.OVERCAST -> Color(0xFF78909C) to Color(0xFF455A64)
    VisualLandscape.CALM -> Color(0xFF81C7D4) to Color(0xFF4F9A94)
    VisualLandscape.BLOOMING_MEADOW -> Color(0xFFAED581) to Color(0xFF66BB6A)
}

/** Large, central IA_comm score. */
@Composable
private fun CollectiveIndexDisplay(collectiveIndex: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = collectiveIndex.toString(),
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

/** Anonymous per-member status cards: agency values only, no names or member identifiers. */
@Composable
private fun MemberStatusRow(memberIndices: List<Int>, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(memberIndices) { index -> MemberStatusCard(agencyValue = index) }
    }
}

@Composable
private fun MemberStatusCard(agencyValue: Int, modifier: Modifier = Modifier) {
    val landscape = VisualLandscape.forIndex(AgencyIndex.of(agencyValue))
    val (dotColor, _) = landscapeColors(landscape)

    Card(
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
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Peer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
