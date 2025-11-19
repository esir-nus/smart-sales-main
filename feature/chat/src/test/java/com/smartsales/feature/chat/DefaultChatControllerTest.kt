package com.smartsales.feature.chat

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.ExportResult
import com.smartsales.data.aicore.FakeAiChatService
import com.smartsales.data.aicore.FakeExportManager
import com.smartsales.data.aicore.FakeTingwuCoordinator
import com.smartsales.data.aicore.TingwuRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 文件路径: feature/chat/src/test/java/com/smartsales/feature/chat/DefaultChatControllerTest.kt
// 文件作用: 覆盖聊天控制器的核心状态机行为
// 最近修改: 2025-11-14
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultChatControllerTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatcherProvider = FakeDispatcherProvider(dispatcher)
    private val shareHandler = RecordingShareHandler()
    private val controller = DefaultChatController(
        chatService = FakeAiChatService(dispatcherProvider),
        exportManager = FakeExportManager(dispatcherProvider),
        tingwuCoordinator = FakeTingwuCoordinator(dispatcherProvider),
        sessionRepository = InMemoryAiSessionRepository(),
        dispatchers = dispatcherProvider,
        shareHandler = shareHandler
    )

    @Test
    fun sendMessage_appendsUserAndAssistantMessages() = runTest(dispatcher) {
        controller.send("测试下一步行动")
        advanceUntilIdle()

        val state = controller.state.value
        assertEquals(2, state.messages.size)
        assertTrue(state.messages.first().role == ChatRole.User)
        assertTrue(state.messages.last().role == ChatRole.Assistant)
        assertTrue(!state.structuredMarkdown.isNullOrBlank())
        assertTrue(shareHandler.assistantCopyText?.startsWith("Echo: 测试下一步行动") == true)
        assertEquals("助手回复已复制，可直接粘贴。", state.clipboardMessage)
    }

    @Test
    fun requestExport_emitsCompletedState() = runTest(dispatcher) {
        controller.send("生成导出")
        advanceUntilIdle()

        controller.requestExport(ExportFormat.PDF)
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue(state.exportState is ChatExportState.Completed)
        assertTrue(shareHandler.sharedResult != null)
    }

    @Test
    fun copyMarkdown_pushesToClipboardHandler() = runTest(dispatcher) {
        controller.send("生成结构化内容")
        advanceUntilIdle()

        controller.copyMarkdown()
        advanceUntilIdle()

        assertTrue(shareHandler.copiedMarkdown?.contains("Echo") == true)
    }

    @Test
    fun startTranscriptJob_injectsTranscriptMessage() = runTest(dispatcher) {
        controller.startTranscriptJob(TingwuRequest(audioAssetName = "demo.wav"))
        advanceUntilIdle()

        val state = controller.state.value
        assertTrue(state.messages.any { it.role == ChatRole.Transcript })
        assertTrue(state.transcriptState is TranscriptState.Ready)
    }

    @Test
    fun importTranscript_formatsMarkdownForReadability() = runTest(dispatcher) {
        controller.importTranscript("客户感谢支持。我们下周跟进!", "demo.wav")

        val state = controller.state.value
        val transcript = state.transcriptState as TranscriptState.Ready
        assertTrue(transcript.markdown.startsWith("### 转写内容"))
        assertTrue(transcript.markdown.contains("- 客户感谢支持。"))
        assertTrue(transcript.markdown.contains("- 我们下周跟进!"))
    }

    private class RecordingShareHandler : ChatShareHandler {
        var copiedMarkdown: String? = null
        var assistantCopyText: String? = null
        var sharedResult: ExportResult? = null

        override suspend fun copyMarkdown(markdown: String): Result<Unit> {
            copiedMarkdown = markdown
            return Result.Success(Unit)
        }

        override suspend fun copyAssistantReply(text: String): Result<Unit> {
            assistantCopyText = text
            return Result.Success(Unit)
        }

        override suspend fun shareExport(result: ExportResult): Result<Unit> {
            sharedResult = result
            return Result.Success(Unit)
        }
    }
}
