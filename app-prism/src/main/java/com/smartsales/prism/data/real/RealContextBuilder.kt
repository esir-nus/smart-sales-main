package com.smartsales.prism.data.real

import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.EntityRef
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.pipeline.ToolArtifact
import com.smartsales.prism.domain.scheduler.ParsedClues
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
    private val entityRepository: EntityRepository
) : ContextBuilder {
    
    // 会话历史（最多保留10轮，避免上下文膨胀）
    private val _sessionHistory = mutableListOf<ChatTurn>()
    private var _lastToolResult: ToolArtifact? = null
    private val _executedTools = mutableSetOf<String>()
    
    private var _turnIndex = 0
    private var _sessionId = "session-${System.currentTimeMillis()}"
    
    override suspend fun build(userText: String, mode: Mode): EnhancedContext {
        _turnIndex++
        
        return EnhancedContext(
            userText = userText,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _sessionId,
                turnIndex = _turnIndex
            ),
            sessionHistory = _sessionHistory.toList(),
            lastToolResult = _lastToolResult,
            executedTools = _executedTools.toSet(),
            // 使用 TimeProvider 获取格式化的日期时间（包含时间，支持 "今晚"、"3小时后"）
            currentDate = timeProvider.formatForLlm()
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
        _turnIndex++
        
        // 根据线索查询 RelevancyLib
        val entityContext = mutableMapOf<String, EntityRef>()
        
        // 按人物线索查询候选
        clues.person?.let { personClue ->
            val candidates = entityRepository.findByAlias(personClue)
            candidates.forEachIndexed { index, entry ->
                entityContext["person_candidate_$index"] = EntityRef(
                    entityId = entry.entityId,
                    displayName = entry.displayName,
                    entityType = entry.entityType.name
                )
            }
        }
        
        // 按地点线索查询候选
        clues.location?.let { locationClue ->
            val locationCandidates = entityRepository.search(locationClue, limit = 3)
                .filter { it.entityType == EntityType.LOCATION }
            locationCandidates.forEachIndexed { index, entry ->
                entityContext["location_candidate_$index"] = EntityRef(
                    entityId = entry.entityId,
                    displayName = entry.displayName,
                    entityType = entry.entityType.name
                )
            }
        }
        
        return EnhancedContext(
            userText = userText,
            modeMetadata = ModeMetadata(
                currentMode = mode,
                sessionId = _sessionId,
                turnIndex = _turnIndex
            ),
            sessionHistory = _sessionHistory.toList(),
            lastToolResult = _lastToolResult,
            executedTools = _executedTools.toSet(),
            currentDate = timeProvider.formatForLlm(),
            entityContext = entityContext  // Phase 2 实体线索
        )
    }
    
    /**
     * 记录用户输入到历史
     */
    fun recordUserTurn(content: String) {
        _sessionHistory.add(ChatTurn(role = "user", content = content))
        pruneHistory()
    }
    
    /**
     * 记录助手响应到历史
     */
    fun recordAssistantTurn(content: String) {
        _sessionHistory.add(ChatTurn(role = "assistant", content = content))
        pruneHistory()
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
        _turnIndex = 0
        _sessionId = "session-${System.currentTimeMillis()}"
    }
    
    /**
     * 修剪历史，保留最近10轮
     */
    private fun pruneHistory() {
        while (_sessionHistory.size > 20) { // 10轮 = 20条消息（user + assistant）
            _sessionHistory.removeAt(0)
        }
    }
}
