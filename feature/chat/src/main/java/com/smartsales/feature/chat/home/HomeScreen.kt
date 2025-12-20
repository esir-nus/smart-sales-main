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
import com.smartsales.data.aicore.debug.XfyunDebugInfoFormatter
import com.smartsales.data.aicore.debug.XfyunFailTypeHints
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.smartsales.feature.chat.BuildConfig

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
    onLoadMoreHistory = viewModel::onLoadMoreHistory,
    onProfileClicked = viewModel::onTapProfile,
    onNewChatClicked = viewModel::onNewChatClicked,
    onSessionSelected = viewModel::setSession,
    chatErrorMessage = state.chatErrorMessage,
    onPickAudioFile = viewModel::onAudioFilePicked,
    onPickImageFile = viewModel::onImagePicked,
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
            onVoiceprintRegisterBase64 = viewModel::registerVoiceprintBase64,
            onVoiceprintApplyLastFeatureId = viewModel::enableVoiceprintAndAddLastFeatureId,
            onVoiceprintApplyXfyunVpTestPreset = viewModel::applyXfyunVoiceprintTestPreset,
            onVoiceprintDeleteFeatureId = viewModel::deleteVoiceprintFeatureId,
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
    onPickAudioFile: (Uri) -> Unit = {},
    onPickImageFile: (Uri) -> Unit = {},
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
    onVoiceprintRegisterBase64: (audioDataBase64: String, audioType: String, uid: String?) -> Unit = { _, _, _ -> },
    onVoiceprintApplyLastFeatureId: () -> Unit = {},
    onVoiceprintApplyXfyunVpTestPreset: () -> Unit = {},
    onVoiceprintDeleteFeatureId: (featureId: String, removeFromSettings: Boolean) -> Unit = { _, _ -> },
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
    var showVoiceprintLabSheet by remember { mutableStateOf(false) }
    val voiceprintLabSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                            onOpenVoiceprintLab = {
                                // 重要：声纹注册是独立工具，不应依赖一次转写会话；否则无法先注册再使用声纹分离。
                                showVoiceprintLabSheet = true
                            },
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
                        if (hudEnabled && showDebugPanel && state.debugSessionMetadata != null) {
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
                                metadata = state.debugSessionMetadata,
                                xfyunTrace = state.xfyunTrace,
                                onRefreshXfyun = onRefreshXfyunTrace,
                                voiceprintLab = state.voiceprintLab,
                                onVoiceprintRegisterBase64 = onVoiceprintRegisterBase64,
                                onVoiceprintApplyLastFeatureId = onVoiceprintApplyLastFeatureId,
                                onVoiceprintDeleteFeatureId = onVoiceprintDeleteFeatureId,
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

                if (BuildConfig.DEBUG && showVoiceprintLabSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showVoiceprintLabSheet = false },
                        sheetState = voiceprintLabSheetState,
                    ) {
                        val copyText: (String) -> Unit = { text ->
                            runCatching { clipboardManager.setText(AnnotatedString(text)) }
                                .onSuccess {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Copied.") }
                                }
                                .onFailure {
                                    Toast.makeText(context, "Copy failed.", Toast.LENGTH_SHORT).show()
                                }
                        }
                            VoiceprintLabPanel(
                                voiceprintLab = state.voiceprintLab,
                                onRegisterBase64 = onVoiceprintRegisterBase64,
                                onApplyLastFeatureId = onVoiceprintApplyLastFeatureId,
                                onApplyTestPreset = onVoiceprintApplyXfyunVpTestPreset,
                                onDeleteFeatureId = onVoiceprintDeleteFeatureId,
                                onCopy = copyText,
                                onClose = { showVoiceprintLabSheet = false },
                            )
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
    onOpenVoiceprintLab: () -> Unit,
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
        if (BuildConfig.DEBUG) {
            IconButton(
                onClick = onOpenVoiceprintLab,
            ) {
                Icon(
                    imageVector = Icons.Filled.AudioFile,
                    contentDescription = "Voiceprint Lab",
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
    metadata: DebugSessionMetadata,
    xfyunTrace: XfyunTraceSnapshot?,
    onRefreshXfyun: () -> Unit,
    voiceprintLab: VoiceprintLabUiState,
    onVoiceprintRegisterBase64: (audioDataBase64: String, audioType: String, uid: String?) -> Unit,
    onVoiceprintApplyLastFeatureId: () -> Unit,
    onVoiceprintDeleteFeatureId: (featureId: String, removeFromSettings: Boolean) -> Unit,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    showRawAssistantOutput: Boolean,
    onToggleRawAssistantOutput: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
            appendLine("stage: ${metadata.stageLabel ?: "-"}")
            appendLine("risk: ${metadata.riskLabel ?: "-"}")
            appendLine("tags: ${metadata.tagsLabel?.ifBlank { "-" } ?: "-"}")
            appendLine("latestSource: ${metadata.latestSourceLabel ?: "-"}")
            appendLine("latestAt: ${metadata.latestAtLabel ?: "-"}")
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
            DebugField(label = "sessionId", value = metadata.sessionId)
            DebugField(label = "title", value = metadata.title)
            DebugField(label = "mainPerson", value = metadata.mainPerson ?: "-")
            DebugField(label = "shortSummary", value = metadata.shortSummary ?: "-")
            DebugField(label = "title6", value = metadata.summaryTitle6Chars ?: "-")
            DebugField(label = "stage", value = metadata.stageLabel ?: "-")
            DebugField(label = "risk", value = metadata.riskLabel ?: "-")
            DebugField(label = "tags", value = metadata.tagsLabel?.ifBlank { "-" } ?: "-")
            DebugField(label = "latestSource", value = metadata.latestSourceLabel ?: "-")
            DebugField(label = "latestAt", value = metadata.latestAtLabel ?: "-")
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
            if (CHAT_DEBUG_HUD_ENABLED) {
                XfyunTraceSection(
                    trace = xfyunTrace,
                    onRefresh = onRefreshXfyun,
                    onCopy = onCopy,
                    voiceprintLab = voiceprintLab,
                    onVoiceprintRegisterBase64 = onVoiceprintRegisterBase64,
                    onVoiceprintApplyLastFeatureId = onVoiceprintApplyLastFeatureId,
                    onVoiceprintDeleteFeatureId = onVoiceprintDeleteFeatureId,
                )
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
private fun XfyunTraceSection(
    trace: XfyunTraceSnapshot?,
    onRefresh: () -> Unit,
    onCopy: (String) -> Unit,
    voiceprintLab: VoiceprintLabUiState,
    onVoiceprintRegisterBase64: (audioDataBase64: String, audioType: String, uid: String?) -> Unit,
    onVoiceprintApplyLastFeatureId: () -> Unit,
    onVoiceprintDeleteFeatureId: (featureId: String, removeFromSettings: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var confirmShare by remember { mutableStateOf(false) }

    @Composable
    fun KeyValueRow(
        label: String,
        value: String,
        copyValue: String = value,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.32f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value.ifBlank { "-" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.54f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = { onCopy(copyValue) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Text(text = "复制")
            }
        }
    }

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
                if (trace != null) {
                    TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.format(trace)) }) {
                        Text(text = "复制摘要")
                    }
                    val rawDumpPath = trace.rawDumpPath
                    if (!rawDumpPath.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                val file = File(rawDumpPath)
                                if (!file.exists() || !file.isFile) {
                                    Toast.makeText(context, "dump 文件不存在，无法分享", Toast.LENGTH_SHORT).show()
                                    return@TextButton
                                }
                                confirmShare = true
                            }
                        ) {
                            Text(text = "分享 dump")
                        }
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

        // --- Compact key-value grid (max 8 rows) ---
        KeyValueRow(
            label = "orderId",
            value = trace.orderId ?: "-",
            copyValue = trace.orderId ?: "",
        )
        val rawDumpValue = trace.rawDumpPath?.let { path ->
            val bytes = trace.rawDumpBytes?.toString() ?: "-"
            "$path (${bytes} bytes)"
        } ?: "-"
        KeyValueRow(
            label = "rawDump",
            value = rawDumpValue,
            copyValue = rawDumpValue,
        )
        val pollValue = "count=${trace.pollCount}, elapsedMs=${trace.elapsedMs}"
        KeyValueRow(label = "poll", value = pollValue, copyValue = pollValue)
        val lastValue = "http=${trace.lastHttpCode ?: "-"}, failType=${trace.lastFailType ?: "-"}"
        KeyValueRow(label = "last", value = lastValue, copyValue = lastValue)
        val uploadBizValue = buildString {
            append("code=${trace.uploadBusinessCode ?: "-"}, ")
            append("category=${trace.uploadFailureCategory ?: "-"}, ")
            val desc = trace.uploadBusinessDescInfo?.trim().orEmpty()
            append("desc=${if (desc.isBlank()) "-" else desc.take(80)}")
        }
        KeyValueRow(label = "uploadBiz", value = uploadBizValue, copyValue = uploadBizValue)
        trace.uploadFailureHint?.takeIf { it.isNotBlank() }?.let { hint ->
            KeyValueRow(label = "uploadHint", value = hint, copyValue = hint)
        }

        // --- Voiceprint (roleType=3) evidence (privacy-safe) ---
        val vpSummary = buildString {
            append("enabled=${trace.voiceprintEnabledSetting}, ")
            append("effective=${trace.voiceprintEffectiveEnabled}, ")
            append("roleType=${trace.voiceprintRoleTypeApplied ?: "-"}, ")
            append("roleNum=${trace.voiceprintRoleNumApplied ?: "-"}, ")
            append("cfg=${trace.voiceprintFeatureIdsConfigured.size}, ")
            append("seen=${trace.voiceprintRoleLabelsSeen.size}, ")
            append("matched=${trace.voiceprintRoleLabelsMatched.size}")
        }
        KeyValueRow(label = "voiceprint", value = vpSummary, copyValue = vpSummary)
        trace.voiceprintDisabledReason?.takeIf { it.isNotBlank() }?.let { reason ->
            KeyValueRow(label = "vpDisabledReason", value = reason, copyValue = reason)
        }
        trace.voiceprintBaseUrlHostUsed?.takeIf { it.isNotBlank() }?.let { host ->
            KeyValueRow(label = "vpBaseUrlHost", value = host, copyValue = host)
        }
        // 重要：
        // - featureIds/rl 可能是敏感标识：HUD 不应默认明文展示。
        // - 这里只提供 copy-only JSON 供排错，且不包含任何签名/密钥/原始 HTTP 包体。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.voiceprintSettingsJson(trace)) }) {
                Text(text = "复制声纹设置(JSON)")
            }
            TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.voiceprintRoleLabelsJson(trace.voiceprintRoleLabelsSeen)) }) {
                Text(text = "复制 rl_seen(JSON)")
            }
            TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.voiceprintRoleLabelsJson(trace.voiceprintRoleLabelsMatched)) }) {
                Text(text = "复制 rl_matched(JSON)")
            }
            TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.voiceprintFeatureIdOrdinalMapJson(trace.voiceprintFeatureIdOrdinalMap)) }) {
                Text(text = "复制 featureId→ordinal(JSON)")
            }
        }

        // --- PostXFyun (compact + runtime proof) ---
        val postSettings = trace.postXfyunSettings
        var showPromptPreview by remember { mutableStateOf(false) }
        var showBatches by remember { mutableStateOf(false) }
        var showSuspicious by remember { mutableStateOf(false) }
        var showEvidence by remember { mutableStateOf(false) }

        @Composable
        fun ProofRow(
            label: String,
            value: String?,
            copyValue: String? = value,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.34f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value?.ifBlank { "-" } ?: "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(
                    onClick = { onCopy(copyValue ?: "") },
                    enabled = !copyValue.isNullOrBlank(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text(text = "Copy")
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "PostXFyun",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                val runStatus = postSettings?.runStatus ?: "UNKNOWN"
                val statusLabel = when (runStatus) {
                    "SKIPPED_DISABLED", "SKIPPED_INVALID_INPUT" -> "SKIPPED"
                    "RAN_REWRITE" -> "RAN"
                    "FAILED" -> "FAILED"
                    else -> runStatus
                }
                ProofRow("Status", statusLabel, statusLabel)
                postSettings?.skipReason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Text(
                        text = "Reason: ${reason.take(120)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                ProofRow("Enabled", postSettings?.enabled?.toString(), postSettings?.enabled?.toString())
                ProofRow("Model requested", postSettings?.modelRequested, postSettings?.modelRequested)
                ProofRow("Model used", postSettings?.modelUsed, postSettings?.modelUsed)

                ProofRow("Prompt template sha", postSettings?.promptSha256, postSettings?.promptSha256)
                ProofRow("Prompt effective sha", postSettings?.promptEffectiveSha256, postSettings?.promptEffectiveSha256)

                // 重要：UI 不允许 inline 展示任何 raw JSON（包括 LLM 决策 JSON 预览），只提供 copy-only 按钮。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { onCopy(XfyunDebugInfoFormatter.postXfyunSettingsJson(trace.postXfyunSettings)) },
                        enabled = trace.postXfyunSettings != null,
                    ) {
                        Text(text = "Copy settings JSON")
                    }
                    TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.postXfyunBatchPlanJson(trace.postXfyunBatchPlan)) }) {
                        Text(text = "Copy batch plan JSON")
                    }
                    TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.postXfyunSuspiciousJson(trace.postXfyunSuspicious)) }) {
                        Text(text = "Copy hints JSON")
                    }
                    TextButton(onClick = { onCopy(XfyunDebugInfoFormatter.postXfyunDecisionsJson(trace.postXfyunDecisions)) }) {
                        Text(text = "Copy decisions JSON")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { onCopy(XfyunDebugInfoFormatter.postXfyunLlmPreviewText(trace)) },
                        enabled = trace.postXfyunDecisions.isNotEmpty() || !trace.postXfyunSettings?.rewriteOutputPreview.isNullOrBlank(),
                    ) {
                        Text(text = "Copy LLM preview")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            val preview = postSettings?.promptEffectivePreview
                            if (preview.isNullOrBlank()) {
                                Toast.makeText(context, "No effective prompt preview", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            onCopy(preview)
                        }
                    ) { Text(text = "Copy effective prompt preview") }
                    TextButton(onClick = { showPromptPreview = !showPromptPreview }) {
                        Text(text = if (showPromptPreview) "Hide" else "Show")
                    }
                }
                if (showPromptPreview) {
                    Text(
                        text = postSettings?.promptEffectivePreview ?: "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val stats = "Candidates=${trace.postXfyunCandidatesCount}, Attempts=${trace.postXfyunArbitrationsAttempted}/${trace.postXfyunArbitrationBudget}, Repairs=${trace.postXfyunRepairsApplied}"
                ProofRow("Stats", stats, stats)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = {
                            val text = trace.postXfyunOriginalMarkdown
                            if (text.isNullOrBlank()) {
                                Toast.makeText(context, "No original transcript", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            onCopy(text)
                        }
                    ) { Text(text = "Copy original") }
                    TextButton(
                        onClick = {
                            val text = trace.postXfyunPolishedMarkdown
                            if (text.isNullOrBlank()) {
                                Toast.makeText(context, "No polished transcript", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            onCopy(text)
                        }
                    ) { Text(text = "Copy polished") }
                }

                // Batches (collapsed by default)
                TextButton(
                    onClick = { showBatches = !showBatches },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(text = if (showBatches) "Hide batches (${trace.postXfyunBatchPlan.size})" else "Show batches (${trace.postXfyunBatchPlan.size})")
                }
                if (showBatches) {
                    trace.postXfyunBatchPlan.take(5).forEach { batch ->
                        Text(
                            text = "${batch.batchId} [${batch.startLineIndex}..${batch.endLineIndexInclusive}]",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Suspicious boundaries (collapsed by default)
                TextButton(
                    onClick = { showSuspicious = !showSuspicious },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(text = if (showSuspicious) "Hide suspicious boundaries (${trace.postXfyunSuspicious.size})" else "Show suspicious boundaries (${trace.postXfyunSuspicious.size})")
                }
                if (showSuspicious) {
                    trace.postXfyunSuspicious.take(5).forEach { item ->
                        val prev = item.prevSpeakerId ?: "-"
                        val next = item.nextSpeakerId ?: "-"
                        Text(
                            text = "${item.susId} #${item.boundaryIndex} b=${item.batchId} local=${item.localBoundaryIndex} gapMs=${item.gapMs} prev=$prev next=$next",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // LLM evidence (collapsed by default)
                TextButton(
                    onClick = { showEvidence = !showEvidence },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(text = if (showEvidence) "Hide LLM evidence" else "Show LLM evidence")
                }
                if (showEvidence) {
                    trace.postXfyunDecisions.take(3).forEach { item ->
                        Text(
                            text = "[${item.attemptIndex}] ${item.susId} #${item.boundaryIndex} gapMs=${item.gapMs} " +
                                "apply=${item.applyStatus} parse=${item.parseStatus} model=${item.modelUsed ?: "-"} reason=${item.reason ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
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
private fun VoiceprintLabPanel(
    voiceprintLab: VoiceprintLabUiState,
    onRegisterBase64: (audioDataBase64: String, audioType: String, uid: String?) -> Unit,
    onApplyLastFeatureId: () -> Unit,
    onApplyTestPreset: () -> Unit,
    onDeleteFeatureId: (featureId: String, removeFromSettings: Boolean) -> Unit,
    onCopy: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var showBase64 by remember { mutableStateOf(false) }
    // 重要：声纹音频 base64 属于敏感数据：只保存在内存，不落盘、不打日志、不进入 trace/HUD。
    var audioBase64 by remember { mutableStateOf("") }
    var manualPasteInput by remember { mutableStateOf("") }
    var audioType by remember { mutableStateOf("raw") }
    var uid by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var deleteFeatureId by remember { mutableStateOf("") }
    var removeFromSettings by remember { mutableStateOf(true) }
    var lastRegisterInProgress by remember { mutableStateOf(false) }

    val lastFeatureId = voiceprintLab.lastFeatureId?.trim().orEmpty()
    var lastClearedFeatureId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lastFeatureId) {
        if (lastFeatureId.isNotBlank() && lastFeatureId != lastClearedFeatureId) {
            // 重要：成功后立即清空 base64，避免敏感音频数据长时间驻留在内存与 UI 中。
            audioBase64 = ""
            manualPasteInput = ""
            showBase64 = false
            lastClearedFeatureId = lastFeatureId
        }
    }
    LaunchedEffect(voiceprintLab.registerInProgress) {
        // 重要：无论成功还是失败，只要一次 register 尝试结束，就清空 base64，避免敏感数据驻留。
        if (lastRegisterInProgress && !voiceprintLab.registerInProgress) {
            audioBase64 = ""
            manualPasteInput = ""
            showBase64 = false
        }
        lastRegisterInProgress = voiceprintLab.registerInProgress
    }

    val base64FilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            // 重要：只读取 txt 内容到内存，不落盘、不打日志；并剔除空白符，得到纯 base64 字符串。
            val rawText = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }.orEmpty()
            audioBase64 = rawText.filterNot { it.isWhitespace() }
            manualPasteInput = ""
            showBase64 = false
        }.onFailure {
            Toast.makeText(context, "Failed to load file.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Voiceprint Lab (Debug only)", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
            }
        }
        Text(
            text = "Warning: Do not paste sensitive audio in production builds. Base64 is never stored; it is sent once and cleared after each attempt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextButton(
            onClick = onApplyTestPreset,
            enabled = !voiceprintLab.registerInProgress && !voiceprintLab.deleteInProgress,
            contentPadding = PaddingValues(horizontal = 0.dp),
        ) {
            Text(text = "Apply XFyun VP Test Preset")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { base64FilePicker.launch(arrayOf("text/plain")) },
                enabled = !voiceprintLab.registerInProgress,
            ) { Text(text = "Load Base64 from .txt") }
            TextButton(onClick = { showBase64 = !showBase64 }) {
                Text(text = if (showBase64) "Hide Base64" else "Show Base64")
            }
            TextButton(
                onClick = {
                    audioBase64 = ""
                    manualPasteInput = ""
                    showBase64 = false
                },
                enabled = (audioBase64.isNotBlank() || manualPasteInput.isNotBlank()) && !voiceprintLab.registerInProgress,
            ) { Text(text = "Clear Base64") }
        }

        val base64Len = audioBase64.length
        val base64Preview = if (base64Len >= 32) {
            "${audioBase64.take(16)}…${audioBase64.takeLast(16)}"
        } else if (base64Len > 0) {
            audioBase64.take(24)
        } else {
            "-"
        }
        Text(
            text = "Loaded base64 length: $base64Len chars",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Preview: $base64Preview",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        // Paste input (optional): do NOT render the full base64 string to avoid Compose lag.
        OutlinedTextField(
            value = manualPasteInput,
            onValueChange = { value ->
                if (value.isBlank()) {
                    manualPasteInput = ""
                    return@OutlinedTextField
                }
                // 重要：保存原始输入到内存并清空输入框，避免 TextField 渲染大段 base64 导致卡顿。
                audioBase64 = value.filterNot { it.isWhitespace() }
                manualPasteInput = ""
                showBase64 = false
            },
            label = { Text(text = "Paste Base64 here (will auto-hide)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showBase64) VisualTransformation.None else PasswordVisualTransformation(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "audio_type:", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { typeMenuExpanded = true }) {
                Text(text = audioType)
                Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                listOf("raw", "speex", "opus-ogg").forEach { type ->
                    DropdownMenuItem(
                        text = { Text(text = type) },
                        onClick = {
                            audioType = type
                            typeMenuExpanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "uid (optional):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = uid,
            onValueChange = { uid = it },
            label = { Text(text = "uid") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onRegisterBase64(audioBase64, audioType, uid.ifBlank { null }) },
                enabled = audioBase64.trim().isNotBlank() && !voiceprintLab.registerInProgress,
            ) {
                if (voiceprintLab.registerInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = "Register Voiceprint")
            }

            TextButton(
                onClick = onApplyLastFeatureId,
                enabled = lastFeatureId.isNotBlank(),
            ) {
                Text(text = "Enable + Add feature_id to Settings")
            }
        }

        if (lastFeatureId.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "feature_id: ${lastFeatureId.take(64)}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = { onCopy(lastFeatureId) }) {
                    Text(text = "Copy feature_id")
                }
            }
        }

        val settingsHint = buildString {
            val enabledSetting = voiceprintLab.enabledSetting
            val count = voiceprintLab.configuredFeatureIdCount
            if (enabledSetting != null || count != null) {
                append("Settings: enabled=")
                append(enabledSetting ?: "-")
                append(", featureIdsCount=")
                append(count ?: "-")
            }
        }
        if (settingsHint.isNotBlank()) {
            Text(
                text = settingsHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val bodySmall = MaterialTheme.typography.bodySmall
        val onSurface = MaterialTheme.colorScheme.onSurface
        val vpHost = voiceprintLab.lastApiHost?.takeIf { it.isNotBlank() } ?: "-"
        val vpPath = voiceprintLab.lastApiPath?.takeIf { it.isNotBlank() } ?: "-"
        val vpHttp = voiceprintLab.lastHttpCode?.toString() ?: "-"
        val vpBizCode = voiceprintLab.lastBusinessCode?.takeIf { it.isNotBlank() } ?: "-"
        val vpBizDesc = voiceprintLab.lastBusinessDesc?.takeIf { it.isNotBlank() } ?: "-"
        val callHint = "Last call: host=$vpHost path=$vpPath http=$vpHttp code=$vpBizCode"
        Text(
            text = callHint,
            style = bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (vpBizDesc != "-") {
            Text(
                text = "Business desc: ${vpBizDesc.take(120)}",
                style = bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        voiceprintLab.lastMessage?.takeIf { it.isNotBlank() }?.let { msg ->
            Text(
                text = msg,
                style = bodySmall,
                color = onSurface,
            )
        }

        HorizontalDivider()

        Text(
            text = "Delete feature_id (optional)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = deleteFeatureId,
            onValueChange = { deleteFeatureId = it },
            label = { Text(text = "feature_id to delete") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Remove from settings", style = MaterialTheme.typography.bodySmall)
            Switch(
                checked = removeFromSettings,
                onCheckedChange = { removeFromSettings = it },
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onDeleteFeatureId(deleteFeatureId, removeFromSettings) },
                enabled = deleteFeatureId.trim().isNotBlank() && !voiceprintLab.deleteInProgress,
            ) {
                if (voiceprintLab.deleteInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = "Delete feature_id")
            }
        }

        Text(
            text = "Next: run transcription and verify in HUD/trace: voiceprintEffectiveEnabled=true, roleTypeApplied=3, roleLabelsMatched>0.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatElapsed(valueMs: Long): String {
    if (valueMs <= 0) return "-"
    val totalSeconds = valueMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m${seconds}s"
}

private fun formatParams(params: Map<String, String>): String {
    if (params.isEmpty()) return "{}"
    return buildString {
        appendLine("{")
        params.entries.forEachIndexed { index, (key, value) ->
            val comma = if (index == params.size - 1) "" else ","
            appendLine("  \"$key\": \"$value\"$comma")
        }
        append("}")
    }
}

@Composable
private fun DebugJsonBlock(
    label: String,
    json: String?,
    onCopy: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!json.isNullOrBlank()) {
                TextButton(onClick = { onCopy(json) }) { Text(text = "复制") }
            }
        }
        val display = if (expanded) json ?: "-" else json?.take(240) ?: "-"
        Text(
            text = display,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 8,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
                .clickable { expanded = !expanded }
        )
        if (!json.isNullOrBlank()) {
            Text(
                text = if (expanded) "点击收起" else "点击展开",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
                labelColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    // 智能分析未选中时保持中性色，避免误导
                    skill.id == QuickSkillId.SMART_ANALYSIS -> MaterialTheme.colorScheme.onSurface
                    skill.isRecommended -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
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
