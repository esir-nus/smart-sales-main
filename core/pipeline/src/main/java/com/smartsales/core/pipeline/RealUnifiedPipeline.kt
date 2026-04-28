package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.ContextBuilder
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.RescheduleTaskParams
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.LlmProfile
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.pipeline.HabitListener
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.core.telemetry.PipelineValve
import javax.inject.Named

/**
 * Unified Pipeline (System II) Implementation
 * Replaces the legacy State-Machine/Redux pattern with a linear ETL flow.
 */
@Singleton
class RealUnifiedPipeline @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val entityDisambiguationService: EntityDisambiguationService,
    private val inputParserService: InputParserService,
    private val schedulerLinter: SchedulerLinter,
    private val entityWriter: com.smartsales.prism.domain.memory.EntityWriter,
    // Wave 3: Extracted LLM Execution
    private val promptCompiler: PromptCompiler,
    private val executor: Executor,
    private val telemetry: PipelineTelemetry,
    private val habitListener: HabitListener,
    @Named("AppScope") private val appScope: kotlinx.coroutines.CoroutineScope,
    private val taskRepository: ScheduledTaskRepository? = null,
    private val activeTaskRetrievalIndex: ActiveTaskRetrievalIndex? = null,
    private val timeProvider: TimeProvider? = null,
    private val uniAExtractionService: RealUniAExtractionService? = null,
    private val uniBExtractionService: RealUniBExtractionService? = null,
    private val uniMExtractionService: RealUniMExtractionService? = null,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService? = null
) : UnifiedPipeline {
    private sealed interface SharedSchedulerCommandResult {
        data class Command(
            val command: SchedulerTaskCommand
        ) : SharedSchedulerCommandResult

        data object Reject : SharedSchedulerCommandResult

        data object NotMatchedOrUnavailable : SharedSchedulerCommandResult
    }

    private val sharedSchedulerCreateInterpreter: SchedulerPathACreateInterpreter? by lazy {
        val uniM = uniMExtractionService ?: return@lazy null
        val uniA = uniAExtractionService ?: return@lazy null
        val uniB = uniBExtractionService ?: return@lazy null
        val sharedTimeProvider = timeProvider ?: return@lazy null
        SchedulerPathACreateInterpreter(
            uniMExtractionService = uniM,
            uniAExtractionService = uniA,
            uniBExtractionService = uniB,
            timeProvider = sharedTimeProvider
        )
    }

    private val sharedSchedulerIntelligenceRouter: SchedulerIntelligenceRouter? by lazy {
        val createInterpreter = sharedSchedulerCreateInterpreter ?: return@lazy null
        val sharedTimeProvider = timeProvider ?: return@lazy null
        SchedulerIntelligenceRouter(
            timeProvider = sharedTimeProvider,
            createInterpreter = createInterpreter,
            globalRescheduleExtractionService = globalRescheduleExtractionService
        )
    }
    
    override suspend fun processInput(input: PipelineInput): Flow<PipelineResult> = flow {
        Log.d("RealUnifiedPipeline", "🚀 Starting unified pipeline ETL for input: ${input.rawText}")
        telemetry.recordEvent(PipelinePhase.ROUTER, "Started processing input: ${input.rawText}")
        emit(PipelineResult.Progress("正在提取意图..."))
        
        // Wave 2: Semantic Disambiguation (Interrupt & Resume)
        val resolvedEntities = mutableListOf<String>()
        var resolvedInputText = input.rawText

        when (val dResult = entityDisambiguationService.process(input.rawText)) {
            is DisambiguationResult.Intercepted -> {
                Log.d("RealUnifiedPipeline", "Input intercepted by Disambiguator")
                emit(PipelineResult.DisambiguationIntercepted(dResult.uiState))
                return@flow
            }
            is DisambiguationResult.Resolved -> {
                Log.d("RealUnifiedPipeline", "Disambiguation successful via explicit declaration. Writing to EntityWriter.")
                // 1. Pipeline writes entities natively
                val upsertResult = entityWriter.upsertFromClue(
                    clue = dResult.declaration.name,
                    resolvedId = null,
                    type = com.smartsales.prism.domain.memory.EntityType.PERSON, 
                    source = "disambiguation_pipeline"
                )
                
                // 2. Extrapolated profile info
                if (!dResult.declaration.jobTitle.isNullOrBlank() || !dResult.declaration.company.isNullOrBlank() || !dResult.declaration.notes.isNullOrBlank()) {
                    val profileUpdates = mutableListOf<String>()
                    dResult.declaration.jobTitle?.let { profileUpdates.add("职位: $it") }
                    dResult.declaration.company?.let { profileUpdates.add("公司: $it") }
                    dResult.declaration.notes?.let { profileUpdates.add("备注: $it") }
                    if (profileUpdates.isNotEmpty()) {
                        entityWriter.updateAttribute(upsertResult.entityId, "notes", profileUpdates.joinToString("\n"))
                    }
                }
                
                // 3. Register aliases
                if (dResult.declaration.aliases.isNotEmpty()) {
                    dResult.declaration.aliases.forEach { alias ->
                        entityWriter.registerAlias(upsertResult.entityId, alias)
                    }
                }
                
                resolvedEntities.add(upsertResult.entityId)
                resolvedInputText = dResult.originalInput
            }
            is DisambiguationResult.Resumed -> {
                Log.d("RealUnifiedPipeline", "Disambiguator completed organically, resuming original intent")
                resolvedInputText = dResult.originalInput
            }
            is DisambiguationResult.PassThrough -> {
                // Not in disambiguation mode. Let's parse normally.
                val parseResult = inputParserService.parseIntent(input.rawText)
                if (parseResult is ParseResult.NeedsClarification) {
                    Log.d("RealUnifiedPipeline", "Pipeline semantic check requires clarification")
                    val uiState = entityDisambiguationService.startDisambiguation(
                        originalInput = input.rawText,
                        originalMode = Mode.ANALYST,
                        ambiguousName = parseResult.ambiguousName,
                        candidates = parseResult.suggestedMatches
                    )
                    emit(PipelineResult.DisambiguationIntercepted(uiState))
                    return@flow
                }
                if (parseResult is ParseResult.EntityDeclaration) {
                    Log.d("RealUnifiedPipeline", "Proactive EntityDeclaration. Writing to EntityWriter directly.")
                    val upsertResult = entityWriter.upsertFromClue(
                        clue = parseResult.name,
                        resolvedId = null,
                        type = com.smartsales.prism.domain.memory.EntityType.PERSON,
                        source = "proactive_pipeline"
                    )
                    if (!parseResult.jobTitle.isNullOrBlank() || !parseResult.company.isNullOrBlank() || !parseResult.notes.isNullOrBlank()) {
                        val profileUpdates = mutableListOf<String>()
                        parseResult.jobTitle?.let { profileUpdates.add("职位: $it") }
                        parseResult.company?.let { profileUpdates.add("公司: $it") }
                        parseResult.notes?.let { profileUpdates.add("备注: $it") }
                        if (profileUpdates.isNotEmpty()) {
                            entityWriter.updateAttribute(upsertResult.entityId, "notes", profileUpdates.joinToString("\n"))
                        }
                    }
                    if (parseResult.aliases.isNotEmpty()) {
                        parseResult.aliases.forEach { alias ->
                            entityWriter.registerAlias(upsertResult.entityId, alias)
                        }
                    }
                    emit(PipelineResult.ConversationalReply("好的，已更新相关信息。"))
                    return@flow
                }
                if (parseResult is ParseResult.Success) {
                    resolvedEntities.addAll(parseResult.resolvedEntityIds)
                }
            }
        }
        
        // 🚦 VALVE: Track the Alias Resolution
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.ALIAS_RESOLUTION,
            payloadSize = resolvedEntities.size,
            summary = "Entity disambiguation/resolution completed",
            rawDataDump = "Resolved IDs: $resolvedEntities\nFinal Input: $resolvedInputText"
        )

        // Wave 1: Parallel Context Assembly (Extract-Transform-Load)
        emit(PipelineResult.Progress("正在梳理上下文..."))
        val contextStart = System.currentTimeMillis()
        val (enhancedContext, _) = coroutineScope {
            // Task 1: Fetch session context from Kernel (RAM)
            val contextDeferred = async {
                Log.d("RealUnifiedPipeline", "fetch: ContextBuilder enhanced context...")
                // Wave 5 T1: Inject the resolvedEntityId from the Gatekeeper if present
                if (input.resolvedEntityId != null && !resolvedEntities.contains(input.resolvedEntityId)) {
                    resolvedEntities.add(input.resolvedEntityId)
                }
                
                contextBuilder.build(
                    userText = resolvedInputText,
                    mode = if (input.intent == QueryQuality.CRM_TASK) Mode.SCHEDULER else Mode.ANALYST,
                    resolvedEntityIds = resolvedEntities, 
                    depth = input.requestedDepth,
                    isBadge = input.isBadge
                )
            }
            
            // Task 2: Future DB query placement (e.g., specific rules, user metadata)
            val metadataDeferred = async {
                Log.d("RealUnifiedPipeline", "fetch: User Metadata...")
                // Simulating an independent DB read to meet ETL architecture requirements
                "User Metadata: Active" 
            }
            
            val enhancedContext = contextDeferred.await()
            val metadata = metadataDeferred.await()
            
            Log.d("RealUnifiedPipeline", "Context Assembly Complete. History turns: ${enhancedContext.sessionHistory.size}")
            
            // 🚦 VALVE: Track the living RAM assembly before it enters the prompt compiler
            PipelineValve.tag(
                checkpoint = PipelineValve.Checkpoint.LIVING_RAM_ASSEMBLED,
                payloadSize = enhancedContext.sessionHistory.size + enhancedContext.entityContext.size,
                summary = "Living RAM assembled and ready for LLM",
                rawDataDump = "Mode: ${enhancedContext.modeMetadata.currentMode}\nEntities: ${enhancedContext.entityContext.keys}\nHistory Turns: ${enhancedContext.sessionHistory.size}"
            )
            
            Pair(enhancedContext, "Context [Mode: ${enhancedContext.modeMetadata.currentMode}, Metadata: $metadata]")
        }
        telemetry.recordEvent(PipelinePhase.CONTEXT_BUILDER, "Context Assemble Time: ${System.currentTimeMillis() - contextStart} ms")
        
        Log.d("RealUnifiedPipeline", "✅ ETL Phase finished. Proceeding to Execution.")
        
        // Background Path: Reinforcement Learning (Habit Listener)
        // Fire-and-forget; does not block the pipeline Flow.
        // Uses the App-level CoroutineScope so it survives if this Flow is cancelled.
        habitListener.analyzeAsync(input.rawText, enhancedContext, appScope)

        // --- Unified System II Execution Route ---
        emit(PipelineResult.Progress("正在深度分析与检索..."))
        val prompt = promptCompiler.compile(enhancedContext)
        Log.d("RealUnifiedPipeline", "🤖 Executing LLM Unified Prompt...")
        val llmResult = executor.execute(LlmProfile.DEFAULT, prompt)

        when (llmResult) {
            is ExecutorResult.Success -> {
                Log.d("RealUnifiedPipeline", "LLM Execution Success, parsing JSON payload.")
                
                // 🚦 VALVE: Track the raw JSON out of the LLM
                PipelineValve.tag(
                    checkpoint = PipelineValve.Checkpoint.LLM_BRAIN_EMISSION,
                    payloadSize = llmResult.content.length,
                    summary = "LLM emitted JSON payload",
                    rawDataDump = llmResult.content
                )
                
                // 2. Parse One Currency standard contract strictly
                try {
                    val jsonInterpreter = kotlinx.serialization.json.Json { 
                        ignoreUnknownKeys = true 
                        coerceInputValues = true 
                    }
                    val mutation = jsonInterpreter.decodeFromString(
                        com.smartsales.prism.domain.core.UnifiedMutation.serializer(),
                        llmResult.content
                    )
                    
                    val directPluginDispatch = mutation.pluginDispatch
                        ?.takeIf { it.toolId.isNotBlank() }
                        ?.let { dispatch ->
                            PipelineResult.ToolDispatch(
                                toolId = PluginToolIds.canonicalize(dispatch.toolId),
                                params = dispatch.parameters
                            )
                        }

                    val normalizedRecommendations = mutation.recommendedWorkflows.map { recommendation ->
                        recommendation.copy(
                            workflowId = PluginToolIds.canonicalize(recommendation.workflowId)
                        )
                    }

                    val legacySingleDispatchFallback = normalizedRecommendations
                        .singleOrNull()
                        ?.takeIf { directPluginDispatch == null && PluginToolIds.isDirectDispatchLane(it.workflowId) }
                        ?.let { recommendation ->
                            PipelineResult.ToolDispatch(
                                toolId = recommendation.workflowId,
                                params = recommendation.parameters
                            )
                        }

                    // 1. Extract and emit the conversational response strictly from the DTO
                    val responseText = mutation.response ?: ""
                    if (responseText.isNotBlank() &&
                        !shouldSuppressToolRoutingReply(
                            responseText = responseText,
                            directDispatch = directPluginDispatch ?: legacySingleDispatchFallback
                        )
                    ) {
                        emit(PipelineResult.ConversationalReply(responseText))
                    }
                    
                    // 🚦 VALVE: Track the strictly typed Linter output
                    PipelineValve.tag(
                        checkpoint = PipelineValve.Checkpoint.LINTER_DECODED,
                        payloadSize = mutation.profileMutations.size + mutation.tasks.size + mutation.recommendedWorkflows.size,
                        summary = "Linter successfully decoded strict data class",
                        rawDataDump = "Classification: ${mutation.classification}\nTasks: ${mutation.tasks.size}\nMutations: ${mutation.profileMutations.size}"
                    )
                    
                    val profileMutations = mutation.profileMutations.map {
                        PipelineResult.ProfileMutation(it.entityId, it.field, it.value)
                    }
                    if (profileMutations.isNotEmpty()) {
                        emit(PipelineResult.MutationProposal(profileMutations = profileMutations))
                    }
                    
                    when {
                        directPluginDispatch != null -> {
                            emit(directPluginDispatch)
                        }
                        legacySingleDispatchFallback != null -> {
                            emit(legacySingleDispatchFallback)
                        }
                        normalizedRecommendations.isNotEmpty() -> {
                            emit(PipelineResult.ToolRecommendation(normalizedRecommendations))
                        }
                    }
                    
                    buildSchedulerTaskCommand(
                        input = input,
                        mutation = mutation,
                        rawJson = llmResult.content,
                        unifiedId = input.unifiedId
                    )?.let { command ->
                        PipelineValve.tag(
                            checkpoint = PipelineValve.Checkpoint.TASK_COMMAND_EMITTED,
                            payloadSize = 1,
                            summary = "Unified pipeline emitted typed scheduler command",
                            rawDataDump = command.toString()
                        )
                        emit(PipelineResult.TaskCommandProposal(command))
                    }
                } catch (e: Exception) {
                    Log.e("RealUnifiedPipeline", "Failed to decode UnifiedMutation", e)
                    emit(PipelineResult.ConversationalReply("操作解析失败：系统未能理解解析指令格式，请重试。（${e.message}）"))
                }
            }
            is ExecutorResult.Failure -> {
                Log.e("RealUnifiedPipeline", "LLM Execution Failed: ${llmResult.error}")
                emit(PipelineResult.ConversationalReply("分析失败: ${llmResult.error}"))
            }
        }
    }

    private suspend fun buildSchedulerTaskCommand(
        input: PipelineInput,
        mutation: com.smartsales.prism.domain.core.UnifiedMutation,
        rawJson: String,
        unifiedId: String
    ): SchedulerTaskCommand? {
        return when (mutation.classification.lowercase()) {
            "schedulable",
            "reschedule" -> when (val shared = buildSharedSchedulerTaskCommand(input, unifiedId)) {
                is SharedSchedulerCommandResult.Command -> shared.command
                SharedSchedulerCommandResult.Reject -> null
                SharedSchedulerCommandResult.NotMatchedOrUnavailable -> {
                    buildLegacySchedulerTaskCommand(
                        mutation = mutation,
                        rawJson = rawJson,
                        unifiedId = unifiedId
                    )
                }
            }
            "deletion" -> mutation.targetTitle
                ?.takeIf { it.isNotBlank() }
                ?.let { SchedulerTaskCommand.DeleteTask(it) }
            else -> null
        }
    }

    private suspend fun buildSharedSchedulerTaskCommand(
        input: PipelineInput,
        unifiedId: String
    ): SharedSchedulerCommandResult {
        val router = sharedSchedulerIntelligenceRouter ?: return SharedSchedulerCommandResult.NotMatchedOrUnavailable
        val shortlist = if (router.mightExpressReschedule(input.rawText)) {
            activeTaskRetrievalIndex?.buildShortlist(input.rawText).orEmpty()
        } else {
            emptyList()
        }
        return when (
            val decision = router.routeGeneral(
                SchedulerIntelligenceRouter.GeneralContext(
                    transcript = input.rawText,
                    surface = SchedulerIntelligenceRouter.SchedulerSurface.PATH_B_TEXT,
                    displayedDateIso = null,
                    activeTaskShortlist = shortlist
                )
            )
        ) {
            is SchedulerIntelligenceRouter.Decision.Create -> {
                buildCreateCommandFromDecision(decision, unifiedId)
                    ?.let(SharedSchedulerCommandResult::Command)
                    ?: SharedSchedulerCommandResult.NotMatchedOrUnavailable
            }
            is SchedulerIntelligenceRouter.Decision.GlobalReschedule -> {
                buildSharedRescheduleCommand(
                    extracted = decision.extracted,
                    unifiedId = unifiedId
                )
            }
            is SchedulerIntelligenceRouter.Decision.NotMatched -> SharedSchedulerCommandResult.NotMatchedOrUnavailable
            is SchedulerIntelligenceRouter.Decision.Reject,
            is SchedulerIntelligenceRouter.Decision.FollowUpReschedule -> SharedSchedulerCommandResult.Reject
        }
    }

    private fun buildCreateCommandFromDecision(
        decision: SchedulerIntelligenceRouter.Decision.Create,
        unifiedId: String
    ): SchedulerTaskCommand? {
        return when (val result = decision.result) {
            is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                fastTrackCreateToCommand(result.intent, unifiedId)
            }
            is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                val operations = result.intents.mapNotNull { intent ->
                    fastTrackCreateToOperation(intent, unifiedId)
                }
                if (operations.isEmpty()) {
                    null
                } else {
                    val exactOperations = operations.filterIsInstance<SchedulerTaskCommand.CreateOperation.Exact>()
                    when {
                        operations.size == 1 -> operationToCommand(operations.single())
                        exactOperations.size == operations.size -> {
                            SchedulerTaskCommand.CreateTasks(
                                CreateTasksParams(
                                    unifiedId = unifiedId,
                                    tasks = exactOperations.flatMap { it.params.tasks }
                                )
                            )
                        }
                        else -> SchedulerTaskCommand.CreateBatch(operations)
                    }
                }
            }
            is SchedulerPathACreateInterpreter.Result.DirectFailure,
            is SchedulerPathACreateInterpreter.Result.NotMatched -> null
        }
    }

    private suspend fun buildSharedRescheduleCommand(
        extracted: com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult.Supported,
        unifiedId: String
    ): SharedSchedulerCommandResult {
        val retrievalIndex = activeTaskRetrievalIndex ?: return SharedSchedulerCommandResult.NotMatchedOrUnavailable
        val tasks = taskRepository ?: return SharedSchedulerCommandResult.NotMatchedOrUnavailable
        val sharedTimeProvider = timeProvider ?: return SharedSchedulerCommandResult.NotMatchedOrUnavailable
        val uniA = uniAExtractionService ?: return SharedSchedulerCommandResult.NotMatchedOrUnavailable

        extracted.newTitle?.let { newTitle ->
            val task = when (
                val resolution = retrievalIndex.resolveTargetByClockAnchor(
                    clockCue = extracted.timeInstruction,
                    nowIso = sharedTimeProvider.now.toString(),
                    timezone = sharedTimeProvider.zoneId.id,
                    displayedDateIso = null
                )
            ) {
                is ActiveTaskResolveResult.Resolved -> tasks.getTask(resolution.taskId)
                is ActiveTaskResolveResult.Ambiguous,
                is ActiveTaskResolveResult.NoMatch -> return SharedSchedulerCommandResult.Reject
            } ?: return SharedSchedulerCommandResult.Reject

            return SharedSchedulerCommandResult.Command(
                SchedulerTaskCommand.RescheduleTask(
                    RescheduleTaskParams(
                        unifiedId = unifiedId,
                        resolvedTaskId = task.id,
                        targetQuery = extracted.timeInstruction,
                        newTitle = newTitle
                    )
                )
            )
        }

        val task = when (
            val resolution = retrievalIndex.resolveTarget(
                target = extracted.target,
                suggestedTaskId = extracted.suggestedTaskId
            )
        ) {
            is ActiveTaskResolveResult.Resolved -> tasks.getTask(resolution.taskId)
            is ActiveTaskResolveResult.Ambiguous,
            is ActiveTaskResolveResult.NoMatch -> return SharedSchedulerCommandResult.Reject
        } ?: return SharedSchedulerCommandResult.Reject

        val resolvedTime = when (
            val timeResult = SchedulerRescheduleTimeInterpreter.resolveNaturalInstruction(
                originalTask = task,
                transcript = extracted.timeInstruction,
                displayedDateIso = task.startTime.atZone(sharedTimeProvider.zoneId).toLocalDate().toString(),
                timeProvider = sharedTimeProvider,
                uniAExtractionService = uniA
            )
        ) {
            is SchedulerRescheduleTimeInterpreter.Result.Success -> timeResult
            SchedulerRescheduleTimeInterpreter.Result.InvalidExactTime,
            SchedulerRescheduleTimeInterpreter.Result.Unsupported -> return SharedSchedulerCommandResult.Reject
        }

        return SharedSchedulerCommandResult.Command(
            SchedulerTaskCommand.RescheduleTask(
                RescheduleTaskParams(
                    unifiedId = unifiedId,
                    resolvedTaskId = task.id,
                    targetQuery = extracted.target.targetQuery,
                    newStartTimeIso = resolvedTime.startTime.toString(),
                    newDurationMinutes = resolvedTime.durationMinutes ?: task.durationMinutes
                )
            )
        )
    }

    private fun buildLegacySchedulerTaskCommand(
        mutation: com.smartsales.prism.domain.core.UnifiedMutation,
        rawJson: String,
        unifiedId: String
    ): SchedulerTaskCommand? {
        return when (mutation.classification.lowercase()) {
            "schedulable" -> when (val parsed = schedulerLinter.parseFastTrackIntent(rawJson)) {
                is FastTrackResult.CreateTasks -> {
                    SchedulerTaskCommand.CreateTasks(
                        parsed.params.copy(unifiedId = unifiedId)
                    )
                }
                is FastTrackResult.CreateVagueTask -> {
                    SchedulerTaskCommand.CreateVagueTask(
                        parsed.params.copy(unifiedId = unifiedId)
                    )
                }
                else -> null
            }
            "reschedule" -> when (val parsed = schedulerLinter.parseFastTrackIntent(rawJson)) {
                is FastTrackResult.RescheduleTask -> {
                    SchedulerTaskCommand.RescheduleTask(
                        parsed.params.copy(unifiedId = unifiedId)
                    )
                }
                else -> null
            }
            else -> null
        }
    }

    private fun fastTrackCreateToCommand(
        intent: FastTrackResult,
        unifiedId: String
    ): SchedulerTaskCommand? {
        return fastTrackCreateToOperation(intent, unifiedId)?.let(::operationToCommand)
    }

    private fun operationToCommand(
        operation: SchedulerTaskCommand.CreateOperation
    ): SchedulerTaskCommand {
        return when (operation) {
            is SchedulerTaskCommand.CreateOperation.Exact -> SchedulerTaskCommand.CreateTasks(operation.params)
            is SchedulerTaskCommand.CreateOperation.Vague -> SchedulerTaskCommand.CreateVagueTask(operation.params)
        }
    }

    private fun fastTrackCreateToOperation(
        intent: FastTrackResult,
        unifiedId: String
    ): SchedulerTaskCommand.CreateOperation? {
        return when (intent) {
            is FastTrackResult.CreateTasks -> {
                SchedulerTaskCommand.CreateOperation.Exact(
                    params = intent.params.copy(unifiedId = unifiedId)
                )
            }
            is FastTrackResult.CreateVagueTask -> {
                SchedulerTaskCommand.CreateOperation.Vague(
                    params = intent.params.copy(unifiedId = unifiedId)
                )
            }
            else -> null
        }
    }

    private fun shouldSuppressToolRoutingReply(
        responseText: String,
        directDispatch: PipelineResult.ToolDispatch?
    ): Boolean {
        if (directDispatch == null) return false
        return responseText == "好的，我为您找到了相关工具，请确认。" ||
            responseText == "好的，我已为您起草工具执行。"
    }
}
