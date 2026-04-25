package com.smartsales.prism.ui.sim

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smartsales.core.pipeline.ActivityAction
import com.smartsales.core.pipeline.ActivityPhase
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.ui.ProMaxAccent
import com.smartsales.prism.ui.ProMaxDanger
import com.smartsales.prism.ui.components.CopyableBubble
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.components.prismStatusBarPadding
import com.smartsales.prism.ui.components.DynamicIsland
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val SIM_HEADER_TEST_TAG = "sim_header"
internal const val SIM_DYNAMIC_ISLAND_TEST_TAG = "sim_dynamic_island"
internal const val SIM_HEADER_MENU_BUTTON_TEST_TAG = "sim_header_menu_button"
internal const val SIM_HEADER_NEW_CHAT_BUTTON_TEST_TAG = "sim_header_new_chat_button"
internal const val SIM_INPUT_BAR_TEST_TAG = "sim_input_bar"
internal const val SIM_INPUT_FIELD_TEST_TAG = "sim_input_field"
internal const val SIM_ATTACH_BUTTON_TEST_TAG = "sim_attach_button"
internal const val SIM_SEND_BUTTON_TEST_TAG = "sim_send_button"
internal const val SIM_ENABLE_SHARED_HOME_HERO_SHELL = true
internal val SIM_IDLE_COMPOSER_ROTATING_HINTS = listOf(
    "输入消息...",
    "也可以上滑这里，打开录音库",
    "点击左侧附件，也能选择录音"
)

private val SimChrome = Color(0xFF202833)
private val SimChromeMuted = Color(0xFF778291)

@Composable
internal fun SimAgentIntelligenceContent(
    history: List<ChatMessage>,
    uiState: UiState,
    inputText: String,
    isSending: Boolean,
    sessionTitle: String,
    agentActivity: AgentActivity?,
    heroGreeting: String,
    transcriptRevealState: Map<String, SimArtifactTranscriptRevealState>,
    onArtifactTranscriptRevealConsumed: (messageId: String, isLongTranscript: Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    onAttachClick: () -> Unit,
    simDynamicIslandState: DynamicIslandUiState,
    showHeaderMenuButton: Boolean = true,
    showHeaderNewSessionButton: Boolean = true,
    showBottomComposer: Boolean = true,
    showIdleComposerHint: Boolean = false,
    enableSimSchedulerPullGesture: Boolean,
    enableSimAudioPullGesture: Boolean,
    onSimSchedulerPullOpen: () -> Unit,
    onSimAudioPullOpen: () -> Unit,
    voiceDraftState: SimVoiceDraftUiState,
    voiceDraftEnabled: Boolean,
    onVoiceDraftPermissionRequested: () -> Unit,
    onVoiceDraftPermissionResult: (Boolean) -> Unit,
    onVoiceDraftStart: () -> Boolean,
    onVoiceDraftFinish: () -> Unit,
    onVoiceDraftCancel: () -> Unit,
    onUpdateInput: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onConfirmPlan: () -> Unit,
    onAmendPlan: () -> Unit
) {
    val showSimSharedHomeHeroShell = SIM_ENABLE_SHARED_HOME_HERO_SHELL
    ObserveSimVoiceDraftSession(
        shouldCancel = voiceDraftState.isRecording || voiceDraftState.isProcessing || voiceDraftState.awaitingMicPermission,
        onCancel = onVoiceDraftCancel
    )

    if (showSimSharedHomeHeroShell) {
        SimHomeHeroShellFrame(
            inputText = inputText,
            isSending = isSending,
            dynamicIslandState = simDynamicIslandState,
            onMenuClick = onMenuClick,
            onNewSessionClick = onNewSessionClick,
            onSchedulerClick = onSchedulerClick,
            showMenuButton = showHeaderMenuButton,
            showNewSessionButton = showHeaderNewSessionButton,
            onTextChanged = onUpdateInput,
            onSend = onSend,
            onAttachClick = onAttachClick,
            showIdleComposerHint = showIdleComposerHint && history.isEmpty(),
            showBottomComposer = showBottomComposer,
            enableSchedulerPullGesture = enableSimSchedulerPullGesture,
            enableAudioPullGesture = enableSimAudioPullGesture,
            onSchedulerPullOpen = onSimSchedulerPullOpen,
            onAudioPullOpen = onSimAudioPullOpen,
            voiceDraftState = voiceDraftState,
            voiceDraftEnabled = voiceDraftEnabled,
            onVoiceDraftPermissionRequested = onVoiceDraftPermissionRequested,
            onVoiceDraftPermissionResult = onVoiceDraftPermissionResult,
            onVoiceDraftStart = onVoiceDraftStart,
            onVoiceDraftFinish = onVoiceDraftFinish,
            onVoiceDraftCancel = onVoiceDraftCancel
        ) { modifier ->
            if (history.isEmpty()) {
                SimHomeHeroGreetingStage(
                    modifier = modifier,
                    greeting = heroGreeting
                )
            } else {
                SimHomeHeroCenterStage(
                    modifier = modifier,
                    contentAlignment = Alignment.TopStart
                ) {
                    SimConversationTimeline(
                        modifier = Modifier.fillMaxSize(),
                        history = history,
                        uiState = uiState,
                        agentActivity = agentActivity,
                        transcriptRevealState = transcriptRevealState,
                        onArtifactTranscriptRevealConsumed = onArtifactTranscriptRevealConsumed,
                        onSuggestionClick = onSuggestionClick,
                        onAttachClick = onAttachClick,
                        onConfirmPlan = onConfirmPlan,
                        onAmendPlan = onAmendPlan
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .prismStatusBarPadding()
                .prismNavigationBarPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            SimShellHeader(
                sessionTitle = sessionTitle,
                dynamicIslandState = simDynamicIslandState,
                onMenuClick = onMenuClick,
                onNewSessionClick = onNewSessionClick,
                onSchedulerClick = onSchedulerClick,
                showMenuButton = showHeaderMenuButton,
                showNewSessionButton = showHeaderNewSessionButton,
                onBoundsChanged = null
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    SimIdleGreeting(greeting = heroGreeting)
                }
            } else {
                SimConversationTimeline(
                    modifier = Modifier.weight(1f),
                    history = history,
                    uiState = uiState,
                    agentActivity = agentActivity,
                    transcriptRevealState = transcriptRevealState,
                    onArtifactTranscriptRevealConsumed = onArtifactTranscriptRevealConsumed,
                    onSuggestionClick = onSuggestionClick,
                    onAttachClick = onAttachClick,
                    onConfirmPlan = onConfirmPlan,
                    onAmendPlan = onAmendPlan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SimInputBar(
                text = inputText,
                isSending = isSending,
                onTextChanged = onUpdateInput,
                onSend = onSend,
                onAttachClick = onAttachClick,
                showIdleComposerHint = showIdleComposerHint && history.isEmpty(),
                voiceDraftState = voiceDraftState,
                voiceDraftEnabled = voiceDraftEnabled,
                onVoiceDraftPermissionRequested = onVoiceDraftPermissionRequested,
                onVoiceDraftPermissionResult = onVoiceDraftPermissionResult,
                onVoiceDraftStart = onVoiceDraftStart,
                onVoiceDraftFinish = onVoiceDraftFinish,
                onVoiceDraftCancel = onVoiceDraftCancel,
                onBoundsChanged = null
            )
        }
    }
}

@Composable
private fun SimConversationTimeline(
    modifier: Modifier = Modifier,
    history: List<ChatMessage>,
    uiState: UiState,
    agentActivity: AgentActivity?,
    transcriptRevealState: Map<String, SimArtifactTranscriptRevealState>,
    onArtifactTranscriptRevealConsumed: (messageId: String, isLongTranscript: Boolean) -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onAttachClick: () -> Unit = {},
    onConfirmPlan: () -> Unit,
    onAmendPlan: () -> Unit
) {
    val showSuggestions = uiState is UiState.Idle &&
        agentActivity == null &&
        history.isNotEmpty() &&
        history.lastOrNull() is ChatMessage.Ai
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
        reverseLayout = true
    ) {
        if (showSuggestions) {
            item(key = "sim_suggestions") {
                SimSuggestionCards(
                    onSuggestionClick = onSuggestionClick,
                    onAttachClick = onAttachClick
                )
            }
        }

        agentActivity?.let { activity ->
            item {
                SimStatusSheet(
                    title = activity.phase.toSimStatusTitle(),
                    action = activity.action?.toSimStatusAction() ?: "处理中...",
                    body = activity.trace.lastOrNull()
                        ?: activity.action?.toSimStatusAction()
                        ?: activity.phase.toSimStatusTitle(),
                    icon = when (activity.phase) {
                        ActivityPhase.EXECUTING -> Icons.Outlined.Build
                        ActivityPhase.ERROR -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.AutoAwesome
                    },
                    accent = when (activity.phase) {
                        ActivityPhase.ERROR -> ProMaxDanger
                        else -> ProMaxAccent
                    }
                )
            }
        }

        if (agentActivity == null && uiState !is UiState.Idle) {
            item {
                when (uiState) {
                    is UiState.Thinking -> SimThinkingBanner(
                        hint = uiState.hint
                    )
                    is UiState.Streaming -> SimStreamingBubble(
                        content = uiState.partialContent
                    )
                    is UiState.AwaitingClarification -> SimAssistantBubble(
                        content = uiState.question,
                        headline = "需要更多信息"
                    )
                    is UiState.MarkdownStrategyState -> SimStrategySheet(
                        title = uiState.title,
                        content = uiState.markdownContent,
                        onConfirm = onConfirmPlan,
                        onAmend = onAmendPlan
                    )
                    is UiState.AudioArtifacts -> SimArtifactBubble(
                        title = uiState.title,
                        artifactsJson = uiState.artifactsJson
                    )
                    is UiState.Response -> SimAssistantBubble(content = uiState.content)
                    is UiState.SchedulerTaskCreated -> SimSystemSheet(
                        title = "日程已创建",
                        description = uiState.title,
                        icon = Icons.Default.Schedule,
                        accent = ProMaxAccent
                    )
                    is UiState.SchedulerMultiTaskCreated -> SimSystemSheet(
                        title = "批量日程已创建",
                        description = "已创建 ${uiState.tasks.size} 个任务",
                        icon = Icons.Default.Schedule,
                        accent = ProMaxAccent
                    )
                    is UiState.Error -> SimAssistantBubble(
                        content = uiState.message,
                        headline = "发生错误",
                        accent = ProMaxDanger,
                        borderColor = ProMaxDanger.copy(alpha = 0.22f)
                    )
                    is UiState.ExecutingTool -> SimStatusSheet(
                        title = "工具执行",
                        action = "执行中...",
                        body = "正在执行: ${uiState.toolName}",
                        icon = Icons.Outlined.Build,
                        accent = ProMaxAccent
                    )
                    is UiState.BadgeDelegationHint -> SimSystemSheet(
                        title = "工牌录入提示",
                        description = "请长按您的智能工牌专属按键来录入此日程。",
                        icon = Icons.Default.Mic,
                        accent = Color(0xFFFACC15)
                    )
                    is UiState.Loading -> SimInlineLoading()
                    else -> Unit
                }
            }
        }

        items(history.reversed()) { message ->
            when (message) {
                is ChatMessage.User -> SimUserBubble(text = message.content)
                is ChatMessage.Ai -> {
                    when (val state = message.uiState) {
                        is UiState.Response -> SimAssistantBubble(content = state.content)
                        is UiState.AwaitingClarification -> SimAssistantBubble(
                            content = state.question,
                            headline = "需要更多信息"
                        )
                        is UiState.SchedulerTaskCreated -> SimSystemSheet(
                            title = "日程已创建",
                            description = state.title,
                            icon = Icons.Default.Schedule,
                            accent = ProMaxAccent
                        )
                        is UiState.SchedulerMultiTaskCreated -> SimSystemSheet(
                            title = "批量日程已创建",
                            description = "已创建 ${state.tasks.size} 个任务",
                            icon = Icons.Default.Schedule,
                            accent = ProMaxAccent
                        )
                        is UiState.MarkdownStrategyState -> SimStrategySheet(
                            title = state.title,
                            content = state.markdownContent,
                            onConfirm = onConfirmPlan,
                            onAmend = onAmendPlan
                        )
                        is UiState.Error -> SimAssistantBubble(
                            content = state.message,
                            headline = "发生错误",
                            accent = ProMaxDanger,
                            borderColor = ProMaxDanger.copy(alpha = 0.22f)
                        )
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
                        is UiState.Thinking -> SimThinkingBanner(
                            hint = state.hint
                        )
                        is UiState.Streaming -> SimStreamingBubble(
                            content = state.partialContent
                        )
                        is UiState.ExecutingTool -> SimStatusSheet(
                            title = "工具执行",
                            action = "执行中...",
                            body = "正在执行: ${state.toolName}",
                            icon = Icons.Outlined.Build,
                            accent = ProMaxAccent
                        )
                        is UiState.BadgeDelegationHint -> SimSystemSheet(
                            title = "工牌录入提示",
                            description = "请长按您的智能工牌专属按键来录入此日程。",
                            icon = Icons.Default.Mic,
                            accent = Color(0xFFFACC15)
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun SimShellHeader(
    sessionTitle: String,
    dynamicIslandState: DynamicIslandUiState,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onSchedulerClick: (DynamicIslandTapAction) -> Unit,
    showMenuButton: Boolean,
    showNewSessionButton: Boolean,
    onBoundsChanged: ((Rect) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onBoundsChanged != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        onBoundsChanged(coordinates.boundsInRoot())
                    }
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showMenuButton) {
            SimHeaderButton(
                icon = Icons.Filled.Menu,
                tint = Color.White.copy(alpha = 0.82f),
                onClick = onMenuClick,
                modifier = Modifier.testTag(SIM_HEADER_MENU_BUTTON_TEST_TAG)
            )
        } else {
            Spacer(modifier = Modifier.size(42.dp))
        }
        if (dynamicIslandState is DynamicIslandUiState.Visible) {
            SimRotatingDynamicIsland(
                state = dynamicIslandState,
                modifier = Modifier.weight(1f),
                onTap = onSchedulerClick
            )
        } else {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sessionTitle.ifBlank { "SIM" },
                    color = Color.White.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (showNewSessionButton) {
            SimHeaderButton(
                icon = Icons.Filled.Add,
                tint = Color.White.copy(alpha = 0.82f),
                onClick = onNewSessionClick,
                modifier = Modifier.testTag(SIM_HEADER_NEW_CHAT_BUTTON_TEST_TAG)
            )
        } else {
            Spacer(modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
private fun SimRotatingDynamicIsland(
    state: DynamicIslandUiState.Visible,
    modifier: Modifier = Modifier,
    onTap: (DynamicIslandTapAction) -> Unit
) {
    Box(modifier = modifier) {
        AnimatedContent(
            targetState = state.item,
            transitionSpec = {
                (slideInVertically { fullHeight -> fullHeight } + fadeIn())
                    .togetherWith(slideOutVertically { fullHeight -> -fullHeight / 2 } + fadeOut())
            },
            label = "sim_dynamic_island_rotation"
        ) { item ->
            DynamicIsland(
                state = DynamicIslandUiState.Visible(item),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SIM_DYNAMIC_ISLAND_TEST_TAG),
                onTap = onTap
            )
        }
    }
}

internal fun resolveSimDynamicIslandIndex(
    items: List<DynamicIslandItem>,
    currentItemKey: String?
): Int {
    if (items.isEmpty()) return 0
    val matchedIndex = currentItemKey
        ?.let { key -> items.indexOfFirst { it.stableKey == key } }
        ?: -1
    return if (matchedIndex >= 0) matchedIndex else 0
}

@Composable
private fun SimHeaderButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(SimChrome.copy(alpha = 0.92f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SimIdleGreeting(greeting: String) {
    Text(
        text = greeting,
        color = Color.White.copy(alpha = 0.96f),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
        modifier = Modifier.padding(start = 2.dp, top = 24.dp)
    )
}

@Composable
private fun SimInputBar(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    showIdleComposerHint: Boolean,
    voiceDraftState: SimVoiceDraftUiState,
    voiceDraftEnabled: Boolean,
    onVoiceDraftPermissionRequested: () -> Unit,
    onVoiceDraftPermissionResult: (Boolean) -> Unit,
    onVoiceDraftStart: () -> Boolean,
    onVoiceDraftFinish: () -> Unit,
    onVoiceDraftCancel: () -> Unit,
    onBoundsChanged: ((Rect) -> Unit)? = null
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onVoiceDraftPermissionResult(granted)
    }
    val actionEnabled = text.isNotBlank() && !isSending
    val showVoiceMic = voiceDraftEnabled && text.isBlank()
    val accentColor = ProMaxAccent
    val displayText = if (text.isBlank() && voiceDraftState.liveTranscript.isNotBlank()) {
        voiceDraftState.liveTranscript
    } else {
        text
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onBoundsChanged != null) {
                    Modifier.onGloballyPositioned { coordinates ->
                        onBoundsChanged(coordinates.boundsInRoot())
                    }
                } else {
                    Modifier
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (voiceDraftState.isRecording) {
            SimVoiceDraftHandshake(
                state = voiceDraftState,
                accentColor = accentColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp, max = 140.dp)
                .testTag(SIM_INPUT_BAR_TEST_TAG)
                .background(Color(0xFF1C232D).copy(alpha = 0.96f), RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .testTag(SIM_ATTACH_BUTTON_TEST_TAG)
                    .clickable(onClick = onAttachClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = "Attach",
                    tint = SimChromeMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = displayText,
                    onValueChange = { updatedText ->
                        if (!voiceDraftState.isRecording) {
                            onTextChanged(updatedText)
                        }
                    },
                    modifier = Modifier.testTag(SIM_INPUT_FIELD_TEST_TAG),
                    singleLine = false,
                    maxLines = 4,
                    textStyle = TextStyle(
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(ProMaxAccent),
                    decorationBox = { innerTextField ->
                        if (displayText.isBlank()) {
                            SimIdleComposerRotatingHint(
                                visible = !voiceDraftState.isRecording && !voiceDraftState.isProcessing,
                                rotatingHints = SIM_IDLE_COMPOSER_ROTATING_HINTS,
                                useFullRotation = showIdleComposerHint
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .testTag(SIM_SEND_BUTTON_TEST_TAG)
                    .background(
                        if (actionEnabled || isSending || showVoiceMic) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.28f)
                        },
                        CircleShape
                    )
                    .then(
                        if (showVoiceMic) {
                            // 使用 Unit 作为 key，避免 isRecording 变化时
                            // 取消 pointerInput 协程导致 tryAwaitRelease 丢失。
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        if (voiceDraftState.isProcessing) return@detectTapGestures
                                        if (
                                            voiceDraftState.isRecording &&
                                            voiceDraftState.interactionMode == SimVoiceDraftInteractionMode.TAP_TO_SEND
                                        ) {
                                            val released = tryAwaitRelease()
                                            if (released) {
                                                onVoiceDraftFinish()
                                            } else {
                                                onVoiceDraftCancel()
                                            }
                                            return@detectTapGestures
                                        }
                                        val hasMicPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!hasMicPermission) {
                                            onVoiceDraftPermissionRequested()
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@detectTapGestures
                                        }
                                        val started = onVoiceDraftStart()
                                        if (!started) return@detectTapGestures
                                        val released = tryAwaitRelease()
                                        if (released) {
                                            onVoiceDraftFinish()
                                        } else {
                                            onVoiceDraftCancel()
                                        }
                                    }
                                )
                            }
                        } else {
                            Modifier.clickable(enabled = actionEnabled) { onSend() }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF11161D),
                            strokeWidth = 2.dp
                        )
                    }

                    showVoiceMic -> {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Mic",
                            tint = Color(0xFF11161D).copy(
                                alpha = if (voiceDraftState.isRecording || voiceDraftState.isProcessing) 1f else 0.88f
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF11161D).copy(alpha = if (actionEnabled) 1f else 0.45f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun simPlaceholderBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "sim_placeholder_shimmer")
    val shimmerOffset = transition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(500)
        ),
        label = "sim_placeholder_shimmer_offset"
    )

    return Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to SimChromeMuted.copy(alpha = 0.4f),
            0.5f to Color.White.copy(alpha = 0.95f),
            1.0f to SimChromeMuted.copy(alpha = 0.4f)
        ),
        startX = shimmerOffset.value,
        endX = shimmerOffset.value + 150f
    )
}

@Composable
private fun SimIdleComposerRotatingHint(
    visible: Boolean,
    rotatingHints: List<String>,
    useFullRotation: Boolean
) {
    if (!visible || rotatingHints.isEmpty()) return

    val displayedHints = if (useFullRotation) {
        rotatingHints
    } else {
        rotatingHints.take(1)
    }
    var currentHintIndex by remember(visible, displayedHints) { mutableStateOf(0) }

    LaunchedEffect(visible, displayedHints) {
        currentHintIndex = 0
        while (visible && displayedHints.size > 1) {
            delay(2600L)
            currentHintIndex = (currentHintIndex + 1) % displayedHints.size
        }
    }

    Text(
        text = displayedHints[currentHintIndex],
        style = TextStyle(
            brush = simPlaceholderBrush(),
            fontSize = 15.sp
        )
    )
}

@Composable
private fun SimThinkingBanner(hint: String?) {
    val palette = rememberSimConversationSurfacePalette()
    val transition = rememberInfiniteTransition(label = "sim_thinking_pulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SimHomeHeroTokens.ConversationBreatheDurationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_thinking_dot_alpha"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = SimHomeHeroTokens.ConversationBreatheMinGlowAlpha,
        targetValue = SimHomeHeroTokens.ConversationBreatheMaxGlowAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SimHomeHeroTokens.ConversationBreatheDurationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_thinking_glow_alpha"
    )
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 4.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .background(palette.surface, shape)
                .border(1.dp, ProMaxAccent.copy(alpha = glowAlpha), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = ProMaxAccent.copy(alpha = dotAlpha),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = hint ?: "SIM 正在分析...",
                color = palette.bodyMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SimStreamingBubble(content: String) {
    if (content.isBlank()) {
        SimThinkingBanner(hint = "SIM 正在生成回复...")
        return
    }
    val palette = rememberSimConversationSurfacePalette()
    val transition = rememberInfiniteTransition(label = "sim_streaming")
    val cursorPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SimHomeHeroTokens.StreamingCursorCycleMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "sim_streaming_cursor_phase"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = SimHomeHeroTokens.ConversationBreatheMinGlowAlpha,
        targetValue = SimHomeHeroTokens.ConversationBreatheMaxGlowAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SimHomeHeroTokens.ConversationBreatheDurationMillis,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sim_streaming_glow_alpha"
    )
    val cursorVisible = cursorPhase < SimHomeHeroTokens.StreamingCursorOnFraction
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 4.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .background(palette.surface, shape)
                .border(1.dp, ProMaxAccent.copy(alpha = glowAlpha), shape)
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            MarkdownText(
                text = content + if (cursorVisible) "\u258C" else "",
                color = palette.body,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

private data class SimSuggestion(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val SimDefaultSuggestions = listOf(
    SimSuggestion(
        icon = Icons.Outlined.TrackChanges,
        title = "复盘一段客户沟通",
        description = "点出关键转折、情绪信号、机会点"
    ),
    SimSuggestion(
        icon = Icons.Outlined.RecordVoiceOver,
        title = "演练开场白或异议应对",
        description = "基于你的行业经验给具体建议"
    ),
    SimSuggestion(
        icon = Icons.Outlined.GraphicEq,
        title = "分析一段录音",
        description = "上滑打开录音库，选择要分析的录音"
    )
)

@Composable
private fun SimSuggestionCards(
    onSuggestionClick: (String) -> Unit,
    onAttachClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SimDefaultSuggestions.forEachIndexed { index, suggestion ->
            SimSuggestionActionCard(
                icon = suggestion.icon,
                title = suggestion.title,
                description = suggestion.description,
                onClick = {
                    onSuggestionClick(suggestion.title)
                    // 分析录音卡片：发送消息后自动打开音频抽屉
                    if (index == 2) {
                        scope.launch {
                            delay(180L)
                            onAttachClick()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SimSuggestionActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val palette = rememberSimConversationSurfacePalette()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.bodyMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
            Text(
                text = description,
                color = palette.bodyMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SimInlineLoading() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = SimChromeMuted,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SIM 正在加载",
            color = SimChromeMuted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SimUserBubble(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.End
    ) {
        CopyableBubble(textToCopy = text) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        color = SimHomeHeroTokens.OutgoingBlue,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomEnd = 4.dp,
                            bottomStart = 20.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun SimAssistantBubble(
    content: String,
    headline: String? = null,
    accent: Color? = null,
    borderColor: Color? = null
) {
    val palette = rememberSimConversationSurfacePalette()
    val resolvedAccent = accent ?: palette.title
    val resolvedBorderColor = borderColor ?: palette.border
    var appeared by remember { mutableStateOf(false) }
    val enterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = SimHomeHeroTokens.BubbleEnterDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "sim_assistant_bubble_enter_alpha"
    )
    val enterOffsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else SimHomeHeroTokens.BubbleEnterOffsetDp,
        animationSpec = tween(
            durationMillis = SimHomeHeroTokens.BubbleEnterDurationMillis,
            easing = FastOutSlowInEasing
        ),
        label = "sim_assistant_bubble_enter_offset"
    )
    LaunchedEffect(Unit) { appeared = true }
    val shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomEnd = 20.dp,
        bottomStart = 4.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .graphicsLayer {
                alpha = enterAlpha
                translationY = enterOffsetY
            },
        horizontalArrangement = Arrangement.Start
    ) {
        CopyableBubble(textToCopy = content) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .background(palette.surface, shape)
                    .border(1.dp, resolvedBorderColor, shape)
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                if (!headline.isNullOrBlank()) {
                    Text(
                        text = headline,
                        color = resolvedAccent.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                MarkdownText(
                    text = content,
                    color = palette.body,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun SimSystemSheet(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color
) {
    val palette = rememberSimConversationSurfacePalette()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = palette.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = palette.bodyMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SimStatusSheet(
    title: String,
    action: String,
    body: String,
    icon: ImageVector,
    accent: Color
) {
    val palette = rememberSimConversationSurfacePalette()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 260.dp, max = 320.dp)
                .background(palette.surface, shape)
                .border(1.dp, palette.border, shape)
                .padding(horizontal = 16.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(accent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent.copy(alpha = 0.90f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = palette.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = action,
                    color = accent.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            HorizontalDivider(color = palette.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                color = palette.bodyMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun SimStrategySheet(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onAmend: () -> Unit
) {
    val palette = rememberSimConversationSurfacePalette()
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(1.dp, palette.border, shape)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                color = palette.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        MarkdownText(
            text = content,
            color = palette.body,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onAmend,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = palette.bodyMuted,
                    containerColor = palette.quietFill
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text("修改计划")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SimHomeHeroTokens.OutgoingBlue.copy(alpha = 0.18f),
                    contentColor = Color.White.copy(alpha = 0.96f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    SimHomeHeroTokens.OutgoingBlue.copy(alpha = 0.24f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("执行此计划")
            }
        }
    }
}

private fun ActivityPhase.toSimStatusTitle(): String = when (this) {
    ActivityPhase.PLANNING -> "规划分析步骤"
    ActivityPhase.EXECUTING -> "工具执行"
    ActivityPhase.RESPONDING -> "回复生成"
    ActivityPhase.COMPLETED -> "处理完成"
    ActivityPhase.ERROR -> "发生错误"
}

private fun ActivityAction.toSimStatusAction(): String = when (this) {
    ActivityAction.THINKING -> "思考中..."
    ActivityAction.PARSING -> "解析中..."
    ActivityAction.TRANSCRIBING -> "转写中..."
    ActivityAction.RETRIEVING -> "检索中..."
    ActivityAction.ASSEMBLING -> "整理中..."
    ActivityAction.STREAMING -> "生成中..."
}
