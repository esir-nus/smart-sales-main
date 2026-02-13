package com.smartsales.prism.data.real

import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.first
import android.util.Log
import com.smartsales.prism.domain.crm.ActivityType
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.KernelWriteBack
import com.smartsales.prism.domain.pipeline.MemoryHit
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.session.EntityState
import com.smartsales.prism.domain.session.SessionWorkingSet
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实上下文构建器 (Kernel)
 *
 * 核心职责:
 * 1. 管理 Session Working Set (RAM) 生命周期
 * 2. 聚合上下文要素: 历史、习惯、实体知识
 * 3. 实现 KernelWriteBack 接口供 EntityWriter 回写
 *
 * @see docs/cerb/session-context/spec.md
 */
@Singleton
class RealContextBuilder @Inject constructor(
    private val timeProvider: TimeProvider,
    private val reinforcementLearner: ReinforcementLearner,
    private val memoryRepository: MemoryRepository,
    private val entityRepository: EntityRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository
) : ContextBuilder, KernelWriteBack {

    // === Kernel State (RAM) ===
    
    /** 会话历史（最多保留10轮） */
    private val _sessionHistory = mutableListOf<ChatTurn>()
    
    /** 写锁 — 保护 Working Set 写操作（替代 @Synchronized） */
    private val _writeLock = Mutex()
    
    /** 当前工作集 */
    private var _workingSet: SessionWorkingSet = createNewWorkingSet()
    
    /** 会话轮次计数（Kernel 管理元数据） */
    private var _turnCount: Int = 0
    
    /** 当前模式缓存（用于 saveToMemory） */
    private var _currentMode: Mode = Mode.COACH

    // === ContextBuilder Implementation (Read-Only) ===

    override suspend fun build(userText: String, mode: Mode): EnhancedContext {
        _turnCount++
        _currentMode = mode

        // Section 2: 全局用户习惯（首轮加载）
        if (_workingSet.userHabitContext == null) {
            _workingSet.userHabitContext = reinforcementLearner.loadUserHabits()
            Log.d("Kernel", "📦 Section 2 loaded: userHabits")
        }

        // Section 1: Entity Knowledge Context（首轮触发）
        if (shouldSearchMemory(_sessionHistory)) {
            // 构建实体知识图谱
            val knowledge = buildEntityKnowledge()
            _workingSet.entityKnowledge = knowledge
            Log.d("Kernel", "📸 Section 1: entityKnowledge=${knowledge?.length ?: 0} chars")

            // Sticky Notes: 加载近期日程
            val schedule = buildScheduleContext()
            _workingSet.scheduleContext = schedule
            Log.d("Kernel", "📅 Sticky Notes: ${schedule?.length ?: 0} chars")

            // Soft-deprecated: memoryHits (legacy support)
            // TODO: Migrate consumers to entityKnowledge and remove this
            val hits = memoryRepository.search(userText, limit = 5).map { entry ->
                MemoryHit(entry.entryId, entry.content, 1.0f)
            }
            _workingSet.memoryHits = hits
            Log.d("Kernel", "🔍 Section 1: memorySearch -> ${hits.size} hits (legacy)")
        }

        return EnhancedContext(
            userText = userText,
            memoryHits = _workingSet.memoryHits,
            entityKnowledge = _workingSet.entityKnowledge,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _workingSet.sessionId,
                turnIndex = _turnCount
            ),
            sessionHistory = _sessionHistory.toList(),
            // Legacy/Dead fields (kept for binary compatibility until consumers updated)
            lastToolResult = null,
            executedTools = emptySet(),
            currentDate = timeProvider.formatForLlm(),
            currentInstant = timeProvider.now.toEpochMilli(),
            habitContext = _workingSet.getCombinedHabitContext(),
            scheduleContext = _workingSet.scheduleContext
        )
    }

    // Sticky Notes: 构建近期日程上下文 (Priority: Urgency > Time)
    private suspend fun buildScheduleContext(): String? {
        val today = timeProvider.today
        val endDate = today.plusDays(3)

        return try {
            val undoneTasks = scheduledTaskRepository.queryByDateRange(today, endDate)
                .first()
                .filterIsInstance<TimelineItemModel.Task>()
                .filter { !it.isDone }
                .sortedWith(
                    compareBy<TimelineItemModel.Task> { it.urgencyLevel.ordinal } // L1 critical first
                        .thenBy { it.startTime } // Closer time first
                )

            if (undoneTasks.isEmpty()) return null

            val totalCount = undoneTasks.size
            val top3 = undoneTasks.take(3)

            buildString {
                // 总数提示（让 LLM 知道还有更多未显示的任务）
                if (totalCount > 3) {
                    appendLine("共 ${totalCount} 个待办（显示前3）：")
                }
                top3.forEach { task ->
                    append("- ${task.dateRange} ${task.title}")
                    task.keyPerson?.let { append("（关键人: $it）") }
                    task.location?.let { append("（地点: $it）") }
                    appendLine()
                }
            }.trimEnd()
        } catch (e: Exception) {
            Log.e("Kernel", "Failed to load sticky notes: ${e.message}")
            null
        }
    }

    override fun getSessionHistory(): List<ChatTurn> = _sessionHistory.toList()

    override suspend fun recordUserMessage(content: String) {
        _sessionHistory.add(ChatTurn(role = "user", content = content))
        pruneHistory()
        saveToMemory(content, MemoryEntryType.USER_MESSAGE)
    }

    override suspend fun recordAssistantMessage(content: String) {
        _sessionHistory.add(ChatTurn(role = "assistant", content = content))
        pruneHistory()
        saveToMemory(content, MemoryEntryType.ASSISTANT_RESPONSE)
    }

    // === KernelWriteBack Implementation (Write) ===

    override suspend fun updateEntityInSession(entityId: String, ref: EntityRef) {
        _writeLock.withLock {
            // Section 1: Write-through update
            _workingSet.entityContext["entity_$entityId"] = ref
            _workingSet.markActive(entityId)
            Log.d("Kernel", "📝 Write-through S1: $entityId → ${ref.displayName}")

            // Section 3: Auto-refresh client habits
            val activeIds = _workingSet.entityContext.values.map { it.entityId }
            if (activeIds.isNotEmpty()) {
                // Now safe to call suspend function (no runBlocking)
                _workingSet.clientHabitContext = reinforcementLearner.loadClientHabits(activeIds)
                Log.d("Kernel", "📦 Section 3 refreshed: ${activeIds.size} entities")
            }
        }
    }

    override suspend fun removeEntityFromSession(entityId: String) {
        _writeLock.withLock {
            _workingSet.entityContext.entries.removeAll { it.value.entityId == entityId }
            _workingSet.entityStates.remove(entityId)
            Log.d("Kernel", "🗑️ Write-through S1 removed: $entityId")
        }
    }

    override suspend fun recordActivity(entityId: String, type: ActivityType, summary: String) {
        val entryId = UUID.randomUUID().toString()
        val structuredJson = """{"activityType":"${type.name}","entityId":"$entityId","summary":"$summary"}"""

        memoryRepository.save(MemoryEntry(
            entryId = entryId,
            sessionId = _workingSet.sessionId,
            content = summary,
            entryType = MemoryEntryType.TASK_RECORD,
            createdAt = timeProvider.now.toEpochMilli(),
            updatedAt = timeProvider.now.toEpochMilli(),
            structuredJson = structuredJson,
            workflow = "entity_writer"
        ))
        Log.d("Kernel", "📜 recordActivity: type=${type.name} entity=$entityId")
    }

    // === Lifecycle ===

    fun resetSession() {
        _sessionHistory.clear()
        _turnCount = 0
        _workingSet = createNewWorkingSet()
        Log.d("Kernel", "🔄 Session reset: ${_workingSet.sessionId}")
    }

    // === Private Helpers ===

    private fun createNewWorkingSet(): SessionWorkingSet {
        return SessionWorkingSet(
            sessionId = "session-${timeProvider.now.toEpochMilli()}",
            createdAt = timeProvider.now.toEpochMilli()
        )
    }

    private fun pruneHistory() {
        while (_sessionHistory.size > 20) { // Keep last 10 turns (20 messages)
            _sessionHistory.removeAt(0)
        }
    }

    /**
     * Context Strategy: First Turn Search
     * Only search memory repository on the very first turn of a session.
     */
    private fun shouldSearchMemory(sessionHistory: List<ChatTurn>): Boolean {
        return sessionHistory.size < 2
    }

    private suspend fun saveToMemory(content: String, type: MemoryEntryType) {
        val activeEntityIds = _workingSet.entityStates
            .filter { it.value.state == EntityState.ACTIVE }
            .keys.toList()

        val structuredJson = if (activeEntityIds.isNotEmpty()) {
            """{"relatedEntityIds":[${activeEntityIds.joinToString(",") { "\"$it\"" }}]}"""
        } else null

        memoryRepository.save(MemoryEntry(
            entryId = UUID.randomUUID().toString(),
            sessionId = _workingSet.sessionId,
            content = content,
            entryType = type,
            createdAt = timeProvider.now.toEpochMilli(),
            updatedAt = timeProvider.now.toEpochMilli(),
            structuredJson = structuredJson,
            workflow = _currentMode.name
        ))
    }

    /**
     * Build Entity Knowledge Graph JSON
     * Used for disambiguation and alias matching in LLM prompt.
     */
    private suspend fun buildEntityKnowledge(): String? {
        val entities = entityRepository.getAll(limit = 100)
        if (entities.isEmpty()) return null

        val root = JSONObject()
        
        entities.groupBy { it.entityType }.forEach { (type, list) ->
            val key = when (type) {
                EntityType.PERSON -> "people"
                EntityType.ACCOUNT -> "accounts"
                EntityType.DEAL -> "deals"
                else -> null
            }
            if (key != null) {
                root.put(key, JSONArray().apply {
                    list.forEach { put(entityToJson(it)) }
                })
            }
        }

        return root.toString()
    }

    private fun entityToJson(entry: EntityEntry): JSONObject {
        return JSONObject().apply {
            put("name", entry.displayName)
            
            // Aliases: 内部消歧用，不暴露给 LLM（可能包含冒犯性别名）
            
            // CRM Fields
            entry.jobTitle?.let { put("role", it) }
            entry.buyingRole?.let { put("buyingRole", it) }
            entry.accountId?.let { put("accountId", it) }
            entry.dealStage?.let { put("dealStage", it) }
            entry.dealValue?.let { put("dealValue", it) }
            entry.closeDate?.let { put("closeDate", it) }
            entry.nextAction?.let { put("nextAction", it) }
            
            // Structured Intelligence
            listOf(
                "attributes" to entry.attributesJson,
                "metrics" to entry.metricsHistoryJson,
                "decisions" to entry.decisionLogJson,
                "related" to entry.relatedEntitiesJson
            ).forEach { (key, jsonStr) ->
                try {
                    val json = if (jsonStr.trim().startsWith("[")) JSONArray(jsonStr) else JSONObject(jsonStr)
                    val length = if (json is JSONArray) json.length() else (json as JSONObject).length()
                    if (length > 0) put(key, json)
                } catch (_: Exception) { /* Ignore invalid JSON */ }
            }
        }
    }
}
