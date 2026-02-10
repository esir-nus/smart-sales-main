package com.smartsales.prism.domain.scheduler

import java.time.Instant

/**
 * 闹钟调度器 — 任务提醒管理
 * @see Prism-V1.md §4.3 (Smart Cascade)
 */
interface AlarmScheduler {
    /**
     * 设置任务提醒
     * @param taskId 任务ID
     * @param taskTitle 任务标题 (用于通知显示)
     * @param triggerAt 触发时间
     * @param type 提醒类型
     */
    suspend fun scheduleReminder(taskId: String, taskTitle: String, triggerAt: Instant, type: ReminderType)

    /**
     * 取消任务提醒
     */
    suspend fun cancelReminder(taskId: String)

    /**
     * 使用智能级联设置提醒 (根据任务类型自动决定提前时间)
     */
    suspend fun scheduleSmartCascade(taskId: String, taskTitle: String, eventTime: Instant, taskType: TaskTypeHint)
}

/**
 * 提醒类型
 */
enum class ReminderType {
    SINGLE,      // 单次提醒
    SMART_CASCADE // 智能级联 (T-30min, T-10min, T-5min based on task type)
}

/**
 * 任务类型提示 (用于智能提醒)
 */
enum class TaskTypeHint {
    MEETING,     // 会议: T-30min
    CALL,        // 电话: T-15min
    PERSONAL,    // 个人: T-10min
    URGENT       // 紧急: T-5min
}
