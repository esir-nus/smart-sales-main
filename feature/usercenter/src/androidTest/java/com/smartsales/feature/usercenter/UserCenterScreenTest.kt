package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/androidTest/java/com/smartsales/feature/usercenter/UserCenterScreenTest.kt
// 模块：:feature:usercenter
// 说明：验证用户中心界面的登录/访客布局与点击回调
// 作者：创建于 2025-11-30

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UserCenterScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loggedIn_showsProfileAndLogout() {
        setContent(
            state = UserCenterUiState(
                displayName = "测试用户",
                email = "tester@example.com",
                isGuest = false,
                canLogout = true
            )
        )

        composeRule.onNodeWithTag(UserCenterTestTags.HEADER_NAME, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UserCenterTestTags.HEADER_EMAIL, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UserCenterTestTags.ROW_LOGOUT, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun guest_showsLoginButtonAndHidesLogout() {
        setContent(
            state = UserCenterUiState(
                displayName = "",
                email = "",
                isGuest = true,
                canLogout = false
            )
        )

        composeRule.onNodeWithText("访客用户", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("请登录以管理账户", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UserCenterTestTags.BUTTON_LOGIN, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onAllNodesWithTag(UserCenterTestTags.ROW_LOGOUT, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun deviceManagerRow_triggersCallback() {
        var clicked = false
        setContent(
            state = UserCenterUiState(
                displayName = "测试用户",
                email = "tester@example.com",
                isGuest = false,
                canLogout = true
            ),
            onDeviceManagerClick = { clicked = true }
        )

        composeRule.onNodeWithTag(UserCenterTestTags.ROW_DEVICE_MANAGER, useUnmergedTree = true)
            .performClick()
        assertTrue(clicked)
    }

    private fun setContent(
        state: UserCenterUiState,
        onDeviceManagerClick: () -> Unit = {},
        onSubscriptionClick: () -> Unit = {},
        onPrivacyClick: () -> Unit = {},
        onGeneralClick: () -> Unit = {},
        onLoginClick: () -> Unit = {},
        onLogoutClick: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                UserCenterScreen(
                    uiState = state,
                    onDeviceManagerClick = onDeviceManagerClick,
                    onSubscriptionClick = onSubscriptionClick,
                    onPrivacyClick = onPrivacyClick,
                    onGeneralSettingsClick = onGeneralClick,
                    onLoginClick = onLoginClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}
