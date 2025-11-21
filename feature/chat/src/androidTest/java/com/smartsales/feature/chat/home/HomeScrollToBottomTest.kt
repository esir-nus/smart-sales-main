package com.smartsales.feature.chat.home

// 文件：feature/chat/src/androidTest/java/com/smartsales/feature/chat/home/HomeScrollToBottomTest.kt
// 模块：:feature:chat
// 说明：验证 Home 层“回到底部”按钮与设备横幅的显示逻辑
// 作者：创建于 2025-11-21
// 覆盖点：
// - 底部时按钮隐藏，上滑后出现，点击后滚动并再次隐藏
// - 横幅在长列表滚动时仍可见
// - 消息不足或为空时按钮不出现，横幅依旧显示
// 测试数据假设：构造 2~35 条消息，内容简单但能触发 LazyColumn 滚动和 Semantics 滚动动作

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScrollToBottomTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun scrollButton_hiddenWhenLatestVisible() {
        setHomeContent(messageCount = 30)

        scrollToText("消息 29")

        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertDoesNotExist()
    }

    @Test
    fun scrollButton_showsAfterScrollingAwayFromLatest() {
        setHomeContent(messageCount = 30)
        scrollToText("消息 29")

        scrollToText("消息 5")

        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertIsDisplayed()
    }

    @Test
    fun deviceBanner_staysVisibleWhileScrollingLongList() {
        setHomeContent(messageCount = 30)
        scrollToText("消息 29")

        scrollToText("消息 5")

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertIsDisplayed()
    }

    @Test
    fun scrollButton_clickReturnsToBottomAndHides() {
        setHomeContent(messageCount = 35)
        scrollToText("消息 34")
        scrollToText("消息 4")

        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !composeRule.onAllNodesWithTag(HomeScreenTestTags.SCROLL_TO_LATEST)
                .fetchSemanticsNodes().any()
        }
        composeRule.onNodeWithText("消息 34").assertIsDisplayed()
    }

    @Test
    fun scrollButton_neverAppearsForShortList() {
        setHomeContent(messageCount = 2)

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertDoesNotExist()
    }

    @Test
    fun deviceBanner_visibleWhenListEmpty() {
        setHomeContent(messageCount = 0)

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertDoesNotExist()
    }

    private fun setHomeContent(messageCount: Int) {
        val messages = (0 until messageCount).map { index ->
            ChatMessageUi(
                id = "message-$index",
                role = if (index % 2 == 0) ChatMessageRole.USER else ChatMessageRole.ASSISTANT,
                content = "消息 $index",
                timestampMillis = index.toLong()
            )
        }
        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    HomeUiState(
                        chatMessages = messages,
                        quickSkills = emptyList(),
                        inputText = ""
                    )
                )
            }
            val snackbarHostState = remember { SnackbarHostState() }
            MaterialTheme {
                HomeScreen(
                    state = uiState,
                    snackbarHostState = snackbarHostState,
                    onInputChanged = {},
                    onSendClicked = {},
                    onQuickSkillSelected = { _ -> },
                    onClearSelectedSkill = {},
                    onDeviceBannerClicked = {},
                    onAudioSummaryClicked = {},
                    onRefreshDeviceAndAudio = {},
                    onLoadMoreHistory = {},
                    onProfileClicked = {},
                )
            }
        }
    }

    private fun scrollToText(text: String) {
        composeRule.onNodeWithText(text, substring = false, useUnmergedTree = true)
            .performScrollTo()
    }
}
