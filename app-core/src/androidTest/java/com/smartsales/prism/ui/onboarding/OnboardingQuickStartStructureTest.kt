package com.smartsales.prism.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class OnboardingQuickStartStructureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun updatedQuickStartState_keepsFixedRowLanes() {
        composeTestRule.setContent {
            SchedulerQuickStartStaticStep(
                captureState = OnboardingQuickStartCaptureState.UPDATED
            )
        }

        composeTestRule.onNodeWithTag(ONBOARDING_QUICK_START_CARD_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(ONBOARDING_QUICK_START_COUNT_BADGE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag("$ONBOARDING_QUICK_START_ROW_TIME_TEST_TAG:preview-4").assertExists()
        composeTestRule.onNodeWithTag("$ONBOARDING_QUICK_START_ROW_DESC_TEST_TAG:preview-4").assertExists()
        composeTestRule.onNodeWithTag("$ONBOARDING_QUICK_START_ROW_BELLS_TEST_TAG:preview-4").assertExists()
        composeTestRule.onNodeWithTag("$ONBOARDING_QUICK_START_ROW_DATE_TEST_TAG:preview-4").assertExists()
    }

    @Test
    fun updatedQuickStartState_showsCondensedSuccessNote() {
        composeTestRule.setContent {
            SchedulerQuickStartStaticStep(
                captureState = OnboardingQuickStartCaptureState.UPDATED
            )
        }

        composeTestRule.onNodeWithTag(ONBOARDING_QUICK_START_SUCCESS_NOTE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithText("体验已就绪").assertExists()
        composeTestRule.onNodeWithText("你可以继续补充或修改，也可以直接进入下一步。").assertDoesNotExist()
        composeTestRule.onNodeWithText("继续下一步").assertExists()
    }

    @Test
    fun quickStartAutoScroll_keepsSuccessNoteVisible_whenPreviewAppearsAndUpdates() {
        var captureState by mutableStateOf(OnboardingQuickStartCaptureState.IDLE)

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .height(420.dp)
            ) {
                SchedulerQuickStartStaticStep(captureState = captureState)
            }
        }

        composeTestRule.onNodeWithTag(ONBOARDING_QUICK_START_SUCCESS_NOTE_TEST_TAG).assertDoesNotExist()

        composeTestRule.runOnIdle {
            captureState = OnboardingQuickStartCaptureState.INITIAL_LIST
        }
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ONBOARDING_QUICK_START_SUCCESS_NOTE_TEST_TAG).assertIsDisplayed()

        composeTestRule.runOnIdle {
            captureState = OnboardingQuickStartCaptureState.UPDATED
        }
        composeTestRule.mainClock.advanceTimeBy(1_500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(ONBOARDING_QUICK_START_SUCCESS_NOTE_TEST_TAG).assertIsDisplayed()
    }
}
