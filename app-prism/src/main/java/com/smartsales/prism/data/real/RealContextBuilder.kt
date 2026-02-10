package com.smartsales.prism.data.real

import android.util.Log
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
        
        // Section 2: 会话首次加载用户习惯（一次性）
        if (_sessionContext.userHabitContext == null) {
            _sessionContext.userHabitContext = reinforcementLearner.loadUserHabits()
            Log.d("WorkingSet", "📦 Section 2 loaded: userHabits")
        }
        
        // Section 1: 记忆搜索（首轮触发）
        if (shouldSearchMemory(userText, _sessionHistory)) {
            val hits = memoryRepository.search(userText, limit = 5).map { entry ->
                MemoryHit(
                    entryId = entry.entryId,
                    content = entry.content,
                    relevanceScore = 1.0f
                )
            }
            _sessionContext.memoryHits = hits  // 写入 RAM Section 1
            Log.d("WorkingSet", "🔍 Section 1: memorySearch('${userText.take(30)}') → ${hits.size} hits")
        }
        
        return EnhancedContext(
            userText = userText,
            memoryHits = _sessionContext.memoryHits,  // 从 RAM 读取
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _sessionId,
                turnIndex = _sessionContext.turnCount
            ),
            sessionHistory = _sessionHistory.toList(),
            lastToolResult = _lastToolResult,
            executedTools = _executedTools.toSet(),
            currentDate = timeProvider.formatForLlm(),
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
