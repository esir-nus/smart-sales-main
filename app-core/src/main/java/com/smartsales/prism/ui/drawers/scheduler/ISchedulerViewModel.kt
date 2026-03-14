package com.smartsales.prism.ui.drawers.scheduler

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.scheduler.ConflictResolution

/**
 * Contract for the Scheduler UI.
 * Connects the "Skin" (SchedulerDrawer) to the "Body" (SchedulerViewModel or Fakes).
 */
interface ISchedulerViewModel {

    // --- State ---
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
    val timelineItems: StateFlow<List<TimelineItemModel>>

    val exactAlarmPermissionNeeded: SharedFlow<Unit>

    // --- Data Getters ---
    fun getCachedTips(taskId: String): List<String>

    // --- Actions ---
    fun onDrawerOpened()
    fun onDateSelected(dayOffset: Int)
    fun onCardExpanded(taskId: String, keyPersonEntityId: String?)
    fun acknowledgeDate(dayOffset: Int)
    fun toggleSelectionMode(enabled: Boolean)
    fun toggleItemSelection(id: String)
    fun deleteItem(id: String)
    fun onItemClick(id: String)
    fun onDeleteItem(id: String)
    fun toggleDone(taskId: String)
    
    fun toggleInspirations()
    fun deleteInspiration(id: String)
    
    fun toggleConflictExpansion(id: String)
    fun handleConflictResolution(resolution: ConflictResolution)
    fun clearConflictWarning()

    fun onReschedule(id: String, text: String)
    fun onToggleSelection(id: String)
    fun onEnterSelectionMode()
    fun onExitSelectionMode()

    fun triggerRefresh()
    fun clearPipelineStatus()

    fun simulateTranscript(fakeMessage: String)
    fun simulateFromMic(wavFile: java.io.File)
}
