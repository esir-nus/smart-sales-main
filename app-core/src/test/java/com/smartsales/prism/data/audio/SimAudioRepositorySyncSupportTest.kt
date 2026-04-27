package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.core.util.Result
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.data.connectivity.BadgeEndpointRecoveryCoordinator
import com.smartsales.prism.data.connectivity.BadgeEndpointSnapshot
import com.smartsales.prism.data.connectivity.BadgeRuntimeKey
import com.smartsales.prism.data.connectivity.legacy.FakePhoneWifiProvider
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.data.connectivity.registry.DeviceRegistryManager
import com.smartsales.prism.data.connectivity.registry.RegisteredDevice
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.service.DownloadServiceOrchestrator
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SimAudioRepositorySyncSupportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var connectivityBridge: FakeConnectivityBridge
    private lateinit var endpointRecoveryCoordinator: BadgeEndpointRecoveryCoordinator
    private lateinit var phoneWifiProvider: FakePhoneWifiProvider
    private lateinit var runtime: SimAudioRepositoryRuntime
    private lateinit var storeSupport: SimAudioRepositoryStoreSupport
    private lateinit var orchestrator: DownloadServiceOrchestrator
    private lateinit var syncSupport: SimAudioRepositorySyncSupport
    private lateinit var connectivityPrompt: FakeConnectivityPrompt
    private lateinit var deviceRegistryManager: DeviceRegistryManager
    private lateinit var activeDeviceFlow: MutableStateFlow<RegisteredDevice?>

    @Before
    fun setup() {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)

        connectivityBridge = FakeConnectivityBridge()
        endpointRecoveryCoordinator = BadgeEndpointRecoveryCoordinator()
        connectivityPrompt = FakeConnectivityPrompt()
        phoneWifiProvider = FakePhoneWifiProvider("OfficeGuest")
        deviceRegistryManager = mock<DeviceRegistryManager>()
        activeDeviceFlow = MutableStateFlow(null)
        whenever(deviceRegistryManager.activeDevice).thenReturn(activeDeviceFlow)
        runtime = SimAudioRepositoryRuntime(
            context = context,
            connectivityBridge = connectivityBridge,
            endpointRecoveryCoordinator = endpointRecoveryCoordinator,
            ossUploader = mock<OssUploader>(),
            tingwuPipeline = mock<TingwuPipeline>(),
            connectivityPrompt = connectivityPrompt,
            phoneWifiProvider = phoneWifiProvider,
            deviceRegistryManager = deviceRegistryManager
        )
        storeSupport = SimAudioRepositoryStoreSupport(runtime)
        orchestrator = mock<DownloadServiceOrchestrator>()
        syncSupport = SimAudioRepositorySyncSupport(
            runtime = runtime,
            storeSupport = storeSupport,
            orchestrator = orchestrator
        )
    }

    @Test
    fun `manual sync fails at strict preflight before list work begins`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = false

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
        assertEquals(listOf("OfficeGuest"), connectivityPrompt.suggestedSsids)
    }

    @Test
    fun `auto sync skips quietly when strict preflight is not ready`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = false

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.AUTO)

        assertEquals(SimBadgeSyncSkippedReason.NOT_READY, outcome.skippedReason)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
        assertTrue(connectivityPrompt.suggestedSsids.isEmpty())
    }

    @Test
    fun `manual sync does not probe readiness while badge download is active`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        runtime.activeBadgeDownloadFilename = "active.wav"
        connectivityBridge.isReadyResult = false

        assertTrue(syncSupport.canSyncFromBadge())
        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncSkippedReason.ALREADY_RUNNING, outcome.skippedReason)
        assertTrue(connectivityBridge.calls.isEmpty())
    }

    @Test
    fun `auto sync prompts ON_CONNECT isolation and arms suppression when validated wifi and badge ip are known`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        noteActiveEndpoint("badge-1", "token-1", "192.168.0.9")
        phoneWifiProvider.snapshot = FakePhoneWifiProvider("OfficeGuest", isValidated = true).snapshot
        connectivityBridge.isReadyResult = false

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.AUTO)

        assertEquals(SimBadgeSyncSkippedReason.NOT_READY, outcome.skippedReason)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
        assertEquals(
            listOf("192.168.0.9" to IsolationTriggerContext.ON_CONNECT),
            connectivityPrompt.isolationPrompts
        )
        assertTrue(syncSupport.shouldSuppressAutoSync())
    }

    @Test
    fun `auto sync is suppressed when known http unreachable latch matches current runtime and endpoint`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        val runtimeKey = noteActiveEndpoint("badge-1", "token-1", "192.168.0.9")
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }

        connectivityBridge.calls.clear()
        endpointRecoveryCoordinator.noteCurrentRuntimeKey(runtimeKey)

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.AUTO)

        assertEquals(SimBadgeSyncSkippedReason.KNOWN_HTTP_UNREACHABLE, outcome.skippedReason)
        assertTrue(connectivityBridge.calls.isEmpty())
    }

    @Test
    fun `auto sync latch clears when runtime key changes`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        noteActiveEndpoint("badge-1", "token-1", "192.168.0.9")
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }

        endpointRecoveryCoordinator.noteCurrentRuntimeKey(
            BadgeRuntimeKey(peripheralId = "badge-2", secureToken = "token-2")
        )

        assertFalse(syncSupport.shouldSuppressAutoSync())
    }

    @Test
    fun `auto sync latch clears when endpoint changes`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        val runtimeKey = noteActiveEndpoint("badge-1", "token-1", "192.168.0.9")
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }

        endpointRecoveryCoordinator.noteResolvedEndpoint(
            BadgeEndpointSnapshot(
                runtimeKey = runtimeKey,
                badgeIp = "192.168.0.12",
                baseUrl = "http://192.168.0.12:8088"
            )
        )

        assertFalse(syncSupport.shouldSuppressAutoSync())
    }

    @Test
    fun `manual sync remains allowed and clears latch after success`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        noteActiveEndpoint("badge-1", "token-1", "192.168.0.9")
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }

        connectivityBridge.calls.clear()
        connectivityBridge.listResult = Result.Success(emptyList())

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.DEVICE_EMPTY, outcome.resultBranch)
        assertTrue(connectivityBridge.calls.containsAll(listOf("isReady", "listRecordings")))
        assertFalse(syncSupport.shouldSuppressAutoSync())
    }

    @Test
    fun `manual sync reaches list stage after ready preflight and surfaces list failure`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
        assertEquals(listOf("isReady", "listRecordings"), connectivityBridge.calls)
        assertEquals(listOf("OfficeGuest"), connectivityPrompt.suggestedSsids)
    }

    @Test
    fun `manual sync reaches download stage after ready preflight and surfaces download failure`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.downloadResults["log_20260327_135948.wav"] = WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
            message = "timeout"
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        assertTrue(
            connectivityBridge.calls.containsAll(
                listOf(
                    "isReady",
                    "listRecordings",
                    "downloadRecording:log_20260327_135948.wav"
                )
            )
        )
    }

    @Test
    fun `manual sync returns device empty when badge list is empty`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(emptyList())

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.DEVICE_EMPTY, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        verify(orchestrator, never()).notifyDownloadStarting()
    }

    @Test
    fun `manual sync returns already present when listed files already exist locally`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260327_135948"))
        runtime.audioFiles.value = listOf(
            AudioFile(
                id = "badge-1",
                filename = "log_20260327_135948.wav",
                timeDisplay = "Now",
                source = AudioSource.SMARTBADGE,
                status = TranscriptionStatus.TRANSCRIBED
            )
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync creates placeholder immediately then upgrades it after background download`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260327_135948"))
        connectivityBridge.downloadResults["log_20260327_135948.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("downloaded.wav").apply { writeText("audio") },
            originalFilename = "log_20260327_135948.wav",
            sizeBytes = 2048L
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        assertEquals(1, outcome.queuedCount)
        verify(orchestrator).notifyDownloadStarting()
        assertEquals(
            AudioLocalAvailability.QUEUED,
            runtime.audioFiles.value.single { it.filename == "log_20260327_135948.wav" }.localAvailability
        )

        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.READY,
            runtime.audioFiles.value.single { it.filename == "log_20260327_135948.wav" }.localAvailability
        )
    }

    @Test
    fun `manual sync removes placeholder when background download is below 1KB threshold`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260327_140000", "log#20260327_140100"))
        val emptyFile = tempFolder.newFile("empty.wav")
        val normalFile = tempFolder.newFile("normal.wav").apply { writeText("audio-content") }
        connectivityBridge.downloadResults["log_20260327_140000.wav"] = WavDownloadResult.Success(
            localFile = emptyFile,
            originalFilename = "log_20260327_140000.wav",
            sizeBytes = 44L
        )
        connectivityBridge.downloadResults["log_20260327_140100.wav"] = WavDownloadResult.Success(
            localFile = normalFile,
            originalFilename = "log_20260327_140100.wav",
            sizeBytes = 2048L
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        assertEquals(2, outcome.queuedCount)

        advanceUntilIdle()

        assertTrue(runtime.audioFiles.value.any { it.filename == "log_20260327_140100.wav" })
        assertFalse(runtime.audioFiles.value.any { it.filename == "log_20260327_140000.wav" })
    }

    @Test
    fun `manual sync treats pipeline ingested recording as already present`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log#20260331_101500"))
        val tempAudio = tempFolder.newFile("badge_temp.wav").apply {
            writeText("audio-bytes")
        }
        SimBadgeAudioPipelineIngestSupport(runtime).ingestCompletedRecording(
            filename = "log_20260331_101500.wav",
            localFile = tempAudio,
            transcript = "客户确认下周二复盘。"
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync suppresses tombstoned badge filenames instead of reimporting them`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.deleteRecordingResults["log_20260327_135948.wav"] = false
        runtime.pendingBadgeDeletes.value = setOf("log_20260327_135948.wav")

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertEquals(setOf("log_20260327_135948.wav"), storeSupport.getPendingBadgeDeletesSnapshot())
        assertTrue(connectivityBridge.calls.contains("deleteRecording:log_20260327_135948.wav"))
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync clears tombstone when badge delete retry succeeds`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.deleteRecordingResults["log_20260327_135948.wav"] = true
        runtime.pendingBadgeDeletes.value = setOf("log_20260327_135948.wav")

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertEquals(0, outcome.queuedCount)
        assertTrue(storeSupport.getPendingBadgeDeletesSnapshot().isEmpty())
        assertTrue(connectivityBridge.calls.contains("deleteRecording:log_20260327_135948.wav"))
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    @Test
    fun `manual sync keeps failed placeholder visible and allows later files to continue`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("a.wav", "b.wav"))
        connectivityBridge.downloadResults["a.wav"] = WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
            message = "timeout"
        )
        connectivityBridge.downloadResults["b.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("b.wav").apply { writeText("audio-content") },
            originalFilename = "b.wav",
            sizeBytes = 2048L
        )

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)

        assertEquals(SimBadgeSyncResultBranch.QUEUED, outcome.resultBranch)
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.FAILED,
            runtime.audioFiles.value.single { it.filename == "a.wav" }.localAvailability
        )
        assertEquals(
            AudioLocalAvailability.READY,
            runtime.audioFiles.value.single { it.filename == "b.wav" }.localAvailability
        )
    }

    @Test
    fun `selectNewSimBadgeFilenames excludes pending badge deletes`() {
        assertEquals(
            listOf("b.wav"),
            selectNewSimBadgeFilenames(
                badgeFilenames = listOf("a.wav", "b.wav"),
                existingBadgeFilenames = emptySet(),
                pendingBadgeDeleteFilenames = setOf("a.wav")
            )
        )
    }

    @Test
    fun `canceled download transitions entry from DOWNLOADING to FAILED`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260401_090000.wav"))
        connectivityBridge.downloadSuspender = { kotlinx.coroutines.awaitCancellation() }

        syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.DOWNLOADING,
            runtime.audioFiles.value.single { it.filename == "log_20260401_090000.wav" }.localAvailability
        )

        syncSupport.cancelBadgeDownload("log_20260401_090000.wav")
        advanceUntilIdle()

        val entry = runtime.audioFiles.value.single { it.filename == "log_20260401_090000.wav" }
        assertEquals(AudioLocalAvailability.FAILED, entry.localAvailability)
        assertEquals("下载被中断，请重试同步", entry.lastErrorMessage)
    }

    @Test
    fun `canceled download allows remaining queued downloads to proceed`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("a.wav", "b.wav"))
        connectivityBridge.downloadSuspender = { kotlinx.coroutines.awaitCancellation() }
        connectivityBridge.downloadResults["b.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("b.wav").apply { writeText("audio-content") },
            originalFilename = "b.wav",
            sizeBytes = 2048L
        )

        syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        syncSupport.cancelBadgeDownload("a.wav")
        connectivityBridge.downloadSuspender = null
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.FAILED,
            runtime.audioFiles.value.single { it.filename == "a.wav" }.localAvailability
        )
        assertEquals(
            AudioLocalAvailability.READY,
            runtime.audioFiles.value.single { it.filename == "b.wav" }.localAvailability
        )
    }

    @Test
    fun `active device change cancels queued and active badge downloads`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        activeDeviceFlow.value = registeredDevice("AA:AA:AA:AA:AA:01")
        val repository = SimAudioRepository(runtime, orchestrator)
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("a.wav", "b.wav"))
        connectivityBridge.downloadSuspender = { kotlinx.coroutines.awaitCancellation() }

        advanceUntilIdle()
        repository.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        assertEquals(
            AudioLocalAvailability.DOWNLOADING,
            runtime.audioFiles.value.single { it.filename == "a.wav" }.localAvailability
        )
        assertTrue(runtime.queuedBadgeDownloads.contains("b.wav"))

        activeDeviceFlow.value = registeredDevice("BB:BB:BB:BB:BB:02")
        advanceUntilIdle()

        assertTrue(runtime.queuedBadgeDownloads.isEmpty())
        assertTrue(runtime.badgeDownloadWorkerJob?.isActive != true)
        assertEquals(
            AudioLocalAvailability.FAILED,
            runtime.audioFiles.value.single { it.filename == "a.wav" }.localAvailability
        )
        assertEquals(
            AudioLocalAvailability.FAILED,
            runtime.audioFiles.value.single { it.filename == "b.wav" }.localAvailability
        )
        assertFalse(connectivityBridge.calls.contains("downloadRecording:b.wav"))
        runtime.repositoryScope.cancel()
    }

    @Test
    fun `same remote filename is scoped separately per active badge`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        connectivityBridge.isReadyResult = true

        activeDeviceFlow.value = registeredDevice("AA:AA:AA:AA:AA:01")
        connectivityBridge.listResult = Result.Success(listOf("shared.wav"))
        connectivityBridge.downloadResults["shared.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("shared-a.wav").apply { writeText("audio-a") },
            originalFilename = "shared.wav",
            sizeBytes = 2048L
        )

        val firstOutcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        activeDeviceFlow.value = registeredDevice("BB:BB:BB:BB:BB:02")
        connectivityBridge.downloadResults["shared.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("shared-b.wav").apply { writeText("audio-b") },
            originalFilename = "shared.wav",
            sizeBytes = 2048L
        )

        val secondOutcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        val sharedEntries = runtime.audioFiles.value.filter { it.filename == "shared.wav" }
        assertEquals(SimBadgeSyncResultBranch.QUEUED, firstOutcome.resultBranch)
        assertEquals(SimBadgeSyncResultBranch.QUEUED, secondOutcome.resultBranch)
        assertEquals(2, sharedEntries.size)
        assertEquals(
            setOf("AA:AA:AA:AA:AA:01", "BB:BB:BB:BB:BB:02"),
            sharedEntries.map { it.badgeMac }.toSet()
        )
        assertTrue(sharedEntries.all { it.localAvailability == AudioLocalAvailability.READY })
    }

    @Test
    fun `sync ownership follows connected runtime key when registry active lags`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        activeDeviceFlow.value = registeredDevice("AA:AA:AA:AA:AA:01")
        endpointRecoveryCoordinator.noteCurrentRuntimeKey(
            BadgeRuntimeKey(
                peripheralId = "BB:BB:BB:BB:BB:02",
                secureToken = "token-b"
            )
        )
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("runtime-owned.wav"))
        connectivityBridge.downloadResults["runtime-owned.wav"] = WavDownloadResult.Success(
            localFile = tempFolder.newFile("runtime-owned.wav").apply { writeText("audio-b") },
            originalFilename = "runtime-owned.wav",
            sizeBytes = 2048L
        )

        syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        val entry = runtime.audioFiles.value.single { it.filename == "runtime-owned.wav" }
        assertEquals("BB:BB:BB:BB:BB:02", entry.badgeMac)
        assertEquals(AudioLocalAvailability.READY, entry.localAvailability)
    }

    @Test
    fun `sync discards list result when active runtime changes before queueing`() = runTest {
        bindRuntimeToTestScheduler(testScheduler)
        endpointRecoveryCoordinator.noteCurrentRuntimeKey(
            BadgeRuntimeKey(
                peripheralId = "BB:BB:BB:BB:BB:02",
                secureToken = "token-b"
            )
        )
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("stale-b.wav"))
        connectivityBridge.onListRecordings = {
            endpointRecoveryCoordinator.noteCurrentRuntimeKey(
                BadgeRuntimeKey(
                    peripheralId = "AA:AA:AA:AA:AA:01",
                    secureToken = "token-a"
                )
            )
        }

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        advanceUntilIdle()

        assertEquals(SimBadgeSyncResultBranch.ALREADY_PRESENT, outcome.resultBranch)
        assertTrue(runtime.audioFiles.value.isEmpty())
        assertTrue(runtime.queuedBadgeDownloads.isEmpty())
        assertFalse(connectivityBridge.calls.any { it.startsWith("downloadRecording:") })
    }

    private fun bindRuntimeToTestScheduler(scheduler: TestCoroutineScheduler) {
        val dispatcher = StandardTestDispatcher(scheduler)
        runtime.overrideConcurrencyForTests(
            dispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
    }

    private suspend fun noteActiveEndpoint(
        peripheralId: String,
        secureToken: String,
        badgeIp: String
    ): BadgeRuntimeKey {
        val runtimeKey = BadgeRuntimeKey(peripheralId = peripheralId, secureToken = secureToken)
        endpointRecoveryCoordinator.noteCurrentRuntimeKey(runtimeKey)
        endpointRecoveryCoordinator.noteResolvedEndpoint(
            BadgeEndpointSnapshot(
                runtimeKey = runtimeKey,
                badgeIp = badgeIp,
                baseUrl = "http://$badgeIp:8088"
            )
        )
        return runtimeKey
    }

    private fun registeredDevice(macAddress: String): RegisteredDevice = RegisteredDevice(
        macAddress = macAddress,
        displayName = "Badge ${macAddress.takeLast(2)}",
        profileId = null,
        registeredAtMillis = 1L,
        lastConnectedAtMillis = 1L,
        isDefault = false
    )

    private class FakeConnectivityBridge : ConnectivityBridge {
        override val connectionState = MutableStateFlow<BadgeConnectionState>(
            BadgeConnectionState.Disconnected
        )
        override val managerStatus = MutableStateFlow<BadgeManagerStatus>(
            BadgeManagerStatus.Disconnected
        )

        var isReadyResult: Boolean = false
        var listResult: Result<List<String>> = Result.Success(emptyList())
        var downloadResults: MutableMap<String, WavDownloadResult> = mutableMapOf()
        var deleteRecordingResults: MutableMap<String, Boolean> = mutableMapOf()
        var downloadSuspender: (suspend () -> Unit)? = null
        var onListRecordings: (suspend () -> Unit)? = null
        val calls = mutableListOf<String>()

        override suspend fun downloadRecording(
            filename: String,
            onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)?
        ): WavDownloadResult {
            calls += "downloadRecording:$filename"
            downloadSuspender?.invoke()
            return downloadResults[filename]
                ?: WavDownloadResult.Error(
                    code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                    message = "missing stub"
                )
        }

        override suspend fun listRecordings(): Result<List<String>> {
            calls += "listRecordings"
            onListRecordings?.invoke()
            return listResult
        }

        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()

        override fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady> = emptyFlow()

        override fun batteryNotifications(): Flow<Int> = emptyFlow()

        override fun firmwareVersionNotifications(): Flow<String> = emptyFlow()

        override fun sdCardSpaceNotifications(): Flow<String> = emptyFlow()

        override suspend fun isReady(): Boolean {
            calls += "isReady"
            return isReadyResult
        }

        override suspend fun deleteRecording(filename: String): Boolean {
            calls += "deleteRecording:$filename"
            return deleteRecordingResults[filename] ?: true
        }

        override suspend fun requestFirmwareVersion(): Boolean {
            calls += "requestFirmwareVersion"
            return false
        }

        override suspend fun requestSdCardSpace(): Boolean {
            calls += "requestSdCardSpace"
            return false
        }

        override suspend fun notifyCommandEnd() {
            calls += "notifyCommandEnd"
        }

        override fun wifiRepairEvents(): kotlinx.coroutines.flow.Flow<com.smartsales.prism.domain.connectivity.WifiRepairEvent> =
            emptyFlow()
    }

    private class FakeConnectivityPrompt : ConnectivityPrompt {
        val suggestedSsids = mutableListOf<String?>()
        val isolationPrompts = mutableListOf<Pair<String, IsolationTriggerContext>>()

        override suspend fun promptWifiMismatch(suggestedSsid: String?) {
            suggestedSsids += suggestedSsid
        }

        override suspend fun promptSuspectedIsolation(
            badgeIp: String,
            triggerContext: IsolationTriggerContext,
            suggestedSsid: String?
        ) {
            isolationPrompts += badgeIp to triggerContext
        }
    }
}
