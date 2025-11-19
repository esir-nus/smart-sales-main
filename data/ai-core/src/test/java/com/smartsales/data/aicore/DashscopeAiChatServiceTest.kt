// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/DashscopeAiChatServiceTest.kt
// 模块：:data:ai-core
// 说明：验证 DashscopeAiChatService 的重试与Markdown合成逻辑
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import java.io.IOException
import java.util.Optional
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashscopeAiChatServiceTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dashscopeClient: RecordingDashscopeClient
    private lateinit var service: DashscopeAiChatService

    @Before
    fun setup() {
        dashscopeClient = RecordingDashscopeClient()
        service = DashscopeAiChatService(
            dispatchers = FakeDispatcherProvider(dispatcher),
            dashscopeClient = dashscopeClient,
            credentialsProvider = FakeCredentialsProvider(apiKey = "key", model = "qwen-turbo"),
            optionalConfig = Optional.of(
                AiCoreConfig(
                    preferFakeAiChat = false,
                    dashscopeMaxRetries = 2,
                    dashscopeRequestTimeoutMillis = 5_000,
                    dashscopeEnableStreaming = true
                )
            )
        )
    }

    @Test
    fun `成功返回会把display文本写入Markdown`() = runTest(dispatcher) {
        dashscopeClient.completionText = "助手结论"

        val result = service.sendMessage(
            AiChatRequest(prompt = "请总结")
        ).assertSuccess()

        assertEquals("助手结论", result.displayText)
        assertTrue(result.structuredMarkdown!!.startsWith("助手结论"))
        assertTrue(result.structuredMarkdown!!.contains("## 输入摘要"))
    }

    @Test
    fun `失败一次后重试成功`() = runTest(dispatcher) {
        dashscopeClient.failureBeforeSuccess = 1

        val result = service.sendMessage(AiChatRequest(prompt = "重试案例")).assertSuccess()

        assertEquals(2, dashscopeClient.attempts)
        assertEquals("重试案例", dashscopeClient.lastRequest?.messages?.last()?.content?.trim())
        assertTrue(result.displayText.isNotBlank())
    }

    @Test
    fun `重试耗尽后返回错误`() = runTest(dispatcher) {
        dashscopeClient.failureBeforeSuccess = 5

        val result = service.sendMessage(AiChatRequest(prompt = "永远失败"))

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertEquals(AiCoreErrorReason.NETWORK, error.reason)
        assertEquals(3, dashscopeClient.attempts)
    }

    @Test
    fun `缺少APIKey直接返回错误`() = runTest(dispatcher) {
        val blankService = DashscopeAiChatService(
            dispatchers = FakeDispatcherProvider(dispatcher),
            dashscopeClient = dashscopeClient,
            credentialsProvider = FakeCredentialsProvider(apiKey = "", model = "qwen-turbo"),
            optionalConfig = Optional.empty()
        )

        val result = blankService.sendMessage(AiChatRequest(prompt = "无法执行"))

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertEquals(AiCoreErrorReason.MISSING_CREDENTIALS, error.reason)
    }

    @Test
    fun `streamMessage emits chunk与完成事件`() = runTest(dispatcher) {
        val streamingClient = RecordingDashscopeClient()
        val streamingService = DashscopeAiChatService(
            dispatchers = FakeDispatcherProvider(dispatcher),
            dashscopeClient = streamingClient,
            credentialsProvider = FakeCredentialsProvider(apiKey = "key", model = "qwen"),
            optionalConfig = Optional.of(
                AiCoreConfig(
                    preferFakeAiChat = false,
                    dashscopeEnableStreaming = true
                )
            )
        )
        streamingClient.streamChunks = listOf("分段1", "分段2")

        val events = streamingService.streamMessage(
            AiChatRequest(prompt = "流式请求")
        ).toList()

        assertTrue(events.filterIsInstance<AiChatStreamEvent.Chunk>().size == 2)
        assertTrue(events.last() is AiChatStreamEvent.Completed)
    }

    private fun <T> Result<T>.assertSuccess(): T =
        when (this) {
            is Result.Success -> data
            is Result.Error -> throw AssertionError("期望成功但失败：${throwable.message}", throwable)
        }

    private class FakeCredentialsProvider(
        private val apiKey: String,
        private val model: String
    ) : DashscopeCredentialsProvider {
        override fun obtain(): DashscopeCredentials = DashscopeCredentials(apiKey, model)
    }

    private class RecordingDashscopeClient : DashscopeClient {
        var completionText: String = "默认回复"
        var failureBeforeSuccess: Int = 0
        var attempts: Int = 0
        var lastRequest: DashscopeRequest? = null
        var streamChunks: List<String> = emptyList()

        override fun generate(request: DashscopeRequest): DashscopeCompletion {
            attempts += 1
            lastRequest = request
            if (attempts <= failureBeforeSuccess) {
                throw IOException("模拟网络错误")
            }
            return DashscopeCompletion(displayText = completionText)
        }

        override fun stream(request: DashscopeRequest): Flow<DashscopeStreamEvent> {
            lastRequest = request
            return flow {
                if (streamChunks.isEmpty()) {
                    emit(DashscopeStreamEvent.Failed("无数据", null))
                    return@flow
                }
                streamChunks.forEach { emit(DashscopeStreamEvent.Chunk(it)) }
                emit(DashscopeStreamEvent.Completed)
            }
        }
    }
}
