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
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandTapAction
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
import com.smartsales.prism.ui.sim.handleRuntimeConnectivityOnboardingReplayRequest
import com.smartsales.prism.ui.sim.initialRuntimeShellState
import com.smartsales.prism.ui.sim.openRuntimeAudioDrawer
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
    val topUrgentTasks by schedulerViewModel.topUrgentTasks.collectAsStateWithLifecycle()
    val activeReminderBanner by schedulerViewModel.activeReminderBanner.collectAsStateWithLifecycle()
    val currentSchedulerFollowUpContext by chatViewModel.currentSchedulerFollowUpContext.collectAsStateWithLifecycle()
    val selectedSchedulerFollowUpTaskId by chatViewModel.selectedSchedulerFollowUpTaskId.collectAsStateWithLifecycle()
    val groupedSessions by chatViewModel.groupedSessions.collectAsStateWithLifecycle()
    val currentChatAudioId by chatViewModel.currentLinkedAudioId.collectAsStateWithLifecycle()
    val transcriptRevealState by chatViewModel.artifactTranscriptRevealState.collectAsStateWithLifecycle()
    val voiceDraftState by chatViewModel.voiceDraftState.collectAsStateWithLifecycle()
    val audioEntries by audioViewModel.entries.collectAsStateWithLifecycle()
    val trackedPendingAudioIds = remember { mutableStateMapOf<String, String>() }
    val isImeVisible = rememberSimImeVisibility()
    var startupSchedulerTeaserPending by remember {
        mutableStateOf(shouldShowFirstLaunchSchedulerTeaser)
    }
    var postOnboardingSchedulerAutoOpenPending by remember {
        mutableStateOf(shouldAutoOpenSchedulerAfterOnboarding)
    }
    val schedulerIslandItems = remember(sessionTitle, topUrgentTasks, shellState.showSchedulerIslandHint) {
        buildSimDynamicIslandItems(
            sessionTitle = sessionTitle,
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
    val dynamicIslandCoordinator = remember(coroutineScope, connectivityViewModel) {
        SimShellDynamicIslandCoordinator(
            parentScope = coroutineScope,
            schedulerItems = schedulerIslandItemsFlow,
            connectivityState = connectivityViewModel.connectionState,
            batteryLevel = connectivityViewModel.batteryLevel,
            takeoverSuppressed = islandTakeoverSuppressedFlow
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

    LaunchedEffect(shellState.activeDrawer, shellState.activeConnectivitySurface) {
        islandTakeoverSuppressedFlow.value =
            shellState.activeDrawer == com.smartsales.prism.ui.sim.RuntimeDrawerType.SCHEDULER ||
                shellState.activeConnectivitySurface != null
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
        val scenario = debugFollowUpScenario ?: return@LaunchedEffect
        handleBadgeSchedulerContinuityIngress(
            event = buildSimDebugFollowUpEvent(scenario),
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

    LaunchedEffect(shouldShowFirstLaunchSchedulerTeaser) {
        if (shouldShowFirstLaunchSchedulerTeaser) {
            startupSchedulerTeaserPending = true
        }
    }

    LaunchedEffect(shouldAutoOpenSchedulerAfterOnboarding) {
        if (shouldAutoOpenSchedulerAfterOnboarding) {
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

    fun replayOnboarding() {
        pairingViewModel.cancelPairing()
        shellState = handleRuntimeConnectivityOnboardingReplayRequest(
            state = shellState,
            source = "audio_drawer_replay"
        )
    }

    LaunchedEffect(
        postOnboardingSchedulerAutoOpenPending,
        startupSchedulerTeaserPending,
        shellState,
        isImeVisible
    ) {
        when {
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
        isImeVisible = isImeVisible,
        showRuntimeIdleComposerHint = shouldShowRuntimeIdleComposerHint(shellState, isImeVisible),
        trackedPendingAudioIds = trackedPendingAudioIds,
        coroutineScope = coroutineScope,
        onImportTestAudio = { importTestAudioLauncher.launch("audio/*") },
        onForcedFirstLaunchOnboardingCompleted = onForcedSetupCompleted,
        onReplayOnboarding = ::replayOnboarding,
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
