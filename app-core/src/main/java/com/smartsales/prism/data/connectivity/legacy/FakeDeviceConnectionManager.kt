package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.Result
import com.smartsales.prism.domain.connectivity.WifiRepairEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeDeviceConnectionManager : DeviceConnectionManager {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state
    
    private val _recordingReadyEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val recordingReadyEvents: SharedFlow<String> = _recordingReadyEvents.asSharedFlow()
    override val audioRecordingReadyEvents: SharedFlow<String> =
        MutableSharedFlow<String>(replay = 0).asSharedFlow()
    override val batteryEvents: SharedFlow<Int> =
        MutableSharedFlow<Int>(replay = 0).asSharedFlow()
    private val _firmwareVersionEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val firmwareVersionEvents: SharedFlow<String> = _firmwareVersionEvents.asSharedFlow()

    private val _wifiRepairEvents = MutableSharedFlow<WifiRepairEvent>(
        replay = 0,
        extraBufferCapacity = 16
    )
    override val wifiRepairEvents: SharedFlow<WifiRepairEvent> = _wifiRepairEvents.asSharedFlow()

    suspend fun emitRepairEvent(event: WifiRepairEvent) {
        _wifiRepairEvents.emit(event)
    }
    
    var stubPairingResult: Result<Unit> = Result.Success(Unit)
    var stubRetryResult: Result<Unit> = Result.Success(Unit)
    var stubHotspotResult: Result<WifiCredentials> = Result.Error(IllegalStateException("Not stubbed"))
    var stubNetworkResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("Not stubbed"))
    var stubConfirmManualWifiProvisionResult: ConnectionState = ConnectionState.WifiProvisioned(
        session = BleSession.fromPeripheral(BlePeripheral("fake-id", "FakeDevice", -50)),
        status = ProvisioningStatus(
            wifiSsid = "FakeWifi",
            handshakeId = "fake-handshake",
            credentialsHash = "fake-credentials"
        )
    )
    
    val selectCalls = mutableListOf<BlePeripheral>()
    val pairingCalls = mutableListOf<Pair<BlePeripheral, WifiCredentials>>()
    val confirmManualWifiProvisionCalls = mutableListOf<WifiCredentials>()
    var retryCalls = 0
    var disconnectCalls = 0
    var forgetCalls = 0
    var autoReconnectCalls = 0
    var forceReconnectCalls = 0
    var queryNetworkStatusCalls = 0
    
    override fun selectPeripheral(peripheral: BlePeripheral) {
        selectCalls.add(peripheral)
        // 仅模拟 BLE/session 已选中；不代表传输已达到可用态。
        _state.value = ConnectionState.Connected(BleSession.fromPeripheral(peripheral))
    }
    
    override suspend fun startPairing(peripheral: BlePeripheral, credentials: WifiCredentials): Result<Unit> {
        pairingCalls.add(peripheral to credentials)
        return stubPairingResult
    }
    
    override suspend fun retry(): Result<Unit> {
        retryCalls++
        return stubRetryResult
    }
    
    override fun disconnectBle() {
        disconnectCalls++
        _state.value = ConnectionState.Disconnected
    }
    
    override fun forgetDevice() {
        forgetCalls++
        _state.value = ConnectionState.Disconnected
    }
    
    override suspend fun requestHotspotCredentials(): Result<WifiCredentials> = stubHotspotResult
    
    override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> {
        queryNetworkStatusCalls++
        return stubNetworkResult
    }

    override suspend fun confirmManualWifiProvision(credentials: WifiCredentials): ConnectionState {
        confirmManualWifiProvisionCalls += credentials
        return stubConfirmManualWifiProvisionResult
    }
    
    override fun scheduleAutoReconnectIfNeeded() {
        autoReconnectCalls++
    }
    
    override fun forceReconnectNow() {
        forceReconnectCalls++
    }

    var stubReconnectAndWaitResult: ConnectionState = ConnectionState.WifiProvisioned(
        session = BleSession.fromPeripheral(BlePeripheral("fake-id", "FakeDevice", -50)),
        status = ProvisioningStatus(
            wifiSsid = "FakeWifi",
            handshakeId = "fake-handshake",
            credentialsHash = "fake-credentials"
        )
    )
    var reconnectAndWaitCalls = 0

    override suspend fun reconnectAndWait(): ConnectionState {
        reconnectAndWaitCalls++
        return stubReconnectAndWaitResult
    }

    var notifyCommandEndCalls = 0
    override suspend fun notifyCommandEnd() {
        notifyCommandEndCalls++
    }

    val voiceVolumeCalls = mutableListOf<Int>()
    var setVoiceVolumeShouldSucceed = true
    override suspend fun setVoiceVolume(level: Int): Boolean {
        voiceVolumeCalls.add(level.coerceIn(0, 100))
        return setVoiceVolumeShouldSucceed
    }

    var requestFirmwareVersionCalls = 0
    var requestFirmwareVersionShouldSucceed = true
    override suspend fun requestFirmwareVersion(): Boolean {
        requestFirmwareVersionCalls++
        return requestFirmwareVersionShouldSucceed
    }
    
    fun setState(newState: ConnectionState) {
        _state.value = newState
    }

    suspend fun emitRecordingReadyEvent(filename: String) {
        _recordingReadyEvents.emit(filename)
    }

    suspend fun emitFirmwareVersion(version: String) {
        _firmwareVersionEvents.emit(version)
    }
    
    fun reset() {
        _state.value = ConnectionState.Disconnected
        selectCalls.clear()
        pairingCalls.clear()
        retryCalls = 0
        confirmManualWifiProvisionCalls.clear()
        disconnectCalls = 0
        forgetCalls = 0
        autoReconnectCalls = 0
        forceReconnectCalls = 0
        queryNetworkStatusCalls = 0
        notifyCommandEndCalls = 0
        requestFirmwareVersionCalls = 0
    }
}
