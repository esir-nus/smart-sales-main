package com.smartsales.prism.ui.scheduler.mapper

import android.util.Log
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.ConflictVisual

/**
 * Pure Kotlin mapper to extract mapping logic out of Compose.
 * Uses the existing TimelineItem UI state projection.
 */
fun SchedulerTimelineItem.toUiState(
    isSelectionMode: Boolean = false,
    selectedInspirationIds: Set<String> = emptySet(),
    expandedConflictIds: Set<String> = emptySet(),
    tipsLoadingSet: Set<String> = emptySet(),
    cachedTips: List<String>? = null
): TimelineItem {
    // Observable Code: Trace mapping execution
    Log.d("SchedulerMapper", "toUiState: id=${this.id}")

    return when (this) {
        is ScheduledTask -> TimelineItem.Task(
            id = id,
            timeDisplay = timeDisplay,
            title = title,
            isDone = isDone,
            hasAlarm = hasAlarm,
            isSmartAlarm = isSmartAlarm,
            urgencyLevel = urgencyLevel,
            dateRange = dateRange,
            location = location,
            notes = notes,
            keyPerson = keyPerson,
            highlights = highlights,
            alarmCascade = alarmCascade,
            processingStatus = null,
            isExiting = false,
            exitDirection = ExitDirection.RIGHT,
            conflictVisual = ConflictVisual.NONE,
            keyPersonEntityId = keyPersonEntityId,
            tips = cachedTips ?: emptyList(),
            tipsLoading = id in tipsLoadingSet,
            clarificationState = clarificationState,
            isVague = isVague,
            hasConflict = hasConflict,
            conflictSummary = conflictSummary
        )
        is SchedulerTimelineItem.Inspiration -> TimelineItem.Inspiration(
            id = id,
            timeDisplay = timeDisplay,
            title = title,
            isSelected = id in selectedInspirationIds,
            isSelectionMode = isSelectionMode
        )
        is SchedulerTimelineItem.Conflict -> TimelineItem.Conflict(
            id = id,
            timeDisplay = timeDisplay,
            conflictText = conflictText,
            taskA = taskA,
            taskB = taskB,
            isExpanded = id in expandedConflictIds
        )
    }
}
