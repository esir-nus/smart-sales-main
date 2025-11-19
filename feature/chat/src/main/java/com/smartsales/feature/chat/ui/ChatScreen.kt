// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/ui/ChatScreen.kt
// 模块：:feature:chat
// 说明：提供聊天界面Compose布局和交互钩子
// 作者：创建于 2025-11-14
package com.smartsales.feature.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportResult
import com.smartsales.feature.chat.ChatExportState
import com.smartsales.feature.chat.ChatMessage
import com.smartsales.feature.chat.ChatRole
import com.smartsales.feature.chat.ChatSkill
import com.smartsales.feature.chat.ChatState
import com.smartsales.feature.chat.TranscriptState

// 文件路径: feature/chat/src/main/java/com/smartsales/feature/chat/ui/ChatScreen.kt
// 文件作用: 提供聊天模块的Compose UI骨架
// 最近修改: 2025-11-14
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPanel(
    state: ChatState,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onSkillToggle: (ChatSkill) -> Unit,
    onCopyMarkdown: () -> Unit,
    onExport: (ExportFormat) -> Unit,
    onTranscriptRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(20.dp)) {
        Text(
            text = "欢迎回来，智能销售助手",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "选择技能",
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatSkill.entries.forEach { skill ->
                FilterChip(
                    selected = state.selectedSkills.contains(skill),
                    onClick = { onSkillToggle(skill) },
                    label = { Text(text = skill.label) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Divider()
        Spacer(modifier = Modifier.height(12.dp))
        ChatHistoryList(
            messages = state.messages,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 480.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.isSending) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Qwen 正在思考…")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = state.draft,
            onValueChange = onDraftChange,
            label = { Text(text = "向 Qwen 输入问题") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onSend(state.draft) },
                enabled = state.draft.isNotBlank() && !state.isSending
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "发送")
            }
            OutlinedButton(onClick = { onTranscriptRequest("demo_meeting.wav") }) {
                Text(text = "加载示例转写")
            }
        }
        TranscriptStatus(state.transcriptState)
        Spacer(modifier = Modifier.height(12.dp))
        MarkdownCard(
            markdown = state.structuredMarkdown,
            clipboardMessage = state.clipboardMessage,
            onCopyMarkdown = onCopyMarkdown
        )
        Spacer(modifier = Modifier.height(12.dp))
        ExportRow(state = state.exportState, onExport = onExport)
        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ChatHistoryList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "暂无对话，发送首条消息开始分析。")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(items = messages, key = { it.id }) { message ->
                    val bubbleColor = when (message.role) {
                        ChatRole.User -> MaterialTheme.colorScheme.primaryContainer
                        ChatRole.Assistant -> MaterialTheme.colorScheme.secondaryContainer
                        ChatRole.Transcript -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                    Surface(
                        color = bubbleColor,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = when (message.role) {
                                    ChatRole.User -> "用户"
                                    ChatRole.Assistant -> "助手"
                                    ChatRole.Transcript -> "转写"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = message.content)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptStatus(state: TranscriptState) {
    when (state) {
        TranscriptState.Idle -> Unit
        is TranscriptState.InProgress -> {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "转写进行中 (${state.percent}%) - ${state.sourceName}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        is TranscriptState.Ready -> {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "已注入转写：${state.sourceName}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        is TranscriptState.Error -> {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MarkdownCard(
    markdown: String?,
    clipboardMessage: String?,
    onCopyMarkdown: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "结构化 Markdown", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onCopyMarkdown, enabled = !markdown.isNullOrBlank()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = markdown ?: "暂未生成结构化内容。")
            if (!clipboardMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = clipboardMessage, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ExportRow(
    state: ChatExportState,
    onExport: (ExportFormat) -> Unit
) {
    Column {
        Text(text = "导出", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onExport(ExportFormat.PDF) }) {
                Text(text = "PDF")
            }
            OutlinedButton(onClick = { onExport(ExportFormat.CSV) }) {
                Text(text = "CSV")
            }
        }
        when (state) {
            ChatExportState.Idle -> Unit
            is ChatExportState.InProgress -> Text(text = "正在导出 ${state.format} ...")
            is ChatExportState.Completed -> Text(text = "已生成 ${state.result.fileName}")
            is ChatExportState.Failed -> Text(
                text = state.reason,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview
@Composable
private fun ChatPanelPreview() {
    val sampleMessages = listOf(
        ChatMessage(role = ChatRole.User, content = "请帮我总结与零售客户的会议纪要"),
        ChatMessage(role = ChatRole.Assistant, content = "已为你整理重点，请查看结构化内容。")
    )
    ChatPanel(
        state = ChatState(
            draft = "下一步行动？",
            messages = sampleMessages,
            structuredMarkdown = "## 摘要\n- 客户确认下单\n- 待提供折扣方案",
            exportState = ChatExportState.Completed(
                ExportResult(
                    fileName = "demo.pdf",
                    mimeType = "application/pdf",
                    payload = ByteArray(0)
                )
            ),
            transcriptState = TranscriptState.Ready("job-1", "## 音频", "demo_meeting.wav")
        ),
        onDraftChange = {},
        onSend = {},
        onSkillToggle = {},
        onCopyMarkdown = {},
        onExport = {},
        onTranscriptRequest = {}
    )
}
