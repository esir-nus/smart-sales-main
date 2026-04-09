package com.smartsales.prism.data.connectivity.legacy

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
    var currentSession: BleSession? = null
    var lastCredentials: WifiCredentials? = null
    var heartbeatJob: Job? = null
    var notificationListenerJob: Job? = null
    var notificationListenerActive = false
    var autoRetryJob: Job? = null
    var autoRetryAttempts = 0
    var reconnectMeta = AutoReconnectMeta()
}
