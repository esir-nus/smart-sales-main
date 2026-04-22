package com.smartsales.prism.ui.scheduler.mapper

import android.util.Log
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.withNormalizedReminderMetadata
import com.smartsales.prism.ui.drawers.scheduler.ExitDirection
import com.smartsales.prism.ui.drawers.scheduler.ReminderBellState
import com.smartsales.prism.ui.drawers.scheduler.ReminderBellVisual
import com.smartsales.prism.ui.drawers.scheduler.TimelineItem
import com.smartsales.prism.ui.drawers.scheduler.ConflictVisual
import java.time.Instant

/**
 * Pure Kotlin mapper to extract mapping logic out of Compose.
 * Uses the existing TimelineItem UI state projection.
 */
fun SchedulerTimelineItem.toUiState(
    now: Instant = Instant.now(),
    isSelectionMode: Boolean = false,
    selectedInspirationIds: Set<String> = emptySet(),
    expandedConflictIds: Set<String> = emptySet(),
    tipsLoadingSet: Set<String> = emptySet(),
    cachedTips: List<String>? = null
): TimelineItem {
    // Observable Code: Trace mapping execution
    Log.d("SchedulerMapper", "toUiState: id=${this.id}")

    return when (this) {
        is ScheduledTask -> {
            val normalizedTask = withReminderMetadataForUi()
            TimelineItem.Task(
                id = normalizedTask.id,
                timeDisplay = normalizedTask.timeDisplay,
                renderKey = normalizedTask.id,
                title = normalizedTask.title,
                isDone = normalizedTask.isDone,
                isInteractive = true,
                sortInstant = normalizedTask.startTime,
                hasAlarm = normalizedTask.hasAlarm,
                isSmartAlarm = normalizedTask.isSmartAlarm,
                urgencyLevel = normalizedTask.urgencyLevel,
                dateRange = normalizedTask.dateRange,
                location = normalizedTask.location,
                notes = normalizedTask.notes,
                keyPerson = normalizedTask.keyPerson,
                highlights = normalizedTask.highlights,
                alarmCascade = normalizedTask.alarmCascade,
                reminderBells = normalizedTask.toReminderBellVisuals(now),
                processingStatus = null,
                isExiting = false,
                exitDirection = ExitDirection.RIGHT,
                conflictVisual = ConflictVisual.NONE,
                keyPersonEntityId = normalizedTask.keyPersonEntityId,
                tips = cachedTips ?: emptyList(),
                tipsLoading = normalizedTask.id in tipsLoadingSet,
                clarificationState = normalizedTask.clarificationState,
                isVague = normalizedTask.isVague,
                hasConflict = normalizedTask.hasConflict,
                conflictSummary = normalizedTask.conflictSummary
            )
        }
        is SchedulerTimelineItem.CrossedOff -> TimelineItem.Task(
            id = id,
            timeDisplay = timeDisplay,
            renderKey = id,
            title = title,
            isDone = true,
            isInteractive = true,
            sortInstant = startTime,
            hasAlarm = hasAlarm,
            isSmartAlarm = false,
            urgencyLevel = urgencyLevel,
            dateRange = "",
            location = null,
            notes = null,
            keyPerson = null,
            highlights = null,
            alarmCascade = reminderCascade,
            reminderBells = reminderCascade.map { offset ->
                ReminderBellVisual(
                    offset = offset,
                    state = ReminderBellState.FIRED
                )
            },
            processingStatus = null,
            isExiting = false,
            exitDirection = ExitDirection.RIGHT,
            conflictVisual = ConflictVisual.NONE,
            keyPersonEntityId = null,
            tips = emptyList(),
            tipsLoading = false,
            clarificationState = null,
            isVague = false,
            hasConflict = false,
            conflictSummary = null
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

internal fun ScheduledTask.toReminderBellVisuals(now: Instant): List<ReminderBellVisual> {
    val normalizedTask = withReminderMetadataForUi()
    if (normalizedTask.alarmCascade.isEmpty()) return emptyList()

    return normalizedTask.alarmCascade.map { offset ->
        val fireInstant = normalizedTask.startTime.minusMillis(UrgencyLevel.parseCascadeOffset(offset))
        ReminderBellVisual(
            offset = offset,
            state = if (normalizedTask.isDone || !fireInstant.isAfter(now)) {
                ReminderBellState.FIRED
            } else {
                ReminderBellState.ACTIVE
            }
        )
    }
}

private fun ScheduledTask.withReminderMetadataForUi(): ScheduledTask {
    if (isDone && !hasAlarm && alarmCascade.isEmpty()) {
        return copy(
            hasAlarm = false,
            alarmCascade = emptyList()
        )
    }
    return withNormalizedReminderMetadata()
}
