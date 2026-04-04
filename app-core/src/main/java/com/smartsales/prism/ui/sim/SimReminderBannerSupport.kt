package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.ui.components.DynamicIslandSchedulerTarget
import java.time.ZoneId

internal data class SimReminderBannerEntry(
    val taskId: String,
    val title: String,
    val supportingText: String,
    val offsetMinutes: Int,
    val target: DynamicIslandSchedulerTarget?,
    val emittedAtMillis: Long
)

internal data class SimReminderBannerState(
    val entries: List<SimReminderBannerEntry>
) {
    val primaryEntry: SimReminderBannerEntry = entries.first()
    val accentKind: SimReminderBannerAccent = resolveSimReminderBannerAccent(entries)
    val headline: String = buildSimReminderBannerHeadline(entries)
    val description: String = buildSimReminderBannerDescription(entries)
}

internal enum class SimReminderBannerAccent {
    NORMAL,
    WARNING
}

internal fun mergeSimReminderBannerEntries(
    existing: List<SimReminderBannerEntry>,
    incoming: SimReminderBannerEntry
): List<SimReminderBannerEntry> {
    return (listOf(incoming) + existing.filterNot { it.taskId == incoming.taskId })
        .sortedWith(
            compareByDescending<SimReminderBannerEntry> { it.offsetMinutes <= 5 }
                .thenByDescending { it.emittedAtMillis }
        )
}

internal fun buildSimReminderBannerEntry(
    task: ScheduledTask?,
    taskId: String,
    title: String,
    offsetMinutes: Int,
    emittedAtMillis: Long
): SimReminderBannerEntry {
    return SimReminderBannerEntry(
        taskId = taskId,
        title = task?.title ?: title,
        supportingText = buildSimReminderSupportingText(task),
        offsetMinutes = offsetMinutes,
        target = task?.let {
            DynamicIslandSchedulerTarget(
                date = it.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                taskId = it.id,
                isConflict = it.hasConflict
            )
        },
        emittedAtMillis = emittedAtMillis
    )
}

private fun buildSimReminderSupportingText(task: ScheduledTask?): String {
    if (task == null) return ""
    return listOfNotNull(task.keyPerson, task.location, task.notes?.takeIf { it.isNotBlank() })
        .firstOrNull()
        .orEmpty()
}

private fun resolveSimReminderBannerAccent(
    entries: List<SimReminderBannerEntry>
): SimReminderBannerAccent = if (entries.any { it.offsetMinutes <= 5 }) {
    SimReminderBannerAccent.WARNING
} else {
    SimReminderBannerAccent.NORMAL
}

private fun buildSimReminderBannerHeadline(entries: List<SimReminderBannerEntry>): String {
    val count = entries.size
    val hasWarning = entries.any { it.offsetMinutes <= 5 }
    return when {
        hasWarning && count == 1 -> "会议即将开始 (5分钟)"
        hasWarning -> "最近有 $count 个提醒即将开始"
        count == 1 -> "15分钟后开始"
        else -> "15分钟内有 $count 个提醒"
    }
}

private fun buildSimReminderBannerDescription(entries: List<SimReminderBannerEntry>): String {
    val primary = entries.first()
    return when {
        entries.size == 1 && primary.supportingText.isNotBlank() -> "${primary.title} · ${primary.supportingText}"
        entries.size == 1 -> primary.title
        primary.supportingText.isNotBlank() -> "${primary.title} · ${primary.supportingText}  +${entries.size - 1}"
        else -> "${primary.title}  +${entries.size - 1}"
    }
}
