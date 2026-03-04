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
import com.smartsales.prism.domain.disambiguation.EntityDisambiguationService
import com.smartsales.prism.domain.disambiguation.DisambiguationResult


class RealAnalystPipeline @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val executor: Executor,
    private val architectService: ArchitectService,
    private val entityDisambiguationService: EntityDisambiguationService
) : AnalystPipeline {

    private val TAG = "RealAnalystPipeline"

    private val _state = MutableStateFlow(AnalystState.IDLE)
    override val state = _state.asStateFlow()
    
    // Tracks which entity we yielded to the Disambiguator for, so we can override the prompt on resume
    private var _awaitingDisambiguationFor: String? = null

    override suspend fun handleInput(
        input: String,
        sessionHistory: List<ChatTurn>
    ): AnalystResponse {
        Log.d(TAG, "handleInput: state=${_state.value} input=$input")

        // Wave 1 Entity Disambiguation: Global Gateway (Interrupt & Resume)
        Log.d(TAG, "Checking Disambiguator State for Analyst input")
        when (val dResult = entityDisambiguationService.process(input)) {
            is DisambiguationResult.Intercepted -> {
                Log.d(TAG, "Analyst input intercepted by Disambiguator")
                return AnalystResponse.Chat("发现陌生的联系人信息，请先根据卡片提示补充档案。") // The UI actually renders the Disambiguator card, but Analyst just yields via chat.
            }
            is DisambiguationResult.Resumed -> {
                Log.d(TAG, "Disambiguator completed for Analyst, resuming original intent")
                // Replay the intent!
                return handleInput(dResult.originalInput, sessionHistory)
            }
            is DisambiguationResult.PassThrough -> {
                Log.d(TAG, "Disambiguator pass-through. Proceeding with Analyst normally.")
            }
        }

        return when (_state.value) {
            AnalystState.IDLE -> {
                // Phase 2 (Wave 3): Architect generates the plan
                // The pipeline assumes Phase 0 (Lightning Router) has already vetted this as a valid TASK/ANALYSIS priority.
                
                var loopCount = 0
                var promptOverride: String? = null
                
                if (_awaitingDisambiguationFor != null) {
                    promptOverride = "[系统提示]: 已知前文提到的 '$_awaitingDisambiguationFor' 的档案已由用户在前文中作为提示补充或提及，请基于当前对话上下文直接进行分析，绝对不要再返回 missing_entities 抱提示缺失此实体。"
                    Log.d(TAG, "Consuming disambiguation hint for $_awaitingDisambiguationFor")
                    _awaitingDisambiguationFor = null
                }
                
                while (loopCount < 3) {
                    loopCount++
                    val baseContext = contextBuilder.build(
                        userText = input, 
                        mode = Mode.ANALYST,
                        depth = com.smartsales.prism.domain.pipeline.ContextDepth.FULL
                    )
                    val context = if (promptOverride != null) {
                        baseContext.copy(systemPromptOverride = promptOverride)
                    } else {
                        baseContext
                    }
                    
                    try {
                        val planResult = architectService.generatePlan(input, context, sessionHistory)
                        
                        // Handle implicit entity missing dynamically during planning (if Architect exposes it)
                        // Or rely on EntityDisambiguationService Gateway at the top.
                        // For now, if the plan succeeds, we move to PROPOSAL.
                        
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
                return handleInput(input, sessionHistory)
            }
            else -> {
                Log.w(TAG, "Invalid statemachine state. Resetting.")
                _state.value = AnalystState.IDLE
                return AnalystResponse.Chat("状态机发生错误，已重置。")
            }
        }
    }
}
