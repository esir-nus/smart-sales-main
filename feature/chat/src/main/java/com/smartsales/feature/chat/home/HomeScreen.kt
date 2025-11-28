package com.smartsales.feature.chat.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.TranscriptionChatRequest
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
    onDeviceSnapshotChanged: (DeviceSnapshotUi?) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            viewModel.setSession(it)
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
        onClearSelectedSkill = viewModel::clearSelectedSkill,
        onDeviceBannerClicked = viewModel::onTapDeviceBanner,
        onAudioSummaryClicked = viewModel::onTapAudioSummary,
        onRefreshDeviceAndAudio = viewModel::onRefreshDeviceAndAudio,
        onLoadMoreHistory = viewModel::onLoadMoreHistory,
        onProfileClicked = viewModel::onTapProfile,
        onOpenHistory = viewModel::onOpenDrawer,
        onNewChatClicked = viewModel::onNewChatClicked,
        onSessionSelected = viewModel::setSession,
        chatErrorMessage = state.chatErrorMessage,
        modifier = modifier
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
    onClearSelectedSkill: () -> Unit,
    onDeviceBannerClicked: () -> Unit,
    onAudioSummaryClicked: () -> Unit,
    onRefreshDeviceAndAudio: () -> Unit,
    onLoadMoreHistory: () -> Unit,
    onProfileClicked: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewChatClicked: () -> Unit = {},
    onSessionSelected: (String) -> Unit = {},
    chatErrorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val refreshingState = remember { mutableStateOf(false) }
    LaunchedEffect(state.deviceSnapshot, state.audioSummary) {
        refreshingState.value = false
    }
    val coroutineScope = rememberCoroutineScope()
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(HomeScreenTestTags.ROOT),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeTopBar(
                onHistoryClick = onOpenHistory,
                onProfileClick = onProfileClicked
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
                onQuickSkillSelected = onQuickSkillSelected,
                onClearSelectedSkill = onClearSelectedSkill
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    DeviceAudioBanner(
                        deviceSnapshot = state.deviceSnapshot,
                        audioSummary = state.audioSummary,
                        onDeviceClick = onDeviceBannerClicked,
                        onAudioClick = onAudioSummaryClicked
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)
                    ) {
                        item("session-list") {
                            SessionListSection(
                                sessions = state.sessionList,
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
                        if (state.chatMessages.isEmpty()) {
                            item("empty") {
                                if (state.sessionList.isEmpty()) {
                                    EmptySessionHint(onNewChatClicked = onNewChatClicked)
                                } else {
                                    EmptyChatHint(
                                        quickSkills = state.quickSkills,
                                        enabled = !state.isSending && !state.isStreaming,
                                        onQuickSkillSelected = onQuickSkillSelected
                                    )
                                }
                            }
                        } else {
                            items(state.chatMessages, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    alignEnd = message.role == ChatMessageRole.USER
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
            AnimatedVisibility(
                visible = showScrollToLatest.value,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                ScrollToLatestButton(
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
        }
    }
}

object HomeScreenTestTags {
    const val ROOT = "home_screen_root"
    // 兼容旧测试
    const val PAGE = ROOT
    const val SESSION_HEADER = "home_session_header"
    const val LIST = "home_messages_list"
    const val DEVICE_BANNER = "home_device_banner"
    const val AUDIO_CARD = "home_audio_card"
    const val PROFILE_BUTTON = "home_profile_button"
    const val HISTORY_BUTTON = "home_history_button"
    const val ACTIVE_SKILL_CHIP = "active_skill_chip"
    const val ACTIVE_SKILL_CHIP_CLOSE = "active_skill_chip_close"
    const val SESSION_LIST = "home_session_list"
    const val SESSION_LIST_ITEM_PREFIX = "home_session_item_"
    const val NEW_CHAT_BUTTON = "home_new_chat_button"
    const val USER_MESSAGE = "home_user_message"
    const val ASSISTANT_MESSAGE = "home_assistant_message"
    const val INPUT_FIELD = "home_input_field"
    const val SEND_BUTTON = "home_send_button"
    const val SCROLL_TO_LATEST = "home_scroll_to_latest_button"
}

@Composable
private fun HomeTopBar(
    onHistoryClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "新对话",
            style = MaterialTheme.typography.titleLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onHistoryClick,
                modifier = Modifier.testTag(HomeScreenTestTags.HISTORY_BUTTON)
            ) {
                Icon(Icons.Filled.History, contentDescription = "历史会话")
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
private fun DeviceAudioBanner(
    deviceSnapshot: DeviceSnapshotUi?,
    audioSummary: AudioSummaryUi?,
    onDeviceClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    val snapshot = deviceSnapshot ?: DeviceSnapshotUi(
        statusText = "设备未连接，点击开始配网",
        connectionState = DeviceConnectionStateUi.DISCONNECTED
    )
    val supportingText = when (snapshot.connectionState) {
        DeviceConnectionStateUi.DISCONNECTED -> "请连接设备以管理文件和查看预览"
        DeviceConnectionStateUi.CONNECTING -> "正在连接设备，请保持靠近"
        DeviceConnectionStateUi.WAITING_FOR_NETWORK -> "设备已连接，等待联网完成"
        DeviceConnectionStateUi.CONNECTED -> "设备在线，可管理文件与录音"
        DeviceConnectionStateUi.ERROR -> snapshot.errorSummary
    }
    // Home 只读取连接/媒体状态，不控制底层逻辑
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HomeScreenTestTags.DEVICE_BANNER)
                    .clickable(onClick = onDeviceClick)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = snapshot.statusText,
                    style = MaterialTheme.typography.titleMedium
                )
                snapshot.deviceName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                snapshot.wifiName?.let {
                    Text(
                        text = "Wi-Fi：$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (snapshot.connectionState == DeviceConnectionStateUi.ERROR) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HomeScreenTestTags.AUDIO_CARD)
                    .clickable(onClick = onAudioClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = audioSummary?.headline ?: "暂无录音摘要",
                        style = MaterialTheme.typography.titleSmall
                    )
                    audioSummary?.detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onAudioClick, enabled = audioSummary != null) {
                    Text(text = "进入录音文件")
                }
            }
        }
    }
}

@Composable
private fun SessionListSection(
    sessions: List<SessionListItemUi>,
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
        if (sessions.isEmpty()) {
            Text(
                text = "暂无历史会话",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    alignEnd: Boolean
) {
    val clipboard = LocalClipboardManager.current
    val bubbleTag = if (alignEnd) {
        HomeScreenTestTags.USER_MESSAGE
    } else {
        HomeScreenTestTags.ASSISTANT_MESSAGE
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .testTag(bubbleTag),
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
                if (!alignEnd) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { clipboard.setText(AnnotatedString(message.content)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "复制",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
    enabled: Boolean,
    onQuickSkillSelected: (QuickSkillId) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LOGO",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "你好，我是您的销售助手",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "我可以帮您：",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "• 分析用户画像、意图、痛点。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• 生成 PDF、CSV 文档及思维导图。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "让我们开始吧",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        QuickSkillRow(
            skills = quickSkills,
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
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onClearSelectedSkill: () -> Unit
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
                enabled = enabled,
                onQuickSkillSelected = onQuickSkillSelected
            )
            selectedSkill?.let {
                ActiveSkillChip(
                    label = it.label,
                    onClear = onClearSelectedSkill,
                    modifier = Modifier.testTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP)
                )
            }
            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HomeScreenTestTags.INPUT_FIELD),
                label = { Text(text = "输入问题") },
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
private fun ActiveSkillChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Filled.Lightbulb,
    onClear: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.testTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP_CLOSE)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "清除快捷技能"
                )
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
    enabled: Boolean,
    onQuickSkillSelected: (QuickSkillId) -> Unit
) {
    if (skills.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(skills, key = { it.id }) { skill ->
            val skillTag = "home_quick_skill_${skill.id}"
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
