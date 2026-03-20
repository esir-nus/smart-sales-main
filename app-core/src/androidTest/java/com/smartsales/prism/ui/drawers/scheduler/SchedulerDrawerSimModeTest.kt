package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SchedulerDrawerSimModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simModeHidesDeprecatedInspirationMultiSelectBar() {
        val viewModel = FakeSchedulerViewModel().apply {
            toggleSelectionMode(true)
            toggleItemSelection("insp_1")
        }

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                enableInspirationMultiSelect = false,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("问AI (1)").assertDoesNotExist()
    }

    @Test
    fun defaultModeStillShowsInspirationMultiSelectBarWhenStateIsActive() {
        val viewModel = FakeSchedulerViewModel().apply {
            toggleSelectionMode(true)
            toggleItemSelection("insp_1")
        }

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("问AI (1)").assertExists()
    }

    @Test
    fun simModeKeepsShelfAskAiAsOnlyInspirationLauncher() {
        val viewModel = FakeSchedulerViewModel().apply {
            toggleSelectionMode(true)
            toggleItemSelection("insp_1")
        }
        var capturedPrompt: String? = null

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                onInspirationAskAi = { capturedPrompt = it },
                enableInspirationMultiSelect = false,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("问AI (1)").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Ask AI").assertCountEquals(1)
        composeTestRule.onNodeWithText("Ask AI").performClick()

        assertEquals("Follow up with Acme Corp about the Q3 renewal.", capturedPrompt)
    }
}
