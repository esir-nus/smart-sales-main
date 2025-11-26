package com.smartsales.feature.chat.home

// 文件：feature/chat/src/androidTest/java/com/smartsales/feature/chat/home/HomeHistoryPanelUiTest.kt
// 模块：:feature:chat
// 说明：验证 Home 右侧历史抽屉的空态展示
// 作者：创建于 2025-11-26

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class HomeHistoryPanelUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptySessions_showsFriendlyMessage() {
        composeRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            MaterialTheme {
                HomeScreen(
                    state = HomeUiState(
                        chatMessages = emptyList(),
                        quickSkills = emptyList(),
                        inputText = "",
                        sessionList = emptyList()
                    ),
                    snackbarHostState = snackbarHostState,
                    onInputChanged = {},
                    onSendClicked = {},
                    onQuickSkillSelected = {},
                    onClearSelectedSkill = {},
                    onDeviceBannerClicked = {},
                    onAudioSummaryClicked = {},
                    onRefreshDeviceAndAudio = {},
                    onLoadMoreHistory = {},
                    onProfileClicked = {},
                    showHistoryPanel = true,
                    historySessions = emptyList()
                )
            }
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_EMPTY).assertIsDisplayed()
    }
}
