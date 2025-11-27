package com.smartsales.feature.chat.history

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryScreen.kt
// 模块：:feature:chat
// 说明：渲染聊天历史列表，提供重命名、删除、置顶与跳转
// 作者：创建于 2025-11-27

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryScreen.kt
// 模块：:feature:chat
// 说明：渲染聊天历史列表，提供重命名、删除、置顶与跳转
// 作者：创建于 2025-11-21

object ChatHistoryTestTags {
    const val PAGE = "chat_history_page"
    const val HEADER = "chat_history_header"
    const val BACK = "chat_history_back"
    const val REFRESH = "chat_history_refresh"
    const val LOADING = "chat_history_loading"
    const val EMPTY = "chat_history_empty"
    const val ERROR = "chat_history_error"
    fun item(sessionId: String) = "chat_history_item_$sessionId"
}

private object HistoryPalette {
    val BackgroundStart = Color(0xFFF7F9FF)
    val BackgroundEnd = Color(0xFFEFF2F7)
    val Card = Color(0xFFFFFFFF)
    val CardMuted = Color(0xFFF8F9FB)
    val Border = Color(0xFFE5E5EA)
    val Accent = Color(0xFF4B7BEC)
    val Warning = Color(0xFFFFB020)
}

private object HistoryShapes {
    val Card = RoundedCornerShape(16.dp)
}

@Composable
fun ChatHistoryRoute(
    modifier: Modifier = Modifier,
    viewModel: ChatHistoryViewModel = hiltViewModel(),
    onSessionSelected: (String) -> Unit,
    onBackClick: () -> Unit = {}
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
        onBackClick = onBackClick,
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
    onBackClick: () -> Unit,
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(ChatHistoryTestTags.PAGE),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            HistoryCard(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag(ChatHistoryTestTags.HEADER)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(ChatHistoryTestTags.BACK)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    Text(
                        text = "会话历史",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag(ChatHistoryTestTags.REFRESH)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(HistoryPalette.BackgroundStart, HistoryPalette.BackgroundEnd)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (state.isLoading) {
                HistoryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ChatHistoryTestTags.LOADING)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(modifier = Modifier.weight(1f))
                        Text(
                            text = "正在加载历史会话...",
                            style = MaterialTheme.typography.bodySmall,
                            color = HistoryPalette.Border
                        )
                    }
                }
            }
            state.errorMessage?.let {
                ErrorBanner(
                    message = it,
                    onDismissError = onDismissError
                )
            }
            if (state.sessions.isEmpty() && !state.isLoading && state.errorMessage == null) {
                HistoryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ChatHistoryTestTags.EMPTY)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = "暂无会话历史，先从首页开始一次对话吧", color = HistoryPalette.Border)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    items(state.sessions, key = { it.id }) { session ->
                        ChatHistoryItem(
                            session = session,
                            formattedTime = formatSessionTime(session.updatedAt),
                            onClick = { onSessionClicked(session.id) },
                            onRename = {
                                renameTarget = session
                                renameText = session.title
                            },
                            onDelete = { onDeleteSession(session.id) },
                            onPinToggle = { onPinToggle(session.id) },
                            modifier = Modifier.testTag(ChatHistoryTestTags.item(session.id))
                        )
                    }
                }
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
                ) { Text(text = "保存") }
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
    formattedTime: String,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPinToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    HistoryCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderColor = if (session.pinned) HistoryPalette.Accent.copy(alpha = 0.4f) else HistoryPalette.Border
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (session.pinned) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                )
                            )
                        }
                    }
                    Text(
                        text = session.lastMessagePreview.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ActionMenu(
                    pinned = session.pinned,
                    onRename = onRename,
                    onDelete = onDelete,
                    onPinToggle = onPinToggle
                )
            }
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismissError: () -> Unit
) {
    HistoryCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(ChatHistoryTestTags.ERROR),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismissError) { Text(text = "知道了") }
        }
    }
}

@Composable
private fun HistoryCard(
    modifier: Modifier = Modifier,
    containerColor: Color = HistoryPalette.Card,
    borderColor: Color = HistoryPalette.Border,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.shadow(6.dp, shape = HistoryShapes.Card, clip = false),
        shape = HistoryShapes.Card,
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        content()
    }
}

private fun formatSessionTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun ActionMenu(
    pinned: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPinToggle: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(text = if (pinned) "取消置顶" else "置顶") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onPinToggle()
                }
            )
            DropdownMenuItem(
                text = { Text(text = "重命名") },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text(text = "删除") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}
