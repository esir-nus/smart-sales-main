package com.smartsales.prism.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
                handshakeHint = "试试说一句",
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
    fun processingTranscript_remainsVisibleWithoutRecordingState() {
        val transcript = "帮我搞定这个客户"

        composeTestRule.setContent {
            OnboardingMicFooter(
                isRecording = false,
                isProcessing = true,
                interactionMode = OnboardingMicInteractionMode.HOLD_TO_SEND,
                handshakeHint = transcript,
                processingLabel = "正在思考...",
                onPressStart = { false },
                onPressEnd = {},
                onPressCancel = {}
            )
        }

        composeTestRule.onNodeWithText(transcript).assertExists()
        composeTestRule.onNodeWithText("正在思考...").assertExists()
    }
}
