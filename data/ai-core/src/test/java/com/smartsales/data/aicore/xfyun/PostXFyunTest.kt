// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/PostXFyunTest.kt
// 模块：:data:ai-core
// 说明：验证 PostXFyun 跨说话人边界分词漂移修复（仅 1~2 字移动 + 严格 JSON 仲裁）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.PostXfyunSettings
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostXFyunTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)

    @Test
    fun `repairs 6 slash d split by moving head to prev`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"d","confidence":0.95,"reason":"6d 被切开"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 3,
                    confidenceThreshold = 0.8,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：型号是6
            - 发言人 2：d传感器可以的
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "型号是6"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1100, endMs = 2000, text = "d传感器可以的"),
        )

        val result = post.polish(markdown, segments)

        assertTrue(result.polishedMarkdown.contains("- 发言人 1：型号是6d"))
        assertTrue(result.polishedMarkdown.contains("- 发言人 2：传感器可以的"))
        assertEquals(1, result.repairs.size)
        assertEquals(PostXFyunAction.MOVE_HEAD_TO_PREV, result.repairs.first().action)
    }

    @Test
    fun `repairs 罗 slash 总 split by moving head to prev`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"总","confidence":0.92,"reason":"称呼被切开"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 3,
                    confidenceThreshold = 0.8,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：好的罗
            - 发言人 2：总我们继续
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "好的罗"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1050, endMs = 2000, text = "总我们继续"),
        )

        val result = post.polish(markdown, segments)

        assertTrue(result.polishedMarkdown.contains("- 发言人 1：好的罗总"))
        assertTrue(result.polishedMarkdown.contains("- 发言人 2：我们继续"))
        assertEquals(1, result.repairs.size)
    }

    @Test
    fun `repairs 为 slash 什 split by moving head to prev`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"什","confidence":0.9,"reason":"为什 被切开"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 3,
                    confidenceThreshold = 0.8,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：这是为
            - 发言人 2：什么原因呢
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "这是为"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1020, endMs = 2000, text = "什么原因呢"),
        )

        val result = post.polish(markdown, segments)

        assertTrue(result.polishedMarkdown.contains("- 发言人 1：这是为什"))
        assertTrue(result.polishedMarkdown.contains("- 发言人 2：么原因呢"))
        assertEquals(1, result.repairs.size)
    }

    @Test
    fun `same speaker boundary is eligible when gap within threshold`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"总","confidence":0.92,"reason":"同说话人也可能切分漂移"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 3,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：好的罗
            - 发言人 1：总我们继续
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "好的罗"),
            XfyunTranscriptSegment(roleId = "1", startMs = 1100, endMs = 2000, text = "总我们继续"),
        )

        val result = post.polish(markdown, segments)

        assertTrue(result.polishedMarkdown.contains("- 发言人 1：好的罗总"))
        assertTrue(result.polishedMarkdown.contains("- 发言人 1：我们继续"))
        assertEquals(1, result.repairs.size)
    }

    @Test
    fun `debug info is populated even when LLM returns NONE`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"NONE","span":"","confidence":0.99,"reason":"无需修复"}"""
        )
        val template = "TEMPLATE-XYZ"
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 1,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
                    promptTemplate = template,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：好的罗
            - 发言人 2：总我们继续
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "好的罗"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1100, endMs = 2000, text = "总我们继续"),
        )

        val result = post.polish(markdown, segments)

        assertEquals(markdown, result.polishedMarkdown)
        assertTrue(result.repairs.isEmpty())

        val debug = requireNotNull(result.debugInfo)
        assertEquals(template.length, debug.settings.promptLength)
        assertTrue(debug.settings.promptPreview.startsWith("TEMPLATE-"))
        assertEquals("(default)", debug.settings.modelEffective)
        assertEquals(1, debug.suspiciousBoundaries.size)
        assertEquals(1, debug.decisions.size)
        assertEquals(PostXFyunAction.NONE, debug.decisions.first().action)
        assertEquals(1, debug.candidatesCount)
        assertEquals(1, debug.arbitrationsAttempted)
        assertEquals(0, debug.repairsApplied)
    }

    @Test
    fun `maxRepairsPerTranscript caps arbitrations even when decisions are always NONE`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"NONE","span":"","confidence":0.99,"reason":"无需修复"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 3,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
                )
            )
        )
        val lines = buildString {
            appendLine("## 讯飞转写")
            repeat(8) { index ->
                appendLine("- 发言人 1：第${index}句")
            }
        }.trimEnd()
        val segments = (0 until 8).map { index ->
            XfyunTranscriptSegment(
                roleId = "1",
                startMs = (index * 1000).toLong(),
                endMs = ((index + 1) * 1000).toLong(),
                text = "第${index}句",
            )
        }

        val result = post.polish(lines, segments)

        assertEquals(lines, result.polishedMarkdown)
        assertTrue(result.repairs.isEmpty())
        assertEquals(3, service.callCount)
        val debug = requireNotNull(result.debugInfo)
        assertEquals(7, debug.candidatesCount)
        assertEquals(3, debug.arbitrationsAttempted)
        assertEquals(0, debug.repairsApplied)
        assertEquals(debug.candidatesCount, debug.decisions.size)
    }

    @Test
    fun `prompt contains suspicious marker and model override is passed through`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"NONE","span":"","confidence":0.99,"reason":"无需修复"}"""
        )
        val template = "PRE={{PREV_LINE}}\nNEXT={{NEXT_LINE}}\nB={{BOUNDARY_MARK}}"
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 1,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
                    model = "qwen-max3",
                    promptTemplate = template,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：好的罗
            - 发言人 2：总我们继续
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "好的罗"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1100, endMs = 2000, text = "总我们继续"),
        )

        val result = post.polish(markdown, segments)

        assertEquals(markdown, result.polishedMarkdown)
        assertTrue(result.polishedMarkdown.contains("〔/suspicious〕").not())
        val prompt = service.requests.single().prompt
        assertTrue(prompt.contains("〔/suspicious〕"))
        assertEquals("qwen-max3", service.requests.single().model)
    }

    @Test
    fun `invalid span or low confidence must fallback to NONE`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"X","confidence":0.2,"reason":"低置信度"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 3,
                    confidenceThreshold = 0.8,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：型号是6
            - 发言人 2：d传感器可以的
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "型号是6"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1100, endMs = 2000, text = "d传感器可以的"),
        )

        val result = post.polish(markdown, segments)

        assertEquals(markdown, result.polishedMarkdown)
        assertTrue(result.repairs.isEmpty())
    }

    private fun provider(settings: PostXfyunSettings): AiParaSettingsProvider {
        return object : AiParaSettingsProvider {
            override fun snapshot(): AiParaSettingsSnapshot {
                return AiParaSettingsSnapshot(
                    transcription = AiParaSettingsSnapshot().transcription.copy(
                        xfyun = AiParaSettingsSnapshot().transcription.xfyun.copy(
                            postXfyun = settings
                        )
                    )
                )
            }
        }
    }

    private class FakeArbitrationService(
        private val json: String,
    ) : AiChatService {
        val requests: MutableList<AiChatRequest> = mutableListOf()
        val callCount: Int
            get() = requests.size

        override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> {
            requests += request
            return Result.Success(
                AiChatResponse(
                    displayText = json,
                    structuredMarkdown = null,
                    references = emptyList()
                )
            )
        }
    }
}
