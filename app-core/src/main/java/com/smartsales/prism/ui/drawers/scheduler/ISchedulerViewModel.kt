package com.smartsales.prism.ui.drawers.scheduler

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask

/**
 * Pure Presentation Boundary for the Scheduler Tracker.
 * 
 * Defines the strict Contract between the Presentation Layer (`@Preview`) 
 * and the Backend (`SchedulerViewModel`).
 * It must NOT contain AST execution methods or pipeline orchestration logic.
 */
interface ISchedulerViewModel {

    // --- State Streams ---
    val activeDayOffset: StateFlow<Int>
    val isSelectionMode: StateFlow<Boolean>
    val selectedInspirationIds: StateFlow<Set<String>>
    val tipsLoading: StateFlow<Set<String>>
    val pipelineStatus: StateFlow<String?>
    val unacknowledgedDates: StateFlow<Set<Int>>
    val rescheduledDates: StateFlow<Set<Int>>
    val conflictWarning: StateFlow<String?>
    val conflictedTaskIds: StateFlow<Set<String>>
    val causingTaskId: StateFlow<String?>
    val isInspirationsExpanded: StateFlow<Boolean>
    val expandedConflictIds: StateFlow<Set<String>>
    val exitingTasks: StateFlow<List<RescheduleExitMotion>>
    val topUrgentTasks: StateFlow<List<ScheduledTask>>
    
    // The Active/Completed Combined Reactive Stream
    val timelineItems: StateFlow<List<SchedulerTimelineItem>>

    val exactAlarmPermissionNeeded: SharedFlow<Unit>

    // --- Actions ---
    fun onDateSelected(dayOffset: Int)
    fun acknowledgeDate(dayOffset: Int)
    
    fun toggleSelectionMode(enabled: Boolean)
    fun toggleItemSelection(id: String)
    fun deleteItem(id: String)
    fun onItemClick(id: String)
    
    // Wave 8 Unification Cross-Off Action
    fun toggleDone(taskId: String)
    
    fun toggleInspirations()
    fun deleteInspiration(id: String)
    fun toggleConflictExpansion(id: String)
    fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution)

    fun onReschedule(taskId: String, text: String)

    // Wave 9 Smart Tips cache bridge
    fun getCachedTips(taskId: String): List<String>
    
    // Pipeline & Audio routing hooks (UI triggers, backend executes)
    fun processAudio(file: java.io.File)
    fun injectTranscript(
        text: String,
        displayedDateIso: String? = null,
        source: DevInjectSource = DevInjectSource.DEV_PANEL
    )
}
