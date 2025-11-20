package com.smartsales.feature.chat.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.chat.core.QuickSkillId

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，渲染聊天、快捷技能以及输入区
// 作者：创建于 2025-11-20

@Composable
fun HomeScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onInputChanged = viewModel::onInputChanged,
        onSendClicked = viewModel::onSendMessage,
        onQuickSkillSelected = viewModel::onSelectQuickSkill,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuickSkillSection(
            skills = state.quickSkills,
            enabled = !state.isSending,
            onQuickSkillSelected = onQuickSkillSelected
        )
        ChatHistoryList(
            messages = state.chatMessages,
            quickSkills = state.quickSkills,
            onQuickSkillSelected = onQuickSkillSelected,
            quickSkillsEnabled = !state.isSending
        )
        ChatInputBar(
            value = state.inputText,
            enabled = !state.isSending,
            onValueChanged = onInputChanged,
            onSendClicked = onSendClicked
        )
    }
}

@Composable
private fun QuickSkillSection(
    skills: List<QuickSkillUi>,
    enabled: Boolean,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    if (skills.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTitle) {
            Text(text = "快捷技能", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(skills) { skill ->
                AssistChip(
                    onClick = { onQuickSkillSelected(skill.id) },
                    enabled = enabled,
                    label = {
                        Text(
                            text = skill.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (skill.isRecommended) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryList(
    messages: List<ChatMessageUi>,
    quickSkills: List<QuickSkillUi>,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    quickSkillsEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (messages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "还没有对话，试试下面的快捷技能开始吧！")
                QuickSkillSection(
                    skills = quickSkills,
                    enabled = quickSkillsEnabled,
                    onQuickSkillSelected = onQuickSkillSelected,
                    modifier = Modifier.fillMaxWidth(),
                    showTitle = false
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessageUi) {
    val bubbleColor = when (message.role) {
        ChatMessageRole.USER -> MaterialTheme.colorScheme.primaryContainer
        ChatMessageRole.ASSISTANT -> MaterialTheme.colorScheme.secondaryContainer
    }
    Card(colors = CardDefaults.cardColors(containerColor = bubbleColor)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = message.content)
            if (message.isStreaming) {
                Text(text = "AI 回复中...", style = MaterialTheme.typography.bodySmall)
            }
            if (message.hasError) {
                Text(
                    text = "发送失败，稍后重试",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    enabled: Boolean,
    onValueChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "输入问题") },
                enabled = enabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSendClicked,
                    enabled = value.isNotBlank() && enabled
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "发送")
                }
            }
        }
    }
}
