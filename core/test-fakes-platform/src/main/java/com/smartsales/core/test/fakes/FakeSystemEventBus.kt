package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.system.SystemEvent
import com.smartsales.prism.domain.system.SystemEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeSystemEventBus : SystemEventBus {
    private val _events = MutableSharedFlow<SystemEvent>()
    override val events: SharedFlow<SystemEvent> = _events.asSharedFlow()

    override suspend fun publish(event: SystemEvent) {
        _events.emit(event)
    }
}
