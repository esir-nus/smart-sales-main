package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import com.smartsales.prism.data.connectivity.legacy.gateway.RateLimitedBleGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DeviceConnectionManagerConnectionSupport(
    private val provisioner: WifiProvisioner,
    private val bleGateway: GattSessionLifecycle,
    private val dispatchers: DispatcherProvider,
    private val badgeStateMonitor: BadgeStateMonitor,
    private val sessionStore: SessionStore,
    private val phoneWifiProvider: PhoneWifiProvider,
    private val scope: CoroutineScope,
    private val runtime: DeviceConnectionManagerRuntime,
    private val ingressSupport: DeviceConnectionManagerIngressSupport
) {

    fun restoreSession() {
        val session = sessionStore.loadSession() ?: return
        runtime.currentSession = session
        runtime.lastCredentials = sessionStore.loadKnownNetworks()
            .maxByOrNull { it.lastUsedAtMillis }
            ?.credentials
        ConnectivityLogger.d(
            "🔌 Restored session: ${session.peripheralId} knownNetworks=${sessionStore.loadKnownNetworks().size}"
        )
    }

    fun cancelAllJobs() {
        runtime.heartbeatJob?.cancel()
        runtime.heartbeatJob = null
        runtime.notificationListenerJob?.cancel()
        runtime.notificationListenerJob = null
        runtime.notificationListenerActive = false
        cancelAutoRetry()
    }

    fun selectPeripheral(peripheral: BlePeripheral) {
        val session = BleSession.fromPeripheral(peripheral)
        runtime.currentSession = session

        scope.launch(dispatchers.io) {
            val result = bleGateway.connect(peripheral.id)
            if (result is Result.Success) {
                badgeStateMonitor.onBleConnected(session)
                ingressSupport.startNotificationListener(session)
                if (runtime.state.value !is ConnectionState.Pairing) {
                    runtime.state.value = ConnectionState.Connected(session)
                }
                ConnectivityLogger.i("📡 Persistent session + listener active for ${peripheral.name}")
            } else {
                runtime.notificationListenerActive = false
                ConnectivityLogger.w("⚠️ Failed to establish persistent GATT for ${peripheral.name}: $result")
                if (runtime.state.value !is ConnectionState.Pairing) {
                    runtime.state.value = ConnectionState.Error(
                        ConnectivityError.Transport("持久 BLE 通知通道未建立")
                    )
                }
            }
        }

        ConnectivityLogger.d(
            "🔌 Select peripheral id=${peripheral.id} name=${peripheral.name} profile=${peripheral.profileId ?: "dynamic"}"
        )
    }

    suspend fun startPairing(
        peripheral: BlePeripheral,
        credentials: WifiCredentials
    ): Result<Unit> = runtime.lock.withLock {
        if (runtime.state.value is ConnectionState.Pairing) {
            return@withLock Result.Error(
                IllegalStateException("Pairing already in progress for ${peripheral.name}")
            )
        }

        runtime.currentSession = BleSession.fromPeripheral(peripheral)
        runtime.lastCredentials = credentials
        runtime.autoRetryAttempts = 0
        cancelAutoRetry()
        runtime.state.value = ConnectionState.Pairing(
            deviceName = peripheral.name,
            progressPercent = 10,
            signalStrengthDbm = peripheral.signalStrengthDbm
        )
        launchProvisioning()
        Result.Success(Unit)
    }

    suspend fun retry(): Result<Unit> = runtime.lock.withLock {
        val session = runtime.currentSession
        val credentials = runtime.lastCredentials
        if (session == null || credentials == null) {
            return@withLock Result.Error(IllegalStateException("No previous pairing attempt to retry"))
        }
        runtime.heartbeatJob?.cancel()
        cancelAutoRetry()
        runtime.autoRetryAttempts = 0
        runtime.state.value = ConnectionState.Pairing(
            deviceName = session.peripheralName,
            progressPercent = 5,
            signalStrengthDbm = session.signalStrengthDbm
        )
        launchProvisioning()
        Result.Success(Unit)
    }

    fun disconnectBle() {
        cancelAllJobs()
        badgeStateMonitor.onBleDisconnected()
        scope.launch(dispatchers.io) { bleGateway.disconnect() }
        runtime.state.value = ConnectionState.Disconnected
        ConnectivityLogger.d("🔌 Soft disconnect (session preserved)")
    }

    fun forgetDevice() {
        cancelAllJobs()
        badgeStateMonitor.onBleDisconnected()
        scope.launch(dispatchers.io) { bleGateway.disconnect() }
        sessionStore.clear()
        runtime.currentSession = null
        runtime.lastCredentials = null
        runtime.reconnectMeta = AutoReconnectMeta()
        runtime.state.value = ConnectionState.Disconnected
        ConnectivityLogger.d("🔌 Hard disconnect (session cleared)")
    }

    suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
        withContext(dispatchers.io) {
            val session = runtime.currentSession
            if (session == null) {
                Result.Error(IllegalStateException("No active session"))
            } else {
                provisioner.requestHotspotCredentials(session)
            }
        }

    suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> =
        withContext(dispatchers.io) {
            val session = runtime.currentSession
            if (session == null) {
                Result.Error(IllegalStateException("No active session"))
            } else {
                performForegroundNetworkQuery(session, promoteConnectedState = true)
            }
        }

    suspend fun confirmManualWifiProvision(
        credentials: WifiCredentials
    ): ConnectionState = withContext(dispatchers.io) {
        val session = runtime.currentSession
            ?: return@withContext ConnectionState.Error(ConnectivityError.MissingSession)
        runtime.lastCredentials = credentials
        runtime.autoRetryAttempts = 0
        cancelAutoRetry()

        val outcome = waitForManualProvisionOnline(session, credentials)
        when (outcome) {
            is ConnectionState.WifiProvisioned -> {
                runtime.state.value = outcome
                startHeartbeat(outcome.session, outcome.status)
            }

            else -> {
                runtime.state.value = ConnectionState.Disconnected
            }
        }
        outcome
    }

    fun hasStoredSession(): Boolean = runtime.currentSession != null

    fun currentSessionOrNull(): BleSession? = runtime.currentSession

    fun startHeartbeat(session: BleSession, status: ProvisioningStatus) {
        runtime.heartbeatJob?.cancel()
        runtime.heartbeatJob = scope.launch(dispatchers.default) {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                runtime.state.value = ConnectionState.Syncing(
                    session = session,
                    status = status,
                    lastHeartbeatAtMillis = System.currentTimeMillis()
                )
            }
        }
    }

    suspend fun connectUsingSession(session: BleSession): ConnectionState {
        bleGateway.connect(session.peripheralId).let { result ->
            if (result is Result.Error) {
                ConnectivityLogger.w("🔌 Persistent GATT connect failed: ${result.throwable.message}")
                runtime.notificationListenerActive = false
                return ConnectionState.Error(
                    ConnectivityError.Transport("持久 BLE 通知通道未建立")
                )
            }
        }

        badgeStateMonitor.onBleConnected(session)
        ingressSupport.startNotificationListener(session)

        return when (val networkStatus = performForegroundNetworkQuery(session, promoteConnectedState = false)) {
            is Result.Success -> {
                resolveReconnectState(session, networkStatus.data)
            }
            is Result.Error -> {
                val error = mapProvisioningError(networkStatus.throwable)
                ConnectionState.Error(error)
            }
        }
    }

    private fun launchProvisioning() {
        val session = runtime.currentSession ?: return
        val credentials = runtime.lastCredentials ?: return
        runtime.heartbeatJob?.cancel()
        scope.launch(dispatchers.io) {
            when (val result = provisioner.provision(session, credentials)) {
                is Result.Success -> handleProvisioningSuccess(session, result.data)
                is Result.Error -> handleProvisioningFailure(session, result.throwable)
            }
        }
    }

    private fun handleProvisioningSuccess(session: BleSession, status: ProvisioningStatus) {
        runtime.autoRetryAttempts = 0
        cancelAutoRetry()
        ConnectivityLogger.i(
            "✅ Provision success device=${session.peripheralName} profile=${session.profileId ?: "dynamic"}"
        )

        runtime.lastCredentials?.let { credentials ->
            persistSessionAndKnownNetwork(session, credentials)
        }

        ingressSupport.startNotificationListener(session)
        runtime.state.value = ConnectionState.WifiProvisioned(session, status)
        startHeartbeat(session, status)
    }

    private fun handleProvisioningFailure(session: BleSession, throwable: Throwable) {
        val error = throwable.toConnectivityError()
        ConnectivityLogger.w(
            "❌ Provision failure device=${session.peripheralName} profile=${session.profileId ?: "dynamic"} error=$error",
            throwable
        )
        runtime.state.value = ConnectionState.Error(error)
        if (shouldAutoRetry(error)) {
            scheduleAutoRetry()
        }
    }

    private fun shouldAutoRetry(error: ConnectivityError): Boolean =
        error is ConnectivityError.Timeout || error is ConnectivityError.Transport

    private fun scheduleAutoRetry() {
        if (runtime.autoRetryAttempts >= AUTO_RETRY_MAX_ATTEMPTS) return
        runtime.autoRetryJob?.cancel()
        runtime.autoRetryJob = scope.launch(dispatchers.default) {
            delay(AUTO_RETRY_DELAY_MS)
            runtime.lock.withLock {
                val session = runtime.currentSession
                val credentials = runtime.lastCredentials
                if (session == null || credentials == null) return@withLock
                if (runtime.state.value !is ConnectionState.Error) return@withLock
                runtime.autoRetryAttempts += 1
                ConnectivityLogger.i(
                    "🔄 Auto retry #${runtime.autoRetryAttempts} for ${session.peripheralName}"
                )
                runtime.state.value = ConnectionState.Pairing(
                    deviceName = session.peripheralName,
                    progressPercent = 5,
                    signalStrengthDbm = session.signalStrengthDbm
                )
                launchProvisioning()
            }
        }
    }

    private fun cancelAutoRetry() {
        runtime.autoRetryJob?.cancel()
        runtime.autoRetryJob = null
    }

    private fun handleNetworkStatusSuccess(session: BleSession, status: DeviceNetworkStatus) {
        if (!runtime.notificationListenerActive) {
            ConnectivityLogger.w("⚠️ Skip connected-state promotion: notification listener inactive")
            return
        }

        if (!hasUsableBadgeIp(status.ipAddress)) {
            if (
                runtime.state.value is ConnectionState.WifiProvisioned ||
                runtime.state.value is ConnectionState.Syncing
            ) {
                runtime.heartbeatJob?.cancel()
                runtime.heartbeatJob = null
                runtime.state.value = ConnectionState.Disconnected
                ConnectivityLogger.w(
                    "⚠️ Network status lost usable IP, demoting shared transport readiness"
                )
            }
            return
        }

        val syntheticStatus = ingressSupport.syntheticProvisioningStatus(status)
        ConnectivityLogger.d(
            "🌐 Network status ok device=${session.peripheralName} ip=${status.ipAddress}"
        )
        runtime.state.value = ConnectionState.WifiProvisioned(session, syntheticStatus)
        startHeartbeat(session, syntheticStatus)
    }

    private fun mapProvisioningError(throwable: Throwable): ConnectivityError {
        return when (throwable) {
            is ProvisioningException.Transport -> {
                val msg = throwable.message ?: "传输失败"
                if (msg.contains("找不到设备")) {
                    ConnectivityError.DeviceNotFound(runtime.currentSession?.peripheralId ?: "unknown")
                } else {
                    ConnectivityError.Transport(msg)
                }
            }

            else -> throwable.toConnectivityError()
        }
    }

    private suspend fun performForegroundNetworkQuery(
        session: BleSession,
        promoteConnectedState: Boolean
    ): Result<DeviceNetworkStatus> {
        return when (val result = queryNetworkStatusWithFloorRetry(session)) {
            is Result.Success -> {
                badgeStateMonitor.onNetworkStatusObserved(result.data)
                if (promoteConnectedState) {
                    handleNetworkStatusSuccess(session, result.data)
                }
                result
            }

            is Result.Error -> {
                badgeStateMonitor.onNetworkStatusQueryFailed()
                result
            }
        }
    }

    private suspend fun queryNetworkStatusWithFloorRetry(
        session: BleSession
    ): Result<DeviceNetworkStatus> {
        val firstAttempt = provisioner.queryNetworkStatus(session)
        if (!shouldRetryAfterQueryFloorTimeout(firstAttempt)) {
            return firstAttempt
        }

        ConnectivityLogger.d(
            "📡 foreground query hit ${QUERY_FLOOR_TIMEOUT_MS}ms BLE floor, retrying after guard delay"
        )
        delay(QUERY_FLOOR_RETRY_DELAY_MS)
        return provisioner.queryNetworkStatus(session)
    }

    private fun shouldRetryAfterQueryFloorTimeout(
        result: Result<DeviceNetworkStatus>
    ): Boolean {
        val timeout = (result as? Result.Error)?.throwable as? ProvisioningException.Timeout
        return timeout?.timeoutMillis == QUERY_FLOOR_TIMEOUT_MS
    }

    private suspend fun resolveReconnectState(
        session: BleSession,
        networkStatus: DeviceNetworkStatus
    ): ConnectionState {
        val ip = networkStatus.ipAddress
        val badgeSsid = normalizeWifiSsid(networkStatus.deviceWifiName)
        val phoneSsid = resolveReadablePhoneSsid()
            ?: return resolvePhoneWifiUnavailableState(
                context = if (!hasUsableBadgeIp(ip)) {
                    "replay skipped"
                } else {
                    "reconnect blocked while badge has ip=$ip"
                },
                badgeSsid = badgeSsid
            )

        if (!hasUsableBadgeIp(ip)) {
            ConnectivityLogger.d("🔌 connectUsingSession: badge offline, ip=$ip")
            return attemptDeterministicWifiReplay(
                session = session,
                badgeSsid = badgeSsid,
                phoneSsid = phoneSsid,
                reason = "badge offline"
            )
        }

        if (badgeSsid != phoneSsid) {
            ConnectivityLogger.w(
                "🔌 reconnect mismatch: badge=$badgeSsid phone=$phoneSsid, attempting alignment replay"
            )
            return attemptDeterministicWifiReplay(
                session = session,
                badgeSsid = badgeSsid,
                phoneSsid = phoneSsid,
                reason = "badge online on different network"
            )
        }

        ConnectivityLogger.d("🔌 connectUsingSession: connected, ip=$ip ssid=$badgeSsid")
        return ConnectionState.WifiProvisioned(
            session,
            ingressSupport.syntheticProvisioningStatus(networkStatus)
        )
    }

    private suspend fun attemptDeterministicWifiReplay(
        session: BleSession,
        badgeSsid: String?,
        phoneSsid: String,
        reason: String
    ): ConnectionState {
        val knownNetwork = sessionStore.findKnownNetworkBySsid(phoneSsid)
        if (knownNetwork == null) {
            ConnectivityLogger.w(
                "🔁 replay skipped ($reason): no known network for phoneSsid=$phoneSsid"
            )
            return ConnectionState.Error(
                ConnectivityError.WifiDisconnected(
                    reason = WifiDisconnectedReason.NO_KNOWN_CREDENTIAL_FOR_PHONE_WIFI,
                    phoneSsid = phoneSsid,
                    badgeSsid = badgeSsid
                )
            )
        }

        val replayCredentials = knownNetwork.credentials
        runtime.lastCredentials = replayCredentials
        ConnectivityLogger.i(
            "🔁 replaying saved Wi‑Fi credentials ssid=${replayCredentials.ssid} because $reason"
        )
        when (val provisionResult = provisioner.provision(session, replayCredentials)) {
            is Result.Success -> {
                return waitForReconnectReplayOnline(session, replayCredentials, phoneSsid)
            }

            is Result.Error -> {
                ConnectivityLogger.w(
                    "🔁 replay failed before online confirmation: ${provisionResult.throwable.message}"
                )
                return ConnectionState.Error(
                    ConnectivityError.WifiDisconnected(
                        reason = WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED,
                        phoneSsid = phoneSsid,
                        badgeSsid = badgeSsid
                    )
                )
            }
        }
    }

    private suspend fun waitForReconnectReplayOnline(
        session: BleSession,
        credentials: WifiCredentials,
        expectedPhoneSsid: String
    ): ConnectionState {
        repeat(RECONNECT_REPLAY_QUERY_ATTEMPTS) { attempt ->
            val delayMs = if (attempt == 0) {
                RECONNECT_REPLAY_FIRST_QUERY_DELAY_MS
            } else {
                RECONNECT_REPLAY_QUERY_DELAY_MS
            }
            delay(delayMs)

            when (val result = performForegroundNetworkQuery(session, promoteConnectedState = false)) {
                is Result.Success -> {
                    val status = result.data
                    val ip = status.ipAddress
                    if (!hasUsableBadgeIp(ip)) {
                        ConnectivityLogger.d("🔁 replay confirm attempt=${attempt + 1}: still offline ip=$ip")
                        return@repeat
                    }

                    val badgeSsid = normalizeWifiSsid(status.deviceWifiName)
                    if (badgeSsid != expectedPhoneSsid) {
                        ConnectivityLogger.w(
                            "🔁 replay confirm mismatch: badge=$badgeSsid phone=$expectedPhoneSsid"
                        )
                        return ConnectionState.Error(
                            ConnectivityError.WifiDisconnected(
                                reason = WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH,
                                phoneSsid = expectedPhoneSsid,
                                badgeSsid = badgeSsid
                            )
                        )
                    }

                    persistSessionAndKnownNetwork(session, credentials)
                    ConnectivityLogger.i("🔁 replay confirmed online ip=$ip ssid=$badgeSsid")
                    return ConnectionState.WifiProvisioned(
                        session,
                        ingressSupport.syntheticProvisioningStatus(status)
                    )
                }

                is Result.Error -> {
                    ConnectivityLogger.w(
                        "🔁 replay confirm query failed attempt=${attempt + 1}: ${result.throwable.message}"
                    )
                }
            }
        }

        return ConnectionState.Error(
            ConnectivityError.WifiDisconnected(
                reason = WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED,
                phoneSsid = expectedPhoneSsid
            )
        )
    }

    private suspend fun waitForManualProvisionOnline(
        session: BleSession,
        credentials: WifiCredentials
    ): ConnectionState {
        val expectedSsid = normalizeWifiSsid(credentials.ssid)

        repeat(MANUAL_PROVISION_QUERY_ATTEMPTS) { attempt ->
            delay(MANUAL_PROVISION_QUERY_DELAY_MS)

            when (val result = performForegroundNetworkQuery(session, promoteConnectedState = false)) {
                is Result.Success -> {
                    val status = result.data
                    val ip = status.ipAddress
                    if (!hasUsableBadgeIp(ip)) {
                        ConnectivityLogger.d(
                            "🛜 manual repair confirm attempt=${attempt + 1}: badge still offline ip=$ip"
                        )
                        return@repeat
                    }

                    val badgeSsid = normalizeWifiSsid(status.deviceWifiName)
                    if (expectedSsid != null && badgeSsid != null && badgeSsid != expectedSsid) {
                        ConnectivityLogger.w(
                            "🛜 manual repair mismatch: expected=$expectedSsid badge=$badgeSsid"
                        )
                        return ConnectionState.Error(
                            ConnectivityError.WifiDisconnected(
                                reason = WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH,
                                phoneSsid = expectedSsid,
                                badgeSsid = badgeSsid
                            )
                        )
                    }

                    if (badgeSsid == null) {
                        ConnectivityLogger.w(
                            "🛜 manual repair confirm attempt=${attempt + 1}: ip=$ip but badge ssid unreadable raw=${status.rawResponse}"
                        )
                        return@repeat
                    }

                    persistSessionAndKnownNetwork(session, credentials)
                    ConnectivityLogger.i(
                        "🛜 manual repair confirmed online ip=$ip ssid=$badgeSsid"
                    )
                    return ConnectionState.WifiProvisioned(
                        session,
                        ingressSupport.syntheticProvisioningStatus(status)
                    )
                }

                is Result.Error -> {
                    ConnectivityLogger.w(
                        "🛜 manual repair confirm query failed attempt=${attempt + 1}: ${result.throwable.message}"
                    )
                }
            }
        }

        return ConnectionState.Error(
            ConnectivityError.WifiDisconnected(
                reason = WifiDisconnectedReason.BADGE_WIFI_OFFLINE,
                badgeSsid = expectedSsid
            )
        )
    }

    private fun resolveReadablePhoneSsid(): String? {
        return when (val snapshot = phoneWifiProvider.currentWifiSnapshot()) {
            PhoneWifiSnapshot.Unavailable -> null
            is PhoneWifiSnapshot.Connected -> snapshot.normalizedSsid
        }
    }

    private fun resolvePhoneWifiUnavailableState(
        context: String,
        badgeSsid: String?
    ): ConnectionState {
        return when (val snapshot = phoneWifiProvider.currentWifiSnapshot()) {
            PhoneWifiSnapshot.Unavailable -> {
                ConnectivityLogger.w("🔁 $context: phone Wi‑Fi unavailable")
                ConnectionState.Error(
                    ConnectivityError.WifiDisconnected(
                        reason = WifiDisconnectedReason.PHONE_WIFI_UNAVAILABLE,
                        badgeSsid = badgeSsid
                    )
                )
            }

            is PhoneWifiSnapshot.Connected -> {
                ConnectivityLogger.w(
                    "🔁 $context: phone Wi‑Fi connected but SSID unreadable raw=${snapshot.rawSsid ?: "null"}"
                )
                ConnectionState.Error(
                    ConnectivityError.WifiDisconnected(
                        reason = WifiDisconnectedReason.PHONE_WIFI_SSID_UNREADABLE,
                        badgeSsid = badgeSsid
                    )
                )
            }
        }
    }

    private fun persistSessionAndKnownNetwork(session: BleSession, credentials: WifiCredentials) {
        sessionStore.saveSession(session)
        sessionStore.upsertKnownNetwork(credentials)
        ConnectivityLogger.d("🔌 Session persisted: ${session.peripheralId}")
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 1_500L
        const val AUTO_RETRY_DELAY_MS = 2_000L
        const val AUTO_RETRY_MAX_ATTEMPTS = 2
        const val RECONNECT_REPLAY_FIRST_QUERY_DELAY_MS = 2_200L
        const val RECONNECT_REPLAY_QUERY_DELAY_MS = 2_000L
        const val RECONNECT_REPLAY_QUERY_ATTEMPTS = 3
        const val MANUAL_PROVISION_QUERY_DELAY_MS = 1_500L
        const val MANUAL_PROVISION_QUERY_ATTEMPTS = 3
        const val QUERY_FLOOR_TIMEOUT_MS = RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS
        const val QUERY_FLOOR_RETRY_DELAY_MS = RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS + 100L
    }
}
