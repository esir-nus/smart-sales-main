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
import androidx.compose.animation.core.* // Added
import androidx.compose.ui.graphics.Brush // Added
import androidx.compose.ui.graphics.Color // Added
import androidx.compose.ui.graphics.graphicsLayer // Added
import androidx.compose.ui.text.SpanStyle // Added
import androidx.compose.ui.text.buildAnnotatedString // Added
import androidx.compose.ui.text.withStyle // Added
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
import androidx.compose.ui.text.style.TextAlign
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
import com.smartsales.domain.config.QuickSkillId
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
import com.smartsales.feature.chat.home.debug.DebugSessionMetadataHud
import com.smartsales.domain.export.ExportGateState
import com.smartsales.feature.chat.history.HistoryDrawerContent
import com.smartsales.feature.chat.home.input.HomeInputArea
import com.smartsales.feature.chat.home.input.QuickSkillRow
import com.smartsales.feature.chat.home.input.ExportGateHint
import com.smartsales.feature.chat.home.messages.MessageBubble
import com.smartsales.feature.chat.home.components.ChromaWave
import com.smartsales.feature.chat.home.components.ChromaWaveRules
import com.smartsales.feature.chat.home.components.ChromaWaveVisualState
import com.smartsales.feature.chat.home.components.MotionState
import com.smartsales.feature.chat.home.components.KnotSymbol
import com.smartsales.feature.chat.home.components.ActionGrid
import com.smartsales.feature.chat.home.theme.AppColors // Restored
import com.smartsales.feature.chat.home.theme.AppTypography
import com.smartsales.feature.chat.home.theme.AppSpacing
import com.smartsales.feature.chat.home.theme.AppDimensions
import com.smartsales.feature.chat.home.components.AuroraBackground

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，仅渲染聊天欢迎区、消息列表、快捷技能与输入框
// 作者：创建于 2025-11-20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    audioViewModel: com.smartsales.feature.chat.audio.AudioViewModel = hiltViewModel(),
    sessionListViewModel: com.smartsales.feature.chat.sessionlist.SessionListViewModel = hiltViewModel(),
    exportViewModel: com.smartsales.feature.chat.export.ExportViewModel = hiltViewModel(),
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
    onDeviceClick: () -> Unit = {}, // Added
    onDeviceSnapshotChanged: (DeviceSnapshotUi?) -> Unit = {},
    onInputFocusChanged: (Boolean) -> Unit = {},
) {
    Log.i("HomeScreenRoute", "HomeScreenRoute composed - entering function")
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val audioState by audioViewModel.state.collectAsStateWithLifecycle()
    val sessionListState by sessionListViewModel.uiState.collectAsStateWithLifecycle()
    val exportState by exportViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
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

    // Snackbar from AudioViewModel
    LaunchedEffect(audioState.snackbarMessage) {
        val message = audioState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            audioViewModel.onDismissSnackbar()
        }
    }

    // 监听导航请求并通过回调交给宿主 Activity
    LaunchedEffect(state.navigationRequest, audioState.navigationRequest) {
        val navRequest = state.navigationRequest ?: audioState.navigationRequest
        if (navRequest != null) {
            dismissKeyboard()
        }
        when (navRequest) {
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
        if (audioState.navigationRequest != null) {
            audioViewModel.onNavigationConsumed()
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
    LaunchedEffect(audioState.deviceSnapshot) {
        onDeviceSnapshotChanged(audioState.deviceSnapshot)
    }
    
    // Handle export events
    LaunchedEffect(Unit) {
        exportViewModel.events.collect { event ->
            when (event) {
                is com.smartsales.feature.chat.export.ExportEvent.ExportCompleted -> {
                    val message = when (event.result) {
                        is com.smartsales.core.util.Result.Success -> "导出成功"
                        is com.smartsales.core.util.Result.Error -> event.result.throwable.message ?: "导出失败"
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }
    
    // Handle session list events
    LaunchedEffect(Unit) {
        sessionListViewModel.events.collect { event ->
            when (event) {
                is com.smartsales.feature.chat.sessionlist.SessionListEvent.SwitchToSession -> {
                    viewModel.setSession(event.id, allowHero = true)
                    showHistoryPanel = false
                }
                is com.smartsales.feature.chat.sessionlist.SessionListEvent.TitleRenamed -> {
                    // Sync current session title if this session was renamed
                    if (event.sessionId == state.currentSession.id) {
                        viewModel.updateCurrentSessionTitle(event.newTitle)
                    }
                }
            }
        }
    }
    
    // Sync current session ID to session list
    LaunchedEffect(state.currentSession.id) {
        sessionListViewModel.setCurrentSessionId(state.currentSession.id)
    }

        HomeScreen(
            state = state,
            audioState = audioState,
            snackbarHostState = snackbarHostState,
            onInputChanged = viewModel::onInputChanged,
            onSendClicked = viewModel::onSendMessage,
        onQuickSkillSelected = viewModel::onSelectQuickSkill,
        onDeviceBannerClicked = audioViewModel::onTapDeviceBanner,
        onAudioSummaryClicked = audioViewModel::onTapAudioSummary,
        onRefreshDeviceAndAudio = audioViewModel::onRefreshDeviceAndAudio,
        onExportPdfClicked = {
            val markdown = viewModel.getExportMarkdown()
            exportViewModel.performExport(
                sessionId = state.currentSession.id,
                format = com.smartsales.data.aicore.ExportFormat.PDF,
                userName = state.userName,
                markdown = markdown
            )
        },
        onExportCsvClicked = {
            val markdown = viewModel.getExportMarkdown()
            exportViewModel.performExport(
                sessionId = state.currentSession.id,
                format = com.smartsales.data.aicore.ExportFormat.CSV,
                userName = state.userName,
                markdown = markdown
            )
        },
        exportInProgress = exportState.inProgress,
        exportGateState = exportState.gateState,
    onLoadMoreHistory = viewModel::onLoadMoreHistory,
    onProfileClicked = viewModel::onTapProfile,
    onNewChatClicked = viewModel::onNewChatClicked,
    onSessionSelected = viewModel::setSession,
    chatErrorMessage = state.chatErrorMessage,
    onPickAudioFile = { uri ->
        coroutineScope.launch {
            val result = audioViewModel.onAudioFilePicked(uri, state.currentSession.id)
            if (result is com.smartsales.core.util.Result.Success) {
                viewModel.onTranscriptionRequested(result.data)
            }
        }
    },
    onPickImageFile = viewModel::onImagePicked,
    onDismissAudioRecoveryHint = audioViewModel::dismissAudioRecoveryHint,
    modifier = modifier,
    showHistoryPanel = showHistoryPanel,
        onToggleHistoryPanel = { showHistoryPanel = true },
        onDismissHistoryPanel = { showHistoryPanel = false },
        historySessions = sessionListState.sessions.take(10),
        onHistorySessionSelected = sessionListViewModel::onSessionSelected,
        onHistorySessionLongPress = sessionListViewModel::onSessionLongPress,
        onHistoryActionPinToggle = sessionListViewModel::onPinToggle,
        onHistoryActionRenameStart = sessionListViewModel::onActionRenameStart,
        onHistoryActionRenameConfirm = sessionListViewModel::onRenameConfirmed,
        onHistoryActionDelete = { sessionId -> sessionListViewModel.onDelete(sessionId, state.currentSession.id) },
        onHistoryActionDismiss = sessionListViewModel::onActionDismiss,
        onHistoryRenameTextChange = sessionListViewModel::onRenameTextChange,
        onToggleDebugMetadata = viewModel::toggleDebugMetadata,
            onRefreshXfyunTrace = viewModel::refreshXfyunTrace,

            onToggleRawAssistantOutput = viewModel::setShowRawAssistantOutput,
            onDismissKeyboard = dismissKeyboard,
            onInputFocusChanged = { focused ->
                viewModel.onInputFocusChanged(focused)
            onInputFocusChanged(focused)
        }
    )

    val actionSession = sessionListState.actionSession
    if (actionSession != null) {
        ModalBottomSheet(
            onDismissRequest = sessionListViewModel::onActionDismiss,
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
                    onClick = { sessionListViewModel.onPinToggle(actionSession.id) }
                )
                SheetAction(
                    label = "重命名",
                    onClick = sessionListViewModel::onActionRenameStart
                )
                SheetAction(
                    label = "删除",
                    onClick = { sessionListViewModel.onDelete(actionSession.id, state.currentSession.id) }
                )
                HorizontalDivider()
                SheetAction(
                    label = "取消",
                    onClick = sessionListViewModel::onActionDismiss
                )
            }
        }
    }

    if (sessionListState.showRenameDialog && actionSession != null) {
        AlertDialog(
            onDismissRequest = sessionListViewModel::onActionDismiss,
            title = { Text(text = "重命名会话") },
            text = {
                OutlinedTextField(
                    value = sessionListState.renameText,
                    onValueChange = sessionListViewModel::onRenameTextChange,
                    label = { Text(text = "标题") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        sessionListViewModel.onRenameConfirmed(actionSession.id, sessionListState.renameText)
                        sessionListViewModel.onActionDismiss()
                    },
                    enabled = sessionListState.renameText.isNotBlank()
                ) {
                    Text(text = "保存标题")
                }
            },
            dismissButton = {
                TextButton(onClick = sessionListViewModel::onActionDismiss) {
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
    audioState: com.smartsales.feature.chat.audio.AudioUiState,
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
    onDeviceClick: () -> Unit = {}, // Added
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
                    deviceSnapshot = audioState.deviceSnapshot,
                    formatSessionTime = ::formatSessionTime,
                    historyDeviceStatus = { HistoryDeviceStatus(snapshot = it) },
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
                // 14.1 Aurora Background (Base Layer)
                AuroraBackground()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(HomeScreenTestTags.ROOT),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        // TODO(hardware): 后续接入真实设备状态，填充顶栏指示器文案
                        HomeTopBar(
                            title = state.currentSession.title,
                            deviceSnapshot = audioState.deviceSnapshot,
                            onHistoryClick = {
                                onDismissKeyboard()
                                onToggleHistoryPanel()
                                coroutineScope.launch { drawerState.open() }
                            },
                            onNewChatClick = onNewChatClicked,
                            onDeviceClick = onDeviceClick, // Wired
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
                            showQuickSkills = true, // Fix T3: Always show chips
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
                            if (audioState.showAudioRecoveryHint) {
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
                                        // P3.5: Delegated to ConversationScreen
                                        com.smartsales.feature.chat.conversation.ConversationScreen(
                                            messages = state.chatMessages,
                                            isLoadingHistory = state.isLoadingHistory,
                                            showRawAssistantOutput = state.showRawAssistantOutput,
                                            smartReasoningText = state.smartReasoningText,
                                            onCopy = { content ->
                                                clipboardManager.setText(AnnotatedString(content))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("已复制到剪贴板")
                                                }
                                            },
                                            onLoadMoreHistory = onLoadMoreHistory,
                                            listState = listState
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Active Chroma Wave (Listening/Thinking/Error)
                        // Uses Strict Rules: Hidden on Hero, Hidden in Idle
                        val visualState = ChromaWaveRules.mapToVisualState(
                            showWelcomeHero = state.showWelcomeHero,
                            waveState = state.waveState
                        )

                        if (visualState != ChromaWaveVisualState.Hidden) {
                            ChromaWave(
                                state = visualState,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(120.dp) // Floating ribbon height
                                    .padding(bottom = 0.dp)
                            )
                        }

                        if (chatErrorMessage != null) {
                            SnackbarHost(
                                hostState = remember { SnackbarHostState() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 96.dp)
                            ) {
                                Text(
                                    text = chatErrorMessage,
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
                                tingwuTrace = state.tingwuTrace,
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
            .testTag(HomeScreenTestTags.HERO)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo / Branding (Optional enhancement, sticking to brief which implies text focus)
            
            // Text Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Greetings
                Text(
                    text = "你好, $userName",
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = AppColors.ChromaticText // Fix T2: Use Chromatic Gradient Token
                    )
                )

                Text(
                    text = "我是您的销售助手", // Fix T2: Removed "让我们开始吧"
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // NOTE: QuickSkillRow moved to HomeInputArea as per Design Brief
        }
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
    onDeviceClick: () -> Unit, // Added
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
        
        // 15.2 Smart Badge Tap Action
        DeviceStatusIndicator(
            snapshot = deviceSnapshot,
            onClick = onDeviceClick
        )

        // 15.1 Header Title Logic: Show "SmartSales" for new/empty sessions
        val displayTitle = if (title.contains("新的") || title.contains("New Chat") || title.isBlank()) {
            "SmartSales"
        } else {
            title
        }

        Text(
            text = displayTitle,
            modifier = Modifier
                .weight(1f)
                .testTag(HomeScreenTestTags.SESSION_TITLE),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        // 15.1 Remove Debug Dot in Release (or hide by default)
        // Only show if HUD is explicitly enabled AND we are in a debuggable state
        // 15.1 Remove Debug Dot in Release (or hide by default)
        // Only show if HUD is explicitly enabled
        // Fix T6: Show dot ALWAYS if HUD enabled. Grey = Off, Green = On.
        if (hudEnabled) {
             IconButton(
                onClick = onToggleDebugMetadata,
                modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_TOGGLE)
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimensions.DebugDotSize) // Fix: Use AppDimensions directly
                        .background(
                            color = if (showDebugMetadata) AppColors.DebugDotActive else AppColors.DebugDotInactive,
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
private fun DeviceStatusIndicator(
    snapshot: DeviceSnapshotUi?,
    onClick: () -> Unit = {} // Added default for preview/compat
) {
    // 14.1 SmartBadge Implementation
    Surface(
        modifier = Modifier
            .testTag(HomeScreenTestTags.HOME_DEVICE_INDICATOR)
            .clickable(onClick = onClick), // 15.2 Interactive
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
            // Icon: Badge Style (Using DeviceHub as placeholder for now, ideally vector resource)
            // TODO: Replace with dedicated Badge icon if available
            Icon(
                imageVector = Icons.Filled.DeviceHub, 
                contentDescription = "智能工牌",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp) 
            )
            
            Text(
                text = "智能工牌", // Localized "Smart Badge"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Green Pulse Dot (Authentic Animation)
            // 15.2 State Mapping: Verify if connected. For now we assume this badge IMPLIES connection or searching.
            // If we want to show disconnected, we might change color.
            // For now, keep Green Pulse as "Alive" signal per design brief.
            val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(Color(0xFF4CD964), CircleShape) // iOS Green
            )
        }
    }
}

@Composable
private fun HistoryDeviceStatus(snapshot: DeviceSnapshotUi?) {
    val isConnected = snapshot?.connectionState == DeviceConnectionStateUi.CONNECTED
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
                    tint = if (isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isConnected && snapshot?.deviceName != null) {
                        snapshot.deviceName
                    } else {
                        "设备状态"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isConnected) {
                    Text(
                        text = "● 已连接",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = snapshot?.statusText ?: "设备状态将在硬件接入后展示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isConnected) {
                Text(
                    text = "硬件上线后将在此显示连接信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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


