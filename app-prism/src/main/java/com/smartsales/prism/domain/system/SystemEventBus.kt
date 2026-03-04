package com.smartsales.prism.domain.system

import kotlinx.coroutines.flow.SharedFlow

sealed class SystemEvent {
    data object AppIdle : SystemEvent()
}

interface SystemEventBus {
    val events: SharedFlow<SystemEvent>
    suspend fun publish(event: SystemEvent)
}
