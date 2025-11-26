package com.smartsales.feature.chat.home

// 文件：feature/chat/src/androidTest/java/com/smartsales/feature/chat/home/HomeEntryBannersUiTest.kt
// 模块：:feature:chat
// 说明：验证 Home 顶部入口卡片点击行为与展示
// 作者：创建于 2025-11-26

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.AudioSummaryUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeScreen
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeEntryBannersUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun banners_triggerCallbacks() {
        var deviceClicks = 0
        var audioClicks = 0
        val snackbarHostState = SnackbarHostState()

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    HomeUiState(
                        chatMessages = emptyList(),
                        quickSkills = emptyList(),
                        inputText = "",
                        deviceSnapshot = DeviceSnapshotUi(
                            deviceName = "测试设备",
                            statusText = "设备已连接",
                            connectionState = DeviceConnectionStateUi.CONNECTED
                        ),
                        audioSummary = AudioSummaryUi(headline = "最近录音 3 条")
                    )
                )
            }
            MaterialTheme {
                HomeScreen(
                    state = uiState,
                    snackbarHostState = snackbarHostState,
                    onInputChanged = {},
                    onSendClicked = {},
                    onQuickSkillSelected = {},
                    onClearSelectedSkill = {},
                    onDeviceBannerClicked = { deviceClicks++ },
                    onAudioSummaryClicked = { audioClicks++ },
                    onRefreshDeviceAndAudio = {},
                    onLoadMoreHistory = {},
                    onProfileClicked = {},
                )
            }
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(HomeScreenTestTags.AUDIO_ENTRY).assertIsDisplayed().performClick()

        assertEquals(1, deviceClicks)
        assertEquals(1, audioClicks)
    }
}
