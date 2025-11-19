// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/FakeAiChatServiceTest.kt
// 模块：:data:ai-core
// 说明：验证 FakeAiChatService 在不同输入下的结果
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAiChatServiceTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dispatcherProvider: FakeDispatcherProvider
    private lateinit var service: FakeAiChatService

    @Before
    fun setup() {
        dispatcherProvider = FakeDispatcherProvider(dispatcher)
        service = FakeAiChatService(dispatcherProvider)
    }

    @Test
    fun `echoes prompt and selected skills`() = runTest(dispatcher) {
        val request = AiChatRequest(
            prompt = "请总结会议纪要",
            skillTags = setOf("pdf", "csv")
        )

        val response = service.sendMessage(request).assertSuccess()

        assertTrue(response.displayText.contains("Echo: 请总结会议纪要"))
        assertTrue(response.displayText.contains("技能: pdf, csv"))
        assertTrue(response.structuredMarkdown!!.contains("- 输入: 请总结会议纪要"))
        assertTrue(response.structuredMarkdown!!.contains("- 技能: pdf, csv"))
    }

    @Test
    fun `transcript markdown优先覆盖结构化输出`() = runTest(dispatcher) {
        val transcript = """
            ## demo.wav 转写
            - 重点：客户确认下季度采购
        """.trimIndent()
        val request = AiChatRequest(
            prompt = "复盘会议",
            transcriptMarkdown = transcript
        )

        val response = service.sendMessage(request).assertSuccess()

        assertEquals(transcript, response.structuredMarkdown)
    }

    @Test
    fun `生成引用列表对应附件顺序`() = runTest(dispatcher) {
        val attachments = listOf(
            AiAttachment("a.pdf", "application/pdf", "file://a"),
            AiAttachment("b.csv", "text/csv", "file://b")
        )
        val response = service.sendMessage(
            AiChatRequest(prompt = "需要引用", attachments = attachments)
        ).assertSuccess()

        assertEquals(attachments.size, response.references.size)
        response.references.forEachIndexed { index, reference ->
            assertTrue(reference.startsWith("${attachments[index].name}#"))
        }
    }

    @Test
    fun `streamMessage emits chunks then completed`() = runTest(dispatcher) {
        val events = service.streamMessage(
            AiChatRequest(prompt = "测试流式输出")
        ).toList()

        assertTrue(events.first() is AiChatStreamEvent.Chunk)
        assertTrue(events.last() is AiChatStreamEvent.Completed)
    }

    private fun <T> Result<T>.assertSuccess(): T {
        return when (this) {
            is Result.Success -> data
            is Result.Error -> throw AssertionError("期望 Success 但得到 Error", throwable)
        }
    }
}
