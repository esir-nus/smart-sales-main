package com.smartsales.prism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.core.Mode
import com.smartsales.prism.domain.core.UiState

/**
 * Prism Chat Screen
 * 
 * Main chat interface for the Prism monolith.
 */
@Composable
fun PrismChatScreen(
    viewModel: PrismViewModel = hiltViewModel()
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    PrismChatContent(
        currentMode = currentMode,
        uiState = uiState,
        inputText = inputText,
        isSending = isSending,
        errorMessage = errorMessage,
        onModeSwitch = viewModel::switchMode,
        onInputChanged = viewModel::updateInput,
        onSend = viewModel::send,
        onClearError = viewModel::clearError
    )
}

@Composable
private fun PrismChatContent(
    currentMode: Mode,
    uiState: UiState,
    inputText: String,
    isSending: Boolean,
    errorMessage: String?,
    onModeSwitch: (Mode) -> Unit,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D1A))
            .padding(16.dp)
    ) {
        // 模式切换栏
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ResponseBubble(uiState = uiState)
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

        // 输入栏
        InputBar(
            text = inputText,
            isSending = isSending,
            onTextChanged = onInputChanged,
            onSend = onSend
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
        Mode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            val (icon, label) = when (mode) {
                Mode.COACH -> "💬" to "Coach"
                Mode.ANALYST -> "🔬" to "Analyst"
                Mode.SCHEDULER -> "📅" to "Scheduler"
            }

            TextButton(
                onClick = { onModeSwitch(mode) },
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) Color(0xFF3A3A5A) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
            ) {
                Text(
                    text = "$icon $label",
                    color = if (isSelected) Color.White else Color(0xFF888888),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ResponseBubble(uiState: UiState) {
    val (message, color) = when (uiState) {
        is UiState.Idle -> "等待输入..." to Color(0xFF666666)
        is UiState.Loading -> "加载中..." to Color(0xFF4FC3F7)
        is UiState.Thinking -> (uiState.hint ?: "思考中...") to Color(0xFF4FC3F7)
        is UiState.Streaming -> uiState.partialContent to Color.White
        is UiState.Response -> uiState.content to Color.White
        is UiState.Error -> "❌ ${uiState.message}" to Color(0xFFFF6B6B)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = message,
            color = color,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun InputBar(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("输入消息...", color = Color(0xFF666666))
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF4FC3F7),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4FC3F7),
                disabledContainerColor = Color(0xFF333333)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("➤", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}
