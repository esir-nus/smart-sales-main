package com.smartsales.core.pipeline

import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.AliasCache
import com.smartsales.prism.domain.memory.CacheResult
import javax.inject.Inject
import javax.inject.Singleton
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.model.ClarificationType
import com.smartsales.prism.domain.model.CandidateOption
import com.smartsales.prism.domain.scheduler.ClarificationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Named
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.scheduler.FastTrackParser
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PluginGateway

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
    private val entityWriter: EntityWriter,
    private val aliasCache: AliasCache,
    private val fastTrackParser: FastTrackParser,
    private val taskRepository: ScheduledTaskRepository,
    private val toolRegistry: ToolRegistry,
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
        val context = contextBuilder.build(input, Mode.ANALYST, depth = ContextDepth.MINIMAL, isBadge = isVoice)
        
        // 🚦 VALVE: Track the raw input entering the OS
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.INPUT_RECEIVED,
            payloadSize = input.length,
            summary = "Raw user input received by Gatekeeper",
            rawDataDump = input
        )
        
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
                // 🚦 VALVE: Track the routing decision out of the OS
                PipelineValve.tag(
                    checkpoint = PipelineValve.Checkpoint.ROUTER_DECISION,
                    payloadSize = 0,
                    summary = "Routed to VAGUE (Clarification Requested)",
                    rawDataDump = "Classification: Vague"
                )
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
            else -> { 
                // SIMPLE_QA, DEEP_ANALYSIS, CRM_TASK, null → 继续往下走 System II pipeline
                // 🚦 VALVE: Track the routing decision out of the OS
                PipelineValve.tag(
                    checkpoint = PipelineValve.Checkpoint.ROUTER_DECISION,
                    payloadSize = 1,
                    summary = "Routed to System II Unified Pipeline",
                    rawDataDump = "Classification: ${routerResult?.queryQuality} | Entities: ${routerResult?.missingEntities}"
                )
            }
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
        
        var pathATaskId: String? = null
        
        // --- PATH A: Optimistic UI for Voice Tasks ---
        if (isVoice && (pipelineInput.intent == QueryQuality.CRM_TASK || pipelineInput.intent == QueryQuality.BADGE_DELEGATION)) {
            // Wave 17 T2: Restored Path A Native OS execution
            val optimisticTask = fastTrackParser.parseToOptimisticTask(unifiedId, input)
            pathATaskId = optimisticTask.id

            PipelineValve.tag(
                checkpoint = PipelineValve.Checkpoint.PATH_A_PARSED,
                payloadSize = input.length,
                summary = "Optimistic Task Parsed | Title: ${optimisticTask.title}",
                rawDataDump = optimisticTask.toString()
            )

            taskRepository.upsertTask(optimisticTask)

            PipelineValve.tag(
                checkpoint = PipelineValve.Checkpoint.PATH_A_DB_WRITTEN,
                payloadSize = optimisticTask.id.hashCode(),
                summary = "Optimistic Task Persisted (Hash ID)",
                rawDataDump = "TaskID: ${optimisticTask.id}"
            )

            emit(PipelineResult.PathACommitted(optimisticTask))
            android.util.Log.d("IntentOrchestrator", "Path A Native Fast-Track Executed for \$unifiedId")
        }
        
        // Delegate to the heavy-duty pipeline and forward its results (PATH B)
        unifiedPipeline.processInput(pipelineInput).collect { result ->
            // Intercept MutationProposals and ToolDispatch to cache them for Open-Loop confirmation
            if (result is PipelineResult.MutationProposal) {
                if (isVoice) {
                    // --- PATH B: Auto-Commit for Voice ---
                    appScope.launch {
                        result.profileMutations.forEach { mut ->
                            entityWriter.updateAttribute(mut.entityId, mut.field, mut.value)
                        }
                    }
                    // Leak 3 Fix: Silence the mutation proposal for voice so UI just shows 'done' from conversational reply
                    return@collect
                } else {
                    this@IntentOrchestrator.pendingProposal = result
                }
            } else if (result is PipelineResult.ToolDispatch) {
                if (isVoice) {
                    // --- PATH B: Auto-Commit for Voice Plugins ---
                    val request = PluginRequest(input, result.params)
                    val silentGateway = object : PluginGateway {
                        override suspend fun getSessionHistory(turns: Int) = ""
                        override suspend fun appendToHistory(message: String) {}
                        override suspend fun emitProgress(message: String) {}
                    }
                    appScope.launch {
                        toolRegistry.executeTool(result.toolId, request, silentGateway).collect {
                            // Run silently in background to complete the auto-commit
                        }
                    }
                    // Swallow ToolDispatch for voice so OS UI doesn't try to render it
                    return@collect
                } else if (result.toolId != "reschedule") {
                    this@IntentOrchestrator.pendingToolDispatch = result
                }
            } else if (isVoice && result is PipelineResult.DisambiguationIntercepted) {
                val taskId = pathATaskId
                if (taskId != null) {
                    val existing = taskRepository.getTask(taskId)
                    val awaitingClarification = result.uiState as? UiState.AwaitingClarification
                    val clarificationState = awaitingClarification?.let { uiState ->
                        ClarificationState.AmbiguousPerson(
                            question = uiState.question,
                            candidates = uiState.candidates.map { candidate ->
                                ClarificationState.PersonCandidate(
                                    entityId = candidate.entityId,
                                    displayName = candidate.displayName,
                                    description = candidate.description
                                )
                            }
                        )
                    } ?: ClarificationState.MissingInformation("需要进一步确认")
                    if (existing != null) {
                        taskRepository.upsertTask(existing.copy(clarificationState = clarificationState))
                    }
                }
                return@collect
            } else if (isVoice && result is PipelineResult.ClarificationNeeded) {
                val taskId = pathATaskId
                if (taskId != null) {
                    val existing = taskRepository.getTask(taskId)
                    if (existing != null) {
                        taskRepository.upsertTask(
                            existing.copy(
                                clarificationState = ClarificationState.MissingInformation(result.question)
                            )
                        )
                    }
                }
                return@collect
            }
            emit(result)
        }
    }
}
}
