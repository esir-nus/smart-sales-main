package com.smartsales.prism.ui.sim

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.prism.data.notification.ExactAlarmPermissionGate
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ConflictResolution
import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.CreateVagueTaskParams
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.MutationResult
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.TaskDefinition
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import com.smartsales.prism.domain.scheduler.RelativeTimeResolver
import com.smartsales.prism.domain.scheduler.UniMAnchorKind
import com.smartsales.prism.domain.scheduler.UniMExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMExtractionResult
import com.smartsales.prism.domain.scheduler.UniMTaskFragment
import com.smartsales.prism.domain.scheduler.UniMTaskMode
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.RescheduleExitMotion
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

/**
 * SIM 调度 ViewModel。
 * 仅复用 Path A 所需的日程域能力，避免把智能版调度运行时整体带回 SIM。
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SimSchedulerViewModel @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val inspirationRepository: InspirationRepository,
    private val scheduleBoard: ScheduleBoard,
    private val alarmScheduler: AlarmScheduler,
    private val exactAlarmPermissionGate: ExactAlarmPermissionGate,
    private val fastTrackMutationEngine: FastTrackMutationEngine,
    private val asrService: AsrService,
    private val uniMExtractionService: RealUniMExtractionService,
    private val uniAExtractionService: RealUniAExtractionService,
    private val uniBExtractionService: RealUniBExtractionService,
    private val uniCExtractionService: RealUniCExtractionService,
    private val timeProvider: TimeProvider
) : ViewModel(), ISchedulerViewModel {

    private enum class SingleTaskTelemetryAnchor {
        NOW_OFFSET,
        NOW_DAY_OFFSET
    }

    private companion object {
        private val NOW_DAY_OFFSET_REGEX = Regex(
            pattern = "(明天|后天|tomorrow|day after tomorrow)"
        )
        private val CLOCK_HINT_REGEX = Regex(
            pattern = "(上午|下午|中午|晚上|凌晨|早上|\\d{1,2}:\\d{2}|\\d{1,2}点半?|\\d{1,2}時|\\d{1,2}时)"
        )
        private val RESCHEDULE_KEYWORDS = listOf(
            "改期到",
            "改到",
            "改成",
            "改期",
            "挪到",
            "推迟到",
            "提前到",
            "往后推",
            "往前提",
            "推迟",
            "推后",
            "延后到",
            "延期到",
            "延后",
            "延期",
            "提前",
            "提早",
            "reschedule to",
            "reschedule",
            "move to",
            "move "
        )
    }

    private data class FragmentAnchorState(
        val exactStart: Instant? = null,
        val anchorDate: LocalDate? = null
    )

    private data class MultiTaskExecutionSummary(
        val createdTasks: List<ScheduledTask>,
        val unresolvedReasons: List<String>,
        val downgradedCount: Int
    )

    private data class DeterministicRelativeCreateCandidate(
        val title: String,
        val startTimeIso: String,
        val matchedText: String,
        val normalizedTranscript: String
    )

    private sealed interface ResolvedMultiTaskFragment {
        data class Resolved(
            val intent: FastTrackResult,
            val nextState: FragmentAnchorState,
            val downgraded: Boolean = false
        ) : ResolvedMultiTaskFragment

        data class Unresolved(val reason: String) : ResolvedMultiTaskFragment
    }

    private val _activeDayOffset = MutableStateFlow(0)
    override val activeDayOffset: StateFlow<Int> = _activeDayOffset.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    override val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedInspirationIds = MutableStateFlow(emptySet<String>())
    override val selectedInspirationIds: StateFlow<Set<String>> = _selectedInspirationIds.asStateFlow()

    private val _tipsLoading = MutableStateFlow(emptySet<String>())
    override val tipsLoading: StateFlow<Set<String>> = _tipsLoading.asStateFlow()

    private val _pipelineStatus = MutableStateFlow<String?>(null)
    override val pipelineStatus: StateFlow<String?> = _pipelineStatus.asStateFlow()

    private val _unacknowledgedDates = MutableStateFlow(emptySet<Int>())
    override val unacknowledgedDates: StateFlow<Set<Int>> = _unacknowledgedDates.asStateFlow()

    private val _rescheduledDates = MutableStateFlow(emptySet<Int>())
    override val rescheduledDates: StateFlow<Set<Int>> = _rescheduledDates.asStateFlow()

    private val _conflictWarning = MutableStateFlow<String?>(null)
    override val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    private val _conflictedTaskIds = MutableStateFlow(emptySet<String>())
    override val conflictedTaskIds: StateFlow<Set<String>> = _conflictedTaskIds.asStateFlow()

    private val _causingTaskId = MutableStateFlow<String?>(null)
    override val causingTaskId: StateFlow<String?> = _causingTaskId.asStateFlow()

    private val _isInspirationsExpanded = MutableStateFlow(true)
    override val isInspirationsExpanded: StateFlow<Boolean> = _isInspirationsExpanded.asStateFlow()

    private val _expandedConflictIds = MutableStateFlow(emptySet<String>())
    override val expandedConflictIds: StateFlow<Set<String>> = _expandedConflictIds.asStateFlow()

    private val _exitingTasks = MutableStateFlow<List<RescheduleExitMotion>>(emptyList())
    override val exitingTasks: StateFlow<List<RescheduleExitMotion>> = _exitingTasks.asStateFlow()

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val exactAlarmPermissionNeeded: SharedFlow<Unit> = _exactAlarmPermissionNeeded.asSharedFlow()

    override val topUrgentTasks: StateFlow<List<ScheduledTask>> = taskRepository
        .queryByDateRange(timeProvider.today, timeProvider.today.plusDays(30))
        .combine(_pipelineStatus) { items, _ ->
            items
                .filterIsInstance<ScheduledTask>()
                .filterNot { it.isDone }
                .sortedWith(
                    compareByDescending<ScheduledTask> { it.hasConflict }
                        .thenBy { it.urgencyLevel.ordinal }
                        .thenBy { it.isVague }
                        .thenBy { it.startTime.truncatedTo(ChronoUnit.MINUTES) }
                )
                .take(3)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val timelineItems: StateFlow<List<SchedulerTimelineItem>> = _activeDayOffset
        .flatMapLatest { dayOffset ->
            combine(
                taskRepository.getTimelineItems(dayOffset),
                inspirationRepository.getAll()
            ) { tasks, inspirations ->
                inspirations + tasks
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private var pipelineStatusResetJob: Job? = null

    override fun onDateSelected(dayOffset: Int) {
        _activeDayOffset.value = dayOffset
        acknowledgeDate(dayOffset)
    }

    override fun acknowledgeDate(dayOffset: Int) {
        _unacknowledgedDates.value = _unacknowledgedDates.value - dayOffset
        _rescheduledDates.value = _rescheduledDates.value - dayOffset
    }

    override fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedInspirationIds.value = emptySet()
        }
    }

    override fun toggleItemSelection(id: String) {
        val next = _selectedInspirationIds.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _selectedInspirationIds.value = next
    }

    override fun deleteItem(id: String) {
        viewModelScope.launch {
            taskRepository.deleteItem(id)
            cancelReminderSafely(id)
            emitStatus("已删除日程")
        }
    }

    override fun onItemClick(id: String) {
        clearFailureState()
    }

    override fun toggleDone(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId) ?: return@launch
            val updatedTask = task.copy(isDone = !task.isDone)
            taskRepository.updateTask(updatedTask)
            if (updatedTask.isDone) {
                cancelReminderSafely(taskId)
            }
            emitStatus(if (task.isDone) "已恢复为待办" else "已标记完成")
        }
    }

    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }

    override fun deleteInspiration(id: String) {
        viewModelScope.launch {
            inspirationRepository.delete(id)
            emitStatus("已删除灵感")
        }
    }

    override fun toggleConflictExpansion(id: String) {
        val next = _expandedConflictIds.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _expandedConflictIds.value = next
    }

    override fun handleConflictResolution(resolution: ConflictResolution) {
        clearFailureState()
        _expandedConflictIds.value = emptySet()
    }

    override fun onReschedule(taskId: String, text: String) {
        viewModelScope.launch {
            clearFailureState()
            val original = taskRepository.getTask(taskId)
                ?: return@launch emitFailure("找不到要改期的日程")
            executeResolvedReschedule(original, text)
        }
    }

    override fun getCachedTips(taskId: String): List<String> = emptyList()

    override fun processAudio(file: File) {
        viewModelScope.launch {
            clearFailureState()
            emitStatus("语音转写中...", autoClear = false)
            when (val transcriptResult = asrService.transcribe(file)) {
                is AsrResult.Success -> processTranscript(transcriptResult.text.trim())
                is AsrResult.Error -> emitFailure("录音转写失败: ${transcriptResult.message}")
            }
        }
    }

    internal suspend fun applyFastTrackResultForTesting(result: FastTrackResult) {
        handleMutation(result)
    }

    private suspend fun processTranscript(transcript: String) {
        if (transcript.isBlank()) {
            emitFailure("未识别到有效日程内容")
            return
        }
        if (looksLikeDeletionTranscript(transcript)) {
            emitFailure("SIM 当前不支持语音删除，请在面板手动操作")
            return
        }
        if (looksLikeRescheduleTranscript(transcript)) {
            handleVoiceRescheduleTranscript(transcript)
            return
        }

        val normalizedTranscript = RelativeTimeResolver.normalizeExplicitRelativeTimeTranscript(transcript)
        val normalizedOverride = normalizedTranscript.takeIf { it != transcript }

        buildDeterministicRelativeCreateCandidate(
            transcript = transcript,
            normalizedTranscript = normalizedTranscript
        )?.let { candidate ->
            Log.d(
                "SimSchedulerRelative",
                "single deterministic relative create matched=${candidate.matchedText} title=${candidate.title} start=${candidate.startTimeIso} transcript=$transcript normalized=${candidate.normalizedTranscript}"
            )
            val result = FastTrackResult.CreateTasks(
                params = CreateTasksParams(
                    unifiedId = UUID.randomUUID().toString(),
                    tasks = listOf(
                        TaskDefinition(
                            title = candidate.title,
                            startTimeIso = candidate.startTimeIso,
                            durationMinutes = 0,
                            urgency = com.smartsales.prism.domain.scheduler.UrgencyEnum.L3_NORMAL
                        )
                    )
                )
            )
            emitSingleTaskExtractionTelemetry(transcript, result)
            handleMutation(result)
            return
        }
        if (
            RelativeTimeResolver.resolveExact(
                userText = transcript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id
            ) != null && !looksLikeMultiTaskCreateTranscript(transcript)
        ) {
            Log.w(
                "SimSchedulerRelative",
                "single deterministic relative create rejected transcript=$transcript normalized=$normalizedTranscript"
            )
            emitFailure("已识别为相对时间日程，但任务内容不完整")
            return
        }

        val batchId = UUID.randomUUID().toString()
        when (val multi = uniMExtractionService.extract(
            UniMExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                batchId = batchId,
                displayedDateIso = displayedDateIso()
            )
        )) {
            is UniMExtractionResult.MultiCreate -> {
                handleMultiTaskCreate(batchId, multi.fragments)
                return
            }
            is UniMExtractionResult.NotMulti -> Unit
        }

        val unifiedId = UUID.randomUUID().toString()
        val exact = uniAExtractionService.extract(
            UniAExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = displayedDateIso()
            )
        )
        if (exact !is FastTrackResult.NoMatch) {
            emitSingleTaskExtractionTelemetry(transcript, exact)
            handleMutation(exact)
            return
        }

        val vague = uniBExtractionService.extract(
            UniBExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = displayedDateIso()
            )
        )
        if (vague !is FastTrackResult.NoMatch) {
            handleMutation(vague)
            return
        }

        val inspiration = uniCExtractionService.extract(
            UniCExtractionRequest(
                transcript = transcript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId
            )
        )
        if (inspiration !is FastTrackResult.NoMatch) {
            handleMutation(inspiration)
            return
        }

        emitFailure(inspiration.reason)
    }

    private suspend fun handleMultiTaskCreate(batchId: String, fragments: List<UniMTaskFragment>) {
        Log.d("SimSchedulerMulti", "batch=$batchId fragments=${fragments.size}")

        var anchorState = FragmentAnchorState()
        val createdTasks = mutableListOf<ScheduledTask>()
        val unresolvedReasons = mutableListOf<String>()
        var downgradedCount = 0

        fragments.forEachIndexed { index, fragment ->
            when (val resolved = resolveMultiTaskFragment(fragment, anchorState)) {
                is ResolvedMultiTaskFragment.Resolved -> {
                    val execution = executeCreateIntent(resolved.intent)
                    if (execution.createdTasks.isNotEmpty()) {
                        createdTasks += execution.createdTasks
                        anchorState = resolved.nextState
                        if (resolved.downgraded) downgradedCount += 1
                    } else {
                        unresolvedReasons += execution.unresolvedReasons.ifEmpty {
                            listOf("片段${index + 1}未创建")
                        }
                    }
                }
                is ResolvedMultiTaskFragment.Unresolved -> {
                    unresolvedReasons += "片段${index + 1}未创建：${resolved.reason}"
                }
            }
        }

        if (createdTasks.isEmpty()) {
            emitFailure(unresolvedReasons.firstOrNull() ?: "未解析到可创建的多任务片段")
            return
        }

        applyAggregatedConflictState(createdTasks)
        val summary = buildMultiTaskStatus(
            createdCount = createdTasks.size,
            unresolvedCount = unresolvedReasons.size,
            downgradedCount = downgradedCount
        )
        Log.d(
            "SimSchedulerMulti",
            "batch=$batchId created=${createdTasks.size} unresolved=${unresolvedReasons.size} downgraded=$downgradedCount"
        )
        emitStatus(summary, autoClear = false)
    }

    private fun emitSingleTaskExtractionTelemetry(transcript: String, result: FastTrackResult) {
        val create = result as? FastTrackResult.CreateTasks ?: return
        val task = create.params.tasks.singleOrNull() ?: return
        val anchor = classifySingleTaskTelemetryAnchor(transcript) ?: return
        val summary = when (anchor) {
            SingleTaskTelemetryAnchor.NOW_OFFSET -> "SIM scheduler single-task NOW_OFFSET extracted"
            SingleTaskTelemetryAnchor.NOW_DAY_OFFSET -> "SIM scheduler single-task NOW_DAY_OFFSET extracted"
        }
        val rawDump = buildString {
            append("title=")
            append(task.title)
            append(", startTimeIso=")
            append(task.startTimeIso)
            append(", transcript=")
            append(transcript)
        }
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.TASK_EXTRACTED,
            payloadSize = 1,
            summary = summary,
            rawDataDump = rawDump
        )
        Log.d(
            "SimSchedulerSingle",
            "anchor=$anchor title=${task.title} start=${task.startTimeIso}"
        )
    }

    private fun classifySingleTaskTelemetryAnchor(transcript: String): SingleTaskTelemetryAnchor? {
        val normalized = transcript.lowercase().replace("：", ":")
        if (
            RelativeTimeResolver.resolveExact(
                userText = normalized,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id
            ) != null
        ) {
            return SingleTaskTelemetryAnchor.NOW_OFFSET
        }
        if (NOW_DAY_OFFSET_REGEX.containsMatchIn(normalized) && CLOCK_HINT_REGEX.containsMatchIn(normalized)) {
            return SingleTaskTelemetryAnchor.NOW_DAY_OFFSET
        }
        return null
    }

    private fun buildDeterministicRelativeCreateCandidate(
        transcript: String,
        normalizedTranscript: String
    ): DeterministicRelativeCreateCandidate? {
        if (looksLikeMultiTaskCreateTranscript(transcript)) return null

        val resolution = RelativeTimeResolver.resolveExact(
            userText = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id
        ) ?: return null

        val strippedTitle = stripDeterministicRelativeTimePhrase(
            transcript = normalizedTranscript,
            matchedText = resolution.matchedText
        ) ?: return null

        return DeterministicRelativeCreateCandidate(
            title = strippedTitle,
            startTimeIso = resolution.startTimeIso,
            matchedText = resolution.matchedText,
            normalizedTranscript = normalizedTranscript
        )
    }

    private fun stripDeterministicRelativeTimePhrase(
        transcript: String,
        matchedText: String
    ): String? {
        val removed = transcript.replaceFirst(matchedText, "")
        val cleaned = removed
            .replace("提醒我", "")
            .replace("请提醒我", "")
            .replace("帮我", "")
            .replace("给我", "")
            .replace("请", "")
            .replace("记得", "")
            .replace("一下", "")
            .replace("去", "")
            .replace("  ", " ")
            .trim()
            .trim('，', ',', '。', '；', ';', '、', ' ')

        return cleaned.takeIf {
            it.isNotBlank() &&
                !it.contains("待会") &&
                !it.contains("过会") &&
                !it.contains("以后想") &&
                !it.contains("之后想")
        }
    }

    private fun looksLikeMultiTaskCreateTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("，", ",", "、", "然后", "再", "以及").any(normalized::contains)
    }

    private suspend fun executeCreateIntent(result: FastTrackResult): MultiTaskExecutionSummary {
        return when (val mutation = fastTrackMutationEngine.execute(result)) {
            is MutationResult.Success -> {
                val createdTasks = mutation.taskIds.mapNotNull { taskRepository.getTask(it) }
                when (result) {
                    is FastTrackResult.CreateTasks,
                    is FastTrackResult.CreateVagueTask -> markCreatedDates(createdTasks)
                    else -> Unit
                }
                if (result is FastTrackResult.CreateTasks) {
                    createdTasks.forEach { scheduleReminderIfExact(it) }
                }
                MultiTaskExecutionSummary(
                    createdTasks = createdTasks,
                    unresolvedReasons = emptyList(),
                    downgradedCount = 0
                )
            }

            is MutationResult.InspirationCreated -> {
                MultiTaskExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf("多任务 create 不应落入灵感分支"),
                    downgradedCount = 0
                )
            }

            is MutationResult.AmbiguousMatch -> {
                MultiTaskExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf("目标不明确，未执行改动"),
                    downgradedCount = 0
                )
            }

            is MutationResult.NoMatch -> {
                MultiTaskExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf(mutation.reason),
                    downgradedCount = 0
                )
            }

            is MutationResult.Error -> {
                MultiTaskExecutionSummary(
                    createdTasks = emptyList(),
                    unresolvedReasons = listOf(mutation.exception.message ?: "日程执行失败"),
                    downgradedCount = 0
                )
            }
        }
    }

    private fun resolveMultiTaskFragment(
        fragment: UniMTaskFragment,
        previous: FragmentAnchorState
    ): ResolvedMultiTaskFragment {
        return when (fragment.anchorKind) {
            UniMAnchorKind.ABSOLUTE -> resolveAbsoluteFragment(fragment)
            UniMAnchorKind.NOW_OFFSET -> resolveNowOffsetFragment(fragment)
            UniMAnchorKind.NOW_DAY_OFFSET -> resolveNowDayOffsetFragment(fragment)
            UniMAnchorKind.PREVIOUS_EXACT_OFFSET -> resolvePreviousExactOffsetFragment(fragment, previous)
            UniMAnchorKind.PREVIOUS_DAY_OFFSET -> resolvePreviousDayOffsetFragment(fragment, previous)
        }
    }

    private fun resolveAbsoluteFragment(fragment: UniMTaskFragment): ResolvedMultiTaskFragment {
        return when (fragment.mode) {
            UniMTaskMode.EXACT -> {
                val startTimeIso = fragment.startTimeIso
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少绝对精确时间")
                val start = parseExactInstant(startTimeIso)
                    ?: return ResolvedMultiTaskFragment.Unresolved("绝对精确时间格式无法解析")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(fragment.title, startTimeIso, fragment.durationMinutes, fragment.urgency),
                    nextState = FragmentAnchorState(
                        exactStart = start,
                        anchorDate = LocalDate.ofInstant(start, timeProvider.zoneId)
                    )
                )
            }
            UniMTaskMode.VAGUE -> {
                val anchorDateIso = fragment.anchorDateIso
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少绝对日期锚点")
                val anchorDate = runCatching { LocalDate.parse(anchorDateIso) }.getOrNull()
                    ?: return ResolvedMultiTaskFragment.Unresolved("绝对日期锚点格式非法")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildVagueCreateResult(fragment.title, anchorDateIso, fragment.timeHint, fragment.urgency),
                    nextState = FragmentAnchorState(
                        exactStart = null,
                        anchorDate = anchorDate
                    )
                )
            }
        }
    }

    private fun resolvePreviousExactOffsetFragment(
        fragment: UniMTaskFragment,
        previous: FragmentAnchorState
    ): ResolvedMultiTaskFragment {
        val offsetMinutes = fragment.relativeOffsetMinutes
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少相对分钟偏移")

        previous.exactStart?.let { previousExact ->
            val targetStart = previousExact.plusSeconds(offsetMinutes.toLong() * 60)
            val startTimeIso = targetStart.atZone(timeProvider.zoneId).toOffsetDateTime().toString()
            return when (fragment.mode) {
                UniMTaskMode.EXACT -> ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(fragment.title, startTimeIso, fragment.durationMinutes, fragment.urgency),
                    nextState = FragmentAnchorState(
                        exactStart = targetStart,
                        anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                    )
                )
                UniMTaskMode.VAGUE -> {
                    val anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                    ResolvedMultiTaskFragment.Resolved(
                        intent = buildVagueCreateResult(fragment.title, anchorDate.toString(), fragment.timeHint, fragment.urgency),
                        nextState = FragmentAnchorState(
                            exactStart = null,
                            anchorDate = anchorDate
                        )
                    )
                }
            }
        }

        previous.anchorDate?.let { anchorDate ->
            return ResolvedMultiTaskFragment.Resolved(
                intent = buildVagueCreateResult(fragment.title, anchorDate.toString(), fragment.timeHint, fragment.urgency),
                nextState = FragmentAnchorState(
                    exactStart = null,
                    anchorDate = anchorDate
                ),
                downgraded = true
            )
        }

        return ResolvedMultiTaskFragment.Unresolved("缺少可用的前序时间锚点")
    }

    private fun resolveNowOffsetFragment(fragment: UniMTaskFragment): ResolvedMultiTaskFragment {
        val offsetMinutes = fragment.relativeOffsetMinutes
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少当前时刻相对分钟偏移")
        val targetStart = timeProvider.now.plusSeconds(offsetMinutes.toLong() * 60)
        val startTimeIso = targetStart.atZone(timeProvider.zoneId).toOffsetDateTime().toString()

        return when (fragment.mode) {
            UniMTaskMode.EXACT -> ResolvedMultiTaskFragment.Resolved(
                intent = buildExactCreateResult(fragment.title, startTimeIso, fragment.durationMinutes, fragment.urgency),
                nextState = FragmentAnchorState(
                    exactStart = targetStart,
                    anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                )
            )
            UniMTaskMode.VAGUE -> {
                val anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildVagueCreateResult(fragment.title, anchorDate.toString(), fragment.timeHint, fragment.urgency),
                    nextState = FragmentAnchorState(
                        exactStart = null,
                        anchorDate = anchorDate
                    )
                )
            }
        }
    }

    private fun resolveNowDayOffsetFragment(fragment: UniMTaskFragment): ResolvedMultiTaskFragment {
        val relativeDayOffset = fragment.relativeDayOffset
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少当前日期相对偏移")
        val targetDate = timeProvider.today.plusDays(relativeDayOffset.toLong())

        return when (fragment.mode) {
            UniMTaskMode.EXACT -> {
                val clockTime = fragment.clockTime
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少当前日期链式精确时钟")
                val localTime = runCatching { LocalTime.parse(clockTime) }.getOrNull()
                    ?: return ResolvedMultiTaskFragment.Unresolved("当前日期链式精确时钟格式非法")
                val startTimeIso = targetDate.atTime(localTime).atZone(timeProvider.zoneId).toOffsetDateTime().toString()
                val start = parseExactInstant(startTimeIso)
                    ?: return ResolvedMultiTaskFragment.Unresolved("当前日期链式精确时间无法解析")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(fragment.title, startTimeIso, fragment.durationMinutes, fragment.urgency),
                    nextState = FragmentAnchorState(
                        exactStart = start,
                        anchorDate = targetDate
                    )
                )
            }
            UniMTaskMode.VAGUE -> ResolvedMultiTaskFragment.Resolved(
                intent = buildVagueCreateResult(fragment.title, targetDate.toString(), fragment.timeHint, fragment.urgency),
                nextState = FragmentAnchorState(
                    exactStart = null,
                    anchorDate = targetDate
                )
            )
        }
    }

    private fun resolvePreviousDayOffsetFragment(
        fragment: UniMTaskFragment,
        previous: FragmentAnchorState
    ): ResolvedMultiTaskFragment {
        val previousDate = previous.anchorDate
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少可用的前序日期锚点")
        val relativeDayOffset = fragment.relativeDayOffset
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少相对日期偏移")
        val targetDate = previousDate.plusDays(relativeDayOffset.toLong())

        return when (fragment.mode) {
            UniMTaskMode.EXACT -> {
                val clockTime = fragment.clockTime
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少链式精确时钟")
                val localTime = runCatching { LocalTime.parse(clockTime) }.getOrNull()
                    ?: return ResolvedMultiTaskFragment.Unresolved("链式精确时钟格式非法")
                val startTimeIso = targetDate.atTime(localTime).atZone(timeProvider.zoneId).toOffsetDateTime().toString()
                val start = parseExactInstant(startTimeIso)
                    ?: return ResolvedMultiTaskFragment.Unresolved("链式精确时间无法解析")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(fragment.title, startTimeIso, fragment.durationMinutes, fragment.urgency),
                    nextState = FragmentAnchorState(
                        exactStart = start,
                        anchorDate = targetDate
                    )
                )
            }
            UniMTaskMode.VAGUE -> ResolvedMultiTaskFragment.Resolved(
                intent = buildVagueCreateResult(fragment.title, targetDate.toString(), fragment.timeHint, fragment.urgency),
                nextState = FragmentAnchorState(
                    exactStart = null,
                    anchorDate = targetDate
                )
            )
        }
    }

    private fun buildExactCreateResult(
        title: String,
        startTimeIso: String,
        durationMinutes: Int,
        urgency: com.smartsales.prism.domain.scheduler.UrgencyEnum
    ): FastTrackResult {
        return FastTrackResult.CreateTasks(
            params = CreateTasksParams(
                unifiedId = UUID.randomUUID().toString(),
                tasks = listOf(
                    TaskDefinition(
                        title = title,
                        startTimeIso = startTimeIso,
                        durationMinutes = durationMinutes,
                        urgency = urgency
                    )
                )
            )
        )
    }

    private fun buildVagueCreateResult(
        title: String,
        anchorDateIso: String,
        timeHint: String?,
        urgency: com.smartsales.prism.domain.scheduler.UrgencyEnum
    ): FastTrackResult {
        return FastTrackResult.CreateVagueTask(
            params = CreateVagueTaskParams(
                unifiedId = UUID.randomUUID().toString(),
                title = title,
                anchorDateIso = anchorDateIso,
                timeHint = timeHint,
                urgency = urgency
            )
        )
    }

    private fun applyAggregatedConflictState(createdTasks: List<ScheduledTask>) {
        createdTasks.firstOrNull { it.hasConflict }?.let(::markConflict)
        if (createdTasks.none { it.hasConflict }) {
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
        }
    }

    private fun buildMultiTaskStatus(
        createdCount: Int,
        unresolvedCount: Int,
        downgradedCount: Int
    ): String {
        return buildString {
            append("已创建 ")
            append(createdCount)
            append(" 个日程")
            if (unresolvedCount > 0) {
                append("，")
                append(unresolvedCount)
                append(" 个片段未创建")
            }
            if (downgradedCount > 0) {
                append("，")
                append(downgradedCount)
                append(" 个片段已按待定处理")
            }
        }
    }

    private fun buildRescheduleExitMotion(
        original: ScheduledTask,
        updatedTask: ScheduledTask,
        sourceOffset: Int,
        destinationOffset: Int
    ): RescheduleExitMotion? {
        if (sourceOffset != activeDayOffset.value) return null
        if (original.startTime == updatedTask.startTime) return null

        val direction = when {
            destinationOffset > sourceOffset -> ExitDirection.RIGHT
            destinationOffset < sourceOffset -> ExitDirection.LEFT
            updatedTask.startTime > original.startTime -> ExitDirection.RIGHT
            updatedTask.startTime < original.startTime -> ExitDirection.LEFT
            else -> return null
        }

        return RescheduleExitMotion(
            renderKey = "${original.id}:exit:${UUID.randomUUID()}",
            sourceTaskId = original.id,
            sourceDayOffset = sourceOffset,
            snapshot = original,
            exitDirection = direction
        )
    }

    private fun armExitMotion(
        motion: RescheduleExitMotion,
        sourceOffset: Int,
        destinationOffset: Int
    ) {
        _exitingTasks.value = _exitingTasks.value + motion
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = 1,
            summary = "SIM scheduler reschedule exit motion armed",
            rawDataDump = "taskId=${motion.sourceTaskId}, sourceDay=$sourceOffset, destinationDay=$destinationOffset, direction=${motion.exitDirection}"
        )
        Log.d(
            "SimSchedulerMotion",
            "taskId=${motion.sourceTaskId} sourceDay=$sourceOffset destinationDay=$destinationOffset direction=${motion.exitDirection}"
        )
        viewModelScope.launch {
            delay(420)
            _exitingTasks.value = _exitingTasks.value.filterNot { it.renderKey == motion.renderKey }
        }
    }

    private suspend fun handleMutation(result: FastTrackResult) {
        when (val mutation = fastTrackMutationEngine.execute(result)) {
            is MutationResult.Success -> {
                clearFailureState()
                val createdTasks = mutation.taskIds.mapNotNull { taskRepository.getTask(it) }
                when (result) {
                    is FastTrackResult.CreateTasks,
                    is FastTrackResult.CreateVagueTask -> markCreatedDates(createdTasks)
                    else -> Unit
                }
                if (result is FastTrackResult.CreateTasks) {
                    createdTasks.forEach { scheduleReminderIfExact(it) }
                }
                createdTasks.firstOrNull { it.hasConflict }?.let(::markConflict)
                if (createdTasks.none { it.hasConflict }) {
                    _conflictedTaskIds.value = emptySet()
                    _causingTaskId.value = null
                }
                emitStatus(
                    when {
                        createdTasks.any { it.hasConflict } -> "已创建日程，存在冲突"
                        createdTasks.any { it.isVague } -> "已加入待定日程"
                        else -> "已创建日程"
                    }
                )
            }

            is MutationResult.InspirationCreated -> {
                clearFailureState()
                emitStatus("已加入灵感箱")
            }

            is MutationResult.AmbiguousMatch -> {
                emitFailure("目标不明确，未执行改动")
            }

            is MutationResult.NoMatch -> {
                emitFailure(mutation.reason)
            }

            is MutationResult.Error -> {
                emitFailure(mutation.exception.message ?: "日程执行失败")
            }
        }
    }

    private fun markConflict(task: ScheduledTask) {
        if (!task.hasConflict) {
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
            return
        }
        _conflictWarning.value = task.conflictSummary ?: "当前日程存在时间冲突"
        _conflictedTaskIds.value = buildSet {
            add(task.id)
            task.conflictWithTaskId?.let(::add)
        }
        _causingTaskId.value = task.id
    }

    private fun markRescheduledDate(start: Instant) {
        val offset = dayOffsetFor(start)
        _rescheduledDates.value = _rescheduledDates.value + offset
        _unacknowledgedDates.value = _unacknowledgedDates.value + offset
    }

    private fun markCreatedDates(tasks: List<ScheduledTask>) {
        val warningOffsets = linkedSetOf<Int>()
        val normalOffsets = linkedSetOf<Int>()

        tasks.forEach { task ->
            val offset = dayOffsetFor(task.startTime)
            if (offset == activeDayOffset.value) return@forEach

            normalOffsets += offset
            if (task.hasConflict) {
                warningOffsets += offset
            }
        }

        if (normalOffsets.isEmpty()) return

        _unacknowledgedDates.value = _unacknowledgedDates.value + normalOffsets
        if (warningOffsets.isNotEmpty()) {
            _rescheduledDates.value = _rescheduledDates.value + warningOffsets
        }
    }

    private fun dayOffsetFor(start: Instant): Int {
        return java.time.LocalDate.ofInstant(start, timeProvider.zoneId)
            .toEpochDay()
            .minus(timeProvider.today.toEpochDay())
            .toInt()
    }

    private fun displayedDateIso(): String {
        return timeProvider.today.plusDays(activeDayOffset.value.toLong()).toString()
    }

    private suspend fun scheduleReminderIfExact(task: ScheduledTask) {
        if (task.isVague || task.isDone) return

        val cascade = task.alarmCascade.ifEmpty {
            UrgencyLevel.buildCascade(task.urgencyLevel)
        }
        if (cascade.isEmpty()) return

        if (exactAlarmPermissionGate.shouldPromptForExactAlarm()) {
            _exactAlarmPermissionNeeded.tryEmit(Unit)
        }

        runCatching {
            alarmScheduler.scheduleCascade(
                taskId = task.id,
                taskTitle = task.title,
                eventTime = task.startTime,
                cascade = cascade
            )
        }.onFailure { error ->
            Log.w("SimSchedulerAlarm", "schedule reminder failed for task=${task.id}: ${error.message}")
        }
    }

    private suspend fun cancelReminderSafely(taskId: String) {
        runCatching {
            alarmScheduler.cancelReminder(taskId)
        }.onFailure { error ->
            Log.w("SimSchedulerAlarm", "cancel reminder failed for task=$taskId: ${error.message}")
        }
    }

    private fun parseExactInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrNull()
    }

    private fun emitStatus(message: String, autoClear: Boolean = true) {
        _pipelineStatus.value = message
        pipelineStatusResetJob?.cancel()
        if (autoClear) {
            pipelineStatusResetJob = viewModelScope.launch {
                delay(1600)
                if (_pipelineStatus.value == message) {
                    _pipelineStatus.value = null
                }
            }
        }
    }

    private fun emitFailure(message: String) {
        _conflictWarning.value = message
        _pipelineStatus.value = message
    }

    private fun clearFailureState() {
        _conflictWarning.value = null
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
    }

    private suspend fun handleVoiceRescheduleTranscript(transcript: String) {
        val targetCue = extractRescheduleTargetCue(transcript)
        val timeInstruction = extractRescheduleTimeInstruction(transcript)
            ?: return emitFailure("SIM 当前仅支持明确时间改期")

        when (val resolution = scheduleBoard.resolveTarget(targetCue, activeDayOffset.value)) {
            is com.smartsales.prism.domain.memory.TargetResolution.Resolved -> {
                val original = taskRepository.getTask(resolution.item.entryId)
                    ?: return emitFailure("找不到要改期的日程")
                executeResolvedReschedule(original, timeInstruction)
            }

            is com.smartsales.prism.domain.memory.TargetResolution.Ambiguous -> {
                emitFailure("目标不明确，未执行改动")
            }

            is com.smartsales.prism.domain.memory.TargetResolution.NoMatch -> {
                emitFailure("未找到匹配的日程，请更具体一些。")
            }
        }
    }

    private suspend fun executeResolvedReschedule(original: ScheduledTask, timeInstruction: String) {
        val resolvedTime = SimRescheduleTimeInterpreter.resolve(
            originalTask = original,
            transcript = timeInstruction,
            displayedDateIso = displayedDateIso(),
            timeProvider = timeProvider,
            uniAExtractionService = uniAExtractionService
        )
        val resolved = when (resolvedTime) {
            is SimRescheduleTimeInterpreter.Result.Success -> resolvedTime
            SimRescheduleTimeInterpreter.Result.Unsupported -> {
                emitFailure("SIM 当前仅支持明确时间改期")
                return
            }
            SimRescheduleTimeInterpreter.Result.InvalidExactTime -> {
                emitFailure("改期时间格式无法解析")
                return
            }
        }

        val newStart = resolved.startTime
        val newDuration = resolved.durationMinutes ?: original.durationMinutes
        val sourceOffset = dayOffsetFor(original.startTime)
        val destinationOffset = dayOffsetFor(newStart)
        val conflict = scheduleBoard.checkConflict(
            proposedStart = newStart.toEpochMilli(),
            durationMinutes = newDuration,
            excludeId = original.id
        ) as? ConflictResult.Conflict

        val updatedTask = original.copy(
            startTime = newStart,
            durationMinutes = newDuration,
            hasConflict = conflict != null,
            conflictWithTaskId = conflict?.overlaps?.firstOrNull()?.entryId,
            conflictSummary = conflict?.overlaps?.firstOrNull()?.let { "与「${it.title}」时间冲突" },
            isVague = false
        )

        taskRepository.rescheduleTask(original.id, updatedTask)
        cancelReminderSafely(original.id)
        scheduleReminderIfExact(updatedTask)
        markConflict(updatedTask)
        if (destinationOffset != activeDayOffset.value) {
            markRescheduledDate(newStart)
        }
        buildRescheduleExitMotion(
            original = original,
            updatedTask = updatedTask,
            sourceOffset = sourceOffset,
            destinationOffset = destinationOffset
        )?.let { armExitMotion(it, sourceOffset, destinationOffset) }
        emitStatus(if (updatedTask.hasConflict) "已改期，存在冲突" else "已改期")
    }

    private fun extractRescheduleTargetCue(text: String): String {
        val trimmed = text.trim()
        val quoted = Regex("[“\"]([^”\"]+)[”\"]").find(trimmed)?.groupValues?.getOrNull(1)
        if (!quoted.isNullOrBlank()) return quoted

        val keywordRange = findRescheduleKeywordRange(trimmed)
        val prefix = keywordRange?.let { trimmed.substring(0, it.first) } ?: trimmed
        return prefix
            .replace("把", " ")
            .replace("请把", " ")
            .replace("帮我把", " ")
            .replace("给我把", " ")
            .replace("那个", " ")
            .replace("这个", " ")
            .replace("一下", " ")
            .replace("  ", " ")
            .trim()
            .trim('“', '”', '"', '\'', '，', ',', '。', ' ')
            .removeSuffix("的时间")
            .removeSuffix("时间")
            .trim()
    }

    private fun extractRescheduleTimeInstruction(text: String): String? {
        val keywordRange = findRescheduleKeywordRange(text) ?: return null
        return text.substring(keywordRange.first)
            .trim()
            .takeIf { it.length >= 2 }
    }

    private fun findRescheduleKeywordRange(text: String): IntRange? {
        val normalized = text.lowercase()
        return RESCHEDULE_KEYWORDS
            .mapNotNull { keyword ->
                normalized.indexOf(keyword)
                    .takeIf { it >= 0 }
                    ?.let { it until (it + keyword.length) }
            }
            .minByOrNull { it.first }
    }

    private fun looksLikeRescheduleTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return RESCHEDULE_KEYWORDS.any { normalized.contains(it) } || normalized.contains("actually")
    }

    private fun looksLikeDeletionTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("删除", "取消", "删掉", "delete", "cancel")
            .any { normalized.contains(it) }
    }
}
