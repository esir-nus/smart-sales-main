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
    
    private val _recordingReadyEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    override val recordingReadyEvents: kotlinx.coroutines.flow.SharedFlow<String> = 
        _recordingReadyEvents.asSharedFlow()
    
    var stubPairingResult: Result<Unit> = Result.Success(Unit)
    var stubRetryResult: Result<Unit> = Result.Success(Unit)
    var stubHotspotResult: Result<WifiCredentials> = Result.Error(IllegalStateException("Not stubbed"))
    var stubNetworkResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("Not stubbed"))
    
    val selectCalls = mutableListOf<BlePeripheral>()
    val pairingCalls = mutableListOf<Pair<BlePeripheral, WifiCredentials>>()
    var retryCalls = 0
    var disconnectCalls = 0
    var forgetCalls = 0
    var autoReconnectCalls = 0
    var forceReconnectCalls = 0
    
    override fun selectPeripheral(peripheral: BlePeripheral) {
        selectCalls.add(peripheral)
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
    
    override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = stubNetworkResult
    
    override fun scheduleAutoReconnectIfNeeded() {
        autoReconnectCalls++
    }
    
    override fun forceReconnectNow() {
        forceReconnectCalls++
    }

    var stubReconnectAndWaitResult: ConnectionState = ConnectionState.Connected(
        BleSession.fromPeripheral(BlePeripheral("fake-id", "FakeDevice", -50))
    )
    var reconnectAndWaitCalls = 0

    override suspend fun reconnectAndWait(): ConnectionState {
        reconnectAndWaitCalls++
        return stubReconnectAndWaitResult
    }
    
    fun setState(newState: ConnectionState) {
        _state.value = newState
    }
    
    fun reset() {
        _state.value = ConnectionState.Disconnected
        selectCalls.clear()
        pairingCalls.clear()
        retryCalls = 0
        disconnectCalls = 0
        forgetCalls = 0
        autoReconnectCalls = 0
        forceReconnectCalls = 0
    }
}
