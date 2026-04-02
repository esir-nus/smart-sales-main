package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

class SimSettingsRoutingTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `openRuntimeSettings closes other overlays and opens settings`() {
        val updated = openRuntimeSettings(
            RuntimeShellState(
                activeDrawer = RuntimeDrawerType.AUDIO,
                audioDrawerMode = RuntimeAudioDrawerMode.CHAT_RESELECT,
                activeConnectivitySurface = RuntimeConnectivitySurface.MODAL,
                showHistory = true
            )
        )

        assertEquals(null, updated.activeDrawer)
        assertEquals(RuntimeAudioDrawerMode.BROWSE, updated.audioDrawerMode)
        assertEquals(null, updated.activeConnectivitySurface)
        assertFalse(updated.showHistory)
        assertTrue(updated.showSettings)
    }

    @Test
    fun `closeRuntimeSettings keeps other shell state intact`() {
        val updated = closeRuntimeSettings(
            RuntimeShellState(showSettings = true)
        )

        assertFalse(updated.showSettings)
        assertEquals(null, updated.activeDrawer)
        assertEquals(null, updated.activeConnectivitySurface)
    }

    @Test
    fun `settings drawer keeps scrim visible with generic overlay alpha`() {
        val state = RuntimeShellState(showSettings = true)

        assertTrue(shouldShowRuntimeShellScrim(state))
        assertEquals(0.4f, resolveRuntimeShellScrimAlpha(state))
    }

    @Test
    fun `settings drawer blocks shell edge gestures`() {
        val state = RuntimeShellState(showSettings = true)

        assertFalse(canOpenSimSchedulerFromEdge(state))
        assertFalse(canOpenSimAudioFromEdge(state, isImeVisible = false))
    }

    @Test
    fun `sim settings drawer source removes stale close icon and face id row`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")

        assertFalse(source.contains("Icons.Default.Close"))
        assertFalse(source.contains("IconButton("))
        assertFalse(source.contains("label = \"面容 ID\""))
        assertTrue(source.contains("PrismStatusBarTopSafeArea()"))
        assertTrue(source.contains(".pointerInput(Unit)"))
    }

    @Test
    fun `sim settings drawer restores approved ia with explicit deferred rows`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")

        assertTrue(source.contains("label = \"主题外观\""))
        assertTrue(source.contains("label = \"AI 实验室\""))
        assertTrue(source.contains("label = \"消息通知\""))
        assertTrue(source.contains("label = \"已用空间\""))
        assertTrue(source.contains("label = \"清除缓存\""))
        assertTrue(source.contains("label = \"修改密码\""))
        assertTrue(source.contains("label = \"帮助中心\""))
        assertTrue(source.contains("label = \"版本\""))
        assertTrue(source.contains("text = \"退出登录\""))
        assertTrue(source.contains("BuildConfig.VERSION_NAME"))
        assertTrue(source.contains("SimUserCenterRowPresentation.DeferredDisabled"))
        assertTrue(source.contains("value = SimDeferredSettingValue"))
        assertTrue(source.contains("showChevron && presentation == SimUserCenterRowPresentation.Interactive"))
    }

    @Test
    fun `sim settings overlay uses deterministic handoff aligned animation instead of spring`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt")

        assertTrue(source.contains("val settingsDrawerSlideSpec = tween<IntOffset>("))
        assertTrue(source.contains("durationMillis = 400"))
        assertTrue(source.contains("val settingsDrawerFadeSpec = tween<Float>("))
        assertTrue(source.contains("durationMillis = 300"))
        assertTrue(source.contains("FastOutSlowInEasing"))
        assertFalse(source.contains("visible = shellState.showSettings,\n            enter = slideInHorizontally(\n                animationSpec = spring("))
    }

    @Test
    fun `sim settings drawer keeps lazy scroll container and local inset ownership`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/sim/SimUserCenterDrawer.kt")

        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("PrismStatusBarTopSafeArea()"))
        assertTrue(source.contains(".prismNavigationBarPadding()"))
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
