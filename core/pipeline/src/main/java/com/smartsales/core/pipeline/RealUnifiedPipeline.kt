package com.smartsales.core.pipeline

import android.util.Log
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
                    mode = Mode.ANALYST,
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

        // --- Wave 3: CRM Task Execution Route ---
        if (input.intent == QueryQuality.CRM_TASK) {
            
            // 🆕 LLM Execution Block
            emit(PipelineResult.Progress("正在进行深度分析..."))
            val prompt = promptCompiler.compile(enhancedContext)
            Log.d("RealUnifiedPipeline", "🤖 Executing LLM Scheduler Prompt...")
            val llmResult = executor.execute(LlmProfile.DEFAULT, prompt)
            
            val lintResult = when (llmResult) {
                is ExecutorResult.Success -> {
                    Log.d("RealUnifiedPipeline", "LLM Execution Success, parsing JSON payload.")
                    schedulerLinter.lint(llmResult.content)
                }
                is ExecutorResult.Failure -> {
                    Log.e("RealUnifiedPipeline", "LLM Execution Failed: ${llmResult.error}")
                    LintResult.Error("LLM 调用失败: ${llmResult.error}")
                }
            }
            
            val today = java.time.LocalDate.now()
            val zone = ZoneId.systemDefault()
            
            when (lintResult) {
                is LintResult.Success -> {
                    val taskId = if (input.replaceItemId != null) {
                        scheduledTaskRepository.updateTask(lintResult.task.copy(id = input.replaceItemId))
                        input.replaceItemId
                    } else {
                        scheduledTaskRepository.insertTask(lintResult.task)
                    }
                    
                    val person = lintResult.parsedClues.person
                    val company = lintResult.parsedClues.company
                    if (lintResult.urgencyLevel in listOf(UrgencyLevel.L1_CRITICAL, UrgencyLevel.L2_IMPORTANT) && 
                        person != null) {
                        try {
                            // 1. Create/find PERSON entity
                            val personResult = entityWriter.upsertFromClue(
                                clue = person,
                                resolvedId = null,
                                type = com.smartsales.prism.domain.memory.EntityType.PERSON,
                                source = "scheduler_pipeline"
                            )
                            
                            // 2 & 3. Create ACCOUNT and Link
                            if (company != null) {
                                val accountResult = entityWriter.upsertFromClue(
                                    clue = company,
                                    resolvedId = null,
                                    type = com.smartsales.prism.domain.memory.EntityType.ACCOUNT,
                                    source = "scheduler_pipeline"
                                )
                                entityWriter.updateProfile(
                                    entityId = personResult.entityId,
                                    updates = mapOf("accountId" to accountResult.entityId)
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("RealUnifiedPipeline", "Failed CRM Entity Linking", e)
                        }
                    }
                    
                    alarmScheduler.scheduleCascade(
                        taskId, lintResult.task.title, lintResult.task.startTime, lintResult.task.alarmCascade
                    )
                    
                    val taskDate = lintResult.task.startTime.atZone(zone).toLocalDate()
                    val dayOffset = ChronoUnit.DAYS.between(today, taskDate).toInt()
                    
                    scheduleBoard.refresh()
                    
                    emit(PipelineResult.SchedulerTaskCreated(
                        taskId = taskId,
                        title = lintResult.task.title,
                        dayOffset = dayOffset,
                        scheduledAtMillis = lintResult.task.startTime.toEpochMilli(),
                        durationMinutes = lintResult.task.durationMinutes,
                        isReschedule = input.replaceItemId != null
                    ))
                }
                is LintResult.MultiTask -> {
                    val createdTasks = mutableListOf<PipelineResult.SchedulerTaskCreated>()
                    var anyConflict = false
                    
                    lintResult.tasks.forEach { enrichedTask ->
                        if (scheduleBoard.checkConflict(
                                enrichedTask.startTime.toEpochMilli(), enrichedTask.durationMinutes, null
                            ) is com.smartsales.prism.domain.memory.ConflictResult.Conflict) {
                            anyConflict = true
                        }
                        
                        val taskId = scheduledTaskRepository.insertTask(enrichedTask)
                        
                        alarmScheduler.scheduleCascade(
                            taskId, enrichedTask.title, enrichedTask.startTime, enrichedTask.alarmCascade
                        )
                        
                        val taskDate = enrichedTask.startTime.atZone(zone).toLocalDate()
                        val dayOffset = ChronoUnit.DAYS.between(today, taskDate).toInt()
                        
                        createdTasks.add(PipelineResult.SchedulerTaskCreated(
                            taskId = taskId,
                            title = enrichedTask.title,
                            dayOffset = dayOffset,
                            scheduledAtMillis = enrichedTask.startTime.toEpochMilli(),
                            durationMinutes = enrichedTask.durationMinutes,
                            isReschedule = false
                        ))
                    }
                    
                    scheduleBoard.refresh()
                    emit(PipelineResult.SchedulerMultiTaskCreated(createdTasks, anyConflict))
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
                is LintResult.Inspiration -> {
                    val ts = System.currentTimeMillis()
                    inspirationRepository.insert(lintResult.content)
                    emit(PipelineResult.ConversationalReply("💡 灵感已保存"))
                }
                is LintResult.NonIntent -> {
                    emit(PipelineResult.ConversationalReply("无法将此请求识别为日程安排或灵感记录。"))
                }
                is LintResult.Error -> {
                    emit(PipelineResult.ConversationalReply("创建日程失败: ${lintResult.message}"))
                }
            }
        } else {
            // --- Wave 3: Analyst Execution Route ---
            emit(PipelineResult.ConversationalReply("Unified Pipeline ETL assembled successfully. Payload: $finalPayload"))
        }
    }
}
