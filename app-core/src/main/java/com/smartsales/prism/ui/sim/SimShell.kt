package com.smartsales.prism.ui.sim

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.BuildConfig
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.AgentIntelligenceVisualMode
import com.smartsales.prism.ui.PrismElevation
import com.smartsales.prism.ui.components.ConnectivityModal
import com.smartsales.prism.ui.components.ConnectivityManagerScreen
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandSchedulerTarget
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.toDayOffset
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.drawers.HistoryDrawer
import com.smartsales.prism.ui.onboarding.SimConnectivityPairingFlow
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.settings.UserCenterScreen
import com.smartsales.prism.ui.theme.BackgroundApp
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.ZoneId

internal const val SIM_BADGE_SCHEDULER_CONTINUITY_INGRESS_ACCEPTED_SUMMARY =
    "SIM badge scheduler continuity ingress accepted"
internal const val SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY =
    "SIM connectivity entry opened"
internal const val SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY =
    "SIM connectivity manager direct entry opened"
internal const val SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY =
    "SIM connectivity setup started"
internal const val SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY =
    "SIM connectivity setup completed to manager"
internal const val SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY =
    "SIM audio persisted artifact opened"
internal const val SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY =
    "SIM audio grounded chat opened from artifact"
private const val SIM_AUDIO_OFFLINE_LOG_TAG = "SimAudioOffline"
private const val SIM_AUDIO_CHAT_ROUTE_LOG_TAG = "SimAudioChatRoute"

internal data class SimBadgeSchedulerFollowUpSeed(
    val threadId: String,
    val transcript: String,
    val tasks: List<SchedulerFollowUpTaskSummary>,
    val batchId: String? = null
)

internal enum class SimDebugFollowUpScenario {
    SINGLE,
    MULTI
}

internal fun buildSimDebugFollowUpEvent(
    scenario: SimDebugFollowUpScenario,
    nowMillis: Long = System.currentTimeMillis()
): PipelineEvent.Complete {
    return when (scenario) {
        SimDebugFollowUpScenario.SINGLE -> PipelineEvent.Complete(
            result = SchedulerResult.TaskCreated(
                taskId = "debug_follow_up_single",
                title = "客户回访",
                dayOffset = 0,
                scheduledAtMillis = nowMillis + 60_000L,
                durationMinutes = 30
            ),
            filename = "debug_follow_up_single.wav",
            transcript = "提醒我一会儿回访客户"
        )
        SimDebugFollowUpScenario.MULTI -> PipelineEvent.Complete(
            result = SchedulerResult.MultiTaskCreated(
                tasks = listOf(
                    SchedulerResult.TaskCreated(
                        taskId = "debug_follow_up_multi_a",
                        title = "客户A回访",
                        dayOffset = 0,
                        scheduledAtMillis = nowMillis + 120_000L,
                        durationMinutes = 30
                    ),
                    SchedulerResult.TaskCreated(
                        taskId = "debug_follow_up_multi_b",
                        title = "客户B回访",
                        dayOffset = 0,
                        scheduledAtMillis = nowMillis + 240_000L,
                        durationMinutes = 30
                    )
                )
            ),
            filename = "debug_follow_up_multi.wav",
            transcript = "安排两个客户回访"
        )
    }
}

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

internal fun handleBadgeSchedulerFollowUpStart(
    seed: SimBadgeSchedulerFollowUpSeed,
    startSession: (SimBadgeSchedulerFollowUpSeed) -> String?,
    startOwner: (String, String) -> Unit
): String? {
    if (seed.transcript.isBlank() || seed.tasks.isEmpty()) return null
    val sessionId = startSession(seed) ?: return null
    startOwner(sessionId, seed.threadId)
    return sessionId
}

internal fun emitBadgeSchedulerContinuityIngressTelemetry(
    promptText: String,
    log: (String) -> Unit = { message -> Log.d("SimBadgeFollowUp", message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = promptText.length,
        summary = SIM_BADGE_SCHEDULER_CONTINUITY_INGRESS_ACCEPTED_SUMMARY,
        rawDataDump = promptText
    )
    log("badge scheduler continuity ingress accepted: $promptText")
}

internal fun emitSimConnectivityRouteTelemetry(
    summary: String,
    detail: String,
    log: (String) -> Unit = { message -> Log.d("SimConnectivityRoute", message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = summary,
        rawDataDump = detail
    )
    log("$summary: $detail")
}

internal fun emitSimAudioPersistedArtifactOpenedTelemetry(
    audioId: String,
    title: String,
    log: (String, String) -> Unit = { tag, message -> Log.d(tag, message) }
) {
    val detail = "audioId=$audioId title=$title"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY,
        rawDataDump = detail
    )
    log(SIM_AUDIO_OFFLINE_LOG_TAG, "$SIM_AUDIO_PERSISTED_ARTIFACT_OPENED_SUMMARY: $detail")
}

internal fun emitSimAudioGroundedChatOpenedFromArtifactTelemetry(
    audioId: String,
    title: String,
    log: (String, String) -> Unit = { tag, message -> Log.d(tag, message) }
) {
    val detail = "audioId=$audioId title=$title"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY,
        rawDataDump = detail
    )
    log(
        SIM_AUDIO_CHAT_ROUTE_LOG_TAG,
        "$SIM_AUDIO_GROUNDED_CHAT_OPENED_FROM_ARTIFACT_SUMMARY: $detail"
    )
}

internal fun extractBadgeSchedulerContinuitySeed(event: PipelineEvent): SimBadgeSchedulerFollowUpSeed? {
    if (event !is PipelineEvent.Complete) return null

    val transcript = event.transcript.takeIf { it.isNotBlank() } ?: return null
    return when (val result = event.result) {
        is SchedulerResult.TaskCreated -> SimBadgeSchedulerFollowUpSeed(
            threadId = UUID.randomUUID().toString(),
            transcript = transcript,
            tasks = listOf(result.toSummary())
        )
        is SchedulerResult.MultiTaskCreated -> result.tasks.takeIf { it.isNotEmpty() }?.let { tasks ->
            SimBadgeSchedulerFollowUpSeed(
                threadId = UUID.randomUUID().toString(),
                transcript = transcript,
                tasks = tasks.map { it.toSummary() },
                batchId = UUID.randomUUID().toString()
            )
        }
        else -> null
    }
}

internal fun handleBadgeSchedulerContinuityIngress(
    event: PipelineEvent,
    startSession: (SimBadgeSchedulerFollowUpSeed) -> String?,
    startOwner: (String, String) -> Unit,
    emitTelemetry: (String) -> Unit = { prompt ->
        emitBadgeSchedulerContinuityIngressTelemetry(prompt)
    }
): String? {
    val seed = extractBadgeSchedulerContinuitySeed(event) ?: return null
    emitTelemetry(seed.transcript)
    return handleBadgeSchedulerFollowUpStart(
        seed = seed,
        startSession = startSession,
        startOwner = startOwner
    )
}

private fun SchedulerResult.TaskCreated.toSummary(): SchedulerFollowUpTaskSummary {
    return SchedulerFollowUpTaskSummary(
        taskId = taskId,
        title = title,
        dayOffset = dayOffset,
        scheduledAtMillis = scheduledAtMillis,
        durationMinutes = durationMinutes
    )
}

internal fun handleSimNewSessionAction(
    activeFollowUp: SimBadgeFollowUpState?,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    startNewSession: () -> Unit
) {
    if (activeFollowUp != null) {
        clearFollowUp(SimBadgeFollowUpClearReason.NEW_SESSION)
    }
    startNewSession()
}

internal fun handleSimSessionSwitchAction(
    targetSessionId: String,
    activeFollowUp: SimBadgeFollowUpState?,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    switchSession: (String) -> Unit
) {
    if (activeFollowUp != null && activeFollowUp.boundSessionId != targetSessionId) {
        clearFollowUp(SimBadgeFollowUpClearReason.SESSION_SWITCHED)
    }
    switchSession(targetSessionId)
}

internal fun handleSimSessionDeleteAction(
    targetSessionId: String,
    activeFollowUp: SimBadgeFollowUpState?,
    clearFollowUp: (SimBadgeFollowUpClearReason) -> Unit,
    deleteSession: (String) -> Unit
) {
    if (activeFollowUp != null && activeFollowUp.boundSessionId == targetSessionId) {
        clearFollowUp(SimBadgeFollowUpClearReason.SESSION_DELETED)
    }
    deleteSession(targetSessionId)
}

internal fun deriveSimFollowUpSurface(state: SimShellState): SimBadgeFollowUpSurface {
    return when {
        state.showHistory -> SimBadgeFollowUpSurface.HISTORY
        state.showSettings -> SimBadgeFollowUpSurface.SETTINGS
        state.activeConnectivitySurface != null -> SimBadgeFollowUpSurface.CONNECTIVITY
        state.activeDrawer == SimDrawerType.SCHEDULER -> SimBadgeFollowUpSurface.SCHEDULER
        else -> SimBadgeFollowUpSurface.CHAT
    }
}

internal fun closeSimOverlays(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = null,
    showHistory = false,
    showSettings = false
)

internal fun openSimScheduler(state: SimShellState): SimShellState = state.copy(
    activeDrawer = SimDrawerType.SCHEDULER,
    showHistory = false,
    activeConnectivitySurface = null,
    showSettings = false
)

internal fun openSimAudioDrawer(
    state: SimShellState,
    mode: SimAudioDrawerMode
): SimShellState = state.copy(
    activeDrawer = SimDrawerType.AUDIO,
    audioDrawerMode = mode,
    showHistory = false,
    activeConnectivitySurface = null,
    showSettings = false
)

internal fun openSimConnectivityModal(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = SimConnectivitySurface.MODAL,
    showHistory = false,
    showSettings = false
)

internal fun openSimConnectivitySetup(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = SimConnectivitySurface.SETUP,
    showHistory = false,
    showSettings = false
)

internal fun openSimConnectivityManager(state: SimShellState): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = SimConnectivitySurface.MANAGER,
    showHistory = false,
    showSettings = false
)

internal fun resolveSimConnectivityEntrySurface(
    connectionState: ConnectionState
): SimConnectivitySurface = when (connectionState) {
    ConnectionState.NEEDS_SETUP -> SimConnectivitySurface.MODAL
    else -> SimConnectivitySurface.MANAGER
}

internal fun openSimConnectivityEntry(
    state: SimShellState,
    connectionState: ConnectionState
): SimShellState = state.copy(
    activeDrawer = null,
    audioDrawerMode = SimAudioDrawerMode.BROWSE,
    activeConnectivitySurface = resolveSimConnectivityEntrySurface(connectionState),
    showHistory = false,
    showSettings = false
)

internal fun closeSimConnectivitySurface(state: SimShellState): SimShellState = state.copy(
    activeConnectivitySurface = null
)

internal fun handleSimConnectivityEntryRequest(
    state: SimShellState,
    connectionState: ConnectionState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): SimShellState {
    val target = resolveSimConnectivityEntrySurface(connectionState)
    val summary = if (target == SimConnectivitySurface.MODAL) {
        SIM_CONNECTIVITY_ENTRY_OPENED_SUMMARY
    } else {
        SIM_CONNECTIVITY_MANAGER_DIRECT_ENTRY_OPENED_SUMMARY
    }
    emitTelemetry(summary, "source=$source state=$connectionState target=$target")
    return openSimConnectivityEntry(state, connectionState)
}

internal fun handleSimConnectivitySetupStart(
    state: SimShellState,
    source: String,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): SimShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_STARTED_SUMMARY,
        "source=$source target=${SimConnectivitySurface.SETUP}"
    )
    return openSimConnectivitySetup(state)
}

internal fun handleSimConnectivitySetupCompleted(
    state: SimShellState,
    emitTelemetry: (String, String) -> Unit = { summary, detail ->
        emitSimConnectivityRouteTelemetry(summary, detail)
    }
): SimShellState {
    emitTelemetry(
        SIM_CONNECTIVITY_SETUP_COMPLETED_SUMMARY,
        "source=pairing_success target=${SimConnectivitySurface.MANAGER}"
    )
    return openSimConnectivityManager(state)
}

internal fun shouldShowSimShellScrim(state: SimShellState): Boolean =
    state.activeDrawer != null ||
        state.showHistory ||
        state.activeConnectivitySurface == SimConnectivitySurface.MODAL

internal fun shouldAttemptSimAudioDrawerAutoSync(
    isDrawerOpen: Boolean,
    mode: SimAudioDrawerMode
): Boolean = isDrawerOpen && mode == SimAudioDrawerMode.BROWSE

internal fun buildSimDynamicIslandItems(
    sessionTitle: String,
    orderedTasks: List<ScheduledTask>
): List<DynamicIslandItem> {
    val normalizedTitle = sessionTitle.ifBlank { "SIM" }
    val activeTasks = orderedTasks
        .filterNot { it.isDone }
        .take(3)
    if (activeTasks.isEmpty()) {
        return listOf(
            DynamicIslandItem(
                sessionTitle = normalizedTitle,
                schedulerSummary = "暂无待办",
                isIdleEntry = true,
                tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
            )
        )
    }
    return activeTasks.map { task ->
        DynamicIslandItem(
            sessionTitle = normalizedTitle,
            schedulerSummary = buildSimDynamicIslandSummary(task),
            isConflict = task.hasConflict,
            tapAction = DynamicIslandTapAction.OpenSchedulerDrawer(
                target = DynamicIslandSchedulerTarget(
                    date = task.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                    taskId = task.id,
                    isConflict = task.hasConflict
                )
            )
        )
    }
}

private fun buildSimDynamicIslandSummary(task: ScheduledTask): String {
    return when {
        task.hasConflict && task.isVague -> "冲突：${task.title} · 待定提醒"
        task.hasConflict && task.timeDisplay.isBlank() -> "冲突：${task.title}"
        task.hasConflict -> "冲突：${task.title} · ${task.timeDisplay}"
        task.isVague -> "即将：${task.title} · 待定提醒"
        task.timeDisplay.isBlank() -> "即将：${task.title}"
        else -> "即将：${task.title} · ${task.timeDisplay}"
    }
}

@Composable
private fun SimSchedulerFollowUpPrompt(
    onOpen: () -> Unit
) {
    val accent = Color(0xFF38BDF8)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xCC111827),
        contentColor = TextPrimary,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "工牌已创建新日程",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "点击进入任务级跟进会话",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            TextButton(
                onClick = onOpen,
                colors = ButtonDefaults.textButtonColors(contentColor = accent)
            ) {
                Text("继续跟进")
            }
        }
    }
}

@Composable
private fun SimSchedulerFollowUpActionStrip(
    context: SchedulerFollowUpContext,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
    onAction: (SimSchedulerFollowUpQuickAction) -> Unit
) {
    val accent = Color(0xFF38BDF8)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE6111827),
        contentColor = TextPrimary,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "任务级跟进",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                context.taskSummaries.forEach { task ->
                    AssistChip(
                        onClick = { onSelectTask(task.taskId) },
                        label = { Text(task.title) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (task.taskId == selectedTaskId) {
                                accent.copy(alpha = 0.18f)
                            } else {
                                Color.White.copy(alpha = 0.06f)
                            },
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                    SimSchedulerFollowUpQuickAction.EXPLAIN to "说明",
                    SimSchedulerFollowUpQuickAction.STATUS to "状态",
                    SimSchedulerFollowUpQuickAction.PREFILL_RESCHEDULE to "改期",
                    SimSchedulerFollowUpQuickAction.MARK_DONE to "完成",
                    SimSchedulerFollowUpQuickAction.DELETE to "删除"
                ).forEach { (action, label) ->
                    AssistChip(
                        onClick = { onAction(action) },
                        label = { Text(label) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.06f),
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundApp)
    ) {
        SimDrawerEdgeGestureLayer(
            state = shellState,
            isImeVisible = isImeVisible,
            onOpenScheduler = { openScheduler() },
            onOpenAudioBrowse = { openAudioDrawer(SimAudioDrawerMode.BROWSE) }
        )

        AgentIntelligenceScreen(
            viewModel = dependencies.chatViewModel,
            onMenuClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    showHistory = true,
                    activeConnectivitySurface = null,
                    showSettings = false
                )
            },
            onNewSessionClick = {
                handleSimNewSessionAction(
                    activeFollowUp = activeFollowUp,
                    clearFollowUp = followUpOwner::clear,
                    startNewSession = chatViewModel::startNewSession
                )
                closeOverlays()
            },
            onAudioBadgeClick = {},
            onSchedulerClick = ::openScheduler,
            onAudioDrawerClick = { openAudioDrawer(SimAudioDrawerMode.BROWSE) },
            onAttachClick = { openAudioDrawer(SimAudioDrawerMode.CHAT_RESELECT) },
            onProfileClick = {
                shellState = shellState.copy(
                    activeDrawer = null,
                    audioDrawerMode = SimAudioDrawerMode.BROWSE,
                    showHistory = false,
                    activeConnectivitySurface = null,
                    showSettings = true
                )
            },
            onDebugClick = {},
            showDebugButton = false,
            visualMode = AgentIntelligenceVisualMode.SIM,
            simDynamicIslandItems = dynamicIslandItems
        )

        val showScrim = shouldShowSimShellScrim(shellState)

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
                        handleSimSessionSwitchAction(
                            targetSessionId = sessionId,
                            activeFollowUp = activeFollowUp,
                            clearFollowUp = followUpOwner::clear,
                            switchSession = chatViewModel::switchSession
                        )
                    },
                    onDeviceClick = {
                        shellState = handleSimConnectivityEntryRequest(
                            state = shellState.copy(showHistory = false),
                            connectionState = connectivityState,
                            source = "history_device"
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
                    onDeleteSession = { sessionId ->
                        handleSimSessionDeleteAction(
                            targetSessionId = sessionId,
                            activeFollowUp = activeFollowUp,
                            clearFollowUp = followUpOwner::clear,
                            deleteSession = chatViewModel::deleteSession
                        )
                    }
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
                isOpen = isAudioDrawerOpen,
                onDismiss = { shellState = shellState.copy(activeDrawer = null, audioDrawerMode = SimAudioDrawerMode.BROWSE) },
                mode = shellState.audioDrawerMode,
                currentChatAudioId = currentChatAudioId,
                showTestImportAction = BuildConfig.DEBUG && shellState.audioDrawerMode == SimAudioDrawerMode.BROWSE,
                showDebugScenarioActions = BuildConfig.DEBUG && shellState.audioDrawerMode == SimAudioDrawerMode.BROWSE,
                viewModel = dependencies.audioViewModel,
                onOpenConnectivity = {
                    shellState = handleSimConnectivityEntryRequest(
                        state = shellState.copy(activeDrawer = null),
                        connectionState = connectivityState,
                        source = "audio_drawer"
                    )
                },
                onSyncFromBadge = { audioViewModel.syncFromBadgeManually() },
                onImportTestAudio = { importTestAudioLauncher.launch("audio/*") },
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

        if (shellState.activeConnectivitySurface == SimConnectivitySurface.MODAL) {
            Box(modifier = Modifier.zIndex(PrismElevation.Drawer)) {
                ConnectivityModal(
                    onDismiss = { shellState = closeSimConnectivitySurface(shellState) },
                    onNavigateToSetup = {
                        shellState = handleSimConnectivitySetupStart(
                            state = shellState,
                            source = "bootstrap_modal"
                        )
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
                onExit = { shellState = closeSimConnectivitySurface(shellState) },
                onCompleted = {
                    shellState = handleSimConnectivitySetupCompleted(shellState)
                }
            )
        }

        AnimatedVisibility(
            visible = shellState.activeConnectivitySurface == SimConnectivitySurface.MANAGER,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(PrismElevation.Drawer + 1f)
        ) {
            ConnectivityManagerScreen(
                onClose = { shellState = closeSimConnectivitySurface(shellState) },
                onNavigateToSetup = {
                    shellState = handleSimConnectivitySetupStart(
                        state = shellState,
                        source = "manager_needs_setup"
                    )
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
                onClose = { shellState = shellState.copy(showSettings = false) }
            )
        }

        val showFollowUpPrompt = activeFollowUp != null &&
            currentSessionId != activeFollowUp?.boundSessionId &&
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
                        activeFollowUp?.boundSessionId?.let(chatViewModel::switchSession)
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
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 108.dp
                    )
                    .zIndex(PrismElevation.Drawer + 2f)
            ) {
                SimSchedulerFollowUpActionStrip(
                    context = currentSchedulerFollowUpContext!!,
                    selectedTaskId = selectedSchedulerFollowUpTaskId,
                    onSelectTask = chatViewModel::selectSchedulerFollowUpTask,
                    onAction = chatViewModel::performSchedulerFollowUpQuickAction
                )
            }
        }

    }
}
