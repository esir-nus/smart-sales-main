package com.smartsales.prism.domain.scheduler
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import com.smartsales.prism.domain.time.TimeProvider

/**
 * Result of a FastTrack execution.
 * Replaces pure Android Log.d statements to enforce Domain Isolation.
 */
sealed class MutationResult {
    data class Success(val taskIds: List<String>) : MutationResult()
    data class InspirationCreated(val id: String) : MutationResult()
    data class AmbiguousMatch(val query: String) : MutationResult()
    data class NoMatch(val query: String, val reason: String) : MutationResult()
    data class Error(val exception: Throwable) : MutationResult()
}

/**
 * Executes Path A FastTrack intent (Create, Reschedule, Inspiration).
 * Pure JVM domain logic. Operates atomically via [ScheduledTaskRepository].
 */
class FastTrackMutationEngine @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val inspirationRepository: InspirationRepository,
    private val timeProvider: TimeProvider
) {
    /**
     * Executes the incoming FastTrack DTO from the Linter.
     */
    suspend fun execute(intent: FastTrackResult): MutationResult {
        return try {
            when (intent) {
                is FastTrackResult.CreateTasks -> handleCreateTasks(intent.params)
                is FastTrackResult.CreateVagueTask -> handleCreateVagueTask(intent.params)
                is FastTrackResult.RescheduleTask -> handleRescheduleTask(intent.params)
                is FastTrackResult.CreateInspiration -> handleCreateInspiration(intent.params)
                is FastTrackResult.NoMatch -> MutationResult.NoMatch("UNKNOWN", intent.reason)
            }
        } catch (e: Exception) {
            MutationResult.Error(e)
        }
    }

    private suspend fun handleCreateTasks(params: CreateTasksParams): MutationResult {
        val newTasks = params.tasks.map { def ->
            val startInst = parseExactStartInstant(def.startTimeIso)
            
            // 1. Evaluate temporal conflict
            val conflictResult = scheduleBoard.checkConflict(
                proposedStart = startInst.toEpochMilli(),
                durationMinutes = def.durationMinutes
            )
            val hasConflict = conflictResult is ConflictResult.Conflict
            val conflictSummary = (conflictResult as? ConflictResult.Conflict)
                ?.overlaps
                ?.firstOrNull()
                ?.let { overlap -> "与「${overlap.title}」时间冲突" }
            val conflictWithTaskId = (conflictResult as? ConflictResult.Conflict)
                ?.overlaps
                ?.firstOrNull()
                ?.entryId
            
            ScheduledTask(
                id = if (params.tasks.size == 1 && !params.unifiedId.isNullOrBlank()) {
                    params.unifiedId
                } else {
                    UUID.randomUUID().toString()
                },
                timeDisplay = "", // Rendered in UI layer
                title = def.title,
                urgencyLevel = UrgencyLevel.valueOf(def.urgency.name),
                startTime = startInst,
                durationMinutes = def.durationMinutes,
                hasConflict = hasConflict,
                conflictWithTaskId = conflictWithTaskId,
                conflictSummary = conflictSummary,
                isVague = false // Or derive from NLP payload
            )
        }
        
        if (newTasks.size == 1 && !params.unifiedId.isNullOrBlank()) {
            val task = newTasks.first().copy(id = params.unifiedId)
            val id = taskRepository.upsertTask(task)
            return MutationResult.Success(listOf(id))
        }

        val ids = taskRepository.batchInsertTasks(newTasks)
        return MutationResult.Success(ids)
    }

    private suspend fun handleCreateVagueTask(params: CreateVagueTaskParams): MutationResult {
        val anchorDate = LocalDate.parse(params.anchorDateIso)
        val anchorInstant = anchorDate.atStartOfDay(timeProvider.zoneId).toInstant()
        val vagueNotes = buildString {
            append("时间待定")
            params.timeHint?.takeIf { it.isNotBlank() }?.let {
                append("（线索：")
                append(it)
                append("）")
            }
        }

        val task = ScheduledTask(
            id = params.unifiedId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            timeDisplay = "待定",
            title = params.title,
            urgencyLevel = UrgencyLevel.valueOf(params.urgency.name),
            startTime = anchorInstant,
            durationMinutes = 0,
            hasConflict = false,
            isVague = true,
            notes = vagueNotes
        )

        val id = taskRepository.upsertTask(task)
        return MutationResult.Success(listOf(id))
    }

    private suspend fun handleRescheduleTask(params: RescheduleTaskParams): MutationResult {
        val query = params.targetQuery ?: return MutationResult.NoMatch("<empty>", "Cannot reschedule without query")
        
        // 1. Lexical Fuzzy Match using ScheduleBoard
        val match = scheduleBoard.findLexicalMatch(query) 
            ?: return MutationResult.AmbiguousMatch(query) // findLexicalMatch returns null if 0 or 2+ matches
            
        // 2. Map old task entity id
        val oldTaskId = match.entryId
        
        // 3. Construct new target time
        val newStartInst = Instant.parse(params.newStartTimeIso)
        val newDuration = params.newDurationMinutes ?: match.durationMinutes
        
        // 4. Check conflict (ignoring the task we are rescheduling and vague tasks)
        val conflictResult = scheduleBoard.checkConflict(
            proposedStart = newStartInst.toEpochMilli(),
            durationMinutes = newDuration,
            excludeId = oldTaskId
        )
        val hasConflict = conflictResult is ConflictResult.Conflict
        
        // 5. Construct New Task (pulling old title, but we don't have the full ScheduledTask here, just ScheduleItem)
        // Wait! findLexicalMatch returns ScheduleItem now, not ScheduledTask!
        // I need to fetch the full ScheduledTask from repo!
        val fullOldTask = taskRepository.getTask(oldTaskId) 
            ?: return MutationResult.NoMatch(query, "Task matched in board but missing in DB")

        val newTask = fullOldTask.copy(
            startTime = newStartInst,
            durationMinutes = newDuration,
            hasConflict = hasConflict,
            isVague = false
        )
        
        // 6. Execute atomic Delete -> Insert
        taskRepository.rescheduleTask(oldTaskId, newTask)
        
        return MutationResult.Success(listOf(oldTaskId))
    }

    private suspend fun handleCreateInspiration(params: CreateInspirationParams): MutationResult {
        val id = inspirationRepository.insert(params.content)
        return MutationResult.InspirationCreated(id)
    }

    private fun parseExactStartInstant(raw: String): Instant {
        return runCatching { Instant.parse(raw) }
            .getOrElse { OffsetDateTime.parse(raw).toInstant() }
    }
}
