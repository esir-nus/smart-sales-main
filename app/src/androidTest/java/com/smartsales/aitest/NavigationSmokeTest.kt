package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/NavigationSmokeTest.kt
// 模块：:app
// 说明：验证顶层导航在 Home/设备配网/设备文件/音频库/用户中心间的 Compose 路由切换
// 作者：创建于 2025-11-21

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.aitest.devicemanager.DeviceManagerRouteTestTags
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

    @Test
    fun launchesHomeByDefault() {
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT).assertIsDisplayed()
        composeRule.onAllNodesWithTag(AudioFilesTestTags.ROOT).assertCountEquals(0)
        composeRule.onAllNodesWithTag(DeviceManagerRouteTestTags.ROOT).assertCountEquals(0)
        composeRule.onAllNodesWithTag(DeviceSetupRouteTestTags.PAGE).assertCountEquals(0)
        composeRule.onAllNodesWithTag(UserCenterTestTags.ROOT).assertCountEquals(0)
    }

    @Test
    fun navigateToAudioFilesFromHomeShell() {
        selectTab(AiFeatureTestTags.CHIP_AUDIO_FILES)
        composeRule.onNodeWithTag(AudioFilesTestTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun navigateToDeviceManagerFromHomeShell() {
        selectTab(AiFeatureTestTags.CHIP_DEVICE_MANAGER)
        composeRule.onNodeWithTag(DeviceManagerRouteTestTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun navigateToDeviceSetupFromHomeShell() {
        selectTab(AiFeatureTestTags.CHIP_DEVICE_SETUP)
        composeRule.onNodeWithTag(DeviceSetupRouteTestTags.PAGE).assertIsDisplayed()
    }

    @Test
    fun navigateToUserCenterAndBackToHome() {
        selectTab(AiFeatureTestTags.CHIP_USER_CENTER)
        composeRule.onNodeWithTag(UserCenterTestTags.ROOT).assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT).assertIsDisplayed()
    }

    @Test
    fun switchTabsDoesNotCrash() {
        val tabs = listOf(
            AiFeatureTestTags.CHIP_HOME,
            AiFeatureTestTags.CHIP_AUDIO_FILES,
            AiFeatureTestTags.CHIP_DEVICE_MANAGER,
            AiFeatureTestTags.CHIP_USER_CENTER,
            AiFeatureTestTags.CHIP_DEVICE_SETUP,
            AiFeatureTestTags.CHIP_HOME
        )
        tabs.forEach { tag ->
            selectTab(tag)
        }
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT).assertIsDisplayed()
    }

    private fun selectTab(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
    }
}
