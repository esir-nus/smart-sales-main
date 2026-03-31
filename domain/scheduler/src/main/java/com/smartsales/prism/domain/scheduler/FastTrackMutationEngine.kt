package com.smartsales.prism.domain.scheduler
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.memory.TargetResolution
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.memory.bypassesConflictEvaluation
import com.smartsales.prism.domain.memory.effectiveConflictOccupancyMinutes
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
            val urgencyLevel = UrgencyLevel.valueOf(def.urgency.name)
            val effectiveConflictDurationMinutes = effectiveConflictOccupancyMinutes(
                title = def.title,
                urgencyLevel = urgencyLevel,
                explicitDurationMinutes = def.durationMinutes
            )
            
            // 1. Evaluate temporal conflict
            val conflictResult = if (bypassesConflictEvaluation(urgencyLevel)) {
                ConflictResult.Clear
            } else {
                scheduleBoard.checkConflict(
                    proposedStart = startInst.toEpochMilli(),
                    durationMinutes = effectiveConflictDurationMinutes
                )
            }
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
                urgencyLevel = urgencyLevel,
                startTime = startInst,
                durationMinutes = def.durationMinutes,
                location = def.location,
                keyPerson = def.keyPerson,
                hasConflict = hasConflict,
                conflictWithTaskId = conflictWithTaskId,
                conflictSummary = conflictSummary,
                isVague = false // Or derive from NLP payload
            ).withNormalizedReminderMetadata()
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
            location = params.location,
            keyPerson = params.keyPerson,
            hasConflict = false,
            isVague = true,
            notes = vagueNotes
        ).withNormalizedReminderMetadata()

        val id = taskRepository.upsertTask(task)
        return MutationResult.Success(listOf(id))
    }

    private suspend fun handleRescheduleTask(params: RescheduleTaskParams): MutationResult {
        val resolvedTaskId = params.resolvedTaskId
        val query = params.targetQuery ?: resolvedTaskId ?: "<empty>"

        // 1. Resolve target using scheduler-owned confidence gating
        val match = if (resolvedTaskId != null) {
            val task = taskRepository.getTask(resolvedTaskId)
                ?: return MutationResult.NoMatch(query, "Task resolved in UI but missing in DB")
            com.smartsales.prism.domain.memory.ScheduleItem(
                entryId = task.id,
                title = task.title,
                scheduledAt = task.startTime.toEpochMilli(),
                durationMinutes = task.durationMinutes,
                durationSource = task.durationSource,
                urgencyLevel = task.urgencyLevel,
                conflictPolicy = task.conflictPolicy,
                participants = task.keyPerson?.let(::listOf) ?: emptyList(),
                location = task.location,
                isVague = task.isVague
            )
        } else {
            when (val resolution = scheduleBoard.resolveTarget(TargetResolutionRequest(targetQuery = query))) {
                is TargetResolution.Resolved -> resolution.item
                is TargetResolution.Ambiguous -> return MutationResult.AmbiguousMatch(query)
                is TargetResolution.NoMatch -> return MutationResult.NoMatch(query, "未找到匹配的日程，请更具体一些。")
            }
        }
            
        // 2. Map old task entity id
        val oldTaskId = match.entryId
        
        // 3. Construct new target time
        val newStartInst = Instant.parse(params.newStartTimeIso)
        val newDuration = params.newDurationMinutes ?: match.durationMinutes
        val fullOldTask = taskRepository.getTask(oldTaskId)
            ?: return MutationResult.NoMatch(query, "Task matched in board but missing in DB")
        val effectiveConflictDurationMinutes = effectiveConflictOccupancyMinutes(
            title = fullOldTask.title,
            urgencyLevel = fullOldTask.urgencyLevel,
            explicitDurationMinutes = newDuration
        )
        
        // 4. Check conflict (ignoring the task we are rescheduling and vague tasks)
        val conflictResult = if (bypassesConflictEvaluation(fullOldTask.urgencyLevel)) {
            ConflictResult.Clear
        } else {
            scheduleBoard.checkConflict(
                proposedStart = newStartInst.toEpochMilli(),
                durationMinutes = effectiveConflictDurationMinutes,
                excludeId = oldTaskId
            )
        }
        val conflict = conflictResult as? ConflictResult.Conflict
        val hasConflict = conflict != null

        // 5. Construct new task while keeping persisted visible duration semantics unchanged.
        val newTask = fullOldTask.copy(
            startTime = newStartInst,
            durationMinutes = newDuration,
            hasConflict = hasConflict,
            conflictWithTaskId = conflict?.overlaps?.firstOrNull()?.entryId,
            conflictSummary = conflict?.overlaps?.firstOrNull()?.let { "与「${it.title}」时间冲突" },
            isVague = false
        ).withNormalizedReminderMetadata()
        
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
