package com.smartsales.core.pipeline

import android.util.Log
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uni-C 轻量语义提取服务。
 * 说明：只负责小模型调用 + Prompt/Linter 对齐，不做业务持久化。
 */
@Singleton
class RealUniCExtractionService @Inject constructor(
    private val executor: Executor,
    private val promptCompiler: PromptCompiler,
    private val schedulerLinter: SchedulerLinter
) {

    suspend fun extract(request: UniCExtractionRequest): FastTrackResult {
        val prompt = promptCompiler.compileUniCExtractionPrompt(request)
        return when (val result = executor.execute(ModelRegistry.EXTRACTOR, prompt)) {
            is ExecutorResult.Success -> {
                Log.d("UniCExtraction", "raw_json=${result.content}")
                val parsed = schedulerLinter.parseUniCExtraction(
                    input = result.content,
                    unifiedId = request.unifiedId,
                    transcript = request.transcript
                )
                when (parsed) {
                    is FastTrackResult.CreateInspiration -> {
                        Log.d(
                            "UniCExtraction",
                            "extracted_content_length=${parsed.params.content.length}"
                        )
                    }

                    is FastTrackResult.NoMatch -> {
                        Log.d("UniCExtraction", "extraction_rejected=${parsed.reason}")
                    }

                    else -> Unit
                }
                parsed
            }
            is ExecutorResult.Failure -> {
                FastTrackResult.NoMatch(
                    reason = "Uni-C extractor failed: ${result.error}"
                )
            }
        }
    }
}
