package com.smartsales.feature.connectivity.gateway

import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Fake implementation for testing. Supports stubbing and call tracking.
 */
class FakeBleGateway : BleGateway {
    var stubProvisionResult: BleGatewayResult = BleGatewayResult.Success("fake-handshake", "fake-hash")
    var stubHotspotResult: HotspotResult = HotspotResult.TransportError("Not stubbed")
    var stubNetworkResult: NetworkQueryResult = NetworkQueryResult.TransportError("Not stubbed")
    var stubGifResult: GifCommandResult = GifCommandResult.Ready
    var stubWavResult: WavCommandResult = WavCommandResult.Ready
    
    val provisionCalls = mutableListOf<Pair<BleSession, WifiCredentials>>()
    val hotspotCalls = mutableListOf<BleSession>()
    val networkCalls = mutableListOf<BleSession>()
    val gifCalls = mutableListOf<Pair<BleSession, GifCommand>>()
    val wavCalls = mutableListOf<Pair<BleSession, WavCommand>>()
    val forgetCalls = mutableListOf<BlePeripheral>()
    
    override suspend fun provision(session: BleSession, credentials: WifiCredentials): BleGatewayResult {
        provisionCalls.add(session to credentials)
        return stubProvisionResult
    }
    
    override suspend fun requestHotspot(session: BleSession): HotspotResult {
        hotspotCalls.add(session)
        return stubHotspotResult
    }
    
    override suspend fun queryNetwork(session: BleSession): NetworkQueryResult {
        networkCalls.add(session)
        return stubNetworkResult
    }
    
    override suspend fun sendGifCommand(session: BleSession, command: GifCommand): GifCommandResult {
        gifCalls.add(session to command)
        return stubGifResult
    }
    
    override suspend fun sendWavCommand(session: BleSession, command: WavCommand): WavCommandResult {
        wavCalls.add(session to command)
        return stubWavResult
    }
    
    override fun listenForTimeSync(session: BleSession): Flow<TimeSyncEvent> = emptyFlow()
    
    override fun forget(peripheral: BlePeripheral) {
        forgetCalls.add(peripheral)
    }
    
    fun reset() {
        provisionCalls.clear()
        hotspotCalls.clear()
        networkCalls.clear()
        gifCalls.clear()
        wavCalls.clear()
        forgetCalls.clear()
    }
}
