package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/HomeHistoryPanelTest.kt
// 模块：:app
// 说明：验证 Home 顶部历史抽屉的展开、选择会话并加载消息的行为
// 作者：创建于 2025-11-26

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeHistoryPanelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(context, HistoryPanelEntryPoint::class.java)
    }

    @Before
    fun seedSessions() = runBlocking {
        // 清理已有会话，避免跨测试污染
        val repo = entryPoint.aiSessionRepository()
        repo.summaries.first().forEach { repo.delete(it.id) }
        entryPoint.chatHistoryRepository().deleteSession("panel-1")

        repo.upsert(
            AiSessionSummary(
                id = "panel-1",
                title = "历史抽屉会话",
                lastMessagePreview = "预置消息",
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        entryPoint.chatHistoryRepository().saveMessages(
            sessionId = "panel-1",
            messages = listOf(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = "panel-1",
                    role = ChatMessageRole.USER.name,
                    content = "预置历史消息",
                    timestampMillis = System.currentTimeMillis()
                )
            )
        )
    }

    @Test
    fun openPanel_selectSession_loadsMessages() {
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT).assertIsDisplayed()

        // 展开历史抽屉
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE).performClick()
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_PANEL).assertIsDisplayed()

        // 点击抽屉里的会话
        composeRule.onNodeWithTag("${HomeScreenTestTags.HISTORY_ITEM_PREFIX}panel-1").performClick()

        // 抽屉应关闭
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.HISTORY_PANEL)
                .fetchSemanticsNodes().isEmpty()
        }

        // Home 页面应加载到该会话的消息
        composeRule.onNodeWithText("预置历史消息").assertIsDisplayed()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HistoryPanelEntryPoint {
    fun aiSessionRepository(): AiSessionRepository
    fun chatHistoryRepository(): ChatHistoryRepository
}
