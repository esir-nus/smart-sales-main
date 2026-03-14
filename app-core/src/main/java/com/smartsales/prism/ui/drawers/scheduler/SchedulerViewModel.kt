package com.smartsales.prism.ui.drawers.scheduler

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.notification.OemCompat
import com.smartsales.prism.domain.mapper.TaskMemoryMapper
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerCoordinator
import com.smartsales.prism.domain.scheduler.TipGenerator
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Pure UI Presentation State Manager for the Scheduler Drawer.
 * 
 * Adheres strictly to the Data-Oriented OS Model Layer 4 boundary.
 * All complex business logic (AST interception, audio pipeline triggers, conflict resolution math)
 * has been stripped and delegated to [SchedulerCoordinator] and the UnifiedPipeline.
 * 
 * Employs Reactive Unification (`Flow.combine`) to synthesize active actionable tasks 
 * with crossed-off factual memory entries, maintaining a seamless UI timeline.
 */
@HiltViewModel
class SchedulerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val taskRepository: ScheduledTaskRepository,
    private val memoryRepository: MemoryRepository,
    private val inspirationRepository: InspirationRepository,
    private val coordinator: SchedulerCoordinator,
    private val tipGenerator: TipGenerator,
    private val asrService: com.smartsales.prism.domain.asr.AsrService,
    private val intentOrchestrator: com.smartsales.core.pipeline.IntentOrchestrator
) : ViewModel(), ISchedulerViewModel {

    // --- State Streams ---

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

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val exactAlarmPermissionNeeded = _exactAlarmPermissionNeeded.asSharedFlow()
    private val exactAlarmPrompted = AtomicBoolean(false)

    // Wave 9: Smart Tips — 提示缓存（taskId → List<String>）
    private val _tipsCache = mutableMapOf<String, List<String>>()

    // Manually trigger refresh actions if needed downstream
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    init {
        // Force the coordinator to sweep expired tasks on mount
        viewModelScope.launch { coordinator.autoCompleteExpiredTasks() }
    }

    /**
     * Wave 8 (Phase 3) Unification UI Stream:
     * Combines Active Tasks from TaskRepo + Crossed-Off Memories from MemoryRepo.
     * Maps them seamlessly for Compose to render the checkmark states correctly.
     */
    override val timelineItems: StateFlow<List<SchedulerTimelineItem>> = combine(
        combine(_activeDayOffset, _refreshTrigger.asSharedFlow()) { offset, _ -> offset }
            .flatMapLatest { offset -> taskRepository.getTimelineItems(offset) },
        combine(_activeDayOffset, _refreshTrigger.asSharedFlow()) { offset, _ -> offset }
            .flatMapLatest { offset ->
                val date = LocalDate.now().plusDays(offset.toLong())
                val startOfDayMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endOfDayMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                // Retrieve all memories for this day that are schedule items
                memoryRepository.observeByTypeAndDateRange(MemoryEntryType.SCHEDULE_ITEM, startOfDayMs, endOfDayMs)
            },
        inspirationRepository.getAll()
    ) { activeTasks, factualMemories, inspirations ->
        
        // Downcast MemoryEntries back to the UI TimelineItemModel constraints
        val crossedOffTasks = factualMemories.map { memory ->
            ScheduledTask(
                id = memory.entryId,
                timeDisplay = "✓", // Completed indicators
                title = memory.title ?: memory.content,
                startTime = Instant.ofEpochMilli(memory.scheduledAt ?: memory.createdAt),
                endTime = null, 
                durationMinutes = 60, // Fallback placeholder
                isDone = true, // Force visually crossed-off
                hasAlarm = false,
                alarmCascade = emptyList(),
                keyPersonEntityId = null // We don't hydrate full CRM links on completed tasks in timeline
            )
        }
        
        // CRASH FIX: Deduplicate. If a task exists in both Active and Crossed-Off lists due to Flow emission timing,
        // drop the Actionable version to prevent Duplicate Key crashes in Compose.
        val crossedOffIds = crossedOffTasks.map { it.id }.toSet()
        val filteredActiveTasks = activeTasks.filter { it.id !in crossedOffIds }
        
        val combinedTasks = (filteredActiveTasks + crossedOffTasks).sortedBy { 
            if (it is ScheduledTask) it.startTime else Instant.MAX 
        }
        
        inspirations + combinedTasks
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Actions ---

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
        if (!enabled) _selectedInspirationIds.value = emptySet()
    }

    override fun toggleItemSelection(id: String) {
        val current = _selectedInspirationIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedInspirationIds.value = current
    }

    override fun deleteItem(id: String) {
        viewModelScope.launch {
            taskRepository.deleteItem(id)
            _tipsCache.remove(id)
            _tipsLoading.value -= id
            _refreshTrigger.tryEmit(Unit)
        }
    }

    override fun onItemClick(id: String) {
        android.util.Log.d("SchedulerVM", "Item clicked: $id")
    }

    /**
     * Cross-Off Lifecycle (Phase 3): 
     * Replaces the old ViewModel pipeline callback with pure OS-Model persistence triggers.
     * Checking an item: Converts to MemoryEntry, inserts to MemoryRepo, deletes from TaskRepo.
     * Unchecking (Undo): Restores to TaskRepo, deletes from MemoryRepo.
     */
    override fun toggleDone(taskId: String) {
        viewModelScope.launch {
            // Check Actionable first:
            val activeTask = taskRepository.getTask(taskId)
            
            if (activeTask != null) {
                // If it's active, we are checking it off.
                val memoryEntry = TaskMemoryMapper.toMemoryEntry(activeTask)
                memoryRepository.save(memoryEntry)
                taskRepository.deleteItem(taskId)
                android.util.Log.d("SchedulerVM", "toggleDone: id=$taskId migrated to Factual Memory.")
                _refreshTrigger.tryEmit(Unit)
            } else {
                android.util.Log.d("SchedulerVM", "toggleDone: Task $taskId not found in active tasks. Unchecking completed memories is not supported by design.")
            }
        }
    }

    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }

    override fun deleteInspiration(id: String) {
        viewModelScope.launch { inspirationRepository.delete(id) }
    }

    override fun toggleConflictExpansion(id: String) {
        val current = _expandedConflictIds.value
        _expandedConflictIds.value = if (current.contains(id)) current - id else current + id
    }

    override fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution) {
        // Send conflict resolution action to Coordinator or Pipeline to process.
        // Coordinator requires text/context usually, but if UI is handling pure resolutions
        // we delegate the action objects.
        // Clearing local UI state for now:
        _conflictWarning.value = null
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
    }

    override fun onReschedule(taskId: String, text: String) {
        val conflictGroup = _conflictedTaskIds.value
        if (taskId in conflictGroup && conflictGroup.size > 1) {
            coordinator.resolveConflictGroup(conflictGroup, text)
            // Clear conflict state
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
        } else {
            // Single task reschedule is handled by Audio/Pipeline externally now, 
            // or we delegate it to another pure Intent Orchestrator function.
            android.util.Log.d("SchedulerVM", "onReschedule: Pipeline routing decoupled from UI.")
        }
    }

    override fun getCachedTips(taskId: String): List<String> = _tipsCache[taskId] ?: emptyList()

    override fun processAudio(file: java.io.File) {
        _pipelineStatus.value = "🎙️ 语音转写中..."
        viewModelScope.launch {
            try {
                val result = asrService.transcribe(file)
                if (result is com.smartsales.prism.domain.asr.AsrResult.Success) {
                    _pipelineStatus.value = "处理意图..."
                    intentOrchestrator.processInput(result.text, isVoice = true).collect { res ->
                        // Add some rudimentary UI feedback
                        if (res is com.smartsales.core.pipeline.PipelineResult.ConversationalReply) {
                            _pipelineStatus.value = "✅ 搞定"
                        } else if (res is com.smartsales.core.pipeline.PipelineResult.MutationProposal) {
                            _pipelineStatus.value = "请确认执行"
                        }
                    }
                } else if (result is com.smartsales.prism.domain.asr.AsrResult.Error) {
                    android.util.Log.e("SchedulerVM", "Transcribe Result Error: ${result.message}")
                    _pipelineStatus.value = "转写失败: ${result.message}"
                }
            } catch (e: Exception) {
                android.util.Log.e("SchedulerVM", "ProcessAudio crash: ", e)
                _pipelineStatus.value = "系统错误"
            } finally {
                kotlinx.coroutines.delay(2000)
                _pipelineStatus.value = null
            }
        }
    }
}
