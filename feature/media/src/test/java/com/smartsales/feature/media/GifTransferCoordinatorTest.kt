package com.smartsales.feature.media

import android.net.Uri
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.DeviceNetworkStatus
import com.smartsales.feature.connectivity.WifiProvisioner
import com.smartsales.feature.connectivity.BadgeHttpClient
import com.smartsales.feature.connectivity.gateway.BleGateway
import com.smartsales.feature.connectivity.gateway.BleGatewayResult
import com.smartsales.feature.connectivity.gateway.GifCommand
import com.smartsales.feature.connectivity.gateway.GifCommandResult
import com.smartsales.feature.connectivity.gateway.HotspotResult
import com.smartsales.feature.connectivity.gateway.NetworkQueryResult
import com.smartsales.feature.connectivity.gateway.TimeSyncEvent
import com.smartsales.feature.connectivity.gateway.WavCommand
import com.smartsales.feature.connectivity.gateway.WavCommandResult
import com.smartsales.feature.connectivity.WifiCredentials
import com.smartsales.feature.connectivity.ProvisioningStatus
import com.smartsales.feature.media.processing.GifFrameExtractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GifTransferCoordinatorTest {

    private lateinit var coordinator: DefaultGifTransferCoordinator
    
    // Fakes
    private val fakeBleGateway = FakeBleGateway()
    private val fakeWifiProvisioner = FakeWifiProvisioner()
    private val fakeHttpClient = FakeBadgeHttpClient()
    private val fakeFrameExtractor = FakeGifFrameExtractor()
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
    }

    private val mockSession = BleSession(
        peripheralId = "test_mac",
        peripheralName = "test_device",
        signalStrengthDbm = -50,
        profileId = "test_profile",
        secureToken = "token",
        establishedAtMillis = 0L
    )

    @Before
    fun setup() {
        coordinator = DefaultGifTransferCoordinator(
            context = RuntimeEnvironment.getApplication(),
            bleGateway = fakeBleGateway,
            wifiProvisioner = fakeWifiProvisioner,
            httpClient = fakeHttpClient,
            frameExtractor = fakeFrameExtractor,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `transfer success flow`() = runTest(testDispatcher) {
        // Given
        val states = mutableListOf<GifTransferState>()
        
        // When
        coordinator.transfer(mockSession, Uri.parse("content://test.gif"))
            .collect { states.add(it) }

        // Then
        // Expected sequence: Preparing -> Extracting -> Connecting -> Uploading(1/2) -> Uploading(2/2) -> Finalizing -> Complete
        assertTrue(states.contains(GifTransferState.Preparing))
        assertTrue(states.any { it is GifTransferState.Extracting })
        assertTrue(states.contains(GifTransferState.Connecting))
        assertTrue(states.any { it is GifTransferState.Uploading })
        assertTrue(states.contains(GifTransferState.Finalizing))
        assertEquals(GifTransferState.Complete, states.last())
        
        // Verify fake interactions
        // Verify fake interactions
        assertEquals(listOf(GifCommand.START, GifCommand.END), fakeBleGateway.commands)
        assertEquals(2, fakeHttpClient.uploadedFiles.size) // Fake extractor returns 2 files
    }

    @Test
    fun `transfer fails on network query error`() = runTest(testDispatcher) {
        // Given
        fakeWifiProvisioner.shouldFail = true
        val states = mutableListOf<GifTransferState>()

        // When
        coordinator.transfer(mockSession, Uri.parse("content://test.gif"))
            .collect { states.add(it) }

        // Then
        assertTrue(states.last() is GifTransferState.Error)
        assertTrue((states.last() as GifTransferState.Error).message.contains("查询设备网络状态"))
    }

    @Test
    fun `transfer fails if badge unreachable`() = runTest(testDispatcher) {
        // Given
        fakeHttpClient.isReachable = false
        val states = mutableListOf<GifTransferState>()

        // When
        coordinator.transfer(mockSession, Uri.parse("content://test.gif"))
            .collect { states.add(it) }

        // Then
        assertTrue(states.last() is GifTransferState.Error)
        assertTrue((states.last() as GifTransferState.Error).message.contains("不可达"))
    }
    
    @Test
    fun `transfer fails on extraction error`() = runTest(testDispatcher) {
        // Given
        fakeFrameExtractor.shouldFail = true
        val states = mutableListOf<GifTransferState>()

        // When
        coordinator.transfer(mockSession, Uri.parse("content://test.gif"))
            .collect { states.add(it) }

        // Then
        assertTrue(states.last() is GifTransferState.Error)
        assertTrue((states.last() as GifTransferState.Error).message.contains("帧提取失败"))
    }
}

// === Fakes ===

class FakeBleGateway : BleGateway {
    val commands = mutableListOf<GifCommand>()
    var shouldFailGifCommand = false

    override suspend fun sendGifCommand(session: BleSession, command: GifCommand): GifCommandResult {
        commands.add(command)
        if (shouldFailGifCommand) return GifCommandResult.TransportError("Fake error")
        return when (command) {
            GifCommand.START -> GifCommandResult.Ready
            GifCommand.END -> GifCommandResult.DisplayOk
        }
    }

    // Unused methods stubbed
    override suspend fun provision(session: BleSession, credentials: WifiCredentials) = BleGatewayResult.Timeout
    override suspend fun requestHotspot(session: BleSession) = HotspotResult.Timeout(0)
    override suspend fun queryNetwork(session: BleSession) = NetworkQueryResult.Timeout(0)
    override suspend fun sendWavCommand(session: BleSession, command: WavCommand) = WavCommandResult.Ready
    override fun listenForTimeSync(session: BleSession): Flow<TimeSyncEvent> = flowOf()
    override fun forget(peripheral: BlePeripheral) {}
}

class FakeWifiProvisioner : WifiProvisioner {
    var shouldFail = false
    override suspend fun provision(session: BleSession, credentials: WifiCredentials) = Result.Success(ProvisioningStatus("ssid", "id", "hash"))
    override suspend fun requestHotspotCredentials(session: BleSession) = Result.Success(WifiCredentials("ssid", "pwd"))
    
    override suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus> {
        if (shouldFail) return Result.Error(Exception("Network error"))
        return Result.Success(DeviceNetworkStatus("192.168.1.100", "dev", "phone", "raw"))
    }
}

class FakeBadgeHttpClient : BadgeHttpClient {
    var isReachable = true
    val uploadedFiles = mutableListOf<File>()
    var shouldFailUpload = false

    override suspend fun isReachable(baseUrl: String): Boolean = isReachable

    override suspend fun uploadJpg(baseUrl: String, file: File): Result<Unit> {
        if (shouldFailUpload) return Result.Error(Exception("Upload failed"))
        uploadedFiles.add(file)
        return Result.Success(Unit)
    }

    // Unused methods stubbed
    override suspend fun listWavFiles(baseUrl: String) = Result.Success(emptyList<String>())
    override suspend fun downloadWav(baseUrl: String, filename: String, dest: File) = Result.Success(Unit)
    override suspend fun deleteWav(baseUrl: String, filename: String) = Result.Success(Unit)
}

class FakeGifFrameExtractor : GifFrameExtractor {
    var shouldFail = false

    override suspend fun extractFrames(
        source: Uri,
        outputDir: File,
        targetWidth: Int,
        targetHeight: Int,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ): Result<List<File>> {
        if (shouldFail) return Result.Error(Exception("Extraction failed"))
        
        if (!outputDir.exists()) outputDir.mkdirs()
        
        // Return 2 fake files
        val f1 = File(outputDir, "1.jpg").apply { createNewFile() }
        val f2 = File(outputDir, "2.jpg").apply { createNewFile() }
        
        onProgress(1, 2)
        onProgress(2, 2)
        
        return Result.Success(listOf(f1, f2))
    }
}
