package com.smartsales.feature.chat.home

import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillDefinition
import com.smartsales.feature.chat.core.QuickSkillId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeScreenViewModelTest.kt
// 模块：:feature:chat
// 说明：验证 HomeScreenViewModel 与快捷技能相关的状态与事件
// 作者：创建于 2025-11-20
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `quick skills loaded from catalog`() = runTest(dispatcher) {
        val catalog = FakeQuickSkillCatalog()
        val viewModel = createViewModel(quickSkillCatalog = catalog).first

        val uiSkills = viewModel.uiState.value.quickSkills

        assertEquals(catalog.homeQuickSkills().map { it.id }, uiSkills.map { it.id })
        assertEquals(catalog.homeQuickSkills().map { it.label }, uiSkills.map { it.label })
    }

    @Test
    fun `typed send clears input and omits quickSkillId`() = runTest(dispatcher) {
        val (viewModel, chatService) = createViewModel()

        viewModel.onInputChanged("回顾销售机会")
        viewModel.onSendMessage()
        advanceUntilIdle()

        val lastRequest = chatService.lastRequest
        assertNotNull(lastRequest)
        assertEquals(null, lastRequest?.quickSkillId)
        assertEquals("", viewModel.uiState.value.inputText)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun `quick skill send passes skill id`() = runTest(dispatcher) {
        val catalog = FakeQuickSkillCatalog()
        val (viewModel, chatService) = createViewModel(quickSkillCatalog = catalog)

        viewModel.onSelectQuickSkill(QuickSkillId.SUMMARIZE_LAST_MEETING)
        advanceUntilIdle()

        val lastRequest = chatService.lastRequest
        assertNotNull(lastRequest)
        assertEquals(QuickSkillId.SUMMARIZE_LAST_MEETING.name, lastRequest?.quickSkillId)
        assertEquals(
            catalog.homeQuickSkills().first { it.id == QuickSkillId.SUMMARIZE_LAST_MEETING }.defaultPrompt,
            lastRequest?.userMessage
        )
        assertFalse(viewModel.uiState.value.isSending)
    }

    private fun createViewModel(
        chatService: FakeAiChatService = FakeAiChatService(),
        quickSkillCatalog: QuickSkillCatalog = FakeQuickSkillCatalog()
    ): Pair<HomeScreenViewModel, FakeAiChatService> {
        val viewModel = HomeScreenViewModel(
            aiChatService = chatService,
            aiSessionRepository = object : AiSessionRepository {
                override suspend fun loadOlderMessages(currentTopMessageId: String?) = emptyList<ChatMessageUi>()
            },
            deviceConnectionManager = object : DeviceConnectionManager {
                override fun snapshot(): DeviceSnapshotUi? = null
            },
            mediaSyncCoordinator = object : MediaSyncCoordinator {
                override fun audioSummary(): AudioSummaryUi? = null
            },
            quickSkillCatalog = quickSkillCatalog,
            sessionId = "test-session"
        )
        return viewModel to chatService
    }

    private class FakeAiChatService : AiChatService {
        var lastRequest: ChatRequest? = null

        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
            lastRequest = request
            emit(ChatStreamEvent.Completed(fullText = "助手回复"))
        }
    }

    private class FakeQuickSkillCatalog : QuickSkillCatalog {
        private val definitions = listOf(
            QuickSkillDefinition(
                id = QuickSkillId.SUMMARIZE_LAST_MEETING,
                label = "Summarize last meeting",
                description = "Summarize recent sales meeting",
                defaultPrompt = "Summarize meeting",
                requiresAudioContext = true,
                isRecommended = true
            ),
            QuickSkillDefinition(
                id = QuickSkillId.EXTRACT_ACTION_ITEMS,
                label = "Extract action items",
                description = "List follow-up tasks",
                defaultPrompt = "List action items",
                requiresAudioContext = true,
                isRecommended = true
            )
        )

        override fun homeQuickSkills(): List<QuickSkillDefinition> = definitions
    }
}
