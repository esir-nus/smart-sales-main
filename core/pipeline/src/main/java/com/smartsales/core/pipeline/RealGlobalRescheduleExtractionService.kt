package com.smartsales.core.pipeline

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局改期提取服务。
 * 说明：只调用提取模型并返回目标线索 + 时间指令，最终命中仍由调度器看板负责。
 */
@Singleton
class RealGlobalRescheduleExtractionService @Inject constructor(
    private val executor: Executor,
    private val promptCompiler: PromptCompiler,
    private val schedulerLinter: SchedulerLinter
) {

    suspend fun extract(request: GlobalRescheduleExtractionRequest): GlobalRescheduleExtractionResult {
        val prompt = promptCompiler.compileGlobalRescheduleExtractionPrompt(request)
        return when (val result = executor.execute(ModelRegistry.EXTRACTOR, prompt)) {
            is ExecutorResult.Success -> {
                when (val parsed = schedulerLinter.parseGlobalRescheduleExtraction(result.content)) {
                    is GlobalRescheduleExtractionResult.Supported -> {
                        val shortlistedTaskIds = request.activeTaskShortlist.mapTo(linkedSetOf()) { it.taskId }
                        parsed
                            .copy(
                                target = TargetResolutionRequest(
                                    targetQuery = parsed.target.targetQuery,
                                    targetPerson = parsed.target.targetPerson,
                                    targetLocation = parsed.target.targetLocation
                                )
                            )
                            .filterByOwnership(shortlistedTaskIds)
                    }

                    else -> parsed
                }
            }

            is ExecutorResult.Failure -> {
                GlobalRescheduleExtractionResult.Failure(
                    reason = "Global reschedule extractor failed: ${result.error}"
                )
            }
        }
    }
}
