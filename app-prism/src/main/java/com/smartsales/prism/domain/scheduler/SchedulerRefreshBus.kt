package com.smartsales.prism.domain.scheduler

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 进程级事件总线 — 闹钟触发时通知 ViewModel 刷新
 *
 * TaskReminderReceiver 在 DEADLINE 闹钟触发时 emit，
 * SchedulerViewModel 收集后执行 autoCompleteExpiredTasks()。
 * App 被杀时无收集者，事件丢弃 — 下次 drawer 打开时 lazy sweep 兜底。
 */
object SchedulerRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 5)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun emit() {
        _events.tryEmit(Unit)
    }
}
