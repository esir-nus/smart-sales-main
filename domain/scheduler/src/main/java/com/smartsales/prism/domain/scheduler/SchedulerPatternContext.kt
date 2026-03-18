package com.smartsales.prism.domain.scheduler

import java.time.LocalDate
import java.time.ZoneId

/**
 * 用户排程习惯摘要 — 供 RL 作为用户全局习惯的辅助信号使用。
 *
 * 注意：
 * 1. 这是调度域导出的摘要信号，不是原始 scheduleContext 粘贴。
 * 2. 默认仅用于用户习惯学习，不能直接推断客户/实体习惯。
 */
data class SchedulerPatternContext(
    val upcomingTaskCount: Int,
    val preferredTimeWindow: String? = null,
    val preferredDurationMinutes: Int? = null,
    val leadTimeStyle: String? = null,
    val urgencyStyle: String? = null
) {
    fun toPromptSummary(): String {
        return buildString {
            appendLine("upcoming_task_count: $upcomingTaskCount")
            preferredTimeWindow?.let { appendLine("preferred_time_window: $it") }
            preferredDurationMinutes?.let { appendLine("preferred_duration_minutes: $it") }
            leadTimeStyle?.let { appendLine("lead_time_style: $it") }
            urgencyStyle?.let { appendLine("urgency_style: $it") }
        }.trimEnd()
    }

    companion object {
        /**
         * 从近期未完成任务中提炼稳定的用户排程习惯信号。
         */
        fun fromTasks(
            tasks: List<ScheduledTask>,
            today: LocalDate,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): SchedulerPatternContext? {
            if (tasks.isEmpty()) return null

            val timeWindow = dominantOrNull(
                tasks.map { task ->
                    val hour = task.startTime.atZone(zoneId).hour
                    when {
                        hour < 12 -> "morning"
                        hour < 17 -> "afternoon"
                        hour < 21 -> "evening"
                        else -> "night"
                    }
                }
            )

            val durationMinutes = dominantOrNull(tasks.map { it.durationMinutes }.filter { it > 0 })

            val leadTimeStyle = dominantOrNull(
                tasks.map { task ->
                    val dayDelta = task.startTime.atZone(zoneId).toLocalDate().toEpochDay() - today.toEpochDay()
                    when {
                        dayDelta <= 0L -> "same_day"
                        dayDelta == 1L -> "next_day"
                        else -> "multi_day"
                    }
                }
            )

            val urgencyStyle = dominantOrNull(
                tasks.map { task ->
                    when (task.urgencyLevel) {
                        UrgencyLevel.L1_CRITICAL -> "critical_heavy"
                        UrgencyLevel.L2_IMPORTANT -> "important_heavy"
                        UrgencyLevel.L3_NORMAL -> "normal_heavy"
                        UrgencyLevel.FIRE_OFF -> "fire_off_heavy"
                    }
                }
            )

            return SchedulerPatternContext(
                upcomingTaskCount = tasks.size,
                preferredTimeWindow = timeWindow,
                preferredDurationMinutes = durationMinutes,
                leadTimeStyle = leadTimeStyle,
                urgencyStyle = urgencyStyle
            )
        }

        private fun <T> dominantOrNull(values: List<T>): T? {
            if (values.isEmpty()) return null
            return values
                .groupingBy { it }
                .eachCount()
                .maxWithOrNull(compareBy<Map.Entry<T, Int>> { it.value }.thenByDescending { values.lastIndexOf(it.key) })
                ?.key
        }
    }
}
