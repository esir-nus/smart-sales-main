package com.smartsales.prism.ui.sim

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.TaskCreationBadgeSignal
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.prism.data.notification.ExactAlarmPermissionGate
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.ConflictResolution
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.SchedulerReminderSurfaceBus
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.RescheduleExitMotion
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * SIM 调度 ViewModel。
 * 仅保留壳层状态暴露与公开入口，重逻辑下沉到稳定的支持协作器。
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SimSchedulerViewModel @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val inspirationRepository: InspirationRepository,
    private val scheduleBoard: ScheduleBoard,
    private val activeTaskRetrievalIndex: ActiveTaskRetrievalIndex,
    private val alarmScheduler: AlarmScheduler,
    private val exactAlarmPermissionGate: ExactAlarmPermissionGate,
    private val fastTrackMutationEngine: FastTrackMutationEngine,
    private val asrService: AsrService,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService,
    private val uniMExtractionService: RealUniMExtractionService,
    private val uniAExtractionService: RealUniAExtractionService,
    private val uniBExtractionService: RealUniBExtractionService,
    private val uniCExtractionService: RealUniCExtractionService,
    private val timeProvider: TimeProvider,
    private val taskCreationBadgeSignal: TaskCreationBadgeSignal
) : ViewModel(), ISchedulerViewModel {

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

    private val _activeReminderBanner = MutableStateFlow<SimReminderBannerState?>(null)
    internal val activeReminderBanner: StateFlow<SimReminderBannerState?> = _activeReminderBanner.asStateFlow()

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val exactAlarmPermissionNeeded: SharedFlow<Unit> = _exactAlarmPermissionNeeded.asSharedFlow()

    private var pipelineStatusResetJob: Job? = null
    private var reminderBannerDismissJob: Job? = null

    private val bridge = SimSchedulerUiBridge(
        getActiveDayOffset = { _activeDayOffset.value },
        getUnacknowledgedDates = { _unacknowledgedDates.value },
        setUnacknowledgedDates = { _unacknowledgedDates.value = it },
        getRescheduledDates = { _rescheduledDates.value },
        setRescheduledDates = { _rescheduledDates.value = it },
        setConflictWarning = { _conflictWarning.value = it },
        setConflictedTaskIds = { _conflictedTaskIds.value = it },
        setCausingTaskId = { _causingTaskId.value = it },
        getExitingTasks = { _exitingTasks.value },
        setExitingTasks = { _exitingTasks.value = it },
        getPipelineStatus = { _pipelineStatus.value },
        setPipelineStatus = { _pipelineStatus.value = it },
        getPipelineStatusResetJob = { pipelineStatusResetJob },
        setPipelineStatusResetJob = { pipelineStatusResetJob = it },
        emitExactAlarmPermissionNeeded = { _exactAlarmPermissionNeeded.tryEmit(Unit) }
    )

    private val projectionSupport = SimSchedulerProjectionSupport(
        scope = viewModelScope,
        timeProvider = timeProvider,
        bridge = bridge
    )
    private val reminderSupport = SimSchedulerReminderSupport(
        alarmScheduler = alarmScheduler,
        exactAlarmPermissionGate = exactAlarmPermissionGate,
        bridge = bridge
    )
    private val mutationCoordinator = SimSchedulerMutationCoordinator(
        taskRepository = taskRepository,
        scheduleBoard = scheduleBoard,
        fastTrackMutationEngine = fastTrackMutationEngine,
        uniAExtractionService = uniAExtractionService,
        timeProvider = timeProvider,
        projectionSupport = projectionSupport,
        reminderSupport = reminderSupport,
        taskCreationBadgeSignal = taskCreationBadgeSignal
    )
    private val ingressCoordinator = SimSchedulerIngressCoordinator(
        taskRepository = taskRepository,
        scheduleBoard = scheduleBoard,
        activeTaskRetrievalIndex = activeTaskRetrievalIndex,
        globalRescheduleExtractionService = globalRescheduleExtractionService,
        uniMExtractionService = uniMExtractionService,
        uniAExtractionService = uniAExtractionService,
        uniBExtractionService = uniBExtractionService,
        uniCExtractionService = uniCExtractionService,
        timeProvider = timeProvider,
        projectionSupport = projectionSupport,
        mutationCoordinator = mutationCoordinator
    )

    override val topUrgentTasks: StateFlow<List<ScheduledTask>> =
        projectionSupport.buildTopUrgentTasks(taskRepository, _pipelineStatus)

    override val timelineItems: StateFlow<List<SchedulerTimelineItem>> =
        projectionSupport.buildTimelineItems(taskRepository, inspirationRepository, _activeDayOffset)

    init {
        viewModelScope.launch {
            SchedulerReminderSurfaceBus.events.collect { event ->
                val task = taskRepository.getTask(event.taskId)
                val entry = buildSimReminderBannerEntry(
                    task = task,
                    taskId = event.taskId,
                    title = event.taskTitle,
                    offsetMinutes = event.offsetMinutes,
                    emittedAtMillis = event.emittedAtMillis
                )
                val mergedEntries = mergeSimReminderBannerEntries(
                    existing = _activeReminderBanner.value?.entries.orEmpty(),
                    incoming = entry
                )
                _activeReminderBanner.value = SimReminderBannerState(mergedEntries)
                reminderBannerDismissJob?.cancel()
                reminderBannerDismissJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(5_000L)
                    _activeReminderBanner.value = null
                }
            }
        }
    }

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
            reminderSupport.cancelReminderSafely(id)
            projectionSupport.emitStatus("已删除日程")
        }
    }

    override fun onItemClick(id: String) {
        projectionSupport.clearFailureState()
    }

    override fun toggleDone(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId) ?: return@launch
            val updatedTask = task.copy(isDone = !task.isDone)
            taskRepository.updateTask(updatedTask)
            if (updatedTask.isDone) {
                reminderSupport.cancelReminderSafely(taskId)
            }
            projectionSupport.emitStatus(if (task.isDone) "已恢复为待办" else "已标记完成")
        }
    }

    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }

    override fun deleteInspiration(id: String) {
        viewModelScope.launch {
            inspirationRepository.delete(id)
            projectionSupport.emitStatus("已删除灵感")
        }
    }

    override fun toggleConflictExpansion(id: String) {
        val next = _expandedConflictIds.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _expandedConflictIds.value = next
    }

    override fun handleConflictResolution(resolution: ConflictResolution) {
        projectionSupport.clearFailureState()
        _expandedConflictIds.value = emptySet()
    }

    override fun onReschedule(taskId: String, text: String) {
        viewModelScope.launch {
            projectionSupport.clearFailureState()
            val original = taskRepository.getTask(taskId)
                ?: return@launch projectionSupport.emitFailure("找不到要改期的日程")
            mutationCoordinator.executeResolvedReschedule(original, text)
        }
    }

    override fun getCachedTips(taskId: String): List<String> = emptyList()

    override fun processAudio(file: File) {
        viewModelScope.launch {
            projectionSupport.clearFailureState()
            projectionSupport.emitStatus("语音转写中...", autoClear = false)
            when (val transcriptResult = asrService.transcribe(file)) {
                is AsrResult.Success -> {
                    val transcript = transcriptResult.text.trim()
                    Log.d(
                        "SimSchedulerViewModel",
                        buildSimSchedulerTranscriptLog(
                            transcript = transcript,
                            source = "scheduler_rec_asr"
                        )
                    )
                    ingressCoordinator.processTranscript(transcript)
                }
                is AsrResult.Error -> projectionSupport.emitFailure("录音转写失败: ${transcriptResult.message}")
            }
        }
    }

    override fun submitDebugTranscript(transcript: String) {
        viewModelScope.launch {
            projectionSupport.clearFailureState()
            projectionSupport.emitStatus("调试输入处理中...", autoClear = false)
            Log.d(
                "SimSchedulerViewModel",
                buildSimSchedulerTranscriptLog(
                    transcript = transcript,
                    source = "scheduler_debug_button"
                )
            )
            ingressCoordinator.processTranscript(transcript)
        }
    }

    internal suspend fun applyFastTrackResultForTesting(result: FastTrackResult) {
        mutationCoordinator.handleMutation(result)
    }

    internal fun dismissActiveReminderBanner() {
        reminderBannerDismissJob?.cancel()
        _activeReminderBanner.value = null
    }
}
