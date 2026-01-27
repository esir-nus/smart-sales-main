package com.smartsales.data.prismlib.notifications

import com.smartsales.domain.prism.core.MemoryCenterNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内存中心通知器实现 — 通过 SharedFlow 发送 Snackbar 通知
 * @see Prism-V1.md §2.2 #7
 */
@Singleton
class RealMemoryCenterNotifier @Inject constructor() : MemoryCenterNotifier {

    private val _notifications = MutableSharedFlow<MemoryNotification>(
        replay = 0,
        extraBufferCapacity = 10  // 缓冲最近 10 条通知
    )
    
    /**
     * 通知流 — UI 层订阅此流显示 Snackbar
     */
    val notifications: SharedFlow<MemoryNotification> = _notifications.asSharedFlow()

    override fun notifyUpdate(category: String, content: String) {
        val memoryCategory = MemoryCategory.fromString(category)
        val truncatedContent = truncate(content, MAX_CONTENT_LENGTH)
        
        val notification = MemoryNotification(
            category = memoryCategory,
            content = truncatedContent,
            timestamp = System.currentTimeMillis()
        )
        
        _notifications.tryEmit(notification)
    }

    /**
     * 截断内容至指定长度
     */
    private fun truncate(content: String, maxLength: Int): String {
        return if (content.length > maxLength) {
            content.take(maxLength - 1) + "…"
        } else {
            content
        }
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 20  // 最大内容长度（含省略号）
    }
}

/**
 * 内存通知数据类
 */
data class MemoryNotification(
    val category: MemoryCategory,
    val content: String,
    val timestamp: Long
) {
    /**
     * 格式化通知信息
     */
    fun format(): String = "${category.prefix}$content"
}

/**
 * 内存更新类别
 */
enum class MemoryCategory(val prefix: String) {
    HABIT("习惯已更新："),
    CLIENT("客户已更新："),
    SCHEDULE("日程已更新："),
    PROFILE("资料已更新：");

    companion object {
        fun fromString(value: String): MemoryCategory {
            return when (value.lowercase()) {
                "habit", "习惯" -> HABIT
                "client", "客户" -> CLIENT
                "schedule", "日程" -> SCHEDULE
                "profile", "资料" -> PROFILE
                else -> HABIT  // 默认
            }
        }
    }
}
