package com.smartsales.feature.media.devicemanager

// 文件：feature/media/src/test/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModelTest.kt
// 模块：:feature:media
// 说明：验证 DeviceManagerViewModel 的列表加载、筛选与操作能力
// 作者：创建于 2025-11-20

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.ProvisioningStatus
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.media.audiofiles.DeviceHttpEndpointProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DeviceManagerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DeviceManagerViewModel
    private lateinit var gateway: FakeDeviceMediaGateway
    private lateinit var connectionManager: FakeDeviceConnectionManager
    private lateinit var endpointProvider: FakeDeviceHttpEndpointProvider

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        gateway = FakeDeviceMediaGateway()
        connectionManager = FakeDeviceConnectionManager()
        endpointProvider = FakeDeviceHttpEndpointProvider()
        viewModel = DeviceManagerViewModel(
            gateway,
            connectionManager,
            FakeDispatcherProvider(dispatcher),
            endpointProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refresh success populates files`() = runTest(dispatcher) {
        connectionManager.emitReady()
        advanceUntilIdle()
        gateway.files = listOf(
            DeviceMediaFile("image-1.jpg", 1024, "image/jpeg", 1_000L, "media/1", "dl/1"),
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2", durationMillis = 90_000),
            DeviceMediaFile("anim.gif", 512, "image/gif", 3_000L, "media/3", "dl/3", durationMillis = 5_000)
        )

        viewModel.onRefreshFiles()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.files.size) // 仅视频+GIF
        assertEquals(2, state.visibleFiles.size)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.loadErrorMessage)
        assertTrue(gateway.fetchCalls >= 1)
    }

    @Test
    fun `mapping sets type label and duration`() = runTest(dispatcher) {
        connectionManager.emitReady()
        advanceUntilIdle()
        gateway.files = listOf(
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2", durationMillis = 75_000),
            DeviceMediaFile("anim.gif", 512, "image/gif", 3_000L, "media/3", "dl/3", durationMillis = 4_000)
        )

        viewModel.onRefreshFiles()
        advanceUntilIdle()

        val files = viewModel.uiState.value.files
        val video = files.first { it.id == "clip.mp4" }
        val gif = files.first { it.id == "anim.gif" }
        assertEquals("视频", video.mediaLabel)
        assertEquals("01:15", video.durationText)
        assertEquals("GIF", gif.mediaLabel)
        assertEquals("00:04", gif.durationText)
    }

    @Test
    fun `refresh failure surfaces error`() = runTest(dispatcher) {
        connectionManager.emitReady()
        advanceUntilIdle()
        gateway.fetchResult = Result.Error(IllegalStateException("offline"))

        viewModel.onRefreshFiles()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("加载设备文件失败，请稍后重试。", state.loadErrorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `connected state triggers initial load`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2")
        )

        connectionManager.emitReady()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.files.size)
        assertEquals(1, gateway.fetchCalls)
    }

    @Test
    fun `refresh when disconnected reports friendly message`() = runTest(dispatcher) {
        advanceUntilIdle() // 等待初始连接状态落地，避免覆盖提示文案
        viewModel.onRefreshFiles()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("设备未连接，无法刷新文件。", state.loadErrorMessage)
        assertEquals(0, gateway.fetchCalls)
    }

    @Test
    fun `retry after failure re-fetches files`() = runTest(dispatcher) {
        connectionManager.emitReady()
        gateway.fetchResult = Result.Error(IllegalStateException("offline"))
        advanceUntilIdle()

        viewModel.onRetryLoad()
        advanceUntilIdle()

        gateway.fetchResult = null
        gateway.files = listOf(DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2"))
        viewModel.onRetryLoad()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.files.size)
        assertEquals(null, state.loadErrorMessage)
        assertTrue(gateway.fetchCalls >= 2)
    }

    @Test
    fun `disconnected clears files and stops loading`() = runTest(dispatcher) {
        connectionManager.emitReady()
        gateway.files = listOf(DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2"))
        advanceUntilIdle()

        connectionManager.emitDisconnected()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertTrue(state.files.isEmpty())
        assertEquals(null, state.loadErrorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `apply file marks ui`() = runTest(dispatcher) {
        connectionManager.emitReady()
        advanceUntilIdle()
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
        assertEquals(null, state.applyInProgressId)
    }

    @Test
    fun `select file updates ui state`() = runTest(dispatcher) {
        gateway.files = listOf(
            DeviceMediaFile("clip.mp4", 2048, "video/mp4", 2_000L, "media/2", "dl/2")
        )
        connectionManager.emitReady()
        advanceUntilIdle()

        viewModel.onSelectFile("clip.mp4")
        val state = viewModel.uiState.value
        assertEquals("clip.mp4", state.selectedFile?.id)
    }

    @Test
    fun `delete file removes entry`() = runTest(dispatcher) {
        connectionManager.emitReady()
        advanceUntilIdle()
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
        connectionManager.emitReady()
        advanceUntilIdle()
        gateway.files = emptyList()
        viewModel.onRefreshFiles()
        advanceUntilIdle()

        gateway.files = listOf(
            DeviceMediaFile("new.mp4", 1024, "video/mp4", 3_000L, "media/3", "dl/3")
        )

        viewModel.onUploadFile(createFakeUploadSource())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, gateway.uploadCalls)
        assertEquals(1, state.files.size)
        assertEquals(false, state.isUploading)
    }

    @Test
    fun `auto detection updates base url when ready`() = runTest(dispatcher) {
        connectionManager.networkStatusResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.50.10",
                deviceWifiName = "RetailWiFi",
                phoneWifiName = "Demo",
                rawResponse = "wifi#address#192.168.50.10#RetailWiFi#Demo"
            )
        )

        connectionManager.emitReady()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("http://192.168.50.10:8000", state.baseUrl)
        assertEquals("http://192.168.50.10:8000", state.autoDetectedBaseUrl)
        assertEquals(false, state.baseUrlWasManual)
        assertEquals(false, state.isAutoDetectingBaseUrl)
    }

    @Test
    fun `manual base url wins over auto detection`() = runTest(dispatcher) {
        connectionManager.networkStatusResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.60.6",
                deviceWifiName = "Office",
                phoneWifiName = "Office",
                rawResponse = "wifi#address#192.168.60.6#Office#Office"
            )
        )

        viewModel.onBaseUrlChanged("http://manual:9000")
        connectionManager.emitReady()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("http://manual:9000", state.baseUrl)
        assertEquals("http://192.168.60.6:9000", state.autoDetectedBaseUrl)
        assertEquals(true, state.baseUrlWasManual)

        viewModel.onUseAutoBaseUrl()
        val updated = viewModel.uiState.value
        assertEquals("http://192.168.60.6:9000", updated.baseUrl)
        assertEquals(false, updated.baseUrlWasManual)
    }

    @Test
    fun `ble connected state still triggers auto detection`() = runTest(dispatcher) {
        connectionManager.networkStatusResult = Result.Success(
            DeviceNetworkStatus(
                ipAddress = "192.168.70.7",
                deviceWifiName = "Store",
                phoneWifiName = "Store",
                rawResponse = "wifi#address#192.168.70.7#Store#Store"
            )
        )

        connectionManager.emitBleConnected()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("http://192.168.70.7:8000", state.autoDetectedBaseUrl)
    }

    private class FakeDeviceHttpEndpointProvider(
        initial: String? = null
    ) : DeviceHttpEndpointProvider {
        private val flow = MutableStateFlow(initial)
        override val deviceBaseUrl: StateFlow<String?> = flow
        fun emit(value: String?) {
            flow.value = value
        }
    }

    private class FakeDeviceMediaGateway : DeviceMediaGateway {
        var files: List<DeviceMediaFile> = emptyList()
        var fetchResult: Result<List<DeviceMediaFile>>? = null
        var uploadCalls = 0
        val appliedNames = mutableSetOf<String>()
        var fetchCalls = 0

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

        override suspend fun downloadFile(baseUrl: String, file: DeviceMediaFile): Result<File> {
            return Result.Success(File.createTempFile("fake", ".tmp"))
        }
    }

    private fun createFakeUploadSource(): DeviceUploadSource {
        val uri = android.net.Uri.parse("file:///tmp/new.png")
        return DeviceUploadSource.AndroidUri(uri)
    }

    private class FakeDeviceConnectionManager : DeviceConnectionManager {
        private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val state: StateFlow<ConnectionState> = _state
        var networkStatusResult: Result<DeviceNetworkStatus> = Result.Error(IllegalStateException("未配置网络"))

        fun emitReady() {
            _state.value = ConnectionState.Syncing(
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
        }

        fun emitBleConnected() {
            _state.value = ConnectionState.Connected(
                session = BleSession(
                    peripheralId = "p1",
                    peripheralName = "BT311",
                    signalStrengthDbm = -50,
                    profileId = "bt311",
                    secureToken = "token",
                    establishedAtMillis = System.currentTimeMillis()
                )
            )
        }

        fun emitDisconnected() {
            _state.value = ConnectionState.Disconnected
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

        override suspend fun queryNetworkStatus(): Result<DeviceNetworkStatus> = networkStatusResult
    }
}
