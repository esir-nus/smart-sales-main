package com.smartsales.prism.ui.onboarding

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPermissionsPrimerResponsiveContractTest {

    private val workingDir = File(System.getProperty("user.dir") ?: ".")

    @Test
    fun `permission primer keeps scrollable content and pinned continue action`() {
        val source = readSource("app-core/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt")

        assertTrue(source.contains("rememberScrollState()"))
        assertTrue(source.contains(".verticalScroll(scrollState)"))
        assertTrue(source.contains("resolveShellLayoutMode("))
        assertTrue(source.contains("ONBOARDING_PERMISSIONS_CONTINUE_TEST_TAG"))
        assertTrue(source.contains(".align(Alignment.BottomCenter)"))
        assertTrue(source.contains(".navigationBarsPadding()"))
        assertTrue(source.contains("padding(bottom = metrics.scrollBottomClearance)"))
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
