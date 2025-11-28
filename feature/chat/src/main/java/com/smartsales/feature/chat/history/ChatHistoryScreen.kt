@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.smartsales.feature.chat.history

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryScreen.kt
// 模块：:feature:chat
// 说明：渲染聊天历史列表，提供重命名、删除、置顶与跳转
// 作者：创建于 2025-11-21

object ChatHistoryTestTags {
    const val PAGE = "chat_history_page"
    const val EMPTY = "chat_history_empty"
    const val ERROR = "chat_history_error"
    const val SHEET = "chat_history_sheet"
    const val SHEET_PIN = "chat_history_sheet_pin"
    const val SHEET_RENAME = "chat_history_sheet_rename"
    const val SHEET_DELETE = "chat_history_sheet_delete"
    const val SHEET_CANCEL = "chat_history_sheet_cancel"
    fun item(sessionId: String) = "chat_history_item_$sessionId"
}

@Composable
fun ChatHistoryRoute(
    modifier: Modifier = Modifier,
    viewModel: ChatHistoryViewModel = hiltViewModel(),
    onSessionSelected: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChatHistoryEvent.NavigateToSession -> onSessionSelected(event.sessionId)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onDismissError()
        }
    }

    ChatHistoryScreen(
        state = state,
        onRefresh = viewModel::loadSessions,
        onSessionClicked = viewModel::onSessionClicked,
        onRenameSession = viewModel::onRenameSession,
        onDeleteSession = viewModel::onDeleteSession,
        onPinToggle = viewModel::onPinToggle,
        onDismissError = viewModel::onDismissError,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun ChatHistoryScreen(
    state: ChatHistoryUiState,
    onRefresh: () -> Unit,
    onSessionClicked: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onPinToggle: (String) -> Unit,
    onDismissError: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var renameTarget by rememberSaveable { mutableStateOf<ChatSessionUi?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var sheetSession by remember { mutableStateOf<ChatSessionUi?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ChatHistoryTestTags.PAGE),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "会话历史", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "长按会话可置顶、重命名或删除，保持与 React 历史侧边一致。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (state.sessions.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(ChatHistoryTestTags.EMPTY),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂无历史记录，开始新的对话吧。")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(state.sessions, key = { it.id }) { session ->
                        ChatHistoryItem(
                            session = session,
                            onClick = { onSessionClicked(session.id) },
                            onLongPress = { sheetSession = session },
                            modifier = Modifier.testTag(ChatHistoryTestTags.item(session.id))
                        )
                    }
                }
            }
        }
    }

    sheetSession?.let { session ->
        ModalBottomSheet(
            onDismissRequest = { sheetSession = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(ChatHistoryTestTags.SHEET),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SheetAction(
                    label = if (session.pinned) "取消置顶" else "置顶到顶部",
                    modifier = Modifier.testTag(ChatHistoryTestTags.SHEET_PIN),
                    onClick = {
                        onPinToggle(session.id)
                        sheetSession = null
                    }
                )
                SheetAction(
                    label = "重命名会话",
                    modifier = Modifier.testTag(ChatHistoryTestTags.SHEET_RENAME),
                    onClick = {
                        sheetSession = null
                        renameTarget = session
                        renameText = session.title
                    }
                )
                SheetAction(
                    label = "删除并清除消息",
                    modifier = Modifier.testTag(ChatHistoryTestTags.SHEET_DELETE),
                    onClick = {
                        onDeleteSession(session.id)
                        sheetSession = null
                    }
                )
                HorizontalDivider()
                SheetAction(
                    label = "取消",
                    modifier = Modifier.testTag(ChatHistoryTestTags.SHEET_CANCEL),
                    onClick = { sheetSession = null }
                )
            }
        }
    }

    if (state.errorMessage != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag(ChatHistoryTestTags.ERROR)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismissError) { Text(text = "知道了") }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(text = "重命名会话") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(text = "标题") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameSession(target.id, renameText.trim())
                        renameTarget = null
                    },
                    enabled = renameText.isNotBlank()
                ) { Text(text = "保存标题") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
private fun ChatHistoryItem(
    session: ChatSessionUi,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (session.pinned) {
                            Spacer(modifier = Modifier.size(8.dp))
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(text = "置顶") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = session.lastMessagePreview.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SheetAction(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
