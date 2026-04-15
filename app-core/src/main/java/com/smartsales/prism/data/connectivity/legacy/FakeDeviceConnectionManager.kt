package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeDeviceConnectionManager : DeviceConnectionManager {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state
    
    private val _recordingReadyEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val recordingReadyEvents: kotlinx.coroutines.flow.SharedFlow<String> =
        _recordingReadyEvents.asSharedFlow()
    override val audioRecordingReadyEvents: kotlinx.coroutines.flow.SharedFlow<String> =
        kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 0).asSharedFlow()
    
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
    
    fun setState(newState: ConnectionState) {
        _state.value = newState
    }

    suspend fun emitRecordingReadyEvent(filename: String) {
        _recordingReadyEvents.emit(filename)
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
    }
}
