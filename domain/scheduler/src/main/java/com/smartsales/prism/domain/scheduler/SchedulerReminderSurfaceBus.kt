package com.smartsales.prism.domain.scheduler

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 进程内提醒表面事件总线。
 *
 * 用途仅限于前台壳层提醒展示，不替代调度持久化真相。
 * 如果进程内没有收集者，事件可以安全丢弃，系统通知仍然是最终兜底。
 */
data class SchedulerReminderSurfaceEvent(
    val taskId: String,
    val offsetMinutes: Int,
    val taskTitle: String,
    val timeText: String,
    val emittedAtMillis: Long = System.currentTimeMillis()
)

object SchedulerReminderSurfaceBus {
    private val _events = MutableSharedFlow<SchedulerReminderSurfaceEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SchedulerReminderSurfaceEvent> = _events.asSharedFlow()

    fun emit(event: SchedulerReminderSurfaceEvent) {
        _events.tryEmit(event)
    }
}
