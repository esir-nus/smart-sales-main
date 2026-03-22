package com.smartsales.core.pipeline

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.UniMExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMExtractionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uni-M 多任务拆解服务。
 * 说明：只负责小模型调用 + Prompt/Linter 对齐，不做业务持久化。
 */
@Singleton
class RealUniMExtractionService @Inject constructor(
    private val executor: Executor,
    private val promptCompiler: PromptCompiler,
    private val schedulerLinter: SchedulerLinter
) {

    suspend fun extract(request: UniMExtractionRequest): UniMExtractionResult {
        val prompt = promptCompiler.compileUniMExtractionPrompt(request)
        return when (val result = executor.execute(ModelRegistry.MULTI_EXTRACTOR, prompt)) {
            is ExecutorResult.Success -> schedulerLinter.parseUniMExtraction(result.content)
            is ExecutorResult.Failure -> {
                UniMExtractionResult.NotMulti(
                    reason = "Uni-M extractor failed: ${result.error}"
                )
            }
        }
    }
}
