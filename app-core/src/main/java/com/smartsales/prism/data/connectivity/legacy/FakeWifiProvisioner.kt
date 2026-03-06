package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.Result

/**
 * Fake implementation for testing. Supports stubbing provisioning results.
 */
class FakeWifiProvisioner : WifiProvisioner {
    var stubProvisionResult: Result<ProvisioningStatus> = Result.Success(
        ProvisioningStatus(wifiSsid = "FakeWiFi", handshakeId = "fake-id", credentialsHash = "fake-hash")
    )
    var stubHotspotResult: Result<WifiCredentials> = Result.Error(IllegalStateException("Not stubbed"))
    var stubNetworkResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("Not stubbed"))
    
    val provisionCalls = mutableListOf<Pair<BleSession, WifiCredentials>>()
    val hotspotCalls = mutableListOf<BleSession>()
    val networkCalls = mutableListOf<BleSession>()
    
    override suspend fun provision(session: BleSession, credentials: WifiCredentials): Result<ProvisioningStatus> {
        provisionCalls.add(session to credentials)
        return stubProvisionResult
    }
    
    override suspend fun requestHotspotCredentials(session: BleSession): Result<WifiCredentials> {
        hotspotCalls.add(session)
        return stubHotspotResult
    }
    
    override suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus> {
        networkCalls.add(session)
        return stubNetworkResult
    }
    
    fun reset() {
        provisionCalls.clear()
        hotspotCalls.clear()
        networkCalls.clear()
    }
}
