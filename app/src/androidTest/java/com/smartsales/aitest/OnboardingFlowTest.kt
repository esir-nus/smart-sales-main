package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/OnboardingFlowTest.kt
// 模块：:app
// 说明：验证首次启动显示引导并保存个人信息后进入 Home
// 作者：创建于 2025-12-10

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsales.aitest.onboarding.OnboardingTestTags
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.usercenter.data.PersistentOnboardingStateRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()

    @Before
    fun setup() {
        PersistentOnboardingStateRepository.testOverrideCompleted = null
        clearOnboardingPrefs()
    }

    @After
    fun tearDown() {
        clearOnboardingPrefs()
        PersistentOnboardingStateRepository.testOverrideCompleted = null
    }

    @Test
    fun firstRun_showsOnboarding_thenEntersHome() {
        // welcome
        composeRule.onNodeWithTag(OnboardingTestTags.WELCOME, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTestTags.BUTTON_START, useUnmergedTree = true)
            .performClick()

        // personal info
        composeRule.onNodeWithTag(OnboardingTestTags.PERSONAL, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(OnboardingTestTags.FIELD_NAME, useUnmergedTree = true)
            .performTextClearance()
            .performTextInput("张三")
        composeRule.onNodeWithTag(OnboardingTestTags.FIELD_ROLE, useUnmergedTree = true)
            .performTextClearance()
            .performTextInput("销售顾问")
        composeRule.onNodeWithTag(OnboardingTestTags.FIELD_INDUSTRY, useUnmergedTree = true)
            .performTextClearance()
            .performTextInput("汽车")
        composeRule.onNodeWithTag(OnboardingTestTags.BUTTON_SAVE, useUnmergedTree = true)
            .performClick()

        // lands in Home, greeting uses saved name
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("你好，张三", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private fun clearOnboardingPrefs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("onboarding_state_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
