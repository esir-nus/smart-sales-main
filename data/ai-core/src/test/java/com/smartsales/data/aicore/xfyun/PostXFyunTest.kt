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
    fun `arbitration budget covers all candidates when list is small`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """
                {
                  "action":"NONE",
                  "span":"",
                  "confidence":0.99,
                  "reason":"无需修复"
                }
            """.trimIndent()
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
        // 说明：候选数较少时，仲裁预算 = candidates.size，确保能覆盖后面的边界。
        assertEquals(7, service.callCount)
        val debug = requireNotNull(result.debugInfo)
        assertEquals(7, debug.candidatesCount)
        assertEquals(7, debug.arbitrationsAttempted)
        assertEquals(0, debug.repairsApplied)
        assertEquals(debug.arbitrationsAttempted, debug.decisions.size)
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
    fun `raw response preview is captured for first 3 arbitrations only`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """
                {
                  "action":"NONE",
                  "span":"",
                  "confidence":0.99,
                  "reason":"无需修复"
                }
            """.trimIndent()
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 5,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
                )
            )
        )
        val lines = buildString {
            appendLine("## 讯飞转写")
            repeat(7) { index ->
                appendLine("- 发言人 1：第${index}句")
            }
        }.trimEnd()
        val segments = (0 until 7).map { index ->
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
        // 说明：候选数较少时，仲裁预算 = candidates.size（此处 6 个边界）。
        assertEquals(6, service.callCount)
        val debug = requireNotNull(result.debugInfo)
        assertEquals(6, debug.candidatesCount)
        assertEquals(6, debug.arbitrationsAttempted)
        assertEquals(6, debug.decisions.size)
        debug.decisions.take(3).forEach { decision ->
            val preview = requireNotNull(decision.rawResponsePreview)
            assertTrue(preview.contains("\\n"))
            assertTrue(preview.length <= 200)
            assertEquals("OK", decision.parseStatus)
        }
        debug.decisions.drop(3).forEach { decision ->
            assertTrue(decision.rawResponsePreview == null)
        }
    }

    @Test
    fun `fenced JSON is parsed with STRIPPED_FENCE_OK status`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """
                ```json
                {"action":"NONE","span":"","confidence":0.99,"reason":"fence"}
                ```
            """.trimIndent()
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 1,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
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

        val debug = requireNotNull(result.debugInfo)
        assertEquals(1, debug.decisions.size)
        assertEquals("STRIPPED_FENCE_OK", debug.decisions.first().parseStatus)
        assertTrue(debug.decisions.first().rawResponsePreview!!.contains("```"))
    }

    @Test
    fun `non-json reply falls back with NON_JSON status`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = "OK {\"action\":\"NONE\",\"span\":\"\",\"confidence\":0.99,\"reason\":\"prefix\"}"
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    maxRepairsPerTranscript = 1,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
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

        assertTrue(result.repairs.isEmpty())
        val debug = requireNotNull(result.debugInfo)
        assertEquals(1, debug.arbitrationsAttempted)
        assertEquals("NON_JSON", debug.decisions.first().parseStatus)
    }

    @Test
    fun `budget traversal reaches later boundary even if early decisions are NONE`() = runTest(dispatcher) {
        val service = SequencedArbitrationService(
            noneUntilCallIndex = 30,
            thenJson = """{"action":"MOVE_HEAD_TO_PREV","span":"d","confidence":0.95,"reason":"6d 被切开"}"""
        )
        val post = PostXFyun(
            dispatchers = dispatchers,
            aiChatService = service,
            aiParaSettingsProvider = provider(
                PostXfyunSettings(
                    enabled = true,
                    // 说明：repairLimit=5 → arbitrationBudget=max(30, 5*10)=50，足以覆盖 index≈30 的问题边界。
                    maxRepairsPerTranscript = 5,
                    confidenceThreshold = 0.8,
                    suspiciousGapThresholdMs = 200L,
                )
            )
        )
        val lines = buildString {
            appendLine("## 讯飞转写")
            repeat(35) { index ->
                when (index) {
                    30 -> appendLine("- 发言人 1：型号是6")
                    31 -> appendLine("- 发言人 2：d底盘，6。")
                    else -> appendLine("- 发言人 1：第${index}句。")
                }
            }
        }.trimEnd()
        val segments = (0 until 35).map { index ->
            XfyunTranscriptSegment(
                roleId = if (index % 2 == 0) "1" else "2",
                startMs = (index * 1000).toLong(),
                endMs = ((index + 1) * 1000).toLong(),
                text = "seg-$index",
            )
        }

        val result = post.polish(lines, segments)

        assertEquals(1, result.repairs.size)
        assertTrue(result.polishedMarkdown.contains("- 发言人 1：型号是6d"))
        assertTrue(result.polishedMarkdown.contains("- 发言人 2：底盘，6。"))
        // 说明：至少要调用到第 31 次仲裁，才能命中 index=30 的修复。
        assertTrue(service.callCount > 30)
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

    @Test
    fun `phrase span is accepted and applied when within maxSpanChars`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"什么车？","confidence":0.95,"reason":"句子被切半"}"""
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
                    maxSpanChars = 24,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：在路虎能买
            - 发言人 2：什么车？您好罗总欢迎来到捷豹路虎，
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "在路虎能买"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1050, endMs = 2000, text = "什么车？您好罗总欢迎来到捷豹路虎，"),
        )

        val result = post.polish(markdown, segments)

        assertTrue(result.polishedMarkdown.contains("- 发言人 1：在路虎能买什么车？"))
        assertTrue(result.polishedMarkdown.contains("- 发言人 2：您好罗总欢迎来到捷豹路虎，"))
        assertEquals(1, result.repairs.size)
        assertEquals(PostXFyunAction.MOVE_HEAD_TO_PREV, result.repairs.first().action)
    }

    @Test
    fun `phrase span is rejected as bad-span when maxSpanChars is small`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"什么车？","confidence":0.95,"reason":"句子被切半"}"""
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
                    maxSpanChars = 2,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：在路虎能买
            - 发言人 2：什么车？您好罗总欢迎来到捷豹路虎，
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "在路虎能买"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1050, endMs = 2000, text = "什么车？您好罗总欢迎来到捷豹路虎，"),
        )

        val result = post.polish(markdown, segments)

        assertEquals(markdown, result.polishedMarkdown)
        assertTrue(result.repairs.isEmpty())
        val debug = requireNotNull(result.debugInfo)
        assertEquals(1, debug.decisions.size)
        assertEquals("bad-span", debug.decisions.first().reason)
        assertTrue(debug.decisions.first().errorHint?.startsWith("span-length>") == true)
        assertEquals(PostXFyunAction.NONE, debug.decisions.first().action)
    }

    @Test
    fun `accepted span still must match boundary to apply`() = runTest(dispatcher) {
        val service = FakeArbitrationService(
            json = """{"action":"MOVE_HEAD_TO_PREV","span":"您好","confidence":0.95,"reason":"测试不匹配"}"""
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
                    maxSpanChars = 24,
                )
            )
        )
        val markdown = """
            ## 讯飞转写
            - 发言人 1：在路虎能买
            - 发言人 2：什么车？您好罗总欢迎来到捷豹路虎，
        """.trimIndent()
        val segments = listOf(
            XfyunTranscriptSegment(roleId = "1", startMs = 0, endMs = 1000, text = "在路虎能买"),
            XfyunTranscriptSegment(roleId = "2", startMs = 1050, endMs = 2000, text = "什么车？您好罗总欢迎来到捷豹路虎，"),
        )

        val result = post.polish(markdown, segments)

        // 说明：span 合法但不在边界起始处，确定性校验会阻止修复。
        assertEquals(markdown, result.polishedMarkdown)
        assertTrue(result.repairs.isEmpty())
        val debug = requireNotNull(result.debugInfo)
        assertEquals(PostXFyunAction.MOVE_HEAD_TO_PREV, debug.decisions.first().action)
        assertEquals("span-not-at-boundary", debug.decisions.first().errorHint)
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

    private class SequencedArbitrationService(
        private val noneUntilCallIndex: Int,
        private val thenJson: String,
    ) : AiChatService {
        private var calls: Int = 0
        val callCount: Int
            get() = calls

        override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> {
            val index = calls
            calls += 1
            val payload = if (index < noneUntilCallIndex) {
                """{"action":"NONE","span":"","confidence":0.99,"reason":"无需修复"}"""
            } else {
                thenJson
            }
            return Result.Success(
                AiChatResponse(
                    displayText = payload,
                    structuredMarkdown = null,
                    references = emptyList()
                )
            )
        }
    }
}
