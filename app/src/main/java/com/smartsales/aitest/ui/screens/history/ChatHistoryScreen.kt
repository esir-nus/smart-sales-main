package com.smartsales.aitest.ui.screens.history

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/history/ChatHistoryScreen.kt
// 模块：:app
// 说明：聊天记录页（本地模拟），支持分组、重命名、置顶与删除
// 作者：创建于 2025-12-02

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartsales.aitest.ui.components.ChatHistoryItem
import com.smartsales.aitest.ui.components.ChatSessionActionsSheet
import com.smartsales.aitest.ui.screens.history.model.ChatHistoryGroupUi
import com.smartsales.aitest.ui.screens.history.model.ChatSessionUi
import com.smartsales.aitest.ui.screens.history.model.groupSessionsByTime
import java.util.UUID
import java.util.concurrent.TimeUnit

// 说明：底部导航已使用 ChatHistoryShell + feature/chat 的 ChatHistoryRoute，此文件保留作旧版本地模拟，不再挂接导航入口。

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen() {
    var sessions by remember { mutableStateOf(getMockSessions()) }
    var selectedSession by remember { mutableStateOf<ChatSessionUi?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }

    val groupedSessions: List<ChatHistoryGroupUi> = remember(sessions) {
        groupSessionsByTime(sessions)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "聊天记录") },
                actions = {
                    IconButton(onClick = { /* TODO: 搜索 */ }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("page_chat_history"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (groupedSessions.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedSessions.forEach { group ->
                        item(key = group.timeLabel) {
                            Text(
                                text = group.timeLabel,
                                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(group.sessions, key = { it.id }) { session ->
                            ChatHistoryItem(
                                session = session,
                                onClick = { /* TODO: resume chat */ },
                                onLongClick = { selectedSession = session }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSession?.let { session ->
        if (!showRenameDialog && !showDeleteDialog) {
            ChatSessionActionsSheet(
                session = session,
                onDismiss = { selectedSession = null },
                onRename = {
                    renameText = session.title
                    showRenameDialog = true
                },
                onPinToggle = {
                    sessions = sessions.map {
                        if (it.id == session.id) it.copy(isPinned = !it.isPinned) else it
                    }
                    selectedSession = null
                },
                onDelete = {
                    showDeleteDialog = true
                }
            )
        }
    }

    if (showRenameDialog && selectedSession != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(text = "重命名会话") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text(text = "新标题") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val targetId = selectedSession?.id
                    if (!renameText.isBlank() && targetId != null) {
                        sessions = sessions.map {
                            if (it.id == targetId) it.copy(title = renameText.trim()) else it
                        }
                        selectedSession = null
                    }
                    showRenameDialog = false
                }) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(text = "取消") }
            }
        )
    }

    if (showDeleteDialog && selectedSession != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "删除会话") },
            text = { Text(text = "确定要删除这个会话吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    val targetId = selectedSession?.id
                    if (targetId != null) {
                        sessions = sessions.filterNot { it.id == targetId }
                        selectedSession = null
                    }
                    showDeleteDialog = false
                }) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(text = "取消") }
            }
        )
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ChatBubbleOutline,
            contentDescription = null,
            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "暂无聊天记录",
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "新的对话会显示在这里",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun getMockSessions(): List<ChatSessionUi> {
    val now = System.currentTimeMillis()
    return listOf(
        ChatSessionUi(
            id = "session_pinned",
            title = "产品咨询 - 客户王先生",
            preview = "客户询问了产品价格和功能，我们介绍了主要卖点…",
            messageCount = 18,
            createdAt = now - TimeUnit.HOURS.toMillis(6),
            lastMessageAt = now - TimeUnit.HOURS.toMillis(2),
            isPinned = true
        ),
        ChatSessionUi(
            id = "session_today",
            title = "销售数据分析",
            preview = "导出了销售报表，需要生成洞察…",
            messageCount = 9,
            createdAt = now - TimeUnit.HOURS.toMillis(5),
            lastMessageAt = now - TimeUnit.HOURS.toMillis(1),
            isPinned = false
        ),
        ChatSessionUi(
            id = "session_yesterday",
            title = "话术辅导",
            preview = "对方提出折扣要求，建议的回复是…",
            messageCount = 12,
            createdAt = now - TimeUnit.DAYS.toMillis(1) - TimeUnit.HOURS.toMillis(2),
            lastMessageAt = now - TimeUnit.DAYS.toMillis(1),
            isPinned = false
        )
    )
}
