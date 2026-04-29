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

    private val _registeredDevices = MutableStateFlow<List<RegisteredDevice>>(emptyList())
    override val registeredDevices: StateFlow<List<RegisteredDevice>> = _registeredDevices.asStateFlow()

    private val _activeDevice = MutableStateFlow<RegisteredDevice?>(null)
    override val activeDevice: StateFlow<RegisteredDevice?> = _activeDevice.asStateFlow()

    override fun initializeOnLaunch() {
        migrateIfNeeded()
        refreshDeviceList()

        val defaultDevice = registry.getDefault()
        if (defaultDevice != null) {
            _activeDevice.value = defaultDevice
            ConnectivityLogger.d("🏠 Registry: default device ${defaultDevice.displayName} (${defaultDevice.macSuffix})")
            val currentSession = sessionStore.loadSession()

            // 如果持久化会话所属设备已被手动断开，阻止自动重连并切换到默认设备会话
            val sessionDevice = currentSession?.peripheralId?.let { registry.findByMac(it) }
            if (sessionDevice?.manuallyDisconnected == true) {
                deviceConnectionManager.setManuallyDisconnected(true)
                ConnectivityLogger.d("🏠 Registry: session device manually disconnected (${sessionDevice.macSuffix}), skipping auto-reconnect")
                seedSessionForDevice(defaultDevice)
                return
            }

            if (currentSession?.peripheralId != null &&
                currentSession.peripheralId != defaultDevice.macAddress &&
                registry.findByMac(currentSession.peripheralId) == null
            ) {
                ConnectivityLogger.w(
                    "[ReconnectGuard] aborted stale launch session ${currentSession.peripheralId}; reseeded ${defaultDevice.macSuffix}"
                )
            }
            if (currentSession?.peripheralId != defaultDevice.macAddress) {
                seedSessionForDevice(defaultDevice)
            }
            if (defaultDevice.manuallyDisconnected) {
                deviceConnectionManager.setManuallyDisconnected(true)
                ConnectivityLogger.d("🏠 Registry: skipping auto-reconnect — user manually disconnected ${defaultDevice.macSuffix}")
                return
            }
            deviceConnectionManager.scheduleAutoReconnectIfNeeded()
        } else {
            ConnectivityLogger.d("🏠 Registry: no devices registered")
        }

        startBleDetectionMonitor()
    }

    private fun startBleDetectionMonitor() {
        scope.launch(dispatchers.io) {
            deviceConnectionManager.state
                .collect { state ->
                    when (state) {
                        is ConnectionState.Disconnected -> {
                            val scanner = bleScanner ?: return@collect
                            val active = _activeDevice.value ?: return@collect
                            if (!active.manuallyDisconnected) {
                                scheduleBleDetectionScan(scanner, active.macAddress)
                            } else {
                                stopBleDetectionScan()
                            }
                        }
                        is ConnectionState.Connected -> {
                            syncActiveDeviceToConnectionState(state)
                        }
                        is ConnectionState.WifiProvisioned,
                        is ConnectionState.WifiProvisionedHttpDelayed,
                        is ConnectionState.Syncing,
                        is ConnectionState.AutoReconnecting,
                        is ConnectionState.Pairing -> {
                            stopBleDetectionScan()
                            // 连接成功后清除所有 BLE 检测标记
                            _registeredDevices.value.filter { it.bleDetected }.forEach { device ->
                                registry.updateBleDetected(device.macAddress, false)
                            }
                            if (_registeredDevices.value.any { it.bleDetected }) refreshDeviceList()
                            syncActiveDeviceToConnectionState(state)
                        }
                        else -> Unit
                    }
                }
        }
    }

    private fun scheduleBleDetectionScan(scanner: BleScanner, targetMac: String) {
        bleDetectionJob?.cancel()
        bleDetectionJob = scope.launch(dispatchers.io) {
            val knownMacs = _registeredDevices.value.map { it.macAddress }.toSet()
            ConnectivityLogger.d("🔍 BLE detection scan started for $targetMac")
            scanner.start()
            // 每隔 2 秒检查扫描结果，最长等待 60 秒
            repeat(30) {
                delay(2_000L)
                val found = scanner.devices.value.filter { it.id in knownMacs }
                if (found.isNotEmpty()) {
                    scanner.stop()
                    handleBleDetectionCandidates(found)
                    return@launch
                }
            }
            scanner.stop()
            ConnectivityLogger.d("🔍 BLE detection scan ended (timeout)")
        }
    }

    internal suspend fun handleBleDetectionCandidates(peripherals: List<BlePeripheral>) {
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
            if (target.macAddress == _activeDevice.value?.macAddress) {
                deviceConnectionManager.forceReconnectNow()
            } else {
                switchToDevice(target.macAddress)
                registry.updateBleDetected(target.macAddress, true)
                refreshDeviceList()
                _activeDevice.value = registry.findByMac(target.macAddress)
            }
        }
    }

    private fun selectBleDetectionTarget(candidateMacs: List<String>): RegisteredDevice? {
        val candidateSet = candidateMacs.toSet()
        val candidates = candidateSet.mapNotNull { registry.findByMac(it) }
        val defaultCandidate = candidates.firstOrNull { it.isDefault }
        if (defaultCandidate?.manuallyDisconnected == true) {
            ConnectivityLogger.i("[DefaultPriority] skipped manuallyDisconnected default ${defaultCandidate.macSuffix}")
        }
        if (defaultCandidate?.manuallyDisconnected == false) {
            val activeMac = _activeDevice.value?.macAddress
            if (activeMac != defaultCandidate.macAddress) {
                ConnectivityLogger.i("[DefaultPriority] switch ${activeMac ?: "none"} → ${defaultCandidate.macAddress}")
            }
            return defaultCandidate
        }

        val activeMac = _activeDevice.value?.macAddress
        val activeCandidate = candidates.firstOrNull { it.macAddress == activeMac }
        if (activeCandidate?.manuallyDisconnected == false) {
            return activeCandidate
        }

        val eligibleCandidates = candidates.filter { !it.manuallyDisconnected }
        return eligibleCandidates.singleOrNull()
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
        ConnectivityLogger.i("[DebugSim] seeded default-first registry default=$DEBUG_DEFAULT_MAC active=$DEBUG_ACTIVE_MAC")
        return true
    }

    override suspend fun debugSimulateBleDetection(manuallyDisconnectDefault: Boolean): Boolean {
        val scenario = if (manuallyDisconnectDefault) {
            DebugBleDetectionL25Scenario.ManualDefaultSuppression
        } else {
            DebugBleDetectionL25Scenario.DefaultPriorityDualAdvertise
        }
        return debugRunBleDetectionL25Scenario(scenario)?.passed == true
    }

    override suspend fun debugRunBleDetectionL25Scenario(
        scenario: DebugBleDetectionL25Scenario
    ): DebugBleDetectionL25Result? {
        if (!debugSeedDefaultPriorityScenario()) return null
        val expectedSelectedMac = when (scenario) {
            DebugBleDetectionL25Scenario.DefaultPriorityDualAdvertise -> DEBUG_DEFAULT_MAC
            DebugBleDetectionL25Scenario.ManualDefaultSuppression -> DEBUG_ACTIVE_MAC
        }
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
            defaultDevice?.manuallyDisconnected == scenario.manuallyDisconnectDefault
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

    private companion object {
        const val DEBUG_DEFAULT_MAC = "14:C1:9F:D7:E3:EE"
        const val DEBUG_ACTIVE_MAC = "14:C1:9F:D7:E4:06"
        const val DEBUG_CHLE_NAME = "CHLE_Intelligent"
        const val DEBUG_CHLE_PROFILE = "chle"
    }
}
