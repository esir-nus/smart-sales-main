package com.smartsales.prism.domain.mapper

import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.scheduler.TimelineItemModel

/**
 * Maps actionable UI layer Scheduler Tasks into factual Domain layer Memory Entries.
 * Implements Phase 3 of the Cross-Off Lifecycle.
 */
object TaskMemoryMapper {

    fun toMemoryEntry(task: TimelineItemModel.Task): MemoryEntry {
        val now = System.currentTimeMillis()
        
        // Preserve CRM linkage if it exists
        val structuredJson = if (task.keyPersonEntityId != null) {
            """{"linkedEntityId": "${task.keyPersonEntityId}", "type": "TASK_CROSS_OFF"}"""
        } else {
            null
        }

        return MemoryEntry(
            entryId = task.id,
            sessionId = "SCHEDULER_CROSSOFF_$now",
            content = task.title, // Maps to main content
            entryType = MemoryEntryType.SCHEDULE_ITEM,
            createdAt = task.startTime?.toEpochMilli() ?: now,
            updatedAt = now,
            isArchived = true, // By definition, crossed-off tasks become archived memory
            scheduledAt = task.startTime?.toEpochMilli() ?: now,
            structuredJson = structuredJson,
            workflow = "SCHEDULER",
            title = task.title,
            completedAt = now,
            outcomeStatus = "SUCCESS",
            outcomeJson = """{"source": "UI_TOGGLE_DONE"}"""
        )
    }
}
