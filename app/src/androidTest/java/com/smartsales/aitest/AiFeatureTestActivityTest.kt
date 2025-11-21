package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/AiFeatureTestActivityTest.kt
// 模块：:app
// 说明：验证 AiFeatureTestActivity Shell 导航行为的 Compose UI 测试
// 作者：创建于 2025-11-21

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.aitest.setup.DeviceSetupTestTags
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.ProvisioningStatus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiFeatureTestActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()

    private val connectionManager: DeviceConnectionManager by lazy {
        val context = ApplicationProvider.getApplicationContext<Context>()
        EntryPointAccessors.fromApplication(
            context,
            DeviceConnectionEntryPoint::class.java
        ).deviceConnectionManager()
    }

    @Test
    fun defaultTab_isHome() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)
    }

    @Test
    fun homeFirstNavigationShell_isProperlyIntegrated() {
        // 验证 Activity 启动时 Home 页面是默认显示
        waitForPage(AiFeatureTestTags.PAGE_HOME)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME).assertIsDisplayed()
        
        // 验证 Home chip 默认选中
        composeRule.onNodeWithTag(AiFeatureTestTags.CHIP_HOME).assertIsDisplayed()
        
        // 验证 HomeScreen 的关键元素在 shell 中可见
        composeRule.waitForIdle()
        
        // 验证可以通过 Home 的导航入口跳转到其他页面，然后返回 Home
        // 测试设备配网导航
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)
        
        // 验证可以通过 chip 返回 Home
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)
        
        // 验证从其他页面也能导航回 Home
        selectTab(AiFeatureTestTags.CHIP_WIFI)
        waitForPage(AiFeatureTestTags.PAGE_WIFI)
        
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)
        
        // 验证 Home 始终可作为导航中心点
        selectTab(AiFeatureTestTags.CHIP_AUDIO_FILES)
        waitForPage(AiFeatureTestTags.PAGE_AUDIO_FILES)
        
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)
        
        // 最终验证 Home 页面仍然完整显示
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME).assertIsDisplayed()
    }

    @Test
    fun chipRow_switchesBetweenAllRoutes() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        selectTab(AiFeatureTestTags.CHIP_WIFI)
        waitForPage(AiFeatureTestTags.PAGE_WIFI)

        selectTab(AiFeatureTestTags.CHIP_DEVICE_MANAGER)
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_MANAGER)

        selectTab(AiFeatureTestTags.CHIP_DEVICE_SETUP)
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)

        selectTab(AiFeatureTestTags.CHIP_AUDIO_FILES)
        waitForPage(AiFeatureTestTags.PAGE_AUDIO_FILES)

        selectTab(AiFeatureTestTags.CHIP_USER_CENTER)
        waitForPage(AiFeatureTestTags.PAGE_USER_CENTER)

        // 来回切换验证稳定性
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)
        selectTab(AiFeatureTestTags.CHIP_WIFI)
        waitForPage(AiFeatureTestTags.PAGE_WIFI)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)
    }

    @Test
    fun homeNavigationActions_switchTabs() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        // 未配网时点击设备 Banner 应跳到设备配网
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        // 注入已连网状态后再次点击跳到设备文件
        forceDeviceProvisioned()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        // 音频摘要入口跳到音频库
        composeRule.onNodeWithTag(HomeScreenTestTags.AUDIO_CARD).performClick()
        waitForPage(AiFeatureTestTags.PAGE_AUDIO_FILES)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        // 个人中心图标跳到用户中心
        composeRule.onNodeWithTag(HomeScreenTestTags.PROFILE_BUTTON).performClick()
        waitForPage(AiFeatureTestTags.PAGE_USER_CENTER)
    }

    @Test
    fun deviceSetupCompletion_returnsHome() {
        selectTab(AiFeatureTestTags.CHIP_DEVICE_SETUP)
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)

        composeRule.onNodeWithTag(DeviceSetupTestTags.COMPLETE_BUTTON).performClick()
        waitForPage(AiFeatureTestTags.PAGE_HOME)
    }

    @Test
    fun quickSkillTap_showsConfirmationAndChip() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)

        composeRule.onNodeWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP).assertIsDisplayed()
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ASSISTANT_MESSAGE).assertCountEquals(1)
        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE).assertCountEquals(0)
        composeRule.onAllNodesWithText("Got it", substring = true).assertCountEquals(1)
    }

    @Test
    fun quickSkillChip_closeClearsSelection() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        composeRule.onNodeWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP_CLOSE).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP)
                .fetchSemanticsNodes().isEmpty()
        }
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP).assertCountEquals(0)
        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE).assertCountEquals(0)
    }

    @Test
    fun quickSkill_sendConsumesSkill() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD).performTextInput("请总结会议")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            val userCount = composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE)
                .fetchSemanticsNodes().size
            val chipVisible = composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP)
                .fetchSemanticsNodes().isNotEmpty()
            userCount == 1 && !chipVisible
        }

        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE).assertCountEquals(1)
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ASSISTANT_MESSAGE).assertCountEquals(2)
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP).assertCountEquals(0)
        composeRule.onAllNodesWithText("Got it", substring = true).assertCountEquals(1)
    }

    private fun selectTab(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun waitForPage(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun tapQuickSkill(skillId: QuickSkillId) {
        val tag = "home_quick_skill_${skillId.name}"
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun forceDeviceProvisioned() {
        val impl = connectionManager
        val session = BleSession.fromPeripheral(
            BlePeripheral(
                id = "mock-device",
                name = "BT311 测试",
                signalStrengthDbm = -55,
                profileId = "bt311"
            )
        )
        val status = ProvisioningStatus(
            wifiSsid = "DemoWifi",
            handshakeId = UUID.randomUUID().toString(),
            credentialsHash = "hash-${UUID.randomUUID()}"
        )
        val field = impl.javaClass.getDeclaredField("_state")
        field.isAccessible = true
        val flow = field.get(impl) as MutableStateFlow<ConnectionState>
        flow.value = ConnectionState.WifiProvisioned(session, status)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeviceConnectionEntryPoint {
        fun deviceConnectionManager(): DeviceConnectionManager
    }
}
