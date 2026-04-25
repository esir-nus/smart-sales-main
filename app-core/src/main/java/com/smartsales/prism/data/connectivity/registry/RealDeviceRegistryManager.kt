package com.smartsales.prism.data.connectivity.registry

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import com.smartsales.prism.data.connectivity.legacy.ConnectivityLogger
import com.smartsales.prism.data.connectivity.legacy.DeviceConnectionManager
import com.smartsales.prism.data.connectivity.legacy.SessionStore
import kotlinx.coroutines.CoroutineScope
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
    private val scope: CoroutineScope
) : DeviceRegistryManager {

    private val switchMutex = Mutex()

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
            // Session is restored in DefaultDeviceConnectionManager.init{} via restoreSession().
            // We just need to ensure the SessionStore has the right session.
            val currentSession = sessionStore.loadSession()
            if (currentSession?.peripheralId != defaultDevice.macAddress) {
                seedSessionForDevice(defaultDevice)
            }
            deviceConnectionManager.scheduleAutoReconnectIfNeeded()
        } else {
            ConnectivityLogger.d("🏠 Registry: no devices registered")
        }
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

        if (wasActive) {
            deviceConnectionManager.forgetDevice()
        }

        registry.remove(macAddress)
        refreshDeviceList()

        if (wasActive) {
            val newDefault = registry.getDefault()
            _activeDevice.value = newDefault
            if (newDefault != null) {
                seedSessionForDevice(newDefault)
                ConnectivityLogger.i("🏠 Registry: removed active device, promoted ${newDefault.macSuffix}")
            } else {
                ConnectivityLogger.i("🏠 Registry: removed last device, NeedsSetup")
            }
        } else if (wasDefault) {
            // Default was removed but it wasn't active — just refresh
            ConnectivityLogger.d("🏠 Registry: removed non-active default, new default promoted")
        }

        ConnectivityLogger.d("🏠 Registry: removed $macAddress")
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
}
