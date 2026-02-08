package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.session.EntityState
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.MemoryHit
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.pipeline.ToolArtifact
import com.smartsales.prism.domain.scheduler.ParsedClues
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
 * @see Prism-V1.md §2.2 #1
 */
@Singleton
class RealContextBuilder @Inject constructor(
    private val timeProvider: TimeProvider,
    private val entityRepository: EntityRepository,
    private val reinforcementLearner: ReinforcementLearner,
    private val memoryRepository: MemoryRepository
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
    
    override suspend fun build(userText: String, mode: Mode): EnhancedContext {
        _sessionContext.incrementTurn()
        
        // Wave 3: Memory search (session-history-aware)
        val memoryHits = if (shouldSearchMemory(userText, _sessionHistory)) {
            memoryRepository.search(userText, limit = 5).map { entry ->
                MemoryHit(
                    entryId = entry.entryId,
                    content = entry.content,
                    relevanceScore = 1.0f
                )
            }
        } else {
            emptyList()
        }
        
        // Wave 3: 获取全局习惯（无实体上下文）
        val habitContext = reinforcementLearner.getHabitContext(entityIds = null)
        
        return EnhancedContext(
            userText = userText,
            memoryHits = memoryHits,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _sessionId,
                turnIndex = _sessionContext.turnCount
            ),
            sessionHistory = _sessionHistory.toList(),
            lastToolResult = _lastToolResult,
            executedTools = _executedTools.toSet(),
            // 使用 TimeProvider 获取格式化的日期时间（包含时间，支持 "今晚"、"3小时后"）
            currentDate = timeProvider.formatForLlm(),
            habitContext = habitContext
        )
    }
    
    /**
     * Phase 2 构建器 — 带 RelevancyLib 实体线索
     * 
     * @param userText 原始用户文本
     * @param mode 当前模式
     * @param clues Phase 1 解析的线索
     * @return 增强上下文（含实体引用）
     */
    suspend fun buildWithClues(userText: String, mode: Mode, clues: ParsedClues): EnhancedContext {
        _sessionContext.incrementTurn()
        
        // 根据线索查询 RelevancyLib
        val entityContext = mutableMapOf<String, EntityRef>()
        
        // 按人物线索查询候选（Wave 2: 路径索引缓存）
        clues.person?.let { personClue ->
            // 先检查会话缓存（O(1)）
            val cachedId = _sessionContext.resolveAlias(personClue)
            if (cachedId != null) {
                Log.d("PathIndex", "HIT alias=$personClue → $cachedId")
                val entry = entityRepository.getById(cachedId)
                if (entry != null) {
                    entityContext["person_candidate_0"] = EntityRef(
                        entityId = entry.entityId,
                        displayName = entry.displayName,
                        entityType = entry.entityType.name
                    )
                    _sessionContext.markActive(cachedId)  // Wave 3: 标记为 ACTIVE（幂等）
                    return@let  // 缓存命中，跳过 DB 搜索
                }
                // 缓存的 ID 对应的实体已被删除，走 fallback
                Log.d("PathIndex", "STALE alias=$personClue, cachedId=$cachedId not found in DB")
            }

            // 缓存未命中：DB 查询，然后缓存第一个结果
            Log.d("PathIndex", "MISS alias=$personClue")
            val candidates = entityRepository.findByAlias(personClue)
            candidates.forEachIndexed { index, entry ->
                entityContext["person_candidate_$index"] = EntityRef(
                    entityId = entry.entityId,
                    displayName = entry.displayName,
                    entityType = entry.entityType.name
                )
                // 缓存第一个候选（Scheduler 管线只使用 candidate_0）
                if (index == 0) {
                    _sessionContext.cacheAlias(personClue, entry.entityId)
                    _sessionContext.markActive(entry.entityId)  // Wave 3: 标记为已加载
                }
            }
        }
        
        // 按地点线索查询候选（Wave 2: 路径索引缓存）
        clues.location?.let { locationClue ->
            val cachedId = _sessionContext.resolveAlias(locationClue)
            if (cachedId != null) {
                Log.d("PathIndex", "HIT alias=$locationClue → $cachedId")
                val entry = entityRepository.getById(cachedId)
                if (entry != null && entry.entityType == EntityType.LOCATION) {
                    entityContext["location_candidate_0"] = EntityRef(
                        entityId = entry.entityId,
                        displayName = entry.displayName,
                        entityType = entry.entityType.name
                    )
                    _sessionContext.markActive(cachedId)  // Wave 3: 标记为 ACTIVE（幂等）
                    return@let
                }
            }

            Log.d("PathIndex", "MISS alias=$locationClue")
            val locationCandidates = entityRepository.search(locationClue, limit = 3)
                .filter { it.entityType == EntityType.LOCATION }
            locationCandidates.forEachIndexed { index, entry ->
                entityContext["location_candidate_$index"] = EntityRef(
                    entityId = entry.entityId,
                    displayName = entry.displayName,
                    entityType = entry.entityType.name
                )
                if (index == 0) {
                    _sessionContext.cacheAlias(locationClue, entry.entityId)
                    _sessionContext.markActive(entry.entityId)  // Wave 3: 标记为已加载
                }
            }
        }
        
        // Wave 3: 从实体上下文提取 ID，获取习惯
        val entityIds = entityContext.values.map { it.entityId }.takeIf { it.isNotEmpty() }
        val habitContext = reinforcementLearner.getHabitContext(entityIds)
        
        return EnhancedContext(
            userText = userText,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _sessionId,
                turnIndex = _sessionContext.turnCount
            ),
            sessionHistory = _sessionHistory.toList(),
            lastToolResult = _lastToolResult,
            executedTools = _executedTools.toSet(),
            currentDate = timeProvider.formatForLlm(),
            entityContext = entityContext,  // Phase 2 实体线索
            habitContext = habitContext      // Wave 3 习惯上下文
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

        memoryRepository.save(MemoryEntry(
            entryId = java.util.UUID.randomUUID().toString(),
            sessionId = _sessionId,
            content = content,
            entryType = type,
            createdAt = timeProvider.now.toEpochMilli(),
            updatedAt = timeProvider.now.toEpochMilli(),
            structuredJson = structuredJson
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
