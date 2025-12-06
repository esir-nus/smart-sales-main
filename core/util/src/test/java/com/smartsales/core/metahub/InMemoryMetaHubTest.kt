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
    fun upsertSession_overwritesExisting() = runTest {
        val first = SessionMetadata(
            sessionId = "session-1",
            mainPerson = "客户A",
            shortSummary = "首轮摘要",
            summaryTitle6Chars = "摘要一",
            location = "北京",
            stage = SessionStage.DISCOVERY,
            riskLevel = RiskLevel.LOW,
            tags = setOf("初访"),
            lastUpdatedAt = 1L
        )
        val updated = first.copy(mainPerson = "客户B", tags = setOf("复访"), lastUpdatedAt = 2L)

        metaHub.upsertSession(first)
        metaHub.upsertSession(updated)

        assertEquals(updated, metaHub.getSession("session-1"))
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
