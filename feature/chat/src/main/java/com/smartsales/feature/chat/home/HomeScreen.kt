package com.smartsales.feature.chat.home

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.smartsales.feature.chat.home.CHAT_DEBUG_HUD_ENABLED
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，仅渲染聊天欢迎区、消息列表、快捷技能与输入框
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
    onInputFocusChanged: (Boolean) -> Unit = {},
) {
    Log.i("HomeScreenRoute", "HomeScreenRoute composed - entering function")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // 抽屉开关保留在 Route 层，避免影响 ViewModel 状态
    var showHistoryPanel by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

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
        if (state.navigationRequest != null) {
            dismissKeyboard()
        }
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
                viewModel.setSession(it, allowHero = false)
            }
            onSessionSelectionConsumed()
        }
    }
    LaunchedEffect(sessionId) {
        sessionId?.let { viewModel.setSession(it, allowHero = false) }
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
        onExportPdfClicked = viewModel::onExportPdfClicked,
        onExportCsvClicked = viewModel::onExportCsvClicked,
        exportInProgress = state.exportInProgress,
        onLoadMoreHistory = viewModel::onLoadMoreHistory,
        onProfileClicked = viewModel::onTapProfile,
    onNewChatClicked = viewModel::onNewChatClicked,
    onSessionSelected = viewModel::setSession,
    chatErrorMessage = state.chatErrorMessage,
        onPickAudioFile = viewModel::onAudioFilePicked,
        onPickImageFile = viewModel::onImagePicked,
        onAudioClicked = {
            dismissKeyboard()
            onNavigateToAudioFiles()
        },
        onDeviceClicked = {
            dismissKeyboard()
            onNavigateToDeviceManager()
        },
        modifier = modifier,
    showHistoryPanel = showHistoryPanel,
        onToggleHistoryPanel = { showHistoryPanel = true },
        onDismissHistoryPanel = { showHistoryPanel = false },
        historySessions = state.sessionList.take(10),
        onHistorySessionSelected = { sessionId ->
            viewModel.setSession(sessionId, allowHero = false)
            showHistoryPanel = false
        },
        onToggleDebugMetadata = viewModel::toggleDebugMetadata,
        onDismissKeyboard = dismissKeyboard,
        onInputFocusChanged = { focused ->
            viewModel.onInputFocusChanged(focused)
            onInputFocusChanged(focused)
        }
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit,
    onDeviceBannerClicked: () -> Unit,
    onAudioSummaryClicked: () -> Unit,
    onRefreshDeviceAndAudio: () -> Unit,
    onLoadMoreHistory: () -> Unit,
    onProfileClicked: () -> Unit,
    onNewChatClicked: () -> Unit = {},
    onSessionSelected: (String) -> Unit = {},
    chatErrorMessage: String? = null,
    exportInProgress: Boolean,
    onPickAudioFile: (Uri) -> Unit = {},
    onPickImageFile: (Uri) -> Unit = {},
    onAudioClicked: () -> Unit = {},
    onDeviceClicked: () -> Unit = {},
    modifier: Modifier = Modifier,
    showHistoryPanel: Boolean = false,
    onToggleHistoryPanel: () -> Unit = {},
    onDismissHistoryPanel: () -> Unit = {},
    historySessions: List<SessionListItemUi> = emptyList(),
    onHistorySessionSelected: (String) -> Unit = {},
    onToggleDebugMetadata: () -> Unit = {},
    onDismissKeyboard: () -> Unit = {},
    onInputFocusChanged: (Boolean) -> Unit = {}
) {
    val drawerState = rememberDrawerState(
        initialValue = if (showHistoryPanel) DrawerValue.Open else DrawerValue.Closed
    )
    LaunchedEffect(showHistoryPanel) {
        if (showHistoryPanel) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue }
            .collectLatest { value ->
                when (value) {
                    DrawerValue.Open -> if (!showHistoryPanel) onToggleHistoryPanel()
                    DrawerValue.Closed -> if (showHistoryPanel) onDismissHistoryPanel()
                }
            }
    }
    BackHandler(enabled = showHistoryPanel) {
        onDismissHistoryPanel()
    }

    Log.i("HomeScreen", "HomeScreen composed - entering function")
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onPickAudioFile(it) }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onPickImageFile(it) }
    }

    val listState = rememberLazyListState()
    val hudEnabled = CHAT_DEBUG_HUD_ENABLED
    val showDebugPanel = hudEnabled && state.showDebugMetadata
    val emptySkillOrder = listOf(
        QuickSkillId.SMART_ANALYSIS,
        QuickSkillId.EXPORT_PDF,
        QuickSkillId.EXPORT_CSV
    )
    val allowedSkills = state.quickSkills
        .filter { it.id in emptySkillOrder }
        .sortedBy { emptySkillOrder.indexOf(it.id) }
        .map { quick ->
            val customLabel = when (quick.id) {
                QuickSkillId.SMART_ANALYSIS -> "智能分析"
                QuickSkillId.EXPORT_PDF -> "生成 PDF"
                QuickSkillId.EXPORT_CSV -> "生成 CSV"
                else -> quick.label
            }
            quick.copy(label = customLabel)
        }
    val chatBusy = state.isSending || state.isStreaming
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

    val dragDismissModifier = Modifier.pointerInput(state.isInputFocused) {
        var typingAtStart = false
        var dismissedInGesture = false
        detectVerticalDragGestures(
            onDragStart = {
                typingAtStart = state.isInputFocused
                dismissedInGesture = false
            },
            onVerticalDrag = { _, dragAmount ->
                if (typingAtStart && !dismissedInGesture && dragAmount > 0f) {
                    onDismissKeyboard()
                    dismissedInGesture = true
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !state.isInputFocused,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight()
                    .testTag(HomeScreenTestTags.HISTORY_PANEL),
                drawerShape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                HistoryDrawerContent(
                    sessions = historySessions,
                    currentSessionId = state.currentSession.id,
                    onSessionSelected = onHistorySessionSelected,
                    onClose = onDismissHistoryPanel
                )
            }
        },
        content = {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .testTag(HomeScreenTestTags.PAGE_HOME)
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(HomeScreenTestTags.ROOT),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                        HomeTopBar(
                            title = state.currentSession.title,
                            onProfileClick = onProfileClicked,
                            deviceSnapshot = state.deviceSnapshot,
                            onHistoryClick = {
                                onDismissKeyboard()
                                onToggleHistoryPanel()
                            },
                            onAudioClick = {
                                onDismissKeyboard()
                            onAudioClicked()
                        },
                    onDeviceClick = {
                        onDismissKeyboard()
                        onDeviceClicked()
                    },
                    onNewChatClick = onNewChatClicked,
                    hudEnabled = hudEnabled,
                    showDebugMetadata = state.showDebugMetadata,
                    onToggleDebugMetadata = { onToggleDebugMetadata() }
                )
                    },
                    bottomBar = {
                        val inputEnabled = !chatBusy && !state.isInputBusy
                        HomeInputArea(
                            quickSkills = allowedSkills,
                            selectedSkill = state.selectedSkill,
                            showQuickSkills = !state.showWelcomeHero,
                            enabled = inputEnabled,
                            busy = chatBusy,
                            inputValue = state.inputText,
                            isSmartAnalysisMode = state.isSmartAnalysisMode,
                            onInputChanged = onInputChanged,
                            onSendClicked = {
                                onSendClicked()
                                onDismissKeyboard()
                            },
                            onQuickSkillSelected = onQuickSkillSelected,
                            onPickAudio = { audioPicker.launch(arrayOf("audio/*")) },
                            onPickImage = { imagePicker.launch(arrayOf("image/*")) },
                            onInputFocusChanged = onInputFocusChanged
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .pointerInput(state.isInputFocused) {
                                if (!state.isInputFocused) return@pointerInput
                                detectTapGestures(onTap = { onDismissKeyboard() })
                            }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .then(dragDismissModifier)
                            ) {
                                AnimatedContent(
                                    targetState = state.chatMessages.isEmpty() && state.showWelcomeHero,
                                    transitionSpec = {
                                        (fadeIn() + slideInVertically { it / 8 }) with
                                            (fadeOut() + slideOutVertically { -it / 8 })
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    label = "feature_home_empty_transition"
                                ) { empty ->
                                    if (empty) {
                                        EmptyStateContent(
                                            userName = state.userName,
                                            skills = allowedSkills,
                                            selectedSkillId = state.selectedSkill?.id,
                                            enabled = !chatBusy && !state.isInputBusy,
                                            onSkillSelected = onQuickSkillSelected
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
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
                                                    item("welcome_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
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
                                                }
                                                if (state.chatMessages.lastOrNull()?.isStreaming == true) {
                                                    item("typing-indicator") {
                                                        AssistantTypingBubble()
                                                    }
                                                }
                                                item("chat-bottom-pad") {
                                                    Spacer(modifier = Modifier.height(72.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                        if (hudEnabled && showDebugPanel && state.debugSessionMetadata != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        onToggleDebugMetadata()
                                    }
                                    .testTag(HomeScreenTestTags.DEBUG_HUD_SCRIM)
                            )
                            DebugSessionMetadataHud(
                                metadata = state.debugSessionMetadata,
                                onClose = {
                                    onToggleDebugMetadata()
                                },
                                onCopy = { text ->
                                    runCatching {
                                        clipboardManager.setText(AnnotatedString(text))
                                    }.onSuccess {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("已复制调试信息")
                                        }
                                    }.onFailure {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("复制失败，请稍后重试")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyStateContent(
    userName: String,
    skills: List<QuickSkillUi>,
    selectedSkillId: QuickSkillId?,
    enabled: Boolean,
    onSkillSelected: (QuickSkillId) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag(HomeScreenTestTags.HERO)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "LOGO",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "你好，$userName",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "我是您的销售助手",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "我可以帮您：",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            QuickSkillRow(
                skills = skills,
                selectedSkillId = selectedSkillId,
                enabled = enabled,
                onQuickSkillSelected = onSkillSelected
            )
            Divider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HomeHeroSection(
    userName: String,
    hasMessages: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeScreenTestTags.HERO),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "你好，$userName",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "我是您的销售助手",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BulletLine("总结对话，分析异议，辅导话术")
                BulletLine("生成日报、PDF/CSV 报告，帮助复盘")
                BulletLine("导出思维导图，辅助会议决策")
            }
            if (!hasMessages) {
                Text(
                    text = "让我们开始吧",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(4.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
object HomeScreenTestTags {
    const val ROOT = "home_screen_root"
    // 兼容旧测试
    const val PAGE = ROOT
    const val PAGE_HOME = "page_home"
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
    const val EXPORT_PDF = "home_export_pdf"
    const val EXPORT_CSV = "home_export_csv"
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
    const val HERO = "home_hero"
    const val DEBUG_HUD_PANEL = "debug_hud_panel"
    const val DEBUG_HUD_TOGGLE = "debug_toggle_metadata"
    const val DEBUG_HUD_SCRIM = "debug_hud_scrim"
    const val DEBUG_HUD_CLOSE = "debug_hud_close"
    const val DEBUG_HUD_COPY = "debug_hud_copy"
    const val AUDIO_TOGGLE = "home_audio_toggle"
    const val DEVICE_TOGGLE = "home_device_toggle"
}

@Composable
private fun HomeTopBar(
    title: String,
    onProfileClick: () -> Unit,
    deviceSnapshot: DeviceSnapshotUi?,
    onHistoryClick: () -> Unit,
    onAudioClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onNewChatClick: () -> Unit,
    hudEnabled: Boolean,
    showDebugMetadata: Boolean,
    onToggleDebugMetadata: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onHistoryClick,
            modifier = Modifier.testTag(HomeScreenTestTags.HISTORY_TOGGLE)
        ) {
            Icon(Icons.Filled.History, contentDescription = "历史记录")
        }
        IconButton(
            onClick = onAudioClick,
            modifier = Modifier.testTag(HomeScreenTestTags.AUDIO_TOGGLE)
        ) {
            Icon(imageVector = Icons.Filled.AudioFile, contentDescription = "音频库")
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .testTag(HomeScreenTestTags.SESSION_TITLE),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(
            onClick = onDeviceClick,
            modifier = Modifier.testTag(HomeScreenTestTags.DEVICE_TOGGLE)
        ) {
            val icon = when (deviceSnapshot?.connectionState) {
                DeviceConnectionStateUi.CONNECTED -> Icons.Filled.DeviceHub
                DeviceConnectionStateUi.CONNECTING, DeviceConnectionStateUi.WAITING_FOR_NETWORK -> Icons.Filled.BluetoothConnected
                else -> Icons.Filled.DeviceHub
            }
            Icon(imageVector = icon, contentDescription = "设备管理")
        }
        if (hudEnabled) {
            IconButton(
                onClick = onToggleDebugMetadata,
                modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_TOGGLE)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            color = if (showDebugMetadata) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            shape = CircleShape
                        )
                )
            }
        }
        IconButton(
            onClick = onNewChatClick,
            modifier = Modifier.testTag(HomeScreenTestTags.NEW_CHAT_BUTTON)
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = "新建对话")
        }
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier.testTag(HomeScreenTestTags.PROFILE_BUTTON)
        ) {
            Icon(Icons.Filled.Person, contentDescription = "个人中心")
        }
    }
}

@Composable
private fun DebugSessionMetadataHud(
    metadata: DebugSessionMetadata,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHalfHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
    val scrollState = rememberScrollState()
    val hudText = remember(metadata) {
        buildString {
            appendLine("sessionId: ${metadata.sessionId}")
            appendLine("title: ${metadata.title}")
            appendLine("mainPerson: ${metadata.mainPerson ?: "-"}")
            appendLine("shortSummary: ${metadata.shortSummary ?: "-"}")
            appendLine("title6: ${metadata.summaryTitle6Chars ?: "-"}")
            if (metadata.notes.isNotEmpty()) {
                appendLine("notes:")
                metadata.notes.forEach { appendLine("- $it") }
            }
        }.trimEnd()
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = screenHalfHeight)
            .testTag(HomeScreenTestTags.DEBUG_HUD_PANEL),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "调试信息",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { onCopy(hudText) },
                        modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_COPY)
                    ) {
                        Text(text = "复制")
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_CLOSE)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "关闭 HUD")
                    }
                }
            }
            DebugField(label = "sessionId", value = metadata.sessionId)
            DebugField(label = "title", value = metadata.title)
            DebugField(label = "mainPerson", value = metadata.mainPerson ?: "-")
            DebugField(label = "shortSummary", value = metadata.shortSummary ?: "-")
            DebugField(label = "title6", value = metadata.summaryTitle6Chars ?: "-")
            if (metadata.notes.isNotEmpty()) {
                Text(
                    text = "notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                metadata.notes.forEach { note ->
                    Text(
                        text = "- $note",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SessionListSection(
    sessions: List<SessionListItemUi>,
    isLoading: Boolean,
    onSessionSelected: (String) -> Unit
) {
    // React 对齐后历史列表入口移至独立页面，这里保留空实现以维护测试 tag 兼容
    if (isLoading) {
        LinearProgressIndicator(modifier = Modifier.testTag(HomeScreenTestTags.SESSION_LOADING))
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .then(if (alignEnd) Modifier.testTag(HomeScreenTestTags.USER_MESSAGE) else modifier),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val shape = RoundedCornerShape(14.dp)
        Surface(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = shape,
            tonalElevation = if (alignEnd) 2.dp else 3.dp,
            shadowElevation = 0.dp,
            color = if (alignEnd) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!alignEnd) {
                    var hasCopied by remember { mutableStateOf(false) }
                    LaunchedEffect(hasCopied) {
                        if (hasCopied) {
                            delay(2_000)
                            hasCopied = false
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onCopyAssistant(message.content)
                                hasCopied = true
                            },
                            modifier = Modifier.testTag("${HomeScreenTestTags.ASSISTANT_COPY_PREFIX}${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasCopied) "已复制" else "复制",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (message.hasError) {
                    Spacer(modifier = Modifier.height(6.dp))
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
private fun AssistantTypingBubble() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "AI 回复中...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSessionTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
private fun EmptyChatHint(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "你好，$userName", style = MaterialTheme.typography.titleMedium)
        Text(text = "我是您的销售助手", style = MaterialTheme.typography.bodyMedium)
        Text(text = "还没有对话，试着发送第一条消息吧。")
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
    showQuickSkills: Boolean,
    enabled: Boolean,
    busy: Boolean,
    inputValue: String,
    isSmartAnalysisMode: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onPickAudio: () -> Unit,
    onPickImage: () -> Unit,
    onInputFocusChanged: (Boolean) -> Unit
) {
    var uploadMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val canSend = enabled && !busy && (
        inputValue.isNotBlank() || selectedSkill != null
        )
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            if (showQuickSkills) {
                QuickSkillRow(
                    skills = quickSkills,
                    selectedSkillId = selectedSkill?.id,
                    enabled = enabled && !busy,
                    onQuickSkillSelected = onQuickSkillSelected
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(
                        onClick = { uploadMenuExpanded = true },
                        enabled = enabled && !busy
                    ) {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = "上传或附件")
                    }
                    DropdownMenu(
                        expanded = uploadMenuExpanded,
                        onDismissRequest = { uploadMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.AudioFile, contentDescription = null) },
                            text = { Text(text = "上传音频") },
                            onClick = {
                                uploadMenuExpanded = false
                                onPickAudio()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                            text = { Text(text = "上传图片") },
                            onClick = {
                                uploadMenuExpanded = false
                                onPickImage()
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = onInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onInputFocusChanged(it.isFocused) }
                        .testTag(HomeScreenTestTags.INPUT_FIELD),
                    placeholder = {
                        val hint = if (isSmartAnalysisMode) {
                            "说明你想分析什么（默认分析最近的一段长内容）"
                        } else {
                            "上传文件或输入消息..."
                        }
                        Text(text = hint)
                    },
                    shape = MaterialTheme.shapes.large,
                    enabled = enabled,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    singleLine = false,
                    minLines = 2
                )
                TextButton(
                    onClick = onSendClicked,
                    enabled = canSend,
                    modifier = Modifier.testTag(HomeScreenTestTags.SEND_BUTTON)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = if (busy) "发送中" else "发送")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (busy) "发送中..." else "发送")
                }
            }
        }
    }
}

@Composable
private fun ExportActionRow(
    exporting: Boolean,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onExportPdfClicked,
            enabled = !exporting,
            modifier = Modifier.testTag(HomeScreenTestTags.EXPORT_PDF),
            label = { Text(text = "导出 PDF") }
        )
        AssistChip(
            onClick = onExportCsvClicked,
            enabled = !exporting,
            modifier = Modifier.testTag(HomeScreenTestTags.EXPORT_CSV),
            label = { Text(text = "导出 CSV") }
        )
    }
}

@Composable
private fun AttachmentRow(
    enabled: Boolean,
    onPickAudio: () -> Unit,
    onPickImage: () -> Unit
){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = onPickAudio,
            enabled = enabled,
            label = { Text(text = "上传音频") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = "上传音频"
                )
            }
        )
        AssistChip(
            onClick = onPickImage,
            enabled = enabled,
            label = { Text(text = "上传图片") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "上传图片"
                )
            }
        )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
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
            AssistChip(
                onClick = { onQuickSkillSelected(skill.id) },
                enabled = enabled,
                modifier = Modifier
                    .testTag(skillTag)
                    .padding(vertical = 2.dp),
                label = {
                    Text(
                        text = skill.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                // 快捷技能作为快捷填充，无需额外气泡
                colors = colors,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
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

@Composable
private fun HistoryDrawerContent(
    sessions: List<SessionListItemUi>,
    currentSessionId: String,
    onSessionSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ChatHistoryTestTags.PAGE),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "历史会话",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭"
                )
            }
        }
        if (sessions.isEmpty()) {
            Text(
                text = "暂无历史会话，先开始一次对话吧。",
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag(HomeScreenTestTags.HISTORY_EMPTY),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(items = sessions, key = { it.id }) { session ->
                    val isCurrent = session.id == currentSessionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSessionSelected(session.id) }
                            .testTag("${HomeScreenTestTags.HISTORY_ITEM_PREFIX}${session.id}"),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isCurrent) 4.dp else 1.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = session.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
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
