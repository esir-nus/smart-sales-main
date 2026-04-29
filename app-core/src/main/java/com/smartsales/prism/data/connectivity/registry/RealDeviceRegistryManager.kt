package com.smartsales.prism.data.connectivity.registry

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.ConnectionState
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.SessionStore
import com.smartsales.prism.data.connectivity.legacy.scan.BleScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RealDeviceRegistryManager(
    private val registry: DeviceRegistry,
    private val sessionStore: SessionStore,
    private val deviceConnectionManager: DeviceConnectionManager,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
    private val bleScanner: BleScanner? = null
) : DeviceRegistryManager {

    private val switchMutex = Mutex()
    private var bleDetectionJob: Job? = null
    private var stateMonitorJob: Job? = null

    private val _registeredDevices = MutableStateFlow<List<RegisteredDevice>>(emptyList())
    override val registeredDevices: StateFlow<List<RegisteredDevice>> = _registeredDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<RegisteredDevice?>(null)
    override val activeDevice: StateFlow<RegisteredDevice?> = _activeDevice.asStateFlow()

    override fun initializeOnLaunch() {
        migrateIfNeeded()
        refreshDeviceList()
        startBleDetectionMonitor()

        val defaultDevice = registry.getDefault()
        if (defaultDevice != null) {
            val currentSession = sessionStore.loadSession()
            val sessionDevice = currentSession?.peripheralId?.let { registry.findByMac(it) }

            val launchDevice = when {
                sessionDevice != null -> sessionDevice
                else -> defaultDevice
            }
            _activeDevice.value = launchDevice
            ConnectivityLogger.d("🏠 Registry: launch active ${launchDevice.displayName} (${launchDevice.macSuffix})")

            // 如果持久化会话所属设备已被手动断开，阻止自动重连并保留该会话归属。
            if (sessionDevice?.manuallyDisconnected == true) {
                deviceConnectionManager.setManuallyDisconnected(true)
                ConnectivityLogger.d("🏠 Registry: session device manually disconnected (${sessionDevice.macSuffix}), skipping auto-reconnect")
                return
            }

            if (currentSession?.peripheralId != null &&
                currentSession.peripheralId != launchDevice.macAddress &&
                registry.findByMac(currentSession.peripheralId) == null
            ) {
                ConnectivityLogger.w(
                    "[ReconnectGuard] aborted stale launch session ${currentSession.peripheralId}; reseeded ${launchDevice.macSuffix}"
                )
            }
            if (currentSession?.peripheralId != launchDevice.macAddress) {
                seedSessionForDevice(launchDevice)
            }
            if (launchDevice.manuallyDisconnected) {
                deviceConnectionManager.setManuallyDisconnected(true)
                ConnectivityLogger.d("🏠 Registry: skipping auto-reconnect — user manually disconnected ${launchDevice.macSuffix}")
                return
            }
            deviceConnectionManager.scheduleAutoReconnectIfNeeded()
        } else {
            ConnectivityLogger.d("🏠 Registry: no devices registered")
        }

    }

    private fun startBleDetectionMonitor() {
        if (stateMonitorJob?.isActive == true) return
        startPassiveBleProximityScan()
        stateMonitorJob = scope.launch(dispatchers.io) {
            deviceConnectionManager.state
                .collect { state ->
                    ConnectivityLogger.d(
                        "[ActiveOnly][Monitor] state=${state::class.simpleName} " +
                            "active=${_activeDevice.value?.macAddress ?: "none"}"
                    )
                    when (state) {
                        is ConnectionState.Connected -> {
                            syncActiveDeviceToConnectionState(state)
                        }
                        is ConnectionState.WifiProvisioned,
                        is ConnectionState.WifiProvisionedHttpDelayed,
                        is ConnectionState.Syncing,
                        is ConnectionState.AutoReconnecting,
                        is ConnectionState.Pairing -> {
                            syncActiveDeviceToConnectionState(state)
                        }
                        else -> Unit
                    }
                }
        }
    }

    private fun startPassiveBleProximityScan() {
        if (registry.isEmpty()) return
        val scanner = bleScanner ?: run {
            ConnectivityLogger.d("[ActiveOnly][Proximity] skip scan: scanner unavailable")
            return
        }
        if (bleDetectionJob?.isActive == true) return
        bleDetectionJob = scope.launch(dispatchers.io) {
            val lastSeenAt = mutableMapOf<String, Long>()
            var lastReconnectTriggerMac: String? = null
            var scanClockMs = 0L
            ConnectivityLogger.d("🔍 Passive BLE proximity scan started")
            scanner.start()
            try {
                while (isActive) {
                    delay(BLE_PROXIMITY_TICK_MS)
                    scanClockMs += BLE_PROXIMITY_TICK_MS
                    val knownMacs = registry.loadAll().map { it.macAddress }.toSet()
                    if (knownMacs.isEmpty()) {
                        lastSeenAt.clear()
                        continue
                    }
                    val found = scanner.devices.value.filter { it.id in knownMacs }
                    found.forEach { peripheral ->
                        lastSeenAt[peripheral.id] = scanClockMs
                    }
                    updatePassiveBleProximity(found.map { it.id }.toSet(), lastSeenAt, scanClockMs)
                    maybeTriggerActiveReconnect(found.map { it.id }, lastReconnectTriggerMac)?.let {
                        lastReconnectTriggerMac = it
                    }
                    if (deviceConnectionManager.state.value !is ConnectionState.Disconnected) {
                        lastReconnectTriggerMac = null
                    }
                }
            } finally {
                scanner.stop()
                ConnectivityLogger.d(
                    "🔍 Passive BLE proximity scan stopped"
                )
            }
        }
    }

    private fun updatePassiveBleProximity(
        foundMacs: Set<String>,
        lastSeenAt: MutableMap<String, Long>,
        now: Long
    ) {
        var changed = false
        registry.loadAll().forEach { device ->
            val lastSeen = lastSeenAt[device.macAddress]
            val shouldBeDetected = device.macAddress in foundMacs ||
                (lastSeen != null && now - lastSeen <= BLE_PROXIMITY_GRACE_MS)
            if (device.bleDetected != shouldBeDetected) {
                registry.updateBleDetected(device.macAddress, shouldBeDetected)
                changed = true
            }
        }
        if (changed) refreshDeviceList()
    }

    private fun maybeTriggerActiveReconnect(
        foundMacs: List<String>,
        lastReconnectTriggerMac: String?
    ): String? {
        val state = deviceConnectionManager.state.value
        if (state !is ConnectionState.Disconnected) return null
        val target = selectBleDetectionTarget(foundMacs) ?: return null
        if (lastReconnectTriggerMac == target.macAddress) return lastReconnectTriggerMac

        ConnectivityLogger.i("🔍 Auto-reconnect triggered by passive BLE proximity for ${target.macAddress}")
        deviceConnectionManager.forceReconnectNow()
        return target.macAddress
    }

    internal suspend fun handleBleDetectionCandidates(peripherals: List<BlePeripheral>) {
        ConnectivityLogger.i(
            "[ActiveOnly][Candidates] raw=${peripherals.joinToString { it.id }} " +
                "registered=${registry.loadAll().joinToString { it.macAddress }}"
        )
        val registeredFound = peripherals.mapNotNull { peripheral ->
            val registered = registry.findByMac(peripheral.id)
            if (registered == null) {
                ConnectivityLogger.w("[ReconnectGuard] aborted stale BLE detection candidate ${peripheral.id}")
                null
            } else {
                peripheral
            }
        }
        registeredFound.forEach { peripheral ->
            ConnectivityLogger.i("🔍 BLE detected: ${peripheral.name} (${peripheral.id})")
            registry.updateBleDetected(peripheral.id, true)
        }
        refreshDeviceList()

        val target = selectBleDetectionTarget(registeredFound.map { it.id })
        if (target != null) {
            ConnectivityLogger.i("🔍 Auto-reconnect triggered by BLE detection for ${target.macAddress}")
            deviceConnectionManager.forceReconnectNow()
        } else {
            ConnectivityLogger.i(
                "[ActiveOnly][Candidates] no eligible active target from " +
                    registeredFound.joinToString { it.id }
            )
        }
    }

    private fun selectBleDetectionTarget(candidateMacs: List<String>): RegisteredDevice? {
        val candidateSet = candidateMacs.toSet()
        val candidates = candidateSet.mapNotNull { registry.findByMac(it) }
        ConnectivityLogger.i(
            "[ActiveOnly][Selector] active=${_activeDevice.value?.macAddress ?: "none"} " +
                "candidates=${candidates.joinToString { "${it.macAddress}(default=${it.isDefault},manual=${it.manuallyDisconnected})" }}"
        )
        val activeMac = _activeDevice.value?.macAddress
        val activeCandidate = candidates.firstOrNull { it.macAddress == activeMac }
        if (activeCandidate?.manuallyDisconnected == false) {
            return activeCandidate
        }
        if (candidates.any { it.macAddress != activeMac }) {
            ConnectivityLogger.i("[ActiveOnly] non-active BLE candidates marked only; active remains ${activeMac ?: "none"}")
        }
        return null
    }

    private fun stopBleDetectionScan() {
        bleDetectionJob?.cancel()
        bleDetectionJob = null
        bleScanner?.stop()
    }

    override fun registerDevice(peripheral: BlePeripheral, session: BleSession) {
        val isFirst = registry.isEmpty()
        val device = RegisteredDevice.fromPairing(peripheral, session, isDefault = isFirst)
        registry.register(device)
        refreshDeviceList()
        startPassiveBleProximityScan()
        _activeDevice.value = registry.findByMac(device.macAddress)
        ConnectivityLogger.i("🏠 Registry: registered ${device.displayName} (${device.macSuffix}) default=$isFirst")
    }

    override fun renameDevice(macAddress: String, newName: String) {
        registry.rename(macAddress, newName)
        refreshDeviceList()
        _activeDevice.value?.let { active ->
            if (active.macAddress == macAddress) {
                _activeDevice.value = registry.findByMac(macAddress)
            }
        }
        ConnectivityLogger.d("🏠 Registry: renamed $macAddress → $newName")
    }

    override fun setDefault(macAddress: String) {
        registry.setDefault(macAddress)
        refreshDeviceList()
        ConnectivityLogger.d("🏠 Registry: set default → $macAddress")
    }

    override suspend fun switchToDevice(macAddress: String) = switchMutex.withLock {
        val current = _activeDevice.value
        if (current?.macAddress == macAddress) return@withLock

        val target = registry.findByMac(macAddress) ?: run {
            ConnectivityLogger.w("🏠 Registry: switch target not found: $macAddress")
            return@withLock
        }

        ConnectivityLogger.i("🏠 Registry: switching ${current?.macSuffix ?: "none"} → ${target.macSuffix}")

        // 用户主动连接，清除 BLE 检测标记
        if (target.bleDetected) {
            registry.updateBleDetected(macAddress, false)
        }

        // Seed session for the target device
        val targetSession = seedSessionForDevice(target)

        _activeDevice.value = target
        registry.updateLastConnected(macAddress)
        refreshDeviceList()

        // Trigger reconnect for the new device
        ConnectivityLogger.d("🏠 Registry: reconnecting target ${target.macSuffix}")
        deviceConnectionManager.forceReconnectToSession(targetSession)
    }

    override fun removeDevice(macAddress: String) {
        val wasActive = _activeDevice.value?.macAddress == macAddress
        val wasDefault = registry.findByMac(macAddress)?.isDefault == true
        val removedStoredSession = sessionStore.loadSession()?.peripheralId == macAddress

        if (wasActive) {
            deviceConnectionManager.forgetDevice()
        }

        registry.remove(macAddress)
        refreshDeviceList()

        val fallbackDevice = registry.getDefault() ?: _registeredDevices.value.maxByOrNull { it.lastConnectedAtMillis }
        if (wasActive) {
            val newDefault = fallbackDevice
            _activeDevice.value = newDefault
            if (newDefault != null) {
                seedSessionForDevice(newDefault)
                ConnectivityLogger.i("🏠 Registry: removed active device, promoted ${newDefault.macSuffix}")
            } else {
                sessionStore.clear()
                ConnectivityLogger.i("🏠 Registry: removed last device, NeedsSetup")
            }
        } else if (removedStoredSession) {
            if (fallbackDevice != null) {
                seedSessionForDevice(fallbackDevice)
                ConnectivityLogger.w("[ReconnectGuard] aborted stale removed session $macAddress; reseeded ${fallbackDevice.macSuffix}")
            } else {
                deviceConnectionManager.forgetDevice()
                sessionStore.clear()
                _activeDevice.value = null
                ConnectivityLogger.w("[ReconnectGuard] aborted stale removed session $macAddress; no registered devices remain")
            }
        } else if (wasDefault) {
            // Default was removed but it wasn't active — just refresh
            ConnectivityLogger.d("🏠 Registry: removed non-active default, new default promoted")
        }

        ConnectivityLogger.d("🏠 Registry: removed $macAddress")
    }

    override fun markManuallyDisconnected(macAddress: String, value: Boolean) {
        registry.updateManuallyDisconnected(macAddress, value)
        refreshDeviceList()
        if (_activeDevice.value?.macAddress == macAddress) {
            _activeDevice.value = registry.findByMac(macAddress)
            deviceConnectionManager.setManuallyDisconnected(value)
        }
        ConnectivityLogger.d("🏠 Registry: manuallyDisconnected=$value for $macAddress")
    }

    override fun updateBleDetected(macAddress: String, value: Boolean) {
        registry.updateBleDetected(macAddress, value)
        refreshDeviceList()
        if (_activeDevice.value?.macAddress == macAddress) {
            _activeDevice.value = registry.findByMac(macAddress)
        }
        ConnectivityLogger.d("🏠 Registry: bleDetected=$value for $macAddress")
    }

    override fun debugSeedDefaultPriorityScenario(): Boolean {
        val now = System.currentTimeMillis()
        val defaultDevice = registry.findByMac(DEBUG_DEFAULT_MAC) ?: RegisteredDevice(
            macAddress = DEBUG_DEFAULT_MAC,
            displayName = DEBUG_CHLE_NAME,
            profileId = DEBUG_CHLE_PROFILE,
            registeredAtMillis = now,
            lastConnectedAtMillis = now - 1_000L,
            isDefault = true
        )
        val activeDevice = registry.findByMac(DEBUG_ACTIVE_MAC) ?: RegisteredDevice(
            macAddress = DEBUG_ACTIVE_MAC,
            displayName = DEBUG_CHLE_NAME,
            profileId = DEBUG_CHLE_PROFILE,
            registeredAtMillis = now,
            lastConnectedAtMillis = now,
            isDefault = false
        )

        registry.register(defaultDevice.copy(manuallyDisconnected = false, bleDetected = false))
        registry.register(activeDevice.copy(manuallyDisconnected = false, bleDetected = false))
        registry.setDefault(DEBUG_DEFAULT_MAC)
        val active = registry.findByMac(DEBUG_ACTIVE_MAC) ?: return false
        seedSessionForDevice(active)
        _activeDevice.value = active
        refreshDeviceList()
        ConnectivityLogger.i("[DebugSim] seeded active-only registry default=$DEBUG_DEFAULT_MAC active=$DEBUG_ACTIVE_MAC")
        return true
    }

    override suspend fun debugSimulateBleDetection(manuallyDisconnectDefault: Boolean): Boolean {
        val scenario = if (manuallyDisconnectDefault) {
            DebugBleDetectionL25Scenario.ManualDefaultSuppression
        } else {
            DebugBleDetectionL25Scenario.ActiveOnlyDualAdvertise
        }
        return debugRunBleDetectionL25Scenario(scenario)?.passed == true
    }

    override suspend fun debugRunBleDetectionL25Scenario(
        scenario: DebugBleDetectionL25Scenario
    ): DebugBleDetectionL25Result? {
        if (!debugSeedDefaultPriorityScenario()) return null
        val expectedSelectedMac = DEBUG_ACTIVE_MAC
        registry.updateManuallyDisconnected(DEBUG_DEFAULT_MAC, scenario.manuallyDisconnectDefault)
        registry.updateManuallyDisconnected(DEBUG_ACTIVE_MAC, false)
        refreshDeviceList()
        ConnectivityLogger.i(
            "[L2.5][BEGIN] scenario=${scenario.scenarioId} source=connectivity_debug_button " +
                "ingress=DeviceRegistryManager.handleBleDetectionCandidates authenticity=synthetic_not_physical_ble"
        )
        ConnectivityLogger.i(
            "[DebugSim] BLE detection candidates default=$DEBUG_DEFAULT_MAC active=$DEBUG_ACTIVE_MAC " +
                "manuallyDisconnectedDefault=${scenario.manuallyDisconnectDefault}"
        )
        handleBleDetectionCandidates(
            listOf(
                BlePeripheral(DEBUG_DEFAULT_MAC, DEBUG_CHLE_NAME, -35, profileId = DEBUG_CHLE_PROFILE),
                BlePeripheral(DEBUG_ACTIVE_MAC, DEBUG_CHLE_NAME, -36, profileId = DEBUG_CHLE_PROFILE)
            )
        )
        val defaultDevice = registry.findByMac(DEBUG_DEFAULT_MAC)
        val activeDevice = registry.findByMac(DEBUG_ACTIVE_MAC)
        val selectedMac = _activeDevice.value?.macAddress
        val defaultBleDetected = defaultDevice?.bleDetected == true
        val activeBleDetected = activeDevice?.bleDetected == true
        val passed = selectedMac == expectedSelectedMac &&
            defaultBleDetected &&
            activeBleDetected &&
            defaultDevice?.manuallyDisconnected == scenario.manuallyDisconnectDefault &&
            deviceConnectionManagerForceTargetMac() == expectedSelectedMac
        val result = DebugBleDetectionL25Result(
            scenarioId = scenario.scenarioId,
            evidenceClass = "L2.5",
            source = "connectivity_debug_button",
            defaultMac = DEBUG_DEFAULT_MAC,
            activeMac = DEBUG_ACTIVE_MAC,
            manuallyDisconnectedDefault = scenario.manuallyDisconnectDefault,
            expectedSelectedMac = expectedSelectedMac,
            selectedMac = selectedMac,
            defaultBleDetected = defaultBleDetected,
            activeBleDetected = activeBleDetected,
            passed = passed
        )
        ConnectivityLogger.i(
            "[L2.5][ASSERT] scenario=${result.scenarioId} expected=${result.expectedSelectedMac} " +
                "selected=${result.selectedMac ?: "none"} defaultBleDetected=${result.defaultBleDetected} " +
                "activeBleDetected=${result.activeBleDetected} manuallyDisconnectedDefault=${result.manuallyDisconnectedDefault} " +
                "pass=${result.passed}"
        )
        ConnectivityLogger.i(
            "[L2.5][END] scenario=${result.scenarioId} result=${if (result.passed) "PASS" else "FAIL"} " +
                "evidenceClass=${result.evidenceClass} authenticity=synthetic_not_physical_ble"
        )
        return result
    }

    private fun migrateIfNeeded() {
        if (!registry.isEmpty()) return
        val session = sessionStore.loadSession() ?: return

        val migrated = RegisteredDevice.fromSession(session, isDefault = true)
        registry.register(migrated)
        ConnectivityLogger.i(
            "🏠 Registry: migrated single-device user: ${migrated.displayName} (${migrated.macSuffix})"
        )
    }

    private fun refreshDeviceList() {
        _registeredDevices.value = registry.loadAll()
    }

    internal fun syncActiveDeviceToConnectionState(state: ConnectionState) {
        val runtimeSession = when (state) {
            is ConnectionState.Connected -> state.session
            is ConnectionState.WifiProvisioned -> state.session
            is ConnectionState.WifiProvisionedHttpDelayed -> state.session
            is ConnectionState.Syncing -> state.session
            else -> null
        } ?: return
        syncActiveDeviceToRuntimeSession(runtimeSession, "runtime")
    }

    private fun syncActiveDeviceToRuntimeSession(session: BleSession, source: String) {
        val runtimeDevice = registry.findByMac(session.peripheralId) ?: return
        if (_activeDevice.value?.macAddress == runtimeDevice.macAddress) return

        _activeDevice.value = runtimeDevice
        registry.updateLastConnected(runtimeDevice.macAddress)
        refreshDeviceList()
        ConnectivityLogger.i(
            "🏠 Registry: active device synced from $source session → ${runtimeDevice.macSuffix}"
        )
    }

    private fun seedSessionForDevice(device: RegisteredDevice): BleSession {
        val session = BleSession(
            peripheralId = device.macAddress,
            peripheralName = device.displayName,
            signalStrengthDbm = 0,
            profileId = device.profileId,
            secureToken = java.util.UUID.randomUUID().toString(),
            establishedAtMillis = device.registeredAtMillis
        )
        sessionStore.saveSession(session)
        return session
    }

    private fun deviceConnectionManagerForceTargetMac(): String? =
        sessionStore.loadSession()?.peripheralId

    private companion object {
        const val BLE_PROXIMITY_TICK_MS = 2_000L
        const val BLE_PROXIMITY_GRACE_MS = 6_000L
        const val DEBUG_DEFAULT_MAC = "14:C1:9F:D7:E3:EE"
        const val DEBUG_ACTIVE_MAC = "14:C1:9F:D7:E4:06"
        const val DEBUG_CHLE_NAME = "CHLE_Intelligent"
        const val DEBUG_CHLE_PROFILE = "chle"
    }
}
