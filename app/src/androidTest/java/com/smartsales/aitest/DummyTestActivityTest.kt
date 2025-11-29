package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/DummyTestActivityTest.kt
// 模块：:app
// 说明：验证 DummyTestActivity 能正常渲染 Compose 树，排查 instrumentation 环境
// 作者：创建于 2025-11-29

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DummyTestActivityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<DummyTestActivity>()

    @Test
    fun dummy_root_is_displayed() {
        composeRule.onNodeWithTag("DUMMY_ROOT").assertIsDisplayed()
    }
}
