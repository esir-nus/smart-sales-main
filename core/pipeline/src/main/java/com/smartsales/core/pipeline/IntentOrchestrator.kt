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
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.RescheduleTaskParams
import com.smartsales.prism.domain.scheduler.ScheduledTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
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
    @Named("AppScope") private val appScope: CoroutineScope,
    private val taskCreationBadgeSignal: TaskCreationBadgeSignal = TaskCreationBadgeSignal.NoOp,
    private val activeTaskRetrievalIndex: ActiveTaskRetrievalIndex? = null,
    private val uniMExtractionService: RealUniMExtractionService? = null,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService? = null
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

    private data class SchedulerTerminalCommit(
        val taskIds: LinkedHashSet<String>,
        val source: String
    ) {
        val primaryTaskId: String?
            get() = taskIds.lastOrNull()

        fun blocks(result: PipelineResult): Boolean {
            return when (result) {
                is PipelineResult.TaskCommandProposal -> true
                is PipelineResult.ToolDispatch ->
                    PluginToolIds.canonicalize(result.toolId) == "reschedule"
                is PipelineResult.ToolDispatchProposal ->
                    PluginToolIds.canonicalize(result.toolId) == "reschedule"
                else -> false
            }
        }
    }

    private data class VoiceSchedulerRoutingOutcome(
        val stopPipeline: Boolean,
        val terminalCommit: SchedulerTerminalCommit? = null
    )

    private val sharedSchedulerCreateInterpreter: SchedulerPathACreateInterpreter? by lazy {
        uniMExtractionService?.let { uniM ->
            SchedulerPathACreateInterpreter(
                uniMExtractionService = uniM,
                uniAExtractionService = uniAExtractionService,
                uniBExtractionService = uniBExtractionService,
                timeProvider = timeProvider
            )
        }
    }

    private val sharedSchedulerIntelligenceRouter: SchedulerIntelligenceRouter? by lazy {
        val createInterpreter = sharedSchedulerCreateInterpreter ?: return@lazy null
        val globalService = globalRescheduleExtractionService ?: return@lazy null
        SchedulerIntelligenceRouter(
            timeProvider = timeProvider,
            createInterpreter = createInterpreter,
            globalRescheduleExtractionService = globalService
        )
    }

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

        var schedulerTerminalCommit: SchedulerTerminalCommit? = null
        fun rememberSchedulerTerminalCommit(source: String, taskIds: List<String>) {
            val committedIds = LinkedHashSet(taskIds.filter { it.isNotBlank() })
            if (committedIds.isNotEmpty()) {
                schedulerTerminalCommit = SchedulerTerminalCommit(
                    taskIds = committedIds,
                    source = source
                )
            }
        }
        var attemptedUniA = false
        var attemptedUniB = false
        var attemptedUniC = false

        if (isVoice) {
            val routeOutcome = attemptSharedVoiceSchedulerRouting(
                input = input,
                displayedDateIso = displayedDateIso,
                unifiedId = unifiedId
            )
            if (routeOutcome.stopPipeline) {
                return@flow
            }
            routeOutcome.terminalCommit?.let { schedulerTerminalCommit = it }
        }

        // --- PATH A: Legacy bounded Uni-A cascade for environments that do not inject the shared router support ---
        if (
            isVoice &&
            schedulerTerminalCommit == null &&
            (sharedSchedulerIntelligenceRouter == null || activeTaskRetrievalIndex == null)
        ) {
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
                            taskCreationBadgeSignal.onTasksCreated()
                            val exactTaskId = mutationResult.taskIds.firstOrNull() ?: unifiedId
                            val exactTask = taskRepository.getTask(exactTaskId)
                            if (exactTask != null) {
                                rememberSchedulerTerminalCommit(
                                    source = "legacy_uni_a",
                                    taskIds = listOf(exactTask.id)
                                )
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
                                    taskCreationBadgeSignal.onTasksCreated()
                                    val exactTaskId = mutationResult.taskIds.firstOrNull() ?: unifiedId
                                    val exactTask = taskRepository.getTask(exactTaskId)
                                    if (exactTask != null) {
                                        rememberSchedulerTerminalCommit(
                                            source = "legacy_uni_b_exact",
                                            taskIds = listOf(exactTask.id)
                                        )
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
                                    taskCreationBadgeSignal.onTasksCreated()
                                    val vagueTaskId = mutationResult.taskIds.firstOrNull() ?: unifiedId
                                    val vagueTask = taskRepository.getTask(vagueTaskId)
                                    if (vagueTask != null) {
                                        rememberSchedulerTerminalCommit(
                                            source = "legacy_uni_b_vague",
                                            taskIds = listOf(vagueTask.id)
                                        )
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
        if ((attemptedUniA || attemptedUniB || attemptedUniC) && schedulerTerminalCommit == null) {
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
                    if (schedulerTerminalCommit?.blocks(result) == true) {
                        android.util.Log.d(
                            "IntentOrchestrator",
                            "Suppressing later-lane scheduler mutation for $unifiedId because Path A terminal owner=${schedulerTerminalCommit?.source} tasks=${schedulerTerminalCommit?.taskIds}"
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
                    if (schedulerTerminalCommit?.blocks(result) == true) {
                        android.util.Log.d(
                            "IntentOrchestrator",
                            "Suppressing later-lane scheduler tool dispatch for $unifiedId because Path A terminal owner=${schedulerTerminalCommit?.source} tasks=${schedulerTerminalCommit?.taskIds}"
                        )
                        return@collect
                    }
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
                val taskId = schedulerTerminalCommit?.primaryTaskId
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
                val taskId = schedulerTerminalCommit?.primaryTaskId
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

    private suspend fun FlowCollector<PipelineResult>.attemptSharedVoiceSchedulerRouting(
        input: String,
        displayedDateIso: String?,
        unifiedId: String
    ): VoiceSchedulerRoutingOutcome {
        val router = sharedSchedulerIntelligenceRouter ?: return VoiceSchedulerRoutingOutcome(stopPipeline = false)
        val retrievalIndex = activeTaskRetrievalIndex ?: return VoiceSchedulerRoutingOutcome(stopPipeline = false)

        val shortlist = if (
            router.mightExpressReschedule(input) ||
            router.looksLikeReplacementCancelTranscript(input)
        ) {
            retrievalIndex.buildShortlist(input)
        } else {
            emptyList()
        }

        return when (
            val decision = router.routeGeneral(
                SchedulerIntelligenceRouter.GeneralContext(
                    transcript = input,
                    surface = SchedulerIntelligenceRouter.SchedulerSurface.TOP_LEVEL_VOICE,
                    displayedDateIso = displayedDateIso,
                    activeTaskShortlist = shortlist
                )
            )
        ) {
            is SchedulerIntelligenceRouter.Decision.Create -> {
                val committed = when (val result = decision.result) {
                    is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                        commitVoiceSchedulerIntent(
                            intent = overrideUnifiedIdForVoice(result.intent, unifiedId),
                            source = "shared_${decision.metadata.owner.name.lowercase()}"
                        )
                    }

                    is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                        commitVoiceBatchSchedulerIntents(
                            intents = result.intents,
                            source = "shared_${decision.metadata.owner.name.lowercase()}"
                        )
                    }

                    else -> null
                }
                if (committed != null) {
                    for (task in committed.tasks) {
                        emit(PipelineResult.PathACommitted(task))
                    }
                }
                VoiceSchedulerRoutingOutcome(
                    stopPipeline = false,
                    terminalCommit = committed?.terminalCommit
                )
            }

            is SchedulerIntelligenceRouter.Decision.GlobalReschedule -> {
                handleSharedVoiceGlobalReschedule(
                    extracted = decision.extracted,
                    unifiedId = unifiedId
                )
            }

            is SchedulerIntelligenceRouter.Decision.Reject -> {
                emit(PipelineResult.ConversationalReply(decision.message))
                VoiceSchedulerRoutingOutcome(stopPipeline = true)
            }

            is SchedulerIntelligenceRouter.Decision.NotMatched -> {
                val inspirationRequest = UniCExtractionRequest(
                    transcript = input,
                    nowIso = timeProvider.now.toString(),
                    timezone = timeProvider.zoneId.id,
                    unifiedId = unifiedId
                )
                when (val inspirationIntent = uniCExtractionService.extract(inspirationRequest)) {
                    is FastTrackResult.CreateInspiration -> {
                        when (val mutationResult = fastTrackMutationEngine.execute(inspirationIntent)) {
                            is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> {
                                emit(
                                    PipelineResult.InspirationCommitted(
                                        id = mutationResult.id,
                                        content = inspirationIntent.params.content
                                    )
                                )
                                VoiceSchedulerRoutingOutcome(stopPipeline = true)
                            }

                            else -> VoiceSchedulerRoutingOutcome(stopPipeline = false)
                        }
                    }

                    else -> VoiceSchedulerRoutingOutcome(stopPipeline = false)
                }
            }

            is SchedulerIntelligenceRouter.Decision.FollowUpReschedule -> {
                VoiceSchedulerRoutingOutcome(stopPipeline = false)
            }
        }
    }

    private suspend fun FlowCollector<PipelineResult>.handleSharedVoiceGlobalReschedule(
        extracted: com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult.Supported,
        unifiedId: String
    ): VoiceSchedulerRoutingOutcome {
        val retrievalIndex = activeTaskRetrievalIndex
            ?: return VoiceSchedulerRoutingOutcome(stopPipeline = false)
        extracted.newTitle?.let { newTitle ->
            val task = when (
                val resolution = retrievalIndex.resolveTargetByClockAnchor(
                    clockCue = extracted.timeInstruction,
                    nowIso = timeProvider.now.toString(),
                    timezone = timeProvider.zoneId.id,
                    displayedDateIso = null
                )
            ) {
                is ActiveTaskResolveResult.Resolved -> {
                    taskRepository.getTask(resolution.taskId)
                        ?: run {
                            emit(PipelineResult.ConversationalReply("找不到要改名的日程。"))
                            return VoiceSchedulerRoutingOutcome(stopPipeline = true)
                        }
                }

                is ActiveTaskResolveResult.Ambiguous -> {
                    emit(PipelineResult.ConversationalReply("该时间存在多个日程，无法确定改名目标"))
                    return VoiceSchedulerRoutingOutcome(stopPipeline = true)
                }

                is ActiveTaskResolveResult.NoMatch -> {
                    emit(PipelineResult.ConversationalReply("未找到该时间的日程，无法改名"))
                    return VoiceSchedulerRoutingOutcome(stopPipeline = true)
                }
            }

            PipelineValve.tag(
                checkpoint = PipelineValve.Checkpoint.MUTATION_COMMIT_REQUESTED,
                payloadSize = 1,
                summary = "SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY",
                rawDataDump = "taskId=${task.id} clockCue=${extracted.timeInstruction}"
            )
            val command = FastTrackResult.RescheduleTask(
                params = RescheduleTaskParams(
                    unifiedId = unifiedId,
                    resolvedTaskId = task.id,
                    targetQuery = extracted.timeInstruction,
                    newTitle = newTitle
                )
            )
            val committed = commitVoiceSchedulerIntent(
                intent = command,
                source = "shared_global_time_anchor_retitle"
            )
            if (committed == null) {
                emit(PipelineResult.ConversationalReply("改名失败，请稍后重试。"))
                return VoiceSchedulerRoutingOutcome(stopPipeline = true)
            }
            for (committedTask in committed.tasks) {
                emit(PipelineResult.PathACommitted(committedTask, SchedulerCommitKind.RESCHEDULE))
            }
            return VoiceSchedulerRoutingOutcome(
                stopPipeline = false,
                terminalCommit = committed.terminalCommit
            )
        }
        val task = when (
            val resolution = retrievalIndex.resolveTarget(
                target = extracted.target,
                suggestedTaskId = extracted.suggestedTaskId
            )
        ) {
            is ActiveTaskResolveResult.Resolved -> {
                taskRepository.getTask(resolution.taskId)
                    ?: run {
                        emit(PipelineResult.ConversationalReply("找不到要改期的日程。"))
                        return VoiceSchedulerRoutingOutcome(stopPipeline = true)
                    }
            }

            is ActiveTaskResolveResult.Ambiguous -> {
                emit(PipelineResult.ConversationalReply("目标不明确，未执行改动。"))
                return VoiceSchedulerRoutingOutcome(stopPipeline = true)
            }

            is ActiveTaskResolveResult.NoMatch -> {
                emit(PipelineResult.ConversationalReply("未找到匹配的日程，请更具体一些。"))
                return VoiceSchedulerRoutingOutcome(stopPipeline = true)
            }
        }

        val resolvedTime = when (
            val timeResult = SchedulerRescheduleTimeInterpreter.resolveNaturalInstruction(
                originalTask = task,
                transcript = extracted.timeInstruction,
                displayedDateIso = task.startTime.atZone(timeProvider.zoneId).toLocalDate().toString(),
                timeProvider = timeProvider,
                uniAExtractionService = uniAExtractionService
            )
        ) {
            is SchedulerRescheduleTimeInterpreter.Result.Success -> timeResult
            SchedulerRescheduleTimeInterpreter.Result.Unsupported -> {
                emit(PipelineResult.ConversationalReply("当前仅支持明确时间改期，请直接说出新的时间。"))
                return VoiceSchedulerRoutingOutcome(stopPipeline = true)
            }

            SchedulerRescheduleTimeInterpreter.Result.InvalidExactTime -> {
                emit(PipelineResult.ConversationalReply("改期时间格式无法解析，请换一种明确说法。"))
                return VoiceSchedulerRoutingOutcome(stopPipeline = true)
            }
        }

        val command = FastTrackResult.RescheduleTask(
            params = RescheduleTaskParams(
                unifiedId = unifiedId,
                resolvedTaskId = task.id,
                targetQuery = extracted.target.targetQuery,
                newStartTimeIso = resolvedTime.startTime.toString(),
                newDurationMinutes = resolvedTime.durationMinutes ?: task.durationMinutes
            )
        )
        val committed = commitVoiceSchedulerIntent(
            intent = command,
            source = "shared_global_reschedule"
        )
        if (committed == null) {
            emit(PipelineResult.ConversationalReply("改期失败，请稍后重试。"))
            return VoiceSchedulerRoutingOutcome(stopPipeline = true)
        }
        for (committedTask in committed.tasks) {
            emit(PipelineResult.PathACommitted(committedTask, SchedulerCommitKind.RESCHEDULE))
        }
        return VoiceSchedulerRoutingOutcome(
            stopPipeline = false,
            terminalCommit = committed.terminalCommit
        )
    }

    private data class CommittedSchedulerTasks(
        val tasks: List<ScheduledTask>,
        val terminalCommit: SchedulerTerminalCommit
    )

    private suspend fun commitVoiceBatchSchedulerIntents(
        intents: List<FastTrackResult>,
        source: String
    ): CommittedSchedulerTasks? {
        val committedTasks = mutableListOf<ScheduledTask>()
        intents.forEach { intent ->
            val committed = commitVoiceSchedulerIntent(
                intent = intent,
                source = source
            )
            if (committed != null) {
                committedTasks += committed.tasks
            }
        }
        if (committedTasks.isEmpty()) return null
        return CommittedSchedulerTasks(
            tasks = committedTasks,
            terminalCommit = SchedulerTerminalCommit(
                taskIds = LinkedHashSet(committedTasks.map { it.id }),
                source = source
            )
        )
    }

    private suspend fun commitVoiceSchedulerIntent(
        intent: FastTrackResult,
        source: String
    ): CommittedSchedulerTasks? {
        return when (val mutationResult = fastTrackMutationEngine.execute(intent)) {
            is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                if (intent.isTaskCreationIntent()) {
                    taskCreationBadgeSignal.onTasksCreated()
                }
                val committedTasks = mutableListOf<ScheduledTask>()
                mutationResult.taskIds.forEach { taskId ->
                    taskRepository.getTask(taskId)?.let(committedTasks::add)
                }
                if (committedTasks.isEmpty()) {
                    null
                } else {
                    CommittedSchedulerTasks(
                        tasks = committedTasks,
                        terminalCommit = SchedulerTerminalCommit(
                            taskIds = LinkedHashSet(committedTasks.map { it.id }),
                            source = source
                        )
                    )
                }
            }

            else -> null
        }
    }

    private fun overrideUnifiedIdForVoice(
        intent: FastTrackResult,
        unifiedId: String
    ): FastTrackResult {
        return when (intent) {
            is FastTrackResult.CreateTasks -> FastTrackResult.CreateTasks(
                intent.params.copy(unifiedId = unifiedId)
            )

            is FastTrackResult.CreateVagueTask -> FastTrackResult.CreateVagueTask(
                intent.params.copy(unifiedId = unifiedId)
            )

            else -> intent
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
                    is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                        taskCreationBadgeSignal.onTasksCreated()
                        "✅ 日程已创建。"
                    }
                    is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> "未能创建日程：${result.reason}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.AmbiguousMatch -> "未找到唯一匹配的任务，请在面板手动处理。"
                    is com.smartsales.prism.domain.scheduler.MutationResult.Error -> "日程创建失败：${result.exception.message ?: "未知错误"}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> "当前命令不支持灵感写入。"
                }
            }
            is SchedulerTaskCommand.CreateVagueTask -> {
                when (val result = fastTrackMutationEngine.execute(FastTrackResult.CreateVagueTask(command.params))) {
                    is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                        taskCreationBadgeSignal.onTasksCreated()
                        "✅ 日程已创建。"
                    }
                    is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> "未能创建日程：${result.reason}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.AmbiguousMatch -> "未找到唯一匹配的任务，请在面板手动处理。"
                    is com.smartsales.prism.domain.scheduler.MutationResult.Error -> "日程创建失败：${result.exception.message ?: "未知错误"}"
                    is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> "当前命令不支持灵感写入。"
                }
            }
            is SchedulerTaskCommand.CreateBatch -> {
                var createdCount = 0
                var firstFailure: String? = null
                command.operations.forEach { operation ->
                    when (operation) {
                        is SchedulerTaskCommand.CreateOperation.Exact -> {
                            when (val result = fastTrackMutationEngine.execute(FastTrackResult.CreateTasks(operation.params))) {
                                is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                                    taskCreationBadgeSignal.onTasksCreated()
                                    createdCount += result.taskIds.size.coerceAtLeast(1)
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> {
                                    if (firstFailure == null) firstFailure = result.reason
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.AmbiguousMatch -> {
                                    if (firstFailure == null) firstFailure = "未找到唯一匹配的任务，请在面板手动处理。"
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.Error -> {
                                    if (firstFailure == null) {
                                        firstFailure = result.exception.message ?: "未知错误"
                                    }
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> {
                                    if (firstFailure == null) firstFailure = "当前命令不支持灵感写入。"
                                }
                            }
                        }
                        is SchedulerTaskCommand.CreateOperation.Vague -> {
                            when (val result = fastTrackMutationEngine.execute(FastTrackResult.CreateVagueTask(operation.params))) {
                                is com.smartsales.prism.domain.scheduler.MutationResult.Success -> {
                                    taskCreationBadgeSignal.onTasksCreated()
                                    createdCount += result.taskIds.size.coerceAtLeast(1)
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.NoMatch -> {
                                    if (firstFailure == null) firstFailure = result.reason
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.AmbiguousMatch -> {
                                    if (firstFailure == null) firstFailure = "未找到唯一匹配的任务，请在面板手动处理。"
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.Error -> {
                                    if (firstFailure == null) {
                                        firstFailure = result.exception.message ?: "未知错误"
                                    }
                                }
                                is com.smartsales.prism.domain.scheduler.MutationResult.InspirationCreated -> {
                                    if (firstFailure == null) firstFailure = "当前命令不支持灵感写入。"
                                }
                            }
                        }
                    }
                }
                when {
                    createdCount > 0 && firstFailure == null -> "✅ 已创建${createdCount}个日程。"
                    createdCount > 0 -> "已创建${createdCount}个日程，部分失败：$firstFailure"
                    firstFailure != null -> "未能创建日程：$firstFailure"
                    else -> "未能创建日程。"
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

    private fun FastTrackResult.isTaskCreationIntent(): Boolean {
        return this is FastTrackResult.CreateTasks || this is FastTrackResult.CreateVagueTask
    }
}
