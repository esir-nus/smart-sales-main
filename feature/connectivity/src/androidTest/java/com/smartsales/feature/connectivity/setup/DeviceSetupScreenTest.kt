package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/androidTest/java/com/smartsales/feature/connectivity/setup/DeviceSetupScreenTest.kt
// 模块：:feature:connectivity
// 说明：验证 DeviceSetupScreen 的步骤文案、按钮与错误提示
// 作者：创建于 2025-11-30

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DeviceSetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyStep_showsActions() {
        composeRule.setContent {
            MaterialTheme {
                DeviceSetupScreen(
                    state = DeviceSetupUiState(
                        step = DeviceSetupStep.Ready,
                        description = "设备已连接，可前往设备管理。",
                        primaryLabel = "进入设备管理",
                        secondaryLabel = "返回首页",
                        isPrimaryEnabled = true
                    ),
                    onStartScan = {},
                    onProvisionWifi = { _, _ -> },
                    onRetry = {},
                    onOpenDeviceManager = {},
                    onDismissError = {},
                    onBackToHome = {},
                    onWifiSsidChanged = {},
                    onWifiPasswordChanged = {}
                )
            }
        }

        composeRule.onNodeWithTag(DeviceSetupTestTags.TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(DeviceSetupTestTags.PRIMARY_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(DeviceSetupTestTags.SECONDARY_BUTTON).assertIsDisplayed()
    }

    @Test
    fun errorBanner_visible() {
        composeRule.setContent {
            MaterialTheme {
                DeviceSetupScreen(
                    state = DeviceSetupUiState(
                        step = DeviceSetupStep.Error,
                        description = "出错",
                        primaryLabel = "重试配网",
                        errorMessage = "失败",
                        secondaryLabel = "返回首页",
                        isPrimaryEnabled = true
                    ),
                    onStartScan = {},
                    onProvisionWifi = { _, _ -> },
                    onRetry = {},
                    onOpenDeviceManager = {},
                    onDismissError = {},
                    onBackToHome = {},
                    onWifiSsidChanged = {},
                    onWifiPasswordChanged = {}
                )
            }
        }

        composeRule.onNodeWithTag(DeviceSetupTestTags.ERROR_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("失败").assertIsDisplayed()
    }

    @Test
    fun pairing_showsWifiForm() {
        composeRule.setContent {
            MaterialTheme {
                DeviceSetupScreen(
                    state = DeviceSetupUiState(
                        step = DeviceSetupStep.Pairing,
                        description = "填写 Wi-Fi 完成配网。",
                        primaryLabel = "下发 Wi-Fi 并继续",
                        secondaryLabel = "返回首页",
                        showWifiForm = true,
                        isPrimaryEnabled = false
                    ),
                    onStartScan = {},
                    onProvisionWifi = { _, _ -> },
                    onRetry = {},
                    onOpenDeviceManager = {},
                    onDismissError = {},
                    onBackToHome = {},
                    onWifiSsidChanged = {},
                    onWifiPasswordChanged = {}
                )
            }
        }

        composeRule.onNodeWithTag(DeviceSetupTestTags.WIFI_SSID).assertIsDisplayed()
        composeRule.onNodeWithTag(DeviceSetupTestTags.WIFI_PASSWORD).assertIsDisplayed()
    }
}
