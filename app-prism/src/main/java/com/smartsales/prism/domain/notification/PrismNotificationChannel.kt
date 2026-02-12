package com.smartsales.prism.domain.notification

/**
 * 通知渠道定义
 *
 * 命名为 PrismNotificationChannel 以避免与 android.app.NotificationChannel 冲突。
 * 每个渠道对应一类业务通知，用户可在系统设置中独立开关。
 */
enum class PrismNotificationChannel(
    val channelId: String,
    val displayName: String
) {
    /** 任务提前预警 — 尊重勿扰模式，标准横幅 */
    TASK_REMINDER_EARLY("prism_task_reminders_v3_early", "任务提醒（提前预警）"),

    /** 任务到期闹钟 — 绕过勿扰模式，全屏+持续振动 */
    TASK_REMINDER_DEADLINE("prism_task_reminders_v3_deadline", "任务闹钟（到点提醒）"),

    /** 教练模式提示 — 习惯养成、会话总结 */
    COACH_NUDGE("prism_coach_nudge", "教练提示"),

    /** 设备状态 — 录音完成、同步状态 */
    BADGE_STATUS("prism_badge_status", "设备状态"),

    /** 记忆更新 — 实体合并通知 */
    MEMORY_UPDATE("prism_memory_update", "记忆更新")
}
