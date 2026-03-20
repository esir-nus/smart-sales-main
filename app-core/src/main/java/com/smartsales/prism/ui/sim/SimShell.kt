package com.smartsales.prism.ui.sim

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.PrismElevation
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.settings.UserCenterScreen
import com.smartsales.prism.ui.theme.BackgroundApp
import kotlinx.coroutines.launch

internal fun emitSchedulerShelfHandoffTelemetry(
    promptText: String,
    log: (String) -> Unit = { message -> Log.d("SimSchedulerShelf", message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = promptText.length,
        summary = SIM_SCHEDULER_SHELF_HANDOFF_REQUEST_SUMMARY,
        rawDataDump = promptText
    )
    log("scheduler shelf handoff requested: $promptText")
}

internal fun handleSchedulerShelfAskAiHandoff(
    promptText: String,
    startSession: (String) -> Unit,
    closeDrawer: () -> Unit,
    emitTelemetry: (String) -> Unit = { prompt -> emitSchedulerShelfHandoffTelemetry(prompt) }
) {
    if (promptText.isBlank()) return
    emitTelemetry(promptText)
    startSession(promptText)
    closeDrawer()
}

@Composable
fun SimShell() {
    val chatViewModel: SimAgentViewModel = viewModel()
    val schedulerViewModel: SimSchedulerViewModel = viewModel()
    val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    val dependencies = remember(chatViewModel, schedulerViewModel, audioViewModel) {
        DefaultSimShellDependencies(
            chatViewModel = chatViewModel,
            schedulerViewModel = schedulerViewModel,
            audioViewModel = audioViewModel
        )
    }
    var shellState by remember { mutableStateOf(SimShellState()) }
    val groupedSessions by chatViewModel.groupedSessions.collectAsStateWithLifecycle()
    val currentChatAudioId by chatViewModel.currentLinkedAudioId.collectAsStateWithLifecycle()
    val audioEntries by audioViewModel.entries.collectAsStateWithLifecycle()
    val trackedPendingAudioIds = remember { mutableStateMapOf<String, String>() }
    val importTestAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { audioViewModel.importTestAudio(it.toString()) }
    }

    LaunchedEffect(audioEntries, trackedPendingAudioIds.keys.toSet()) {
        val completedAudioIds = mutableListOf<String>()
        trackedPendingAudioIds.forEach { (audioId, _) ->
            val entry = audioEntries.firstOrNull { it.item.id == audioId } ?: return@forEach
            when (entry.item.status) {
                AudioStatus.PENDING -> {
                    if (entry.failureMessage != null) {
                        chatViewModel.failPendingAudio(audioId, entry.failureMessage)
                        completedAudioIds += audioId
                    } else {
                        chatViewModel.updatePendingAudioState(
                            audioId = audioId,
                            status = com.smartsales.prism.domain.audio.TranscriptionStatus.PENDING,
                            progress = 0f
                        )
                    }
                }

                AudioStatus.TRANSCRIBING -> chatViewModel.updatePendingAudioState(
                    audioId = audioId,
                    status = com.smartsales.prism.domain.audio.TranscriptionStatus.TRANSCRIBING,
                    progress = entry.item.progress ?: 0f
                )

                AudioStatus.TRANSCRIBED -> {
                    val artifacts = audioViewModel.getArtifacts(audioId)
                    if (artifacts != null) {
                        chatViewModel.appendCompletedAudioArtifacts(audioId, artifacts)
                    } else {
                        chatViewModel.completePendingAudio(audioId)
                    }
                    completedAudioIds += audioId
                }
            }
        }
        completedAudioIds.forEach(trackedPendingAudioIds::remove)
    }

    fun closeOverlays() {
        shellState = shellState.copy(
            activeDrawer = null,
            audioDrawerMode = SimAudioDrawerMode.BROWSE,
            showHistory = false,
            showConnectivity = false,
            showSettings = false
        )
    }

    fun openScheduler() {
        shellState = shellState.copy(
            activeDrawer = SimDrawerType.SCHEDULER,
            showHistory = false,
            showConnectivity = false,
            showSettings = false
        )
    }

    fun openAudioDrawer(mode: SimAudioDrawerMode) {
        shellState = shellState.copy(
            activeDrawer = SimDrawerType.AUDIO,
            audioDrawerMode = mode,
            showHistory = false,
            showConnectivity = false,
            showSettings = false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundApp)
    ) {
        AgentIntelligenceScreen(
            viewModel = dependencies.chatViewModel,
            onMenuClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    showHistory = true,
                    showConnectivity = false,
                    showSettings = false
                )
            },
            onNewSessionClick = {
                chatViewModel.startNewSession()
                closeOverlays()
            },
            onAudioBadgeClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    audioDrawerMode = SimAudioDrawerMode.BROWSE,
                    showHistory = false,
                    showConnectivity = true,
                    showSettings = false
                )
            },
            onAudioDrawerClick = { openAudioDrawer(SimAudioDrawerMode.BROWSE) },
            onAttachClick = { openAudioDrawer(SimAudioDrawerMode.CHAT_RESELECT) },
            onProfileClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    audioDrawerMode = SimAudioDrawerMode.BROWSE,
                    showHistory = false,
                    showConnectivity = false,
                    showSettings = true
                )
            },
            onDebugClick = {},
            showDebugButton = false
        )

        val showScrim = shellState.activeDrawer != null ||
            shellState.showHistory ||
            shellState.showConnectivity

        AnimatedVisibility(
            visible = showScrim,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(PrismElevation.Scrim)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { closeOverlays() }
            )
        }

        if (shellState.showHistory) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                HistoryDrawer(
                    groupedSessions = groupedSessions,
                    displayName = chatViewModel.currentDisplayName,
                    onSessionClick = { sessionId ->
                        closeOverlays()
                        chatViewModel.switchSession(sessionId)
                    },
                    onDeviceClick = {
                        shellState = shellState.copy(
                            showHistory = false,
                            showConnectivity = true
                        )
                    },
                    onSettingsClick = {
                        closeOverlays()
                        shellState = shellState.copy(showSettings = true)
                    },
                    onProfileClick = {
                        closeOverlays()
                        shellState = shellState.copy(showSettings = true)
                    },
                    onPinSession = chatViewModel::togglePin,
                    onRenameSession = chatViewModel::renameSession,
                    onDeleteSession = chatViewModel::deleteSession
                )
            }
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SchedulerDrawer(
                isOpen = shellState.activeDrawer == SimDrawerType.SCHEDULER,
                onDismiss = { shellState = shellState.copy(activeDrawer = null) },
                onInspirationAskAi = { promptText ->
                    handleSchedulerShelfAskAiHandoff(
                        promptText = promptText,
                        startSession = chatViewModel::startSchedulerShelfSession,
                        closeDrawer = { shellState = shellState.copy(activeDrawer = null) }
                    )
                },
                enableInspirationMultiSelect = false,
                viewModel = dependencies.schedulerViewModel
            )
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SimAudioDrawer(
                isOpen = shellState.activeDrawer == SimDrawerType.AUDIO,
                onDismiss = { shellState = shellState.copy(activeDrawer = null, audioDrawerMode = SimAudioDrawerMode.BROWSE) },
                mode = shellState.audioDrawerMode,
                currentChatAudioId = currentChatAudioId,
                showTestImportAction = BuildConfig.DEBUG,
                showDebugScenarioActions = BuildConfig.DEBUG && shellState.audioDrawerMode == SimAudioDrawerMode.BROWSE,
                viewModel = dependencies.audioViewModel,
                onImportTestAudio = { importTestAudioLauncher.launch("audio/*") },
                onSeedDebugFailureScenario = { audioViewModel.seedDebugFailureScenario() },
                onSeedDebugMissingSectionsScenario = { audioViewModel.seedDebugMissingSectionsScenario() },
                onSeedDebugFallbackScenario = { audioViewModel.seedDebugFallbackScenario() },
                onAskAi = { discussion ->
                    coroutineScope.launch {
                        val sessionId = chatViewModel.selectAudioForChat(
                            audioId = discussion.audioId,
                            title = discussion.title,
                            summary = discussion.summary,
                            entersPendingFlow = false
                        )
                        audioViewModel.bindDiscussion(discussion.audioId, sessionId)
                        audioViewModel.getArtifacts(discussion.audioId)?.let { artifacts ->
                            chatViewModel.appendCompletedAudioArtifacts(discussion.audioId, artifacts)
                        }
                        shellState = shellState.copy(
                            activeDrawer = null,
                            audioDrawerMode = SimAudioDrawerMode.BROWSE
                        )
                        trackedPendingAudioIds.remove(discussion.audioId)
                    }
                },
                onSelectForChat = { selection ->
                    val entersPendingFlow = selection.status != AudioStatus.TRANSCRIBED
                    coroutineScope.launch {
                        val sessionId = chatViewModel.selectAudioForChat(
                            audioId = selection.audioId,
                            title = selection.title,
                            summary = selection.summary,
                            entersPendingFlow = entersPendingFlow
                        )
                        audioViewModel.bindDiscussion(selection.audioId, sessionId)
                        shellState = shellState.copy(
                            activeDrawer = null,
                            audioDrawerMode = SimAudioDrawerMode.BROWSE
                        )

                        if (!entersPendingFlow) {
                            trackedPendingAudioIds.remove(selection.audioId)
                            audioViewModel.getArtifacts(selection.audioId)?.let { artifacts ->
                                chatViewModel.appendCompletedAudioArtifacts(selection.audioId, artifacts)
                            }
                        } else {
                            trackedPendingAudioIds[selection.audioId] = sessionId
                            if (selection.status == AudioStatus.PENDING) {
                                runCatching {
                                    audioViewModel.startTranscriptionForChat(selection.audioId)
                                }.onFailure { error ->
                                    trackedPendingAudioIds.remove(selection.audioId)
                                    chatViewModel.failPendingAudio(
                                        audioId = selection.audioId,
                                        message = error.message ?: "转写失败，请稍后重试。"
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }

        if (shellState.showConnectivity) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                com.smartsales.prism.ui.components.ConnectivityModal(
                    onDismiss = { shellState = shellState.copy(showConnectivity = false) },
                    onNavigateToSetup = {
                        shellState = shellState.copy(showConnectivity = false)
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = shellState.showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            UserCenterScreen(
                onClose = { shellState = shellState.copy(showSettings = false) }
            )
        }

        if (!showScrim && !shellState.showSettings) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(84.dp)
                    .zIndex(PrismElevation.Handles)
                    .pointerInput(Unit) {
                        var accumulatedDrag = 0f
                        detectVerticalDragGestures(
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f }
                        ) { _, dragAmount ->
                            if (dragAmount > 0) {
                                accumulatedDrag += dragAmount
                                if (accumulatedDrag > 60f) {
                                    openScheduler()
                                    accumulatedDrag = 0f
                                }
                            }
                        }
                    }
            )
        }
    }
}
