package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Fake ViewModel for Parallel UI Proving Ground.
 * Emits hardcoded states representing specific UX edge cases without hitting the database or LLM.
 * Safe for `@Preview`.
 */
class FakeSchedulerViewModel : ISchedulerViewModel {

    private val now = Instant.now()
    
    // --- Fake Data Seeding ---
    
    private val fakeInspiration = SchedulerTimelineItem.Inspiration(
        id = "insp_1",
        timeDisplay = "💡",
        title = "Follow up with Acme Corp about the Q3 renewal."
    )
    
    private val fakeTaskActive = ScheduledTask(
        id = "task_1",
        timeDisplay = "10:00",
        title = "Call John Doe re: pricing",
        startTime = now.plus(1, ChronoUnit.HOURS),
        endTime = now.plus(2, ChronoUnit.HOURS),
        durationMinutes = 60,
        hasAlarm = true,
        alarmCascade = listOf("-1h", "0m"),
        isDone = false,
        keyPersonEntityId = "person_1"
    )
    
    private val fakeTaskCrossedOff = ScheduledTask(
        id = "task_2",
        timeDisplay = "✓",
        title = "Check morning emails",
        startTime = now.minus(2, ChronoUnit.HOURS),
        endTime = now.minus(1, ChronoUnit.HOURS),
        durationMinutes = 60,
        hasAlarm = false,
        alarmCascade = emptyList(),
        isDone = true, // Force crossed off UI state
        keyPersonEntityId = null
    )
    
    private val fakeTaskConflict1 = ScheduledTask(
        id = "task_conflict_1",
        timeDisplay = "14:00",
        title = "Client Presentation",
        startTime = now.plus(4, ChronoUnit.HOURS),
        endTime = now.plus(5, ChronoUnit.HOURS),
        durationMinutes = 60,
        hasAlarm = true,
        alarmCascade = emptyList(),
        isDone = false,
        keyPersonEntityId = null
    )
    
    private val fakeTaskConflict2 = ScheduledTask(
        id = "task_conflict_2",
        timeDisplay = "14:00",
        title = "Internal Sync (Rescheduled)",
        startTime = now.plus(4, ChronoUnit.HOURS),
        endTime = now.plus(4, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES),
        durationMinutes = 30,
        hasAlarm = false,
        alarmCascade = emptyList(),
        isDone = false,
        keyPersonEntityId = null
    )

    // --- State Implementations ---

    private val _activeDayOffset = MutableStateFlow(0)
    override val activeDayOffset: StateFlow<Int> = _activeDayOffset.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    override val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedInspirationIds = MutableStateFlow(setOf<String>())
    override val selectedInspirationIds: StateFlow<Set<String>> = _selectedInspirationIds.asStateFlow()

    private val _tipsLoading = MutableStateFlow(setOf<String>())
    override val tipsLoading: StateFlow<Set<String>> = _tipsLoading.asStateFlow()

    // Simulate an active audio pipeline process
    private val _pipelineStatus = MutableStateFlow<String?>("🎙️ 录音中...")
    override val pipelineStatus: StateFlow<String?> = _pipelineStatus.asStateFlow()

    private val _unacknowledgedDates = MutableStateFlow(setOf(0, 1))
    override val unacknowledgedDates: StateFlow<Set<Int>> = _unacknowledgedDates.asStateFlow()

    private val _rescheduledDates = MutableStateFlow(setOf(2))
    override val rescheduledDates: StateFlow<Set<Int>> = _rescheduledDates.asStateFlow()

    // Simulate conflict
    private val _conflictWarning = MutableStateFlow<String?>("⚠️ 时间有冲突")
    override val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    private val _conflictedTaskIds = MutableStateFlow(setOf(fakeTaskConflict1.id, fakeTaskConflict2.id))
    override val conflictedTaskIds: StateFlow<Set<String>> = _conflictedTaskIds.asStateFlow()

    private val _causingTaskId = MutableStateFlow<String?>(fakeTaskConflict2.id)
    override val causingTaskId: StateFlow<String?> = _causingTaskId.asStateFlow()

    private val _isInspirationsExpanded = MutableStateFlow(true)
    override val isInspirationsExpanded: StateFlow<Boolean> = _isInspirationsExpanded.asStateFlow()

    private val _expandedConflictIds = MutableStateFlow(setOf(fakeTaskConflict1.id))
    override val expandedConflictIds: StateFlow<Set<String>> = _expandedConflictIds.asStateFlow()

    private val _timelineItems = MutableStateFlow(
        listOf(
            fakeInspiration,
            fakeTaskCrossedOff,
            fakeTaskActive,
            fakeTaskConflict1,
            fakeTaskConflict2
        )
    )
    override val timelineItems: StateFlow<List<SchedulerTimelineItem>> = _timelineItems.asStateFlow()

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>()
    override val exactAlarmPermissionNeeded: SharedFlow<Unit> = _exactAlarmPermissionNeeded.asSharedFlow()

    // --- Actions (No-ops or local state manipulators) ---

    override fun onDateSelected(dayOffset: Int) {
        _activeDayOffset.value = dayOffset
    }

    override fun acknowledgeDate(dayOffset: Int) {
        _unacknowledgedDates.value -= dayOffset
        _rescheduledDates.value -= dayOffset
    }

    override fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
    }

    override fun toggleItemSelection(id: String) {
        val current = _selectedInspirationIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedInspirationIds.value = current
    }

    override fun deleteItem(id: String) {
        _timelineItems.value = _timelineItems.value.filter { it.id != id }
    }

    override fun onItemClick(id: String) {
        // Compose standard click
    }

    override fun toggleDone(taskId: String) {
        _timelineItems.value = _timelineItems.value.map {
            if (it is ScheduledTask && it.id == taskId) {
                it.copy(isDone = !it.isDone)
            } else {
                it
            }
        }
    }

    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }

    override fun deleteInspiration(id: String) {
        _timelineItems.value = _timelineItems.value.filter { it.id != id }
    }

    override fun toggleConflictExpansion(id: String) {
        val current = _expandedConflictIds.value
        _expandedConflictIds.value = if (current.contains(id)) current - id else current + id
    }

    override fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution) {
        _conflictWarning.value = null
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
    }

    override fun onReschedule(taskId: String, text: String) {
        // Resolve conflict visual state for demo
        _conflictWarning.value = null
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
    }

    override fun getCachedTips(taskId: String): List<String> {
        return if (taskId == fakeTaskActive.id) listOf("Mention Q3 budget", "Confirm travel dates") else emptyList()
    }

    override fun processAudio(file: java.io.File) {
        _pipelineStatus.value = "🎙️ Fake 转写中..."
    }

    /**
     * L2 Simulated On-Device Protocol:
     * Sets specific Fake states based on scenario string for UI validation without backend.
     */
    fun debugRunScenario(scenario: String) {
        when (scenario) {
            "EMPTY" -> {
                _timelineItems.value = emptyList()
                _conflictWarning.value = null
            }
            "LOADED" -> {
                _timelineItems.value = listOf(fakeTaskActive, fakeTaskCrossedOff)
                _conflictWarning.value = null
            }
            "CONFLICT" -> {
                _timelineItems.value = listOf(fakeTaskConflict1, fakeTaskConflict2, fakeTaskActive)
                _conflictWarning.value = "⚠️ 时间有冲突"
                _causingTaskId.value = fakeTaskConflict2.id
                _conflictedTaskIds.value = setOf(fakeTaskConflict1.id, fakeTaskConflict2.id)
            }
            "INSPIRATIONS" -> {
                _timelineItems.value = listOf(fakeInspiration, fakeTaskActive)
                _isInspirationsExpanded.value = true
            }
        }
    }
}
