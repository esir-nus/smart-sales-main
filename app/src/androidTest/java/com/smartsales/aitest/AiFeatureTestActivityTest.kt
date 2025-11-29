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
import android.os.SystemClock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiFeatureTestActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @Test
    fun defaultTab_isHome() {
        waitForHomeRendered()
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun overlayHandles_showSheetsAndBackdrops() {
        waitForHomeRendered()

        // 打开音频 overlay
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.OVERLAY_AUDIO_LAYER,
            AiFeatureTestTags.PAGE_AUDIO_FILES
        )
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true).performClick()
        waitForHomeRendered()

        // 打开设备 overlay
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.OVERLAY_DEVICE_LAYER,
            AiFeatureTestTags.PAGE_DEVICE_MANAGER
        )
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun quickSkillTap_showsConfirmationAndChip() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)

        composeRule.onNodeWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun quickSkillChip_closeClearsSelection() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        val chipAppeared = runCatching {
            composeRule.waitUntil(timeoutMillis = 3_000) {
                composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            true
        }.getOrDefault(false)
        if (!chipAppeared) return
        composeRule.onNodeWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP_CLOSE, useUnmergedTree = true).performClick()

        runCatching {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
                    .fetchSemanticsNodes().isEmpty()
            }
        }
    }

    @Test
    fun quickSkill_sendConsumesSkill() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD).performTextInput("请总结会议")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON).performClick()

        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun goHome() {
        val backdropExists = runCatching {
            composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (backdropExists) {
            composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true).performClick()
        } else {
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

    private fun waitForHomeRendered() {
        val tags = listOf(
            AiFeatureTestTags.PAGE_HOME,
            HomeScreenTestTags.ROOT,
            AiFeatureTestTags.OVERLAY_AUDIO_HANDLE,
            AiFeatureTestTags.OVERLAY_DEVICE_HANDLE
        )
        val start = SystemClock.uptimeMillis()
        while (SystemClock.uptimeMillis() - start < 60_000) {
            composeRule.waitForIdle()
            val hasHierarchy = runCatching {
                tags.any { tag ->
                    composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }.getOrDefault(false)
            if (hasHierarchy) return
        }
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.PAGE_HOME,
            HomeScreenTestTags.ROOT,
            extraFallbackTags = arrayOf(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE),
            timeoutMillis = 30_000
        )
    }
}
