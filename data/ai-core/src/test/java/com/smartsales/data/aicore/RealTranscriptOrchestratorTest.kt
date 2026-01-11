package com.smartsales.data.aicore

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/RealTranscriptOrchestratorTest.kt
// 模块：:data:ai-core
// 说明：验证转写元数据推理的缓存、解析与写入行为
// 作者：创建于 2025-12-06

import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionStage
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.core.metahub.SpeakerRole
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealTranscriptOrchestratorTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)

    @Test
    fun `parses json and writes transcript plus session metadata`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            ```json
            {
              "speaker_map": {
                "spk_1": {"display_name": "罗总", "role": "客户", "confidence": 1.2},
                "spk_2": {"display_name": "销售顾问", "role": "销售", "confidence": 0.6}
              },
              "main_person": "罗总",
              "short_summary": "简要摘要",
              "summary_title_6chars": "核心纪要",
              "location": "上海",
              "stage": "DISCOVERY",
              "risk_level": "HIGH",
              "chapters": [
                 {"title": "开场", "start_ms": 0, "end_ms": 1000, "summary": "简短摘要"}
              ],
              "key_points": [
                 {"text": "重点", "time_range": {"start_ms": 100, "end_ms": 500}}
              ]
            }
            ```
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-1",
                sessionId = "s-1",
                diarizedSegments = listOf(
                    DiarizedSegment(speakerId = "spk_1", speakerIndex = 0, startMs = 0, endMs = 1_000, text = "你好"),
                    DiarizedSegment(speakerId = "spk_2", speakerIndex = 1, startMs = 1_000, endMs = 2_000, text = "欢迎光临")
                ),
                speakerLabels = mapOf("spk_1" to "发言人 1")
            )
        )
        advanceUntilIdle()

        val speaker = result?.speakerMap?.get("spk_1")
        assertEquals("罗总", speaker?.displayName)
        assertEquals(1f, speaker?.confidence)
        val session = metaHub.getSession("s-1")
        assertEquals("罗总", session?.mainPerson)
        assertEquals("简要摘要", session?.shortSummary)
        assertEquals("核心纪要", session?.summaryTitle6Chars)
        assertEquals(SessionStage.DISCOVERY, session?.stage)
        assertEquals(RiskLevel.HIGH, session?.riskLevel)

        assertEquals(1, result?.chapters?.size)
        assertEquals("开场", result?.chapters?.first()?.title)
        assertEquals(0L, result?.chapters?.first()?.startMs)
        assertEquals("简短摘要", result?.chapters?.first()?.summary)

        assertEquals(1, result?.keyPoints?.size)
        assertEquals("重点", result?.keyPoints?.first()?.text)
        assertEquals(100L, result?.keyPoints?.first()?.timeRange?.startMs)
    }

    @Test
    fun `cache hit returns existing metadata without invoking ai`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val cached = TranscriptMetadata(
            transcriptId = "t-2",
            sessionId = "s-2",
            speakerMap = mapOf(
                "spk_1" to SpeakerMeta(
                    displayName = "客户A",
                    role = SpeakerRole.CUSTOMER,
                    confidence = 0.9f
                )
            )
        )
        metaHub.upsertTranscript(cached)
        val ai = RecordingAiChatService("{}", mutableListOf())
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-2",
                sessionId = "s-2",
                diarizedSegments = listOf(
                    DiarizedSegment("spk_1", 0, 0, 1_000, "片段")
                ),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertEquals("客户A", result?.speakerMap?.get("spk_1")?.displayName)
        assertEquals(0, ai.callCount)
    }

    @Test
    fun `force true bypasses cache and overwrites speakers`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        metaHub.upsertTranscript(
            TranscriptMetadata(
                transcriptId = "t-3",
                sessionId = "s-3",
                speakerMap = mapOf(
                    "spk_1" to SpeakerMeta(
                        displayName = "旧标签",
                        role = SpeakerRole.OTHER,
                        confidence = 0.5f
                    )
                )
            )
        )
        val ai = RecordingAiChatService(
            """
            {"speaker_map":{"spk_1":{"display_name":"新客户","role":"客户","confidence":0.4}}}
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-3",
                sessionId = "s-3",
                diarizedSegments = listOf(
                    DiarizedSegment("spk_1", 0, 0, 1_000, "感谢光临")
                ),
                speakerLabels = emptyMap(),
                force = true
            )
        )
        advanceUntilIdle()

        assertEquals(1, ai.callCount)
        assertEquals("新客户", result?.speakerMap?.get("spk_1")?.displayName)
        assertEquals(SpeakerRole.CUSTOMER, result?.speakerMap?.get("spk_1")?.role)
    }

    @Test
    fun `invalid json fails soft without corrupting metadata`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService("not-json")
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-4",
                sessionId = "s-4",
                diarizedSegments = listOf(
                    DiarizedSegment("spk_1", 0, 0, 500, "你好")
                ),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertNull(result)
        assertNull(metaHub.getTranscriptBySession("s-4"))
    }

    @Test
    fun `confidence is clamped into valid range`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            {"speaker_map":{
              "spk_1":{"display_name":"客户","confidence":1.8},
              "spk_2":{"display_name":"销售","confidence":-0.2}
            }}
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-5",
                sessionId = "s-5",
                diarizedSegments = listOf(
                    DiarizedSegment("spk_1", 0, 0, 500, "欢迎"),
                    DiarizedSegment("spk_2", 1, 500, 1_000, "介绍产品")
                ),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertEquals(1f, result?.speakerMap?.get("spk_1")?.confidence)
        assertEquals(0f, result?.speakerMap?.get("spk_2")?.confidence)
    }

    @Test
    fun `parses fenced json with leading text and ignores unknown keys`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            文本开头
            ```json
            {
              "speaker_map": {
                "spk_a": {"display_name": "顾客", "role": "客户", "confidence": 0.95}
              },
              "main_person": "顾客",
              "extra_field": "ignored"
            }
            ```
            结尾文本
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-6",
                sessionId = "s-6",
                diarizedSegments = listOf(DiarizedSegment("spk_a", 0, 0, 1_000, "你好")),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertEquals("顾客", result?.speakerMap?.get("spk_a")?.displayName)
        assertEquals("顾客", metaHub.getSession("s-6")?.mainPerson)
    }

    @Test
    fun `recovers when json contains unknown fields`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            {
              "speaker_map": {
                "spk_x": {"display_name": "客户X", "role": "other", "confidence": 0.5}
              },
              "location": "上海",
              "unknown_block": {"foo": "bar"}
            }
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-7",
                sessionId = "s-7",
                diarizedSegments = listOf(DiarizedSegment("spk_x", 0, 0, 1_000, "hi")),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertEquals("客户X", result?.speakerMap?.get("spk_x")?.displayName)
        assertEquals("上海", result?.location)
    }

    @Test
    fun `chapters with invalid timestamps are silently skipped`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            {
              "speaker_map": {},
              "chapters": [
                 {"title": "正常章节", "start_ms": 0, "end_ms": 1000},
                 {"title": "倒序时间", "start_ms": 5000, "end_ms": 1000},
                 {"title": "负数开始", "start_ms": -100, "end_ms": 500},
                 {"title": "缺少结束", "start_ms": 0}
              ]
            }
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-edge-1",
                sessionId = "s-edge-1",
                diarizedSegments = listOf(DiarizedSegment("spk_1", 0, 0, 1_000, "测试")),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        // Only the valid chapter should be parsed
        assertEquals(1, result?.chapters?.size)
        assertEquals("正常章节", result?.chapters?.first()?.title)
    }

    @Test
    fun `empty or missing chapters and keyPoints arrays return empty lists`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            {
              "speaker_map": {"spk_1": {"display_name": "客户"}},
              "chapters": [],
              "main_person": "客户"
            }
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-edge-2",
                sessionId = "s-edge-2",
                diarizedSegments = listOf(DiarizedSegment("spk_1", 0, 0, 1_000, "测试")),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertEquals(0, result?.chapters?.size)
        assertEquals(0, result?.keyPoints?.size)
        assertEquals("客户", result?.speakerMap?.get("spk_1")?.displayName)
    }

    @Test
    fun `keyPoints without time_range are still parsed`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            {
              "speaker_map": {},
              "key_points": [
                 {"text": "有时间范围", "time_range": {"start_ms": 100, "end_ms": 200}},
                 {"text": "无时间范围"},
                 {"text": "无效时间范围", "time_range": {"start_ms": 500, "end_ms": 100}}
              ]
            }
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-edge-3",
                sessionId = "s-edge-3",
                diarizedSegments = listOf(DiarizedSegment("spk_1", 0, 0, 1_000, "测试")),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        // All 3 keyPoints are parsed (text is the only required field)
        assertEquals(3, result?.keyPoints?.size)
        assertEquals("有时间范围", result?.keyPoints?.get(0)?.text)
        assertEquals(100L, result?.keyPoints?.get(0)?.timeRange?.startMs)
        assertEquals("无时间范围", result?.keyPoints?.get(1)?.text)
        assertNull(result?.keyPoints?.get(1)?.timeRange)
        assertEquals("无效时间范围", result?.keyPoints?.get(2)?.text)
        assertNull(result?.keyPoints?.get(2)?.timeRange) // Invalid range becomes null
    }

    private class RecordingAiChatService(
        private val responseText: String,
        private val history: MutableList<AiChatRequest> = mutableListOf()
    ) : AiChatService {
        var callCount: Int = 0
            private set

        override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> {
            callCount += 1
            history += request
            return Result.Success(
                AiChatResponse(
                    displayText = responseText,
                    structuredMarkdown = responseText,
                    references = emptyList()
                )
            )
        }
    }
}
