package com.smartsales.prism.data.memory

import com.smartsales.prism.domain.memory.*
import com.smartsales.prism.domain.scheduler.*
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.data.scheduler.TaskRetrievalCandidate
import com.smartsales.prism.data.scheduler.TaskRetrievalScoring
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日程看板实现 — 封装 ScheduledTaskRepository 提供快速冲突检测
 * 
 * 使用硬编码 Kotlin 逻辑，无需 LLM。
 */
@Singleton
class RealScheduleBoard @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val timeProvider: TimeProvider
) : ScheduleBoard {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _upcomingItems = MutableStateFlow<List<ScheduleItem>>(emptyList())
    override val upcomingItems: StateFlow<List<ScheduleItem>> = _upcomingItems.asStateFlow()
    
    init {
        // 订阅任务变化，投射到 ScheduleItem
        scope.launch {
            // 查询未来7天的任务
            val today = LocalDate.now()
            val endDate = today.plusDays(7)
            
            taskRepository.queryByDateRange(today, endDate).collect { items ->
                _upcomingItems.value = items
                    .filterIsInstance<ScheduledTask>()
                    .filter { !it.isDone }  // 只检测未完成任务
                    .map { it.toScheduleItem() }
            }
        }
    }
    
    /**
     * 冲突检测 — 硬编码 Kotlin 逻辑
     * 
     * 时间重叠判断: s1 < e2 && s2 < e1
     */
    override suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int,
        excludeId: String?
    ): ConflictResult {
        val overlaps = _upcomingItems.value.filter { slot ->
            // 排除指定ID (避免新任务与自己冲突)
            slot.entryId != excludeId &&
            // FIRE_OFF 永不参与冲突
            !bypassesConflictEvaluation(slot.urgencyLevel) &&
            // 不检测模糊任务的冲突
            !slot.isVague &&
            // 只检查 EXCLUSIVE 策略的项目
            slot.conflictPolicy == ConflictPolicy.EXCLUSIVE &&
            // 精准任务即使没有显式时长，也必须参与点位占用冲突判断
            overlapsInScheduleBoard(
                proposedStart = proposedStart,
                proposedDurationMinutes = durationMinutes,
                existingStart = slot.scheduledAt,
                existingDurationMinutes = slot.effectiveConflictDurationMinutes
            )
        }
        
        return if (overlaps.isEmpty()) {
            Log.d("ScheduleBoard", "✅ checkConflict: CLEAR (proposed=${proposedStart}, dur=${durationMinutes}min)")
            ConflictResult.Clear
        } else {
            Log.d("ScheduleBoard", "⚠️ checkConflict: ${overlaps.size} conflicts (${overlaps.map { it.title }})")
            ConflictResult.Conflict(overlaps)
        }
    }
    
    override suspend fun refresh() {
        Log.d("ScheduleBoard", "🔄 refresh: triggered (no-op with Room persistence)")
        // Room sends updates automatically via the Flow collected in init {}
        // No need to manually re-query or block execution
    }
    
    override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? =
        when (val result = resolveTarget(TargetResolutionRequest(targetQuery = targetQuery))) {
            is TargetResolution.Resolved -> result.item
            else -> null
        }

    override suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution {
        val normalizedQuery = TaskRetrievalScoring.normalize(request.targetQuery)
        val normalizedPerson = TaskRetrievalScoring.normalize(request.targetPerson)
        val normalizedLocation = TaskRetrievalScoring.normalize(request.targetLocation)
        if (
            normalizedQuery.length < 2 &&
            normalizedPerson.length < 2 &&
            normalizedLocation.length < 2
        ) {
            return TargetResolution.NoMatch(request.describeForFailure())
        }

        val ranked = _upcomingItems.value
            .map { candidate ->
                candidate to scoreCandidate(
                    query = normalizedQuery,
                    person = normalizedPerson,
                    location = normalizedLocation,
                    candidate = candidate,
                    preferredTaskIds = request.preferredTaskIds
                )
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }

        val top = ranked.firstOrNull() ?: return TargetResolution.NoMatch(request.describeForFailure())
        val runnerUp = ranked.getOrNull(1)
        val topScore = top.second
        val runnerScore = runnerUp?.second ?: 0

        if (topScore < TaskRetrievalScoring.MIN_RESOLUTION_SCORE) {
            return TargetResolution.NoMatch(request.describeForFailure())
        }

        if (runnerUp != null && topScore - runnerScore < TaskRetrievalScoring.MIN_MARGIN_SCORE) {
            return TargetResolution.Ambiguous(
                query = request.describeForFailure(),
                candidateIds = ranked.take(3).map { it.first.entryId }
            )
        }

        return TargetResolution.Resolved(top.first)
    }
    
    /**
     * Task -> ScheduleItem 转换
     */
    private fun ScheduledTask.toScheduleItem(): ScheduleItem {
        return ScheduleItem(
            entryId = id,
            title = title,
            scheduledAt = startTime.toEpochMilli(),
            durationMinutes = durationMinutes,
            durationSource = durationSource,
            urgencyLevel = urgencyLevel,
            conflictPolicy = conflictPolicy,
            participants = keyPerson?.let { listOf(it) } ?: emptyList(),
            location = location,
            isVague = isVague
        )
    }

    private fun scoreCandidate(
        query: String,
        person: String,
        location: String,
        candidate: ScheduleItem,
        preferredTaskIds: Set<String>
    ): Int = TaskRetrievalScoring.scoreCandidate(
        query = query,
        person = person,
        location = location,
        candidate = TaskRetrievalCandidate(
            id = candidate.entryId,
            title = candidate.title,
            participants = candidate.participants.orEmpty(),
            location = candidate.location
        ),
        preferredTaskIds = preferredTaskIds
    )
}
