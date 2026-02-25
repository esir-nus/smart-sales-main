package com.smartsales.prism.domain.repository

import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SessionPreview
import kotlinx.coroutines.flow.Flow

/**
 * 历史会话仓库 — 获取和管理会话列表
 */
interface HistoryRepository {
    /**
     * 获取分组会话列表（同步）
     */
    fun getGroupedSessions(): Map<String, List<SessionPreview>>

    /**
     * 响应式获取分组会话列表
     * Room 表变更时自动重新发射
     */
    fun getGroupedSessionsFlow(): Flow<Map<String, List<SessionPreview>>>
    
    /**
     * 获取所有会话
     */
    fun getSessions(): List<SessionPreview>
    
    /**
     * 置顶/取消置顶会话
     */
    fun togglePin(sessionId: String)
    
    /**
     * 重命名会话
     */
    fun renameSession(sessionId: String, newClientName: String, newSummary: String)
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String)
    
    /**
     * 获取会话详情
     */
    fun getSession(sessionId: String): SessionPreview?
    
    /**
     * 创建新会话
     * @param linkedAudioId 可选，关联的音频文件ID（用于分析模式）
     * @return 新会话的ID
     */
    fun createSession(clientName: String, summary: String, linkedAudioId: String? = null): String

    /**
     * 保存消息到会话
     */
    fun saveMessage(sessionId: String, isUser: Boolean, content: String, orderIndex: Int)

    /**
     * 获取会话消息列表
     */
    fun getMessages(sessionId: String): List<ChatMessage>

    /**
     * 清除会话消息（用于重写）
     */
    fun clearMessages(sessionId: String)
}
