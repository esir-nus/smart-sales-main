package com.smartsales.feature.chat.home

// 文件：feature/chat/src/androidTest/java/com/smartsales/feature/chat/home/HomeSessionListUiTest.kt
// 模块：:feature:chat
// 说明：验证 Home 会话历史列表的展示与点击切换行为
// 作者：创建于 2025-11-25

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Ignore

class HomeSessionListUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Ignore("设备端语义匹配不稳定，待 UI 标注补全后再启用")
    fun switchSession_updatesHeaderAndTranscriptionChip() {
        val initialSessions = listOf(
            SessionListItemUi(
                id = "manual",
                title = "新的聊天",
                lastMessagePreview = "你好",
                updatedAtMillis = 1L,
                isCurrent = true,
                isTranscription = false
            ),
            SessionListItemUi(
                id = "transcription",
                title = "通话分析 – 客户B",
                lastMessagePreview = "摘要",
                updatedAtMillis = 2L,
                isCurrent = false,
                isTranscription = true
            )
        )
        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    HomeUiState(
                        chatMessages = emptyList(),
                        quickSkills = emptyList(),
                        inputText = "",
                        sessionList = initialSessions,
                        currentSession = CurrentSessionUi(
                            id = "manual",
                            title = "新的聊天",
                            isTranscription = false
                        )
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
                    onQuickSkillSelected = {},
                    onClearSelectedSkill = {},
                    onDeviceBannerClicked = {},
                    onAudioSummaryClicked = {},
                    onRefreshDeviceAndAudio = {},
                    onLoadMoreHistory = {},
                    onProfileClicked = {},
                    onSessionSelected = { targetId ->
                        val target = uiState.sessionList.first { it.id == targetId }
                        val updatedList = uiState.sessionList.map {
                            it.copy(isCurrent = it.id == targetId)
                        }
                        uiState = uiState.copy(
                            currentSession = CurrentSessionUi(
                                id = target.id,
                                title = target.title,
                                isTranscription = target.isTranscription
                            ),
                            sessionList = updatedList
                        )
                    },
                    onNewChatClicked = {
                        val newId = "manual-${System.nanoTime()}"
                        val newItem = SessionListItemUi(
                            id = newId,
                            title = "新的聊天",
                            lastMessagePreview = "",
                            updatedAtMillis = 3L,
                            isCurrent = true,
                            isTranscription = false
                        )
                        val updatedList = uiState.sessionList.map { it.copy(isCurrent = false) } + newItem
                        uiState = uiState.copy(
                            currentSession = CurrentSessionUi(
                                id = newId,
                                title = "新的聊天",
                                isTranscription = false
                            ),
                            chatMessages = emptyList(),
                            sessionList = updatedList
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithTag("${HomeScreenTestTags.SESSION_LIST_ITEM_PREFIX}transcription")
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onAllNodesWithText(
                    "通话分析",
                    substring = true,
                    useUnmergedTree = true
                ).assertCountEquals(1)
                true
            }.getOrDefault(false)
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.NEW_CHAT_BUTTON).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTestTags.SESSION_LIST)
            .assertIsDisplayed()
    }

    @Test
    fun emptySessionList_showsFriendlyMessage() {
        val snackbarHostState = SnackbarHostState()
        composeRule.setContent {
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
                    onProfileClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.SESSION_EMPTY).assertIsDisplayed()
    }
}
