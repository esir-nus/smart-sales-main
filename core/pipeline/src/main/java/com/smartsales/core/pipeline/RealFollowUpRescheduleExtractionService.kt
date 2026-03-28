package com.smartsales.core.pipeline

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 跟进改期 V2 影子提取服务。
 * 说明：只负责小模型调用 + Prompt/Linter 对齐，不做任何真实写入。
 */
@Singleton
class RealFollowUpRescheduleExtractionService @Inject constructor(
    private val executor: Executor,
    private val promptCompiler: PromptCompiler,
    private val schedulerLinter: SchedulerLinter
) {

    suspend fun extract(request: FollowUpRescheduleExtractionRequest): FollowUpRescheduleExtractionResult {
        val prompt = promptCompiler.compileFollowUpRescheduleExtractionPrompt(request)
        return when (val result = executor.execute(ModelRegistry.EXTRACTOR, prompt)) {
            is ExecutorResult.Success -> schedulerLinter.parseFollowUpRescheduleExtraction(
                input = result.content,
                transcript = request.transcript
            )

            is ExecutorResult.Failure -> {
                FollowUpRescheduleExtractionResult.Failure(
                    reason = "Follow-up reschedule V2 extractor failed: ${result.error}"
                )
            }
        }
    }
}
