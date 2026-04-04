package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.prism.domain.activity.ThinkingPolicy
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.ui.components.*
import com.smartsales.prism.ui.components.agent.*
import com.smartsales.prism.ui.sim.SimArtifactTranscriptRevealState
import com.smartsales.prism.ui.sim.SimArtifactBubble
import com.smartsales.prism.ui.sim.SimTranscriptPresentation
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentYellow
import com.smartsales.prism.ui.theme.TextMuted
import com.smartsales.prism.ui.theme.TextSecondary

@Composable
internal fun ChatTimeline(
    modifier: Modifier = Modifier,
    history: List<ChatMessage>,
    uiState: UiState,
    agentActivity: AgentActivity?,
    taskBoardItems: List<com.smartsales.prism.domain.analyst.TaskBoardItem>,
    isThinkingCollapsed: Boolean,
    transcriptRevealState: Map<String, SimArtifactTranscriptRevealState>,
    onArtifactTranscriptRevealConsumed: (messageId: String, isLongTranscript: Boolean) -> Unit,
    onConfirmPlan: () -> Unit,
    onAmendPlan: () -> Unit,
    onSelectTaskBoardItem: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ActiveTaskHorizon(
            state = uiState.toActiveTaskHorizonState(),
            onCancelRequested = { }
        )

        if (taskBoardItems.isNotEmpty()) {
            com.smartsales.prism.ui.analyst.TaskBoard(
                items = taskBoardItems,
                onItemClick = onSelectTaskBoardItem
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            agentActivity?.let { activity ->
                item {
                    val maxLines = ThinkingPolicy.maxTraceLines(Mode.ANALYST)
                    val autoCollapse = ThinkingPolicy.shouldAutoCollapse(Mode.ANALYST)
                    AgentActivityBanner(activity, maxLines, autoCollapse)
                }
            }

            if (agentActivity == null && uiState !is UiState.Idle) {
                item {
                    when (uiState) {
                        is UiState.Thinking -> ThinkingCanvas(
                            state = uiState.toThinkingCanvasState(isCollapsed = isThinkingCollapsed),
                            onToggleExpanded = { }
                        )
                        is UiState.Streaming -> ThinkingCanvas(
                            state = uiState.toThinkingCanvasState(isCollapsed = isThinkingCollapsed),
                            onToggleExpanded = { }
                        )
                        is UiState.AwaitingClarification,
                        is UiState.MarkdownStrategyState,
                        is UiState.AudioArtifacts,
                        is UiState.Response,
                        is UiState.SchedulerTaskCreated,
                        is UiState.SchedulerMultiTaskCreated,
                        is UiState.Error -> ResponseBubble(
                            uiState = uiState,
                            onConfirmPlan = onConfirmPlan,
                            onAmendPlan = onAmendPlan
                        )
                        is UiState.ExecutingTool -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = AccentBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("正在执行: ${uiState.toolName}", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                        is UiState.BadgeDelegationHint -> {
                            InlineStatusNotice(
                                icon = Icons.Default.Mic,
                                iconTint = AccentYellow,
                                containerColor = AccentYellow.copy(alpha = 0.1f),
                                borderColor = AccentYellow.copy(alpha = 0.3f),
                                text = "请长按您的智能工牌专属按键来录入此日程。"
                            )
                        }
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentBlue)
                            }
                        }
                        else -> Unit
                    }
                }
            }

            items(history.reversed()) { message ->
                when (message) {
                    is ChatMessage.User -> UserBubble(text = message.content)
                    is ChatMessage.Ai -> {
                        when (val state = message.uiState) {
                            is UiState.Response,
                            is UiState.AwaitingClarification,
                            is UiState.SchedulerTaskCreated,
                            is UiState.SchedulerMultiTaskCreated,
                            is UiState.MarkdownStrategyState,
                            is UiState.Error -> {
                                ResponseBubble(
                                    uiState = state,
                                    onConfirmPlan = onConfirmPlan,
                                    onAmendPlan = onAmendPlan
                                )
                            }
                            is UiState.AudioArtifacts -> {
                                val revealState = transcriptRevealState[message.id]
                                SimArtifactBubble(
                                    title = state.title,
                                    artifactsJson = state.artifactsJson,
                                    transcriptPresentation = SimTranscriptPresentation(
                                        enableInitialReveal = revealState?.consumed != true,
                                        startCollapsed = revealState?.isLongTranscript == true,
                                        minRevealMillis = if (revealState?.consumed == true) 0L else 1000L
                                    ),
                                    onTranscriptRevealConsumed = { isLongTranscript ->
                                        onArtifactTranscriptRevealConsumed(message.id, isLongTranscript)
                                    }
                                )
                            }
                            is UiState.Thinking -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = ProMaxAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        state.hint ?: "处理中...",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            is UiState.ExecutingTool -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = AccentBlue,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在执行: ${state.toolName}", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            is UiState.BadgeDelegationHint -> {
                                InlineStatusNotice(
                                    icon = Icons.Default.Mic,
                                    iconTint = AccentYellow,
                                    containerColor = AccentYellow.copy(alpha = 0.1f),
                                    borderColor = AccentYellow.copy(alpha = 0.3f),
                                    text = "请长按您的智能工牌专属按键来录入此日程。"
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineStatusNotice(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    borderColor: Color,
    text: String
) {
    PrismSurface(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = containerColor,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconTint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(text, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
