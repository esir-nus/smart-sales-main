package com.smartsales.core.context

import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import kotlinx.coroutines.flow.first
import com.smartsales.core.context.Log
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.repository.HistoryRepository

import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.crm.writeback.KernelWriteBack
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.session.EntityState
import com.smartsales.prism.domain.session.SessionWorkingSet
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
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
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val historyRepository: HistoryRepository,
    private val telemetry: PipelineTelemetry
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
    private var _currentMode: Mode = Mode.ANALYST

    // === ContextBuilder Implementation (Read-Only) ===

    override suspend fun build(
        userText: String, 
        mode: Mode, 
        resolvedEntityIds: List<String>,
        depth: ContextDepth,
        isBadge: Boolean
    ): EnhancedContext {
        Log.d("Kernel", "🚀 Building EnhancedContext with depth=$depth for mode=${mode.name}")
        _currentMode = mode

        // Section 2: 全局用户习惯（首轮加载）
        if (depth == ContextDepth.FULL && _workingSet.userHabitContext == null) {
            telemetry.recordEvent(PipelinePhase.RL_MODULE, "Loading user habits (Section 2)")
            _workingSet.userHabitContext = reinforcementLearner.loadUserHabits()
            Log.d("Kernel", "📦 Section 2 loaded: userHabits")
        }

        // Section 1: Entity Knowledge Context
        val previouslyActiveIds = _workingSet.entityStates
            .filter { it.value.state == EntityState.ACTIVE || it.value.state == EntityState.MENTIONED }
            .keys.toList()
            
        val allEntityIds = (resolvedEntityIds + _workingSet.entityContext.values.map { it.entityId } + previouslyActiveIds).distinct()
        
        // Enforce state machine for explicit mentions
        resolvedEntityIds.forEach { 
            _workingSet.markActive(it) 
        }
        
        val missingIds = allEntityIds - _workingSet.entityCache.keys
        
        if (depth == ContextDepth.FULL) {
            if (shouldSearchMemory(_sessionHistory) || missingIds.isNotEmpty()) {
                telemetry.recordEvent(PipelinePhase.MEMORY_LOOKUP, "Loading entity knowledge: ${missingIds.size} missing, ${allEntityIds.size} total (Section 1)")
                
                // Delta Loading: Only fetch missing entities from DB
                if (missingIds.isNotEmpty()) {
                    val fetched = missingIds.mapNotNull { entityRepository.getById(it) }
                    fetched.forEach { _workingSet.entityCache[it.entityId] = it }
                }
                
                // Build final JSON from RAM Cache
                val activeEntities = allEntityIds.mapNotNull { _workingSet.entityCache[it] }
                val knowledge = buildEntityKnowledgeFromEntries(activeEntities)
                _workingSet.entityKnowledge = knowledge
                Log.d("Kernel", "📸 Section 1: entityKnowledge=${knowledge?.length ?: 0} chars")
            }

            if (shouldSearchMemory(_sessionHistory)) {
                // Sticky Notes: 加载近期日程
                val schedule = buildScheduleContext()
                _workingSet.scheduleContext = schedule
                Log.d("Kernel", "📅 Sticky Notes: ${schedule?.length ?: 0} chars")
            }
        }

        telemetry.recordEvent(PipelinePhase.CONTEXT_BUILDER, "Kernel RAM assembly complete (Turn $_turnCount)")

        return EnhancedContext(
            userText = userText,
            isBadge = isBadge,

            entityKnowledge = if (depth == ContextDepth.FULL) _workingSet.entityKnowledge else null,
            entityContext = if (depth == ContextDepth.FULL) _workingSet.entityContext else emptyMap(),
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _workingSet.sessionId,
                turnIndex = _turnCount
            ),
            sessionHistory = _sessionHistory.toList(),
            // 传入 Document Context
            documentContext = if (depth == ContextDepth.MINIMAL) null else _workingSet.documentContext,
            // Legacy/Dead fields (kept for binary compatibility until consumers updated)
            lastToolResult = null,
            executedTools = emptySet(),
            currentDate = timeProvider.formatForLlm(),
            currentInstant = timeProvider.now.toEpochMilli(),
            habitContext = if (depth == ContextDepth.FULL) _workingSet.getCombinedHabitContext() else null,
            scheduleContext = if (depth == ContextDepth.FULL) _workingSet.scheduleContext else null
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
        _turnCount++
        _sessionHistory.add(ChatTurn(role = "user", content = content))
        pruneHistory()
        
        // Write-through to SSD
        val turnIndex = _sessionHistory.size - 1
        historyRepository.saveMessage(_workingSet.sessionId, isUser = true, content = content, orderIndex = turnIndex)
        
        saveToMemory(content, MemoryEntryType.USER_MESSAGE)
    }

    override suspend fun recordAssistantMessage(content: String) {
        _turnCount++
        _sessionHistory.add(ChatTurn(role = "assistant", content = content))
        pruneHistory()
        
        // Write-through to SSD
        val turnIndex = _sessionHistory.size - 1
        historyRepository.saveMessage(_workingSet.sessionId, isUser = false, content = content, orderIndex = turnIndex)
        
        saveToMemory(content, MemoryEntryType.ASSISTANT_RESPONSE)
    }

    // === KernelWriteBack Implementation (Write) ===

    override suspend fun updateEntityInSession(entityId: String, ref: EntityRef, entry: EntityEntry?) {
        _writeLock.withLock {
            // Section 1: Write-through update
            _workingSet.entityContext["entity_$entityId"] = ref
            _workingSet.markActive(entityId)
            
            if (entry != null) {
                _workingSet.entityCache[entityId] = entry
            } else {
                _workingSet.entityCache.remove(entityId) // Force refresh on next context build
            }
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
            _workingSet.entityCache.remove(entityId)
            Log.d("Kernel", "🗑️ Write-through S1 removed: $entityId")
        }
    }

    override suspend fun applyHabitUpdates(observations: List<com.smartsales.prism.domain.rl.RlObservation>) {
        _writeLock.withLock {
            val hasGlobal = observations.any { it.entityId == null }
            val hasClient = observations.any { it.entityId != null }
            
            if (hasGlobal) {
                _workingSet.userHabitContext = reinforcementLearner.loadUserHabits()
                Log.d("Kernel", "📦 Section 2 (Global Habits) refreshed via Write-Through")
            }
            if (hasClient) {
                val activeIds = _workingSet.entityContext.values.map { it.entityId }
                if (activeIds.isNotEmpty()) {
                    _workingSet.clientHabitContext = reinforcementLearner.loadClientHabits(activeIds)
                    Log.d("Kernel", "📦 Section 3 (Client Habits) refreshed via Write-Through")
                }
            }
        }
    }


    // === Lifecycle ===
    
    override fun resetSession() {
        _sessionHistory.clear()
        _turnCount = 0
        _workingSet = createNewWorkingSet()
        Log.d("Kernel", "🔄 Session reset: ${_workingSet.sessionId}")
    }

    override fun getActiveSessionId(): String = _workingSet.sessionId

    override fun loadSession(sessionId: String, history: List<ChatTurn>) {
        _sessionHistory.clear()
        _sessionHistory.addAll(history)
        _turnCount = history.size / 2
        // 使用指定的 sessionId 创建 WorkingSet，而非随机生成
        _workingSet = SessionWorkingSet(
            sessionId = sessionId,
            createdAt = timeProvider.now.toEpochMilli()
        )
        Log.d("Kernel", "📂 Session loaded: $sessionId, ${history.size} turns")
    }

    /**
     * 加载临时文档上下文到 RAM
     * 用于跨流传递大文本（例如音频分析时的转写结果）
     */
    override fun loadDocumentContext(payload: String) {
        _workingSet.documentContext = payload
        Log.d("Kernel", "📄 Document context loaded: ${payload.length} chars")
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
    private fun buildEntityKnowledgeFromEntries(entities: List<EntityEntry>): String? {
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
            put("name", entry.displayName as Any)
            
            // Aliases: 内部消歧用，不暴露给 LLM（可能包含冒犯性别名）
            
            // CRM Fields
            if (entry.jobTitle != null) put("role", entry.jobTitle as Any)
            if (entry.buyingRole != null) put("buyingRole", entry.buyingRole as Any)
            if (entry.accountId != null) put("accountId", entry.accountId as Any)
            if (entry.dealStage != null) put("dealStage", entry.dealStage as Any)
            if (entry.dealValue != null) put("dealValue", entry.dealValue as Any)
            if (entry.closeDate != null) put("closeDate", entry.closeDate as Any)
            if (entry.nextAction != null) put("nextAction", entry.nextAction as Any)
            
            // Structured Intelligence
            val jsonProperties: List<Pair<String, String?>> = listOf(
                Pair("attributes", entry.attributesJson),
                Pair("metrics", entry.metricsHistoryJson),
                Pair("decisions", entry.decisionLogJson),
                Pair("related", entry.relatedEntitiesJson)
            )
            jsonProperties.forEach { (key, jsonStr) ->
                if (!jsonStr.isNullOrBlank()) {
                    try {
                        val trimmed: String = jsonStr.trim()
                        val json: Any = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
                        val length = if (json is JSONArray) json.length() else (json as JSONObject).length()
                        if (length > 0) put(key, json)
                    } catch (_: Exception) { 
                        // Fallback: If not valid JSON, inject as raw text so LLM still sees it
                        put(key, jsonStr as Any) 
                    }
                }
            }
        }
    }
}
