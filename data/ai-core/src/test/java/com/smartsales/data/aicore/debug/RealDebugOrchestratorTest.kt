// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt
// 模块：:data:ai-core
// 说明：验证 DebugOrchestrator 生成三段 HUD 文本的基础结构
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.debug

import com.smartsales.core.util.DefaultDispatcherProvider
import com.smartsales.core.metahub.InMemoryMetaHub
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
}
