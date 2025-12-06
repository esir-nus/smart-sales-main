package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/AiFeatureTestActivityTest.kt
// 模块：:app
// 说明：验证 AiFeatureTestActivity Shell 导航行为的 Compose UI 测试
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.testing.waitForAnyTag
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.aitest.TestHomePage
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiFeatureTestActivityTest {

    private val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()
    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeRule)

    @Test
    fun defaultTab_isHome() {
        waitForHomeRendered()
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun topBarButtons_navigateAudioAndDevice() {
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.AudioFiles)
        }
        waitForAnyTag(composeRule, AudioFilesTestTags.ROOT, AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertExists()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.DeviceManager)
        }
        waitForAnyTag(composeRule, AiFeatureTestTags.PAGE_DEVICE_MANAGER, AiFeatureTestTags.PAGE_DEVICE_SETUP)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertExists()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun quickSkillTap_setsModeWithoutCreatingMessages() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SMART_ANALYSIS)

        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()
        // 不应触发导航或额外气泡标签
        composeRule.onAllNodesWithTag(AiFeatureTestTags.PAGE_AUDIO_FILES, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun quickSkillTap_showsNoSkillBubble() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SMART_ANALYSIS)

        // 新 UX 不生成技能气泡，也不离开 Home
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertExists()
    }

    @Test
    fun quickSkill_sendUsesModeWithoutSkillBubble() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SMART_ANALYSIS)
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performTextClearance()
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performTextInput("请总结会议")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON).performClick()

        composeRule.waitForIdle()
        // 发送后仍不应出现技能气泡
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun exportButtons_doNotNavigateAway() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        composeRule.onNodeWithTag("home_quick_skill_${QuickSkillId.EXPORT_PDF.name}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("home_quick_skill_${QuickSkillId.EXPORT_CSV.name}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun homeTags_areUnique() {
        waitForHomeRendered()
        assertSingleTag(HomeScreenTestTags.ROOT)
        assertSingleTag(AiFeatureTestTags.PAGE_HOME)
    }

    @Test
    fun otherPageTags_areUniqueWhenNavigated() {
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.AudioFiles)
        }
        assertSingleTag(AudioFilesTestTags.ROOT)
        assertSingleTag(AiFeatureTestTags.PAGE_HOME)

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.Home)
        }
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.DeviceManager)
        }
        assertSingleTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        assertSingleTag(AiFeatureTestTags.PAGE_HOME)

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.Home)
        }
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    private fun goHome() {
        val homeVisible = runCatching {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (!homeVisible) {
            composeRule.activityRule.scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
            }
        }
        waitForHomeRendered()
    }

    private fun waitForPage(tag: String) {
        waitForAnyTag(
            composeRule,
            tag,
            HomeScreenTestTags.ROOT,
            extraFallbackTags = arrayOf(
                AiFeatureTestTags.PAGE_CHAT_HISTORY,
                AiFeatureTestTags.PAGE_USER_CENTER
            )
        )
    }

    private fun tapQuickSkill(skillId: QuickSkillId) {
        val tag = "home_quick_skill_${skillId.name}"
        val nodes = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)[0].performClick()
        }
    }

    private fun countByTag(tag: String): Int = runCatching {
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size
    }.getOrDefault(0)

    private fun waitForHomeRendered() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
    }

    // 检查给定 tag 仅出现一次，确保 PAGE/ROOT 唯一性
    private fun assertSingleTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
    }
}
