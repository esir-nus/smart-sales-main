package com.smartsales.prism.ui.drawers.scheduler

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerCoordinator
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.TipGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Pure UI Presentation State Manager for the Scheduler Drawer.
 *
 * Wave 3D keeps this file as a legacy compatibility host only.
 * Heavy projection, mutation, and audio ingress responsibilities live in stable support files.
 */
@HiltViewModel
class SchedulerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val taskRepository: ScheduledTaskRepository,
    private val memoryRepository: MemoryRepository,
    private val inspirationRepository: InspirationRepository,
    private val coordinator: SchedulerCoordinator,
    private val tipGenerator: TipGenerator,
    private val asrService: AsrService,
    private val intentOrchestrator: IntentOrchestrator,
    private val toolRegistry: ToolRegistry
) : ViewModel(), ISchedulerViewModel {

    private val _activeDayOffset = MutableStateFlow(0)
    override val activeDayOffset: StateFlow<Int> = _activeDayOffset.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    override val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedInspirationIds = MutableStateFlow(setOf<String>())
    override val selectedInspirationIds: StateFlow<Set<String>> = _selectedInspirationIds.asStateFlow()

    private val _tipsLoading = MutableStateFlow<Set<String>>(emptySet())
    override val tipsLoading: StateFlow<Set<String>> = _tipsLoading.asStateFlow()

    private val _pipelineStatus = MutableStateFlow<String?>(null)
    override val pipelineStatus: StateFlow<String?> = _pipelineStatus.asStateFlow()

    private val _unacknowledgedDates = MutableStateFlow<Set<Int>>(emptySet())
    override val unacknowledgedDates: StateFlow<Set<Int>> = _unacknowledgedDates.asStateFlow()

    private val _rescheduledDates = MutableStateFlow<Set<Int>>(emptySet())
    override val rescheduledDates: StateFlow<Set<Int>> = _rescheduledDates.asStateFlow()

    private val _conflictWarning = MutableStateFlow<String?>(null)
    override val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    private val _conflictedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    override val conflictedTaskIds: StateFlow<Set<String>> = _conflictedTaskIds.asStateFlow()

    private val _causingTaskId = MutableStateFlow<String?>(null)
    override val causingTaskId: StateFlow<String?> = _causingTaskId.asStateFlow()

    private val _isInspirationsExpanded = MutableStateFlow(false)
    override val isInspirationsExpanded: StateFlow<Boolean> = _isInspirationsExpanded.asStateFlow()

    private val _expandedConflictIds = MutableStateFlow<Set<String>>(emptySet())
    override val expandedConflictIds: StateFlow<Set<String>> = _expandedConflictIds.asStateFlow()

    private val _exitingTasks = MutableStateFlow<List<RescheduleExitMotion>>(emptyList())
    override val exitingTasks: StateFlow<List<RescheduleExitMotion>> = _exitingTasks.asStateFlow()

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val exactAlarmPermissionNeeded: SharedFlow<Unit> = _exactAlarmPermissionNeeded.asSharedFlow()
    private val exactAlarmPrompted = AtomicBoolean(false)

    private val _tipsCache = mutableMapOf<String, List<String>>()
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    private val projectionSupport = SchedulerViewModelProjectionSupport(
        scope = viewModelScope,
        taskRepository = taskRepository,
        memoryRepository = memoryRepository,
        inspirationRepository = inspirationRepository
    )

    private val legacyActions = SchedulerViewModelLegacyActions(
        taskRepository = taskRepository,
        memoryRepository = memoryRepository,
        inspirationRepository = inspirationRepository,
        coordinator = coordinator,
        getTipsLoading = { _tipsLoading.value },
        setTipsLoading = { _tipsLoading.value = it },
        tipsCache = _tipsCache,
        emitRefresh = { _refreshTrigger.tryEmit(Unit) },
        getConflictedTaskIds = { _conflictedTaskIds.value },
        clearConflictState = {
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
        }
    )

    private val audioIngressCoordinator = SchedulerViewModelAudioIngressCoordinator(
        scope = viewModelScope,
        asrService = asrService,
        intentOrchestrator = intentOrchestrator,
        bridge = SchedulerViewModelAudioBridge(
            getActiveDayOffset = { _activeDayOffset.value },
            getPipelineStatus = { _pipelineStatus.value },
            setPipelineStatus = { _pipelineStatus.value = it }
        )
    )

    override val timelineItems: StateFlow<List<SchedulerTimelineItem>> =
        projectionSupport.buildTimelineItems(
            activeDayOffset = _activeDayOffset,
            refreshTrigger = _refreshTrigger.asSharedFlow()
        )

    override val topUrgentTasks: StateFlow<List<ScheduledTask>> =
        projectionSupport.buildTopUrgentTasks()

    init {
        viewModelScope.launch { coordinator.autoCompleteExpiredTasks() }
        viewModelScope.launch {
            SchedulerDevInjectionBridge.requests.collect { request ->
                SchedulerDevInjectionBridge.consume(request)
                injectTranscript(
                    text = request.text,
                    displayedDateIso = request.displayedDateIso,
                    source = request.source
                )
            }
        }
    }

    override fun onDateSelected(dayOffset: Int) {
        _activeDayOffset.value = dayOffset
        acknowledgeDate(dayOffset)
        viewModelScope.launch { coordinator.autoCompleteExpiredTasks() }
    }

    override fun acknowledgeDate(dayOffset: Int) {
        _unacknowledgedDates.value -= dayOffset
        _rescheduledDates.value -= dayOffset
    }

    override fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedInspirationIds.value = emptySet()
        }
    }

    override fun toggleItemSelection(id: String) {
        val current = _selectedInspirationIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedInspirationIds.value = current
    }

    override fun deleteItem(id: String) {
        viewModelScope.launch {
            legacyActions.deleteItem(id)
        }
    }

    override fun onItemClick(id: String) {
        android.util.Log.d("SchedulerVM", "Item clicked: $id")
    }

    override fun toggleDone(taskId: String) {
        viewModelScope.launch {
            legacyActions.toggleDone(taskId)
        }
    }

    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }

    override fun deleteInspiration(id: String) {
        viewModelScope.launch {
            legacyActions.deleteInspiration(id)
        }
    }

    override fun toggleConflictExpansion(id: String) {
        val current = _expandedConflictIds.value
        _expandedConflictIds.value = if (current.contains(id)) current - id else current + id
    }

    override fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution) {
        legacyActions.handleConflictResolution(resolution)
    }

    override fun onReschedule(taskId: String, text: String) {
        legacyActions.onReschedule(taskId, text)
    }

    override fun getCachedTips(taskId: String): List<String> = _tipsCache[taskId] ?: emptyList()

    override fun processAudio(file: File) {
        audioIngressCoordinator.processAudio(file)
    }

    override fun injectTranscript(
        text: String,
        displayedDateIso: String?,
        source: DevInjectSource
    ) {
        audioIngressCoordinator.injectTranscript(
            text = text,
            displayedDateIso = displayedDateIso,
            source = source
        )
    }
}
