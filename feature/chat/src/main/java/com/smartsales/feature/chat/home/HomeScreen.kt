// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 页面 Compose UI（包含 Debug HUD）
// 作者：创建于 2025-12-15

package com.smartsales.feature.chat.home

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.smartsales.feature.chat.core.QuickSkillId
import java.io.File
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import com.smartsales.data.aicore.debug.TingwuTraceSnapshot
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.smartsales.feature.chat.BuildConfig
import com.smartsales.feature.chat.home.export.ExportGateState


// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，仅渲染聊天欢迎区、消息列表、快捷技能与输入框
// 作者：创建于 2025-11-20

@OptIn(ExperimentalMaterial3Api::class)
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
    exportGateState = state.exportGateState,
    onLoadMoreHistory = viewModel::onLoadMoreHistory,
    onProfileClicked = viewModel::onTapProfile,
    onNewChatClicked = viewModel::onNewChatClicked,
    onSessionSelected = viewModel::setSession,
    chatErrorMessage = state.chatErrorMessage,
    onPickAudioFile = viewModel::onAudioFilePicked,
    onPickImageFile = viewModel::onImagePicked,
    onDismissAudioRecoveryHint = viewModel::dismissAudioRecoveryHint,
    modifier = modifier,
    showHistoryPanel = showHistoryPanel,
        onToggleHistoryPanel = { showHistoryPanel = true },
        onDismissHistoryPanel = { showHistoryPanel = false },
        historySessions = state.sessionList.take(10),
        onHistorySessionSelected = { sessionId ->
            viewModel.setSession(sessionId, allowHero = false)
            showHistoryPanel = false
        },
        onHistorySessionLongPress = viewModel::onHistorySessionLongPress,
        onHistoryActionPinToggle = viewModel::onHistorySessionPinToggle,
        onHistoryActionRenameStart = viewModel::onHistoryActionRenameStart,
        onHistoryActionRenameConfirm = { sessionId, title -> viewModel.onHistorySessionRenameConfirmed(sessionId, title) },
        onHistoryActionDelete = viewModel::onHistorySessionDelete,
        onHistoryActionDismiss = viewModel::onHistoryActionDismiss,
        onHistoryRenameTextChange = viewModel::onHistoryRenameTextChange,
        onToggleDebugMetadata = viewModel::toggleDebugMetadata,
            onRefreshXfyunTrace = viewModel::refreshXfyunTrace,

            onToggleRawAssistantOutput = viewModel::setShowRawAssistantOutput,
            onDismissKeyboard = dismissKeyboard,
            onInputFocusChanged = { focused ->
                viewModel.onInputFocusChanged(focused)
            onInputFocusChanged(focused)
        }
    )

    val actionSession = state.historyActionSession
    if (actionSession != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::onHistoryActionDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SheetAction(
                    label = if (actionSession.pinned) "取消置顶" else "置顶会话",
                    onClick = { viewModel.onHistorySessionPinToggle(actionSession.id) }
                )
                SheetAction(
                    label = "重命名",
                    onClick = { viewModel.onHistoryActionRenameStart() }
                )
                SheetAction(
                    label = "删除",
                    onClick = { viewModel.onHistorySessionDelete(actionSession.id) }
                )
                HorizontalDivider()
                SheetAction(
                    label = "取消",
                    onClick = viewModel::onHistoryActionDismiss
                )
            }
        }
    }

    if (state.showHistoryRenameDialog && actionSession != null) {
        AlertDialog(
            onDismissRequest = viewModel::onHistoryActionDismiss,
            title = { Text(text = "重命名会话") },
            text = {
                OutlinedTextField(
                    value = state.historyRenameText,
                    onValueChange = viewModel::onHistoryRenameTextChange,
                    label = { Text(text = "标题") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onHistorySessionRenameConfirmed(actionSession.id, state.historyRenameText)
                        viewModel.onHistoryActionDismiss()
                    },
                    enabled = state.historyRenameText.isNotBlank()
                ) {
                    Text(text = "保存标题")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onHistoryActionDismiss) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
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
    exportGateState: ExportGateState?,
    onPickAudioFile: (Uri) -> Unit = {},
    onPickImageFile: (Uri) -> Unit = {},
    onDismissAudioRecoveryHint: () -> Unit = {},
    modifier: Modifier = Modifier,
    showHistoryPanel: Boolean = false,
    onToggleHistoryPanel: () -> Unit = {},
    onDismissHistoryPanel: () -> Unit = {},
    historySessions: List<SessionListItemUi> = emptyList(),
    onHistorySessionSelected: (String) -> Unit = {},
    onHistorySessionLongPress: (String) -> Unit = {},
    onHistoryActionPinToggle: (String) -> Unit = {},
    onHistoryActionRenameStart: () -> Unit = {},
    onHistoryActionRenameConfirm: (String, String) -> Unit = { _, _ -> },
    onHistoryActionDelete: (String) -> Unit = {},
    onHistoryActionDismiss: () -> Unit = {},
    onHistoryRenameTextChange: (String) -> Unit = {},
    onToggleDebugMetadata: () -> Unit = {},
    onRefreshXfyunTrace: () -> Unit = {},

    onToggleRawAssistantOutput: (Boolean) -> Unit = {},
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
    val context = LocalContext.current
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
                    // TODO(hardware): 接入真实设备状态后更新此处占位卡片
                    deviceSnapshot = state.deviceSnapshot,
                    onSessionSelected = onHistorySessionSelected,
                    onSessionLongPress = onHistorySessionLongPress,
                    onUserCenterClick = onProfileClicked
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
                        // TODO(hardware): 后续接入真实设备状态，填充顶栏指示器文案
                        HomeTopBar(
                            title = state.currentSession.title,
                            deviceSnapshot = state.deviceSnapshot,
                            onHistoryClick = {
                                onDismissKeyboard()
                                onToggleHistoryPanel()
                                coroutineScope.launch { drawerState.open() }
                            },
                            onNewChatClick = onNewChatClicked,
                            hudEnabled = hudEnabled,
                            showDebugMetadata = state.showDebugMetadata,
                            onToggleDebugMetadata = { onToggleDebugMetadata() },
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
                            exportGateState = exportGateState,
                            onInputChanged = onInputChanged,
                            onSendClicked = {
                                onSendClicked()
                                onDismissKeyboard()
                            },
                            onQuickSkillSelected = onQuickSkillSelected,
                            onExportPdfClicked = onExportPdfClicked,
                            onExportCsvClicked = onExportCsvClicked,
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
                            if (state.showAudioRecoveryHint) {
                                AudioRecoveryHintBanner(
                                    onReupload = { audioPicker.launch(arrayOf("audio/*")) },
                                    onDismiss = onDismissAudioRecoveryHint
                                )
                            }
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
                                            exportGateState = exportGateState,
                                            enabled = !chatBusy && !state.isInputBusy,
                                            onSkillSelected = onQuickSkillSelected,
                                            onExportPdfClicked = onExportPdfClicked,
                                            onExportCsvClicked = onExportCsvClicked
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
                                                        val alignEnd = message.role == ChatMessageRole.USER
                                                        MessageBubble(
                                                            message = message,
                                                            alignEnd = alignEnd,
                                                            modifier = tagModifier,
                                                            reasoningText = if (!alignEnd && message.isSmartAnalysis) {
                                                                state.smartReasoningText
                                                            } else {
                                                                null
                                                            },
                                                            showRawAssistantOutput = state.showRawAssistantOutput,
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
                        if (hudEnabled && showDebugPanel) {
                            LaunchedEffect(showDebugPanel) { onRefreshXfyunTrace() }
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
                                snapshot = state.debugSnapshot,
                                xfyunTrace = state.xfyunTrace,
                                tingwuTrace = state.tingwuTrace,
                                onRefreshXfyun = onRefreshXfyunTrace,
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
                                showRawAssistantOutput = state.showRawAssistantOutput,
                                onToggleRawAssistantOutput = onToggleRawAssistantOutput,
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
    exportGateState: ExportGateState?,
    enabled: Boolean,
    onSkillSelected: (QuickSkillId) -> Unit,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit
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
                onQuickSkillSelected = onSkillSelected,
                onExportPdfClicked = onExportPdfClicked,
                onExportCsvClicked = onExportCsvClicked
            )
            ExportGateHint(exportGateState = exportGateState)
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

@Composable
private fun AudioRecoveryHintBanner(
    onReupload: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "上次音频处理似乎被中断。建议重新上传音频继续。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReupload) {
                    Text(text = "重新上传")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = "知道了")
                }
            }
        }
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
    const val HISTORY_USER_CENTER = "home_history_user_center"
    const val HERO = "home_hero"
    const val DEBUG_HUD_PANEL = "debug_hud_panel"
    const val DEBUG_HUD_TOGGLE = "debug_toggle_metadata"
    const val DEBUG_HUD_SCRIM = "debug_hud_scrim"
    const val DEBUG_HUD_CLOSE = "debug_hud_close"
    const val DEBUG_HUD_COPY = "debug_hud_copy"
    const val HOME_DEVICE_INDICATOR = "home_device_indicator"
    const val HISTORY_DEVICE_STATUS = "home_history_device_status"
}

@Composable
private fun HomeTopBar(
    title: String,
    deviceSnapshot: DeviceSnapshotUi?,
    onHistoryClick: () -> Unit,
    onNewChatClick: () -> Unit,
    hudEnabled: Boolean,
    showDebugMetadata: Boolean,
    onToggleDebugMetadata: () -> Unit,

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
            Icon(Icons.Filled.Menu, contentDescription = "历史记录")
        }
        DeviceStatusIndicator(snapshot = deviceSnapshot)
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .testTag(HomeScreenTestTags.SESSION_TITLE),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    }
}

@Suppress("UnusedParameter")
@Composable
private fun DeviceStatusIndicator(snapshot: DeviceSnapshotUi?) {
    Surface(
        modifier = Modifier
            .testTag(HomeScreenTestTags.HOME_DEVICE_INDICATOR),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DeviceHub,
                contentDescription = "设备状态",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "设备状态",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Suppress("UnusedParameter")
@Composable
private fun HistoryDeviceStatus(snapshot: DeviceSnapshotUi?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .testTag(HomeScreenTestTags.HISTORY_DEVICE_STATUS),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DeviceHub,
                    contentDescription = "设备状态",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "设备状态",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "设备状态将在硬件接入后展示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "硬件上线后将在此显示连接、电量与存储信息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DebugSessionMetadataHud(
    snapshot: com.smartsales.data.aicore.debug.DebugSnapshot?,
    xfyunTrace: XfyunTraceSnapshot?,
    tingwuTrace: TingwuTraceSnapshot?,
    onRefreshXfyun: () -> Unit,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    showRawAssistantOutput: Boolean,
    onToggleRawAssistantOutput: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenHalfHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
    val scrollState = rememberScrollState()
    val section1Text = snapshot?.section1EffectiveRunText ?: "加载中..."
    val section2Text = snapshot?.section2RawTranscriptionText ?: "加载中..."
    val section3Text = snapshot?.section3PreprocessedText ?: "加载中..."
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
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_CLOSE)
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "关闭 HUD")
                }
                }
            }
        if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "显示原始助手回复（仅调试）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "切换后气泡显示原始模型文本，不影响元数据或正式体验。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showRawAssistantOutput,
                        onCheckedChange = onToggleRawAssistantOutput
                    )
                }
            }
            // 重要：HUD 内容由 Orchestrator 生成，UI 只渲染字符串，不做二次拼装。
            DebugSnapshotBlock(
                title = "Section 1 — Effective Run Snapshot",
                content = section1Text,
                onCopy = onCopy,
                testTag = HomeScreenTestTags.DEBUG_HUD_COPY,
            )
            DebugSnapshotBlock(
                title = "Section 2 — Raw Transcription Output",
                content = section2Text,
                onCopy = onCopy,
                testTag = null,
            )
            DebugSnapshotBlock(
                title = "Section 3 — Preprocessed Snapshot",
                content = section3Text,
                onCopy = onCopy,
                testTag = null,
            )

            if (CHAT_DEBUG_HUD_ENABLED) {
                TingwuTraceSection(
                    trace = tingwuTrace,
                )
                XfyunTraceSection(
                    trace = xfyunTrace,
                    onRefresh = onRefreshXfyun,
                )
            }
        }
    }
}

@Composable
private fun DebugSnapshotBlock(
    title: String,
    content: String,
    onCopy: (String) -> Unit,
    testTag: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = { onCopy(content) },
                enabled = content.isNotBlank(),
                modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
            ) {
                Text(text = "复制")
            }
        }
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(10.dp),
        )
    }
}

@Composable
private fun TingwuTraceSection(
    trace: TingwuTraceSnapshot?,
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Tingwu 调试",
            style = MaterialTheme.typography.titleMedium
        )
        if (trace == null) {
            Text(
                text = "暂无 Tingwu 调用记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        val rawDumpPath = trace.transcriptionDumpPath
        val rawFile = rawDumpPath?.let { path -> File(path) }?.takeIf { it.exists() && it.isFile }
        val rawStatus = if (rawFile == null) "missing" else "available"
        Text(
            text = "raw.json: $rawStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                val file = rawFile ?: return@TextButton
                runCatching {
                    // 重要：调试导出使用 FileProvider，避免复制大文本或内联展示原始内容。
                    val authority = "${context.packageName}.chatfileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "导出 ${file.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }.onFailure { error ->
                    Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = rawFile != null,
        ) {
            Text(text = "导出 Raw JSON")
        }
        val transcriptPath = trace.transcriptDumpPath
        val transcriptFile = transcriptPath?.let { path -> File(path) }?.takeIf { it.exists() && it.isFile }
        val transcriptStatus = if (transcriptFile == null) "missing" else "available"
        Text(
            text = "transcript.txt: $transcriptStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                val file = transcriptFile ?: return@TextButton
                runCatching {
                    // 重要：调试导出使用 FileProvider，避免复制大文本或内联展示原始内容。
                    val authority = "${context.packageName}.chatfileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "导出 ${file.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }.onFailure { error ->
                    Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = transcriptFile != null,
        ) {
            Text(text = "导出 Transcript")
        }
    }
}

@Composable
private fun XfyunTraceSection(
    trace: XfyunTraceSnapshot?,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    var confirmShare by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "XFyun 调试",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRefresh) { Text(text = "刷新") }
                if (trace?.rawDumpPath?.isNotBlank() == true) {
                    TextButton(onClick = { confirmShare = true }) {
                        Text(text = "分享 dump")
                    }
                }
            }
        }
        if (trace == null) {
            Text(
                text = "暂无 XFyun 调用记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val rawDumpPath = trace.rawDumpPath
        val rawFile = rawDumpPath?.let { path -> File(path) }?.takeIf { it.exists() && it.isFile }
        val rawStatus = if (rawFile == null) "missing" else "available"
        Text(
            text = "raw.json: $rawStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trace.orderId?.takeIf { it.isNotBlank() }?.let { orderId ->
            Text(
                text = "orderId: ${orderId.take(64)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (trace != null && confirmShare) {
        AlertDialog(
            onDismissRequest = { confirmShare = false },
            title = { Text(text = "分享 XFyun 原始返回") },
            text = {
                Text(text = "将导出并分享 XFyun 原始返回 JSON，可能包含转写文本。是否继续？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmShare = false
                        val path = trace.rawDumpPath
                        if (path.isNullOrBlank()) {
                            Toast.makeText(context, "暂无可分享的 dump 文件", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val file = File(path)
                        if (!file.exists() || !file.isFile) {
                            Toast.makeText(context, "dump 文件不存在，无法分享", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        runCatching {
                            // 重要：
                            // - 使用 FileProvider 生成 content:// Uri，避免 file:// 在新系统上被拦截崩溃。
                            // - 授予临时读权限，便于微信等第三方 App 读取文件内容。
                            val authority = "${context.packageName}.chatfileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(intent, "分享 ${file.name}").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        }.onFailure { throwable ->
                            Toast.makeText(context, throwable.message ?: "分享失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(text = "继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmShare = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable

private fun formatMillis(value: Long): String {
    if (value <= 0) return "-"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(value))
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
    reasoningText: String? = null,
    onCopyAssistant: (String) -> Unit = {},
    showRawAssistantOutput: Boolean = false
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
                val renderMarkdown = !alignEnd && !message.isSmartAnalysis && !message.hasError && !showRawAssistantOutput
                if (renderMarkdown) {
                    MarkdownMessageText(
                        text = message.content
                    )
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!alignEnd && !message.hasError && !reasoningText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = reasoningText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    exportGateState: ExportGateState?,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit,
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
                    onQuickSkillSelected = onQuickSkillSelected,
                    onExportPdfClicked = onExportPdfClicked,
                    onExportCsvClicked = onExportCsvClicked
                )
                ExportGateHint(exportGateState = exportGateState)
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
    onQuickSkillSelected: (QuickSkillId) -> Unit,
    onExportPdfClicked: () -> Unit,
    onExportCsvClicked: () -> Unit
) {
    if (skills.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(skills, key = { it.id }) { skill ->
            val skillTag = "home_quick_skill_${skill.id}"
            val isExportSkill = skill.id == QuickSkillId.EXPORT_PDF || skill.id == QuickSkillId.EXPORT_CSV
            val isSelected = !isExportSkill && skill.id == selectedSkillId
            val skillEnabled = enabled
            val colors = AssistChipDefaults.assistChipColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                labelColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    // 智能分析未选中时保持中性色，避免误导
                    skill.id == QuickSkillId.SMART_ANALYSIS -> MaterialTheme.colorScheme.onSurface
                    skill.isRecommended -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            AssistChip(
                onClick = {
                    if (isExportSkill) {
                        // 导出类技能为立即动作：不改变输入框/选中态。
                        when (skill.id) {
                            QuickSkillId.EXPORT_PDF -> onExportPdfClicked()
                            QuickSkillId.EXPORT_CSV -> onExportCsvClicked()
                            else -> Unit
                        }
                    } else {
                        onQuickSkillSelected(skill.id)
                    }
                },
                enabled = skillEnabled,
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
private fun ExportGateHint(exportGateState: ExportGateState?) {
    if (exportGateState == null || exportGateState.ready || exportGateState.reason.isBlank()) return
    Text(
        text = exportGateState.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
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
@OptIn(ExperimentalFoundationApi::class)
private fun HistoryDrawerContent(
    sessions: List<SessionListItemUi>,
    currentSessionId: String,
    deviceSnapshot: DeviceSnapshotUi?,
    onSessionSelected: (String) -> Unit,
    onSessionLongPress: (String) -> Unit,
    onUserCenterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ChatHistoryTestTags.PAGE),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HistoryDeviceStatus(snapshot = deviceSnapshot)
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                Text(
                    text = "暂无历史会话，先开始一次对话吧。",
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag(HomeScreenTestTags.HISTORY_EMPTY),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(items = sessions, key = { it.id }) { session ->
                    val isCurrent = session.id == currentSessionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSessionSelected(session.id) },
                                onLongClick = { onSessionLongPress(session.id) }
                            )
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
        Spacer(modifier = Modifier.height(8.dp))
        HistoryUserCenter(onClick = onUserCenterClick)
    }
}

@Composable
private fun HistoryUserCenter(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(HomeScreenTestTags.HISTORY_USER_CENTER),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "个人中心",
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "个人中心",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "查看账号设置与偏好",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
