package com.smartsales.feature.chat.home

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.chat.core.QuickSkillId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，渲染设备/音频提示、聊天记录、快捷技能与输入框
// 作者：创建于 2025-11-20

@Composable
fun HomeScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    sessionId: String? = null,
    transcriptionRequest: TranscriptionChatRequest? = null,
    onTranscriptionRequestConsumed: () -> Unit = {},
    selectedSessionId: String? = null,
    onSessionSelectionConsumed: () -> Unit = {},
    onNavigateToDeviceManager: () -> Unit = {},
    onNavigateToDeviceSetup: () -> Unit = {},
    onNavigateToAudioFiles: () -> Unit = {},
    onNavigateToUserCenter: () -> Unit = {},
    onNavigateToChatHistory: () -> Unit = {},
    onDeviceSnapshotChanged: (DeviceSnapshotUi?) -> Unit = {},
) {
    Log.i("HomeScreenRoute", "HomeScreenRoute composed - entering function")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // 抽屉开关保留在 Route 层，避免影响 ViewModel 状态
    var showHistoryPanel by rememberSaveable { mutableStateOf(false) }

    // Snackbar 展示来自 ViewModel 的一次性提醒
    LaunchedEffect(state.snackbarMessage) {
        val message = state.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onDismissError()
        }
    }

    // 监听导航请求并通过回调交给宿主 Activity
    LaunchedEffect(state.navigationRequest) {
        when (state.navigationRequest) {
            HomeNavigationRequest.DeviceManager -> onNavigateToDeviceManager()
            HomeNavigationRequest.DeviceSetup -> onNavigateToDeviceSetup()
            HomeNavigationRequest.AudioFiles -> onNavigateToAudioFiles()
            HomeNavigationRequest.UserCenter -> onNavigateToUserCenter()
            HomeNavigationRequest.ChatHistory -> onNavigateToChatHistory()
            else -> Unit
        }
        if (state.navigationRequest != null) {
            viewModel.onNavigationConsumed()
        }
    }
    LaunchedEffect(transcriptionRequest) {
        transcriptionRequest?.let {
            viewModel.onTranscriptionRequested(it)
            onTranscriptionRequestConsumed()
        }
    }
    LaunchedEffect(selectedSessionId) {
        selectedSessionId?.let {
            val currentId = state.currentSession.id
            if (it != currentId) {
                viewModel.setSession(it)
            }
            onSessionSelectionConsumed()
        }
    }
    LaunchedEffect(sessionId) {
        sessionId?.let { viewModel.setSession(it) }
    }
    LaunchedEffect(state.deviceSnapshot) {
        onDeviceSnapshotChanged(state.deviceSnapshot)
    }

    HomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onInputChanged = viewModel::onInputChanged,
        onSendClicked = viewModel::onSendMessage,
        onQuickSkillSelected = viewModel::onSelectQuickSkill,
        onDeviceBannerClicked = viewModel::onTapDeviceBanner,
        onAudioSummaryClicked = viewModel::onTapAudioSummary,
        onRefreshDeviceAndAudio = viewModel::onRefreshDeviceAndAudio,
        onLoadMoreHistory = viewModel::onLoadMoreHistory,
        onProfileClicked = viewModel::onTapProfile,
        onNewChatClicked = viewModel::onNewChatClicked,
        onSessionSelected = viewModel::setSession,
        chatErrorMessage = state.chatErrorMessage,
        modifier = modifier,
        showHistoryPanel = showHistoryPanel,
        onToggleHistoryPanel = { viewModel.onOpenDrawer() },
        onDismissHistoryPanel = { showHistoryPanel = false },
        historySessions = state.sessionList.take(10),
        onHistorySessionSelected = { sessionId ->
            viewModel.setSession(sessionId)
            showHistoryPanel = false
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onDeviceBannerClicked: () -> Unit,
    onAudioSummaryClicked: () -> Unit,
    onRefreshDeviceAndAudio: () -> Unit,
    onLoadMoreHistory: () -> Unit,
    onProfileClicked: () -> Unit,
    onNewChatClicked: () -> Unit = {},
    onSessionSelected: (String) -> Unit = {},
    chatErrorMessage: String? = null,
    modifier: Modifier = Modifier,
    showHistoryPanel: Boolean = false,
    onToggleHistoryPanel: () -> Unit = {},
    onDismissHistoryPanel: () -> Unit = {},
    historySessions: List<SessionListItemUi> = emptyList(),
    onHistorySessionSelected: (String) -> Unit = {}
) {
    Log.i("HomeScreen", "HomeScreen composed - entering function")
    val refreshingState = remember { mutableStateOf(false) }
    LaunchedEffect(state.deviceSnapshot, state.audioSummary) {
        refreshingState.value = false
    }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshingState.value,
        onRefresh = {
            if (!refreshingState.value) {
                refreshingState.value = true
                onRefreshDeviceAndAudio()
            }
        }
    )

    val listState = rememberLazyListState()
    val showScrollToLatest = remember { mutableStateOf(false) }
    LaunchedEffect(listState, state.chatMessages.size, state.isLoadingHistory) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect { atTop ->
            if (atTop && !state.isLoadingHistory && state.chatMessages.isNotEmpty()) {
                onLoadMoreHistory()
            }
        }
    }
    LaunchedEffect(listState, state.chatMessages.size) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val lastMessageIndex = state.chatMessages.lastIndex.takeIf { it >= 0 }
                showScrollToLatest.value = when {
                    lastMessageIndex == null -> false
                    lastVisibleIndex == null -> false
                    else -> lastVisibleIndex < lastMessageIndex
                }
            }
    }
    LaunchedEffect(state.currentSession.id, state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.scrollToItem(state.chatMessages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeScreenTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                onProfileClick = onProfileClicked,
                deviceSnapshot = state.deviceSnapshot,
                onHistoryClick = onToggleHistoryPanel
            )
        },
        bottomBar = {
            HomeInputArea(
                quickSkills = state.quickSkills,
                selectedSkill = state.selectedSkill,
                enabled = !state.isSending && !state.isStreaming,
                inputValue = state.inputText,
                onInputChanged = onInputChanged,
                onSendClicked = onSendClicked,
                onQuickSkillSelected = onQuickSkillSelected
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                SessionHeader(
                    session = state.currentSession,
                    onNewChatClicked = onNewChatClicked
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val hasActiveChat = state.chatMessages.isNotEmpty()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(HomeScreenTestTags.LIST),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                        userScrollEnabled = true
                    ) {
                        if (!hasActiveChat) {
                            item("welcome") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "你好，我是您的销售助手",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "我可以帮您总结对话、分析异议、辅导话术、生成日报。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            item("session-list") {
                                SessionListSection(
                                    sessions = state.sessionList,
                                    isLoading = state.isLoadingHistory,
                                    onSessionSelected = onSessionSelected
                                )
                            }
                            if (state.isLoadingHistory) {
                                item("history-loading") {
                                    Text(
                                        text = "加载历史记录...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                            item("empty") {
                                if (state.sessionList.isEmpty()) {
                                    EmptySessionHint(onNewChatClicked = onNewChatClicked)
                                } else {
                                    EmptyChatHint(
                                        quickSkills = state.quickSkills,
                                        selectedSkillId = state.selectedSkill?.id,
                                        enabled = !state.isSending && !state.isStreaming,
                                        onQuickSkillSelected = onQuickSkillSelected
                                    )
                                }
                            }
                        } else {
                            if (state.isLoadingHistory) {
                                item("history-loading") {
                                    Text(
                                        text = "加载历史记录...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                            itemsIndexed(state.chatMessages, key = { _, item -> item.id }) { index, message ->
                                val isTranscriptSummary = message.role == ChatMessageRole.ASSISTANT &&
                                    message.content.contains("通话分析")
                                val tagModifier = if (message.role == ChatMessageRole.ASSISTANT &&
                                    (index == state.chatMessages.lastIndex || isTranscriptSummary)
                                ) {
                                    Modifier.testTag(HomeScreenTestTags.ASSISTANT_MESSAGE)
                                } else {
                                    Modifier
                                }
                                MessageBubble(
                                    message = message,
                                    alignEnd = message.role == ChatMessageRole.USER,
                                    modifier = tagModifier,
                                    onCopyAssistant = { content ->
                                        clipboardManager.setText(AnnotatedString(content))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("已复制到剪贴板")
                                        }
                                    }
                                )
                            }
                            item("chat-bottom-pad") {
                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = refreshingState.value,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            chatErrorMessage?.let { message ->
                SnackbarHost(
                    hostState = remember { SnackbarHostState() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (showScrollToLatest.value) {
                ScrollToLatestButton(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    onClick = {
                        val lastIndex = state.chatMessages.lastIndex
                        if (lastIndex >= 0) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(lastIndex)
                            }
                        }
                    }
                )
            }
            if (showHistoryPanel) {
                HistoryPanel(
                    sessions = historySessions,
                    currentSessionId = state.currentSession.id,
                    onDismiss = onDismissHistoryPanel,
                    onSessionSelected = onHistorySessionSelected
                )
            }
        }
    }
}

object HomeScreenTestTags {
    const val ROOT = "home_screen_root"
    // 兼容旧测试
    const val PAGE = ROOT
    const val DEVICE_ENTRY = "home_device_entry"
    const val AUDIO_ENTRY = "home_audio_entry"
    const val SESSION_HEADER = "home_session_header"
    const val LIST = "home_messages_list"
    const val DEVICE_BANNER = DEVICE_ENTRY
    const val AUDIO_CARD = AUDIO_ENTRY
    const val PROFILE_BUTTON = "home_profile_button"
    const val ACTIVE_SKILL_CHIP = "active_skill_chip"
    const val ACTIVE_SKILL_CHIP_CLOSE = "active_skill_chip_close"
    const val SESSION_LIST = "home_session_list"
    const val SESSION_LOADING = "home_session_loading"
    const val SESSION_EMPTY = "home_session_empty"
    const val SESSION_LIST_ITEM_PREFIX = "home_session_item_"
    const val SESSION_CURRENT_PREFIX = "home_session_current_"
    const val NEW_CHAT_BUTTON = "home_new_chat_button"
    const val SESSION_TITLE = "home_session_title"
    const val USER_MESSAGE = "home_user_message"
    const val ASSISTANT_MESSAGE = "home_assistant_message"
    const val ASSISTANT_COPY_PREFIX = "home_assistant_copy_"
    const val INPUT_FIELD = "home_input_field"
    const val SEND_BUTTON = "home_send_button"
    const val SCROLL_TO_LATEST = "home_scroll_to_latest_button"
    const val HISTORY_TOGGLE = "home_history_toggle"
    const val HISTORY_PANEL = "home_history_panel"
    const val HISTORY_EMPTY = "home_history_empty"
    const val HISTORY_ITEM_PREFIX = "home_history_item_"
}

@Composable
private fun HomeTopBar(
    onProfileClick: () -> Unit,
    deviceSnapshot: DeviceSnapshotUi?,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AI 助手",
            style = MaterialTheme.typography.titleLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (deviceSnapshot?.connectionState == DeviceConnectionStateUi.CONNECTED) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = "设备已连接") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.testTag(HomeScreenTestTags.HISTORY_TOGGLE)
            ) {
                Icon(Icons.Filled.History, contentDescription = "历史记录")
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier.testTag(HomeScreenTestTags.PROFILE_BUTTON)
            ) {
                Icon(Icons.Filled.Person, contentDescription = "个人中心")
            }
        }
    }
}

@Composable
private fun EntryCards(
    deviceSnapshot: DeviceSnapshotUi?,
    audioSummary: AudioSummaryUi?,
    onDeviceClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EntryCard(
            title = "设备管理",
            subtitle = deviceSnapshot?.statusText ?: "查看设备文件与连接状态",
            supporting = deviceSnapshot?.deviceName ?: "点击进入设备管理",
            onClick = onDeviceClick,
            testTag = HomeScreenTestTags.DEVICE_ENTRY
        )
        EntryCard(
            title = "音频库",
            subtitle = audioSummary?.headline ?: "同步录音并查看转写",
            supporting = audioSummary?.detail ?: "上传或查看最近转写内容",
            onClick = onAudioClick,
            testTag = HomeScreenTestTags.AUDIO_ENTRY
        )
    }
}

@Composable
private fun EntryCard(
    title: String,
    subtitle: String,
    supporting: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SessionListSection(
    sessions: List<SessionListItemUi>,
    isLoading: Boolean,
    onSessionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HomeScreenTestTags.SESSION_LIST),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "历史会话",
            style = MaterialTheme.typography.titleSmall
        )
        if (isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(modifier = Modifier.weight(1f))
                Text(
                    text = "正在加载历史会话...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(HomeScreenTestTags.SESSION_LOADING)
                )
            }
        }
        if (sessions.isEmpty()) {
            Text(
                text = "暂无历史会话，先发一条消息试试吧",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(HomeScreenTestTags.SESSION_EMPTY)
            )
            return
        }
        sessions.forEach { session ->
            SessionListItem(
                session = session,
                onClick = { onSessionSelected(session.id) }
            )
        }
    }
}

@Composable
private fun SessionListItem(
    session: SessionListItemUi,
    onClick: () -> Unit
) {
    val background = if (session.isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${HomeScreenTestTags.SESSION_LIST_ITEM_PREFIX}${session.id}")
            .clickable(onClick = onClick),
        color = background,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.isTranscription) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(text = "通话分析") },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                if (session.isCurrent) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(text = "当前") },
                        modifier = Modifier.testTag("${HomeScreenTestTags.SESSION_CURRENT_PREFIX}${session.id}"),
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            if (session.lastMessagePreview.isNotBlank()) {
                Text(
                    text = session.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatSessionTime(session.updatedAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageUi,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
    onCopyAssistant: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .then(
                if (alignEnd) Modifier.testTag(HomeScreenTestTags.USER_MESSAGE) else modifier
            ),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (alignEnd) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!alignEnd) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { onCopyAssistant(message.content) },
                            modifier = Modifier.testTag("${HomeScreenTestTags.ASSISTANT_COPY_PREFIX}${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "复制助手消息",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Text(text = message.content)
                if (message.isStreaming) {
                    Text(
                        text = "AI 回复中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
}

private fun formatSessionTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun EmptyChatHint(
    quickSkills: List<QuickSkillUi>,
    selectedSkillId: QuickSkillId?,
    enabled: Boolean,
    onQuickSkillSelected: (QuickSkillId) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "还没有对话，试试下面的快捷技能开始吧！")
        QuickSkillRow(
            skills = quickSkills,
            selectedSkillId = selectedSkillId,
            enabled = enabled,
            onQuickSkillSelected = onQuickSkillSelected
        )
    }
}

@Composable
private fun EmptySessionHint(onNewChatClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "还没有对话，点击「新建对话」开始聊天")
        TextButton(onClick = onNewChatClicked) {
            Text(text = "新建对话")
        }
    }
}

@Composable
private fun HomeInputArea(
    quickSkills: List<QuickSkillUi>,
    selectedSkill: QuickSkillUi?,
    enabled: Boolean,
    inputValue: String,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit
) {
    Surface(
        tonalElevation = 4.dp
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickSkillRow(
            skills = quickSkills,
            selectedSkillId = selectedSkill?.id,
            enabled = enabled,
            onQuickSkillSelected = onQuickSkillSelected
        )
        OutlinedTextField(
            value = inputValue,
            onValueChange = onInputChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HomeScreenTestTags.INPUT_FIELD),
                label = { Text(text = "上传文件或输入消息...") },
                enabled = enabled
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSendClicked,
                    enabled = inputValue.isNotBlank() && enabled,
                    modifier = Modifier.testTag(HomeScreenTestTags.SEND_BUTTON)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "发送")
                }
            }
        }
    }
}

@Composable
private fun SessionHeader(
    session: CurrentSessionUi,
    onNewChatClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag(HomeScreenTestTags.SESSION_HEADER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
                Text(
                    text = session.title,
                    modifier = Modifier.testTag(HomeScreenTestTags.SESSION_TITLE),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            if (session.isTranscription) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = "通话分析") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
        TextButton(
            onClick = onNewChatClicked,
            modifier = Modifier.testTag(HomeScreenTestTags.NEW_CHAT_BUTTON)
        ) {
            Text(text = "新建对话")
        }
    }
}

@Composable
private fun QuickSkillRow(
    skills: List<QuickSkillUi>,
    selectedSkillId: QuickSkillId?,
    enabled: Boolean,
    onQuickSkillSelected: (QuickSkillId) -> Unit
) {
    if (skills.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(skills, key = { it.id }) { skill ->
            val skillTag = "home_quick_skill_${skill.id}"
            val isSelected = skill.id == selectedSkillId
            val colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                labelColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (skill.isRecommended) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Box {
                AssistChip(
                    onClick = { onQuickSkillSelected(skill.id) },
                    enabled = enabled,
                    modifier = Modifier.testTag(skillTag),
                    label = {
                        Text(
                            text = skill.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    // 快捷技能视为模式选择，仅高亮选中态，不向聊天插入消息
                    colors = colors
                )
                if (isSelected) {
                    // 测试用：标记当前选中的快捷技能，便于断言选中态
                    Box(modifier = Modifier.fillMaxSize().testTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP))
                }
            }
        }
    }
}

@Composable
private fun ScrollToLatestButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.testTag(HomeScreenTestTags.SCROLL_TO_LATEST),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 6.dp
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "回到底部",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun HistoryPanel(
    sessions: List<SessionListItemUi>,
    currentSessionId: String,
    onDismiss: () -> Unit,
    onSessionSelected: (String) -> Unit
) {
    // 侧边抽屉：覆盖在右侧，点击遮罩即可关闭
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(320.dp)
                .fillMaxHeight()
                .testTag(HomeScreenTestTags.HISTORY_PANEL),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "会话抽屉", style = MaterialTheme.typography.titleMedium)
                if (sessions.isEmpty()) {
                    Text(
                        text = "暂无历史会话，先开始一次对话吧。",
                        modifier = Modifier.testTag(HomeScreenTestTags.HISTORY_EMPTY),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sessions.forEach { session ->
                        val isCurrent = session.id == currentSessionId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSessionSelected(session.id)
                                }
                                .testTag("${HomeScreenTestTags.HISTORY_ITEM_PREFIX}${session.id}"),
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            tonalElevation = if (isCurrent) 4.dp else 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (session.lastMessagePreview.isNotBlank()) {
                                    Text(
                                        text = session.lastMessagePreview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatSessionTime(session.updatedAtMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
