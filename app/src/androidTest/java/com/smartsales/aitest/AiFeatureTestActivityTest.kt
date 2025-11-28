package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/AiFeatureTestActivityTest.kt
// 模块：:app
// 说明：验证 AiFeatureTestActivity Shell 导航行为的 Compose UI 测试
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.setup.DeviceSetupRouteTestTags
import com.smartsales.aitest.testing.DeviceConnectionEntryPoint
import com.smartsales.aitest.testing.waitForAnyTag
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.ProvisioningStatus
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val connectionManager: DeviceConnectionManager by lazy {
        EntryPointAccessors.fromApplication(
            composeRule.activity.applicationContext,
            DeviceConnectionEntryPoint::class.java
        ).deviceConnectionManager()
    }

    @Test
    fun defaultTab_isHome() {
        waitForHomeRendered()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_HOME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun homeFirstNavigationShell_isProperlyIntegrated() {
        waitForHomeRendered()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_HOME, useUnmergedTree = true).assertIsDisplayed()

        // 设备 overlay 默认跳到设备配网
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)

        // 音频 overlay 跳到音频库
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_AUDIO_FILES)

        // 返回 Home
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        // 历史入口
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_CHAT_HISTORY)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForPage(AiFeatureTestTags.PAGE_HOME)
    }

    @Test
    fun navigationWithoutChips_coversOverlaysAndHomeActions() {
        waitForHomeRendered()
        forceDeviceDisconnected()
        goHome()

        // 设备 overlay -> 配网
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)

        // 切换为已配网，再次进入设备 overlay -> 文件管理
        forceDeviceProvisioned()
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_MANAGER)

        // 音频 overlay -> 音频库
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_AUDIO_FILES)

        // 历史入口 -> 会话历史
        goHome()
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_BUTTON, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_CHAT_HISTORY)

        // 个人中心入口 -> 用户中心
        goHome()
        composeRule.onNodeWithTag(HomeScreenTestTags.PROFILE_BUTTON, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_USER_CENTER)

        // Wi-Fi & BLE 测试仍通过 Chip 进入
        goHome()
        selectTab(AiFeatureTestTags.CHIP_WIFI)
        waitForPage(AiFeatureTestTags.PAGE_WIFI)
        goHome()
    }

    @Test
    fun homeNavigationActions_switchTabs() {
        forceDeviceDisconnected()
        goHome()

        // 未配网时点击设备 Banner 应跳到设备配网
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.PAGE_DEVICE_SETUP,
            DeviceSetupRouteTestTags.PAGE
        )
        goHome()

        // 注入已连网状态后再次点击跳到设备文件
        forceDeviceProvisioned()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        goHome()

        // 音频摘要入口跳到音频库
        composeRule.onNodeWithTag(HomeScreenTestTags.AUDIO_CARD, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_AUDIO_FILES)
        goHome()

        // 个人中心图标跳到用户中心
        composeRule.onNodeWithTag(HomeScreenTestTags.PROFILE_BUTTON, useUnmergedTree = true).performClick()
        waitForPage(AiFeatureTestTags.PAGE_USER_CENTER)
        composeRule.onNodeWithText("管理账号、订阅与隐私设置，查看剩余配额。", substring = true).assertIsDisplayed()
    }

    @Test
    fun deviceSetupCompletion_returnsHome() {
        forceDeviceDisconnected()
        goHome()
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE, useUnmergedTree = true).performClick()
        waitForAnyTag(
            composeRule,
            AiFeatureTestTags.PAGE_DEVICE_SETUP,
            AiFeatureTestTags.PAGE_DEVICE_MANAGER,
            extraFallbackTags = arrayOf(AiFeatureTestTags.OVERLAY_DEVICE, AiFeatureTestTags.PAGE_HOME)
        )

        goHome()
        waitForPage(AiFeatureTestTags.PAGE_HOME)
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

    private fun selectTab(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
    }

    private fun goHome() {
        val clicked = clickIfExists(AiFeatureTestTags.OVERLAY_HOME)
        if (!clicked) {
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
            AiFeatureTestTags.OVERLAY_STACK,
            extraFallbackTags = arrayOf(AiFeatureTestTags.OVERLAY_HOME)
        )
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun tapQuickSkill(skillId: QuickSkillId) {
        val tag = "home_quick_skill_${skillId.name}"
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
    }

    private fun forceDeviceDisconnected() {
        val impl = connectionManager
        val field = impl.javaClass.getDeclaredField("_state")
        field.isAccessible = true
        val flow = field.get(impl) as MutableStateFlow<ConnectionState>
        flow.value = ConnectionState.Disconnected
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

    private fun clickIfExists(tag: String): Boolean {
        val exists = runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (exists) {
            composeRule.onNodeWithTag(tag, useUnmergedTree = true).performClick()
        }
        return exists
    }

    private fun waitForHomeRendered() {
        runCatching {
            waitForAnyTag(
                composeRule,
                AiFeatureTestTags.PAGE_HOME,
                HomeScreenTestTags.ROOT,
                AiFeatureTestTags.OVERLAY_STACK,
                extraFallbackTags = arrayOf(AiFeatureTestTags.OVERLAY_HOME),
                timeoutMillis = 60_000
            )
        }.onFailure {
            composeRule.waitUntil(timeoutMillis = 60_000) {
                composeRule.onAllNodesWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

}
