package com.smartsales.prism.ui.sim

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.toDayOffset
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.drawers.AudioStatus
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun SimShell(
    badgeAudioPipeline: BadgeAudioPipeline,
    debugFollowUpScenario: SimDebugFollowUpScenario? = null
) {
    val chatViewModel: SimAgentViewModel = hiltViewModel()
    val schedulerViewModel: SimSchedulerViewModel = viewModel()
    val followUpOwner: SimBadgeFollowUpOwner = viewModel()
    val audioViewModel: SimAudioDrawerViewModel = hiltViewModel()
    val connectivityViewModel: ConnectivityViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    val dependencies = remember(chatViewModel, schedulerViewModel, audioViewModel) {
        DefaultSimShellDependencies(
            chatViewModel = chatViewModel,
            schedulerViewModel = schedulerViewModel,
            audioViewModel = audioViewModel
        )
    }
    var shellState by remember { mutableStateOf(SimShellState()) }
    val connectivityState by connectivityViewModel.connectionState.collectAsStateWithLifecycle()
    val activeFollowUp by followUpOwner.activeFollowUp.collectAsStateWithLifecycle()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val sessionTitle by chatViewModel.sessionTitle.collectAsStateWithLifecycle()
    val topUrgentTasks by schedulerViewModel.topUrgentTasks.collectAsStateWithLifecycle()
    val currentSchedulerFollowUpContext by chatViewModel.currentSchedulerFollowUpContext.collectAsStateWithLifecycle()
    val selectedSchedulerFollowUpTaskId by chatViewModel.selectedSchedulerFollowUpTaskId.collectAsStateWithLifecycle()
    val groupedSessions by chatViewModel.groupedSessions.collectAsStateWithLifecycle()
    val currentChatAudioId by chatViewModel.currentLinkedAudioId.collectAsStateWithLifecycle()
    val audioEntries by audioViewModel.entries.collectAsStateWithLifecycle()
    val trackedPendingAudioIds = remember { mutableStateMapOf<String, String>() }
    val isAudioDrawerOpen = shellState.activeDrawer == SimDrawerType.AUDIO
    val isImeVisible = rememberSimImeVisibility()
    val dynamicIslandItems = remember(sessionTitle, topUrgentTasks) {
        buildSimDynamicIslandItems(
            sessionTitle = sessionTitle,
            orderedTasks = topUrgentTasks
        )
    }
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
                startOwner = { sessionId, threadId ->
                    followUpOwner.startBadgeSchedulerFollowUp(
                        boundSessionId = sessionId,
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
            startOwner = { sessionId, threadId ->
                followUpOwner.startBadgeSchedulerFollowUp(
                    boundSessionId = sessionId,
                    threadId = threadId
                )
            }
        )
    }

    LaunchedEffect(isAudioDrawerOpen, shellState.audioDrawerMode) {
        if (shouldAttemptSimAudioDrawerAutoSync(isAudioDrawerOpen, shellState.audioDrawerMode)) {
            audioViewModel.maybeAutoSyncFromBadge()
        } else if (!isAudioDrawerOpen) {
            audioViewModel.resetSyncSession()
        }
    }

    val followUpSurface = deriveSimFollowUpSurface(shellState)
    LaunchedEffect(activeFollowUp?.threadId, followUpSurface) {
        if (activeFollowUp != null) {
            followUpOwner.markSurface(followUpSurface)
        }
    }

    fun closeOverlays() {
        shellState = closeSimOverlays(shellState)
    }

    fun openScheduler(action: DynamicIslandTapAction = DynamicIslandTapAction.OpenSchedulerDrawer()) {
        when (action) {
            is DynamicIslandTapAction.OpenSchedulerDrawer -> {
                action.target
                    ?.toDayOffset()
                    ?.let(schedulerViewModel::onDateSelected)
            }
        }
        shellState = openSimScheduler(shellState)
    }

    fun openAudioDrawer(mode: SimAudioDrawerMode) {
        shellState = openSimAudioDrawer(shellState, mode)
    }

    SimShellContent(
        chatViewModel = chatViewModel,
        dependencies = dependencies,
        audioViewModel = audioViewModel,
        connectivityViewModel = connectivityViewModel,
        connectivityState = connectivityState,
        shellState = shellState,
        activeFollowUp = activeFollowUp,
        currentSessionId = currentSessionId,
        groupedSessions = groupedSessions,
        currentChatAudioId = currentChatAudioId,
        currentSchedulerFollowUpContext = currentSchedulerFollowUpContext,
        selectedSchedulerFollowUpTaskId = selectedSchedulerFollowUpTaskId,
        dynamicIslandItems = dynamicIslandItems,
        isImeVisible = isImeVisible,
        trackedPendingAudioIds = trackedPendingAudioIds,
        coroutineScope = coroutineScope,
        onImportTestAudio = { importTestAudioLauncher.launch("audio/*") },
        clearFollowUp = followUpOwner::clear,
        closeOverlays = ::closeOverlays,
        openScheduler = ::openScheduler,
        openAudioDrawer = ::openAudioDrawer,
        mutateShellState = { transform ->
            shellState = transform(shellState)
        }
    )
}
