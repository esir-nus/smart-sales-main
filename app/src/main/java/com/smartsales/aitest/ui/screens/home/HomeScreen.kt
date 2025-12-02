package com.smartsales.aitest.ui.screens.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/home/HomeScreen.kt
// 模块：:app
// 说明：底部导航 Home 页聊天体验，接入 feature/chat 的真实聊天 ViewModel
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.aitest.ui.components.ChatMessageBubble
import com.smartsales.aitest.ui.components.ChatWelcomeScreen
import com.smartsales.aitest.ui.components.SkillChips
import com.smartsales.aitest.ui.screens.home.model.SkillSuggestion
import com.smartsales.aitest.ui.screens.home.model.toSkillSuggestion
import com.smartsales.aitest.ui.screens.home.model.toUiMessages
import com.smartsales.feature.chat.home.HomeScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val messages = uiState.chatMessages.toUiMessages()
    val skills: List<SkillSuggestion> = uiState.quickSkills.map { it.toSkillSuggestion() }
    val isLoading = uiState.isStreaming || uiState.isSending
    val showWelcome = uiState.showWelcomeHero && uiState.chatMessages.isEmpty()
    val inputEnabled = !uiState.isInputBusy && !uiState.isBusy
    val canSend = uiState.inputText.isNotBlank() && !uiState.isBusy

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "新对话") },
                actions = {
                    IconButton(onClick = { viewModel.onNewChatClicked() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "新建对话")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("page_home"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (showWelcome) {
                    ChatWelcomeScreen(userName = uiState.userName, modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            ChatMessageBubble(message = message)
                        }
                        if (isLoading) {
                            item(key = "typing") {
                                TypingIndicator()
                            }
                        }
                    }
                }
            }
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Column {
                    uiState.chatErrorMessage?.let { error ->
                        ErrorBanner(
                            message = error,
                            onDismiss = { viewModel.onDismissError() }
                        )
                    }
                    if (uiState.chatMessages.isEmpty()) {
                        SkillChips(
                            skills = skills,
                            onSkillClick = { skill -> viewModel.onSelectQuickSkill(skill.id) }
                        )
                    }
                    ChatInputArea(
                        inputText = uiState.inputText,
                        onInputChange = viewModel::onInputChanged,
                        onSend = {
                            if (canSend) {
                                viewModel.onSendMessage()
                            }
                        },
                        inputEnabled = inputEnabled,
                        canSend = canSend
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    inputEnabled: Boolean,
    canSend: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            enabled = inputEnabled,
            placeholder = { Text(text = "输入消息...") }
        )
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Filled.Send, contentDescription = "发送")
        }
    }
}

@Composable
private fun TypingIndicator() {
    Text(
        text = "正在思考...",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭错误"
                )
            }
        }
    }
}
