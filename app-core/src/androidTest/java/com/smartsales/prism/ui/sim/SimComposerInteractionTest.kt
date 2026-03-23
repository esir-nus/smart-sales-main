package com.smartsales.prism.ui.sim

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.AgentIntelligenceVisualMode
import com.smartsales.prism.ui.SIM_ATTACH_BUTTON_TEST_TAG
import com.smartsales.prism.ui.SIM_INPUT_FIELD_TEST_TAG
import com.smartsales.prism.ui.SIM_SEND_BUTTON_TEST_TAG
import com.smartsales.prism.ui.fakes.FakeAgentViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SimComposerInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simComposer_remainsInteractiveUnderShellGestureLayer() {
        val viewModel = FakeAgentViewModel()
        var schedulerOpenCount = 0
        var audioOpenCount = 0
        var attachClickCount = 0

        composeTestRule.setContent {
            var headerBottomPx by remember { mutableStateOf<Float?>(null) }
            var composerTopPx by remember { mutableStateOf<Float?>(null) }
            var rootHeightPx by remember { mutableStateOf(0f) }
            val gestureAnchors = remember(headerBottomPx, composerTopPx, rootHeightPx) {
                buildSimGestureAnchors(
                    headerBottomPx = headerBottomPx,
                    composerTopPx = composerTopPx,
                    rootHeightPx = rootHeightPx
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        rootHeightPx = coordinates.size.height.toFloat()
                    }
            ) {
                AgentIntelligenceScreen(
                    viewModel = viewModel,
                    visualMode = AgentIntelligenceVisualMode.SIM,
                    showDebugButton = false,
                    onAttachClick = { attachClickCount += 1 },
                    onSimHeaderBoundsChanged = { bounds ->
                        headerBottomPx = bounds.bottom
                    },
                    onSimComposerBoundsChanged = { bounds ->
                        composerTopPx = bounds.top
                    }
                )
                SimDrawerEdgeGestureLayer(
                    state = SimShellState(),
                    isImeVisible = false,
                    gestureAnchors = gestureAnchors,
                    onOpenScheduler = { schedulerOpenCount += 1 },
                    onOpenAudioBrowse = { audioOpenCount += 1 }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SIM_INPUT_FIELD_TEST_TAG)
            .assertExists()
            .performClick()
        composeTestRule.onNodeWithTag(SIM_INPUT_FIELD_TEST_TAG)
            .performTextInput("hello sim")
        composeTestRule.onNodeWithTag(SIM_ATTACH_BUTTON_TEST_TAG)
            .assertExists()
            .performClick()
        composeTestRule.onNodeWithTag(SIM_SEND_BUTTON_TEST_TAG)
            .assertExists()
            .performClick()

        composeTestRule.onNodeWithText("hello sim")
            .assertExists()

        composeTestRule.runOnIdle {
            assertEquals(0, schedulerOpenCount)
            assertEquals(0, audioOpenCount)
            assertEquals(1, attachClickCount)
            assertEquals("", viewModel.inputText.value)
            assertEquals(1, viewModel.history.value.size)
        }
    }
}
