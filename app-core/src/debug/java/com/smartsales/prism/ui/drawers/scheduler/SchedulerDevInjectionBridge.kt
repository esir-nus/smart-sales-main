package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.prism.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@OptIn(ExperimentalCoroutinesApi::class)
object SchedulerDevInjectionBridge {
    private val _requests = MutableSharedFlow<SchedulerDevInjectionRequest>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val requests: SharedFlow<SchedulerDevInjectionRequest> = _requests.asSharedFlow()

    fun emit(request: SchedulerDevInjectionRequest): Boolean {
        if (!BuildConfig.ENABLE_SCHEDULER_DEV_TOOLS) {
            return false
        }
        return _requests.tryEmit(request)
    }

    fun consume(request: SchedulerDevInjectionRequest) {
        if (_requests.replayCache.lastOrNull() == request) {
            _requests.resetReplayCache()
        }
    }
}
