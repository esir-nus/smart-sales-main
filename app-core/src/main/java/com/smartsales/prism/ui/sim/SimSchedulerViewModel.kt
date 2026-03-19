package com.smartsales.prism.ui.sim

import androidx.lifecycle.ViewModel
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.ConflictResolution
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * SIM Scheduler 壳层 ViewModel。
 * Wave 1 仅证明抽屉接线，不接入完整 Path A 运行时。
 */
class SimSchedulerViewModel : ViewModel(), ISchedulerViewModel {

    private val now = Instant.now()

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

    private val _unacknowledgedDates = MutableStateFlow(setOf(0))
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

    private val _timelineItems = MutableStateFlow<List<SchedulerTimelineItem>>(
        listOf(
            ScheduledTask(
                id = "sim_task_today",
                timeDisplay = "10:30",
                title = "SIM Shell Wave 1 Review",
                urgencyLevel = UrgencyLevel.L2_IMPORTANT,
                startTime = now.plus(1, ChronoUnit.HOURS),
                endTime = now.plus(2, ChronoUnit.HOURS),
                durationMinutes = 60,
                durationSource = DurationSource.DEFAULT,
                conflictPolicy = ConflictPolicy.EXCLUSIVE,
                hasAlarm = true,
                alarmCascade = listOf("-15m")
            ),
            ScheduledTask(
                id = "sim_task_done",
                timeDisplay = "✓",
                title = "Boundary Constitution Frozen",
                urgencyLevel = UrgencyLevel.L3_NORMAL,
                isDone = true,
                startTime = now.minus(2, ChronoUnit.HOURS),
                endTime = now.minus(90, ChronoUnit.MINUTES),
                durationMinutes = 30,
                durationSource = DurationSource.DEFAULT,
                conflictPolicy = ConflictPolicy.EXCLUSIVE
            )
        )
    )
    override val timelineItems: StateFlow<List<SchedulerTimelineItem>> = _timelineItems.asStateFlow()

    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>()
    override val exactAlarmPermissionNeeded: SharedFlow<Unit> = _exactAlarmPermissionNeeded.asSharedFlow()

    override fun onDateSelected(dayOffset: Int) {
        _activeDayOffset.value = dayOffset
        _unacknowledgedDates.value = _unacknowledgedDates.value - dayOffset
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
        _timelineItems.value = _timelineItems.value.filterNot { it.id == id }
        _pipelineStatus.value = "SIM Shell 已删除本地演示项"
    }

    override fun onItemClick(id: String) {
        _pipelineStatus.value = "Wave 1 仅验证 Scheduler 抽屉接线"
    }

    override fun toggleDone(taskId: String) {
        _timelineItems.value = _timelineItems.value.map { item ->
            if (item is ScheduledTask && item.id == taskId) {
                item.copy(isDone = !item.isDone)
            } else {
                item
            }
        }
    }

    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }

    override fun deleteInspiration(id: String) {
        _timelineItems.value = _timelineItems.value.filterNot { it.id == id }
    }

    override fun toggleConflictExpansion(id: String) {
        val next = _expandedConflictIds.value.toMutableSet()
        if (!next.add(id)) next.remove(id)
        _expandedConflictIds.value = next
    }

    override fun handleConflictResolution(resolution: ConflictResolution) {
        _conflictWarning.value = null
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
        _expandedConflictIds.value = emptySet()
    }

    override fun onReschedule(taskId: String, text: String) {
        _pipelineStatus.value = "Wave 1 不执行真实改期，仅保留壳层动作"
    }

    override fun getCachedTips(taskId: String): List<String> {
        return emptyList()
    }

    override fun processAudio(file: java.io.File) {
        _pipelineStatus.value = "SIM Scheduler 未接入录音驱动入口"
    }
}
