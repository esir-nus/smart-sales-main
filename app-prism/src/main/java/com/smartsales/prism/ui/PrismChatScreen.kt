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
import androidx.compose.material3.*
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
import com.smartsales.prism.ui.components.ThinkingBox
import com.smartsales.prism.ui.components.UserBubble

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
    onAudioBadgeClick: () -> Unit = {}
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val history by viewModel.history.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    PrismChatContent(
        currentMode = currentMode,
        history = history,
        uiState = uiState,
        inputText = inputText,
        isSending = isSending,
        errorMessage = errorMessage,
        onModeSwitch = viewModel::switchMode,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::send,
        onClearError = viewModel::clearError,
        onMenuClick = onMenuClick,
        onNewSessionClick = onNewSessionClick,
        onAudioBadgeClick = onAudioBadgeClick
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
    onModeSwitch: (Mode) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onClearError: () -> Unit,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onAudioBadgeClick: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .navigationBarsPadding() // 底部导航栏安全区
            .imePadding()            // 键盘弹出时推动内容
            .padding(16.dp)
    ) {
        // 聊天头部 (v2.5 Aligned)
        ChatHeader(
            onMenuClick = onMenuClick,
            onNewSessionClick = onNewSessionClick,
            onAudioBadgeClick = onAudioBadgeClick,
            onDebugClick = { 
                Toast.makeText(context, "Debug Cmds: /plan, /think, /md, /error", Toast.LENGTH_LONG).show() 
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 模式切换栏 (v2.5 Aligned: Coach/Analyst ONLY)
        ModeToggleBar(
            currentMode = currentMode,
            onModeSwitch = onModeSwitch
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 对话区域
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            reverseLayout = true // 从底部开始渲染
        ) {
            // 1. 当前正在进行的响应 (Loading/Thinking/Streaming)
            if (uiState !is UiState.Idle && uiState !is UiState.Error) {
                item {
                    when (uiState) {
                        is UiState.PlanCard -> {
                            PlanCard(
                                plan = (uiState as UiState.PlanCard).plan,
                                completedSteps = (uiState as UiState.PlanCard).completedSteps
                            )
                        }
                        is UiState.Thinking -> {
                            ThinkingBox(
                                content = (uiState as UiState.Thinking).hint ?: "正在分析...",
                                isComplete = false
                            )
                        }
                        else -> {
                           // Loading 状态暂不显示 specialized UI，或显示 LoadingBubble
                        }
                    }
                }
            }

            // 2. 历史消息 (倒序显示，因为 reverseLayout = true)
            items(history.reversed()) { message ->
                when (message) {
                    is ChatMessage.User -> {
                        UserBubble(text = message.content)
                    }
                    is ChatMessage.Ai -> {
                        when (val state = message.uiState) {
                            is UiState.Response -> ResponseBubble(uiState = state)
                            is UiState.PlanCard -> PlanCard(plan = state.plan, completedSteps = state.completedSteps)
                            else -> Text("暂不支持的消息类型", color = Color.Gray)
                        }
                    }
                }
            }
        }

        // 错误提示
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.padding(vertical = 8.dp),
                action = {
                    TextButton(onClick = onClearError) {
                        Text("关闭", color = Color.White)
                    }
                },
                containerColor = Color(0xFF3D2020)
            ) {
                Text(error, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 输入栏 (Unified Component)
        InputBar(
            text = inputText,
            isSending = isSending,
            onTextChanged = onInputChanged,
            onSend = onSend,
            onAttachClick = {
                Toast.makeText(context, "Attachment Picker (Max 11)", Toast.LENGTH_SHORT).show()
            },
            onMicClick = {
                Toast.makeText(context, "Voice Note (Phone Mic)", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun ModeToggleBar(
    currentMode: Mode,
    onModeSwitch: (Mode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // v2.5: Only Coach and Analyst are Modes. Scheduler is a Drawer.
        val validModes = listOf(Mode.COACH, Mode.ANALYST)
        
        validModes.forEach { mode ->
            val isSelected = mode == currentMode
            val (icon, label, color) = when (mode) {
                Mode.COACH -> Triple("💬", "Coach", Color(0xFF9C27B0))   // Purple
                Mode.ANALYST -> Triple("🔬", "Analyst", Color(0xFF2196F3)) // Blue
                else -> Triple("?", "Unknown", Color.Gray)
            }

            TextButton(
                onClick = { onModeSwitch(mode) },
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
 * 聊天头部 — spec §1.1
 */
@Composable
private fun ChatHeader(
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onAudioBadgeClick: () -> Unit,
    onDebugClick: () -> Unit // New (v2.5)
) {
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
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "History",
                    tint = Color.White
                )
            }
            
            // Audio Badge Trigger -> Connectivity Modal (v2.5)
            Text(
                text = "📶", // Device Status
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onAudioBadgeClick() }
            )
        }
        
        // 中间: 会话标题
        Text(
            text = "Session: 新对话",
            color = Color.White,
            fontSize = 14.sp
        )
        
        // 右侧: Debug + 新建会话
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Debug Toggle [🐞] (v2.5)
            IconButton(onClick = onDebugClick) {
                Icon(
                    imageVector = Icons.Filled.BugReport,
                    contentDescription = "Debug",
                    tint = Color(0xFF666666) // Dimmed
                )
            }
            
            IconButton(onClick = onNewSessionClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Session",
                    tint = Color.White
                )
            }
        }
    }
}
