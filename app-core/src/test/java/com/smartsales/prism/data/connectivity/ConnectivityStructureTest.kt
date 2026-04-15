package com.smartsales.prism.data.connectivity

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `connectivity host files are reduced to seam responsibilities`() {
        val gatewayHost = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGateway.kt"
        )
        val managerHost = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt"
        )

        assertTrue(gatewayHost.contains("class GattBleGateway @Inject constructor("))
        assertTrue(gatewayHost.contains("private val runtime = GattBleGatewayRuntime()"))
        assertTrue(gatewayHost.contains("private val sessionSupport = GattBleGatewaySessionSupport("))
        assertFalse(gatewayHost.contains("internal class GattContext("))
        assertFalse(gatewayHost.contains("internal class GatewayGattCallback("))
        assertFalse(gatewayHost.contains("internal fun parseBadgeNotificationPayload("))
        assertFalse(gatewayHost.contains("internal fun mergeNetworkFragments("))

        assertTrue(managerHost.contains("interface DeviceConnectionManager"))
        assertTrue(managerHost.contains("class DefaultDeviceConnectionManager @Inject constructor("))
        assertTrue(managerHost.contains("private val runtime = DeviceConnectionManagerRuntime()"))
        assertTrue(managerHost.contains("private val ingressSupport = DeviceConnectionManagerIngressSupport("))
        assertTrue(managerHost.contains("private val reconnectSupport = DeviceConnectionManagerReconnectSupport("))
        assertFalse(managerHost.contains("private fun launchReconnect("))
        assertFalse(managerHost.contains("private fun startNotificationListener("))
        assertFalse(managerHost.contains("internal fun String.toBadgeDownloadFilename()"))
        assertFalse(managerHost.contains("private fun handleProvisioningSuccess("))
    }

    @Test
    fun `wave2b extracted files own runtime protocol reconnect and ingress roles`() {
        val gatewayRuntime = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayRuntime.kt"
        )
        val gatewayProtocol = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt"
        )
        val gatewaySession = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewaySessionSupport.kt"
        )
        val managerRuntime = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerRuntime.kt"
        )
        val managerIngress = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerIngressSupport.kt"
        )
        val managerConnection = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerConnectionSupport.kt"
        )
        val managerReconnect = readSource(
            "app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerReconnectSupport.kt"
        )

        assertTrue(gatewayRuntime.contains("internal class GattBleGatewayRuntime"))
        assertTrue(gatewayRuntime.contains("val configCache = mutableMapOf<String, BleGatewayConfig>()"))
        assertTrue(gatewayRuntime.contains("var persistentSession: GattContext? = null"))

        assertTrue(gatewayProtocol.contains("internal class GattBleGatewayProtocolSupport"))
        assertTrue(gatewayProtocol.contains("internal fun parseBadgeNotificationPayload("))
        assertTrue(gatewayProtocol.contains("internal fun mergeNetworkFragments("))

        assertTrue(gatewaySession.contains("internal class GattBleGatewaySessionSupport("))
        assertTrue(gatewaySession.contains("internal class GattContext("))
        assertTrue(gatewaySession.contains("internal class GatewayGattCallback("))
        assertTrue(gatewaySession.contains("suspend fun provision("))

        assertTrue(managerRuntime.contains("internal class DeviceConnectionManagerRuntime"))
        assertTrue(managerRuntime.contains("var reconnectMeta = AutoReconnectMeta()"))
        assertTrue(managerRuntime.contains("var notificationListenerActive = false"))
        assertTrue(managerRuntime.contains("var notificationListenerGeneration = 0L"))

        assertTrue(managerIngress.contains("internal class DeviceConnectionManagerIngressSupport("))
        assertTrue(managerIngress.contains("fun startNotificationListener(session: BleSession)"))
        assertTrue(managerIngress.contains("internal fun String.toBadgeDownloadFilename()"))

        assertTrue(managerConnection.contains("internal class DeviceConnectionManagerConnectionSupport("))
        assertTrue(managerConnection.contains("fun selectPeripheral(peripheral: BlePeripheral)"))
        assertTrue(managerConnection.contains("suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus>"))
        assertTrue(managerConnection.contains("suspend fun connectUsingSession(session: BleSession): ConnectionState"))

        assertTrue(managerReconnect.contains("internal class DeviceConnectionManagerReconnectSupport("))
        assertTrue(managerReconnect.contains("fun scheduleAutoReconnectIfNeeded()"))
        assertTrue(managerReconnect.contains("suspend fun reconnectAndWait(): ConnectionState"))
        assertTrue(managerReconnect.contains("internal fun requiredIntervalFor("))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File(workingDir, relativePath),
            File(workingDir, "app-core/$relativePath"),
            File(workingDir.parentFile ?: workingDir, relativePath),
            File(workingDir.parentFile ?: workingDir, "app-core/$relativePath")
        )

        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Source file not found for $relativePath from ${workingDir.absolutePath}")
    }
}
