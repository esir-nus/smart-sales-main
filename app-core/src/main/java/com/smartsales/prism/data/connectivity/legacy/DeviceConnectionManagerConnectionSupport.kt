package com.smartsales.prism.data.connectivity.legacy

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.BadgeRuntimeKey
import com.smartsales.prism.data.connectivity.legacy.badge.BadgeStateMonitor
import com.smartsales.prism.data.connectivity.legacy.gateway.GattSessionLifecycle
import com.smartsales.prism.data.connectivity.legacy.gateway.RateLimitedBleGateway
import com.smartsales.prism.data.connectivity.legacy.scan.BleScanner
import com.smartsales.prism.domain.connectivity.WifiRepairEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class DeviceConnectionManagerConnectionSupport(
    private val provisioner: WifiProvisioner,
    private val bleGateway: GattSessionLifecycle,
    private val httpReachabilityProbe: suspend (String) -> Boolean,
    private val endpointRecoveryCoordinator: BadgeEndpointRecoveryCoordinator,
    private val dispatchers: DispatcherProvider,
    private val badgeStateMonitor: BadgeStateMonitor,
    private val sessionStore: SessionStore,
    private val scope: CoroutineScope,
    private val runtime: DeviceConnectionManagerRuntime,
    private val ingressSupport: DeviceConnectionManagerIngressSupport,
    private val bleScanner: BleScanner? = null
) {

    internal var reconnectSupport: DeviceConnectionManagerReconnectSupport? = null

    fun restoreSession() {
        val session = sessionStore.loadSession() ?: return
        useSession(session)
    }

    fun useSession(session: BleSession) {
        val knownNetworks = sessionStore.loadKnownNetworks()
        runtime.currentSession = session
        runtime.lastCredentials = knownNetworks.maxByOrNull { it.lastUsedAtMillis }?.credentials
        ConnectivityLogger.d("🔌 Restored session: ${session.peripheralId} knownNetworks=${knownNetworks.size}")
    }

    fun cancelAllJobs() {
        runtime.heartbeatJob?.cancel()
        runtime.heartbeatJob = null
        runtime.notificationListenerJob?.cancel()
        runtime.notificationListenerJob = null
        runtime.reconnectJob?.cancel()
        runtime.reconnectJob = null
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
        runtime.state.value = peripheral.toPairingState(progressPercent = 10)
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
        runtime.state.value = session.toPairingState(progressPercent = 5)
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

    suspend fun requestHotspotCredentials(): Result<WifiCredentials> = withContext(dispatchers.io) {
        runtime.currentSession?.let { provisioner.requestHotspotCredentials(it) }
            ?: Result.Error(IllegalStateException("No active session"))
    }

    suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = withContext(dispatchers.io) {
        runtime.currentSession?.let { performForegroundNetworkQuery(it, promoteConnectedState = true) }
            ?: Result.Error(IllegalStateException("No active session"))
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
        val heartbeatTarget = when (outcome) {
            is ConnectionState.WifiProvisioned -> outcome
            is ConnectionState.WifiProvisionedHttpDelayed ->
                ConnectionState.WifiProvisioned(outcome.session, outcome.status)
            else -> null
        }
        if (heartbeatTarget != null) {
            runtime.state.value = heartbeatTarget
            startHeartbeat(heartbeatTarget.session, heartbeatTarget.status)
        } else {
            runtime.state.value = ConnectionState.Disconnected
        }
        outcome
    }

    suspend fun replayLatestSavedWifiCredentialForMediaFailure(): ConnectionState = withContext(dispatchers.io) {
        val session = runtime.currentSession
            ?: return@withContext ConnectionState.Error(ConnectivityError.MissingSession)
        val latestCredentials = latestSavedCredentials()
            ?: return@withContext wifiDisconnectedError(WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED)
        ConnectivityLogger.w(
            "🛜 solid IP + media sync failure; replaying latest saved credential ssid=${latestCredentials.ssid}"
        )
        replaySavedCredential(
            session = session,
            credentials = latestCredentials,
            initialDelayMs = MEDIA_REPLAY_FIRST_QUERY_DELAY_MS,
            retryDelayMs = MEDIA_REPLAY_QUERY_DELAY_MS,
            source = "media_failure",
            failureBadgeSsid = normalizeWifiSsid(latestCredentials.ssid)
        )
    }

    fun hasStoredSession(): Boolean = runtime.currentSession != null

    fun currentSessionOrNull(): BleSession? = runtime.currentSession

    fun startHeartbeat(session: BleSession, status: ProvisioningStatus) {
        runtime.heartbeatJob?.cancel()
        runtime.heartbeatJob = scope.launch(dispatchers.default) {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (bleGateway.isReachable()) {
                    runtime.state.value = ConnectionState.Syncing(
                        session = session,
                        status = status,
                        lastHeartbeatAtMillis = System.currentTimeMillis()
                    )
                } else {
                    ConnectivityLogger.w("💔 Heartbeat: badge unreachable, transitioning to Disconnected")
                    badgeStateMonitor.onBleDisconnected()
                    runtime.state.value = ConnectionState.Disconnected
                    reconnectSupport?.scheduleAutoReconnectIfNeeded()
                    return@launch
                }
            }
        }
    }

    suspend fun connectUsingSession(session: BleSession): ConnectionState {
        // 切换设备时先同步清理旧 GATT，避免网关复用旧徽章会话。
        cancelActiveTransportJobs()
        badgeStateMonitor.onBleDisconnected()
        ConnectivityLogger.d("🔌 Soft disconnect (session preserved)")
        bleGateway.disconnect()
        val activeSession = connectOrRescan(session)
            ?: return ConnectionState.Error(ConnectivityError.DeviceNotFound(session.peripheralId))

        badgeStateMonitor.onBleConnected(activeSession)
        ingressSupport.startNotificationListener(activeSession)

        return when (val status = performForegroundNetworkQuery(activeSession, promoteConnectedState = false)) {
            is Result.Success -> resolveReconnectState(activeSession, status.data)
            is Result.Error -> ConnectionState.Error(mapProvisioningError(status.throwable))
        }
    }

    private fun cancelActiveTransportJobs() {
        runtime.heartbeatJob?.cancel()
        runtime.heartbeatJob = null
        runtime.notificationListenerJob?.cancel()
        runtime.notificationListenerJob = null
        runtime.notificationListenerActive = false
    }

    private suspend fun connectOrRescan(session: BleSession): BleSession? {
        val directResult = bleGateway.connect(session.peripheralId)
        if (directResult is Result.Success) return session

        ConnectivityLogger.w("🔌 Stored MAC ${session.peripheralId} unreachable, attempting scan fallback")
        runtime.notificationListenerActive = false

        val peripheral = bleScanner?.scanForFirst() ?: run {
            ConnectivityLogger.w("🔌 Scan fallback: no matching device found")
            return null
        }

        if (peripheral.id == session.peripheralId) {
            ConnectivityLogger.w("🔌 Scan fallback: same MAC found but connect failed earlier")
            return null
        }

        ConnectivityLogger.i("🔌 Scan fallback: found ${peripheral.name} at new MAC ${peripheral.id}")
        val updatedSession = session.copy(
            peripheralId = peripheral.id,
            peripheralName = peripheral.name,
            signalStrengthDbm = peripheral.signalStrengthDbm
        )

        val retryResult = bleGateway.connect(updatedSession.peripheralId)
        if (retryResult is Result.Error) {
            ConnectivityLogger.w("🔌 Scan fallback: connect to new MAC also failed: ${retryResult.throwable.message}")
            return null
        }

        runtime.currentSession = updatedSession
        sessionStore.saveSession(updatedSession)
        ConnectivityLogger.i("🔌 Session updated to new MAC ${updatedSession.peripheralId}")
        return updatedSession
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
        ConnectivityLogger.i("✅ Provision success device=${session.peripheralName} profile=${session.profileId ?: "dynamic"}")
        runtime.lastCredentials?.let { persistSessionAndKnownNetwork(session, it) }

        ingressSupport.startNotificationListener(session)
        runtime.state.value = ConnectionState.WifiProvisioned(session, status)
        startHeartbeat(session, status)
    }

    private fun handleProvisioningFailure(session: BleSession, throwable: Throwable) {
        val error = throwable.toConnectivityError()
        ConnectivityLogger.w("❌ Provision failure device=${session.peripheralName} profile=${session.profileId ?: "dynamic"} error=$error", throwable)
        runtime.state.value = ConnectionState.Error(error)
        if (shouldAutoRetry(error)) scheduleAutoRetry()
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
                runtime.state.value = session.toPairingState(progressPercent = 5)
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
            if (runtime.state.value is ConnectionState.WifiProvisioned || runtime.state.value is ConnectionState.Syncing) {
                runtime.heartbeatJob?.cancel()
                runtime.heartbeatJob = null
                runtime.state.value = ConnectionState.Disconnected
                ConnectivityLogger.w("⚠️ Network status lost usable IP, demoting shared transport readiness")
            }
            return
        }

        val syntheticStatus = ingressSupport.syntheticProvisioningStatus(status)
        ConnectivityLogger.d("🌐 Network status ok device=${session.peripheralName} ip=${status.ipAddress}")
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

    private suspend fun performForegroundNetworkQuery(session: BleSession, promoteConnectedState: Boolean): Result<DeviceNetworkStatus> {
        return when (val result = queryNetworkStatusWithFloorRetry(session)) {
            is Result.Success -> {
                badgeStateMonitor.onNetworkStatusObserved(result.data)
                if (promoteConnectedState) handleNetworkStatusSuccess(session, result.data)
                result
            }
            is Result.Error -> {
                badgeStateMonitor.onNetworkStatusQueryFailed()
                result
            }
        }
    }

    private suspend fun queryNetworkStatusWithFloorRetry(session: BleSession): Result<DeviceNetworkStatus> {
        val firstAttempt = provisioner.queryNetworkStatus(session)
        if (!shouldRetryAfterQueryFloorTimeout(firstAttempt)) return firstAttempt

        ConnectivityLogger.d("📡 foreground query hit ${QUERY_FLOOR_TIMEOUT_MS}ms BLE floor, retrying after guard delay")
        delay(QUERY_FLOOR_RETRY_DELAY_MS)
        return provisioner.queryNetworkStatus(session)
    }

    private fun shouldRetryAfterQueryFloorTimeout(result: Result<DeviceNetworkStatus>): Boolean {
        val timeout = (result as? Result.Error)?.throwable as? ProvisioningException.Timeout
        return timeout?.timeoutMillis == QUERY_FLOOR_TIMEOUT_MS
    }

    private fun resolveReconnectState(session: BleSession, networkStatus: DeviceNetworkStatus): ConnectionState {
        val ip = networkStatus.ipAddress
        val badgeSsid = normalizeWifiSsid(networkStatus.deviceWifiName)

        if (hasUsableBadgeIp(ip)) {
            ConnectivityLogger.d("🔌 connectUsingSession: connected, ip=$ip ssid=$badgeSsid")
            return ConnectionState.WifiProvisioned(session, ingressSupport.syntheticProvisioningStatus(networkStatus))
        }

        ConnectivityLogger.w("🔌 badge reported IP#0.0.0.0 or unusable ip=$ip")
        ConnectivityLogger.w("🛜 badge IP#0.0.0.0 branch: prompt manual Wi-Fi repair, skip silent replay")
        return wifiDisconnectedError(WifiDisconnectedReason.BADGE_WIFI_OFFLINE, badgeSsid = badgeSsid)
    }

    private suspend fun replaySavedCredential(
        session: BleSession,
        credentials: WifiCredentials,
        initialDelayMs: Long,
        retryDelayMs: Long,
        source: String,
        failureBadgeSsid: String?
    ): ConnectionState {
        repeat(RECONNECT_REPLAY_QUERY_ATTEMPTS) { attempt ->
            val attemptNumber = attempt + 1
            ConnectivityLogger.i("🔁 saved credential replay attempt=$attemptNumber/$RECONNECT_REPLAY_QUERY_ATTEMPTS source=$source ssid=${credentials.ssid}")
            when (val provisionResult = provisioner.provision(session, credentials)) {
                is Result.Success -> {
                    endpointRecoveryCoordinator.armPostCredentialGrace(session.runtimeKey())
                    ConnectivityLogger.i("🔁 saved credential replay write ok attempt=$attemptNumber source=$source")
                }
                is Result.Error -> {
                    ConnectivityLogger.w("🔁 saved credential replay write failed attempt=$attemptNumber source=$source error=${provisionResult.throwable.message}")
                    if (attempt < RECONNECT_REPLAY_QUERY_ATTEMPTS - 1) delay(retryDelayMs)
                    return@repeat
                }
            }

            delay(if (attempt == 0) initialDelayMs else retryDelayMs)

            when (val result = performForegroundNetworkQuery(session, promoteConnectedState = false)) {
                is Result.Success -> {
                    val status = result.data
                    val ip = status.ipAddress
                    if (!hasUsableBadgeIp(ip)) {
                        ConnectivityLogger.d("🔁 replay confirm attempt=${attempt + 1}: still offline ip=$ip")
                        return@repeat
                    }

                    val badgeSsid = normalizeWifiSsid(status.deviceWifiName)
                    persistSessionAndKnownNetwork(session, credentials)
                    ConnectivityLogger.i("🔁 saved credential replay confirmed online attempt=$attemptNumber source=$source ip=$ip ssid=$badgeSsid")
                    return ConnectionState.WifiProvisioned(session, ingressSupport.syntheticProvisioningStatus(status))
                }
                is Result.Error -> ConnectivityLogger.w(
                    "🔁 replay confirm query failed attempt=$attemptNumber source=$source: ${result.throwable.message}"
                )
            }
        }

        ConnectivityLogger.w("🔁 saved credential replay exhausted source=$source")
        return wifiDisconnectedError(WifiDisconnectedReason.CREDENTIAL_REPLAY_FAILED, badgeSsid = failureBadgeSsid)
    }

    private fun latestSavedCredentials(): WifiCredentials? =
        runtime.lastCredentials ?: sessionStore.loadKnownNetworks()
            .maxByOrNull { it.lastUsedAtMillis }
            ?.credentials
            ?.also { runtime.lastCredentials = it }

    private suspend fun waitForManualProvisionOnline(
        session: BleSession,
        credentials: WifiCredentials
    ): ConnectionState {
        val expectedSsid = normalizeWifiSsid(credentials.ssid)
        val runtimeKey = session.runtimeKey()
        runtime.repairEvents.tryEmit(WifiRepairEvent.CredentialsDispatched(credentials.ssid))

        repeat(MANUAL_PROVISION_QUERY_ATTEMPTS) { attempt ->
            endpointRecoveryCoordinator
                .consumePostCredentialProbeDelayMs(runtimeKey = runtimeKey, allowImplicitArm = true)
                ?.let { delayMs ->
                    ConnectivityLogger.i("🛜 repair readiness grace wait=${delayMs}ms attempt=${attempt + 1} runtime=${runtimeKey.toLogString()}")
                delay(delayMs)
            }
            val attemptNumber = attempt + 1

            when (val result = performForegroundNetworkQuery(session, promoteConnectedState = false)) {
                is Result.Success -> {
                    val status = result.data
                    val ip = status.ipAddress
                    val badgeSsid = normalizeWifiSsid(status.deviceWifiName)
                    ConnectivityLogger.d("🛜 repair query attempt=$attemptNumber ip=$ip badgeSsid=${badgeSsid ?: "null"}")

                    if (!hasUsableBadgeIp(ip)) {
                        ConnectivityLogger.d("🛜 manual repair confirm attempt=$attemptNumber: badge still offline ip=$ip")
                        runtime.repairEvents.tryEmit(WifiRepairEvent.BadgeOffline)
                        return@repeat
                    }

                    runtime.repairEvents.tryEmit(WifiRepairEvent.UsableIpObserved(ip))

                    if (expectedSsid != null && badgeSsid != null && badgeSsid != expectedSsid) {
                        endpointRecoveryCoordinator.clearPostCredentialGrace(runtimeKey)
                        ConnectivityLogger.w("🛜 manual repair mismatch: expected=$expectedSsid badge=$badgeSsid")
                        ConnectivityLogger.w("🛜 repair outcome=badge_phone_network_mismatch")
                        runtime.repairEvents.tryEmit(WifiRepairEvent.DefinitiveMismatch(expectedSsid, badgeSsid))
                        return wifiDisconnectedError(
                            WifiDisconnectedReason.BADGE_PHONE_NETWORK_MISMATCH,
                            phoneSsid = expectedSsid,
                            badgeSsid = badgeSsid
                        )
                    }

                    if (badgeSsid == null) {
                        ConnectivityLogger.w(
                            "🛜 manual repair confirm attempt=$attemptNumber: ip=$ip but badge ssid unreadable raw=${status.rawResponse}"
                        )
                        return@repeat
                    }

                    // 传输已确认：IP 可用 + SSID 匹配
                    runtime.repairEvents.tryEmit(WifiRepairEvent.TargetSsidObserved(badgeSsid))
                    runtime.repairEvents.tryEmit(WifiRepairEvent.TransportConfirmed(ip, badgeSsid, runtimeKey.toLogString()))
                    ConnectivityLogger.i("🛜 repair TRANSPORT_CONFIRMED ip=$ip ssid=$badgeSsid runtime=${runtimeKey.toLogString()}")

                    val baseUrl = "http://$ip:8088"
                    val reachable = httpReachabilityProbe(baseUrl)
                    ConnectivityLogger.i("🛜 repair probe attempt=$attemptNumber url=$baseUrl reachable=$reachable")

                    if (reachable) {
                        persistSessionAndKnownNetwork(session, credentials)
                        endpointRecoveryCoordinator.clearPostCredentialGrace(runtimeKey)
                        runtime.repairEvents.tryEmit(WifiRepairEvent.HttpReady(baseUrl))
                        ConnectivityLogger.i("🛜 repair outcome=wifi_provisioned ip=$ip ssid=$badgeSsid")
                        return ConnectionState.WifiProvisioned(session, ingressSupport.syntheticProvisioningStatus(status))
                    }

                    if (attempt < MANUAL_PROVISION_QUERY_ATTEMPTS - 1) return@repeat

                    persistSessionAndKnownNetwork(session, credentials)
                    endpointRecoveryCoordinator.clearPostCredentialGrace(runtimeKey)
                    runtime.repairEvents.tryEmit(WifiRepairEvent.HttpDelayed(baseUrl))
                    ConnectivityLogger.i("🛜 repair HTTP_DELAYED baseUrl=$baseUrl runtime=${runtimeKey.toLogString()}")
                    ConnectivityLogger.i("🛜 repair outcome=http_delayed (transport confirmed)")
                    return ConnectionState.WifiProvisionedHttpDelayed(
                        session,
                        ingressSupport.syntheticProvisioningStatus(status),
                        baseUrl
                    )
                }

                is Result.Error -> {
                    ConnectivityLogger.w("🛜 repair query attempt=$attemptNumber error=${result.throwable.message}")
                    ConnectivityLogger.w("🛜 manual repair confirm query failed attempt=$attemptNumber: ${result.throwable.message}")
                }
            }
        }

        endpointRecoveryCoordinator.clearPostCredentialGrace(runtimeKey)
        ConnectivityLogger.w("🛜 repair outcome=badge_wifi_offline")
        return wifiDisconnectedError(WifiDisconnectedReason.BADGE_WIFI_OFFLINE, badgeSsid = expectedSsid)
    }

    private fun persistSessionAndKnownNetwork(session: BleSession, credentials: WifiCredentials) {
        sessionStore.saveSession(session)
        sessionStore.upsertKnownNetwork(credentials)
        ConnectivityLogger.d("🔌 Session persisted: ${session.peripheralId}")
    }

    private fun BlePeripheral.toPairingState(progressPercent: Int): ConnectionState.Pairing =
        ConnectionState.Pairing(name, progressPercent, signalStrengthDbm)

    private fun BleSession.toPairingState(progressPercent: Int): ConnectionState.Pairing =
        ConnectionState.Pairing(peripheralName, progressPercent, signalStrengthDbm)

    private fun wifiDisconnectedError(
        reason: WifiDisconnectedReason,
        phoneSsid: String? = null,
        badgeSsid: String? = null
    ): ConnectionState.Error = ConnectionState.Error(
        ConnectivityError.WifiDisconnected(reason = reason, phoneSsid = phoneSsid, badgeSsid = badgeSsid)
    )

    private fun BleSession.runtimeKey(): BadgeRuntimeKey =
        BadgeRuntimeKey(peripheralId = peripheralId, secureToken = secureToken)

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 1_500L
        const val AUTO_RETRY_DELAY_MS = 2_000L
        const val AUTO_RETRY_MAX_ATTEMPTS = 2
        const val RECONNECT_REPLAY_QUERY_ATTEMPTS = 3
        const val MEDIA_REPLAY_FIRST_QUERY_DELAY_MS = 2_200L
        const val MEDIA_REPLAY_QUERY_DELAY_MS = 2_000L
        const val MANUAL_PROVISION_QUERY_ATTEMPTS = 3
        const val QUERY_FLOOR_TIMEOUT_MS = RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS
        const val QUERY_FLOOR_RETRY_DELAY_MS = RateLimitedBleGateway.MIN_QUERY_INTERVAL_MS + 100L
    }
}
