package com.smartsales.prism.data.real

import com.smartsales.prism.domain.system.SystemEvent
import com.smartsales.prism.domain.system.SystemEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealSystemEventBus @Inject constructor() : SystemEventBus {
    private val _events = MutableSharedFlow<SystemEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<SystemEvent> = _events.asSharedFlow()

    override suspend fun publish(event: SystemEvent) {
        _events.emit(event)
    }
}
