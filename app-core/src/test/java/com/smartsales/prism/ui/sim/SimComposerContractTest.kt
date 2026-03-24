package com.smartsales.prism.ui.sim

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimComposerContractTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `sim composer no longer advertises long press mic placeholder copy`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")

        assertTrue(source.contains("text = \"输入消息...\""))
        assertFalse(source.contains("输入消息，或长按工牌说话"))
    }

    @Test
    fun `sim composer uses send only action instead of mic route`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")

        assertTrue(source.contains("imageVector = Icons.AutoMirrored.Filled.Send"))
        assertFalse(source.contains("onMicClick = onAudioDrawerClick"))
    }

    @Test
    fun `sim composer placeholder uses dedicated shimmer brush helper`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentIntelligenceContent.kt")

        assertTrue(source.contains("private fun simPlaceholderBrush(): Brush"))
        assertTrue(source.contains("Brush.horizontalGradient("))
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
