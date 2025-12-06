package com.smartsales.core.metahub

// 文件：core/util/src/test/java/com/smartsales/core/metahub/InMemoryMetaHubTest.kt
// 模块：:core:util
// 说明：验证内存版元数据中心的基础读写行为
// 作者：创建于 2025-12-04

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.assertEquals

class InMemoryMetaHubTest {

    private val metaHub = InMemoryMetaHub()

    @Test
    fun upsertSession_mergesWithExistingPreservingNonNullFields() = runTest {
        val first = SessionMetadata(
            sessionId = "session-1",
            mainPerson = "客户A",
            shortSummary = "首轮摘要",
            summaryTitle6Chars = "摘要一",
            location = "北京",
            stage = SessionStage.DISCOVERY,
            riskLevel = RiskLevel.LOW,
            tags = setOf("初访"),
            crmRows = listOf(CrmRow(client = "A", owner = "张三")),
            lastUpdatedAt = 1L
        )
        val updated = SessionMetadata(
            sessionId = "session-1",
            mainPerson = null, // 应保留旧值
            shortSummary = "复访摘要",
            summaryTitle6Chars = null,
            location = "上海",
            stage = SessionStage.NEGOTIATION,
            riskLevel = RiskLevel.MEDIUM,
            tags = setOf("复访", ""),
            crmRows = listOf(
                CrmRow(client = "B", owner = "李四"),
                CrmRow(client = "A", owner = "张三") // 重复，应被去重
            ),
            lastUpdatedAt = 2L
        )

        metaHub.upsertSession(first)
        metaHub.upsertSession(updated)

        val merged = metaHub.getSession("session-1")
        // 非空保留旧值
        assertEquals("客户A", merged?.mainPerson)
        // 非空覆盖
        assertEquals("复访摘要", merged?.shortSummary)
        assertEquals(SessionStage.NEGOTIATION, merged?.stage)
        // tags 合并去重
        assertEquals(setOf("初访", "复访"), merged?.tags)
        // crmRows 合并去重
        assertEquals(
            listOf(
                CrmRow(client = "A", owner = "张三"),
                CrmRow(client = "B", owner = "李四")
            ),
            merged?.crmRows
        )
        // 时间取较大值
        assertEquals(2L, merged?.lastUpdatedAt)
    }

    @Test
    fun transcriptAndExport_roundTrip() = runTest {
        val transcript = TranscriptMetadata(
            transcriptId = "transcript-1",
            sessionId = "session-2",
            speakerMap = mapOf(
                "1" to SpeakerMeta(displayName = "客户", role = SpeakerRole.CUSTOMER, confidence = 0.9f)
            ),
            source = TranscriptSource.TINGWU,
            createdAt = 10L
        )
        val export = ExportMetadata(
            sessionId = "session-2",
            lastPdfFileName = "20251204_客户_纪要.pdf",
            lastPdfGeneratedAt = 20L,
            lastCsvFileName = "20251204_客户_纪要.csv",
            lastCsvGeneratedAt = 30L
        )

        metaHub.upsertTranscript(transcript)
        metaHub.upsertExport(export)

        assertEquals(transcript, metaHub.getTranscriptBySession("session-2"))
        assertEquals(export, metaHub.getExport("session-2"))
    }

    @Test
    fun logUsage_appendsEntries() = runTest {
        val usage = TokenUsage(
            sessionId = "session-3",
            model = "qwen-turbo",
            tokensIn = 12,
            tokensOut = 34,
            latencyMs = 56,
            timestamp = 78
        )

        metaHub.logUsage(usage)

        assertEquals(listOf(usage), metaHub.dumpUsage())
    }
}
