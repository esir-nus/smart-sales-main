package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.AliasCache
import com.smartsales.prism.domain.memory.CacheResult
import javax.inject.Inject
import javax.inject.Singleton
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.model.ClarificationType
import com.smartsales.prism.domain.model.CandidateOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Named

/**
 * IntentOrchestrator (Phase 0 Gateway)
 * 
 * Sits at Layer 3 above the UnifiedPipeline and below the Presentation layer.
 * Evaluates intents using LightningRouter to short-circuit noise and greetings to the MascotService,
 * protecting the heavy UnifiedPipeline from processing conversational filler.
 * 
 * Wave 3: Houses the Open-Loop `PendingProposalStore` to bridge stateless pipeline evaluations
 * with stateful database write-backs.
 */
@Singleton
class IntentOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val lightningRouter: LightningRouter,
    private val mascotService: MascotService,
    private val unifiedPipeline: UnifiedPipeline,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val entityWriter: EntityWriter,
    private val aliasCache: AliasCache,
    @Named("AppScope") private val appScope: CoroutineScope
) {
    private var pendingProposal: PipelineResult.MutationProposal? = null
    private var pendingToolDispatch: PipelineResult.ToolDispatch? = null

    suspend fun processInput(input: String, isVoice: Boolean = false): Flow<PipelineResult> {
        return flow {
            // Wave 3 Open-Loop Protocol: Any new substantive input clears the pending state
            // to avoid committing stale LLM hallucinations if the user ignored a previous confirmation card.
            if (input != "确认执行") {
                pendingProposal = null
                pendingToolDispatch = null
            } else {
                // If it is a confirmation, execute the pending plan
                if (pendingToolDispatch != null) {
                    val dispatch = pendingToolDispatch!!
                    pendingToolDispatch = null
                    // Actually emit the ToolDispatch to the UI/PluginRegistry layer for execution
                    emit(dispatch) 
                    return@flow
                }
                
                val proposal = pendingProposal
                if (proposal == null) {
                    emit(PipelineResult.ConversationalReply("没有可执行的草案。"))
                    return@flow
                }
                
                // Execute actual database writes asynchronously so UI responds instantly
                appScope.launch {
                    proposal.task?.let { task ->
                        scheduledTaskRepository.insertTask(task)
                        scheduleBoard.refresh()
                    }
                    
                    proposal.profileMutations.forEach { mut ->
                        // Wave 3: Safe interaction with EntityWriter
                        entityWriter.updateAttribute(mut.entityId, mut.field, mut.value)
                    }
                }
                
                pendingProposal = null
                emit(PipelineResult.ConversationalReply("✅ 执行成功。"))
                return@flow
            }

        // Build minimal context for latency-sensitive phase 0 evaluation
        val context = contextBuilder.build(input, Mode.ANALYST, depth = ContextDepth.MINIMAL)
        val routerResult = lightningRouter.evaluateIntent(context)

        // BugFix 4.3/4.5: 短路拦截 — 只有非语音时才阻断 BADGE_DELEGATION
        // Kotlin when 不支持 fall-through，所以拦截逻辑提前处理
        when (routerResult?.queryQuality) {
            QueryQuality.NOISE, QueryQuality.GREETING -> {
                // Short-circuit to System I (Mascot) without blocking the flow
                appScope.launch {
                    mascotService.interact(MascotInteraction.Text(input))
                }
                emit(PipelineResult.MascotIntercepted)
                return@flow
            }
            QueryQuality.VAGUE -> {
                // Route back to user for clarification or immediate answer
                emit(PipelineResult.ConversationalReply(routerResult.response))
                return@flow
            }
            QueryQuality.BADGE_DELEGATION -> {
                if (!isVoice) {
                    // Wave 6: Hardware Delegation Enforcement — 非语音输入不走任务创建
                    emit(PipelineResult.BadgeDelegationIntercepted)
                    return@flow
                }
                // isVoice=true → 继续往下走 System II pipeline
            }
            else -> { /* SIMPLE_QA, DEEP_ANALYSIS, CRM_TASK, null → 继续往下走 System II pipeline */ }
        }

        // === System II Pipeline 入口 ===
        // 所有未被短路的 intent (BADGE_DELEGATION+voice, DEEP_ANALYSIS, etc.) 都走这里
        val missingList = routerResult?.missingEntities ?: emptyList()
        val cacheResult = aliasCache.match(missingList)
        
        var resolvedEntityId: String? = null
        when (cacheResult) {
            is CacheResult.Ambiguous -> {
                val uiCandidates = cacheResult.candidates.map { entry ->
                    CandidateOption(
                        entityId = entry.entityId,
                        displayName = entry.displayName,
                        description = entry.jobTitle
                    )
                }
                
                val uiState = UiState.AwaitingClarification(
                    question = "找到多个由于同名或别名冲突的实体，请选择：",
                    clarificationType = ClarificationType.AMBIGUOUS_PERSON,
                    candidates = uiCandidates
                )
                
                emit(PipelineResult.DisambiguationIntercepted(uiState))
                return@flow
            }
            is CacheResult.ExactMatch -> {
                resolvedEntityId = cacheResult.entityId
            }
            is CacheResult.Miss -> {
                // Proceed normally to System II
            }
        }
        
        val unifiedId = java.util.UUID.randomUUID().toString()
        android.util.Log.d("IntentOrchestrator", "processInput: Minted unifiedId=\$unifiedId for input=\$input")
        
        val pipelineInput = PipelineInput(
            rawText = input,
            isVoice = isVoice,
            isBadge = isVoice,
            intent = routerResult?.queryQuality ?: QueryQuality.DEEP_ANALYSIS,
            resolvedEntityId = resolvedEntityId,
            unifiedId = unifiedId
        )
        
        // Delegate to the heavy-duty pipeline and forward its results
        unifiedPipeline.processInput(pipelineInput).collect { result ->
            // Intercept MutationProposals and ToolDispatch to cache them for Open-Loop confirmation
            if (result is PipelineResult.MutationProposal) {
                this@IntentOrchestrator.pendingProposal = result
            } else if (result is PipelineResult.ToolDispatch && result.toolId != "reschedule") {
                this@IntentOrchestrator.pendingToolDispatch = result
            }
            emit(result)
        }
    }
}
}
