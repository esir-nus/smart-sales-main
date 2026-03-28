package com.smartsales.prism.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingMicFooterGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun holdRelease_stillSendsAfterRecordingStateFlips() {
        var releaseCount = 0
        var cancelCount = 0

        composeTestRule.setContent {
            var isRecording by remember { mutableStateOf(false) }

            OnboardingMicFooter(
                isRecording = isRecording,
                isProcessing = false,
                interactionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                processingLabel = "正在思考...",
                onPressStart = {
                    isRecording = true
                    true
                },
                onPressEnd = {
                    isRecording = false
                    releaseCount += 1
                },
                onPressCancel = {
                    isRecording = false
                    cancelCount += 1
                }
            )
        }

        composeTestRule.onNodeWithTag(ONBOARDING_MIC_BUTTON_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, releaseCount)
            assertEquals(0, cancelCount)
        }
        composeTestRule.onNodeWithText("按住说话").assertExists()
    }

    @Test
    fun tapToSend_clickStopsListening() {
        var releaseCount = 0
        var cancelCount = 0

        composeTestRule.setContent {
            var isRecording by remember { mutableStateOf(true) }

            OnboardingMicFooter(
                isRecording = isRecording,
                isProcessing = false,
                interactionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                processingLabel = "正在思考...",
                onPressStart = { false },
                onPressEnd = {
                    isRecording = false
                    releaseCount += 1
                },
                onPressCancel = {
                    isRecording = false
                    cancelCount += 1
                }
            )
        }

        composeTestRule.onNodeWithTag(ONBOARDING_MIC_BUTTON_TEST_TAG)
            .assertExists()
            .performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertEquals(1, releaseCount)
            assertEquals(0, cancelCount)
        }
        composeTestRule.onNodeWithText("按住说话").assertExists()
    }
}
