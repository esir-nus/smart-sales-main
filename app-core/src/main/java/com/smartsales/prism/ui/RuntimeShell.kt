package com.smartsales.prism.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandLane
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.DynamicIslandVisualState
import com.smartsales.prism.ui.components.toDayOffset
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.sim.DefaultSimShellDependencies
import com.smartsales.prism.ui.sim.RuntimeAudioDrawerMode
import com.smartsales.prism.ui.sim.RuntimeShellContent
import com.smartsales.prism.ui.sim.SimAgentViewModel
import com.smartsales.prism.ui.sim.SimAudioDrawerViewModel
import com.smartsales.prism.ui.sim.SimBadgeFollowUpOwner
import com.smartsales.prism.ui.sim.SimDebugFollowUpScenario
import com.smartsales.prism.ui.sim.SimSchedulerViewModel
import com.smartsales.prism.ui.sim.SimShellDynamicIslandCoordinator
import com.smartsales.prism.ui.sim.buildSimDebugFollowUpEvent
import com.smartsales.prism.ui.sim.buildSimDynamicIslandItems
import com.smartsales.prism.ui.sim.closeRuntimeOverlays
import com.smartsales.prism.ui.sim.dismissRuntimeSchedulerIslandHint
import com.smartsales.prism.ui.sim.deriveRuntimeFollowUpSurface
import com.smartsales.prism.ui.sim.handleBadgeSchedulerContinuityIngress
import com.smartsales.prism.ui.sim.handleRuntimeConnectivityEntryRequest
import com.smartsales.prism.ui.sim.initialRuntimeShellState
import com.smartsales.prism.ui.sim.openRuntimeAudioDrawer
import com.smartsales.prism.ui.sim.openRuntimeConnectivityModal
import com.smartsales.prism.ui.sim.openRuntimeScheduler
import com.smartsales.prism.ui.sim.rememberSimImeVisibility
import com.smartsales.prism.ui.sim.shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff
import com.smartsales.prism.ui.sim.shouldAutoOpenRuntimeSchedulerStartupTeaser
import com.smartsales.prism.ui.sim.shouldShowRuntimeIdleComposerHint
import com.smartsales.prism.ui.sim.startRuntimeForcedFirstLaunchOnboarding
import com.smartsales.prism.ui.onboarding.PairingFlowViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun RuntimeShell(
    badgeAudioPipeline: BadgeAudioPipeline,
    debugFollowUpScenario: SimDebugFollowUpScenario? = null,
    forceSetupOnLaunch: Boolean = false,
    onForcedSetupCompleted: () -> Unit = {},
    shouldShowFirstLaunchSchedulerTeaser: Boolean = false,
    onFirstLaunchSchedulerTeaserShown: () -> Unit = {},
    shouldAutoOpenSchedulerAfterOnboarding: Boolean = false,
    onPostOnboardingSchedulerAutoOpened: () -> Unit = {}
) {
    val schedulerEnabled = true
    val chatViewModel: SimAgentViewModel = hiltViewModel()
    val schedulerViewModel: SimSchedulerViewModel = viewModel()
    val followUpOwner: SimBadgeFollowUpOwner = viewModel()
    val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()
    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val pairingViewModel: PairingFlowViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    val dependencies = remember(chatViewModel, schedulerViewModel, audioViewModel) {
        DefaultSimShellDependencies(
            chatViewModel = chatViewModel,
            schedulerViewModel = schedulerViewModel,
            audioViewModel = audioViewModel
        )
    }
    var shellState by remember { mutableStateOf(initialRuntimeShellState(forceSetupOnLaunch)) }
    val connectivityState by connectivityViewModel.connectionState.collectAsStateWithLifecycle()
    val activeFollowUp by followUpOwner.activeFollowUp.collectAsStateWithLifecycle()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val sessionTitle by chatViewModel.sessionTitle.collectAsStateWithLifecycle()
    val currentSessionHasAudioContextHistory by
        chatViewModel.currentSessionHasAudioContextHistory.collectAsStateWithLifecycle()
    val activeDevice by connectivityViewModel.activeDevice.collectAsStateWithLifecycle()
    val rawTopUrgentTasks by schedulerViewModel.topUrgentTasks.collectAsStateWithLifecycle()
    val rawActiveReminderBanner by schedulerViewModel.activeReminderBanner.collectAsStateWithLifecycle()
    val rawCurrentSchedulerFollowUpContext by chatViewModel.currentSchedulerFollowUpContext.collectAsStateWithLifecycle()
    val rawSelectedSchedulerFollowUpTaskId by chatViewModel.selectedSchedulerFollowUpTaskId.collectAsStateWithLifecycle()
    val groupedSessions by chatViewModel.groupedSessions.collectAsStateWithLifecycle()
    val currentChatAudioId by chatViewModel.currentLinkedAudioId.collectAsStateWithLifecycle()
    val transcriptRevealState by chatViewModel.artifactTranscriptRevealState.collectAsStateWithLifecycle()
    val voiceDraftState by chatViewModel.voiceDraftState.collectAsStateWithLifecycle()
    val audioEntries by audioViewModel.entries.collectAsStateWithLifecycle()
    val topUrgentTasks = if (schedulerEnabled) rawTopUrgentTasks else emptyList()
    val activeReminderBanner = if (schedulerEnabled) rawActiveReminderBanner else null
    val currentSchedulerFollowUpContext = if (schedulerEnabled) {
        rawCurrentSchedulerFollowUpContext
    } else {
        null
    }
    val selectedSchedulerFollowUpTaskId = if (schedulerEnabled) {
        rawSelectedSchedulerFollowUpTaskId
    } else {
        null
    }
    val trackedPendingAudioIds = remember { mutableStateMapOf<String, String>() }
    val isImeVisible = rememberSimImeVisibility()
    var startupSchedulerTeaserPending by remember {
        mutableStateOf(schedulerEnabled && shouldShowFirstLaunchSchedulerTeaser)
    }
    var postOnboardingSchedulerAutoOpenPending by remember {
        mutableStateOf(schedulerEnabled && shouldAutoOpenSchedulerAfterOnboarding)
    }
    val schedulerIslandItems = remember(
        schedulerEnabled,
        sessionTitle,
        currentSessionHasAudioContextHistory,
        topUrgentTasks,
        shellState.showSchedulerIslandHint
    ) {
        buildSimDynamicIslandItems(
            sessionTitle = sessionTitle,
            sessionHasAudioContextHistory = currentSessionHasAudioContextHistory,
            orderedTasks = topUrgentTasks,
            showIdleTeachingHint = shellState.showSchedulerIslandHint
        )
    }
    val schedulerIslandItemsFlow = remember {
        MutableStateFlow(emptyList<DynamicIslandItem>())
    }
    val islandTakeoverSuppressedFlow = remember {
        MutableStateFlow(false)
    }
    val activeDeviceNameFlow = remember {
        MutableStateFlow<String?>(null)
    }
    val dynamicIslandCoordinator = remember(coroutineScope, connectivityViewModel) {
        SimShellDynamicIslandCoordinator(
            parentScope = coroutineScope,
            schedulerItems = schedulerIslandItemsFlow,
            connectivityState = connectivityViewModel.connectionState,
            batteryLevel = connectivityViewModel.batteryLevel,
            takeoverSuppressed = islandTakeoverSuppressedFlow,
            activeDeviceName = activeDeviceNameFlow
        )
    }
    DisposableEffect(dynamicIslandCoordinator) {
        onDispose {
            dynamicIslandCoordinator.close()
        }
    }
    val dynamicIslandPresentation by dynamicIslandCoordinator.presentation.collectAsStateWithLifecycle()
    val importTestAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { audioViewModel.importTestAudio(it.toString()) }
    }

    LaunchedEffect(schedulerIslandItems) {
        schedulerIslandItemsFlow.value = schedulerIslandItems
    }

    LaunchedEffect(activeDevice) {
        activeDeviceNameFlow.value = activeDevice?.displayName
    }

    LaunchedEffect(shellState.activeDrawer, shellState.activeConnectivitySurface) {
        islandTakeoverSuppressedFlow.value =
            shellState.activeDrawer == com.smartsales.prism.ui.sim.RuntimeDrawerType.SCHEDULER ||
                shellState.activeConnectivitySurface != null
    }

    // 自动重连：仅在有效 UI 状态确认为 DISCONNECTED 时调度退避重连，
    // 避免 WIFI_MISMATCH / 手动修复流程被后台重连覆盖。
    LaunchedEffect(connectivityViewModel) {
        connectivityViewModel.effectiveState.collect { state ->
            if (state == com.smartsales.prism.ui.components.connectivity.ConnectionState.DISCONNECTED) {
                connectivityViewModel.scheduleAutoReconnect()
            }
        }
    }

    LaunchedEffect(connectivityViewModel) {
        connectivityViewModel.promptRequests.collectLatest {
            shellState = openRuntimeConnectivityModal(shellState)
        }
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

    LaunchedEffect(audioViewModel) {
        audioViewModel.deletedAudioIds.collectLatest { audioId ->
            chatViewModel.handleDeletedAudio(audioId)
        }
    }

    LaunchedEffect(badgeAudioPipeline) {
        if (!schedulerEnabled) return@LaunchedEffect
        badgeAudioPipeline.events.collectLatest { event ->
            handleBadgeSchedulerContinuityIngress(
                event = event,
                startSession = { seed ->
                    chatViewModel.createBadgeSchedulerFollowUpSession(
                        threadId = seed.threadId,
                        transcript = seed.transcript,
                        tasks = seed.tasks,
                        batchId = seed.batchId
                    )
                },
                startOwner = { boundSessionId, threadId ->
                    followUpOwner.startBadgeSchedulerFollowUp(
                        boundSessionId = boundSessionId,
                        threadId = threadId
                    )
                }
            )
        }
    }

    LaunchedEffect(debugFollowUpScenario) {
        if (!schedulerEnabled) return@LaunchedEffect
        val scenario = debugFollowUpScenario ?: return@LaunchedEffect
        val seed = com.smartsales.prism.ui.sim.extractBadgeSchedulerContinuitySeed(
            buildSimDebugFollowUpEvent(scenario)
        ) ?: return@LaunchedEffect
        com.smartsales.prism.ui.sim.emitBadgeSchedulerContinuityIngressTelemetry(seed.transcript)
        val sessionId = chatViewModel.createDebugBadgeSchedulerFollowUpSession(
            threadId = seed.threadId,
            transcript = seed.transcript,
            tasks = seed.tasks,
            batchId = seed.batchId
        ) ?: return@LaunchedEffect
        chatViewModel.switchSession(sessionId)
        followUpOwner.startBadgeSchedulerFollowUp(
            boundSessionId = sessionId,
            threadId = seed.threadId
        )
    }

    LaunchedEffect(shouldShowFirstLaunchSchedulerTeaser) {
        if (schedulerEnabled && shouldShowFirstLaunchSchedulerTeaser) {
            startupSchedulerTeaserPending = true
        }
    }

    LaunchedEffect(shouldAutoOpenSchedulerAfterOnboarding) {
        if (schedulerEnabled && shouldAutoOpenSchedulerAfterOnboarding) {
            postOnboardingSchedulerAutoOpenPending = true
        }
    }

    LaunchedEffect(forceSetupOnLaunch) {
        if (forceSetupOnLaunch && !shellState.isForcedFirstLaunchOnboarding) {
            shellState = startRuntimeForcedFirstLaunchOnboarding(shellState)
        }
    }

    val followUpSurface = deriveRuntimeFollowUpSurface(shellState)
    LaunchedEffect(activeFollowUp?.threadId, followUpSurface) {
        if (activeFollowUp != null) {
            followUpOwner.markSurface(followUpSurface)
        }
    }

    fun closeOverlays() {
        shellState = closeRuntimeOverlays(shellState)
    }

    fun openScheduler(action: DynamicIslandTapAction = DynamicIslandTapAction.OpenSchedulerDrawer()) {
        if (!schedulerEnabled) {
            shellState = handleRuntimeConnectivityEntryRequest(
                state = shellState,
                connectionState = connectivityState,
                source = "harmony_scheduler_blocked"
            )
            return
        }
        when (action) {
            is DynamicIslandTapAction.OpenSchedulerDrawer -> {
                action.target
                    ?.toDayOffset()
                    ?.let(schedulerViewModel::onDateSelected)
            }
            DynamicIslandTapAction.OpenConnectivityEntry -> Unit
        }
        shellState = openRuntimeScheduler(
            dismissRuntimeSchedulerIslandHint(shellState)
        )
    }

    fun handleDynamicIslandTap(action: DynamicIslandTapAction) {
        when (action) {
            is DynamicIslandTapAction.OpenSchedulerDrawer -> openScheduler(action)
            DynamicIslandTapAction.OpenConnectivityEntry -> {
                shellState = handleRuntimeConnectivityEntryRequest(
                    state = shellState,
                    connectionState = connectivityState,
                    source = "dynamic_island"
                )
            }
        }
    }

    fun openAudioDrawer(mode: RuntimeAudioDrawerMode) {
        shellState = openRuntimeAudioDrawer(shellState, mode)
    }

    LaunchedEffect(
        schedulerEnabled,
        postOnboardingSchedulerAutoOpenPending,
        startupSchedulerTeaserPending,
        shellState,
        isImeVisible
    ) {
        when {
            !schedulerEnabled -> Unit
            shouldAutoOpenRuntimeSchedulerPostOnboardingHandoff(
                state = shellState,
                isImeVisible = isImeVisible,
                handoffPending = postOnboardingSchedulerAutoOpenPending
            ) -> {
                postOnboardingSchedulerAutoOpenPending = false
                startupSchedulerTeaserPending = false
                onPostOnboardingSchedulerAutoOpened()
                onFirstLaunchSchedulerTeaserShown()
                openScheduler()
            }

            shouldAutoOpenRuntimeSchedulerStartupTeaser(
                state = shellState,
                isImeVisible = isImeVisible,
                teaserPending = startupSchedulerTeaserPending
            ) -> {
                startupSchedulerTeaserPending = false
                onFirstLaunchSchedulerTeaserShown()
                openScheduler()
            }
        }
    }

    RuntimeShellContent(
        chatViewModel = chatViewModel,
        dependencies = dependencies,
        audioViewModel = audioViewModel,
        pairingViewModel = pairingViewModel,
        connectivityViewModel = connectivityViewModel,
        connectivityState = connectivityState,
        shellState = shellState,
        activeReminderBanner = activeReminderBanner,
        activeFollowUp = activeFollowUp,
        currentSessionId = currentSessionId,
        groupedSessions = groupedSessions,
        currentChatAudioId = currentChatAudioId,
        transcriptRevealState = transcriptRevealState,
        currentSchedulerFollowUpContext = currentSchedulerFollowUpContext,
        selectedSchedulerFollowUpTaskId = selectedSchedulerFollowUpTaskId,
        voiceDraftState = voiceDraftState,
        dynamicIslandState = dynamicIslandPresentation.uiState,
        schedulerEnabled = schedulerEnabled,
        isImeVisible = isImeVisible,
        showRuntimeIdleComposerHint = shouldShowRuntimeIdleComposerHint(shellState, isImeVisible),
        currentSessionHasAudioContextHistory = currentSessionHasAudioContextHistory,
        trackedPendingAudioIds = trackedPendingAudioIds,
        coroutineScope = coroutineScope,
        onImportTestAudio = { importTestAudioLauncher.launch("audio/*") },
        onForcedFirstLaunchOnboardingCompleted = onForcedSetupCompleted,
        dismissReminderBanner = schedulerViewModel::dismissActiveReminderBanner,
        clearFollowUp = followUpOwner::clear,
        closeOverlays = ::closeOverlays,
        openScheduler = ::handleDynamicIslandTap,
        openAudioDrawer = ::openAudioDrawer,
        mutateShellState = { transform ->
            shellState = transform(shellState)
        }
    )
}
