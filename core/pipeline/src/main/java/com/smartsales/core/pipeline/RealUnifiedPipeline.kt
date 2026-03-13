package com.smartsales.core.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.context.ContextDepth


import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.LlmProfile
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.pipeline.HabitListener
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import javax.inject.Named
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.LintResult
import com.smartsales.prism.domain.scheduler.UrgencyLevel

/**
 * Unified Pipeline (System II) Implementation
 * Replaces the legacy State-Machine/Redux pattern with a linear ETL flow.
 */
@Singleton
class RealUnifiedPipeline @Inject constructor(
    private val contextBuilder: ContextBuilder,
    private val entityDisambiguationService: EntityDisambiguationService,
    private val inputParserService: InputParserService,
    private val entityWriter: com.smartsales.prism.domain.memory.EntityWriter,
    private val schedulerLinter: SchedulerLinter,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val inspirationRepository: InspirationRepository,
    private val alarmScheduler: AlarmScheduler,
    // Wave 4: Synchronous Auto-Renaming
    private val sessionTitleGenerator: com.smartsales.prism.domain.session.SessionTitleGenerator,
    
    // Wave 3: Extracted LLM Execution
    private val promptCompiler: PromptCompiler,
    private val executor: Executor,
    private val telemetry: PipelineTelemetry,
    private val habitListener: HabitListener,
    @Named("AppScope") private val appScope: kotlinx.coroutines.CoroutineScope
) : UnifiedPipeline {
    
    override suspend fun processInput(input: PipelineInput): Flow<PipelineResult> = flow {
        Log.d("RealUnifiedPipeline", "🚀 Starting unified pipeline ETL for input: ${input.rawText}")
        telemetry.recordEvent(PipelinePhase.ROUTER, "Started processing input: ${input.rawText}")
        emit(PipelineResult.Progress("正在提取意图..."))
        
        // Wave 2: Semantic Disambiguation (Interrupt & Resume)
        val resolvedEntities = mutableListOf<String>()
        var resolvedInputText = input.rawText
        var generatedTitle: com.smartsales.prism.domain.session.TitleResult? = null // Wave 4

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
                    
                    // Wave 4: Generate title synchronously from the raw JSON payload
                    if (parseResult.rawParsedJson.isNotBlank()) {
                        generatedTitle = sessionTitleGenerator.generateTitle(
                            rawParsedJson = parseResult.rawParsedJson,
                            resolvedNames = parseResult.resolvedEntityIds // In reality we'd want names, but IDs are safer than nothing for debugging
                        )
                        Log.d("RealUnifiedPipeline", "Auto-Rename prepared via JSON: $generatedTitle")
                        generatedTitle?.clientName?.let { name ->
                            emit(PipelineResult.AutoRenameTriggered(name))
                        }
                    }
                }
            }
        }

        // Wave 1: Parallel Context Assembly (Extract-Transform-Load)
        emit(PipelineResult.Progress("正在梳理上下文..."))
        val contextStart = System.currentTimeMillis()
        val (enhancedContext, finalPayload) = coroutineScope {
            // Task 1: Fetch session context from Kernel (RAM)
            val contextDeferred = async {
                Log.d("RealUnifiedPipeline", "fetch: ContextBuilder enhanced context...")
                contextBuilder.build(
                    userText = resolvedInputText,
                    mode = if (input.intent == QueryQuality.CRM_TASK) Mode.SCHEDULER else Mode.ANALYST,
                    resolvedEntityIds = resolvedEntities, 
                    depth = input.requestedDepth
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
                
                // 1. Always extract and emit the conversational response first
                try {
                    val json = org.json.JSONObject(llmResult.content)
                    val responseText = json.optString("response", "")
                    if (responseText.isNotBlank()) {
                        emit(PipelineResult.ConversationalReply(responseText))
                    }
                } catch (e: Exception) {
                    Log.e("RealUnifiedPipeline", "Failed to parse Unified JSON, falling back to raw content", e)
                    emit(PipelineResult.ConversationalReply(llmResult.content))
                    return@flow
                }
                
                // 2. Lint for physical mutations (Tasks and CRM fields)
                val lintResult = schedulerLinter.lint(llmResult.content)
                when (lintResult) {
                    is LintResult.Success -> {
                        val mappedMutations = lintResult.profileMutations.map {
                            PipelineResult.ProfileMutation(it.entityId, it.field, it.value)
                        }
                        emit(PipelineResult.MutationProposal(task = lintResult.task, profileMutations = mappedMutations))
                    }
                    is LintResult.MultiTask -> {
                        val mappedMutations = lintResult.profileMutations.map {
                            PipelineResult.ProfileMutation(it.entityId, it.field, it.value)
                        }
                        // Emit the first task alongside the profile mutations
                        val firstTask = lintResult.tasks.firstOrNull()
                        if (firstTask != null) {
                            val anyConflict = scheduleBoard.checkConflict(
                                firstTask.startTime.toEpochMilli(), firstTask.durationMinutes, null
                            ) is com.smartsales.prism.domain.memory.ConflictResult.Conflict
                            emit(PipelineResult.MutationProposal(task = firstTask, profileMutations = mappedMutations, isConflict = anyConflict))
                        } else if (mappedMutations.isNotEmpty()) {
                            emit(PipelineResult.MutationProposal(profileMutations = mappedMutations))
                        }
                        
                        // Emit remaining tasks without duplicating profile mutations
                        lintResult.tasks.drop(1).forEach { enrichedTask ->
                            val conflict = scheduleBoard.checkConflict(
                                enrichedTask.startTime.toEpochMilli(), enrichedTask.durationMinutes, null
                            ) is com.smartsales.prism.domain.memory.ConflictResult.Conflict
                            emit(PipelineResult.MutationProposal(task = enrichedTask, profileMutations = emptyList(), isConflict = conflict))
                        }
                    }
                    is LintResult.Incomplete -> {
                        emit(PipelineResult.ClarificationNeeded(lintResult.question))
                    }
                    is LintResult.Deletion -> {
                        if (input.replaceItemId != null) {
                            scheduledTaskRepository.deleteItem(input.replaceItemId)
                            alarmScheduler.cancelReminder(input.replaceItemId)
                            scheduleBoard.refresh()
                            emit(PipelineResult.ConversationalReply("🗑️ 已删除任务"))
                        } else {
                            val matches = scheduleBoard.upcomingItems.value.filter { 
                                it.title.contains(lintResult.targetTitle, ignoreCase = true) 
                            }
                            if (matches.size == 1) {
                                scheduledTaskRepository.deleteItem(matches.first().entryId)
                                scheduleBoard.refresh()
                                emit(PipelineResult.ConversationalReply("🗑️ 已删除'${matches.first().title}'"))
                            } else {
                                emit(PipelineResult.ConversationalReply("未找到明确要删除的任务，请提供更具体的名字。"))
                            }
                        }
                    }
                    is LintResult.Reschedule -> {
                        val matches = scheduleBoard.upcomingItems.value.filter {
                            it.title.contains(lintResult.targetTitle, ignoreCase = true)
                        }
                        if (matches.size == 1) {
                            emit(PipelineResult.ToolDispatch("reschedule", mapOf("targetId" to matches.first().entryId, "instruction" to lintResult.newInstruction)))
                        } else {
                            emit(PipelineResult.ConversationalReply("找到多个匹配项或未找到匹配项，无法改期"))
                        }
                    }
                    is LintResult.NonIntent -> {
                        // Do nothing, just a normal reply which was already emitted above
                    }
                    is LintResult.Error -> {
                        val classification = try { org.json.JSONObject(llmResult.content).optString("classification", "non_intent") } catch (e: Exception) { "non_intent" }
                        if (classification != "non_intent") {
                            emit(PipelineResult.ConversationalReply("操作解析失败: ${lintResult.message}"))
                        }
                    }
                }
            }
            is ExecutorResult.Failure -> {
                Log.e("RealUnifiedPipeline", "LLM Execution Failed: ${llmResult.error}")
                emit(PipelineResult.ConversationalReply("分析失败: ${llmResult.error}"))
            }
        }
    }
}
