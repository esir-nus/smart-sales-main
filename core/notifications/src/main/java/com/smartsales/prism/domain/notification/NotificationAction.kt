package com.smartsales.prism.domain.notification

/**
 * Android 平台无关的通知点击意图抽象
 *
 * 将具体的 PendingIntent 抽象至 Domain 层，保持 Domain 层不依赖 Android API。
 */
sealed class NotificationAction {
    /** 点击后无任何操作 (或仅关闭通知) */
    data object None : NotificationAction()

    /**
     * 点击后打开 App
     * @param deepLinkParam 可选的深链接参数，用于打开特定页面或执行特定动作
     */
    data class OpenApp(val deepLinkParam: String? = null) : NotificationAction()
}
