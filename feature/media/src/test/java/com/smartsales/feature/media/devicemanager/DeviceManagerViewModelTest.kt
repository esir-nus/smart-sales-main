package com.smartsales.feature.media.devicemanager

// 文件：feature/media/src/test/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModelTest.kt
// 模块：:feature:media
// 说明：验证 DeviceManagerViewModel 的列表加载、筛选与操作能力
// 作者：创建于 2025-11-20

import android.net.Uri
import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.ProvisioningStatus
import com.smartsales.feature.connectivity.WifiCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceManagerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DeviceManagerViewModel
    private lateinit var gateway: FakeDeviceMediaGateway
    private lateinit var connectionManager: FakeDeviceConnectionManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gateway = FakeDeviceMediaGateway()
        connectionManager = FakeDeviceConnectionManager()
        connectionManager.emitReady()
        viewModel = DeviceManagerViewModel(
            gateway,
            connectionManager,
            FakeDispatcherProvider(dispatcher)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh success populates files`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("image-1.jpg", 1024, "image/jpeg", 1_000L, "media/1", "dl/1"),
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2")
        )

        viewModel.onRefreshFiles()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.files.size)
        assertEquals(1, state.visibleFiles.size) // 默认图片标签
    }

    @Test
    fun `refresh failure surfaces error`() = runTest(dispatcher) {
        gateway.fetchResult = Result.Error(IllegalStateException("offline"))

        viewModel.onRefreshFiles()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("offline", state.errorMessage)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun `switch tab filters files`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("image-1.jpg", 1024, "image/jpeg", 1_000L, "media/1", "dl/1"),
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2")
        )
        viewModel.onRefreshFiles()
        advanceUntilIdle()

        viewModel.onSelectTab(DeviceMediaTab.Videos)
        val state = viewModel.uiState.value
        assertEquals(1, state.visibleFiles.size)
        assertEquals("clip.mp4", state.visibleFiles.first().displayName)
    }

    @Test
    fun `apply file marks ui`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2")
        )
        viewModel.onRefreshFiles()
        advanceUntilIdle()

        viewModel.onApplyFile("clip.mp4")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(gateway.appliedNames.contains("clip.mp4"))
        assertEquals(true, state.files.first().isApplied)
    }

    @Test
    fun `delete file removes entry`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2")
        )
        viewModel.onRefreshFiles()
        advanceUntilIdle()

        viewModel.onDeleteFile("clip.mp4")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.files.size)
    }

    @Test
    fun `upload triggers fetch`() = runTest(dispatcher) {
        gateway.files = emptyList()
        viewModel.onRefreshFiles()
        advanceUntilIdle()

        gateway.files = listOf(
            DeviceMediaFile("new.png", 1024, "image/png", 3_000L, "media/3", "dl/3")
        )

        viewModel.onUploadFile(DeviceUploadSource.AndroidUri(Uri.EMPTY))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, gateway.uploadCalls)
        assertEquals(1, state.files.size)
        assertEquals(false, state.isUploading)
    }

    @Test
    fun `auto detected base url populates state and refreshes`() = runTest(dispatcher) {
        val session = createSession("session-auto")
        connectionManager.networkResult = Result.Success(
            DeviceNetworkStatus("192.168.4.2", "Device", "Phone", "raw")
        )
        connectionManager.emitState(
            ConnectionState.WifiProvisioned(session, ProvisioningStatus("WiFi", "h", "hash"))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("http://192.168.4.2:8000", state.baseUrl)
        assertEquals("http://192.168.4.2:8000", state.autoDetectedBaseUrl)
        assertEquals(false, state.isBaseUrlManual)
        assertEquals(1, gateway.fetchCalls)
    }

    @Test
    fun `manual override holds even when new auto base arrives`() = runTest(dispatcher) {
        val firstSession = createSession("session-auto-1")
        connectionManager.networkResult = Result.Success(
            DeviceNetworkStatus("192.168.4.5", "Device", "Phone", "raw")
        )
        connectionManager.emitState(
            ConnectionState.WifiProvisioned(firstSession, ProvisioningStatus("WiFi", "h", "hash"))
        )
        advanceUntilIdle()

        viewModel.onBaseUrlChanged("http://custom.local")
        val nextSession = createSession("session-auto-2")
        connectionManager.networkResult = Result.Success(
            DeviceNetworkStatus("192.168.4.7", "Device", "Phone", "raw")
        )
        connectionManager.emitState(
            ConnectionState.WifiProvisioned(nextSession, ProvisioningStatus("WiFi", "h2", "hash2"))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("http://custom.local", state.baseUrl)
        assertTrue(state.isBaseUrlManual)
        assertEquals("http://192.168.4.7:8000", state.autoDetectedBaseUrl)
    }

    @Test
    fun `manual override can revert to auto detected base`() = runTest(dispatcher) {
        val session = createSession("session-auto-3")
        connectionManager.networkResult = Result.Success(
            DeviceNetworkStatus("192.168.8.8", "Device", "Phone", "raw")
        )
        connectionManager.emitState(
            ConnectionState.WifiProvisioned(session, ProvisioningStatus("WiFi", "h", "hash"))
        )
        advanceUntilIdle()
        viewModel.onBaseUrlChanged("http://custom.local")

        viewModel.onUseAutoBaseUrl()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("http://192.168.8.8:8000", state.baseUrl)
        assertEquals(false, state.isBaseUrlManual)
    }

    private fun createSession(id: String) = BleSession(
        peripheralId = id,
        peripheralName = "BT311-$id",
        signalStrengthDbm = -50,
        profileId = "bt311",
        secureToken = "token-$id",
        establishedAtMillis = System.currentTimeMillis()
    )

    private class FakeDeviceMediaGateway : DeviceMediaGateway {
        var files: List<DeviceMediaFile> = emptyList()
        var fetchResult: Result<List<DeviceMediaFile>>? = null
        var uploadCalls = 0
        var fetchCalls = 0
        val appliedNames = mutableSetOf<String>()

        override suspend fun fetchFiles(baseUrl: String): Result<List<DeviceMediaFile>> {
            fetchCalls += 1
            return fetchResult ?: Result.Success(files)
        }

        override suspend fun uploadFile(baseUrl: String, source: DeviceUploadSource): Result<Unit> {
            uploadCalls += 1
            return Result.Success(Unit)
        }

        override suspend fun applyFile(baseUrl: String, fileName: String): Result<Unit> {
            appliedNames.add(fileName)
            return Result.Success(Unit)
        }

        override suspend fun deleteFile(baseUrl: String, fileName: String): Result<Unit> {
            files = files.filterNot { it.name == fileName }
            return Result.Success(Unit)
        }
    }

    private class FakeDeviceConnectionManager : DeviceConnectionManager {
        private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val state: StateFlow<ConnectionState> = _state
        var networkResult: Result<DeviceNetworkStatus> =
            Result.Error(IllegalStateException("no network"))

        fun emitReady() {
            emitState(
                ConnectionState.Syncing(
                    session = BleSession(
                        peripheralId = "p1",
                        peripheralName = "BT311",
                        signalStrengthDbm = -50,
                        profileId = "bt311",
                        secureToken = "token",
                        establishedAtMillis = System.currentTimeMillis()
                    ),
                    status = ProvisioningStatus("WiFi", "handshake", "hash"),
                    lastHeartbeatAtMillis = System.currentTimeMillis()
                )
            )
        }

        fun emitState(state: ConnectionState) {
            _state.value = state
        }

        override fun selectPeripheral(peripheral: BlePeripheral) = Unit
        override suspend fun startPairing(
            peripheral: BlePeripheral,
            credentials: WifiCredentials
        ): Result<Unit> = Result.Success(Unit)

        override suspend fun retry(): Result<Unit> = Result.Success(Unit)
        override fun forgetDevice() = Unit
        override suspend fun requestHotspotCredentials(): Result<WifiCredentials> =
            Result.Error(IllegalStateException())

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = networkResult
    }
}
