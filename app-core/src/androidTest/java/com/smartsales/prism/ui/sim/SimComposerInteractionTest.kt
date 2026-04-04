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
            Box(modifier = Modifier.fillMaxSize()) {
                AgentIntelligenceScreen(
                    viewModel = viewModel,
                    visualMode = AgentIntelligenceVisualMode.SIM,
                    showDebugButton = false,
                    onAttachClick = { attachClickCount += 1 },
                    enableSimSchedulerPullGesture = true,
                    enableSimAudioPullGesture = true,
                    onSimSchedulerPullOpen = { schedulerOpenCount += 1 },
                    onSimAudioPullOpen = { audioOpenCount += 1 },
                    simVoiceDraftStateOverride = SimVoiceDraftUiState(),
                    simVoiceDraftEnabledOverride = true,
                    onSimVoiceDraftStart = { false }
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

    @Test
    fun simComposer_voiceDraftFillsInputAndTurnsActionIntoSend() {
        val viewModel = FakeAgentViewModel()
        var finishCount = 0

        composeTestRule.setContent {
            var voiceDraftState by remember {
                mutableStateOf(
                    SimVoiceDraftUiState(
                        isRecording = true,
                        interactionMode = SimVoiceDraftInteractionMode.TAP_TO_SEND
                    )
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AgentIntelligenceScreen(
                    viewModel = viewModel,
                    visualMode = AgentIntelligenceVisualMode.SIM,
                    showDebugButton = false,
                    simVoiceDraftStateOverride = voiceDraftState,
                    simVoiceDraftEnabledOverride = true,
                    onSimVoiceDraftFinish = {
                        finishCount += 1
                        viewModel.updateInput("语音草稿内容")
                        voiceDraftState = SimVoiceDraftUiState()
                    },
                    onSimVoiceDraftCancel = {
                        voiceDraftState = SimVoiceDraftUiState()
                    }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SIM_SEND_BUTTON_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                up()
            }

        composeTestRule.onNodeWithText("语音草稿内容").assertExists()

        composeTestRule.onNodeWithTag(SIM_SEND_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, finishCount)
            assertEquals("", viewModel.inputText.value)
            assertEquals(1, viewModel.history.value.size)
        }
    }
}
