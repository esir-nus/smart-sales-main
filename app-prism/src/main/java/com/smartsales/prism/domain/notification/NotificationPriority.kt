package com.smartsales.prism.domain.notification

/**
 * 通知优先级
 *
 * 映射到 Android NotificationCompat.PRIORITY_* 常量。
 */
enum class NotificationPriority {
    /** 静默 — 无声无振动 */
    LOW,

    /** 正常 — 显示在通知栏 */
    DEFAULT,

    /** 高优先级 — 横幅 + 振动 */
    HIGH,

    /** 紧急 — 到点提醒，持续振动直到用户关闭 */
    URGENT
}
