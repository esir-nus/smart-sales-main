package com.smartsales.feature.media

import android.net.Uri
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
import com.smartsales.core.util.Result as CoreResult

// 文件：feature/media/src/test/java/com/smartsales/feature/media/GifUploadViewModelTest.kt
// 模块：:feature:media
// 说明：GifUploadViewModel单元测试
// 作者：创建于 2026-01-10

@RunWith(RobolectricTestRunner::class)
class GifUploadViewModelTest {

    @Test
    fun `initial state is idle`() {
        val viewModel = createViewModel(connected = false)
        
        assertEquals(GifUploadStep.Idle, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.selectedFileName)
        assertFalse(viewModel.uiState.value.isActionEnabled)
    }

    @Test
    fun `selectImage updates fileName`() {
        val viewModel = createViewModel(connected = false)
        
        viewModel.selectImage(Uri.parse("content://test"), "test.gif")
        
        assertEquals("test.gif", viewModel.uiState.value.selectedFileName)
        assertEquals(GifUploadStep.Idle, viewModel.uiState.value.step)
    }

    @Test
    fun `action enabled when image selected and connected`() {
        val viewModel = createViewModel(connected = true)
        
        viewModel.selectImage(Uri.parse("content://test"), "test.gif")
        
        assertTrue("Action should be enabled", viewModel.uiState.value.isActionEnabled)
    }

    @Test
    fun `action disabled when no session`() {
        val viewModel = createViewModel(connected = false)
        
        viewModel.selectImage(Uri.parse("content://test"), "test.gif")
        
        assertFalse("Action should be disabled without session", viewModel.uiState.value.isActionEnabled)
    }

    @Test
    fun `startUpload without session shows error`() {
        val viewModel = createViewModel(connected = false)
        viewModel.selectImage(Uri.parse("content://test"), "test.gif")
        
        viewModel.startUpload()
        
        assertEquals(GifUploadStep.Error, viewModel.uiState.value.step)
        assertEquals("徽章未连接", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `dismissError returns to idle`() {
        val viewModel = createViewModel(connected = false)
        viewModel.selectImage(Uri.parse("content://test"), "test.gif")
        viewModel.startUpload()
        
        viewModel.dismissError()
        
        assertEquals(GifUploadStep.Idle, viewModel.uiState.value.step)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearSelection resets state`() {
        val viewModel = createViewModel(connected = false)
        viewModel.selectImage(Uri.parse("content://test"), "test.gif")
        
        viewModel.clearSelection()
        
        assertNull(viewModel.uiState.value.selectedFileName)
        assertEquals(GifUploadStep.Idle, viewModel.uiState.value.step)
    }

    // === Helper ===
    
    private fun createViewModel(connected: Boolean): GifUploadViewModel {
        val connectionManager = FakeConnectionManager(connected)
        return GifUploadViewModel(FakeGifCoordinator(), connectionManager)
    }

    // === Fakes ===

    private class FakeGifCoordinator : GifTransferCoordinator {
        override fun transfer(session: BleSession, gifUri: Uri): Flow<GifTransferState> {
            return flowOf(GifTransferState.Preparing, GifTransferState.Complete)
        }
    }

    private class FakeConnectionManager(connected: Boolean) : DeviceConnectionManager {
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
