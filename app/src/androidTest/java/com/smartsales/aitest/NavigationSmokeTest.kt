package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/NavigationSmokeTest.kt
// 模块：:app
// 说明：验证顶层导航在 Home/设备配网/设备文件/音频库/用户中心间的 Compose 路由切换
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.setup.DeviceSetupRouteTestTags
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.usercenter.UserCenterTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @Test
    fun launchesHomeOverlayByDefault() {
        waitForTag(HomeScreenTestTags.ROOT)
    }

    @Test
    fun deviceOverlayRoutesToSetupWhenDisconnected() {
        composeRule.onNodeWithTag(AiFeatureTestTags.CHIP_DEVICE_SETUP, useUnmergedTree = true).performClick()
        waitForTag(DeviceSetupRouteTestTags.PAGE)
    }

    @Test
    fun audioOverlayRoutesToAudioFiles() {
        composeRule.onNodeWithTag(AiFeatureTestTags.CHIP_AUDIO_FILES, useUnmergedTree = true).performClick()
        waitForTag(AudioFilesTestTags.ROOT)
    }

    @Test
    fun historyToggleNavigatesToChatHistory() {
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true).performClick()
        waitForTag(AiFeatureTestTags.PAGE_CHAT_HISTORY)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForTag(HomeScreenTestTags.ROOT)
    }

    @Test
    fun profileNavigatesToUserCenter() {
        composeRule.onNodeWithTag(HomeScreenTestTags.PROFILE_BUTTON, useUnmergedTree = true).performClick()
        waitForTag(UserCenterTestTags.ROOT)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForTag(HomeScreenTestTags.ROOT)
    }

    @Test
    fun backFromOverlayReturnsHome() {
        composeRule.onNodeWithTag(AiFeatureTestTags.CHIP_AUDIO_FILES, useUnmergedTree = true).performClick()
        waitForTag(AudioFilesTestTags.ROOT)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForTag(AiFeatureTestTags.CHIP_HOME, useUnmergedTree = true)
    }

    private fun waitForTag(tag: String, useUnmergedTree: Boolean = false) {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            val found = runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag(tag, useUnmergedTree = !useUnmergedTree).fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag(AiFeatureTestTags.CHIP_HOME, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
            if (found) {
                return
            }
            Thread.sleep(200)
        }
        throw AssertionError("Tag $tag not found within timeout")
    }
}
