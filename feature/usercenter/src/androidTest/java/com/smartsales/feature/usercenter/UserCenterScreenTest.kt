package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/androidTest/java/com/smartsales/feature/usercenter/UserCenterScreenTest.kt
// 模块：:feature:usercenter
// 说明：验证用户中心界面的登录/访客布局与点击回调
// 作者：创建于 2025-11-30

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UserCenterScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deviceManagerRow_triggersCallback() {
        var clicked = 0
        setContent(
            state = UserCenterUiState(
                displayName = "测试用户",
                role = "销售",
                industry = "汽车",
                isGuest = false
            ),
            onDeviceManagerClick = { clicked += 1 }
        )

        composeRule.onNodeWithTag(UserCenterTestTags.ROW_DEVICE_MANAGER, useUnmergedTree = true)
            .performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun fields_render_and_saveButtonVisible() {
        setContent(
            state = UserCenterUiState(
                displayName = "李雷",
                role = "销售经理",
                industry = "制造业",
                isGuest = false
            )
        )

        composeRule.onNodeWithTag(UserCenterTestTags.FIELD_NAME, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UserCenterTestTags.FIELD_ROLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UserCenterTestTags.FIELD_INDUSTRY, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(UserCenterTestTags.BUTTON_SAVE, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    private fun setContent(
        state: UserCenterUiState,
        onDeviceManagerClick: () -> Unit = {},
        onPrivacyClick: () -> Unit = {},
        onDisplayNameChange: (String) -> Unit = {},
        onRoleChange: (String) -> Unit = {},
        onIndustryChange: (String) -> Unit = {},
        onSave: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                UserCenterScreen(
                    uiState = state,
                    onDeviceManagerClick = onDeviceManagerClick,
                    onPrivacyClick = onPrivacyClick,
                    onDisplayNameChange = onDisplayNameChange,
                    onRoleChange = onRoleChange,
                    onIndustryChange = onIndustryChange,
                    onSave = onSave
                )
            }
        }
    }
}
