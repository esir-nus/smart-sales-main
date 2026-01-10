package com.smartsales.feature.media

import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import com.smartsales.core.util.Result as CoreResult

// 文件：feature/media/src/test/java/com/smartsales/feature/media/WavDownloadViewModelTest.kt
// 模块：:feature:media
// 说明：WavDownloadViewModel单元测试
// 作者：创建于 2026-01-10

@RunWith(RobolectricTestRunner::class)
class WavDownloadViewModelTest {

    @Test
    fun `initial state is idle`() {
        val viewModel = createViewModel(connected = false)
        
        assertEquals(WavDownloadStep.Idle, viewModel.uiState.value.step)
        assertTrue(viewModel.uiState.value.availableFiles.isEmpty())
        assertTrue(viewModel.uiState.value.selectedFiles.isEmpty())
    }

    @Test
    fun `startScan without session shows error`() {
        val viewModel = createViewModel(connected = false)
        
        viewModel.startScan()
        
        assertEquals(WavDownloadStep.Error, viewModel.uiState.value.step)
        assertEquals("徽章未连接", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `dismissError returns to idle`() {
        val viewModel = createViewModel(connected = false)
        viewModel.startScan()
        
        viewModel.dismissError()
        
        assertEquals(WavDownloadStep.Idle, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `reset clears all state`() {
        val viewModel = createViewModel(connected = false)
        viewModel.startScan() // Put into error state
        
        viewModel.reset()
        
        assertEquals(WavDownloadStep.Idle, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `startDownload without files does nothing`() {
        val viewModel = createViewModel(connected = true)
        
        viewModel.startDownload()
        
        // With no selected files, startDownload just returns early
        // State remains Idle
        assertEquals(WavDownloadStep.Idle, viewModel.uiState.value.step)
    }

    // === Helper ===

    private fun createViewModel(connected: Boolean): WavDownloadViewModel {
        val connectionManager = FakeConnectionManagerForWav(connected)
        return WavDownloadViewModel(FakeWavCoordinator(), connectionManager)
    }

    // === Fakes ===

    private class FakeWavCoordinator : WavDownloadCoordinator {
        override fun listFiles(session: BleSession): Flow<WavListState> {
            return flowOf(WavListState.Scanning, WavListState.Found(listOf("test.wav")))
        }

        override fun downloadFiles(
            session: BleSession,
            files: List<String>,
            destDir: File
        ): Flow<WavDownloadState> {
            return flowOf(
                WavDownloadState.Downloading("test.wav", 1, 1),
                WavDownloadState.Complete
            )
        }
    }

    private class FakeConnectionManagerForWav(connected: Boolean) : DeviceConnectionManager {
        private val _state = MutableStateFlow<ConnectionState>(
            if (connected) {
                ConnectionState.Connected(
                    BleSession.fromPeripheral(BlePeripheral("test-id", "Test", -50, null))
                )
            } else {
                ConnectionState.Disconnected
            }
        )
        override val state: StateFlow<ConnectionState> = _state

        override fun selectPeripheral(peripheral: BlePeripheral) {}
        override suspend fun startPairing(peripheral: BlePeripheral, credentials: com.smartsales.feature.connectivity.WifiCredentials) = CoreResult.Success(Unit)
        override suspend fun retry() = CoreResult.Success(Unit)
        override fun forgetDevice() {}
        override suspend fun requestHotspotCredentials() = CoreResult.Error(IllegalStateException())
        override suspend fun queryNetworkStatus() = CoreResult.Error(IllegalStateException())
        override fun scheduleAutoReconnectIfNeeded() {}
        override fun forceReconnectNow() {}
    }
}
