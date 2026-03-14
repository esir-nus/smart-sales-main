package com.smartsales.prism.ui.fakes

import com.smartsales.prism.domain.scheduler.ConflictResolution
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.memory.ScheduleItem
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.memory.ConflictPolicy

/**
 * Fake ISchedulerViewModel for testing and @Preview.
 * Enforces the Anti-Drift Protocol Phase 2: Parallel Proving Ground.
 */
class FakeSchedulerViewModel : ISchedulerViewModel {

    private val _activeDayOffset = MutableStateFlow(0)
    override val activeDayOffset: StateFlow<Int> = _activeDayOffset.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    override val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedInspirationIds = MutableStateFlow<Set<String>>(emptySet())
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

    private val _timelineItems = MutableStateFlow<List<TimelineItemModel>>(emptyList())
    override val timelineItems: StateFlow<List<TimelineItemModel>> = _timelineItems.asStateFlow()

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>()
    override val exactAlarmPermissionNeeded: SharedFlow<Unit> = _exactAlarmPermissionNeeded.asSharedFlow()

    private val tipsCache = mutableMapOf<String, List<String>>()

    override fun getCachedTips(taskId: String): List<String> = tipsCache[taskId] ?: emptyList()

    // --- Mock Scenarios ---
    
    fun debugRunScenario(scenario: String) {
        when (scenario) {
            "EMPTY" -> {
                _activeDayOffset.value = 0
                _timelineItems.value = emptyList()
                _pipelineStatus.value = null
                _conflictWarning.value = null
                _isInspirationsExpanded.value = false
            }
            "LOADED" -> {
                _activeDayOffset.value = 0
                _isInspirationsExpanded.value = false
                _timelineItems.value = listOf(
                    TimelineItemModel.Task(
                        id = "t1",
                        title = "Q2 Review with Alice",
                        timeDisplay = "09:00",
                        isDone = true,
                        hasAlarm = false,
                        durationMinutes = 60,
                        startTime = Instant.now().minusSeconds(3600),
                        urgencyLevel = UrgencyLevel.L3_NORMAL
                    ),
                    TimelineItemModel.Task(
                        id = "t2",
                        title = "Finalize UI Mocks",
                        timeDisplay = "14:00",
                        isDone = false,
                        hasAlarm = true,
                        durationMinutes = 120,
                        startTime = Instant.now().plusSeconds(3600),
                        urgencyLevel = UrgencyLevel.L1_CRITICAL,
                        location = "Meeting Room B",
                        keyPerson = "Bob Design"
                    )
                )
            }
            "CONFLICT" -> {
                _activeDayOffset.value = 0
                _conflictWarning.value = "⚠️ 时间重叠检测"
                _causingTaskId.value = "t_new"
                _conflictedTaskIds.value = setOf("t_existing", "t_new")
                _timelineItems.value = listOf(
                    TimelineItemModel.Conflict(
                        id = "conflict_1",
                        timeDisplay = "15:00",
                        conflictText = "2 个任务冲突",
                        taskA = ScheduleItem(
                            entryId = "t_existing",
                            title = "Team Weekly Sync",
                            scheduledAt = Instant.now().plusSeconds(7200).toEpochMilli(),
                            durationMinutes = 60,
                            durationSource = DurationSource.DEFAULT,
                            conflictPolicy = ConflictPolicy.EXCLUSIVE
                        ),
                        taskB = ScheduleItem(
                            entryId = "t_new",
                            title = "Urgent Client Call",
                            scheduledAt = Instant.now().plusSeconds(8100).toEpochMilli(),
                            durationMinutes = 30,
                            durationSource = DurationSource.DEFAULT,
                            conflictPolicy = ConflictPolicy.EXCLUSIVE
                        )
                    )
                )
            }
            "INSPIRATIONS" -> {
                _activeDayOffset.value = 0
                _isInspirationsExpanded.value = true
                _timelineItems.value = listOf(
                    TimelineItemModel.Inspiration(
                        id = "i1",
                        title = "Discuss Q3 Roadmap with Charlie",
                        timeDisplay = "AI Suggested"
                    ),
                    TimelineItemModel.Inspiration(
                        id = "i2",
                        title = "Follow up on Project Mono",
                        timeDisplay = "From Call Transcript"
                    ),
                    // Adding a valid task below inspirations
                    TimelineItemModel.Task(
                        id = "t1",
                        title = "Standup",
                        timeDisplay = "10:00",
                        isDone = false,
                        hasAlarm = true,
                        durationMinutes = 30,
                        startTime = Instant.now(),
                        urgencyLevel = UrgencyLevel.L3_NORMAL
                    )
                )
            }
        }
    }

    // --- Actions ---
    override fun onDrawerOpened() {}
    override fun onDateSelected(dayOffset: Int) { _activeDayOffset.value = dayOffset }
    override fun onCardExpanded(taskId: String, keyPersonEntityId: String?) {
        tipsCache[taskId] = listOf("Ask about the recent deployment", "Follow up on the email")
    }
    override fun acknowledgeDate(dayOffset: Int) {}
    override fun toggleSelectionMode(enabled: Boolean) { _isSelectionMode.value = enabled }
    override fun toggleItemSelection(id: String) {
        val current = _selectedInspirationIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedInspirationIds.value = current
    }
    override fun deleteItem(id: String) {}
    override fun onItemClick(id: String) {}
    override fun onDeleteItem(id: String) {}
    override fun toggleDone(taskId: String) {}
    override fun toggleInspirations() { _isInspirationsExpanded.value = !_isInspirationsExpanded.value }
    override fun deleteInspiration(id: String) {}
    override fun toggleConflictExpansion(id: String) {
        val current = _expandedConflictIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _expandedConflictIds.value = current
    }
    override fun handleConflictResolution(resolution: ConflictResolution) {}
    override fun clearConflictWarning() { _conflictWarning.value = null }
    override fun onReschedule(id: String, text: String) {}
    override fun onToggleSelection(id: String) { toggleItemSelection(id) }
    override fun onEnterSelectionMode() { toggleSelectionMode(true) }
    override fun onExitSelectionMode() { toggleSelectionMode(false) }
    override fun triggerRefresh() {}
    override fun clearPipelineStatus() { _pipelineStatus.value = null }
    override fun simulateTranscript(fakeMessage: String) { _pipelineStatus.value = "Processing $fakeMessage" }
    override fun simulateFromMic(wavFile: File) {}
}
