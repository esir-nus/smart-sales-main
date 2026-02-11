package com.smartsales.prism.data.fakes

import android.app.PendingIntent
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel

/**
 * 测试用通知服务假实现
 *
 * 记录所有 show() 调用，支持 hasPermission 模拟。
 */
class FakeNotificationService : NotificationService {

    data class ShownNotification(
        val id: String,
        val title: String,
        val body: String,
        val channel: PrismNotificationChannel,
        val priority: NotificationPriority
    )

    private val _shown = mutableListOf<ShownNotification>()
    val shownNotifications: List<ShownNotification> get() = _shown.toList()

    private val _cancelled = mutableListOf<String>()
    val cancelledIds: List<String> get() = _cancelled.toList()

    var permissionGranted: Boolean = true

    override fun show(
        id: String,
        title: String,
        body: String,
        channel: PrismNotificationChannel,
        priority: NotificationPriority,
        contentIntent: PendingIntent?
    ) {
        if (!hasPermission()) return
        _shown.add(ShownNotification(id, title, body, channel, priority))
    }

    override fun cancel(id: String) {
        _cancelled.add(id)
        _shown.removeAll { it.id == id }
    }

    override fun hasPermission(): Boolean = permissionGranted

    override fun stopVibration() { /* no-op */ }

    override fun startPersistentVibration() { /* no-op */ }

    /** 清空测试状态 */
    fun reset() {
        _shown.clear()
        _cancelled.clear()
        permissionGranted = true
    }
}
