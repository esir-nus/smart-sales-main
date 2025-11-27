package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/AiFeatureTestActivityTest.kt
// 模块：:app
// 说明：验证 AiFeatureTestActivity Shell 导航行为的 Compose UI 测试
// 作者：创建于 2025-11-21

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.di.DeviceConnectionEntryPoint
import com.smartsales.aitest.setup.DeviceSetupRouteTestTags
import com.smartsales.aitest.ui.HomeOverlayTestTags
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.connectivity.BlePeripheral
import com.smartsales.feature.connectivity.BleSession
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.connectivity.DefaultDeviceConnectionManager
import com.smartsales.feature.connectivity.ProvisioningStatus
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import org.junit.Rule
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class AiFeatureTestActivityTest {
    // Q1：waitForHomeShell 仅等待 AiFeatureTestTags.OVERLAY_SHELL（Home shell 根标签）
    // Q2：homeFirstNavigationShell_isProperlyIntegrated 也断言同一根标签，保持一致
    // 说明：createAndroidComposeRule 会自动启动 AiFeatureTestActivity 并注册 Compose 树，
    // 错配规则会导致 “No compose hierarchies found in the app”。

    // 预授予 BLE/定位权限，避免系统弹窗影响 Compose 测试
    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()

    @Test
    fun defaultTab_isHome() {
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER, timeout = 15_000)
    }

    @Test
    @Ignore("TODO: overlay shell readiness; re-enable after adding explicit waits")
    fun homeFirstNavigationShell_isProperlyIntegrated() {
        // 先等待 Compose 树稳定，避免 “No compose hierarchies found” 直接抛错
        composeRule.waitForIdle()
        waitForHomeShell()
        // 调试：打印未合并语义树，便于确认 Home shell 标签是否存在（稳定后可移除）
        composeRule.onRoot(useUnmergedTree = true).printToLog("HomeShellTree")
        // 调试校验：根标签应唯一存在，用于区分标签缺失与等待超时
        composeRule.onAllNodesWithTag(
            AiFeatureTestTags.OVERLAY_SHELL,
            useUnmergedTree = true
        ).assertCountEquals(1)
        waitForHomeShell()
        dumpTree("HomeShellTree")

        // 验证 Activity 启动时 Home 页面是默认显示
        // 测试根标签：Home shell 外层容器
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_SHELL, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME).assertIsDisplayed()

        // 验证 Home chip 默认选中
        composeRule.onNodeWithTag(AiFeatureTestTags.CHIP_HOME).assertIsDisplayed()

        // 验证可以通过 Home 的导航入口跳转到其他页面，然后返回 Home
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)

        // 验证可以通过 chip 返回 Home
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        // 验证从其他页面也能导航回 Home
        selectTab(AiFeatureTestTags.CHIP_WIFI)
        waitForPage(AiFeatureTestTags.PAGE_WIFI)

        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        // 最终验证 Home 页面仍然完整显示
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)
    }

    @Test
    @Ignore("TODO: overlay shell readiness; re-enable after adding explicit waits")
    fun chipRow_switchesBetweenAllRoutes() {
        waitForHomeShell()
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        selectTab(AiFeatureTestTags.CHIP_CHAT_HISTORY)
        waitForPage(AiFeatureTestTags.PAGE_CHAT_HISTORY)

        selectTab(AiFeatureTestTags.CHIP_USER_CENTER)
        waitForPage(AiFeatureTestTags.PAGE_USER_CENTER)

        // 来回切换验证稳定性
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)
    }

    @Test
    @Ignore("TODO: overlay shell readiness; re-enable after adding explicit waits")
    fun homeNavigationActions_switchTabs() {
        waitForHomeShell()
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        // 未配网时点击设备 Banner 应跳到设备配网
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).performClick()
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        // 注入已连网状态后再次点击跳到设备文件
        forceDeviceProvisioned()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).performClick()
        waitForOverlay(HomeOverlayTestTags.DEVICE_LAYER)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        // 音频摘要入口跳到音频库
        composeRule.onNodeWithTag(HomeScreenTestTags.AUDIO_ENTRY).performClick()
        waitForOverlay(HomeOverlayTestTags.AUDIO_LAYER)
        selectTab(AiFeatureTestTags.CHIP_HOME)
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        // 个人中心图标跳到用户中心
        composeRule.onNodeWithTag(HomeScreenTestTags.PROFILE_BUTTON).performClick()
        waitForPage(AiFeatureTestTags.PAGE_USER_CENTER)
    }

    @Test
    @Ignore("TODO: device setup navigation timing; re-enable after adding explicit waits")
    fun deviceSetupCompletion_returnsHome() {
        waitForHomeShell()
        selectTab(AiFeatureTestTags.CHIP_DEVICE_SETUP)
        waitForPage(AiFeatureTestTags.PAGE_DEVICE_SETUP)

        composeRule.onNodeWithTag(DeviceSetupRouteTestTags.COMPLETE_BUTTON).performClick()
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)
    }

    @Test
    @Ignore("TODO: quick skill timing; re-enable after ensuring input is enabled and waits added")
    fun quickSkillTap_showsConfirmationAndChip() {
        waitForHomeShell()
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)

        composeRule.onNodeWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP).assertIsDisplayed()
        waitForAssistantMessages()
        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE).assertCountEquals(0)
        composeRule.onAllNodesWithText("Got it", substring = true).assertCountEquals(1)
    }

    @Test
    fun quickSkillChip_closeClearsSelection() {
        waitForHomeShell()
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        composeRule.onNodeWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP_CLOSE).performClick()

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP).assertCountEquals(0)
        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE).assertCountEquals(0)
    }

    @Test
    @Ignore("TODO: quick skill send flow; re-enable after ensuring input is enabled and waits added")
    fun quickSkill_sendConsumesSkill() {
        waitForHomeShell()
        waitForOverlay(HomeOverlayTestTags.HOME_LAYER)

        tapQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD).performTextInput("请总结会议")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ASSISTANT_MESSAGE, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val userCount = composeRule.onAllNodesWithTag(
            HomeScreenTestTags.USER_MESSAGE,
            useUnmergedTree = true
        ).fetchSemanticsNodes().size
        val assistantCount = composeRule.onAllNodesWithTag(
            HomeScreenTestTags.ASSISTANT_MESSAGE,
            useUnmergedTree = true
        ).fetchSemanticsNodes().size
        assertTrue("应至少有一条用户消息", userCount >= 1)
        assertTrue("应至少有一条助手消息", assistantCount >= 1)
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP).assertCountEquals(0)
        composeRule.onAllNodesWithText("Got it", substring = true).assertCountEquals(1)
    }

    private fun selectTab(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun waitForPage(tag: String) {
        val mappedOverlay = when (tag) {
            AiFeatureTestTags.PAGE_HOME -> HomeOverlayTestTags.HOME_LAYER
            AiFeatureTestTags.PAGE_AUDIO_FILES -> HomeOverlayTestTags.AUDIO_LAYER
            AiFeatureTestTags.PAGE_DEVICE_MANAGER -> HomeOverlayTestTags.DEVICE_LAYER
            else -> null
        }
        try {
            mappedOverlay?.let {
                composeRule
                    .onNodeWithTag(it, useUnmergedTree = true)
                    .assertIsDisplayed()
            }
            composeRule
                .onNodeWithTag(tag)
                .assertIsDisplayed()
        } catch (e: AssertionError) {
            composeRule.onRoot(useUnmergedTree = true)
                .printToLog("PageWait_debug_${mappedOverlay ?: "none"}_$tag")
            println("PageWait_debug_${mappedOverlay ?: "none"}_$tag: page not displayed")
            throw e
        }
    }

    private fun tapQuickSkill(skillId: QuickSkillId) {
        val tag = "home_quick_skill_${skillId.name}"
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun waitForHomeShell() {
        // Home shell 相关 tag 仅在未合并语义树可见（内层叠层与单元测试一致）
        composeRule.waitForIdle()
        // 测试：统一用 OVERLAY_SHELL 作为 Home shell 根标识（Activity 层打标，期望唯一）
        composeRule.onAllNodesWithTag(
            AiFeatureTestTags.OVERLAY_SHELL,
            useUnmergedTree = true
        ).assertCountEquals(1)
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_SHELL, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private fun forceDeviceProvisioned() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            DeviceConnectionEntryPoint::class.java
        )
        val impl: DefaultDeviceConnectionManager = entryPoint.connectionManager()
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
        impl.overrideStateForTest(ConnectionState.WifiProvisioned(session, status))
    }

    private fun waitForAssistantMessages() {
        composeRule.waitForIdle()
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithTag(
                    HomeScreenTestTags.ASSISTANT_MESSAGE,
                    useUnmergedTree = true
                ).fetchSemanticsNodes().isNotEmpty()
            }
            val assistantCount = composeRule.onAllNodesWithTag(
                HomeScreenTestTags.ASSISTANT_MESSAGE,
                useUnmergedTree = true
            ).fetchSemanticsNodes().size
            assertTrue("应至少有一条助手消息", assistantCount >= 1)
        } catch (e: AssertionError) {
            // 调试：助手消息缺失时打印语义树
            composeRule.onRoot(useUnmergedTree = true).printToLog("AssistantWait_debug")
            println("AssistantWait_debug: expected assistant message tag=${HomeScreenTestTags.ASSISTANT_MESSAGE}")
            throw e
        }
    }

    private fun waitForOverlay(tag: String, timeout: Long = 10_000) {
        composeRule.waitUntil(timeoutMillis = timeout) {
            runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        try {
            composeRule
                .onNodeWithTag(tag, useUnmergedTree = true)
                .assertIsDisplayed()
        } catch (e: AssertionError) {
            // 调试用：overlay 不可见时 dump 语义树
            composeRule.onRoot(useUnmergedTree = true)
                .printToLog("PageWait_debug_$tag")
            println("PageWait_debug_$tag: overlay tag not displayed: $tag")
            throw e
        }
    }

    private fun dumpTree(label: String) {
        composeRule.onRoot().printToLog(label)
    }
}
