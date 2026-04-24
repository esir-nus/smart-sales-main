package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.prism.domain.connectivity.WifiRepairEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex

internal class DeviceConnectionManagerRuntime {
    val lock = Mutex()
    val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val recordingReadyEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 3
    )
    val audioRecordingReadyEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 3
    )
    val batteryEvents = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 3
    )
    val firmwareVersionEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 3
    )
    val repairEvents = MutableSharedFlow<WifiRepairEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    var currentSession: BleSession? = null
    var lastCredentials: WifiCredentials? = null
    var heartbeatJob: Job? = null
    var notificationListenerJob: Job? = null
    var notificationListenerActive = false
    var notificationListenerGeneration = 0L
    var autoRetryJob: Job? = null
    var autoRetryAttempts = 0
    var reconnectMeta = AutoReconnectMeta()
}
