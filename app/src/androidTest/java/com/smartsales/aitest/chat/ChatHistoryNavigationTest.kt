package com.smartsales.aitest.chat

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.aitest.AiFeatureTestActivity
import com.smartsales.aitest.AiFeatureTestTags
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
import java.util.UUID
import kotlinx.coroutines.runBlocking
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

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(context, ChatHistoryEntryPoint::class.java)
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
        // 打开会话历史 Tab
        composeRule.onNodeWithTag(AiFeatureTestTags.CHIP_CHAT_HISTORY).performClick()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_CHAT_HISTORY).assertIsDisplayed()

        // 点击历史项返回 Home
        composeRule.onNodeWithText("历史会话").performClick()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME).assertIsDisplayed()

        // 验证历史消息已加载
        composeRule.onNodeWithTag(HomeScreenTestTags.USER_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithText("预置消息").assertIsDisplayed()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatHistoryEntryPoint {
    fun aiSessionRepository(): AiSessionRepository
    fun chatHistoryRepository(): ChatHistoryRepository
}
