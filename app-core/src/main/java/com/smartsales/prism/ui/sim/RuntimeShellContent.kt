package com.smartsales.prism.ui.sim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.AgentIntelligenceVisualMode
import com.smartsales.prism.ui.PrismElevation
import com.smartsales.prism.ui.components.ConnectivityManagerScreen
import com.smartsales.prism.ui.components.ConnectivityModal
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.onboarding.OnboardingCoordinator
import com.smartsales.prism.ui.onboarding.OnboardingExitPolicy
import com.smartsales.prism.ui.onboarding.OnboardingHost
import com.smartsales.prism.ui.onboarding.PairingFlowViewModel
import com.smartsales.prism.ui.theme.PrismThemeDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun RuntimeShellContent(
    chatViewModel: SimAgentViewModel,
    dependencies: SimShellDependencies,
    audioViewModel: SimAudioDrawerViewModel,
    pairingViewModel: PairingFlowViewModel,
    connectivityViewModel: ConnectivityViewModel,
    connectivityState: ConnectionState,
    shellState: RuntimeShellState,
    activeFollowUp: SimBadgeFollowUpState?,
    currentSessionId: String?,
    groupedSessions: Map<String, List<SessionPreview>>,
    currentChatAudioId: String?,
    currentSchedulerFollowUpContext: SchedulerFollowUpContext?,
    selectedSchedulerFollowUpTaskId: String?,
    dynamicIslandState: DynamicIslandUiState,
    isImeVisible: Boolean,
    showRuntimeIdleComposerHint: Boolean,
    trackedPendingAudioIds: SnapshotStateMap<String, String>,
    coroutineScope: CoroutineScope,
    onImportTestAudio: () -> Unit,
    onForcedFirstLaunchOnboardingCompleted: () -> Unit,
    onReplayOnboarding: () -> Unit,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    closeOverlays: () -> Unit,
    openScheduler: (DynamicIslandTapAction) -> Unit,
    openAudioDrawer: (RuntimeAudioDrawerMode) -> Unit,
    mutateShellState: ((RuntimeShellState) -> RuntimeShellState) -> Unit
) {
    val prismColors = PrismThemeDefaults.colors
    val isAudioDrawerOpen = shellState.activeDrawer == RuntimeDrawerType.AUDIO
    val showScrim = shouldShowRuntimeShellScrim(shellState)
    val scrimAlpha = resolveRuntimeShellScrimAlpha(shellState)
    val settingsDrawerSlideSpec = tween<IntOffset>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )
    val settingsDrawerFadeSpec = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )
    val showSchedulerInteractionShield = shellState.activeDrawer == RuntimeDrawerType.SCHEDULER
    val showSimHeaderMenuButton = shellState.activeDrawer != RuntimeDrawerType.SCHEDULER
    val showSimHeaderNewSessionButton = shellState.activeDrawer != RuntimeDrawerType.SCHEDULER
    val showSimBottomComposer = shellState.activeDrawer != RuntimeDrawerType.SCHEDULER
    val schedulerGapDismissHeight = SimHomeHeroTokens.BottomMonolithHeight + 16.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(prismColors.appBackground)
    ) {
        AgentIntelligenceScreen(
            viewModel = dependencies.chatViewModel,
            onMenuClick = {
                mutateShellState { state ->
                    handleRuntimeHistoryEntryRequest(
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
            onAudioDrawerClick = { openAudioDrawer(RuntimeAudioDrawerMode.BROWSE) },
            onAttachClick = { openAudioDrawer(RuntimeAudioDrawerMode.CHAT_RESELECT) },
            onProfileClick = {
                mutateShellState(::openRuntimeSettings)
            },
            onDebugClick = {},
            showDebugButton = false,
            visualMode = AgentIntelligenceVisualMode.SIM,
            simDynamicIslandState = dynamicIslandState,
            showSimHeaderMenuButton = showSimHeaderMenuButton,
            showSimHeaderNewSessionButton = showSimHeaderNewSessionButton,
            showSimBottomComposer = showSimBottomComposer,
            showSimIdleComposerHint = showRuntimeIdleComposerHint,
            enableSimSchedulerPullGesture = canOpenSimSchedulerFromEdge(shellState),
            enableSimAudioPullGesture = canOpenSimAudioFromEdge(shellState, isImeVisible),
            onSimSchedulerPullOpen = { openScheduler(DynamicIslandTapAction.OpenSchedulerDrawer()) },
            onSimAudioPullOpen = { openAudioDrawer(RuntimeAudioDrawerMode.BROWSE) }
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
                    .background(prismColors.backdropScrim.copy(alpha = scrimAlpha))
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
                                    prismColors.backdropScrim.copy(alpha = 0.08f),
                                    prismColors.backdropScrim.copy(alpha = 0.14f),
                                    prismColors.backdropScrim.copy(alpha = 0.22f),
                                    prismColors.backdropScrim.copy(alpha = 0.30f)
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
                    displayName = chatViewModel.currentDisplayName,
                    subscriptionTier = chatViewModel.currentSubscriptionTier,
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
                    onOpenSettings = {
                        mutateShellState(::openRuntimeSettings)
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
        }

        Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
            SchedulerDrawer(
                isOpen = shellState.activeDrawer == RuntimeDrawerType.SCHEDULER,
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
                            audioDrawerMode = RuntimeAudioDrawerMode.BROWSE
                        )
                    }
                },
                mode = shellState.audioDrawerMode,
                currentChatAudioId = currentChatAudioId,
                showTestImportAction = BuildConfig.DEBUG && shellState.audioDrawerMode == RuntimeAudioDrawerMode.BROWSE,
                showDebugScenarioActions = BuildConfig.DEBUG && shellState.audioDrawerMode == RuntimeAudioDrawerMode.BROWSE,
                viewModel = audioViewModel,
                onOpenConnectivity = {
                    mutateShellState { state ->
                        handleRuntimeConnectivityEntryRequest(
                            state = state.copy(activeDrawer = null),
                            connectionState = connectivityState,
                            source = "audio_drawer"
                        )
                    }
                },
                onSyncFromBadge = { audioViewModel.syncFromBadgeManually() },
                onImportTestAudio = onImportTestAudio,
                onReplayOnboarding = onReplayOnboarding,
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
                        mutateShellState { state ->
                            state.copy(
                                activeDrawer = null,
                                audioDrawerMode = RuntimeAudioDrawerMode.BROWSE
                            )
                        }
                        trackedPendingAudioIds.remove(discussion.audioId)
                    }
                },
                onSelectForChat = { selection ->
                    val isLocalReady = selection.localAvailability == AudioLocalAvailability.READY
                    if (selection.status == AudioStatus.PENDING && !isLocalReady) {
                        return@SimAudioDrawer onSelectForChat
                    }
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
                                audioDrawerMode = RuntimeAudioDrawerMode.BROWSE
                            )
                        }

                        if (!entersPendingFlow) {
                            trackedPendingAudioIds.remove(selection.audioId)
                        } else {
                            trackedPendingAudioIds[selection.audioId] = sessionId
                            if (selection.status == AudioStatus.PENDING && isLocalReady) {
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
                },
                onDeleteAudio = { audioId ->
                    audioViewModel.deleteAudio(audioId)
                }
            )
        }

        if (shellState.activeConnectivitySurface == RuntimeConnectivitySurface.MODAL) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                ConnectivityModal(
                    onDismiss = { mutateShellState(::closeRuntimeConnectivitySurface) },
                    onNavigateToSetup = {
                        mutateShellState { state ->
                            handleRuntimeConnectivitySetupStart(
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
            visible = shellState.activeConnectivitySurface == RuntimeConnectivitySurface.SETUP,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            OnboardingCoordinator(
                host = OnboardingHost.SIM_CONNECTIVITY,
                onExit = {
                    val wasForcedFirstLaunch = shellState.isForcedFirstLaunchOnboarding
                    mutateShellState { state ->
                        handleRuntimeConnectivitySetupSkipped(
                            state = state,
                            source = if (wasForcedFirstLaunch) {
                                "first_launch_skip"
                            } else {
                                "manual_replay_skip"
                            }
                        )
                    }
                    if (wasForcedFirstLaunch) {
                        onForcedFirstLaunchOnboardingCompleted()
                    }
                },
                onComplete = {
                    val wasForcedFirstLaunch = shellState.isForcedFirstLaunchOnboarding
                    mutateShellState(::handleRuntimeConnectivitySetupCompleted)
                    if (wasForcedFirstLaunch) {
                        onForcedFirstLaunchOnboardingCompleted()
                    }
                },
                exitPolicy = if (shellState.isForcedFirstLaunchOnboarding) {
                    OnboardingExitPolicy.EXPLICIT_ACTION_ONLY
                } else {
                    OnboardingExitPolicy.ALLOW_EXIT
                },
                pairingViewModel = pairingViewModel
            )
        }

        AnimatedVisibility(
            visible = shellState.activeConnectivitySurface == RuntimeConnectivitySurface.MANAGER,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            ConnectivityManagerScreen(
                onClose = { mutateShellState(::closeRuntimeConnectivitySurface) },
                onNavigateToSetup = {
                    mutateShellState { state ->
                        handleRuntimeConnectivitySetupStart(
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
            enter = slideInHorizontally(
                animationSpec = settingsDrawerSlideSpec,
                initialOffsetX = { it }
            ) + fadeIn(animationSpec = settingsDrawerFadeSpec),
            exit = slideOutHorizontally(
                animationSpec = settingsDrawerSlideSpec,
                targetOffsetX = { it }
            ) + fadeOut(animationSpec = settingsDrawerFadeSpec),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            SimUserCenterDrawer(
                onClose = { mutateShellState(::closeRuntimeSettings) }
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
