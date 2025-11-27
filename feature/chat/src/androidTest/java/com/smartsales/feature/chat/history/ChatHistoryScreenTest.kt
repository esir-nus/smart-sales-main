package com.smartsales.feature.chat.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// 文件：feature/chat/src/androidTest/java/com/smartsales/feature/chat/history/ChatHistoryScreenTest.kt
// 模块：:feature:chat
// 说明：验证 ChatHistoryScreen 的空态、点击与错误提示
// 作者：创建于 2025-11-21
class ChatHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sessions_renderAndClickNotifies() {
        var clicked: String? = null
        composeRule.setContent {
            MaterialTheme {
                ChatHistoryScreen(
                    state = ChatHistoryUiState(
                        sessions = listOf(
                            ChatSessionUi(
                                id = "s1",
                                title = "会话一",
                                lastMessagePreview = "最近一条消息",
                                updatedAt = 1_000,
                                pinned = false
                            )
                        )
                    ),
                    onRefresh = {},
                    onSessionClicked = { clicked = it },
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {},
                    onBackClick = {}
                )
            }
        }

        composeRule.onNodeWithText("会话一").assertIsDisplayed()
        composeRule.onNodeWithText("最近一条消息").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatHistoryTestTags.item("s1")).performClick()
        assertEquals("s1", clicked)
    }

    @Test
    fun emptyState_visible() {
        composeRule.setContent {
            MaterialTheme {
                ChatHistoryScreen(
                    state = ChatHistoryUiState(),
                    onRefresh = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {},
                    onBackClick = {}
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.EMPTY).assertIsDisplayed()
    }

    @Test
    fun errorBanner_dismissHides() {
        composeRule.setContent {
            MaterialTheme {
                val state = remember {
                    mutableStateOf(
                        ChatHistoryUiState(
                            errorMessage = "加载失败"
                        )
                    )
                }
                ChatHistoryScreen(
                    state = state.value,
                    onRefresh = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = { state.value = state.value.copy(errorMessage = null) },
                    onBackClick = {}
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onAllNodesWithTag(ChatHistoryTestTags.ERROR).assertCountEquals(0)
    }
}
