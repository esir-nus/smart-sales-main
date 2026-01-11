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
import com.smartsales.data.aicore.params.InMemoryAiParaSettingsRepository
import com.smartsales.data.aicore.params.TranscriptionSettings
import com.smartsales.data.aicore.params.TRANSCRIPTION_PROVIDER_XFYUN
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
            pipelineTracer = FakePipelineTracer(),
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
        // M2B Observability lines
        assertTrue(snapshot.section1EffectiveRunText.contains("--- M2B TranscriptionDerivedState ---"))
        assertTrue(snapshot.section1EffectiveRunText.contains("m2b.chaptersCount:"))
        assertTrue(snapshot.section1EffectiveRunText.contains("m2b.keyPointsCount:"))
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
                        prov = Provenance(source = "tingwu.preprocess", updatedAt = 123L)
                    )
                )
            )
        )
        val orchestrator = RealDebugOrchestrator(
            metaHub = metaHub,
            aiParaSettingsRepository = settingsRepository,
            tingwuTraceStore = TingwuTraceStore(),
            pipelineTracer = FakePipelineTracer(),
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
    }

    @Test
    fun `snapshot prefers v1 window plan over derived batch plan`() = runTest {
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
                        prov = Provenance(source = "tingwu.preprocess", updatedAt = 123L)
                    )
                )
            )
        )
        val traceStore = TingwuTraceStore().apply {
            record(
                v1BatchPlanRule = "v1_windowed",
                v1BatchDurationMs = 600_000L,
                v1OverlapMs = 10_000L,
                v1BatchPlanTotalBatches = 3,
                v1BatchPlanCurrentBatchIndex = 2
            )
        }
        val orchestrator = RealDebugOrchestrator(
            metaHub = metaHub,
            aiParaSettingsRepository = settingsRepository,
            tingwuTraceStore = traceStore,
            pipelineTracer = FakePipelineTracer(),
            dispatchers = DefaultDispatcherProvider,
        )

        val snapshot = orchestrator.getDebugSnapshot(
            sessionId = "s-1",
            jobId = "job-1",
            sessionTitle = "客户复盘",
            isTitleUserEdited = true
        )
        val section3 = snapshot.section3PreprocessedText
        assertTrue(section3.contains("tingwu.batchPlan:"))
        assertTrue(section3.contains(
            "rule=v1_windowed; batchDurationMs=600000; overlapMs=10000; totalBatches=3; currentBatchIndex=2"
        ))
        assertFalse(section3.contains("rule=derived;"))
    }
}

// Fake PipelineTracer for testing
class FakePipelineTracer : com.smartsales.data.aicore.debug.PipelineTracer {
    override val enabled: Boolean = true
    private val _events = kotlinx.coroutines.flow.MutableStateFlow<List<com.smartsales.data.aicore.debug.PipelineEvent>>(emptyList())
    override val events: kotlinx.coroutines.flow.StateFlow<List<com.smartsales.data.aicore.debug.PipelineEvent>> = _events.asStateFlow()
    
    override fun emit(stage: com.smartsales.data.aicore.debug.PipelineStage, status: String, message: String) {
        val event = com.smartsales.data.aicore.debug.PipelineEvent(stage, status, message)
        _events.value = (_events.value + event).takeLast(50)
    }
    
    override fun clear() {
        _events.value = emptyList()
    }
}

