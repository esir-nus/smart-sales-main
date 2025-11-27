package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/NavigationSmokeTest.kt
// 模块：:app
// 说明：验证顶层导航在 Home/设备配网/设备文件/音频库/用户中心间的 Compose 路由切换
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.devicemanager.DeviceManagerRouteTestTags
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.aitest.setup.DeviceSetupRouteTestTags
import com.smartsales.aitest.ui.HomeOverlayTestTags
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.usercenter.UserCenterTestTags
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {

    // 自动授予必要权限，避免弹窗导致导航用例失败
    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()

    @Test
    fun launchesHomeByDefault() {
        waitForAnyTag(
            tags = listOf(
                HomeOverlayTestTags.HOME_LAYER,
                AiFeatureTestTags.OVERLAY_SHELL
            ),
            timeout = 20_000
        )
        composeRule.onAllNodesWithTag(AudioFilesTestTags.ROOT).assertCountEquals(0)
        composeRule.onAllNodesWithTag(DeviceManagerRouteTestTags.ROOT).assertCountEquals(0)
        composeRule.onAllNodesWithTag(DeviceSetupRouteTestTags.PAGE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UserCenterTestTags.ROOT).assertCountEquals(0)
    }

    @Test
    fun navigateToAudioFilesFromHomeShell() {
        // 当前 shell 无音频库 chip，改为通过 Home 音频入口验证导航由 Home 层覆盖
        waitForTag(HomeScreenTestTags.AUDIO_ENTRY)
        composeRule.onNodeWithTag(HomeScreenTestTags.AUDIO_ENTRY).performClick()
        waitForOverlay(HomeOverlayTestTags.AUDIO_LAYER)
    }

    @Test
    fun navigateToDeviceManagerFromHomeShell() {
        waitForTag(HomeScreenTestTags.DEVICE_ENTRY)
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).performClick()
        waitForAnyTag(
            tags = listOf(
                HomeOverlayTestTags.DEVICE_LAYER,
                DeviceManagerRouteTestTags.ROOT,
                AiFeatureTestTags.PAGE_DEVICE_SETUP
            ),
            timeout = 25_000
        )
    }

    @Test
    @Ignore("TODO: overlay/page readiness; re-enable after adding explicit waits for navigation transitions")
    fun navigateToDeviceSetupFromHomeShell() {
        selectTab(AiFeatureTestTags.CHIP_DEVICE_SETUP)
        waitForTag(DeviceSetupRouteTestTags.PAGE)
    }

    @Test
    @Ignore("TODO: overlay/page readiness; re-enable after adding explicit waits for navigation transitions")
    fun navigateToUserCenterAndBackToHome() {
        selectTab(AiFeatureTestTags.CHIP_USER_CENTER)
        waitForTag(UserCenterTestTags.ROOT)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)
    }

    @Test
    fun switchTabsDoesNotCrash() {
        val tabs = listOf(
            AiFeatureTestTags.CHIP_HOME,
            AiFeatureTestTags.CHIP_USER_CENTER,
            AiFeatureTestTags.CHIP_DEVICE_SETUP,
            AiFeatureTestTags.CHIP_CHAT_HISTORY,
            AiFeatureTestTags.CHIP_HOME
        )
        tabs.forEach { tag ->
            selectTab(tag)
            composeRule.waitForIdle()
        }
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)
    }

    private fun selectTab(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun waitForTag(tag: String, timeout: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun waitForOverlay(tag: String, timeout: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun waitForAnyTag(tags: List<String>, timeout: Long) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            tags.any { tag ->
                runCatching {
                    composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                        .fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false)
            }
        }
        // Try to assert the first tag found for visibility
        tags.firstOrNull { tag ->
            runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }?.let { foundTag ->
            composeRule.onNodeWithTag(foundTag, useUnmergedTree = true).assertIsDisplayed()
        }
    }
}
