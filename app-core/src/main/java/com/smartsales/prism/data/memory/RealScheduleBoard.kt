package com.smartsales.prism.data.memory

import com.smartsales.prism.domain.memory.*
import com.smartsales.prism.domain.scheduler.*
import com.smartsales.prism.domain.time.TimeProvider
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
        when (val result = resolveTarget(targetQuery)) {
            is TargetResolution.Resolved -> result.item
            else -> null
        }

    override suspend fun resolveTarget(
        targetQuery: String,
        preferredDayOffset: Int?
    ): TargetResolution {
        val normalizedQuery = normalizeTargetText(targetQuery)
        if (normalizedQuery.length < 2) {
            return TargetResolution.NoMatch(targetQuery)
        }

        val ranked = _upcomingItems.value
            .map { candidate ->
                candidate to scoreCandidate(
                    query = normalizedQuery,
                    candidate = candidate,
                    preferredDayOffset = preferredDayOffset
                )
            }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }

        val top = ranked.firstOrNull() ?: return TargetResolution.NoMatch(targetQuery)
        val runnerUp = ranked.getOrNull(1)
        val topScore = top.second
        val runnerScore = runnerUp?.second ?: 0

        if (topScore < 55) {
            return TargetResolution.NoMatch(targetQuery)
        }

        if (runnerUp != null && topScore - runnerScore < 12) {
            return TargetResolution.Ambiguous(
                query = targetQuery,
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
        candidate: ScheduleItem,
        preferredDayOffset: Int?
    ): Int {
        val normalizedTitle = normalizeTargetText(candidate.title)
        if (normalizedTitle.isBlank()) return 0

        var score = 0

        if (normalizedTitle == query) score += 120
        if (normalizedTitle.contains(query)) score += 80
        if (query.contains(normalizedTitle)) score += 35

        score += (diceCoefficient(query, normalizedTitle) * 45f).toInt()
        score += (tokenOverlap(query, normalizedTitle) * 35f).toInt()

        candidate.participants.orEmpty()
            .map(::normalizeTargetText)
            .filter { it.isNotBlank() }
            .maxOfOrNull { participant ->
                (tokenOverlap(query, participant) * 18f).toInt() +
                    (diceCoefficient(query, participant) * 12f).toInt()
            }
            ?.let { score += it }

        normalizeTargetText(candidate.location).takeIf { it.isNotBlank() }?.let { location ->
            score += (tokenOverlap(query, location) * 12f).toInt()
            score += (diceCoefficient(query, location) * 10f).toInt()
        }

        if (preferredDayOffset != null) {
            val candidateDay = LocalDate.ofInstant(Instant.ofEpochMilli(candidate.scheduledAt), timeProvider.zoneId)
            val offset = java.time.temporal.ChronoUnit.DAYS.between(timeProvider.today, candidateDay).toInt()
            if (offset == preferredDayOffset) {
                score += 10
            } else if (kotlin.math.abs(offset - preferredDayOffset) == 1) {
                score += 4
            }
        }

        return score
    }

    private fun normalizeTargetText(raw: String?): String {
        if (raw.isNullOrBlank()) return ""

        val lowered = raw
            .lowercase()
            .replace("“", " ")
            .replace("”", " ")
            .replace("\"", " ")
            .replace("'", " ")
            .replace("，", " ")
            .replace(",", " ")
            .replace("。", " ")
            .replace("：", " ")
            .replace(":", " ")
            .replace("？", " ")
            .replace("?", " ")
            .replace("！", " ")
            .replace("!", " ")
            .replace("跟", " ")
            .replace("那个", " ")
            .replace("这个", " ")
            .replace("一下", " ")
            .replace("帮我", " ")
            .replace("把", " ")
            .replace("给我", " ")
            .replace("改到", " ")
            .replace("改成", " ")
            .replace("改期", " ")
            .replace("挪到", " ")
            .replace("延期", " ")
            .replace("延后", " ")
            .replace("reschedule", " ")
            .replace("move", " ")
            .replace("to", " ")

        return lowered
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenOverlap(left: String, right: String): Float {
        val leftTokens = buildSearchTokens(left)
        val rightTokens = buildSearchTokens(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0f
        val shared = leftTokens.intersect(rightTokens).size.toFloat()
        return shared / minOf(leftTokens.size, rightTokens.size).toFloat()
    }

    private fun diceCoefficient(left: String, right: String): Float {
        val leftGrams = buildBigrams(left)
        val rightGrams = buildBigrams(right)
        if (leftGrams.isEmpty() || rightGrams.isEmpty()) return 0f
        val shared = leftGrams.intersect(rightGrams).size.toFloat()
        return (2f * shared) / (leftGrams.size + rightGrams.size).toFloat()
    }

    private fun buildSearchTokens(value: String): Set<String> {
        val compact = value.replace(" ", "")
        val tokens = mutableSetOf<String>()
        value.split(" ")
            .filter { it.isNotBlank() }
            .forEach(tokens::add)
        if (compact.length >= 2) {
            buildBigrams(compact).forEach(tokens::add)
        } else if (compact.isNotBlank()) {
            tokens += compact
        }
        return tokens
    }

    private fun buildBigrams(value: String): Set<String> {
        if (value.length < 2) return emptySet()
        return buildSet {
            for (index in 0 until value.length - 1) {
                add(value.substring(index, index + 2))
            }
        }
    }
}
