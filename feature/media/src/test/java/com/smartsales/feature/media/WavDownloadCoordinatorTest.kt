package com.smartsales.feature.media

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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WavDownloadCoordinatorTest {

    private lateinit var coordinator: DefaultWavDownloadCoordinator
    
    private val fakeBleGateway = FakeBleGatewayForWav()
    private val fakeWifiProvisioner = FakeWifiProvisionerForWav()
    private val fakeHttpClient = FakeBadgeHttpClientForWav()
    
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

    private val tempDir = File(System.getProperty("java.io.tmpdir"), "wav_test").apply { mkdirs() }

    @Before
    fun setup() {
        coordinator = DefaultWavDownloadCoordinator(
            context = RuntimeEnvironment.getApplication(),
            bleGateway = fakeBleGateway,
            wifiProvisioner = fakeWifiProvisioner,
            httpClient = fakeHttpClient,
            dispatchers = dispatchers
        )
    }

    // === listFiles Tests ===

    @Test
    fun `listFiles success flow`() = runTest(testDispatcher) {
        // Given
        val states = mutableListOf<WavListState>()

        // When
        coordinator.listFiles(mockSession).collect { states.add(it) }

        // Then
        // Expected: Scanning -> Found
        assertTrue(states.contains(WavListState.Scanning))
        assertTrue(states.last() is WavListState.Found)
        val found = states.last() as WavListState.Found
        assertEquals(2, found.files.size)
        assertEquals("rec1.wav", found.files[0])
        
        assertEquals(WavCommand.GET, fakeBleGateway.lastWavCommand)
    }
    
    @Test
    fun `listFiles fails on BLE not ready`() = runTest(testDispatcher) {
        // Given
        fakeBleGateway.shouldFailWavCommand = true
        val states = mutableListOf<WavListState>()

        // When
        coordinator.listFiles(mockSession).collect { states.add(it) }

        // Then
        assertTrue(states.last() is WavListState.Error)
    }

    // === downloadFiles Tests ===

    @Test
    fun `downloadFiles success flow`() = runTest(testDispatcher) {
        // Given
        val states = mutableListOf<WavDownloadState>()
        val filesToDownload = listOf("rec1.wav", "rec2.wav")

        // When
        coordinator.downloadFiles(mockSession, filesToDownload, tempDir)
            .collect { states.add(it) }

        // Then
        // Expected: Downloading(1) -> Downloading(2) -> Complete
        assertTrue(states.any { it is WavDownloadState.Downloading && it.filename == "rec1.wav" })
        assertTrue(states.any { it is WavDownloadState.Downloading && it.filename == "rec2.wav" })
        assertEquals(WavDownloadState.Complete, states.last())
        
        assertEquals(WavCommand.END, fakeBleGateway.lastWavCommand)
    }
    
    @Test
    fun `downloadFiles fails on http error`() = runTest(testDispatcher) {
        // Given
        fakeHttpClient.shouldFailDownload = true
        val states = mutableListOf<WavDownloadState>()
        val filesToDownload = listOf("rec1.wav")

        // When
        coordinator.downloadFiles(mockSession, filesToDownload, tempDir)
            .collect { states.add(it) }

        // Then
        assertTrue(states.last() is WavDownloadState.Error)
    }
}

// === Fakes ===

class FakeBleGatewayForWav : BleGateway {
    var lastWavCommand: WavCommand? = null
    var shouldFailWavCommand = false

    override suspend fun sendWavCommand(session: BleSession, command: WavCommand): WavCommandResult {
        lastWavCommand = command
        if (shouldFailWavCommand) return WavCommandResult.TransportError("Fake error")
        return when (command) {
            WavCommand.GET -> WavCommandResult.Ready
            WavCommand.END -> WavCommandResult.Done
        }
    }

    // Unused
    override suspend fun provision(session: BleSession, credentials: WifiCredentials) = BleGatewayResult.Timeout
    override suspend fun requestHotspot(session: BleSession) = HotspotResult.Timeout(0)
    override suspend fun queryNetwork(session: BleSession) = NetworkQueryResult.Timeout(0)
    override suspend fun sendGifCommand(session: BleSession, command: GifCommand) = GifCommandResult.Ready
    override fun listenForTimeSync(session: BleSession): Flow<TimeSyncEvent> = flowOf()
    override fun forget(peripheral: BlePeripheral) {}
}

class FakeWifiProvisionerForWav : WifiProvisioner {
    override suspend fun provision(session: BleSession, credentials: WifiCredentials) = Result.Success(ProvisioningStatus("ssid", "", ""))
    override suspend fun requestHotspotCredentials(session: BleSession) = Result.Success(WifiCredentials("", ""))
    
    override suspend fun queryNetworkStatus(session: BleSession): Result<DeviceNetworkStatus> {
        return Result.Success(DeviceNetworkStatus("192.168.1.100", "dev", "phone", "raw"))
    }
}

class FakeBadgeHttpClientForWav : BadgeHttpClient {
    var shouldFailDownload = false

    override suspend fun listWavFiles(baseUrl: String) = Result.Success(listOf("rec1.wav", "rec2.wav"))
    
    override suspend fun downloadWav(baseUrl: String, filename: String, dest: File): Result<Unit> {
        if (shouldFailDownload) return Result.Error(Exception("Download failed"))
        dest.createNewFile()
        return Result.Success(Unit)
    }

    // Unused
    override suspend fun uploadJpg(baseUrl: String, file: File) = Result.Success(Unit)
    override suspend fun deleteWav(baseUrl: String, filename: String) = Result.Success(Unit)
    override suspend fun isReachable(baseUrl: String) = true
}
