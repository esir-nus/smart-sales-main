package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import java.time.Instant

/**
 * Pure Scheduled Task Domain Model.
 * Specifically stripped of all UI rendering flags (e.g. tipsLoading).
 */
data class ScheduledTask(
    override val id: String,
    override val timeDisplay: String,
    val title: String,
    val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
    val isDone: Boolean = false,
    val hasAlarm: Boolean = false,
    val isSmartAlarm: Boolean = false, // "智能提醒"
    val startTime: Instant, // Raw data for persistence
    val endTime: Instant? = null, // Raw data for persistence
    val durationMinutes: Int = 0, // 持续时间 (fire-off 无时长)
    val durationSource: DurationSource = DurationSource.DEFAULT, // 持续时间来源
    val conflictPolicy: ConflictPolicy = ConflictPolicy.EXCLUSIVE, // 冲突策略
    val dateRange: String = "",
    val location: String? = null,
    val notes: String? = null,
    val keyPerson: String? = null,
    val keyPersonEntityId: String? = null,  // Wave 9: Entity ID for tip generation
    val highlights: String? = null,
    val alarmCascade: List<String> = emptyList(), // e.g. ["-1h", "-15m", "-5m"]
    val hasConflict: Boolean = false, // Wave 17 Path A
    val conflictWithTaskId: String? = null, // Wave 19 T4: 冲突对端任务 ID
    val conflictSummary: String? = null,    // Wave 19 T4: 用户可见的冲突说明
    val isVague: Boolean = false,     // Wave 17 Path A
    val clarificationState: ClarificationState? = null // Wave 14: Anchor for Path B disambiguation
) : SchedulerTimelineItem

/**
 * Wave 14: Represents the disambiguation state of an optimistic task.
 * Used by The Unfolded Card UI.
 */
sealed class ClarificationState {
    data class PersonCandidate(
        val entityId: String,
        val displayName: String,
        val description: String?
    )

    data class AmbiguousPerson(
        val question: String,
        val candidates: List<PersonCandidate>
    ) : ClarificationState()
    
    data class MissingInformation(
        val question: String
    ) : ClarificationState()
}
