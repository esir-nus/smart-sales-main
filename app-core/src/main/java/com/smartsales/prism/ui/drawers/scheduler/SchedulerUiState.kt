package com.smartsales.prism.ui.drawers.scheduler

/**
 * UI States emitted by the SchedulerViewModel for the Scheduler Drawer.
 * See: docs/cerb-ui/scheduler/contract.md
 */
sealed class SchedulerUiState {
    object Idle : SchedulerUiState()
    object ScanningTimeline : SchedulerUiState()
    
    data class ConflictDetected(
        val existingTaskTitle: String,
        val proposedTime: String,
        val conflictReason: String
    ) : SchedulerUiState()
    
    data class TaskConfirm(
        val title: String,
        val startTime: String,
        val endTime: String,
        val urgency: String,
        val contextTags: List<String>
    ) : SchedulerUiState()
    
    object Executing : SchedulerUiState()
    
    data class Success(val message: String) : SchedulerUiState()
    
    data class Error(val message: String) : SchedulerUiState()
}
