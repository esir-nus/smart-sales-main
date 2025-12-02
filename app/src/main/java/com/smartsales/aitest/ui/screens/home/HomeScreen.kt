package com.smartsales.aitest.ui.screens.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/home/HomeScreen.kt
// 模块：:app
// 说明：底部导航 Home 页的聊天 UI（本地模拟，无业务依赖）
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.ChatBubble
import com.smartsales.aitest.ui.components.ChatInput
import com.smartsales.aitest.ui.components.ChatWelcome
import com.smartsales.aitest.ui.screens.home.model.ChatMessage
import com.smartsales.aitest.ui.screens.home.model.MessageRole
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var input by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val userName = "用户" // TODO: 未来接入真实用户信息

    fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    Scaffold(
        bottomBar = {
            ChatInput(
                input = input,
                onInputChange = { input = it },
                onSend = {
                    if (input.isBlank() || isLoading) return@ChatInput
                    val userMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = MessageRole.USER,
                        content = input.trim()
                    )
                    messages.add(userMessage)
                    input = ""
                    scrollToBottom()
                    isLoading = true
                    scope.launch {
                        delay(1500)
                        messages.add(
                            ChatMessage(
                                role = MessageRole.ASSISTANT,
                                content = "这是一个模拟回复。真实的 AI 集成将在后续任务中实现。"
                            )
                        )
                        isLoading = false
                        scrollToBottom()
                    }
                },
                isLoading = isLoading,
                onSkillClick = { skill -> input = skill }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("page_home")
        ) {
            if (messages.isEmpty()) {
                ChatWelcome(userName = userName, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message = message)
                    }
                    if (isLoading) {
                        item(key = "loading") {
                            RowLoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowLoadingIndicator() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
            strokeWidth = 3.dp
        )
        Text(
            text = "正在思考...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}
