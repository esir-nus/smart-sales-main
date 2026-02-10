package com.smartsales.prism.domain.scheduler

import java.time.Instant

/**
 * 闹钟调度器 — 基于级联偏移量设置提醒
 *
 * 级联偏移量由 UrgencyLevel.buildCascade() 决定，调度器只负责执行。
 * WHEN 由此接口决定，HOW（通知显示）由 NotificationService 负责。
 */
interface AlarmScheduler {
    /**
     * 设置级联提醒
     *
     * @param taskId 任务ID (用于取消)
     * @param taskTitle 任务标题 (用于通知显示)
     * @param eventTime 事件开始时间
     * @param cascade 级联偏移量列表 (e.g. ["-2h", "-1h", "-15m", "-1m"])
     *                空列表 = 不设置提醒 (FIRE_OFF)
     */
    suspend fun scheduleCascade(
        taskId: String,
        taskTitle: String,
        eventTime: Instant,
        cascade: List<String>
    )

    /**
     * 取消任务所有级联提醒
     */
    suspend fun cancelReminder(taskId: String)
}
