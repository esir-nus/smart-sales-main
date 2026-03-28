package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.core.util.Result
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.connectivity.BadgeConnectionState
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.RecordingNotification
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SimAudioRepositorySyncSupportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var connectivityBridge: FakeConnectivityBridge
    private lateinit var syncSupport: SimAudioRepositorySyncSupport

    @Before
    fun setup() {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)

        connectivityBridge = FakeConnectivityBridge()
        val runtime = SimAudioRepositoryRuntime(
            context = context,
            connectivityBridge = connectivityBridge,
            ossUploader = mock<OssUploader>(),
            tingwuPipeline = mock<TingwuPipeline>()
        )
        syncSupport = SimAudioRepositorySyncSupport(
            runtime = runtime,
            storeSupport = SimAudioRepositoryStoreSupport(runtime)
        )
    }

    @Test
    fun `manual sync fails at strict preflight before list work begins`() = runTest {
        connectivityBridge.isReadyResult = false

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
    }

    @Test
    fun `auto sync skips quietly when strict preflight is not ready`() = runTest {
        connectivityBridge.isReadyResult = false

        val outcome = syncSupport.syncFromBadge(SimBadgeSyncTrigger.AUTO)

        assertEquals(SimBadgeSyncSkippedReason.NOT_READY, outcome.skippedReason)
        assertEquals(listOf("isReady"), connectivityBridge.calls)
    }

    @Test
    fun `manual sync reaches list stage after ready preflight and surfaces list failure`() = runTest {
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Error(Exception("socket timeout"))

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
        assertEquals(listOf("isReady", "listRecordings"), connectivityBridge.calls)
    }

    @Test
    fun `manual sync reaches download stage after ready preflight and surfaces download failure`() = runTest {
        connectivityBridge.isReadyResult = true
        connectivityBridge.listResult = Result.Success(listOf("log_20260327_135948.wav"))
        connectivityBridge.downloadResults["log_20260327_135948.wav"] = WavDownloadResult.Error(
            code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
            message = "timeout"
        )

        val error = runCatching {
            syncSupport.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
        }.exceptionOrNull()

        assertEquals(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE, error?.message)
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
        val calls = mutableListOf<String>()

        override suspend fun downloadRecording(filename: String): WavDownloadResult {
            calls += "downloadRecording:$filename"
            return downloadResults[filename]
                ?: WavDownloadResult.Error(
                    code = WavDownloadResult.ErrorCode.DOWNLOAD_FAILED,
                    message = "missing stub"
                )
        }

        override suspend fun listRecordings(): Result<List<String>> {
            calls += "listRecordings"
            return listResult
        }

        override fun recordingNotifications(): Flow<RecordingNotification> = emptyFlow()

        override suspend fun isReady(): Boolean {
            calls += "isReady"
            return isReadyResult
        }

        override suspend fun deleteRecording(filename: String): Boolean = true
    }
}
