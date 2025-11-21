package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/androidTest/java/com/smartsales/feature/connectivity/setup/DeviceSetupScreenTest.kt
// 模块：:feature:connectivity
// 说明：验证 DeviceSetupScreen 的步骤文案、按钮与错误提示
// 作者：创建于 2025-11-21

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
                        progressMessage = "设备已上线"
                    ),
                    onStartScan = {},
                    onProvisionWifi = { _, _ -> },
                    onRetry = {},
                    onOpenDeviceManager = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithTag(DeviceSetupTestTags.STEP_TEXT).assertIsDisplayed()
        composeRule.onNodeWithTag(DeviceSetupTestTags.PRIMARY_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(DeviceSetupTestTags.TO_DEVICE_MANAGER).assertIsDisplayed()
    }

    @Test
    fun errorBanner_visibleAndDismissible() {
        composeRule.setContent {
            MaterialTheme {
                DeviceSetupScreen(
                    state = DeviceSetupUiState(
                        step = DeviceSetupStep.Error,
                        progressMessage = "出错",
                        errorMessage = "失败"
                    ),
                    onStartScan = {},
                    onProvisionWifi = { _, _ -> },
                    onRetry = {},
                    onOpenDeviceManager = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithTag(DeviceSetupTestTags.ERROR_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("失败").assertIsDisplayed()
    }
}
