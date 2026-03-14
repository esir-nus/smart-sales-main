package com.smartsales.prism.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.prism.domain.activity.ThinkingPolicy
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.ui.components.*
import com.smartsales.prism.ui.components.agent.*
import com.smartsales.prism.ui.fakes.FakeAgentViewModel
import com.smartsales.prism.ui.theme.*
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AgentIntelligenceScreen(
    viewModel: IAgentViewModel = hiltViewModel<AgentViewModel>(),
    onMenuClick: () -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onAudioBadgeClick: () -> Unit = {},
    onAudioDrawerClick: () -> Unit = {},
    onTingwuClick: () -> Unit = {},
    onArtifactsClick: () -> Unit = {},
    onDebugClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val history by viewModel.history.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sessionTitle by viewModel.sessionTitle.collectAsState()
    val agentActivity by viewModel.agentActivity.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val taskBoardItems by viewModel.taskBoardItems.collectAsState()
    val heroUpcoming by viewModel.heroUpcoming.collectAsState()
    val heroAccomplished by viewModel.heroAccomplished.collectAsState()
    val heroGreeting by viewModel.heroGreeting.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    AgentIntelligenceContent(
        history = history,
        uiState = uiState,
        inputText = inputText,
        isSending = isSending,
        errorMessage = errorMessage,
        sessionTitle = sessionTitle,
        agentActivity = agentActivity,
        taskBoardItems = taskBoardItems,
        heroUpcoming = heroUpcoming,
        heroAccomplished = heroAccomplished,
        heroGreeting = heroGreeting,
        onMenuClick = onMenuClick,
        onNewSessionClick = onNewSessionClick,
        onAudioBadgeClick = onAudioBadgeClick,
        onAudioDrawerClick = onAudioDrawerClick,
        onTingwuClick = onTingwuClick,
        onArtifactsClick = onArtifactsClick,
        onDebugClick = onDebugClick,
        onProfileClick = onProfileClick,
        onUpdateInput = viewModel::updateInput,
        onSend = viewModel::send,
        onConfirmPlan = viewModel::confirmAnalystPlan,
        onAmendPlan = viewModel::amendAnalystPlan,
        onSelectTaskBoardItem = viewModel::selectTaskBoardItem
    )
}

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
    heroUpcoming: List<TimelineItemModel.Task>,
    heroAccomplished: List<TimelineItemModel.Task>,
    heroGreeting: String,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onAudioBadgeClick: () -> Unit,
    onAudioDrawerClick: () -> Unit,
    onTingwuClick: () -> Unit,
    onArtifactsClick: () -> Unit,
    onDebugClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUpdateInput: (String) -> Unit,
    onSend: () -> Unit,
    onConfirmPlan: () -> Unit,
    onAmendPlan: () -> Unit,
    onSelectTaskBoardItem: (String) -> Unit
) {
    var isThinkingCollapsed by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 60.dp.toPx() } }

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            var unconsumedDrag = 0f
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                if (available.y < 0) {
                    unconsumedDrag -= available.y
                    if (unconsumedDrag > thresholdPx) {
                        onAudioDrawerClick()
                        unconsumedDrag = 0f
                    }
                } else if (available.y > 0) {
                    unconsumedDrag = 0f
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                unconsumedDrag = 0f
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFC), // Top: Very light gray
                        Color(0xFFF5F0F8), // Middle: Soft lavender hint
                        Color(0xFFEDE5F2)  // Bottom: Subtle purple aurora
                    )
                )
            )
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 80.dp) // Space for Floating Header
                .navigationBarsPadding()
                .padding(bottom = 140.dp) // Space for Dock
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
                ActiveTaskHorizon(
                    state = uiState.toActiveTaskHorizonState(),
                    onCancelRequested = { /* Hook up to pipeline cancel */ }
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                                    onToggleExpanded = { isThinkingCollapsed = !isThinkingCollapsed }
                                )
                                is UiState.Streaming -> ThinkingCanvas(
                                    state = uiState.toThinkingCanvasState(isCollapsed = isThinkingCollapsed),
                                    onToggleExpanded = { isThinkingCollapsed = !isThinkingCollapsed }
                                )
                                is UiState.AwaitingClarification,
                                is UiState.MarkdownStrategyState,
                                is UiState.Response,
                                is UiState.SchedulerTaskCreated,
                                is UiState.SchedulerMultiTaskCreated,
                                is UiState.Error -> ResponseBubble(
                                    uiState = uiState,
                                    onConfirmPlan = onConfirmPlan,
                                    onAmendPlan = onAmendPlan
                                )
                                is UiState.ExecutingTool -> {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentBlue, strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("正在执行: ${uiState.toolName}", color = TextSecondary, fontSize = 13.sp)
                                    }
                                }
                                is UiState.BadgeDelegationHint -> {
                                    PrismSurface(
                                        shape = RoundedCornerShape(16.dp),
                                        backgroundColor = AccentYellow.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(vertical = 4.dp).border(1.dp, AccentYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                            Text("🎙️", fontSize = 16.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text("请长按您的智能工牌专属按键来录入此日程。", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                                is UiState.Loading -> {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = AccentBlue)
                                    }
                                }
                                else -> {}
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
                                    is UiState.Thinking -> {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                            Text("🧠", fontSize = 12.sp) 
                                            Spacer(Modifier.width(8.dp))
                                            Text(state.hint ?: "Thinking...", color = TextMuted, fontSize = 12.sp)
                                        }
                                    }
                                    is UiState.ExecutingTool -> {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentBlue, strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                            Text("正在执行: ${state.toolName}", color = TextSecondary, fontSize = 13.sp)
                                        }
                                    }
                                    is UiState.BadgeDelegationHint -> {
                                        PrismSurface(
                                            shape = RoundedCornerShape(16.dp),
                                            backgroundColor = AccentYellow.copy(alpha = 0.1f),
                                            modifier = Modifier.padding(vertical = 4.dp).border(1.dp, AccentYellow.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                                                Text("🎙️", fontSize = 16.sp)
                                                Spacer(Modifier.width(8.dp))
                                                Text("请长按您的智能工牌专属按键来录入此日程。", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // --- Layer 2: Floating Header ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            ProMaxHeader(
                sessionTitle = sessionTitle,
                onMenuClick = onMenuClick,
                onNewSessionClick = onNewSessionClick,
                onDebugClick = onDebugClick,
                onDeviceClick = onAudioBadgeClick
            )
        }

        // --- Layer 3: Floating Dock ---
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
                    onAttachClick = onAudioDrawerClick
                )
            }
        }
        
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                containerColor = AccentDanger
            ) { Text(error, color = Color.White) }
        }
    }
}

// Local Pro Max Components (Copied from AgentChatScreen for encapsulation)

@Composable
private fun ProMaxHeader(
    sessionTitle: String,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onDebugClick: () -> Unit,
    onDeviceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.Menu, onClick = onMenuClick)
            GlassCircleButton(icon = Icons.Filled.Bluetooth, onClick = onDeviceClick, tint = AccentYellow)
        }
        Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            Text(
                text = sessionTitle.ifEmpty { "新对话" },
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.BugReport, onClick = onDebugClick, tint = TextMuted)
            GlassCircleButton(icon = Icons.Filled.Add, onClick = onNewSessionClick)
        }
    }
}

@Composable
private fun HomeHeroDashboard(
    greeting: String,
    upcoming: List<TimelineItemModel.Task>,
    accomplished: List<TimelineItemModel.Task>,
    onProfileClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 20.dp, end = 20.dp)) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.clickable { onProfileClick() }
        )
        if (upcoming.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("📋 待办 (${upcoming.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                upcoming.forEach { HeroTaskRow(task = it, isDone = false) }
            }
        }
        if (accomplished.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("✅ 已完成", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                accomplished.forEach { HeroTaskRow(task = it, isDone = true) }
            }
        }
    }
}

@Composable
private fun HeroTaskRow(task: TimelineItemModel.Task, isDone: Boolean) {
    val urgencyDot = when (task.urgencyLevel) {
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L1_CRITICAL -> "🔴"
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT -> "🟡"
        else -> "⚪"
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (isDone) {
            Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        } else {
            Text(urgencyDot, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(task.timeDisplay.split(" - ").firstOrNull() ?: "", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(task.title, style = MaterialTheme.typography.bodySmall, color = if (isDone) TextMuted else TextSecondary, fontWeight = FontWeight.Normal)
    }
}

@Composable
private fun GlassInputCapsule(text: String, onTextChanged: (String) -> Unit, onSend: () -> Unit, onAttachClick: () -> Unit) {
    PrismSurface(shape = CircleShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Filled.AttachFile, "Attach", tint = TextPrimary)
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) Text("输入消息...", color = TextMuted)
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 16.sp),
                    cursorBrush = SolidColor(AccentBlue)
                )
            }
            Box(
                modifier = Modifier.size(40.dp).background(Color.Black, CircleShape).clickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (text.isEmpty()) Icons.Filled.Mic else Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassCircleButton(icon: ImageVector, onClick: () -> Unit, tint: Color = TextPrimary) {
    PrismSurface(shape = CircleShape, modifier = Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

// ===================================================================
// Previews (Parallel Proving Ground)
// Powered strictly by FakeAgentViewModel to prove state coverage
// ===================================================================

@Preview(showBackground = true, name = "1. Idle (Hero)")
@Composable
fun PreviewAgentIntelligence_Idle() {
    AgentIntelligenceScreen(viewModel = FakeAgentViewModel().apply { debugRunScenario("IDLE") })
}

@Preview(showBackground = true, name = "2. Thinking State")
@Composable
fun PreviewAgentIntelligence_Thinking() {
    AgentIntelligenceScreen(viewModel = FakeAgentViewModel().apply { 
        debugRunScenario("THINKING") 
        updateInput("安排明天会议")
        send()
    })
}

@Preview(showBackground = true, name = "3. Awaiting Clarification")
@Composable
fun PreviewAgentIntelligence_Clarification() {
    AgentIntelligenceScreen(viewModel = FakeAgentViewModel().apply { 
        debugRunScenario("WAITING_CLARIFICATION") 
        updateInput("开会")
        send()
    })
}

@Preview(showBackground = true, name = "4. Streaming State")
@Composable
fun PreviewAgentIntelligence_Streaming() {
    AgentIntelligenceScreen(viewModel = FakeAgentViewModel().apply { 
        debugRunScenario("STREAMING") 
        updateInput("写一份总结")
        send()
    })
}

@Preview(showBackground = true, name = "5. Scheduler Task Created")
@Composable
fun PreviewAgentIntelligence_Scheduler() {
    AgentIntelligenceScreen(viewModel = FakeAgentViewModel().apply { 
        debugRunScenario("SCHEDULER_TASK") 
        updateInput("安排与张总技术会议")
        send()
    })
}
