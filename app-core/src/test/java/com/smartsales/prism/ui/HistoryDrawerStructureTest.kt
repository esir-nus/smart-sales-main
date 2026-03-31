package com.smartsales.prism.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryDrawerStructureTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `history drawer keeps visible overflow as the primary row action seam`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt")

        assertTrue(source.contains("combinedClickable("))
        assertTrue(source.contains("onLongClick = { showMenu = true }"))
        assertTrue(source.contains("Icons.Filled.MoreVert"))
        assertTrue(source.contains("DropdownMenu("))
        assertTrue(source.contains("置顶"))
        assertTrue(source.contains("重命名"))
        assertTrue(source.contains("删除"))
    }

    @Test
    fun `agent shell keeps full app history drawer handoffs on the current ownership seams`() {
        val content = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentShellContent.kt")
        val reducer = readSource("app-core/src/main/java/com/smartsales/prism/ui/AgentShellReducer.kt")

        assertTrue(content.contains("HistoryDrawer("))
        assertTrue(content.contains("onDeviceClick = {"))
        assertTrue(reducer.contains("activeDrawer = DrawerType.CONNECTIVITY"))
        assertTrue(content.contains("onSettingsClick = openUserCenter"))
        assertTrue(content.contains("onProfileClick = openUserCenter"))
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
