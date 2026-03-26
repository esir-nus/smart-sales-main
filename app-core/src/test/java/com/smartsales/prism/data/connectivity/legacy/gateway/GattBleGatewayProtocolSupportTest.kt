package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GattBleGatewayProtocolSupportTest {

    @Test
    fun `dispatchProvisioningCredentials sends ssid then password and returns success without ack`() = runTest {
        val support = GattBleGatewayProtocolSupport()
        val writes = mutableListOf<String>()
        val waits = mutableListOf<Long>()
        val reference = support.dispatchProvisioningCredentials(
            credentials = WifiCredentials(ssid = "MstRobot", password = "secret"),
            sendSsid = {},
            sendPassword = {},
            waitBetweenWrites = {}
        )

        val result = support.dispatchProvisioningCredentials(
            credentials = WifiCredentials(ssid = "MstRobot", password = "secret"),
            sendSsid = { payload -> writes.add(payload.decodeToString()) },
            sendPassword = { payload -> writes.add(payload.decodeToString()) },
            waitBetweenWrites = { gapMs -> waits.add(gapMs) }
        )

        assertEquals(listOf("SD#MstRobot", "PD#secret"), writes)
        assertEquals(listOf(PROVISIONING_COMMAND_GAP_MS), waits)
        assertTrue(result.handshakeId.isNotBlank())
        assertEquals(reference.credentialsHash, result.credentialsHash)
    }

    @Test
    fun `dispatchProvisioningCredentials sanitizes separators before sending`() = runTest {
        val support = GattBleGatewayProtocolSupport()
        val writes = mutableListOf<String>()

        support.dispatchProvisioningCredentials(
            credentials = WifiCredentials(ssid = "Office#Wifi", password = "sec#ret"),
            sendSsid = { payload -> writes.add(payload.decodeToString()) },
            sendPassword = { payload -> writes.add(payload.decodeToString()) },
            waitBetweenWrites = {}
        )

        assertEquals(listOf("SD#Office-Wifi", "PD#sec-ret"), writes)
    }
}
