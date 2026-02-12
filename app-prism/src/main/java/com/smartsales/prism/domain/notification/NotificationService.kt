package com.smartsales.prism.domain.notification

import android.app.PendingIntent

/**
 * 系统通知服务接口
 *
 * 所有 Android 通知逻辑统一通过此接口。
 * 功能模块（Scheduler, Coach, Badge）不直接构建通知。
 */
interface NotificationService {
    /**
     * 显示通知
     * @param id 通知唯一标识 (用于去重和取消)
     * @param title 通知标题
     * @param body 通知正文
     * @param channel 通知渠道
     * @param priority 优先级
     */
    fun show(
        id: String,
        title: String,
        body: String,
        channel: PrismNotificationChannel = PrismNotificationChannel.TASK_REMINDER_EARLY,
        priority: NotificationPriority = NotificationPriority.HIGH,
        contentIntent: PendingIntent? = null
    )

    /**
     * 取消指定通知
     */
    fun cancel(id: String)

    /**
     * 检查通知权限是否已授予
     */
    fun hasPermission(): Boolean

    /**
     * 停止持续振动（URGENT 通知关闭时调用）
     */
    fun stopVibration()

    /**
     * 启动持续振动（URGENT 通知显示时调用）
     */
    fun startPersistentVibration()
}
