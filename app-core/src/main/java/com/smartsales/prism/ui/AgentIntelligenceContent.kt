package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandStateMapper
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.sim.SimAgentIntelligenceContent
import com.smartsales.prism.ui.sim.SimAgentViewModel
import com.smartsales.prism.ui.theme.AccentDanger

@Composable
internal fun AgentIntelligenceContent(
    history: List<ChatMessage>,
    uiState: UiState,
    inputText: String,
    isSending: Boolean,
    errorMessage: String?,
    sessionTitle: String,
    agentActivity: AgentActivity?,
    taskBoardItems: List<com.smartsales.prism.domain.analyst.TaskBoardItem>,
    heroUpcoming: List<ScheduledTask>,
    heroAccomplished: List<ScheduledTask>,
    heroGreeting: String,
    isSimShell: Boolean,
    transcriptRevealState: Map<String, SimAgentViewModel.ArtifactTranscriptRevealState>,
    onArtifactTranscriptRevealConsumed: (messageId: String, isLongTranscript: Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    onAudioBadgeClick: () -> Unit,
    onAudioDrawerClick: () -> Unit,
    onAttachClick: () -> Unit,
    onTingwuClick: () -> Unit,
    onArtifactsClick: () -> Unit,
    onDebugClick: () -> Unit,
    onProfileClick: () -> Unit,
    showDebugButton: Boolean,
    simDynamicIslandItems: List<DynamicIslandItem>,
    enableSimSchedulerPullGesture: Boolean,
    enableSimAudioPullGesture: Boolean,
    onSimSchedulerPullOpen: () -> Unit,
    onSimAudioPullOpen: () -> Unit,
    onUpdateInput: (String) -> Unit,
    onSend: () -> Unit,
    onConfirmPlan: () -> Unit,
    onAmendPlan: () -> Unit,
    onSelectTaskBoardItem: (String) -> Unit
) {
    val isThinkingCollapsed = remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 60.dp.toPx() } }
    val nestedScrollConnection = remember(isSimShell, thresholdPx) {
        if (isSimShell) {
            null
        } else {
            object : NestedScrollConnection {
                var unconsumedDrag = 0f

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y < 0) {
                        unconsumedDrag -= available.y
                        if (unconsumedDrag > thresholdPx) {
                            onAudioDrawerClick()
                            unconsumedDrag = 0f
                        }
                    } else if (available.y > 0) {
                        unconsumedDrag = 0f
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    unconsumedDrag = 0f
                    return Velocity.Zero
                }
            }
        }
    }

    val proMaxOnyx = Color(0xFF0B0C10)
    val proMaxAccentGlow = Color(0xFF38BDF8).copy(alpha = 0.08f)
    val proMaxTopWash = Color(0xFF111827)
    val dynamicIslandState = DynamicIslandStateMapper.fromScheduler(
        sessionTitle = sessionTitle,
        upcoming = heroUpcoming
    )
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val glowRadiusPx = with(LocalDensity.current) { (screenWidthDp * 2.5f).toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = if (isSimShell) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF121A24), Color(0xFF0E151D), Color(0xFF0B1219))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(proMaxTopWash, proMaxOnyx, Color(0xFF06070A))
                    )
                }
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isSimShell) Color(0xFF29465F).copy(alpha = 0.12f) else proMaxAccentGlow,
                        Color.Transparent
                    ),
                    radius = glowRadiusPx
                )
            )
            .then(
                if (nestedScrollConnection != null) {
                    Modifier.nestedScroll(nestedScrollConnection)
                } else {
                    Modifier
                }
            )
    ) {
        if (isSimShell) {
            SimAgentIntelligenceContent(
                history = history,
                uiState = uiState,
                inputText = inputText,
                isSending = isSending,
                sessionTitle = sessionTitle,
                agentActivity = agentActivity,
                heroGreeting = heroGreeting,
                transcriptRevealState = transcriptRevealState,
                onArtifactTranscriptRevealConsumed = onArtifactTranscriptRevealConsumed,
                onMenuClick = onMenuClick,
                onNewSessionClick = onNewSessionClick,
                onSchedulerClick = onSchedulerClick,
                onAttachClick = onAttachClick,
                simDynamicIslandItems = simDynamicIslandItems,
                enableSimSchedulerPullGesture = enableSimSchedulerPullGesture,
                enableSimAudioPullGesture = enableSimAudioPullGesture,
                onSimSchedulerPullOpen = onSimSchedulerPullOpen,
                onSimAudioPullOpen = onSimAudioPullOpen,
                onUpdateInput = onUpdateInput,
                onSend = onSend,
                onConfirmPlan = onConfirmPlan,
                onAmendPlan = onAmendPlan
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 80.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 140.dp)
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        HomeHeroDashboard(
                            greeting = heroGreeting,
                            upcoming = heroUpcoming,
                            accomplished = heroAccomplished,
                            onProfileClick = onProfileClick
                        )
                    }
                } else {
                    ChatTimeline(
                        modifier = Modifier.weight(1f),
                        history = history,
                        uiState = uiState,
                        agentActivity = agentActivity,
                        taskBoardItems = taskBoardItems,
                        isThinkingCollapsed = isThinkingCollapsed.value,
                        transcriptRevealState = transcriptRevealState,
                        onArtifactTranscriptRevealConsumed = onArtifactTranscriptRevealConsumed,
                        onConfirmPlan = onConfirmPlan,
                        onAmendPlan = onAmendPlan,
                        onSelectTaskBoardItem = onSelectTaskBoardItem
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                PrismSurface(
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                ) {
                    ProMaxHeader(
                        dynamicIslandState = dynamicIslandState,
                        onMenuClick = onMenuClick,
                        onNewSessionClick = onNewSessionClick,
                        onSchedulerClick = onSchedulerClick,
                        onDebugClick = onDebugClick,
                        onDeviceClick = onAudioBadgeClick,
                        showDebugButton = showDebugButton
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassInputCapsule(
                        text = inputText,
                        onTextChanged = onUpdateInput,
                        onSend = onSend,
                        onAttachClick = onAttachClick
                    )
                }
            }
        }

        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isSimShell) 20.dp else 80.dp),
                containerColor = AccentDanger
            ) {
                Text(error, color = Color.White)
            }
        }
    }
}
