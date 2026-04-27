package com.smartsales.prism.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartsales.prism.domain.connectivity.IsolationTriggerContext
import com.smartsales.prism.ui.components.connectivity.WifiRepairState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WifiMismatchViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun blankSubmit_showsValidationAndDoesNotOpenConfirmDialog() {
        val updates = mutableListOf<Pair<String, String>>()

        composeTestRule.setContent {
            MaterialTheme {
                WifiMismatchView(
                    suggestedSsid = null,
                    errorMessage = null,
                    onUpdate = { ssid, password -> updates += ssid to password },
                    onInputChanged = {},
                    onIgnore = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("wifi_mismatch_password_input")
            .performTextInput("secret")
        composeTestRule.onNodeWithText("更新配置").performClick()

        composeTestRule.onNodeWithTag("wifi_mismatch_error").assertExists()
        composeTestRule.onNodeWithTag("wifi_mismatch_confirm_dialog").assertDoesNotExist()
        assertEquals(emptyList<Pair<String, String>>(), updates)
    }

    @Test
    fun validSubmit_opensConfirmDialogAndCancelKeepsFormUnsent() {
        val updates = mutableListOf<Pair<String, String>>()

        composeTestRule.setContent {
            MaterialTheme {
                WifiMismatchView(
                    suggestedSsid = null,
                    errorMessage = null,
                    onUpdate = { ssid, password -> updates += ssid to password },
                    onInputChanged = {},
                    onIgnore = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("wifi_mismatch_ssid_input")
            .performTextInput("OfficeGuest")
        composeTestRule.onNodeWithTag("wifi_mismatch_password_input")
            .performTextInput("PlainSecret123")
        composeTestRule.onNodeWithText("更新配置").performClick()

        composeTestRule.onNodeWithTag("wifi_mismatch_confirm_dialog").assertExists()
        composeTestRule.onNodeWithTag("wifi_mismatch_confirm_ssid").assertExists()
        composeTestRule.onNodeWithText("密码：已隐藏").assertExists()
        composeTestRule.onNodeWithTag("wifi_mismatch_confirm_cancel_button").performClick()

        composeTestRule.onNodeWithTag("wifi_mismatch_confirm_dialog").assertDoesNotExist()
        assertEquals(emptyList<Pair<String, String>>(), updates)
    }

    @Test
    fun confirmSendsTrimmedCredentialsExactlyOnce() {
        val updates = mutableListOf<Pair<String, String>>()

        composeTestRule.setContent {
            MaterialTheme {
                WifiMismatchView(
                    suggestedSsid = null,
                    errorMessage = null,
                    onUpdate = { ssid, password -> updates += ssid to password },
                    onInputChanged = {},
                    onIgnore = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("wifi_mismatch_ssid_input")
            .performTextInput("  OfficeGuest  ")
        composeTestRule.onNodeWithTag("wifi_mismatch_password_input")
            .performTextInput("  secret123  ")
        composeTestRule.onNodeWithText("更新配置").performClick()
        composeTestRule.onNodeWithTag("wifi_mismatch_confirm_button").performClick()

        assertEquals(listOf("OfficeGuest" to "secret123"), updates)
    }

    @Test
    fun editingFields_clearsPreviouslyShownServiceError() {
        composeTestRule.setContent {
            MaterialTheme {
                val errorState = remember { mutableStateOf<String?>("服务端错误") }
                WifiMismatchView(
                    suggestedSsid = "OfficeGuest",
                    errorMessage = errorState.value,
                    onUpdate = { _, _ -> },
                    onInputChanged = { errorState.value = null },
                    onIgnore = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("wifi_mismatch_error").assertExists()
        composeTestRule.onNodeWithTag("wifi_mismatch_password_input").performTextInput("secret")
        composeTestRule.onNodeWithTag("wifi_mismatch_error").assertDoesNotExist()
    }

    @Test
    fun isolationWifiRepair_invokesDedicatedWifiRepairCallback() {
        var wifiRepairClicks = 0
        var ignoreClicks = 0

        composeTestRule.setContent {
            MaterialTheme {
                WifiMismatchView(
                    repairState = WifiRepairState.HardFailure(
                        WifiRepairState.HardFailure.HardFailureReason.SUSPECTED_ISOLATION
                    ),
                    suggestedSsid = null,
                    errorMessage = null,
                    onUpdate = { _, _ -> },
                    onInputChanged = {},
                    onIgnore = { ignoreClicks++ },
                    onStartWifiRepair = { wifiRepairClicks++ },
                    isolationBadgeIp = "192.168.1.18",
                    isolationTriggerContext = IsolationTriggerContext.ON_CONNECT
                )
            }
        }

        composeTestRule.onNodeWithText("修复 Wi-Fi 配置").performClick()

        assertEquals(1, wifiRepairClicks)
        assertEquals(0, ignoreClicks)
    }
}
