package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.legacy.FakePhoneWifiProvider
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.service.DownloadServiceOrchestrator
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SimAudioRepositoryNamespaceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var connectivityBridge: ConnectivityBridge
    private lateinit var ossUploader: OssUploader
    private lateinit var tingwuPipeline: TingwuPipeline

    @Before
    fun setUp() {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)
        connectivityBridge = mock()
        ossUploader = mock()
        tingwuPipeline = mock()
    }

    @Test
    fun `sim namespace helpers return expected filenames`() {
        assertEquals("sim_audio_metadata.json", SIM_AUDIO_METADATA_FILENAME)
        assertEquals("sim_audio_pending_badge_deletes.json", SIM_AUDIO_PENDING_BADGE_DELETE_FILENAME)
        assertEquals("sim_audio-1.wav", simStoredAudioFilename("audio-1", "wav"))
        assertEquals("sim_audio-1_artifacts.json", simArtifactFilename("audio-1"))
    }

    @Test
    fun `sim repository reload preserves bound session id from sim metadata file`() {
        val metadataFile = File(tempFolder.root, SIM_AUDIO_METADATA_FILENAME)
        val originalEntry = AudioFile(
            id = "audio-1",
            filename = "SIM_Wave2_Seed.mp3",
            timeDisplay = "Now",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.TRANSCRIBED,
            boundSessionId = "session-123"
        )
        metadataFile.writeText(Json.encodeToString(listOf(originalEntry)))

        val repository = SimAudioRepository(
            runtime = SimAudioRepositoryRuntime(
                context = context,
                connectivityBridge = connectivityBridge,
                endpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator(),
                ossUploader = ossUploader,
                tingwuPipeline = tingwuPipeline,
                connectivityPrompt = mock<ConnectivityPrompt>(),
                phoneWifiProvider = FakePhoneWifiProvider("OfficeGuest"),
                deviceRegistryManager = mock<DeviceRegistryManager>()
            ),
            orchestrator = mock<DownloadServiceOrchestrator>(),
            autoDownloader = mock<SimBadgeAudioAutoDownloader>()
        )

        assertEquals("session-123", repository.getBoundSessionId("audio-1"))
    }

    @Test
    fun `sim repository bindSession persists to sim metadata file and survives reload`() {
        val metadataFile = File(tempFolder.root, SIM_AUDIO_METADATA_FILENAME)
        metadataFile.writeText(
            Json.encodeToString(
                listOf(
                    AudioFile(
                        id = "audio-2",
                        filename = "SIM_Wave2_Seed.mp3",
                        timeDisplay = "Now",
                        source = AudioSource.PHONE,
                        status = TranscriptionStatus.PENDING
                    )
                )
            )
        )

        val repository = SimAudioRepository(
            runtime = SimAudioRepositoryRuntime(
                context = context,
                connectivityBridge = connectivityBridge,
                endpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator(),
                ossUploader = ossUploader,
                tingwuPipeline = tingwuPipeline,
                connectivityPrompt = mock<ConnectivityPrompt>(),
                phoneWifiProvider = FakePhoneWifiProvider("OfficeGuest"),
                deviceRegistryManager = mock<DeviceRegistryManager>()
            ),
            orchestrator = mock<DownloadServiceOrchestrator>(),
            autoDownloader = mock<SimBadgeAudioAutoDownloader>()
        )

        repository.bindSession("audio-2", "session-456")

        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline) {
            if (metadataFile.exists() && metadataFile.readText().contains("session-456")) {
                break
            }
            Thread.sleep(20)
        }

        assertTrue(metadataFile.readText().contains("session-456"))
        assertTrue(!File(tempFolder.root, "audio_metadata.json").exists())

        val reloadedRepository = SimAudioRepository(
            runtime = SimAudioRepositoryRuntime(
                context = context,
                connectivityBridge = connectivityBridge,
                endpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator(),
                ossUploader = ossUploader,
                tingwuPipeline = tingwuPipeline,
                connectivityPrompt = mock<ConnectivityPrompt>(),
                phoneWifiProvider = FakePhoneWifiProvider("OfficeGuest"),
                deviceRegistryManager = mock<DeviceRegistryManager>()
            ),
            orchestrator = mock<DownloadServiceOrchestrator>(),
            autoDownloader = mock<SimBadgeAudioAutoDownloader>()
        )

        assertEquals("session-456", reloadedRepository.getBoundSessionId("audio-2"))
    }
}
