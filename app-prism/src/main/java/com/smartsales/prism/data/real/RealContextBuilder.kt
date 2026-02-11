package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import org.json.JSONArray
import org.json.JSONObject
import com.smartsales.prism.domain.session.EntityState
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.MemoryHit
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.pipeline.ToolArtifact
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.session.SessionContext
import com.smartsales.prism.domain.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实上下文构建器 — 聚合会话历史和工具结果
 * 
 * Phase 4: 支持上下文感知的重规划
 * Wave 2: 支持 RelevancyLib 实体查询注入
 * Wave 3: Entity Knowledge Context（结构化实体图谱注入 LLM prompt）
 * @see Prism-V1.md §2.2 #1
 */
@Singleton
class RealContextBuilder @Inject constructor(
    private val timeProvider: TimeProvider,
    private val reinforcementLearner: ReinforcementLearner,
    private val memoryRepository: MemoryRepository,
    private val entityRepository: EntityRepository
) : ContextBuilder {
    
    // 会话历史（最多保留10轮，避免上下文膨胀）
    private val _sessionHistory = mutableListOf<ChatTurn>()
    private var _lastToolResult: ToolArtifact? = null
    private val _executedTools = mutableSetOf<String>()
    
    private var _sessionId = "session-${System.currentTimeMillis()}"
    private var _sessionContext = SessionContext(
        sessionId = _sessionId,
        createdAt = timeProvider.now.toEpochMilli()
    )
    private var _currentMode: Mode = Mode.COACH  // Wave 4: 缓存当前模式用于 saveToMemory
    
    override suspend fun build(userText: String, mode: Mode): EnhancedContext {
        _sessionContext.incrementTurn()
        _currentMode = mode  // Wave 4: 缓存模式
        
        // Section 2: 会话首次加载用户习惯（一次性）
        if (_sessionContext.userHabitContext == null) {
            _sessionContext.userHabitContext = reinforcementLearner.loadUserHabits()
            Log.d("WorkingSet", "📦 Section 2 loaded: userHabits")
        }
        
        // Section 1: Entity Knowledge Context（首轮触发）
        if (shouldSearchMemory(userText, _sessionHistory)) {
            // Wave 3: 加载实体知识图谱
            val knowledge = buildEntityKnowledge()
            _sessionContext.entityKnowledge = knowledge
            Log.d("WorkingSet", "📸 Section 1: entityKnowledge=${knowledge?.length ?: 0} chars")
            
            // Soft-deprecated: 保留 memoryHits 兼容下游
            val hits = memoryRepository.search(userText, limit = 5).map { entry ->
                MemoryHit(
                    entryId = entry.entryId,
                    content = entry.content,
                    relevanceScore = 1.0f
                )
            }
            _sessionContext.memoryHits = hits
            Log.d("WorkingSet", "🔍 Section 1: memorySearch('${userText.take(30)}') → ${hits.size} hits (soft-deprecated)")
        }
        
        return EnhancedContext(
            userText = userText,
            memoryHits = _sessionContext.memoryHits,  // soft-deprecated, 保留兼容
            entityKnowledge = _sessionContext.entityKnowledge,  // Wave 3: 实体知识图谱
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _sessionId,
                turnIndex = _sessionContext.turnCount
            ),
            sessionHistory = _sessionHistory.toList(),
            lastToolResult = _lastToolResult,
            executedTools = _executedTools.toSet(),
            currentDate = timeProvider.formatForLlm(),
            currentInstant = timeProvider.now.toEpochMilli(),
            habitContext = _sessionContext.getCombinedHabitContext()  // 从 RAM 读取
        )
    }
    
    /**
     * 获取当前会话历史
     */
    override fun getSessionHistory(): List<ChatTurn> = _sessionHistory.toList()
    
    /**
     * 记录用户输入到历史
     */
    override suspend fun recordUserMessage(content: String) {
        _sessionHistory.add(ChatTurn(role = "user", content = content))
        pruneHistory()
        saveToMemory(content, MemoryEntryType.USER_MESSAGE)
    }
    
    /**
     * 记录助手响应到历史
     */
    override suspend fun recordAssistantMessage(content: String) {
        _sessionHistory.add(ChatTurn(role = "assistant", content = content))
        pruneHistory()
        saveToMemory(content, MemoryEntryType.ASSISTANT_RESPONSE)
    }
    
    /**
     * 持久化记忆条目（含实体标记）
     * Wave 2: 填充 structuredJson 以支持实体时间线
     */
    private suspend fun saveToMemory(content: String, type: MemoryEntryType) {
        val activeEntityIds = _sessionContext.entityStates
            .filter { it.value.state == EntityState.ACTIVE }
            .keys.toList()

        val structuredJson = if (activeEntityIds.isNotEmpty()) {
            """{"relatedEntityIds":[${activeEntityIds.joinToString(",") { "\"$it\"" }}]}"""
        } else null

        val entryId = java.util.UUID.randomUUID().toString()
        Log.d("CoachMemory", "✏️ saveToMemory: type=$type, entities=${activeEntityIds.size}, content='${content.take(40)}...'")

        memoryRepository.save(MemoryEntry(
            entryId = entryId,
            sessionId = _sessionId,
            content = content,
            entryType = type,
            createdAt = timeProvider.now.toEpochMilli(),
            updatedAt = timeProvider.now.toEpochMilli(),
            structuredJson = structuredJson,
            workflow = _currentMode.name  // Wave 4: 记录来源模式
        ))
    }
    
    /**
     * 记录工具执行结果
     */
    fun recordToolResult(toolId: String, title: String, preview: String) {
        _lastToolResult = ToolArtifact(
            toolId = toolId,
            title = title,
            preview = preview
        )
        _executedTools.add(toolId)
    }
    
    // ========================================
    // Entity Knowledge Context — Wave 3
    // ========================================
    
    /**
     * 构建实体知识图谱 JSON，注入 LLM prompt
     *
     * 从 EntityRepository 加载所有实体，按类型分组为结构化 JSON。
     * LLM 用此数据做自然消歧和别名匹配。
     *
     * @return JSON string 或 null（无实体时）
     * @see memory-center/spec.md § Entity Knowledge Context
     */
    private suspend fun buildEntityKnowledge(): String? {
        val entities = entityRepository.getAll(limit = 100)
        if (entities.isEmpty()) return null
        
        val root = JSONObject()
        
        // 按类型分组
        val people = entities.filter { it.entityType == EntityType.PERSON }
        val accounts = entities.filter { it.entityType == EntityType.ACCOUNT }
        val deals = entities.filter { it.entityType == EntityType.DEAL }
        
        if (people.isNotEmpty()) {
            root.put("people", JSONArray().apply {
                people.forEach { put(entityToJson(it)) }
            })
        }
        if (accounts.isNotEmpty()) {
            root.put("accounts", JSONArray().apply {
                accounts.forEach { put(entityToJson(it)) }
            })
        }
        if (deals.isNotEmpty()) {
            root.put("deals", JSONArray().apply {
                deals.forEach { put(entityToJson(it)) }
            })
        }
        
        val result = root.toString()
        Log.d("WorkingSet", "📸 buildEntityKnowledge: ${entities.size} entities, ${result.length} chars")
        return result
    }
    
    /**
     * 单个实体 → JSON 对象（仅包含非空字段）
     */
    private fun entityToJson(entry: EntityEntry): JSONObject {
        return JSONObject().apply {
            put("name", entry.displayName)
            
            // 别名（只有非空时才添加）
            val aliases = try { JSONArray(entry.aliasesJson) } catch (_: Exception) { JSONArray() }
            if (aliases.length() > 0) put("aliases", aliases)
            
            // CRM 字段（按类型有选择地添加）
            entry.jobTitle?.let { put("role", it) }
            entry.buyingRole?.let { put("buyingRole", it) }
            entry.accountId?.let { put("accountId", it) }
            entry.dealStage?.let { put("dealStage", it) }
            entry.dealValue?.let { put("dealValue", it) }
            entry.closeDate?.let { put("closeDate", it) }
            
            // 结构化智能字段（非空且非默认空值时添加）
            val attrs = try { JSONObject(entry.attributesJson) } catch (_: Exception) { null }
            if (attrs != null && attrs.length() > 0) put("attributes", attrs)
            
            val metrics = try { JSONObject(entry.metricsHistoryJson) } catch (_: Exception) { null }
            if (metrics != null && metrics.length() > 0) put("metrics", metrics)
            
            val decisions = try { JSONArray(entry.decisionLogJson) } catch (_: Exception) { null }
            if (decisions != null && decisions.length() > 0) put("decisions", decisions)
            
            val related = try { JSONArray(entry.relatedEntitiesJson) } catch (_: Exception) { null }
            if (related != null && related.length() > 0) put("related", related)
        }
    }

    // ========================================
    // Internal Kernel methods — called by EntityWriter (App → Kernel)
    // NOT on ContextBuilder interface
    // ========================================

    /**
     * 写入实体到 RAM Section 1（Write-Through 的 RAM 半段）
     *
     * EntityWriter 在 SSD 写入后调用此方法，确保当前会话能立即看到变更。
     * 同时自动填充 Section 3（客户习惯）。
     *
     * @param entityId 实体 ID
     * @param ref 实体引用
     */
    @Synchronized
    fun updateEntityInSession(entityId: String, ref: EntityRef) {
        // Section 1: 更新实体引用
        _sessionContext.entityContext["entity_$entityId"] = ref
        _sessionContext.markActive(entityId)
        Log.d("WorkingSet", "📝 Write-through S1: $entityId → ${ref.displayName}")

        // Section 3: 自动填充客户习惯
        val activeEntityIds = _sessionContext.entityContext.values.map { it.entityId }
        if (activeEntityIds.isNotEmpty()) {
            kotlinx.coroutines.runBlocking {
                _sessionContext.clientHabitContext = reinforcementLearner.loadClientHabits(activeEntityIds)
            }
            Log.d("WorkingSet", "📦 Section 3 auto-refreshed: ${activeEntityIds.size} entities")
        }
    }

    /**
     * 从 RAM Section 1 移除实体（删除时的 Write-Through）
     *
     * @param entityId 被删除的实体 ID
     */
    @Synchronized
    fun removeEntityFromSession(entityId: String) {
        _sessionContext.entityContext.entries.removeAll { it.value.entityId == entityId }
        _sessionContext.entityStates.remove(entityId)
        Log.d("WorkingSet", "🗑️ Write-through S1 removed: $entityId")
    }

    /**
     * Application → Kernel 回调: 记录实体变更历史
     *
     * EntityWriter 在 updateProfile() 检测到字段变更后调用。
     * 持久化为 MemoryEntry（entryType=TASK_RECORD），structuredJson 存储变更元数据。
     * FakeClientProfileHub.toUnifiedActivity() 负责反向映射为 UnifiedActivity。
     *
     * @param entityId 变更的实体 ID
     * @param type 变更类型（NAME_CHANGE, TITLE_CHANGE 等）
     * @param summary 人类可读摘要（如 "张总 → 张经理"）
     */
    suspend fun recordActivity(
        entityId: String,
        type: com.smartsales.prism.domain.crm.ActivityType,
        summary: String
    ) {
        val entryId = java.util.UUID.randomUUID().toString()
        val structuredJson = """{"activityType":"${type.name}","entityId":"$entityId","summary":"$summary"}"""

        memoryRepository.save(MemoryEntry(
            entryId = entryId,
            sessionId = _sessionId,
            content = summary,
            entryType = MemoryEntryType.TASK_RECORD,
            createdAt = timeProvider.now.toEpochMilli(),
            updatedAt = timeProvider.now.toEpochMilli(),
            structuredJson = structuredJson,
            workflow = "entity_writer"
        ))

        Log.d("WorkingSet", "📜 recordActivity: type=${type.name} entity=$entityId summary='$summary'")
    }

    /**
     * 重置会话（模式切换或新会话时调用）
     */
    fun resetSession() {
        _sessionHistory.clear()
        _lastToolResult = null
        _executedTools.clear()
        _sessionId = "session-${System.currentTimeMillis()}"
        _sessionContext = SessionContext(
            sessionId = _sessionId,
            createdAt = timeProvider.now.toEpochMilli()
        )
    }
    
    /**
     * 修剪历史，保留最近10轮
     */
    private fun pruneHistory() {
        while (_sessionHistory.size > 20) { // 10轮 = 20条消息（user + assistant）
            _sessionHistory.removeAt(0)
        }
    }
    
    /**
     * Context Strategy: First Turn Search
     * 
     * Rely on Session Context pattern:
     * 1. First turn: Search MemoryRepository to seed session context
     * 2. Subsequent turns: Trust in-memory session history (no re-search)
     * 
     * Future (Phase 3): Add Entity State Machine triggers here.
     */
    private fun shouldSearchMemory(input: String, sessionHistory: List<ChatTurn>): Boolean {
        // Simple, robust heuristic: If allow-list is empty (new session), search memory.
        // Once session is established, we rely on the conversation context.
        val shouldSearch = sessionHistory.size < 2
        
        Log.d("CoachMemory", "shouldSearchMemory=$shouldSearch (historySize=${sessionHistory.size})")
        return shouldSearch
    }
}
