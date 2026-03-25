package com.smartsales.prism.ui.sim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.AgentIntelligenceVisualMode
import com.smartsales.prism.ui.PrismElevation
import com.smartsales.prism.ui.components.ConnectivityManagerScreen
import com.smartsales.prism.ui.components.ConnectivityModal
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.onboarding.SimConnectivityPairingFlow
import com.smartsales.prism.ui.settings.UserCenterScreen
import com.smartsales.prism.ui.theme.BackgroundApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun SimShellContent(
    chatViewModel: SimAgentViewModel,
    dependencies: SimShellDependencies,
    audioViewModel: SimAudioDrawerViewModel,
    connectivityViewModel: ConnectivityViewModel,
    connectivityState: ConnectionState,
    shellState: SimShellState,
    activeFollowUp: SimBadgeFollowUpState?,
    currentSessionId: String?,
    groupedSessions: Map<String, List<SessionPreview>>,
    currentChatAudioId: String?,
    currentSchedulerFollowUpContext: SchedulerFollowUpContext?,
    selectedSchedulerFollowUpTaskId: String?,
    dynamicIslandItems: List<DynamicIslandItem>,
    isImeVisible: Boolean,
    trackedPendingAudioIds: SnapshotStateMap<String, String>,
    coroutineScope: CoroutineScope,
    onImportTestAudio: () -> Unit,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    closeOverlays: () -> Unit,
    openScheduler: (DynamicIslandTapAction) -> Unit,
    openAudioDrawer: (SimAudioDrawerMode) -> Unit,
    mutateShellState: ((SimShellState) -> SimShellState) -> Unit
) {
    val isAudioDrawerOpen = shellState.activeDrawer == SimDrawerType.AUDIO
    val showScrim = shouldShowSimShellScrim(shellState)
    val showSchedulerInteractionShield = shellState.activeDrawer == SimDrawerType.SCHEDULER
    val showSimBottomComposer = shellState.activeDrawer != SimDrawerType.SCHEDULER
    val schedulerGapDismissHeight = SimHomeHeroTokens.BottomMonolithHeight + 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundApp)
    ) {
        AgentIntelligenceScreen(
            viewModel = dependencies.chatViewModel,
            onMenuClick = {
                mutateShellState { state ->
                    handleSimHistoryEntryRequest(
                        state = state,
                        source = "hamburger"
                    )
                }
            },
            onNewSessionClick = {
                handleSimNewSessionAction(
                    activeFollowUp = activeFollowUp,
                    clearFollowUp = clearFollowUp,
                    startNewSession = chatViewModel::startNewSession
                )
                closeOverlays()
            },
            onAudioBadgeClick = {},
            onSchedulerClick = openScheduler,
            onAudioDrawerClick = { openAudioDrawer(SimAudioDrawerMode.BROWSE) },
            onAttachClick = { openAudioDrawer(SimAudioDrawerMode.CHAT_RESELECT) },
            onProfileClick = {
                mutateShellState { state ->
                    state.copy(
                        activeDrawer = null,
                        audioDrawerMode = SimAudioDrawerMode.BROWSE,
                        showHistory = false,
                        activeConnectivitySurface = null,
                        showSettings = true
                    )
                }
            },
            onDebugClick = {},
            showDebugButton = false,
            visualMode = AgentIntelligenceVisualMode.SIM,
            simDynamicIslandItems = dynamicIslandItems,
            showSimBottomComposer = showSimBottomComposer,
            enableSimSchedulerPullGesture = canOpenSimSchedulerFromEdge(shellState),
            enableSimAudioPullGesture = canOpenSimAudioFromEdge(shellState, isImeVisible),
            onSimSchedulerPullOpen = { openScheduler(DynamicIslandTapAction.OpenSchedulerDrawer()) },
            onSimAudioPullOpen = { openAudioDrawer(SimAudioDrawerMode.BROWSE) }
        )

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

        AnimatedVisibility(
            visible = showSchedulerInteractionShield,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(PrismElevation.Scrim)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = schedulerGapDismissHeight)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(schedulerGapDismissHeight)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Black.copy(alpha = 0.26f),
                                    Color.Black.copy(alpha = 0.34f)
                                )
                            )
                        )
                        .clickable {
                            mutateShellState { state -> state.copy(activeDrawer = null) }
                        }
                )
            }
        }

        AnimatedVisibility(
            visible = shellState.showHistory,
            enter = slideInHorizontally(
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                initialOffsetX = { -it }
            ),
            exit = slideOutHorizontally(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                ),
                targetOffsetX = { -it }
            )
        ) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                SimHistoryDrawer(
                    groupedSessions = groupedSessions,
                    currentSessionId = currentSessionId,
                    onSessionClick = { sessionId ->
                        closeOverlays()
                        handleSimSessionSwitchAction(
                            targetSessionId = sessionId,
                            activeFollowUp = activeFollowUp,
                            clearFollowUp = clearFollowUp,
                            switchSession = chatViewModel::switchSession
                        )
                    },
                    onPinSession = chatViewModel::togglePin,
                    onRenameSession = chatViewModel::renameSession,
                    onDeleteSession = { sessionId ->
                        handleSimSessionDeleteAction(
                            targetSessionId = sessionId,
                            activeFollowUp = activeFollowUp,
                            clearFollowUp = clearFollowUp,
                            deleteSession = chatViewModel::deleteSession
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SchedulerDrawer(
                isOpen = shellState.activeDrawer == SimDrawerType.SCHEDULER,
                onDismiss = { mutateShellState { state -> state.copy(activeDrawer = null) } },
                visualMode = SchedulerDrawerVisualMode.SIM,
                onInspirationAskAi = { promptText ->
                    handleSchedulerShelfAskAiHandoff(
                        promptText = promptText,
                        startSession = chatViewModel::startSchedulerShelfSession,
                        closeDrawer = {
                            mutateShellState { state -> state.copy(activeDrawer = null) }
                        }
                    )
                },
                enableInspirationMultiSelect = false,
                viewModel = dependencies.schedulerViewModel
            )
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SimAudioDrawer(
                isOpen = isAudioDrawerOpen,
                onDismiss = {
                    mutateShellState { state ->
                        state.copy(
                            activeDrawer = null,
                            audioDrawerMode = SimAudioDrawerMode.BROWSE
                        )
                    }
                },
                mode = shellState.audioDrawerMode,
                currentChatAudioId = currentChatAudioId,
                showTestImportAction = BuildConfig.DEBUG && shellState.audioDrawerMode == SimAudioDrawerMode.BROWSE,
                showDebugScenarioActions = BuildConfig.DEBUG && shellState.audioDrawerMode == SimAudioDrawerMode.BROWSE,
                viewModel = audioViewModel,
                onOpenConnectivity = {
                    mutateShellState { state ->
                        handleSimConnectivityEntryRequest(
                            state = state.copy(activeDrawer = null),
                            connectionState = connectivityState,
                            source = "audio_drawer"
                        )
                    }
                },
                onSyncFromBadge = { audioViewModel.syncFromBadgeManually() },
                onImportTestAudio = onImportTestAudio,
                onSeedDebugFailureScenario = { audioViewModel.seedDebugFailureScenario() },
                onSeedDebugMissingSectionsScenario = { audioViewModel.seedDebugMissingSectionsScenario() },
                onSeedDebugFallbackScenario = { audioViewModel.seedDebugFallbackScenario() },
                onArtifactOpened = { audioId, title ->
                    emitSimAudioPersistedArtifactOpenedTelemetry(
                        audioId = audioId,
                        title = title
                    )
                },
                onAskAi = { discussion ->
                    emitSimAudioGroundedChatOpenedFromArtifactTelemetry(
                        audioId = discussion.audioId,
                        title = discussion.title
                    )
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
                        mutateShellState { state ->
                            state.copy(
                                activeDrawer = null,
                                audioDrawerMode = SimAudioDrawerMode.BROWSE
                            )
                        }
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
                        mutateShellState { state ->
                            state.copy(
                                activeDrawer = null,
                                audioDrawerMode = SimAudioDrawerMode.BROWSE
                            )
                        }

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

        if (shellState.activeConnectivitySurface == SimConnectivitySurface.MODAL) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                ConnectivityModal(
                    onDismiss = { mutateShellState(::closeSimConnectivitySurface) },
                    onNavigateToSetup = {
                        mutateShellState { state ->
                            handleSimConnectivitySetupStart(
                                state = state,
                                source = "bootstrap_modal"
                            )
                        }
                    },
                    viewModel = connectivityViewModel
                )
            }
        }

        AnimatedVisibility(
            visible = shellState.activeConnectivitySurface == SimConnectivitySurface.SETUP,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            SimConnectivityPairingFlow(
                onExit = { mutateShellState(::closeSimConnectivitySurface) },
                onCompleted = { mutateShellState(::handleSimConnectivitySetupCompleted) }
            )
        }

        AnimatedVisibility(
            visible = shellState.activeConnectivitySurface == SimConnectivitySurface.MANAGER,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            ConnectivityManagerScreen(
                onClose = { mutateShellState(::closeSimConnectivitySurface) },
                onNavigateToSetup = {
                    mutateShellState { state ->
                        handleSimConnectivitySetupStart(
                            state = state,
                            source = "manager_needs_setup"
                        )
                    }
                },
                viewModel = connectivityViewModel
            )
        }

        AnimatedVisibility(
            visible = shellState.showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            UserCenterScreen(
                onClose = { mutateShellState { state -> state.copy(showSettings = false) } }
            )
        }

        val showFollowUpPrompt = activeFollowUp != null &&
            currentSessionId != activeFollowUp.boundSessionId &&
            shellState.activeDrawer == null &&
            !shellState.showHistory &&
            shellState.activeConnectivitySurface == null &&
            !shellState.showSettings

        if (showFollowUpPrompt) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 104.dp)
                    .zIndex(PrismElevation.Drawer + 2f)
            ) {
                SimSchedulerFollowUpPrompt(
                    onOpen = {
                        val followUpSessionId = activeFollowUp?.boundSessionId
                        if (followUpSessionId != null) {
                            chatViewModel.switchSession(followUpSessionId)
                        }
                    }
                )
            }
        }

        if (currentSchedulerFollowUpContext != null &&
            shellState.activeDrawer == null &&
            !shellState.showHistory &&
            shellState.activeConnectivitySurface == null &&
            !shellState.showSettings
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 108.dp)
                    .zIndex(PrismElevation.Drawer + 2f)
            ) {
                SimSchedulerFollowUpActionStrip(
                    context = currentSchedulerFollowUpContext,
                    selectedTaskId = selectedSchedulerFollowUpTaskId,
                    onSelectTask = chatViewModel::selectSchedulerFollowUpTask,
                    onAction = chatViewModel::performSchedulerFollowUpQuickAction
                )
            }
        }
    }
}
