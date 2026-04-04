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
    fun secondTap_stillSendsAfterRecordingStateFlips() {
        var startCount = 0
        var endCount = 0
        var cancelCount = 0

        composeTestRule.setContent {
            var isRecording by remember { mutableStateOf(false) }

            OnboardingMicFooter(
                isRecording = isRecording,
                isProcessing = false,
                interactionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
                handshakeHint = "试试说一句",
                processingLabel = "正在思考...",
                onPressStart = {
                    isRecording = true
                    startCount += 1
                    true
                },
                onPressEnd = {
                    isRecording = false
                    endCount += 1
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
            assertEquals(1, startCount)
            assertEquals(0, endCount)
            assertEquals(0, cancelCount)
        }
        composeTestRule.onNodeWithText("正在聆听...再次点击结束").assertExists()

        composeTestRule.onNodeWithTag(ONBOARDING_MIC_BUTTON_TEST_TAG)
            .performTouchInput {
                down(center)
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, startCount)
            assertEquals(1, endCount)
            assertEquals(0, cancelCount)
        }
        composeTestRule.onNodeWithText("点击开始说话").assertExists()
    }

    @Test
    fun processingTranscript_remainsVisibleWithoutRecordingState() {
        val transcript = "帮我搞定这个客户"

        composeTestRule.setContent {
            OnboardingMicFooter(
                isRecording = false,
                isProcessing = true,
                interactionMode = OnboardingMicInteractionMode.TAP_TO_SEND,
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
