package com.smartsales.aitest.home

// 文件：app/src/androidTest/java/com/smartsales/aitest/home/HomeAssistantCopyTest.kt
// 模块：:app
// 说明：验证 Home 助手消息复制按钮可见且可触发提示
// 作者：创建于 2025-11-21

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.HomeScreen
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.HomeUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAssistantCopyTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun assistantMessage_copyButtonCopiesAndShowsSnackbar() {
        val assistantMessage = ChatMessageUi(
            id = "copy-test-1",
            role = ChatMessageRole.ASSISTANT,
            content = "复制内容",
            timestampMillis = System.currentTimeMillis()
        )
        val state = HomeUiState(chatMessages = listOf(assistantMessage))

        composeRule.setContent {
            HomeScreen(
                state = state,
                snackbarHostState = SnackbarHostState(),
                onInputChanged = {},
                onSendClicked = {},
                onQuickSkillSelected = {},
                onDeviceBannerClicked = {},
                onAudioSummaryClicked = {},
                onRefreshDeviceAndAudio = {},
                onLoadMoreHistory = {},
                onProfileClicked = {},
                onNewChatClicked = {},
                onSessionSelected = {},
                modifier = Modifier,
                showHistoryPanel = false,
                onToggleHistoryPanel = {},
                onDismissHistoryPanel = {},
                historySessions = emptyList(),
                onHistorySessionSelected = {}
            )
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.ASSISTANT_MESSAGE, useUnmergedTree = true)
            .assertIsDisplayed()

        val copyTag = "${HomeScreenTestTags.ASSISTANT_COPY_PREFIX}${assistantMessage.id}"
        composeRule.onNodeWithTag(copyTag, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("已复制", useUnmergedTree = true).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(2_100)
        composeRule.onNodeWithText("复制", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("已复制到剪贴板", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
