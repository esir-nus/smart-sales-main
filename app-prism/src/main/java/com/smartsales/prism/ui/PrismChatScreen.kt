package com.smartsales.prism.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star // v2.6 Home Hero Fallback
import androidx.compose.material3.*
import androidx.compose.animation.core.* // v2.6 Home Hero Animation
import androidx.compose.ui.graphics.Brush // v2.6 Home Hero Gradient
import androidx.compose.ui.graphics.graphicsLayer // v2.6 Home Hero Layer
import android.view.HapticFeedbackConstants // v2.6 Haptic
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.ui.components.InputBar
import com.smartsales.prism.ui.components.PlanCard
import com.smartsales.prism.ui.components.ResponseBubble
import com.smartsales.prism.ui.components.UserBubble
import com.smartsales.prism.ui.components.AgentActivityBanner
import com.smartsales.prism.domain.activity.AgentActivity
import com.smartsales.prism.domain.activity.ThinkingPolicy

/**
 * Prism Chat Screen
 * 
 * Main chat interface for the Prism monolith.
 */
@Composable
fun PrismChatScreen(
    viewModel: PrismViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onAudioBadgeClick: () -> Unit = {},
    onTingwuClick: () -> Unit = {}, // v2.6.5 Floating Action
    onArtifactsClick: () -> Unit = {} // v2.6.5 Floating Action
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val history by viewModel.history.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sessionTitle by viewModel.sessionTitle.collectAsState() // Observe Title
    val agentActivity by viewModel.agentActivity.collectAsState() // Agent Activity (Thinking Trace)

    PrismChatContent(
        currentMode = currentMode,
        history = history,
        uiState = uiState,
        inputText = inputText,
        isSending = isSending,
        errorMessage = errorMessage,
        sessionTitle = sessionTitle, // Pass Title
        onModeSwitch = viewModel::switchMode,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::send,
        onClearError = viewModel::clearError,
        onMenuClick = onMenuClick,
        onNewSessionClick = viewModel::startNewSession, // Connect to VM
        onAudioBadgeClick = onAudioBadgeClick,
        onDebugClick = viewModel::cycleDebugState,
        onTitleChange = viewModel::updateSessionTitle,
        onTingwuClick = onTingwuClick,
        onArtifactsClick = onArtifactsClick,
        agentActivity = agentActivity
    )
}

@Composable
private fun PrismChatContent(
    currentMode: Mode,
    history: List<ChatMessage>,
    uiState: UiState,
    inputText: String,
    isSending: Boolean,
    errorMessage: String?,
    sessionTitle: String, // v2.6
    onModeSwitch: (Mode) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onClearError: () -> Unit,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onAudioBadgeClick: () -> Unit,
    onDebugClick: () -> Unit,
    onTitleChange: (String) -> Unit, // v2.6
    onTingwuClick: () -> Unit,
    onArtifactsClick: () -> Unit,
    agentActivity: AgentActivity? = null
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        // 聊天头部 (v2.6 Updated: Editable Title + Placeholders)
        ChatHeader(
            sessionTitle = sessionTitle,
            onMenuClick = onMenuClick,
            onNewSessionClick = onNewSessionClick,
            onAudioBadgeClick = onAudioBadgeClick,
            onDebugClick = { 
                onDebugClick()
                Toast.makeText(context, "调试状态切换中...", Toast.LENGTH_SHORT).show()
            },
            onTitleChange = onTitleChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        // v2.6: Home Hero (Breathing Aura + Greeting)
        // Only visible when history is empty (Clean Desk state)
        if (history.isEmpty()) {
            HomeHero(context = context)
            Spacer(modifier = Modifier.height(16.dp))
        }



        // --- TRANSIENT ACTIVITY BANNER (Not persisted to history) ---
        // Agent Activity Banner (Two-tier: Phase + Action + Trace)
        agentActivity?.let { activity ->
            val maxLines = ThinkingPolicy.maxTraceLines(currentMode)
            AgentActivityBanner(
                activity = activity,
                maxLines = maxLines
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Legacy transient states (Parsing/Executing) — fallback
        if (agentActivity == null) {
            when (uiState) {
                is UiState.AnalystParsing -> {
                    com.smartsales.prism.ui.components.ThinkingTicker(
                        text = uiState.ticker,
                        progress = uiState.progress
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is UiState.AnalystExecuting -> {
                    Text("⚙️ ${uiState.planTitle}", color = Color.Cyan) 
                    Spacer(modifier = Modifier.height(8.dp))
                }
                // Note: UiState.Thinking now handled by AgentActivityBanner above
                else -> {} // Cards are rendered in LazyColumn via history
            }
        }

        // --- MAIN CONTENT AREA (weighted) ---
        // Single Box that contains:
        // - LazyColumn (always, for chat history)
        // - Floating Actions (overlaid when history is empty)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 对话区域 (always present, may be empty)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                reverseLayout = true
            ) {
                // 1. 当前正在进行的响应 (Loading/Thinking/Streaming)
                if (uiState !is UiState.Idle && uiState !is UiState.Error) {
                    item {
                        when (uiState) {
                            // Note: UiState.Thinking now handled by AgentActivityBanner
                            is UiState.Response -> {
                                 ResponseBubble(uiState = uiState as UiState.Response)
                            }
                            else -> {}
                        }
                    }
                }

                // 2. 历史消息 (包括 Plan Card 和 Artifact Card)
                items(history.reversed()) { message ->
                    when (message) {
                        is ChatMessage.User -> UserBubble(text = message.content)
                        is ChatMessage.Ai -> {
                            when (val state = message.uiState) {
                                is UiState.Response -> ResponseBubble(uiState = state)
                                is UiState.AnalystProposal -> {
                                    com.smartsales.prism.ui.components.AnalystProposalCard(
                                        plan = state.plan
                                    )
                                }
                                is UiState.AnalystResult -> {
                                    com.smartsales.prism.ui.components.ArtifactCard(
                                        artifact = state.artifact,
                                        onFullView = { Toast.makeText(context, "正在打开全文...", Toast.LENGTH_SHORT).show() },
                                        onDownload = { Toast.makeText(context, "正在下载 PDF...", Toast.LENGTH_SHORT).show() },
                                        onShare = { Toast.makeText(context, "正在分享...", Toast.LENGTH_SHORT).show() }
                                    )
                                }
                                is UiState.PlanCard -> {
                                    PlanCard(
                                        plan = state.plan,
                                        completedSteps = state.completedSteps
                                    )
                                }
                                else -> Text("暂不支持的消息类型", color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Floating Actions (Clean Desk - overlaid on right side when no history)
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    HomeFloatingActions(
                        onTingwuClick = onTingwuClick,
                        onArtifactsClick = onArtifactsClick
                    )
                }
            }
        }

        // 错误提示
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(vertical = 8.dp),
                action = { TextButton(onClick = onClearError) { Text("关闭", color = Color.White) } },
                containerColor = Color(0xFF3D2020)
            ) { Text(error, color = Color.White) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 模式切换栏 (Moved to Bottom per Spec §1.2)
        ModeToggleBar(
            currentMode = currentMode,
            onModeSwitch = onModeSwitch
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 输入栏
        InputBar(
            text = inputText,
            isSending = isSending,
            onTextChanged = onInputChanged,
            onSend = onSend,
            onAttachClick = { /* Legacy, now unused */ },
            onMicClick = { Toast.makeText(context, "语音备注（手机麦克风）", Toast.LENGTH_SHORT).show() },
            onUploadFile = { Toast.makeText(context, "上传文件...", Toast.LENGTH_SHORT).show() },
            onUploadImage = { Toast.makeText(context, "上传图片...", Toast.LENGTH_SHORT).show() },
            onUploadAudio = { Toast.makeText(context, "上传音频...", Toast.LENGTH_SHORT).show() }
        )
    }
}

// v2.6: Spec §1.1 Home Hero
@Composable
private fun HomeHero(context: android.content.Context) {
    val infiniteTransition = rememberInfiniteTransition(label = "Aura")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AuraScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Breathing Aura (Purple/Aurora)
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.8f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF9C27B0).copy(alpha = 0.4f), Color.Transparent)
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        ) {
            // Core Icon (Sparkle/Brain)
            Icon(
                imageVector = Icons.Default.Star, // Fallback to Star (Core) to avoid dependency issues
                contentDescription = "AI Aura",
                tint = Color(0xFFE1BEE7),
                modifier = Modifier.size(48.dp).align(Alignment.Center)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Greeting Text (Time Aware)
        // Fallback logic for simplicity in this iteration, ideal involves Calendar
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "上午好"
            in 12..18 -> "下午好"
            else -> "晚上好"
        }
        Text(
            text = "$greeting, Frank",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModeToggleBar(
    currentMode: Mode,
    onModeSwitch: (Mode) -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current // For Haptics
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val validModes = listOf(Mode.COACH, Mode.ANALYST)
        
        validModes.forEach { mode ->
            val isSelected = mode == currentMode
            val (icon, label, color) = when (mode) {
                Mode.COACH -> Triple("💬", "Coach", Color(0xFF9C27B0))
                Mode.ANALYST -> Triple("🔬", "Analyst", Color(0xFF2196F3))
                else -> Triple("?", "Unknown", Color.Gray)
            }

            TextButton(
                onClick = { 
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) // v2.6 Haptic
                    onModeSwitch(mode) 
                },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
            ) {
                Text(
                    text = "$icon $label",
                    color = if (isSelected) color else Color(0xFF888888),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 聊天头部 — spec §1.1 (v2.6 Refined)
 */
@Composable
private fun ChatHeader(
    sessionTitle: String,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onAudioBadgeClick: () -> Unit,
    onDebugClick: () -> Unit,
    onTitleChange: (String) -> Unit
) {
    // Removed: isEditing and tempTitle states - Title is auto-generated, not editable

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D1A))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧: 菜单 + 设备状态
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, "History", tint = Color.White)
            }
            // Device State: Wrapped in Box for easier clicking (Min 48dp target)
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .clickable { onAudioBadgeClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📶", fontSize = 16.sp)
            }
            // Text(
            //    text = "📶", 
            //    fontSize = 16.sp,
            //    modifier = Modifier.padding(start = 8.dp).clickable { onAudioBadgeClick() }
            // )
        }
        
        // 中间: 会话标题 (v2.6 Auto-Generated Display ONLY)
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "Session: $sessionTitle",
                color = Color.White,
                fontSize = 14.sp
                // Removed: clickable { isEditing = true } - Title is auto-generated
            )
        }
        
        // 右侧: [🐞] [➕] (Clean Header - Removed Avatar/Floating Actions)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDebugClick) {
                Icon(Icons.Filled.BugReport, "Debug", tint = Color(0xFF666666))
            }
            IconButton(onClick = onNewSessionClick) {
                Icon(Icons.Filled.Add, "New Session", tint = Color.White)
            }
        }
    }
}

@Composable
private fun HomeFloatingActions(
    onTingwuClick: () -> Unit,
    onArtifactsClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tingwu FAB [≣]
        FloatingActionButton(
            onClick = onTingwuClick,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
            modifier = Modifier.size(56.dp)
        ) {
            Text("≣", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        // Artifacts FAB [📦]
        FloatingActionButton(
            onClick = onArtifactsClick,
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
            modifier = Modifier.size(56.dp)
        ) {
            Text("📦", fontSize = 20.sp)
        }
    }
}
