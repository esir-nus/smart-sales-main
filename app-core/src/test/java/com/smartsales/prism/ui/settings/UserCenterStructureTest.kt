package com.smartsales.prism.ui.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserCenterStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `full app user center keeps the screenshot override structure without biometric`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/settings/UserCenterScreen.kt")

        assertTrue(source.contains("widthIn(max = 356.dp)"))
        assertTrue(source.contains("contentAlignment = Alignment.TopCenter"))
        assertTrue(source.contains("UserCenterSection(title = \"空间管理\")"))
        assertTrue(source.contains("UserCenterInfoRow("))
        assertTrue(source.contains("label = \"已用空间\""))
        assertTrue(source.contains("UserCenterActionRow("))
        assertTrue(source.contains("label = \"清除缓存\""))
        assertTrue(source.contains("label = \"消息通知\""))
        assertFalse(source.contains("Icons.Default.Close"))
        assertFalse(source.contains("IconButton("))
        assertFalse(source.contains("label = \"面容 ID\""))
        assertFalse(source.contains("text = \"个人中心\""))
    }

    @Test
    fun `agent shell presents user center as a stronger centered overlay instead of a side drawer`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt")

        assertTrue(source.contains("Color.Black.copy(alpha = 0.38f)"))
        assertTrue(source.contains("padding(top = 12.dp, bottom = 8.dp)"))
        assertTrue(source.contains("scaleIn("))
        assertTrue(source.contains("scaleOut("))
        assertFalse(source.contains("slideInHorizontally("))
        assertFalse(source.contains("slideOutHorizontally("))
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
