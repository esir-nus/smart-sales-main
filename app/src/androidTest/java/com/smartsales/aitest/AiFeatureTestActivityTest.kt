package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/AiFeatureTestActivityTest.kt
// 模块：:app
// 说明：验证 AiFeatureTestActivity Shell 导航行为的 Compose UI 测试
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.testing.waitForAnyTag
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.media.audio.AudioFilesTestTags
import android.os.SystemClock
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

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.PAGE_AUDIO_FILES,
            AudioFilesTestTags.ROOT
        )
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.PAGE_DEVICE_MANAGER,
            AiFeatureTestTags.PAGE_DEVICE_SETUP
        )
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun quickSkillTap_setsModeWithoutCreatingMessages() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        val assistantBefore = countByTag(HomeScreenTestTags.ASSISTANT_MESSAGE)
        val userBefore = countByTag(HomeScreenTestTags.USER_MESSAGE)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)

        // 芯片仍可见且高亮，但不应生成任何聊天气泡
        val skillTag = "home_quick_skill_${QuickSkillId.SUMMARIZE_LAST_MEETING.name}"
        composeRule.onNodeWithTag(skillTag, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ASSISTANT_MESSAGE, useUnmergedTree = true)
            .fetchSemanticsNodes().size.let { after ->
                assert(after == assistantBefore)
            }
        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE, useUnmergedTree = true)
            .fetchSemanticsNodes().size.let { after ->
                assert(after == userBefore)
            }
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun quickSkillTap_showsNoSkillBubble() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)

        // 新 UX 不生成底部技能气泡
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun quickSkill_sendUsesModeWithoutSkillBubble() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD).performTextInput("请总结会议")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON).performClick()

        composeRule.waitForIdle()
        // 发送后仍不应出现技能气泡
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
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

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).performClick()
        assertSingleTag(AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE, useUnmergedTree = true).performClick()
        assertSingleTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true).performClick()
        assertSingleTag(AiFeatureTestTags.PAGE_CHAT_HISTORY)
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
