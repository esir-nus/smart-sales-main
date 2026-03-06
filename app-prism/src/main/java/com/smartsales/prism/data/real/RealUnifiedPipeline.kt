package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.unifiedpipeline.PipelineInput
import com.smartsales.prism.domain.unifiedpipeline.PipelineResult
import com.smartsales.prism.domain.unifiedpipeline.UnifiedPipeline
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.smartsales.prism.domain.scheduler.SchedulerLinter
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
    private val entityDisambiguationService: com.smartsales.prism.domain.disambiguation.EntityDisambiguationService,
    private val inputParserService: com.smartsales.prism.domain.parser.InputParserService,
    private val entityWriter: com.smartsales.prism.domain.memory.EntityWriter,
    private val schedulerLinter: SchedulerLinter,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val inspirationRepository: InspirationRepository,
    private val alarmScheduler: AlarmScheduler,
    // Wave 4: Synchronous Auto-Renaming
    private val sessionTitleGenerator: com.smartsales.prism.domain.session.SessionTitleGenerator
) : UnifiedPipeline {
    
    override suspend fun processInput(input: PipelineInput): Flow<PipelineResult> = flow {
        Log.d("RealUnifiedPipeline", "🚀 Starting unified pipeline ETL for input: ${input.rawText}")
        
        // Wave 2: Semantic Disambiguation (Interrupt & Resume)
        val resolvedEntities = mutableListOf<String>()
        var resolvedInputText = input.rawText
        var generatedTitle: com.smartsales.prism.domain.session.TitleResult? = null // Wave 4

        when (val dResult = entityDisambiguationService.process(input.rawText)) {
            is com.smartsales.prism.domain.disambiguation.DisambiguationResult.Intercepted -> {
                Log.d("RealUnifiedPipeline", "Input intercepted by Disambiguator")
                emit(PipelineResult.DisambiguationIntercepted(dResult.uiState))
                return@flow
            }
            is com.smartsales.prism.domain.disambiguation.DisambiguationResult.Resolved -> {
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
                        entityWriter.updateProfile(upsertResult.entityId, mapOf("notes" to profileUpdates.joinToString("\n")))
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
            is com.smartsales.prism.domain.disambiguation.DisambiguationResult.Resumed -> {
                Log.d("RealUnifiedPipeline", "Disambiguator completed organically, resuming original intent")
                resolvedInputText = dResult.originalInput
            }
            is com.smartsales.prism.domain.disambiguation.DisambiguationResult.PassThrough -> {
                // Not in disambiguation mode. Let's parse normally.
                val parseResult = inputParserService.parseIntent(input.rawText)
                if (parseResult is com.smartsales.prism.domain.parser.ParseResult.NeedsClarification) {
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
                if (parseResult is com.smartsales.prism.domain.parser.ParseResult.Success) {
                    resolvedEntities.addAll(parseResult.resolvedEntityIds)
                    
                    // Wave 4: Generate title synchronously from the raw JSON payload
                    if (parseResult.rawParsedJson.isNotBlank()) {
                        generatedTitle = sessionTitleGenerator.generateTitle(
                            rawParsedJson = parseResult.rawParsedJson,
                            resolvedNames = parseResult.resolvedEntityIds // In reality we'd want names, but IDs are safer than nothing for debugging
                        )
                        Log.d("RealUnifiedPipeline", "Auto-Rename prepared via JSON: $generatedTitle")
                        emit(PipelineResult.AutoRenameTriggered(generatedTitle!!.clientName))
                    }
                }
            }
        }

        // Wave 1: Parallel Context Assembly (Extract-Transform-Load)
        val finalPayload = coroutineScope {
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
            
            "Context [Mode: ${enhancedContext.modeMetadata.currentMode}, Metadata: $metadata]"
        }
        
        Log.d("RealUnifiedPipeline", "✅ ETL Phase finished. Proceeding to Execution.")
        
        // --- Wave 3: CRM Task Execution Route ---
        if (input.intent == com.smartsales.prism.domain.analyst.QueryQuality.CRM_TASK) {
            val lintResult = schedulerLinter.lint(resolvedInputText)
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
                    
                    if (lintResult.urgencyLevel in listOf(UrgencyLevel.L1_CRITICAL, UrgencyLevel.L2_IMPORTANT) && 
                        lintResult.parsedClues.person != null) {
                        try {
                            val personEntityId = entityDisambiguationService.startDisambiguation(
                                lintResult.parsedClues.person, Mode.SCHEDULER, lintResult.parsedClues.person, emptyList()
                            )
                            if (personEntityId is com.smartsales.prism.domain.model.UiState.Response) {
                                // Ignore disambig intercepts safely in background for now
                            }
                        } catch (e: Exception) {
                            Log.e("RealUnifiedPipeline", "Failed inline profile update", e)
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
