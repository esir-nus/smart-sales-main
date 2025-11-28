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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.testing.waitForAnyTag
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import com.smartsales.aitest.setup.DeviceSetupRouteTestTags
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.usercenter.UserCenterTestTags
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.aitest.testing.DeviceConnectionEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val connectionManager: DeviceConnectionManager by lazy {
        EntryPointAccessors.fromApplication(
            composeRule.activity.applicationContext,
            DeviceConnectionEntryPoint::class.java
        ).deviceConnectionManager()
    }

    @Test
    fun launchesHomeOverlayByDefault() {
        goHome()
        waitForHomeRendered()
    }

    @Test
    fun deviceOverlayRoutesToSetupWhenDisconnected() {
        forceDeviceDisconnected()
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            DeviceSetupRouteTestTags.PAGE,
            AiFeatureTestTags.PAGE_DEVICE_SETUP,
            AiFeatureTestTags.PAGE_DEVICE_MANAGER,
            extraFallbackTags = arrayOf(AiFeatureTestTags.OVERLAY_DEVICE, AiFeatureTestTags.PAGE_HOME)
        )
    }

    @Test
    fun audioOverlayRoutesToAudioFiles() {
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, AudioFilesTestTags.ROOT, AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.onNodeWithText("同步并管理设备录音", substring = true).assertIsDisplayed()
        val analysisButtons = composeRule.onAllNodesWithText("用 AI 分析转写", substring = true).fetchSemanticsNodes()
        assert(analysisButtons.isEmpty())
    }

    @Test
    fun historyToggleNavigatesToChatHistory() {
        goHome()
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true).performClick()
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
        composeRule.onNodeWithText("管理账号、订阅与隐私设置，查看剩余配额。").assertIsDisplayed()
        composeRule.onNodeWithText("订阅管理").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        waitForHomeRendered()
    }

    @Test
    fun backFromOverlayReturnsHome() {
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, AudioFilesTestTags.ROOT, AiFeatureTestTags.PAGE_AUDIO_FILES)

        goHome()
    }

    private fun goHome() {
        val overlayClicked = runCatching {
            composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_HOME, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (overlayClicked) {
            composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_HOME, useUnmergedTree = true).performClick()
        } else {
            composeRule.activityRule.scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
            }
        }
        waitForHomeRendered()
    }

    private fun forceDeviceDisconnected() {
        val impl = connectionManager
        val field = impl.javaClass.getDeclaredField("_state")
        field.isAccessible = true
        val flow = field.get(impl) as MutableStateFlow<ConnectionState>
        flow.value = ConnectionState.Disconnected
    }

    private fun waitForHomeRendered() {
        waitForAnyTag(
            composeRule,
            HomeScreenTestTags.ROOT,
            AiFeatureTestTags.PAGE_HOME,
            AiFeatureTestTags.OVERLAY_STACK,
            extraFallbackTags = arrayOf(AiFeatureTestTags.OVERLAY_HOME)
        )
    }

}
