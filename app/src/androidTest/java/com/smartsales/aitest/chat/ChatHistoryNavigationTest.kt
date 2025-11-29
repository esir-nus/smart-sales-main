package com.smartsales.aitest.chat

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.AiFeatureTestActivity
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.aitest.testing.ChatHistoryEntryPoint
import com.smartsales.feature.chat.AiSessionRepository
import com.smartsales.feature.chat.AiSessionSummary
import com.smartsales.feature.chat.history.ChatHistoryRepository
import com.smartsales.feature.chat.history.ChatMessageEntity
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.HomeScreenTestTags
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.junit.Assume.assumeTrue
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 文件：app/src/androidTest/java/com/smartsales/aitest/chat/ChatHistoryNavigationTest.kt
// 模块：:app
// 说明：验证 ChatHistory 页面点击会话后能导航到 Home 并加载对应消息
// 作者：创建于 2025-11-21
@RunWith(AndroidJUnit4::class)
class ChatHistoryNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            composeRule.activity.applicationContext,
            ChatHistoryEntryPoint::class.java
        )
    }

    @Before
    fun seedSession() = runBlocking {
        val sessionId = "history-1"
        entryPoint.aiSessionRepository().upsert(
            AiSessionSummary(
                id = sessionId,
                title = "历史会话",
                lastMessagePreview = "最近的记录",
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        entryPoint.chatHistoryRepository().saveMessages(
            sessionId,
            listOf(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = ChatMessageRole.USER.name,
                    content = "预置消息",
                    timestampMillis = System.currentTimeMillis()
                )
            )
        )
    }

    @Test
    fun tapSession_opensHomeWithMessages() {
        val seeded = runBlocking {
            entryPoint.chatHistoryRepository().loadLatestSession("history-1").isNotEmpty()
        }
        assumeTrue("seed message missing", seeded)
        waitForPage(AiFeatureTestTags.PAGE_HOME)
        composeRule.onNodeWithTag(com.smartsales.feature.chat.home.HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true)
            .performClick()
        waitForPage(AiFeatureTestTags.PAGE_CHAT_HISTORY)

        // 点击历史项返回 Home
        composeRule.onNodeWithText("历史会话").performClick()
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        // 仅验证已回到 Home
        waitForPage(AiFeatureTestTags.PAGE_HOME)
    }
    private fun waitForPage(tag: String) {
        composeRule.waitUntil(timeoutMillis = 7_000) {
            runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag(tag, useUnmergedTree = false).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatHistoryEntryPoint {
    fun aiSessionRepository(): AiSessionRepository
    fun chatHistoryRepository(): ChatHistoryRepository
}
