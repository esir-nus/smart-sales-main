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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
        setHomeContent(messageCount = 40)

        scrollToBottom(messageCount = 40)
        waitForScrollButtonHidden()
    }

    @Test
    fun scrollButton_showsAfterScrollingAwayFromLatest() {
        setHomeContent(messageCount = 40)
        scrollToBottom(messageCount = 40)
        waitForScrollButtonHidden()

        scrollToIndex(5)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    @Test
    fun deviceBanner_staysVisibleWhileScrollingLongList() {
        setHomeContent(messageCount = 40)
        scrollToBottom(messageCount = 40)
        waitForScrollButtonHidden()

        scrollToIndex(5)

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertIsDisplayed()
    }

    @Test
    fun scrollButton_clickReturnsToBottomAndHides() {
        setHomeContent(messageCount = 50)
        scrollToBottom(messageCount = 50)
        scrollToIndex(4)

        composeRule.onNodeWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).performClick()

        waitForScrollButtonHidden()
        composeRule.onNodeWithText("消息 49").assertIsDisplayed()
    }

    @Test
    fun scrollButton_neverAppearsForShortList() {
        setHomeContent(messageCount = 2)

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).assertIsDisplayed()
        composeRule.onAllNodesWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertCountEquals(0)
    }

    @Test
    fun deviceBanner_visibleWhenListEmpty() {
        setHomeContent(messageCount = 0)

        composeRule.onNodeWithTag(HomeScreenTestTags.DEVICE_ENTRY).assertIsDisplayed()
        composeRule.onAllNodesWithTag(HomeScreenTestTags.SCROLL_TO_LATEST).assertCountEquals(0)
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

    private fun scrollToBottom(messageCount: Int) {
        val targetIndex = messageCount + 1
        scrollToIndex(targetIndex)
        composeRule.waitForIdle()
    }

    private fun scrollToIndex(index: Int) {
        composeRule.onAllNodesWithTag(HomeScreenTestTags.LIST)[0]
            .performScrollToIndex(index)
    }

    private fun waitForScrollButtonHidden(timeoutMillis: Long = 5_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            runCatching {
                composeRule.onAllNodesWithTag(HomeScreenTestTags.SCROLL_TO_LATEST)
                    .assertCountEquals(0)
                true
            }.getOrDefault(false)
        }
    }
}
