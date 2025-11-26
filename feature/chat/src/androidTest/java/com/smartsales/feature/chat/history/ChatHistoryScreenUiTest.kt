package com.smartsales.feature.chat.history

// 文件：feature/chat/src/androidTest/java/com/smartsales/feature/chat/history/ChatHistoryScreenUiTest.kt
// 模块：:feature:chat
// 说明：验证 ChatHistoryScreen 的标题、列表、空/加载状态与点击行为
// 作者：创建于 2025-11-26

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatHistoryScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list_rendersTitlePreviewAndTime_andHandlesClick() {
        val sessions = listOf(
            ChatSessionUi(
                id = "s1",
                title = "会话一",
                lastMessagePreview = "预览一",
                updatedAt = 1_700_000_000_000,
                pinned = false
            ),
            ChatSessionUi(
                id = "s2",
                title = "会话二",
                lastMessagePreview = "预览二",
                updatedAt = 1_700_000_500_000,
                pinned = true
            )
        )
        var clickedId: String? = null
        val expectedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(1_700_000_000_000))
        val snackbarHostState = SnackbarHostState()

        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    ChatHistoryUiState(
                        sessions = sessions,
                        isLoading = false,
                        errorMessage = null
                    )
                )
            }
            MaterialTheme {
                ChatHistoryScreen(
                    state = uiState,
                    onRefresh = {},
                    onBackClick = {},
                    onSessionClicked = { clickedId = it },
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {},
                    snackbarHostState = snackbarHostState
                )
            }
        }

        composeRule.onNodeWithText("会话历史").assertIsDisplayed()
        composeRule.onNodeWithTag(ChatHistoryTestTags.item("s1")).assertIsDisplayed()
        composeRule.onNodeWithText("预览一").assertIsDisplayed()
        composeRule.onNodeWithText(expectedTime).assertIsDisplayed()

        composeRule.onNodeWithTag(ChatHistoryTestTags.item("s2")).performClick()
        assertEquals("s2", clickedId)
    }

    @Test
    fun emptyState_showsFriendlyMessage() {
        val snackbarHostState = SnackbarHostState()
        composeRule.setContent {
            MaterialTheme {
                ChatHistoryScreen(
                    state = ChatHistoryUiState(
                        sessions = emptyList(),
                        isLoading = false,
                        errorMessage = null
                    ),
                    onRefresh = {},
                    onBackClick = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {},
                    snackbarHostState = snackbarHostState
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithText("暂无会话历史，先从首页开始一次对话吧").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsIndicator() {
        val snackbarHostState = SnackbarHostState()
        composeRule.setContent {
            MaterialTheme {
                ChatHistoryScreen(
                    state = ChatHistoryUiState(
                        sessions = emptyList(),
                        isLoading = true,
                        errorMessage = null
                    ),
                    onRefresh = {},
                    onBackClick = {},
                    onSessionClicked = {},
                    onRenameSession = { _, _ -> },
                    onDeleteSession = {},
                    onPinToggle = {},
                    onDismissError = {},
                    snackbarHostState = snackbarHostState
                )
            }
        }

        composeRule.onNodeWithTag(ChatHistoryTestTags.LOADING).assertIsDisplayed()
    }
}
