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
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.ClarificationState
import com.smartsales.prism.domain.scheduler.FastTrackResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Named
import com.smartsales.core.telemetry.PipelineValve

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
import com.smartsales.prism.domain.scheduler.FastTrackMutationEngine
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import com.smartsales.core.pipeline.ToolRegistry
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.prism.domain.time.TimeProvider

@Singleton
class IntentOrchestrator @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val lightningRouter: LightningRouter,
    private val mascotService: MascotService,
    private val unifiedPipeline: UnifiedPipeline,
    private val entityWriter: EntityWriter,
    private val aliasCache: AliasCache,
    private val uniAExtractionService: RealUniAExtractionService,
    private val uniBExtractionService: RealUniBExtractionService,
    private val uniCExtractionService: RealUniCExtractionService,
    private val fastTrackMutationEngine: FastTrackMutationEngine,
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val toolRegistry: ToolRegistry,
    private val timeProvider: TimeProvider,
    @Named("AppScope") private val appScope: CoroutineScope
) {
    private sealed interface PendingExecution {
        data class ProfileMutation(
            val mutations: List<PipelineResult.ProfileMutation>
        ) : PendingExecution

        data class SchedulerTask(
            val command: SchedulerTaskCommand
        ) : PendingExecution

        data class PluginDispatch(
            val toolId: String,
            val params: Map<String, Any>,
            val rawInput: String
        ) : PendingExecution
    }

    private var pendingExecution: PendingExecution? = null

    suspend fun processInput(
        input: String,
        isVoice: Boolean = false,
        displayedDateIso: String? = null
    ): Flow<PipelineResult> {
        return flow {
            // Wave 3 Open-Loop Protocol: Any new substantive input clears the pending state
            // to avoid committing stale LLM hallucinations if the user ignored a previous confirmation card.
            if (input != "确认执行") {
                pendingExecution = null
            } else {
                val execution = pendingExecution
                pendingExecution = null
                if (execution == null) {
                    emit(PipelineResult.ConversationalReply("没有可执行的草案。"))
                    return@flow
                }

                when (execution) {
                    is PendingExecution.PluginDispatch -> {
                        val canonicalToolId = PluginToolIds.canonicalize(execution.toolId)
                        val request = PluginRequest(execution.rawInput, execution.params)
                        val runtimeGateway = RuntimePluginGateway(
                            toolId = canonicalToolId,
                            contextBuilder = contextBuilder,
                            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
                        )
                        emit(PipelineResult.PluginExecutionStarted(canonicalToolId))
                        toolRegistry.executeTool(canonicalToolId, request, runtimeGateway).collect { state ->
                            emit(PipelineResult.PluginExecutionEmittedState(state))
                        }
                    }
                    is PendingExecution.ProfileMutation -> {
                        PipelineValve.tag(
                            checkpoint = PipelineValve.Checkpoint.MUTATION_COMMIT_REQUESTED,
                            payloadSize = execution.mutations.size,
                            summary = "Profile mutation commit requested",
                            rawDataDump = execution.mutations.joinToString("\n") { "${it.entityId}:${it.field}=${it.value}" }
                        )
                        withContext(Dispatchers.IO) {
                            execution.mutations.forEach { mut ->
                                entityWriter.updateAttribute(mut.entityId, mut.field, mut.value)
                            }
                        }
                        emit(PipelineResult.ConversationalReply("✅ 执行成功。"))
                    }
                    is PendingExecution.SchedulerTask -> {
                        PipelineValve.tag(
                            checkpoint = PipelineValve.Checkpoint.MUTATION_COMMIT_REQUESTED,
                            payloadSize = 1,
                            summary = "Scheduler task command commit requested",
                            rawDataDump = execution.command.toString()
                        )
                        val commitReply = withContext(Dispatchers.IO) {
                            executeSchedulerTaskCommand(execution.command)
                        }
                        emit(PipelineResult.ConversationalReply(commitReply))
                    }
                }
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

        // 🚦 VALVE: Track the routing decision out of the OS globally
        // SPEC COMPLIANCE (Wave 16 Audit): Payload size must represent the node count of the evaluated context, NOT a lazy boolean.
        val gatekeeperNodeCount = context.entityContext.size + context.sessionHistory.size + context.audioTranscripts.size
        
        val routeSummary = when (routerResult?.queryQuality) {
            QueryQuality.NOISE, QueryQuality.GREETING -> "Short-circuited to System I (Mascot)"
            QueryQuality.BADGE_DELEGATION -> if (!isVoice) "Intercepted: Hardware Delegation Enforcement" else "Routed to System II Unified Pipeline"
            else -> "Routed to System II Unified Pipeline"
        }
        
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.ROUTER_DECISION,
            payloadSize = gatekeeperNodeCount,
            summary = routeSummary,
            rawDataDump = "Classification: ${routerResult?.queryQuality} | Entities: ${routerResult?.missingEntities ?: "[]"}"
        )

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
        var attemptedUniA = false
        var attemptedUniB = false
        var attemptedUniC = false
        
        // --- PATH A: Bounded Uni-A attempt for surviving voice input ---
        if (isVoice) {
            attemptedUniA = true
            android.util.Log.d(
                "IntentOrchestrator",
                "Uni-A attempt started for $unifiedId with router=${pipelineInput.intent}"
            )
            val extractionRequest = UniAExtractionRequest(
                transcript = input,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = displayedDateIso
            )
            when (val fastTrackIntent = uniAExtractionService.extract(extractionRequest)) {
                is FastTrackResult.CreateTasks -> {
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.PATH_A_PARSED,
                        payloadSize = input.length,
                        summary = "Uni-A exact task parsed",
                        rawDataDump = fastTrackIntent.toString()
                    )
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.TASK_EXTRACTED,
                        payloadSize = input.length,
                        summary = "Uni-A/Uni-D exact task extracted",
                        rawDataDump = fastTrackIntent.toString()
                    )

                    when (val mutationResult = fastTrackMutationEngine.execute(fastTrackIntent)) {
                        is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                            val exactTaskId = mutationResult.taskIds.firstOrNull() ?: unifiedId
                            val exactTask = taskRepository.getTask(exactTaskId)
                            if (exactTask != null) {
                                pathATaskId = exactTask.id
                                PipelineValve.tag(
                                    checkpoint = PipelineValve.Checkpoint.CONFLICT_EVALUATED,
                                    payloadSize = exactTask.durationMinutes,
                                    summary = if (exactTask.hasConflict) {
                                        "Uni-D overlap detected"
                                    } else {
                                        "Uni-A conflict clear"
                                    },
                                    rawDataDump = exactTask.conflictSummary ?: exactTask.id
                                )
                                PipelineValve.tag(
                                    checkpoint = PipelineValve.Checkpoint.PATH_A_DB_WRITTEN,
                                    payloadSize = exactTask.id.hashCode(),
                                    summary = if (exactTask.hasConflict) {
                                        "Uni-D conflict-visible task persisted"
                                    } else {
                                        "Uni-A exact task persisted"
                                    },
                                    rawDataDump = "TaskID: ${exactTask.id}"
                                )
                                emit(PipelineResult.PathACommitted(exactTask))
                                android.util.Log.d("IntentOrchestrator", "Uni-A exact Path A committed for $unifiedId")
                            }
                        }
                        is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> {
                            android.util.Log.d(
                                "IntentOrchestrator",
                                "Uni-A exact create rejected for $unifiedId: ${mutationResult.reason}"
                            )
                        }
                        else -> Unit
                    }
                }
                is FastTrackResult.NoMatch -> {
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.PATH_A_PARSED,
                        payloadSize = input.length,
                        summary = "Uni-A exited without exact commit",
                        rawDataDump = fastTrackIntent.reason
                    )
                    android.util.Log.d(
                        "IntentOrchestrator",
                        "Uni-A exited NotExact for $unifiedId: ${fastTrackIntent.reason}"
                    )

                    attemptedUniB = true
                    android.util.Log.d(
                        "IntentOrchestrator",
                        "Uni-B attempt started for $unifiedId after Uni-A NotExact"
                    )
                    val vagueRequest = UniBExtractionRequest(
                        transcript = input,
                        nowIso = timeProvider.now.toString(),
                        timezone = timeProvider.zoneId.id,
                        unifiedId = unifiedId,
                        displayedDateIso = displayedDateIso
                    )
                    when (val vagueIntent = uniBExtractionService.extract(vagueRequest)) {
                        is FastTrackResult.CreateTasks -> {
                            PipelineValve.tag(
                                checkpoint = PipelineValve.Checkpoint.TASK_EXTRACTED,
                                payloadSize = input.length,
                                summary = "Uni-B explicit-clock exact task promoted",
                                rawDataDump = vagueIntent.toString()
                            )
                            when (val mutationResult = fastTrackMutationEngine.execute(vagueIntent)) {
                                is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                                    val exactTaskId = mutationResult.taskIds.firstOrNull() ?: unifiedId
                                    val exactTask = taskRepository.getTask(exactTaskId)
                                    if (exactTask != null) {
                                        pathATaskId = exactTask.id
                                        PipelineValve.tag(
                                            checkpoint = PipelineValve.Checkpoint.CONFLICT_EVALUATED,
                                            payloadSize = exactTask.durationMinutes,
                                            summary = if (exactTask.hasConflict) {
                                                "Uni-D overlap detected"
                                            } else {
                                                "Uni-A conflict clear"
                                            },
                                            rawDataDump = exactTask.conflictSummary ?: exactTask.id
                                        )
                                        PipelineValve.tag(
                                            checkpoint = PipelineValve.Checkpoint.PATH_A_DB_WRITTEN,
                                            payloadSize = exactTask.id.hashCode(),
                                            summary = if (exactTask.hasConflict) {
                                                "Uni-D conflict-visible task persisted"
                                            } else {
                                                "Uni-B explicit-clock exact task persisted"
                                            },
                                            rawDataDump = "TaskID: ${exactTask.id}"
                                        )
                                        emit(PipelineResult.PathACommitted(exactTask))
                                        android.util.Log.d("IntentOrchestrator", "Uni-B explicit-clock exact Path A committed for $unifiedId")
                                    }
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> {
                                    android.util.Log.d(
                                        "IntentOrchestrator",
                                        "Uni-B exact promotion rejected for $unifiedId: ${mutationResult.reason}"
                                    )
                                }
                                else -> Unit
                            }
                        }
                        is FastTrackResult.CreateVagueTask -> {
                            PipelineValve.tag(
                                checkpoint = PipelineValve.Checkpoint.TASK_EXTRACTED_VAGUE,
                                payloadSize = input.length,
                                summary = "Uni-B vague task parsed",
                                rawDataDump = vagueIntent.toString()
                            )
                            when (val mutationResult = fastTrackMutationEngine.execute(vagueIntent)) {
                                is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                                    val vagueTaskId = mutationResult.taskIds.firstOrNull() ?: unifiedId
                                    val vagueTask = taskRepository.getTask(vagueTaskId)
                                    if (vagueTask != null) {
                                        pathATaskId = vagueTask.id
                                        PipelineValve.tag(
                                            checkpoint = PipelineValve.Checkpoint.PATH_A_DB_WRITTEN,
                                            payloadSize = vagueTask.id.hashCode(),
                                            summary = "Uni-B vague task persisted",
                                            rawDataDump = "TaskID: ${vagueTask.id}"
                                        )
                                        emit(PipelineResult.PathACommitted(vagueTask))
                                        android.util.Log.d("IntentOrchestrator", "Uni-B vague Path A committed for $unifiedId")
                                    }
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> {
                                    android.util.Log.d(
                                        "IntentOrchestrator",
                                        "Uni-B vague create rejected for $unifiedId: ${mutationResult.reason}"
                                    )
                                }
                                else -> Unit
                            }
                        }
                        is FastTrackResult.NoMatch -> {
                            android.util.Log.d(
                                "IntentOrchestrator",
                                "Uni-B exited without vague commit for $unifiedId: ${vagueIntent.reason}"
                            )

                            attemptedUniC = true
                            android.util.Log.d(
                                "IntentOrchestrator",
                                "Uni-C attempt started for $unifiedId after Uni-B declined"
                            )
                            val inspirationRequest = UniCExtractionRequest(
                                transcript = input,
                                nowIso = timeProvider.now.toString(),
                                timezone = timeProvider.zoneId.id,
                                unifiedId = unifiedId
                            )
                            when (val inspirationIntent = uniCExtractionService.extract(inspirationRequest)) {
                                is FastTrackResult.CreateInspiration -> {
                                    PipelineValve.tag(
                                        checkpoint = PipelineValve.Checkpoint.THOUGHT_EXTRACTED,
                                        payloadSize = input.length,
                                        summary = "Uni-C inspiration parsed",
                                        rawDataDump = inspirationIntent.toString()
                                    )
                                    when (val mutationResult = fastTrackMutationEngine.execute(inspirationIntent)) {
                                        is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> {
                                            PipelineValve.tag(
                                                checkpoint = PipelineValve.Checkpoint.PATH_A_DB_WRITTEN,
                                                payloadSize = mutationResult.id.hashCode(),
                                                summary = "Uni-C inspiration persisted",
                                                rawDataDump = "InspirationID: ${mutationResult.id}"
                                            )
                                            emit(
                                                PipelineResult.InspirationCommitted(
                                                    id = mutationResult.id,
                                                    content = inspirationIntent.params.content
                                                )
                                            )
                                            android.util.Log.d("IntentOrchestrator", "Uni-C inspiration committed for $unifiedId")
                                            return@flow
                                        }
                                        is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> {
                                            android.util.Log.d(
                                                "IntentOrchestrator",
                                                "Uni-C inspiration rejected for $unifiedId: ${mutationResult.reason}"
                                            )
                                        }
                                        else -> Unit
                                    }
                                }
                                is FastTrackResult.NoMatch -> {
                                    android.util.Log.d(
                                        "IntentOrchestrator",
                                        "Uni-C exited without inspiration commit for $unifiedId: ${inspirationIntent.reason}"
                                    )
                                }
                                else -> Unit
                            }
                        }
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
        if ((attemptedUniA || attemptedUniB || attemptedUniC) && pathATaskId == null) {
            android.util.Log.d("IntentOrchestrator", "Path A produced no commit for $unifiedId; falling through to Path B")
        }
        
        // Delegate to the heavy-duty pipeline and forward its results (PATH B)
        unifiedPipeline.processInput(pipelineInput).collect { result ->
            // Intercept proposals and typed task commands to cache them for Open-Loop confirmation
            if (result is PipelineResult.MutationProposal) {
                if (isVoice) {
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.MUTATION_COMMIT_REQUESTED,
                        payloadSize = result.profileMutations.size,
                        summary = "Voice profile mutation auto-commit requested",
                        rawDataDump = result.profileMutations.joinToString("\n") { "${it.entityId}:${it.field}=${it.value}" }
                    )
                    appScope.launch(Dispatchers.IO) {
                        result.profileMutations.forEach { mut ->
                            entityWriter.updateAttribute(mut.entityId, mut.field, mut.value)
                        }
                    }
                    // Leak 3 Fix: Silence the mutation proposal for voice so UI just shows 'done' from conversational reply
                    return@collect
                } else {
                    pendingExecution = PendingExecution.ProfileMutation(result.profileMutations)
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.MUTATION_PROPOSAL_CACHED,
                        payloadSize = result.profileMutations.size,
                        summary = "Profile mutation proposal cached for confirmation",
                        rawDataDump = result.profileMutations.joinToString("\n") { "${it.entityId}:${it.field}=${it.value}" }
                    )
                }
            } else if (result is PipelineResult.TaskCommandProposal) {
                if (isVoice) {
                    if (pathATaskId != null) {
                        android.util.Log.d(
                            "IntentOrchestrator",
                            "Suppressing later-lane scheduler command for $unifiedId because Path A already committed task=$pathATaskId"
                        )
                        return@collect
                    }
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.TASK_COMMAND_ROUTED,
                        payloadSize = 1,
                        summary = "Voice scheduler command routed to owning executor",
                        rawDataDump = result.command.toString()
                    )
                    appScope.launch(Dispatchers.IO) {
                        executeSchedulerTaskCommand(result.command)
                    }
                    return@collect
                } else {
                    pendingExecution = PendingExecution.SchedulerTask(result.command)
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.MUTATION_PROPOSAL_CACHED,
                        payloadSize = 1,
                        summary = "Scheduler task command cached for confirmation",
                        rawDataDump = result.command.toString()
                    )
                }
            } else if (result is PipelineResult.ToolDispatch) {
                val canonicalToolId = PluginToolIds.canonicalize(result.toolId)
                if (isVoice) {
                    // --- PATH B: Auto-Commit for Voice Plugins ---
                    val request = PluginRequest(input, result.params)
                    val runtimeGateway = RuntimePluginGateway(
                        toolId = canonicalToolId,
                        contextBuilder = contextBuilder,
                        allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
                    )
                    emit(PipelineResult.PluginExecutionStarted(canonicalToolId))
                    toolRegistry.executeTool(canonicalToolId, request, runtimeGateway).collect { state ->
                        emit(PipelineResult.PluginExecutionEmittedState(state))
                    }
                    return@collect
                } else if (canonicalToolId != "reschedule") {
                    pendingExecution = PendingExecution.PluginDispatch(
                        toolId = canonicalToolId,
                        params = result.params,
                        rawInput = input
                    )
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.MUTATION_PROPOSAL_CACHED,
                        payloadSize = 1,
                        summary = "Plugin dispatch cached for confirmation",
                        rawDataDump = "$canonicalToolId:${result.params}"
                    )
                    emit(PipelineResult.ToolDispatchProposal(canonicalToolId, result.params))
                    return@collect
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

    private suspend fun executeSchedulerTaskCommand(command: SchedulerTaskCommand): String {
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.TASK_COMMAND_ROUTED,
            payloadSize = 1,
            summary = "Scheduler task command handed to owning executor",
            rawDataDump = command.toString()
        )
        return when (command) {
            is SchedulerTaskCommand.CreateTasks -> {
                when (val result = fastTrackMutationEngine.execute(FastTrackResult.CreateTasks(command.params))) {
                    is com.smartsales.prism.domain.scheduler.MutationResult.Success -> "✅ 日程已创建。"
                    is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> "未能创建日程：${result.reason}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.AmbiguousMatch -> "未找到唯一匹配的任务，请在面板手动处理。"
                    is com.smartsales.prism.domain.scheduler.MutationResult.Error -> "日程创建失败：${result.exception.message ?: "未知错误"}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> "当前命令不支持灵感写入。"
                }
            }
            is SchedulerTaskCommand.RescheduleTask -> {
                when (val result = fastTrackMutationEngine.execute(FastTrackResult.RescheduleTask(command.params))) {
                    is com.smartsales.prism.domain.scheduler.MutationResult.Success -> "✅ 日程已改期。"
                    is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> "未能改期：${result.reason}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.AmbiguousMatch -> "未找到唯一匹配的任务，请在面板手动处理。"
                    is com.smartsales.prism.domain.scheduler.MutationResult.Error -> "改期失败：${result.exception.message ?: "未知错误"}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> "当前命令不支持灵感写入。"
                }
            }
            is SchedulerTaskCommand.DeleteTask -> {
                val match = scheduleBoard.findLexicalMatch(command.targetTitle)
                    ?: return "未找到唯一匹配的任务，请在面板手动处理。"
                taskRepository.deleteItem(match.entryId)
                "✅ 日程已删除。"
            }
        }
    }
}
