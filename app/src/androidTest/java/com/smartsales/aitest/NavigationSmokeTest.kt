package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/NavigationSmokeTest.kt
// 模块：:app
// 说明：验证顶层导航在 Home/设备配网/设备文件/音频库/用户中心间的 Compose 路由切换
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.testing.waitForAnyTag
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.usercenter.UserCenterTestTags
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {

    private val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()
    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeRule)

    @Test
    fun launchesHomeOverlayByDefault() {
        goHome()
        waitForHomeRendered()
    }

    @Test
    fun audioOverlayShowsAudioFiles() {
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, AudioFilesTestTags.ROOT, AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.onNodeWithText("管理录音、同步 Tingwu 转写并用 AI 分析通话。", substring = true).assertIsDisplayed()
        val analysisButtons = composeRule.onAllNodesWithText("用 AI 分析转写", substring = true).fetchSemanticsNodes()
        assert(analysisButtons.isEmpty())
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun historyToggleNavigatesToChatHistory() {
        goHome()
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, ChatHistoryTestTags.PAGE, AiFeatureTestTags.PAGE_CHAT_HISTORY)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForHomeRendered()
    }

    @Test
    fun profileNavigatesToUserCenter() {
        goHome()
        composeRule.onNodeWithTag(HomeScreenTestTags.PROFILE_BUTTON, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, UserCenterTestTags.ROOT, AiFeatureTestTags.PAGE_USER_CENTER)
        composeRule.onAllNodesWithText("账号信息、订阅与隐私偏好都在这里，保存后会同步到 React 端。", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()
        composeRule.onAllNodesWithText("订阅管理", substring = true, useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForHomeRendered()
    }

    @Test
    fun backFromOverlayReturnsHome() {
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, AudioFilesTestTags.ROOT, AiFeatureTestTags.PAGE_AUDIO_FILES)

        goHome()
    }

    private fun goHome() {
        waitForShell()

        val homeVisible = hasTag(HomeScreenTestTags.ROOT)
        if (homeVisible) return

        val backdropExists = hasTag(AiFeatureTestTags.OVERLAY_BACKDROP)
        if (backdropExists) {
            composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true).performClick()
        } else if (isOnNonHomePage()) {
            composeRule.activityRule.scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
            }
        }
        waitForHomeRendered()
    }

    private fun waitForHomeRendered() {
        waitForShell()
        composeRule.waitUntil(timeoutMillis = 60_000) { hasTag(HomeScreenTestTags.ROOT) }
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            hasTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE) &&
                hasTag(AiFeatureTestTags.OVERLAY_DEVICE_HANDLE)
        }
    }

    private fun waitForShell() {
        composeRule.waitUntil(timeoutMillis = 10_000) { hasTag(AiFeatureTestTags.OVERLAY_SHELL) }
    }

    private fun hasTag(tag: String): Boolean =
        runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)

    private fun isOnNonHomePage(): Boolean = hasTag(AiFeatureTestTags.PAGE_USER_CENTER) ||
        hasTag(AiFeatureTestTags.PAGE_CHAT_HISTORY) ||
        hasTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER) ||
        hasTag(AiFeatureTestTags.PAGE_DEVICE_SETUP) ||
        hasTag(AiFeatureTestTags.PAGE_AUDIO_FILES) ||
        hasTag(AiFeatureTestTags.PAGE_WIFI)
}
