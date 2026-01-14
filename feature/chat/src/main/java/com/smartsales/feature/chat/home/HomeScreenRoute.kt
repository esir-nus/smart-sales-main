package com.smartsales.feature.chat.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.feature.chat.export.ExportEvent
import com.smartsales.feature.chat.sessionlist.SessionListEvent
import kotlinx.coroutines.launch

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
                is ExportEvent.ExportCompleted -> {
                    val message = when (event.result) {
                        is Result.Success -> "导出成功"
                        is Result.Error -> event.result.throwable.message ?: "导出失败"
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
                is SessionListEvent.SwitchToSession -> {
                    viewModel.setSession(event.id, allowHero = true)
                    showHistoryPanel = false
                }
                is SessionListEvent.TitleRenamed -> {
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
        onExportPdfClicked = {
            val markdown = viewModel.getExportMarkdown()
            exportViewModel.performExport(
                sessionId = state.currentSession.id,
                format = ExportFormat.PDF,
                userName = state.userName,
                markdown = markdown
            )
        },
        onExportCsvClicked = {
            val markdown = viewModel.getExportMarkdown()
            exportViewModel.performExport(
                sessionId = state.currentSession.id,
                format = ExportFormat.CSV,
                userName = state.userName,
                markdown = markdown
            )
        },
        exportGateState = exportState.gateState,
    onLoadMoreHistory = viewModel::onLoadMoreHistory,
    onProfileClicked = viewModel::onTapProfile,
    onNewChatClicked = viewModel::onNewChatClicked,
    onDeviceClick = onNavigateToDeviceManager,
    chatErrorMessage = state.chatErrorMessage,
    onPickAudioFile = { uri ->
        coroutineScope.launch {
            val result = audioViewModel.onAudioFilePicked(uri, state.currentSession.id)
            if (result is Result.Success) {
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
        onToggleDebugMetadata = viewModel::toggleDebugMetadata,
            onRefreshTrace = viewModel::refreshXfyunTrace,
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
