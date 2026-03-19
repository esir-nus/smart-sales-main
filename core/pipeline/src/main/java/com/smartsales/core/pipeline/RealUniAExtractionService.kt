package com.smartsales.core.pipeline

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uni-A 轻量语义提取服务。
 * 说明：只负责小模型调用 + Prompt/Linter 对齐，不做业务持久化。
 */
@Singleton
class RealUniAExtractionService @Inject constructor(
    private val executor: Executor,
    private val promptCompiler: PromptCompiler,
    private val schedulerLinter: SchedulerLinter
) {

    suspend fun extract(request: UniAExtractionRequest): FastTrackResult {
        val prompt = promptCompiler.compileUniAExtractionPrompt(request)
        return when (val result = executor.execute(ModelRegistry.EXTRACTOR, prompt)) {
            is ExecutorResult.Success -> schedulerLinter.parseUniAExtraction(
                input = result.content,
                unifiedId = request.unifiedId,
                transcript = request.transcript,
                nowIso = request.nowIso,
                timezone = request.timezone,
                displayedDateIso = request.displayedDateIso
            )
            is ExecutorResult.Failure -> {
                FastTrackResult.NoMatch(
                    reason = "Uni-A extractor failed: ${result.error}"
                )
            }
        }
    }
}
