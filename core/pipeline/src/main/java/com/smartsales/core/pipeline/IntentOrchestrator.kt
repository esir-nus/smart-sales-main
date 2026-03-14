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

    suspend fun processInput(input: String): Flow<PipelineResult> {
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
            }
            QueryQuality.BADGE_DELEGATION -> {
                // Wave 6: Hardware Delegation Enforcement
                emit(PipelineResult.BadgeDelegationIntercepted)
                return@flow
            }
            else -> {
                // T1 Sync Loop: Fast-Fail Entity Disambiguation before hitting System II
                val missingList = routerResult?.missingEntities ?: emptyList()
                val cacheResult = aliasCache.match(missingList)
                
                var resolvedEntityId: String? = null
                when (cacheResult) {
                    is CacheResult.Ambiguous -> {
                        // HALT - Do not hit UnifiedPipeline
                        // Map the cache candidates directly into a UI State so the ViewModel doesn't need to know about EntityEntry
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
                        // Proceed normally to System II, it might handle creation or search
                    }
                }
                
                // System II Task (Deep Analysis, Simple QA, or Direct Task)
                val pipelineInput = PipelineInput(
                    rawText = input,
                    isVoice = false,
                    intent = routerResult?.queryQuality ?: QueryQuality.DEEP_ANALYSIS,
                    resolvedEntityId = resolvedEntityId
                )
                
                // Delegate to the heavy-duty pipeline and forward its results
                unifiedPipeline.processInput(pipelineInput).collect { result ->
                    // Intercept MutationProposals and ToolDispatch to cache them for Open-Loop confirmation
                    if (result is PipelineResult.MutationProposal) {
                        this@IntentOrchestrator.pendingProposal = result
                    } else if (result is PipelineResult.ToolDispatch && result.toolId != "reschedule") {
                        // "reschedule" is an internal legacy tool hack bypassing this block normally,
                        // but Vault IDs like GENERATE_PDF should require confirmation.
                        this@IntentOrchestrator.pendingToolDispatch = result
                    }
                    emit(result)
                }
            }
        }
    }
}
}
