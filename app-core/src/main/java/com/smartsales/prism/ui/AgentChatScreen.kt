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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.ui.components.*
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.prism.domain.activity.ThinkingPolicy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import com.smartsales.prism.ui.theme.*
import com.smartsales.prism.ui.components.agent.*

/**
 * Prism Chat Screen (Sleek Glass Version)
 * 
 * - Removes opaque background (relies on Shell)
 * - Glass Chat Header
 * - Glass Mode Toggle
 * - Aurora Home Hero
 */
@Composable
fun AgentChatScreen(
    viewModel: AgentViewModel = hiltViewModel(),
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

    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 60.dp.toPx() } }
    
    // UI Local State
    var isThinkingCollapsed by remember { mutableStateOf(false) }

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

    /* 
     * Pro Max Layout Strategy:
     * - Everything floats in a Box (The "Void")
     * - Header: Absolute Top
     * - Content: Padding for Header/Dock
     * - Dock: Absolute Bottom
     * - FABs: Absolute Right center
     */
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
        // --- Layer 1: Main Scrollable Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 80.dp) // Space for Floating Header
                .navigationBarsPadding()
                .padding(bottom = 140.dp) // Space for Dock
        ) {
            // Hero (Only if empty history)
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter // Fixed: Top alignment
                ) {
                    HomeHeroDashboard(
                        greeting = heroGreeting,
                        upcoming = heroUpcoming,
                        accomplished = heroAccomplished,
                        onProfileClick = onProfileClick
                    )
                }
            } else {
                // Agent Intelligence: Active Task Horizon (Sticky at the top of chat)
                ActiveTaskHorizon(
                    state = uiState.toActiveTaskHorizonState(),
                    onCancelRequested = { /* Hook up to pipeline cancel */ }
                )

                // V2: Task Board for Analyst Mode — STICKY
                if (taskBoardItems.isNotEmpty()) {
                    com.smartsales.prism.ui.analyst.TaskBoard(
                        items = taskBoardItems,
                        onItemClick = viewModel::selectTaskBoardItem
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Chat List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    reverseLayout = true
                ) {
                    // (Chat Content Logic - Same as before)
                    // 1. Agent Activity
                    agentActivity?.let { activity ->
                        item {
                            val maxLines = ThinkingPolicy.maxTraceLines(Mode.ANALYST)
                            val autoCollapse = ThinkingPolicy.shouldAutoCollapse(Mode.ANALYST)
                            AgentActivityBanner(activity, maxLines, autoCollapse)
                        }
                    }
                    
                    // Legacy Fallback
                    val currentUiState = uiState
                    if (agentActivity == null && currentUiState !is UiState.Idle && currentUiState !is UiState.Error) {
                        item {
                            when (currentUiState) {
                                is UiState.Response,
                                is UiState.AwaitingClarification,
                                is UiState.SchedulerTaskCreated,
                                is UiState.SchedulerMultiTaskCreated,
                                is UiState.MarkdownStrategyState,
                                is UiState.Error -> ResponseBubble(
                                    uiState = currentUiState,
                                    onConfirmPlan = viewModel::confirmAnalystPlan,
                                    onAmendPlan = viewModel::amendAnalystPlan
                                )
                                is UiState.Streaming -> ThinkingCanvas(
                                    state = currentUiState.toThinkingCanvasState(isCollapsed = isThinkingCollapsed),
                                    onToggleExpanded = { isThinkingCollapsed = !isThinkingCollapsed }
                                )
                                else -> {}
                            }
                        }
                    }

                    // 2. Message History
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
                                            onConfirmPlan = viewModel::confirmAnalystPlan,
                                            onAmendPlan = viewModel::amendAnalystPlan
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
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = AccentBlue,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("正在执行: ${state.toolName}", color = TextSecondary, fontSize = 13.sp)
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
        
        // --- Layer 2: Floating Header (5-Button Layout) ---
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

        // --- Layer 3: Floating Right Toolbar (FABs) ---
        // Hidden when keyboard is open or if preferred, kept strict.
        // For now, assume always visible but pushed up by IME if needed.
        if (history.isEmpty()) {
             Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Documents (List -> Description/File)
                GlassFab(icon = Icons.AutoMirrored.Filled.TextSnippet, onClick = { /* Specs */ }) 
                // Cube (Inventory -> Science/ViewInAr)
                GlassFab(icon = Icons.Outlined.Science, onClick = onArtifactsClick) // Using Science as Cube proxy
            }
        }

        // --- Layer 4: Floating Dock (Switcher + Input) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding() // Float above keyboard
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Input Bar (Glass Capsule)
                GlassInputCapsule(
                    text = inputText,
                    onTextChanged = viewModel::updateInput,
                    onSend = viewModel::send,
                    onAttachClick = onAudioDrawerClick
                )
            }
        }
        
        // Error Toast
         errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                containerColor = AccentDanger
            ) { Text(error, color = Color.White) }
        }
    }
}

// ===================================================================
// Pro Max Components (Local Implementation to match Prototype specs)
// ===================================================================

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
        // Left Group
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.Menu, onClick = onMenuClick)
            // Device Connectivity - Yellow Tint
             GlassCircleButton(
                 icon = Icons.Filled.Bluetooth, 
                 onClick = onDeviceClick,
                 tint = AccentYellow 
             )
        }

        // Center Title (Floating Badge)
        // Center Title (Floating Text - Assumption Removed)
        Box(
            modifier = Modifier.height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sessionTitle.ifEmpty { "新对话" },
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }

        // Right Group
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.BugReport, onClick = onDebugClick, tint = TextMuted)
            GlassCircleButton(icon = Icons.Filled.Add, onClick = onNewSessionClick)
        }
    }
}

@Composable
private fun HomeHeroDashboard(
    greeting: String,
    upcoming: List<com.smartsales.prism.domain.scheduler.TimelineItemModel.Task>,
    accomplished: List<com.smartsales.prism.domain.scheduler.TimelineItemModel.Task>,
    onProfileClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        // 动态问候 — 点击名字进入个人中心
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.clickable { onProfileClick() }
        )

        // 待办 section
        if (upcoming.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "📋 待办 (${upcoming.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                upcoming.forEach { task ->
                    HeroTaskRow(task = task, isDone = false)
                }
            }
        }

        // 已完成 section
        if (accomplished.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "✅ 已完成",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                accomplished.forEach { task ->
                    HeroTaskRow(task = task, isDone = true)
                }
            }
        }
    }
}

@Composable
private fun HeroTaskRow(
    task: com.smartsales.prism.domain.scheduler.TimelineItemModel.Task,
    isDone: Boolean
) {
    val urgencyDot = when (task.urgencyLevel) {
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L1_CRITICAL -> "🔴"
        com.smartsales.prism.domain.scheduler.UrgencyLevel.L2_IMPORTANT -> "🟡"
        else -> "⚪"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp), // No card background
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDone) {
            Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        } else {
            Text(urgencyDot, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = task.timeDisplay.split(" - ").firstOrNull() ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDone) TextMuted else TextSecondary,
            fontWeight = FontWeight.Normal
        )
    }
}



@Composable
private fun GlassInputCapsule(text: String, onTextChanged: (String) -> Unit, onSend: () -> Unit, onAttachClick: () -> Unit) {
    PrismSurface(
        shape = CircleShape, // Fully rounded capsule
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus Button
            IconButton(onClick = onAttachClick) {
                Icon(Icons.Filled.AttachFile, "Attach", tint = TextPrimary) // Web: Paperclip
            }
            
            // Input Field
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                     Text("输入消息...", color = TextMuted)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(AccentBlue)
                )
            }
            
            // Mic/Send Button
             Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black, CircleShape) // Web: Black Button
                    .clickable { onSend() },
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
private fun GlassCircleButton(
    icon: ImageVector, 
    onClick: () -> Unit, 
    tint: Color = TextPrimary
) {
    PrismSurface(
        shape = CircleShape,
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun GlassFab(icon: ImageVector, onClick: () -> Unit) {
     PrismSurface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
         contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
    }
}


