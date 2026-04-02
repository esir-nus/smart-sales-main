package com.smartsales.prism.domain.mapper

import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maps actionable UI layer Scheduler Tasks into factual Domain layer Memory Entries.
 * Implements Phase 3 of the Cross-Off Lifecycle.
 */
object TaskMemoryMapper {

    fun toMemoryEntry(
        task: ScheduledTask,
        completionSource: String = COMPLETION_SOURCE_UI_TOGGLE_DONE
    ): MemoryEntry {
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
            createdAt = task.startTime.toEpochMilli(),
            updatedAt = now,
            isArchived = true, // By definition, crossed-off tasks become archived memory
            scheduledAt = task.startTime.toEpochMilli(),
            structuredJson = structuredJson,
            workflow = "SCHEDULER",
            title = task.title,
            completedAt = now,
            outcomeStatus = "SUCCESS",
            outcomeJson = buildOutcomeJson(
                completionSource = completionSource,
                reminderCascade = task.alarmCascade,
                urgencyLevel = task.urgencyLevel
            )
        )
    }

    internal fun buildOutcomeJson(
        completionSource: String,
        reminderCascade: List<String>,
        urgencyLevel: UrgencyLevel = inferUrgencyLevel(reminderCascade)
    ): String {
        return JSONObject().apply {
            put("source", completionSource)
            put("reminderCascade", JSONArray(reminderCascade))
            put("urgencyLevel", urgencyLevel.name)
        }.toString()
    }

    internal fun reminderCascadeFromOutcomeJson(outcomeJson: String?): List<String> {
        if (outcomeJson.isNullOrBlank()) return emptyList()

        return runCatching {
            val json = JSONObject(outcomeJson)
            val reminderArray = json.optJSONArray("reminderCascade") ?: return emptyList()
            buildList {
                for (index in 0 until reminderArray.length()) {
                    val value = reminderArray.optString(index)
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }

    internal fun urgencyLevelFromOutcomeJson(outcomeJson: String?): UrgencyLevel {
        if (outcomeJson.isNullOrBlank()) {
            return inferUrgencyLevel(emptyList())
        }

        val parsed = runCatching {
            val json = JSONObject(outcomeJson)
            val raw = json.optString("urgencyLevel")
            if (raw.isBlank()) null else UrgencyLevel.valueOf(raw)
        }.getOrNull()

        return parsed ?: inferUrgencyLevel(reminderCascadeFromOutcomeJson(outcomeJson))
    }

    private fun inferUrgencyLevel(reminderCascade: List<String>): UrgencyLevel = when (reminderCascade.size) {
        3 -> UrgencyLevel.L1_CRITICAL
        2 -> UrgencyLevel.L2_IMPORTANT
        else -> UrgencyLevel.L3_NORMAL
    }

    const val COMPLETION_SOURCE_UI_TOGGLE_DONE = "UI_TOGGLE_DONE"
    const val COMPLETION_SOURCE_AUTO_EXPIRED = "AUTO_EXPIRED"
}
