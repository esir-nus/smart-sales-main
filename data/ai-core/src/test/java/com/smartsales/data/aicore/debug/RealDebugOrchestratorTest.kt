// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt
// 模块：:data:ai-core
// 说明：验证 DebugOrchestrator 生成三段 HUD 文本的基础结构
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

import com.smartsales.core.util.DefaultDispatcherProvider
import com.smartsales.core.metahub.ConversationDerivedStateDelta
import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.M2PatchRecord
import com.smartsales.core.metahub.PreprocessSnapshot
import com.smartsales.core.metahub.Provenance
import com.smartsales.core.metahub.SuspiciousBoundary
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.data.aicore.params.TranscriptionSettings
import com.smartsales.data.aicore.params.TRANSCRIPTION_PROVIDER_XFYUN
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RealDebugOrchestratorTest {
    @Test
    fun `snapshot includes lane decision and placeholders`() = runTest {
        val settingsRepository = InMemoryAiParaSettingsRepository()
        settingsRepository.update {
            it.copy(
                transcription = TranscriptionSettings(
                    provider = TRANSCRIPTION_PROVIDER_XFYUN,
                    xfyunEnabled = false,
                )
            )
        }
        val orchestrator = RealDebugOrchestrator(
            metaHub = InMemoryMetaHub(),
            aiParaSettingsRepository = settingsRepository,
            tingwuTraceStore = TingwuTraceStore(),
            xfyunTraceStore = XfyunTraceStore(),
            dispatchers = DefaultDispatcherProvider,
        )

        val snapshot = orchestrator.getDebugSnapshot(
            sessionId = "s-1",
            jobId = "job-1",
            sessionTitle = "客户复盘",
            isTitleUserEdited = true
        )
        assertTrue(snapshot.section1EffectiveRunText.contains("sessionId: s-1"))
        assertTrue(snapshot.section1EffectiveRunText.contains("lane.selected: TINGWU"))
        assertTrue(snapshot.section1EffectiveRunText.contains("lane.disabledReason: XFYUN_DISABLED_BY_SETTING"))
        assertTrue(snapshot.section1EffectiveRunText.contains("exportGate.ready"))
        assertTrue(snapshot.section2RawTranscriptionText.contains("Tingwu:"))
        assertTrue(snapshot.section3PreprocessedText.contains("Preprocessed"))
    }

    @Test
    fun `snapshot prefers metahub preprocess when meaningful`() = runTest {
        val settingsRepository = InMemoryAiParaSettingsRepository()
        val metaHub = InMemoryMetaHub()
        metaHub.appendM2Patch(
            sessionId = "s-1",
            patch = M2PatchRecord(
                patchId = "m2-preprocess-1",
                createdAt = 123L,
                prov = Provenance(source = "tingwu.preprocess", updatedAt = 123L),
                payload = ConversationDerivedStateDelta(
                    preprocess = PreprocessSnapshot(
                        first20Rendered = listOf("METAHUB_PREVIEW_LINE"),
                        suspiciousBoundaries = listOf(
                            SuspiciousBoundary(index = 5, reason = "gap"),
                            SuspiciousBoundary(index = 1, reason = "speaker-change")
                        ),
                        prov = Provenance(source = "tingwu.preprocess", updatedAt = 123L)
                    )
                )
            )
        )
        val orchestrator = RealDebugOrchestrator(
            metaHub = metaHub,
            aiParaSettingsRepository = settingsRepository,
            tingwuTraceStore = TingwuTraceStore(),
            xfyunTraceStore = XfyunTraceStore(),
            dispatchers = DefaultDispatcherProvider,
        )

        val snapshot = orchestrator.getDebugSnapshot(
            sessionId = "s-1",
            jobId = "job-1",
            sessionTitle = "客户复盘",
            isTitleUserEdited = true
        )
        val section3 = snapshot.section3PreprocessedText
        assertTrue(section3.contains(
            "preprocess.source: metahub.m2.preprocess; " +
                "prov=tingwu.preprocess; updatedAt=123; version=1"
        ))
        val preprocessIndex = section3.indexOf("preprocess.source: metahub.m2.preprocess;")
        val previewIndex = section3.indexOf("tingwu.preview:")
        assertTrue(preprocessIndex in 0 until previewIndex)
        assertTrue(section3.contains("tingwu.preview:"))
        assertTrue(section3.contains("METAHUB_PREVIEW_LINE"))
        assertTrue(section3.contains("[Section3B: Tingwu Suspicious Boundaries]"))
        assertTrue(section3.contains("source: metahub.m2.preprocess"))
        assertTrue(section3.contains("count=2"))
        assertTrue(section3.contains("indices: 1,5"))
        assertTrue(section3.contains("details: 1(speaker-change),5(gap)"))
    }
}
