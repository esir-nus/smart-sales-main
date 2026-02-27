package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.analyst.AnalystPipeline
import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.analyst.AnalystState
import com.smartsales.prism.domain.analyst.ArchitectService
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.model.Mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.smartsales.prism.domain.analyst.ConsultantService
import com.smartsales.prism.domain.analyst.EntityResolverService
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.pipeline.KernelWriteBack
import com.smartsales.prism.domain.pipeline.EntityRef


class RealAnalystPipeline @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor,
    private val architectService: ArchitectService,
    private val consultantService: ConsultantService,
    private val entityRepository: EntityRepository,
    private val entityResolverService: EntityResolverService,
    private val kernelWriteBack: KernelWriteBack
) : AnalystPipeline {

    private val TAG = "RealAnalystPipeline"

    private val _state = MutableStateFlow(AnalystState.IDLE)
    override val state = _state.asStateFlow()

    override suspend fun handleInput(
        input: String,
        sessionHistory: List<ChatTurn>
    ): AnalystResponse {
        Log.d(TAG, "handleInput: state=${_state.value} input=$input")

        return when (_state.value) {
            AnalystState.IDLE -> {
                _state.value = AnalystState.CONSULTING
                var loopCount = 0
                while (loopCount < 3) {
                    loopCount++
                    
                    val context = contextBuilder.build(input, Mode.ANALYST)
                    val consultantResult = consultantService.evaluateIntent(context)
                    if (consultantResult == null) {
                        _state.value = AnalystState.IDLE
                        return AnalystResponse.Chat("我没完全明白，能再详细说说你想分析的内容吗？")
                    }

                    if (consultantResult.missingEntities.isNotEmpty()) {
                        val candidates = entityRepository.getAll(limit = 100)
                        var anyResolved = false
                        var unresolvedEntity: String? = null
                        
                        for (missingEntity in consultantResult.missingEntities) {
                            val resolved = entityResolverService.resolve(missingEntity, candidates)
                            if (resolved != null) {
                                kernelWriteBack.updateEntityInSession(
                                    resolved.entityId, 
                                    EntityRef(resolved.entityId, resolved.displayName, resolved.entityType.name)
                                )
                                anyResolved = true
                            } else {
                                unresolvedEntity = missingEntity
                                break
                            }
                        }
                        
                        if (unresolvedEntity != null) {
                            _state.value = AnalystState.IDLE
                            return AnalystResponse.Chat("我是不是没有【$unresolvedEntity】的记录？")
                        }
                        
                        if (anyResolved) {
                            continue
                        }
                    }

                    if (!consultantResult.infoSufficient) {
                        _state.value = AnalystState.IDLE
                        return AnalystResponse.Chat(consultantResult.response)
                    } else {
                        // Phase 2 (Wave 3): Architect generates the plan
                        try {
                            val planResult = architectService.generatePlan(input, context, sessionHistory)
                            _state.value = AnalystState.PROPOSAL
                            return AnalystResponse.Plan(
                                title = planResult.title,
                                summary = planResult.summary,
                                markdownContent = planResult.markdownContent
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to generate plan from ArchitectService", e)
                            _state.value = AnalystState.IDLE
                            return AnalystResponse.Chat("生成计划失败，请重试。")
                        }
                    }
                }
                
                _state.value = AnalystState.IDLE
                return AnalystResponse.Chat("我尝试解析实体次数过多，暂时无法继续。")
            }
            AnalystState.PROPOSAL -> {
                // Phase 3 (Wave 3): Architect executes the investigation
                _state.value = AnalystState.INVESTIGATING
                try {
                    // Retrieve the actual markdown strategy from the history (the last Assistant chat turn which caused PROPOSAL)
                    val lastAssistantTurn = sessionHistory.lastOrNull { it.role == "assistant" }
                    val strategyMarkdown = lastAssistantTurn?.content ?: "未提供策略"

                    val planPayload = com.smartsales.prism.domain.analyst.PlanResult(
                        title = "待执行的分析策略",
                        summary = "根据确认的策略进行提取和分析",
                        markdownContent = strategyMarkdown
                    )

                    val context = contextBuilder.build(input, Mode.ANALYST)
                    val investigationResult = architectService.investigate(planPayload, context, sessionHistory)
                    
                    _state.value = AnalystState.RESULT
                    return AnalystResponse.Analysis(
                        content = investigationResult.analysisContent,
                        suggestedWorkflows = investigationResult.suggestedWorkflows
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to execute investigation from ArchitectService", e)
                    _state.value = AnalystState.IDLE
                    return AnalystResponse.Chat("深度调查执行失败，请重试。")
                }
            }
            AnalystState.RESULT -> {
                _state.value = AnalystState.IDLE
                return AnalystResponse.Chat("我知道了。")
            }
            else -> {
                Log.w(TAG, "Invalid statemachine state. Resetting.")
                _state.value = AnalystState.IDLE
                return AnalystResponse.Chat("状态机发生错误，已重置。")
            }
        }
    }
}
