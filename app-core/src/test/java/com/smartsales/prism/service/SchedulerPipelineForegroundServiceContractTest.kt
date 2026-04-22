package com.smartsales.prism.service

import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerPipelineForegroundServiceContractTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `service debounce constant matches approved stop window`() = runTest {
        assertEquals(800L, SCHEDULER_PIPELINE_STOP_DEBOUNCE_MS)
    }

    @Test
    fun `manifest declares scheduler foreground data sync service contract`() {
        val manifest = readSource("app-core/src/main/AndroidManifest.xml")
        val serviceSource = readSource(
            "app-core/src/main/java/com/smartsales/prism/service/SchedulerPipelineForegroundService.kt"
        )

        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE"))
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
        assertTrue(manifest.contains("android:name=\".service.SchedulerPipelineForegroundService\""))
        assertTrue(manifest.contains("android:foregroundServiceType=\"dataSync\""))
        assertTrue(serviceSource.contains("return START_NOT_STICKY"))
        assertTrue(serviceSource.contains("startForeground type=DATA_SYNC"))
        assertTrue(serviceSource.contains("stopSelf reason=drain_debounce"))
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
