package com.smartsales.feature.chat.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                        groups = listOf(
                            ChatHistoryGroupUi(
                                label = "7天内",
                                items = listOf(
                                    ChatSessionUi(
                                        id = "s1",
                                        title = "会话一",
                                        lastMessagePreview = "最近一条消息",
                                        updatedAt = 1_000,
                                        pinned = false
                                    )
                                )
                            )
                        )
                    ),
                    onBackClick = {},
                    onSessionClicked = { clicked = it },
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {}
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
                    onBackClick = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {}
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
                    onBackClick = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = { state.value = state.value.copy(errorMessage = null) }
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onNodeWithTag(ChatHistoryTestTags.ERROR).assertDoesNotExist()
    }

    @Test
    fun longPress_opensSheet_andPinAction() {
        var pinToggled = false
        composeRule.setContent {
            MaterialTheme {
                ChatHistoryScreen(
                    state = ChatHistoryUiState(
                        groups = listOf(
                            ChatHistoryGroupUi(
                                label = "7天内",
                                items = listOf(
                                    ChatSessionUi(
                                        id = "s1",
                                        title = "会话一",
                                        lastMessagePreview = "最近一条消息",
                                        updatedAt = 1_000,
                                        pinned = false
                                    )
                                )
                            )
                        )
                    ),
                    onBackClick = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = { pinToggled = true },
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.item("s1"))
            .performTouchInput { longClick() }
        composeRule.onNodeWithTag(ChatHistoryTestTags.SHEET).assertIsDisplayed()
        composeRule.onNodeWithTag(ChatHistoryTestTags.SHEET_PIN).performClick()
        composeRule.waitForIdle()
        assertTrue(pinToggled)
    }

    @Test
    fun longPress_supportsRenameAndDelete() {
        var renamed: Pair<String, String>? = null
        var deleted: String? = null
        composeRule.setContent {
            MaterialTheme {
                ChatHistoryScreen(
                    state = ChatHistoryUiState(
                        groups = listOf(
                            ChatHistoryGroupUi(
                                label = "7天内",
                                items = listOf(
                                    ChatSessionUi(
                                        id = "s1",
                                        title = "会话一",
                                        lastMessagePreview = "最近一条消息",
                                        updatedAt = 1_000,
                                        pinned = false
                                    )
                                )
                            )
                        )
                    ),
                    onBackClick = {},
                    onSessionClicked = {},
                    onRenameSession = { id, title -> renamed = id to title },
                    onDeleteSession = { deleted = it },
                    onPinToggle = {},
                    onDismissError = {}
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.item("s1"))
            .performTouchInput { longClick() }
        composeRule.onNodeWithTag(ChatHistoryTestTags.SHEET_RENAME).performClick()
        composeRule.onNodeWithText("标题").performTextReplacement("新标题")
        composeRule.onNodeWithText("保存标题").performClick()
        assertEquals("s1" to "新标题", renamed)

        composeRule.onNodeWithTag(ChatHistoryTestTags.item("s1"))
            .performTouchInput { longClick() }
        composeRule.onNodeWithTag(ChatHistoryTestTags.SHEET_DELETE).performClick()
        assertEquals("s1", deleted)
    }
}
