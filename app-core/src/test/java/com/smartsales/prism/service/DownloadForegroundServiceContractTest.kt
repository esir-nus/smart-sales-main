package com.smartsales.prism.service

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadForegroundServiceContractTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `snapshot tracks queued downloading ready and queue state`() {
        val snapshot = buildDownloadForegroundSnapshot(
            audioFiles = listOf(
                newAudioFile("queued.wav", AudioLocalAvailability.QUEUED),
                newAudioFile("downloading.wav", AudioLocalAvailability.DOWNLOADING),
                newAudioFile("ready.wav", AudioLocalAvailability.READY)
            ),
            queueSize = 2
        )

        assertEquals(1, snapshot.queuedCount)
        assertEquals(1, snapshot.downloadingCount)
        assertEquals(1, snapshot.completedCount)
        assertEquals(3, snapshot.totalCount)
        assertEquals("downloading.wav", snapshot.currentFilename)
        assertTrue(snapshot.hasPendingDownloads)
    }

    @Test
    fun `notification model shows active filename and completion ratio`() {
        val model = buildDownloadForegroundNotificationModel(
            DownloadForegroundSnapshot(
                downloadingCount = 1,
                queuedCount = 1,
                completedCount = 2,
                queueSize = 0,
                currentFilename = "call_001.wav"
            )
        )

        assertTrue(model.contentText.contains("call_001"))
        assertEquals(50, model.progressPercent)
        assertFalse(model.indeterminate)
    }

    @Test
    fun `stop coordinator stops only after idle debounce`() = runTest {
        val coordinator = DownloadForegroundStopCoordinator()
        var stopCount = 0
        val idleSnapshot = DownloadForegroundSnapshot(
            downloadingCount = 0,
            queuedCount = 0,
            completedCount = 1,
            queueSize = 0,
            currentFilename = null
        )

        coordinator.onSnapshot(idleSnapshot, this) {
            stopCount += 1
        }

        advanceTimeBy(DOWNLOAD_FOREGROUND_STOP_DEBOUNCE_MS - 1)
        assertEquals(0, stopCount)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(1, stopCount)
    }

    @Test
    fun `stop coordinator cancels pending stop when downloads resume`() = runTest {
        val coordinator = DownloadForegroundStopCoordinator()
        var stopCount = 0
        val idleSnapshot = DownloadForegroundSnapshot(
            downloadingCount = 0,
            queuedCount = 0,
            completedCount = 1,
            queueSize = 0,
            currentFilename = null
        )
        val activeSnapshot = idleSnapshot.copy(downloadingCount = 1, currentFilename = "resume.wav")

        coordinator.onSnapshot(idleSnapshot, this) {
            stopCount += 1
        }
        advanceTimeBy(DOWNLOAD_FOREGROUND_STOP_DEBOUNCE_MS / 2)

        coordinator.onSnapshot(activeSnapshot, this) {
            stopCount += 1
        }
        advanceTimeBy(DOWNLOAD_FOREGROUND_STOP_DEBOUNCE_MS)
        advanceUntilIdle()

        assertEquals(0, stopCount)
    }

    @Test
    fun `manifest declares foreground data sync service contract`() {
        val manifest = readSource("app-core/src/main/AndroidManifest.xml")
        val serviceSource = readSource(
            "app-core/src/main/java/com/smartsales/prism/service/DownloadForegroundService.kt"
        )

        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
        assertTrue(manifest.contains("android:name=\".service.DownloadForegroundService\""))
        assertTrue(manifest.contains("android:foregroundServiceType=\"dataSync\""))
        assertTrue(manifest.contains("android:stopWithTask=\"false\""))
        assertTrue(serviceSource.contains("super.onStartCommand("))
        assertTrue(serviceSource.contains("return START_STICKY"))
    }

    private fun newAudioFile(
        filename: String,
        availability: AudioLocalAvailability
    ): AudioFile {
        return AudioFile(
            id = filename,
            filename = filename,
            timeDisplay = "Now",
            source = AudioSource.SMARTBADGE,
            status = TranscriptionStatus.PENDING,
            localAvailability = availability
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File(workingDir, relativePath),
            File(workingDir, "app-core/$relativePath"),
            File(workingDir.parentFile ?: workingDir, relativePath),
            File(workingDir.parentFile ?: workingDir, "app-core/$relativePath")
        )

        return candidates.firstOrNull { it.exists() }?.readText()
            ?: error("Source file not found for $relativePath from ${workingDir.absolutePath}")
    }
}
