package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDrawerLiveObservationTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `badge audio drawer inventory flows stay eagerly observed`() {
        val simDrawerViewModel = readSource(
            "app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt"
        )
        val sharedAudioViewModel = readSource(
            "app-core/src/main/java/com/smartsales/prism/ui/drawers/audio/AudioViewModel.kt"
        )

        assertTrue(
            simDrawerViewModel.hasEagerAudioStateIn(
                propertyName = "entries",
                sourceCall = "repository.getAudioFiles()"
            )
        )
        assertTrue(
            sharedAudioViewModel.hasEagerAudioStateIn(
                propertyName = "audioItems",
                sourceCall = "audioRepository.getAudioFiles()"
            )
        )
        assertFalse(simDrawerViewModel.contains("SharingStarted.WhileSubscribed"))
        assertFalse(sharedAudioViewModel.contains("SharingStarted.WhileSubscribed"))
    }

    private fun String.hasEagerAudioStateIn(propertyName: String, sourceCall: String): Boolean {
        val compact = filterNot { it.isWhitespace() }
        return compact.contains("val$propertyName:") &&
            compact.contains("=$sourceCall.map") &&
            compact.contains("started=SharingStarted.Eagerly")
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
