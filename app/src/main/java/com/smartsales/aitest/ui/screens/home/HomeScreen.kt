package com.smartsales.aitest.ui.screens.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/home/HomeScreen.kt
// 模块：:app
// 说明：底部导航 Home 页聊天体验（本地模拟，无后端依赖）
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.ChatMessageBubble
import com.smartsales.aitest.ui.components.ChatWelcomeScreen
import com.smartsales.aitest.ui.components.SkillChips
import com.smartsales.aitest.ui.screens.home.model.ChatMessage
import com.smartsales.aitest.ui.screens.home.model.MessageRole
import com.smartsales.aitest.ui.screens.home.model.SkillSuggestion
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val userName = "用户" // TODO: 接入真实用户
    val skills = remember {
        listOf(
            SkillSuggestion("skill_summary", "内容总结", "帮我总结这段通话的关键内容"),
            SkillSuggestion("skill_objection", "异议分析", "分析客户提出的异议和顾虑"),
            SkillSuggestion("skill_coach", "话术辅导", "给出应对建议和话术优化"),
            SkillSuggestion("skill_report", "生成日报", "根据今天的工作生成日报")
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "新对话") },
                actions = {
                    IconButton(onClick = {
                        messages = emptyList()
                        inputText = ""
                        isLoading = false
                    }) {
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
                if (messages.isEmpty()) {
                    ChatWelcomeScreen(userName = userName, modifier = Modifier.fillMaxSize())
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
                    if (messages.isEmpty()) {
                        SkillChips(
                            skills = skills,
                            onSkillClick = { skill -> inputText = skill.prompt }
                        )
                    }
                    ChatInputArea(
                        inputText = inputText,
                        onInputChange = { inputText = it },
                        onSend = {
                            if (inputText.isBlank() || isLoading) return@ChatInputArea
                            val prompt = inputText
                            val userMessage = ChatMessage(
                                id = UUID.randomUUID().toString(),
                                content = prompt.trim(),
                                role = MessageRole.USER,
                                timestamp = System.currentTimeMillis()
                            )
                            messages = messages + userMessage
                            inputText = ""
                            isLoading = true
                            scope.launch {
                                delay(1500)
                                val aiMessage = ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    content = "我理解您的需求。根据您的问题「$prompt」，我建议采用以下策略：\n\n1. 先了解客户的具体需求\n2. 提供针对性的解决方案\n3. 强调产品的独特价值\n\n您需要我进一步分析吗？",
                                    role = MessageRole.ASSISTANT,
                                    timestamp = System.currentTimeMillis()
                                )
                                messages = messages + aiMessage
                                isLoading = false
                            }
                        },
                        isLoading = isLoading
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
    isLoading: Boolean
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
            enabled = !isLoading,
            placeholder = { Text(text = "输入消息...") }
        )
        FilledIconButton(
            onClick = onSend,
            enabled = inputText.isNotBlank() && !isLoading,
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
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    )
}
