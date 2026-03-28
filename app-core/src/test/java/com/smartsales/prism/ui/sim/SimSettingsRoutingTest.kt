package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

class SimSettingsRoutingTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `openSimSettings closes other overlays and opens settings`() {
        val updated = openSimSettings(
            SimShellState(
                activeDrawer = SimDrawerType.AUDIO,
                audioDrawerMode = SimAudioDrawerMode.CHAT_RESELECT,
                activeConnectivitySurface = SimConnectivitySurface.MODAL,
                showHistory = true
            )
        )

        assertEquals(null, updated.activeDrawer)
        assertEquals(SimAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertEquals(null, updated.activeConnectivitySurface)
        assertFalse(updated.showHistory)
        assertTrue(updated.showSettings)
    }

    @Test
    fun `closeSimSettings keeps other shell state intact`() {
        val updated = closeSimSettings(
            SimShellState(showSettings = true)
        )

        assertFalse(updated.showSettings)
        assertEquals(null, updated.activeDrawer)
        assertEquals(null, updated.activeConnectivitySurface)
    }

    @Test
    fun `settings drawer keeps scrim visible with generic overlay alpha`() {
        val state = SimShellState(showSettings = true)

        assertTrue(shouldShowSimShellScrim(state))
        assertEquals(0.4f, resolveSimShellScrimAlpha(state))
    }

    @Test
    fun `settings drawer blocks shell edge gestures`() {
        val state = SimShellState(showSettings = true)

        assertFalse(canOpenSimSchedulerFromEdge(state))
        assertFalse(canOpenSimAudioFromEdge(state, isImeVisible = false))
    }

    @Test
    fun `sim settings drawer source removes stale close icon and face id row`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")

        assertFalse(source.contains("Icons.Default.Close"))
        assertFalse(source.contains("IconButton("))
        assertFalse(source.contains("label = \"面容 ID\""))
        assertTrue(source.contains(".prismStatusBarTopSafeBandPadding()"))
        assertTrue(source.contains(".pointerInput(Unit)"))
    }

    @Test
    fun `sim settings drawer keeps approved storage split and about order`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")

        assertTrue(source.contains("label = \"已用空间\""))
        assertTrue(source.contains("label = \"清除缓存\""))
        assertFalse(source.contains("label = \"本地缓存\""))
        assertFalse(source.contains("清除 (128MB)"))
        assertTrue(source.contains("label = \"版本\""))
        assertTrue(source.contains("label = \"帮助中心\""))
        assertTrue(source.indexOf("label = \"版本\"") < source.indexOf("label = \"帮助中心\""))
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
