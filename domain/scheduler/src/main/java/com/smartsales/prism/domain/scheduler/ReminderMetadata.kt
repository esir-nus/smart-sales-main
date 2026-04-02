package com.smartsales.prism.domain.scheduler

/**
 * 调度提醒元数据规范化。
 *
 * 当前产品不支持自定义 reminder cascade，
 * 因此精确任务的提醒真值必须从 urgencyLevel 推导。
 */
fun normalizedReminderCascade(
    urgencyLevel: UrgencyLevel,
    isVague: Boolean
): List<String> {
    return if (isVague) emptyList() else UrgencyLevel.buildCascade(urgencyLevel)
}

fun ScheduledTask.normalizedReminderCascade(): List<String> {
    return normalizedReminderCascade(
        urgencyLevel = urgencyLevel,
        isVague = isVague
    )
}

fun ScheduledTask.withNormalizedReminderMetadata(): ScheduledTask {
    val normalizedCascade = normalizedReminderCascade()
    return copy(
        hasAlarm = normalizedCascade.isNotEmpty(),
        alarmCascade = normalizedCascade
    )
}
