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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.smartsales.feature.chat.home.components.HomeTopBar
import com.smartsales.feature.chat.home.components.HeroSection
import com.smartsales.feature.chat.home.components.AudioRecoveryBanner
import com.smartsales.feature.chat.home.components.ScrollToLatestButton
import com.smartsales.feature.chat.home.history.HistoryDeviceCard

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt
// 模块：:feature:chat
// 说明：Home 层 Compose UI，仅渲染聊天欢迎区、消息列表、快捷技能与输入框
// 作者：创建于 2025-11-20





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
    onLoadMoreHistory: () -> Unit,
    onProfileClicked: () -> Unit,
    onNewChatClicked: () -> Unit = {},
    onDeviceClick: () -> Unit = {},
    chatErrorMessage: String? = null,
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
                    historyDeviceStatus = { HistoryDeviceCard(snapshot = it) },
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
                    containerColor = Color.Transparent, // T1: Transparent to show Aurora
                    contentColor = MaterialTheme.colorScheme.onSurface,
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
                                AudioRecoveryBanner(
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
                                        (fadeIn() + slideInVertically { it / 8 }) togetherWith
                                            (fadeOut() + slideOutVertically { -it / 8 })
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    label = "feature_home_empty_transition"
                                ) { empty ->
                                    if (empty) {
                                        HeroSection(
                                            userName = state.userName,
                                            modifier = Modifier.padding(horizontal = 16.dp)
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

private fun formatSessionTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}





