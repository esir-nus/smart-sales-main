package com.smartsales.aitest.ui

// 文件：app/src/test/java/com/smartsales/aitest/ui/HomeOverlayShellTest.kt
// 模块：:app
// 说明：验证 HomeOverlayShell 的层级切换显示状态
// 作者：创建于 2025-11-26

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class HomeOverlayShellTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Before
    fun setup() {}

    @Test
    fun showsCorrespondingLayer_whenPageChanges() {
        val page = mutableStateOf(OverlayPage.Home)
        composeRule.setContent {
            HomeOverlayShell(
                currentPage = page.value,
                onPageChange = { page.value = it },
                homeContent = {
                    Box(Modifier.fillMaxSize()) { Text("home") }
                },
                audioContent = {
                    Box(Modifier.fillMaxSize()) { Text("audio") }
                },
                deviceManagerContent = {
                    Box(Modifier.fillMaxSize()) { Text("device") }
                }
            )
        }

        composeRule.onNodeWithTag(HomeOverlayTestTags.HOME_LAYER, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("home", useUnmergedTree = true).assertIsDisplayed()

        composeRule.runOnUiThread { page.value = OverlayPage.Audio }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeOverlayTestTags.AUDIO_LAYER, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("audio", useUnmergedTree = true).assertIsDisplayed()

        composeRule.runOnUiThread { page.value = OverlayPage.Device }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeOverlayTestTags.DEVICE_LAYER, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("device", useUnmergedTree = true).assertIsDisplayed()
    }
}
