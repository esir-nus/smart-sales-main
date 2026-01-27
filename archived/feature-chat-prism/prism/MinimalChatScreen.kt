package com.smartsales.feature.chat.prism

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.UiState
import com.smartsales.feature.chat.prism.components.InputBar
import com.smartsales.feature.chat.prism.components.ModeToggleBar
import com.smartsales.feature.chat.prism.components.ResponseBubble
import com.smartsales.feature.chat.prism.components.ThinkingBox

/**
 * 最小化聊天界面 — Prism 集成测试屏幕
 * @see Prism-V1.md §2.2
 */
@Composable
fun MinimalChatScreen(
    viewModel: PrismViewModel = hiltViewModel()
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    MinimalChatContent(
        currentMode = currentMode,
        uiState = uiState,
        inputText = inputText,
        isSending = isSending,
        errorMessage = errorMessage,
        onModeSwitch = viewModel::switchMode,
        onInputChanged = viewModel::onInputChanged,
        onSend = viewModel::send,
        onClearError = viewModel::clearError
    )
}

@Composable
private fun MinimalChatContent(
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
            // 根据 UiState 显示内容
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

        Spacer(modifier = Modifier.height(16.dp))

        // Input Tools Skeleton Playground (Temporary Verification)
        ToolsPlayground()
    }
}

@Composable
private fun ToolsPlayground() {
    var isRecording by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222233), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Text("🛠️ Skeleton Playground", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        
        // MOL-08 Tingwu
        com.smartsales.feature.chat.prism.components.TingwuRecorder(
            isRecording = isRecording,
            amplitude = if (isRecording) 0.8f else 0f,
            onStart = { isRecording = true },
            onStop = { isRecording = false }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // MOL-09 Vision
            com.smartsales.feature.chat.prism.components.VisionPicker(onImageSelected = {})
            
            // MOL-10 BLE
            com.smartsales.feature.chat.prism.components.BleStatusIndicator(
                isConnected = isConnected,
                deviceName = "Glass V2",
                onConnect = { isConnected = !isConnected }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // MOL-11 URL
        com.smartsales.feature.chat.prism.components.UrlInputBox(
            url = url,
            isFetching = isFetching,
            onUrlChanged = { url = it },
            onFetch = { 
                isFetching = true 
                // Mock fetch
            }
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MinimalChatContentPreview() {
    MinimalChatContent(
        currentMode = Mode.COACH,
        uiState = UiState.Response(
            content = "根据您与张总的交流记录，我发现他对A3打印机方案表现出较高兴趣，但对售后服务有顾虑。",
            structuredJson = null
        ),
        inputText = "",
        isSending = false,
        errorMessage = null,
        onModeSwitch = {},
        onInputChanged = {},
        onSend = {},
        onClearError = {}
    )
}
