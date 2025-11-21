package com.smartsales.feature.chat.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartsales.feature.chat.core.QuickSkillId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，渲染设备/音频提示、聊天记录、快捷技能与输入框
// 作者：创建于 2025-11-20

@Composable
fun HomeScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel(),
    onNavigateToDeviceManager: () -> Unit = {},
    onNavigateToDeviceSetup: () -> Unit = {},
    onNavigateToAudioFiles: () -> Unit = {},
    onNavigateToUserCenter: () -> Unit = {}
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
            else -> Unit
        }
        if (state.navigationRequest != null) {
            viewModel.onNavigationConsumed()
        }
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
            .testTag(HomeScreenTestTags.PAGE),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { HomeTopBar(onProfileClick = onProfileClicked) },
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
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    DeviceAudioBanner(
                        deviceSnapshot = state.deviceSnapshot,
                        audioSummary = state.audioSummary,
                        onDeviceClick = onDeviceBannerClicked,
                        onAudioClick = onAudioSummaryClicked
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
                    ) {
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
                                EmptyChatHint(
                                    quickSkills = state.quickSkills,
                                    enabled = !state.isSending && !state.isStreaming,
                                    onQuickSkillSelected = onQuickSkillSelected
                                )
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
        }
    }
}

object HomeScreenTestTags {
    const val PAGE = "home_screen_page"
    const val DEVICE_BANNER = "home_device_banner"
    const val AUDIO_CARD = "home_audio_card"
    const val PROFILE_BUTTON = "home_profile_button"
    const val ACTIVE_SKILL_CHIP = "active_skill_chip"
    const val ACTIVE_SKILL_CHIP_CLOSE = "active_skill_chip_close"
    const val USER_MESSAGE = "home_user_message"
    const val ASSISTANT_MESSAGE = "home_assistant_message"
    const val INPUT_FIELD = "home_input_field"
    const val SEND_BUTTON = "home_send_button"
    const val SCROLL_TO_LATEST = "home_scroll_to_latest_button"
}

@Composable
private fun HomeTopBar(onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AI 助手",
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier.testTag(HomeScreenTestTags.PROFILE_BUTTON)
        ) {
            Icon(Icons.Filled.Person, contentDescription = "个人中心")
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
    // Home 只读取连接/媒体状态，不控制底层逻辑
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    text = deviceSnapshot?.statusText ?: "等待连接设备",
                    style = MaterialTheme.typography.titleMedium
                )
                deviceSnapshot?.deviceName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                deviceSnapshot?.wifiName?.let {
                    Text(
                        text = "Wi-Fi：$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                deviceSnapshot?.errorSummary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Divider()
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
                    Text(text = "查看音频")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageUi,
    alignEnd: Boolean
) {
    val bubbleTag = if (alignEnd) {
        HomeScreenTestTags.USER_MESSAGE
    } else {
        HomeScreenTestTags.ASSISTANT_MESSAGE
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            }
        }
    }
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
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "还没有对话，试试下面的快捷技能开始吧！")
        QuickSkillRow(
            skills = quickSkills,
            enabled = enabled,
            onQuickSkillSelected = onQuickSkillSelected
        )
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
